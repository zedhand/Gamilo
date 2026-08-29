package com.gamilo.app.ui.screens.voicelog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.ai.JobVoiceExtractor
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens

/**
 * Full-screen voice-accelerator overlay: capture -> loading mask -> review -> explicit confirm.
 * Every field the extractor guessed is editable before saving, and nothing commits without the
 * user tapping "Confirm & Save".
 */
@Composable
fun VoiceLogSheet(viewModel: VoiceLogViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()

    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) { viewModel.startCapture() }
    LaunchedEffect(state) { if (state is VoiceLogState.Saved) onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background.copy(alpha = 0.96f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(GamiloDimens.ScreenPadding)
                .background(GamiloColors.Surface)
                .border(GamiloDimens.BorderWidth, GamiloColors.Border)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(GamiloDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val current = state) {
                VoiceLogState.Listening -> ListeningMask()
                is VoiceLogState.Error -> ErrorState(current.message, onDismiss)
                is VoiceLogState.Review -> ReviewForm(
                    draft = current.draft,
                    onConfirm = { clientName, title -> viewModel.confirm(clientName, title, current.draft.rawText) },
                    onCancel = onDismiss,
                )
                VoiceLogState.Idle, VoiceLogState.Saved -> {}
            }
        }
    }
}

@Composable
private fun ListeningMask() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = GamiloDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(color = GamiloColors.Accent)
        Text("LISTENING…", color = GamiloColors.Accent, fontWeight = FontWeight.Bold)
        Text(
            "Try: \"New job for Jane Smith to replace the kitchen faucet\"",
            color = GamiloColors.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("COULD NOT TRANSCRIBE", color = GamiloColors.Danger, fontWeight = FontWeight.Bold)
        Text(message, color = GamiloColors.TextSecondary, fontSize = 13.sp)
        GamiloButton(label = "Close", onClick = onDismiss)
    }
}

@Composable
private fun ReviewForm(draft: JobVoiceExtractor.Draft, onConfirm: (String, String) -> Unit, onCancel: () -> Unit) {
    var clientName by remember { mutableStateOf(draft.clientName ?: "") }
    var title by remember { mutableStateOf(draft.title ?: "") }

    Text("REVIEW BEFORE SAVING", color = GamiloColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Text("“${draft.rawText}”", color = GamiloColors.TextSecondary, fontSize = 13.sp)

    BorderedTextField(label = "Client name", value = clientName, onValueChange = { clientName = it }, testTag = "voice_client_input")
    BorderedTextField(label = "Job title", value = title, onValueChange = { title = it }, testTag = "voice_title_input")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GamiloButton(
            label = "Confirm & Save",
            onClick = {
                val trimmedClient = clientName.trim()
                val trimmedTitle = title.trim()
                if (trimmedClient.isNotBlank() && trimmedTitle.isNotBlank()) onConfirm(trimmedClient, trimmedTitle)
            },
            modifier = Modifier.testTag("voice_confirm_button"),
        )
        GamiloButton(label = "Cancel", onClick = onCancel)
    }
}
