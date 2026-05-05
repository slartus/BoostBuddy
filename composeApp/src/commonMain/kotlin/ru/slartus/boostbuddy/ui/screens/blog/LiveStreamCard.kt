package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.data.repositories.models.LiveStream

@Composable
internal fun LiveStreamCard(
    stream: LiveStream,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverFallbackUrl: String? = null,
) {
    val isLive = stream.status is LiveStream.Status.Live
    val title = stream.title.ifEmpty { DEFAULT_TITLE }
    val overlayLabel = when (stream.status) {
        is LiveStream.Status.Live -> "Идёт трансляция"
        LiveStream.Status.Scheduled -> "Трансляция скоро начнётся"
    }
    val coverUrl = stream.coverImageUrl
        ?: stream.video?.previewUrl
        ?: coverFallbackUrl?.takeIf { it.isNotBlank() }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(LiveStreamCardTestTags.ROOT),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderRow(isLive = isLive, hasAccess = stream.hasAccess)
            Text(
                modifier = Modifier.testTag(LiveStreamCardTestTags.TITLE),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Cover(
                previewUrl = coverUrl,
                overlayLabel = overlayLabel,
                onClick = onClick,
            )
            ActionsRow(
                isLiked = stream.isLiked,
                likesCount = stream.likesCount,
                onLikeClick = onLikeClick,
                onShareClick = onShareClick,
            )
            if (!stream.hasAccess && stream.subscription != null) {
                TierHint(subscription = stream.subscription)
            }
        }
    }
}

@Composable
private fun ActionsRow(
    isLiked: Boolean,
    likesCount: Int,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(LiveStreamCardTestTags.ACTIONS_ROW),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.testTag(LiveStreamCardTestTags.LIKE_BUTTON),
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Убрать лайк" else "Лайк",
                tint = if (isLiked) LIVE_RED else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (likesCount > 0) {
            Text(
                modifier = Modifier.testTag(LiveStreamCardTestTags.LIKES_COUNT),
                text = likesCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onShareClick,
            modifier = Modifier.testTag(LiveStreamCardTestTags.SHARE_BUTTON),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Поделиться",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeaderRow(
    isLive: Boolean,
    hasAccess: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.testTag(LiveStreamCardTestTags.EYEBROW),
            text = "Трансляция",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.weight(1f))
        StatusBadge(isLive = isLive)
        if (!hasAccess) {
            Icon(
                modifier = Modifier
                    .size(18.dp)
                    .testTag(LiveStreamCardTestTags.LOCK_ICON),
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Платный контент",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusBadge(isLive: Boolean, modifier: Modifier = Modifier) {
    val containerColor = if (isLive) LIVE_RED else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isLive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(LiveStreamCardTestTags.STATUS_BADGE),
    ) {
        Text(
            text = if (isLive) "LIVE" else "СКОРО",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

@Composable
private fun Cover(
    previewUrl: String?,
    overlayLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(COVER_ASPECT_RATIO)
            .clip(RoundedCornerShape(12.dp))
            .background(COVER_FALLBACK)
            .clickable(onClick = onClick)
            .testTag(LiveStreamCardTestTags.COVER),
    ) {
        if (previewUrl != null) {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = rememberImagePainter(previewUrl),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f),
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .testTag(LiveStreamCardTestTags.PLAY_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Открыть трансляцию",
                tint = Color.White,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .testTag(LiveStreamCardTestTags.OVERLAY_LABEL),
            text = overlayLabel,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun TierHint(subscription: LiveStream.Subscription, modifier: Modifier = Modifier) {
    val priceText = subscription.priceRub
        ?.let { rub -> " · ${rub.toLong()} ₽" }
        .orEmpty()
    Row(
        modifier = modifier.testTag(LiveStreamCardTestTags.TIER_HINT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${subscription.name}$priceText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val DEFAULT_TITLE = "Трансляция"
private const val COVER_ASPECT_RATIO = 16f / 9f
private val LIVE_RED = Color(0xFFE53935)
private val COVER_FALLBACK = Color(0xFF2E2E2E)

internal object LiveStreamCardTestTags {
    private const val NAME = "live_stream_card"
    const val ROOT = "${NAME}_root"
    const val EYEBROW = "${NAME}_eyebrow"
    const val STATUS_BADGE = "${NAME}_status_badge"
    const val LOCK_ICON = "${NAME}_lock_icon"
    const val TITLE = "${NAME}_title"
    const val COVER = "${NAME}_cover"
    const val PLAY_BUTTON = "${NAME}_play_button"
    const val OVERLAY_LABEL = "${NAME}_overlay_label"
    const val ACTIONS_ROW = "${NAME}_actions_row"
    const val LIKE_BUTTON = "${NAME}_like_button"
    const val LIKES_COUNT = "${NAME}_likes_count"
    const val SHARE_BUTTON = "${NAME}_share_button"
    const val TIER_HINT = "${NAME}_tier_hint"
}
