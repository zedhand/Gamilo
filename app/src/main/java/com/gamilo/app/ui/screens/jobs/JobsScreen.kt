package com.gamilo.app.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens

@Composable
fun JobsScreen(viewModel: JobsViewModel) {
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())

    var clientName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background)
            .padding(GamiloDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BorderedTextField(label = "Client name", value = clientName, onValueChange = { clientName = it }, testTag = "job_client_input") }
        item { BorderedTextField(label = "Job title", value = title, onValueChange = { title = it }, testTag = "job_title_input") }
        item { BorderedTextField(label = "Notes (optional)", value = notes, onValueChange = { notes = it }) }
        item {
            GamiloButton(
                label = "Add Job",
                onClick = {
                    if (clientName.isNotBlank() && title.isNotBlank()) {
                        viewModel.addJob(clientName, title, notes)
                        clientName = ""
                        title = ""
                        notes = ""
                    }
                },
            )
        }

        if (jobs.isEmpty()) {
            item { Text(text = "No jobs yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp) }
        } else {
            items(jobs, key = { it.id }) { job ->
                JobRow(
                    job = job,
                    onSetStatus = { viewModel.setStatus(job, it) },
                    onDelete = { viewModel.deleteJob(job.id) },
                )
            }
        }
    }
}

@Composable
private fun JobRow(job: JobEntity, onSetStatus: (JobStatus) -> Unit, onDelete: () -> Unit) {
    ListRowCard {
        Text(text = job.title, color = GamiloColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(text = job.clientName, color = GamiloColors.TextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JobStatus.entries.forEach { status ->
                StatusChip(status = status, selected = job.status == status, onClick = { onSetStatus(status) })
            }
        }
        Text(
            text = "DELETE",
            color = GamiloColors.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
        )
    }
}

@Composable
private fun StatusChip(status: JobStatus, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = status.name.replace('_', ' '),
        color = if (selected) GamiloColors.Accent else GamiloColors.TextSecondary,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 11.sp,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    )
}
