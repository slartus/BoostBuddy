package ru.slartus.boostbuddy.components.video

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.repositories.VideoRepository
import kotlin.math.abs

internal class TimeCodeManager(
    private val scope: CoroutineScope,
    private val videoRepository: VideoRepository,
) {
    private var contentId: String? = null
    private val timeCodeFlow = MutableSharedFlow<Long>(replay = 0)
    private val lastSentPosition: AtomicLong = atomic(0L)

    init {
        subscribeToTimeCodeFlow()
    }

    fun setContentId(id: String) {
        this.contentId = id
    }

    fun onPositionChanged(position: Long) = scope.launch {
        timeCodeFlow.emit(position)
    }

    fun putLastPosition() = scope.launch {
        timeCodeFlow.replayCache.lastOrNull()?.let { position ->
            putTimeCode(position)
        }
    }

    private fun subscribeToTimeCodeFlow() = scope.launch {
        timeCodeFlow
            .collect { position ->
                val positionDiff = abs(position - lastSentPosition.value)
                val timeDiffSeconds = positionDiff / 1000

                if (timeDiffSeconds >= DEBOUNCE_SECONDS) {
                    putTimeCode(position)
                }
            }
    }

    private suspend fun putTimeCode(position: Long) {
        val id = contentId ?: return
        runCatching {
            videoRepository.putTimeCode(id, position / 1000)
        }.onSuccess {
            lastSentPosition.getAndSet(position)
        }
    }

    private companion object {
        const val DEBOUNCE_SECONDS = 10
    }
}