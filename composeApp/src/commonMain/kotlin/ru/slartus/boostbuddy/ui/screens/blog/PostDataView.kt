package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.common.QrDialog
import ru.slartus.boostbuddy.ui.theme.LightColorScheme
import ru.slartus.boostbuddy.utils.Platform

@Composable
internal fun ContentView(
    signedQuery: String,
    postData: Content,
    onVideoClick: (okVideoData: Content.OkVideo) -> Unit
) {
    val platformConfiguration = LocalPlatformConfiguration.current
    var qrDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }
    val openUrl: (title: String, url: String) -> Unit = { title, url ->
        when (platformConfiguration.platform) {
            Platform.Android,
            Platform.iOS -> platformConfiguration.openBrowser(url) {
                qrDialogData = title to url
            }

            Platform.AndroidTV -> qrDialogData = title to url
        }
    }
    when (postData) {
        is Content.Link,
        is Content.Text,
        is Content.Smile,
        Content.Unknown -> FocusableBox { PostDataUnknownView() }

        is Content.OkVideo -> FocusableBox { PostDataOkVideoView(postData, onVideoClick) }
        is Content.Image -> FocusableBox { PostDataImageView(postData) }
        is Content.Video -> FocusableBox { PostDataVideoView(postData, openUrl) }
        is Content.AudioFile -> PostDataAudioFileView(signedQuery, postData)
        is Content.AnnotatedText -> FocusableBox { AnnotatedTextView(postData, openUrl) }
    }

    qrDialogData?.let { data ->
        QrDialog(data.first, data.second, onDismiss = {
            qrDialogData = null
        })
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
private fun AnnotatedTextView(
    postData: Content.AnnotatedText,
    onUrlClick: (title: String, url: String) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val onClick: (Int) -> Unit = { offset ->
        postData.content.getStringAnnotations(start = offset, end = offset)
            .firstOrNull()
            ?.let {
                onUrlClick(postData.content.substring(42..53), it.item)
            }
    }
    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            layoutResult.value?.let { layoutResult ->
                onClick(layoutResult.getOffsetForPosition(pos))
            }
        }
    }
    val inlineContentMap = remember(postData) {
        postData.smiles.distinctBy { it.name }.associate { smile ->
            smile.name to InlineTextContent(
                Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = rememberImagePainter(
                        smile.mediumUrl ?: smile.smallUrl ?: smile.smallUrl.orEmpty()
                    ),
                    contentDescription = "smile ${smile.name}",
                )
            }
        }
    }
    Text(
        modifier = Modifier.then(pressIndicator).fillMaxWidth().focusable(),
        text = postData.content,
        inlineContent = inlineContentMap,
        style = MaterialTheme.typography.bodySmall,
        onTextLayout = {
            layoutResult.value = it
        }
    )
}

@Composable
private fun PostDataVideoView(
    postData: Content.Video,
    onUrlClick: (title: String, url: String) -> Unit
) {
    if (postData.previewUrl != null) {
        VideoPreview(
            url = postData.previewUrl,
            onClick = {
                onUrlClick("Видео", postData.url)
            }
        )
    } else {
        Text(
            modifier = Modifier.fillMaxWidth().clickable {
                onUrlClick("Видео", postData.url)
            },
            text = postData.url,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun VideoPreview(url: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .heightIn(min = 200.dp)
    ) {
        Image(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .wrapContentHeight(),
            painter = rememberImagePainter(url),
            contentDescription = "preview",
        )

        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .align(Alignment.Center)
                .padding(4.dp)
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                tint = LightColorScheme.background,
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play video icon"
            )
        }
    }
}

@Composable
private fun PostDataImageView(postData: Content.Image) {
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
    postData: Content.OkVideo,
    onVideoClick: (okVideoData: Content.OkVideo) -> Unit,
) {
    VideoPreview(postData.previewUrl, onClick = { onVideoClick(postData) })
}