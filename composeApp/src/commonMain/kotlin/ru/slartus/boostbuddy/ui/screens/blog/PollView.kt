package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.linkColor
import ru.slartus.boostbuddy.ui.common.HorizontalSpacer
import ru.slartus.boostbuddy.ui.common.VerticalSpacer

@Composable
internal fun PollView(poll: Poll, onOptionClick: (Poll, PollOption) -> Unit) {
    Column(
        Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        PollTitleView(poll.titleText)
        VerticalSpacer(12.dp)
        PollOptionsView(poll, onOptionClick = { option ->
            onOptionClick(poll, option)
        })
        VerticalSpacer(12.dp)
        PollCounterView(poll.counter)
    }
}

@Composable
private fun PollCounterView(counter: Int) {
    FocusableBox {
        Text(
            modifier = Modifier.fillMaxWidth().focusable(),
            text = "Проголосовал: $counter",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PollTitleView(text: String) {
    FocusableBox {
        Text(
            modifier = Modifier.fillMaxWidth().focusable(),
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PollOptionsView(poll: Poll, onOptionClick: (PollOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        poll.options.forEach { option ->
            PollOptionView(
                option = option,
                allowVote = !poll.isFinished,
                voted = option.id in poll.answer,
                onClick = {
                    onOptionClick(option)
                }
            )
        }
    }
}

@Composable
private fun PollOptionView(
    option: PollOption,
    allowVote: Boolean,
    voted: Boolean,
    onClick: () -> Unit
) {
    FocusableBox {
        Box(
            Modifier
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(enabled = allowVote, onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(4.dp),
                verticalAlignment = CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = option.text,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
                HorizontalSpacer(4.dp)
                if (voted) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Default.Check,
                        tint = linkColor,
                        contentDescription = "empty avatar"
                    )
                }
                Text(
                    text = option.fractionText,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Box(
                modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .height(2.dp)
                    .background(linkColor)
                    .fillMaxWidth(option.fraction / 100f)
            )
        }
    }
}