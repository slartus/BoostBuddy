package ru.slartus.boostbuddy.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import ru.slartus.boostbuddy.ui.theme.LightTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QrDialog(title: String, url: String, onDismiss: () -> Unit) {
    var isDialogOpen by remember { mutableStateOf(false) }

    val platformConfiguration = LocalPlatformConfiguration.current
    if (isDialogOpen) {
        BasicAlertDialog(
            onDismissRequest = {
                isDialogOpen = false
                onDismiss()
            }
        ) {
            Box(
                Modifier
                    .widthIn(max = 200.dp)
                    .wrapContentHeight()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                LightTheme {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = CenterHorizontally
                    ) {
                        Text(
                            modifier = Modifier.align(CenterHorizontally),
                            text = title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        VerticalSpacer(16.dp)
                        Text(
                            modifier = Modifier.fillMaxWidth().clickable {
                                platformConfiguration.openBrowser(url)
                            },
                            text = url,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodySmall
                        )
                        VerticalSpacer(16.dp)
                        val qrcodePainter = rememberQrCodePainter(url)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Image(
                                modifier = Modifier.fillMaxWidth(),
                                painter = qrcodePainter,
                                contentScale = ContentScale.FillWidth,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        isDialogOpen = true
    }
}