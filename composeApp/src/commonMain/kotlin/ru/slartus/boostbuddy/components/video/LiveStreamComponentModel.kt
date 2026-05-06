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

/**
 * Heartbeat для live-стрима: периодический PUT на server чтобы он знал, что
 * клиент смотрит. VideoComponentImpl создаёт экземпляр только когда
 * `liveBlogUrl != null` — для VOD-флоу модель отсутствует.
 */
internal class LiveStreamComponentModel(
    private val scope: CoroutineScope,
    private val streamRepository: StreamRepository,
    private val blogUrl: String,
) {
    // Atomic чтобы stopHeartbeat был thread-safe: его могут вызвать одновременно
    // handlePlaybackEnded (Main) и doOnDestroy (lifecycle thread), плюс external
    // companion scope бежит на Dispatchers.Default.
    private val heartbeatJob = atomic<Job?>(null)
    private val heartbeatStopped = atomic(false)

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

    private companion object {
        private const val HEARTBEAT_INTERVAL_MS = 30_000L

        // Live'ём scope-singleton'ом, чтобы stop-heartbeat пережил onDestroy
        // компонента: heartbeat должен быть закрыт серверу до того как мы уйдём
        // с экрана. Сам scope не отменяем — fire-and-forget с NonCancellable.
        private val externalHeartbeatScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
