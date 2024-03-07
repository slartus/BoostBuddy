package ru.slartus.boostbuddy.components

import ru.slartus.boostbuddy.data.repositories.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.PostData

interface VideoTypeComponent {
    val postData: PostData
    fun onDismissClicked()
    fun onItemClicked(playerUrl: PlayerUrl)
}

class VideoTypeComponentImpl(
    override val postData: PostData,
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