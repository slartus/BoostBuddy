package ru.slartus.boostbuddy.components.blog

import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.PostData

interface VideoTypeComponent {
    val postData: PostData.OkVideo
    fun onDismissClicked()
    fun onItemClicked(playerUrl: PlayerUrl)
}

class VideoTypeComponentImpl(
    override val postData: PostData.OkVideo,
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