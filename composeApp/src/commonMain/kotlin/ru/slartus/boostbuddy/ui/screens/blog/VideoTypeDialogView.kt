package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.components.blog.text
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoTypeDialogView(
    postData: Content.OkVideo,
    onItemClicked: (PlayerUrl) -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        modifier = modifier.navigationBarsPadding(),
        onDismissRequest = { onDismissClicked() },
        sheetState = sheetState
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            postData.playerUrls.filter { it.url.isNotEmpty() }.forEach {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onItemClicked(it) }
                        .padding(16.dp),
                    text = it.quality.text
                )
            }
        }
    }
}