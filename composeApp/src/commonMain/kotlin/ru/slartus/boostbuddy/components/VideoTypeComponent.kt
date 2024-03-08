package ru.slartus.boostbuddy.components

import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.PostData

interface VideoTypeComponent {
    val postData: PostData.Video
    fun onDismissClicked()
    fun onItemClicked(playerUrl: PlayerUrl)
}

class VideoTypeComponentImpl(
    override val postData: PostData.Video,
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