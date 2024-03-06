package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import ru.slartus.boostbuddy.data.repositories.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.PostData

interface VideoComponent {
    val state: Value<VideoViewState>
}

data class VideoViewState(
    val postData: PostData
){
    val playerUrl: PlayerUrl? = postData.videoUrls?.firstOrNull()
}

class VideoComponentImpl(
    componentContext: ComponentContext,
    private val postData: PostData
) : VideoComponent, ComponentContext by componentContext {
    private val scope = coroutineScope()

    private val _state = MutableValue(VideoViewState(postData))
    override var state: Value<VideoViewState> = _state

}