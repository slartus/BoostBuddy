package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.data.repositories.models.PostData
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration

@Composable
fun PostDataView(postData: PostData, onVideoClick: (okVideoData: PostData.OkVideo) -> Unit) {
    FocusableBox {
        when (postData) {
            is PostData.Text -> PostDataTextView(postData)
            PostData.Unknown -> PostDataUnknownView()
            is PostData.OkVideo -> PostDataOkVideoView(postData, onVideoClick)
            is PostData.Image -> PostDataImageView(postData)
            is PostData.Link -> PostDataLinkView(postData)
            is PostData.Video -> PostDataVideoView(postData)
            is PostData.AudioFile -> PostDataAudioFileView(postData)
        }
    }
}

@Composable
private fun PostDataUnknownView() {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "UNKNOWN_CONTENT",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun PostDataTextView(postData: PostData.Text) {
    Text(
        modifier = Modifier.fillMaxWidth().focusable(),
        text = postData.content.text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium
    )
}


@Composable
private fun PostDataAudioFileView(
    postData: PostData.AudioFile
) {
    val platformConfiguration = LocalPlatformConfiguration.current
    Text(
        modifier = Modifier.fillMaxWidth().clickable {
            platformConfiguration.openBrowser(postData.url)
        },
        text = postData.title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun PostDataVideoView(
    postData: PostData.Video
) {
    val platformConfiguration = LocalPlatformConfiguration.current
    Text(
        modifier = Modifier.fillMaxWidth().clickable {
            platformConfiguration.openBrowser(postData.url)
        },
        text = postData.url,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium
    )
}


@Composable
private fun PostDataLinkView(
    postData: PostData.Link
) {
    val platformConfiguration = LocalPlatformConfiguration.current

    Text(
        modifier = Modifier.fillMaxWidth().clickable {
            platformConfiguration.openBrowser(postData.url)
        },
        text = postData.text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun PostDataImageView(postData: PostData.Image) {
    Box(modifier = Modifier.heightIn(min = 200.dp).focusable()) {
        Image(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .wrapContentHeight(),
            painter = rememberImagePainter(postData.url),
            contentDescription = "url",
        )
    }
}

@Composable
private fun PostDataOkVideoView(
    postData: PostData.OkVideo,
    onVideoClick: (okVideoData: PostData.OkVideo) -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable { onVideoClick(postData) }
            .heightIn(min = 200.dp)
    ) {
        Image(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .wrapContentHeight(),
            painter = rememberImagePainter(postData.previewUrl),
            contentDescription = "preview",
        )
    }
}