package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRect
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import ru.slartus.boostbuddy.components.video.VideoState

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    vid: String,
    url: String,
    title: String,
    position: Long,
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit,
    onStopClick: () -> Unit
) {

    val player = remember { AVPlayer(uRL = NSURL.URLWithString(url)!!) }
    val playerLayer = remember { AVPlayerLayer() }
    val avPlayerViewController = remember { AVPlayerViewController() }
    avPlayerViewController.player = player
    avPlayerViewController.showsPlaybackControls = true

    playerLayer.player = player
    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val playerContainer = UIView()
            playerContainer.addSubview(avPlayerViewController.view)
            // Return the playerContainer as the root UIView
            playerContainer.apply {

            }
        },
        onResize = { view: UIView, rect: CValue<CGRect> ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.setFrame(rect)
            playerLayer.setFrame(rect)
            avPlayerViewController.view.layer.frame = rect
            CATransaction.commit()
        },
        update = { view ->
            player.play()
            avPlayerViewController.player!!.play()
        }
    )
}