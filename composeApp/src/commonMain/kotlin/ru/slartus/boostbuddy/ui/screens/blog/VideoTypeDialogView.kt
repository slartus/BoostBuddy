package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.blog.VideoTypeComponent
import ru.slartus.boostbuddy.components.blog.text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoTypeDialogView(
    component: VideoTypeComponent,
    modifier: Modifier = Modifier
) {
    val state by component.viewStates.subscribeAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        modifier = modifier.navigationBarsPadding(),
        onDismissRequest = { component.onDismissClicked() },
        sheetState = sheetState
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            state.postData.playerUrls.filter { it.url.isNotEmpty() }.forEach {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { component.onItemClicked(it) }
                        .padding(16.dp),
                    text = it.quality.text
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    component.onUseSystemPlayerClicked(!state.useSystemPlayer)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.useSystemPlayer,
                    onCheckedChange = { newCheckedState ->
                        component.onUseSystemPlayerClicked(newCheckedState)
                    },
                )
                Text(text = "Системный видео-плеер")
            }
        }
    }
}