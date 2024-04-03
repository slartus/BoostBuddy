package ru.slartus.boostbuddy.components.blog

import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl

interface VideoTypeComponent {
    val postData: Content.OkVideo
    fun onDismissClicked()
    fun onItemClicked(playerUrl: PlayerUrl)
}

class VideoTypeComponentImpl(
    override val postData: Content.OkVideo,
    private val onDismissed: () -> Unit,
    private val onItemClicked: (PlayerUrl) -> Unit
) : VideoTypeComponent {
    override fun onDismissClicked() {
        onDismissed()
    }

    override fun onItemClicked(playerUrl: PlayerUrl) {
        onItemClicked.invoke(playerUrl)
    }

}