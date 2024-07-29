package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
internal fun PollView(
    poll: Poll,
    onOptionClick: (Poll, PollOption) -> Unit,
    onVoteClick: () -> Unit,
    onDeleteVoteClick: () -> Unit
) {
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
        if (poll.isFinished)
            PollFinishedView()
        VerticalSpacer(12.dp)
        PollOptionsView(poll, onOptionClick = { option ->
            onOptionClick(poll, option)
        })

        if (poll.isMultiple && !poll.isFinished) {
            VerticalSpacer(12.dp)
            PollMultipleButton(
                poll = poll,
                onVoteClick = onVoteClick,
                onDeleteVoteClick = onDeleteVoteClick
            )
        }

        VerticalSpacer(12.dp)
        PollCounterView(poll.counter)
    }
}

@Composable
private fun PollMultipleButton(
    poll: Poll,
    onVoteClick: () -> Unit,
    onDeleteVoteClick: () -> Unit
) {
    if (poll.answer.isEmpty())
        Button(onClick = onVoteClick, enabled = poll.checked.isNotEmpty()) {
            Text("Проголосовать")
        }
    else
        Button(onClick = onDeleteVoteClick) {
            Text("Отозвать голос")
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
private fun PollFinishedView() {
    FocusableBox {
        Text(
            modifier = Modifier.fillMaxWidth().focusable(),
            text = "Опрос завершен",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PollOptionsView(poll: Poll, onOptionClick: (PollOption) -> Unit) {
    val isMultipleUnVoted =
        remember(poll) { poll.isMultiple && !poll.isFinished && poll.answer.isEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        poll.options.forEach { option ->
            if (isMultipleUnVoted) {
                PollOptionCheckableView(
                    option = option,
                    checked = option.id in poll.checked,
                    onClick = {
                        onOptionClick(option)
                    }
                )
            } else {
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
}

@Composable
private fun PollOptionContainer(
    fraction: Float,
    onClick: (() -> Unit)? = null,
    block: @Composable BoxScope.() -> Unit
) {
    FocusableBox {
        Box(
            Modifier
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .then(
                    if (onClick != null) Modifier.clickable { onClick() }
                    else Modifier
                )
        ) {
            block()
            Box(
                modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .height(2.dp)
                    .background(linkColor)
                    .fillMaxWidth(fraction)
            )
        }
    }
}

@Composable
private fun PollOptionCheckableView(
    option: PollOption,
    checked: Boolean,
    onClick: () -> Unit
) {
    PollOptionContainer(
        fraction = option.fraction / 100f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(end = 8.dp),
            verticalAlignment = CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    onClick()
                },
            )
            Text(
                modifier = Modifier.weight(1f),
                text = option.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalSpacer(4.dp)
            Text(
                text = option.fractionText,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
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
    PollOptionContainer(
        fraction = option.fraction / 100f,
        onClick = if (allowVote) onClick else null
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp),
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
    }

}