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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PostDataTextContent
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.theme.LightColorScheme

@Composable
fun ContentView(postData: Content, onVideoClick: (okVideoData: Content.OkVideo) -> Unit) {
    FocusableBox {
        when (postData) {
            is Content.Text -> PostDataTextView(postData)
            Content.Unknown -> PostDataUnknownView()
            is Content.OkVideo -> PostDataOkVideoView(postData, onVideoClick)
            is Content.Image -> PostDataImageView(postData)
            is Content.Link -> PostDataLinkView(postData)
            is Content.Video -> PostDataVideoView(postData)
            is Content.AudioFile -> PostDataAudioFileView(postData)
            is Content.Smile -> ContentSmileView(postData)
        }
    }
}

@Composable
private fun ContentSmileView(data: Content.Smile) {
    val url = remember(data) { data.mediumUrl ?: data.smallUrl ?: data.smallUrl.orEmpty() }
    Image(
        modifier = Modifier.size(24.dp),
        painter = rememberImagePainter(url),
        contentDescription = "smile",
    )
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
private fun PostDataTextView(postData: Content.Text) {
    val annotatedText = postData.content?.rememberAnnotatedString() ?: return

    val platformConfiguration = LocalPlatformConfiguration.current

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val onClick: (Int) -> Unit = { offset ->
        annotatedText.getStringAnnotations(start = offset, end = offset).firstOrNull()?.let {
            platformConfiguration.openBrowser(it.item)
        }
    }
    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            layoutResult.value?.let { layoutResult ->
                onClick(layoutResult.getOffsetForPosition(pos))
            }
        }
    }
    Text(
        modifier = Modifier.then(pressIndicator).fillMaxWidth().focusable(),
        text = annotatedText,
        style = MaterialTheme.typography.bodySmall,
        onTextLayout = {
            layoutResult.value = it
        }
    )
}

@Composable
private fun PostDataTextContent.rememberAnnotatedString(): AnnotatedString {
    val linkColor = remember { Color(241, 95, 44) }
    return remember {
        buildAnnotatedString {
            append(text)
            styleData?.forEach { styleData ->
                addStyle(
                    styleData.style.toSpanStyle(),
                    styleData.from,
                    styleData.from + styleData.length
                )
            }
            urls?.forEach { url ->
                addStringAnnotation(
                    tag = "URL",
                    annotation = url.url,
                    start = url.from,
                    end = url.from + url.length
                )
                addStyle(
                    SpanStyle(color = linkColor),
                    start = url.from,
                    end = url.from + url.length
                )
            }
        }
    }
}

private fun PostDataTextContent.Style.toSpanStyle(): SpanStyle = when (this) {
    PostDataTextContent.Style.Normal -> SpanStyle(fontStyle = FontStyle.Normal)
    PostDataTextContent.Style.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    PostDataTextContent.Style.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    PostDataTextContent.Style.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
}

@Composable
private fun PostDataAudioFileView(
    postData: Content.AudioFile
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
    postData: Content.Video
) {
    val platformConfiguration = LocalPlatformConfiguration.current
    if (postData.previewUrl != null) {
        VideoPreview(
            url = postData.previewUrl,
            onClick = {
                platformConfiguration.openBrowser(postData.url)
            }
        )
    } else {
        Text(
            modifier = Modifier.fillMaxWidth().clickable {
                platformConfiguration.openBrowser(postData.url)
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
private fun PostDataLinkView(
    postData: Content.Link
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