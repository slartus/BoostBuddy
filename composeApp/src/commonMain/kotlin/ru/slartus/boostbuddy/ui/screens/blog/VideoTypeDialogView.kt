package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.PostData


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoTypeDialogView(
    postData: PostData.OkVideo,
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
        Column {
            postData.playerUrls.filter { it.url.isNotEmpty() }.forEach {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onItemClicked(it) }
                        .padding(16.dp),
                    text = it.type
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}