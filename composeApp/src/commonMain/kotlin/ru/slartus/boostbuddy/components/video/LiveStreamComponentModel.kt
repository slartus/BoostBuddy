package ru.slartus.boostbuddy.components.video

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.repositories.StreamRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality

/**
 * Инкапсулирует live-специфичную логику плеера: heartbeat, наличие DVR,
 * выбор URL под режим (live edge / DVR seek-back). VideoComponentImpl создаёт
 * экземпляр только когда `liveBlogUrl != null` — для VOD-флоу модель отсутствует.
 */
internal class LiveStreamComponentModel(
    private val scope: CoroutineScope,
    private val streamRepository: StreamRepository,
    private val blogUrl: String,
    private val postData: Content.OkVideo,
) {
    // Atomic чтобы stopHeartbeat был thread-safe: его могут вызвать одновременно
    // handlePlaybackEnded (Main) и doOnDestroy (lifecycle thread), плюс external
    // companion scope бежит на Dispatchers.Default.
    private val heartbeatJob = atomic<Job?>(null)
    private val heartbeatStopped = atomic(false)
    private val hasReachedLiveEdge = atomic(false)
    private val dvrFallbackAttempts = atomic(0)

    val hasDvrSupport: Boolean
        get() = postData.dvrPlayerUrls.isNotEmpty()

    fun startHeartbeat() {
        heartbeatJob.getAndSet(null)?.cancel()
        heartbeatJob.value = scope.launch {
            while (isActive) {
                streamRepository.heartbeat(blogUrl, stop = false)
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun stopHeartbeat() {
        if (!heartbeatStopped.compareAndSet(expect = false, update = true)) return
        heartbeatJob.getAndSet(null)?.cancel()
        externalHeartbeatScope.launch(NonCancellable) {
            streamRepository.heartbeat(blogUrl, stop = true)
        }
    }

    /**
     * Возвращает effective atEdge для применения, или null если update нужно
     * проигнорировать. Защищаемся от cold-start окна, когда ExoPlayer ещё
     * догоняет live edge: пока хотя бы раз не пришёл `true`, событие `false`
     * считаем шумом догоняния буфера, а не пользовательским seek-back —
     * иначе свапнемся на DVR URL до того, как у CDN появится DVR-буфер,
     * и получим 404. Без DVR — всегда atEdge=true, чтобы не показывать
     * пустые состояния (selector скорости и т.п.).
     */
    fun processLiveEdgeChange(requestedAtEdge: Boolean): Boolean? {
        if (!hasDvrSupport) return true
        if (requestedAtEdge) {
            hasReachedLiveEdge.value = true
            return true
        }
        return if (hasReachedLiveEdge.value) false else null
    }

    /**
     * Сбросить трекинг достижения live edge. Используется после fallback
     * с DVR URL обратно на live edge URL: на новом источнике мы снова
     * проходим cold-start окно, и `false` события от controller'а до первого
     * достижения live edge снова должны игнорироваться.
     */
    fun resetLiveEdgeTracking() {
        hasReachedLiveEdge.value = false
    }

    /**
     * Защита от бесконечного цикла DVR↔live-edge fallback'ов: если post-fallback
     * controller успеет послать `true→false` до первого Ready, мы снова свапнем
     * на DVR URL, снова получим 404 и снова фолбэчимся. После MAX_DVR_FALLBACKS
     * прекращаем попытки и отдаём ошибку наверх. Сбрасывается на Ready.
     */
    fun shouldAttemptDvrFallback(): Boolean {
        val attempts = dvrFallbackAttempts.incrementAndGet()
        return attempts <= MAX_DVR_FALLBACKS
    }

    fun resetDvrFallbackAttempts() {
        dvrFallbackAttempts.value = 0
    }

    fun playerUrlForMode(atEdge: Boolean, quality: VideoQuality): PlayerUrl? {
        if (!hasDvrSupport) return null
        val source = if (atEdge) postData.playerUrls else postData.dvrPlayerUrls
        return source.firstOrNull { it.quality == quality }
    }

    private companion object {
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val MAX_DVR_FALLBACKS = 2

        // Live'ём scope-singleton'ом, чтобы stop-heartbeat пережил onDestroy
        // компонента: heartbeat должен быть закрыт серверу до того как мы уйдём
        // с экрана. Сам scope не отменяем — fire-and-forget с NonCancellable.
        private val externalHeartbeatScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
