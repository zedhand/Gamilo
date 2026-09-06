package com.gamilo.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.timer.ShiftTimerController
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.MonospaceNumeric

@Composable
fun HomeScreen(viewModel: HomeViewModel, isVoiceLogEligible: Boolean, onStartVoiceLog: () -> Unit) {
    val activeJobs by viewModel.activeJobs.collectAsState(initial = emptyList())
    val openTasksCount by viewModel.openTasksCount.collectAsState(initial = 0)
    val openSession by viewModel.openSession.collectAsState(initial = null)

    // Driven off the DB-backed open session (not the button click) so the persistent
    // notification is also correct after a process restart with an already-active shift,
    // not just the in-session start/end tap.
    val context = LocalContext.current
    val timerController = remember { ShiftTimerController(context) }
    LaunchedEffect(openSession?.id) {
        val session = openSession
        if (session != null) timerController.start(session.startedAt) else timerController.stop()
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartVoiceLog()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background)
            .padding(GamiloDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GamiloButton(
                label = if (openSession == null) "Start Shift" else "End Shift",
                onClick = viewModel::toggleShift,
                tall = true,
            )
        }
        if (isVoiceLogEligible) {
            item {
                GamiloButton(
                    label = "Voice Log",
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) onStartVoiceLog() else recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
            }
        }
        openSession?.let { session ->
            item {
                Text(
                    text = "SHIFT STARTED AT ${DateFormat.format("HH:mm", session.startedAt)}",
                    color = GamiloColors.TextSecondary,
                    fontFamily = MonospaceNumeric,
                    fontSize = 13.sp,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(label = "Active Jobs", value = activeJobs.size, modifier = Modifier.weight(1f))
                StatTile(label = "Open Tasks", value = openTasksCount, modifier = Modifier.weight(1f))
            }
        }

        item {
            Text(text = "ACTIVE JOBS", color = GamiloColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        if (activeJobs.isEmpty()) {
            item {
                Text(text = "No active jobs yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            items(activeJobs, key = { it.id }) { job -> JobRow(job) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(GamiloColors.Surface)
            .border(GamiloDimens.BorderWidth, GamiloColors.Border)
            .padding(12.dp),
    ) {
        Text(text = value.toString(), color = GamiloColors.Accent, fontFamily = MonospaceNumeric, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(text = label.uppercase(), color = GamiloColors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun JobRow(job: JobEntity) {
    ListRowCard {
        Text(text = job.title, color = GamiloColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(text = job.clientName, color = GamiloColors.TextSecondary, fontSize = 12.sp)
    }
}
