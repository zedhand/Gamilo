package com.gamilo.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.TaskEntity
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.FilterBarHost
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.JobPickerSection
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens

@Composable
fun TasksScreen(viewModel: TasksViewModel, filter: GlobalFilter, onFilterChange: (GlobalFilter) -> Unit) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val jobsById = remember(jobs) { jobs.associateBy { it.id } }
    var newTitle by remember { mutableStateOf("") }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(filter) { viewModel.setFilter(filter) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FilterBarHost(filter = filter, jobs = jobs, onFilterChange = onFilterChange)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GamiloColors.Background)
                    .padding(GamiloDimens.ScreenPadding)
                    .testTag("tasks_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    BorderedTextField(label = "New task", value = newTitle, onValueChange = { newTitle = it }, testTag = "task_title_input")
                }
                item { SectionLabel("Job") }
                item { JobPickerSection(jobs = jobs, selectedJobId = selectedJobId, onSelect = { selectedJobId = it }) }
                item {
                    GamiloButton(
                        label = "Add Task",
                        onClick = {
                            viewModel.addTask(newTitle, selectedJobId)
                            newTitle = ""
                        },
                    )
                }
                if (tasks.isEmpty()) {
                    item { Text(text = "No tasks yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp) }
                } else {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task,
                            jobTitle = task.jobId?.let { jobsById[it]?.title },
                            onToggle = { viewModel.toggleDone(task) },
                            onDelete = { viewModel.deleteTask(task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, jobTitle: String?, onToggle: () -> Unit, onDelete: () -> Unit) {
    ListRowCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.isDone) GamiloColors.TextSecondary else GamiloColors.TextPrimary,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    fontSize = 14.sp,
                )
                jobTitle?.let { Text(text = it, color = GamiloColors.TextSecondary, fontSize = 11.sp) }
            }
            Text(
                text = "DELETE",
                color = GamiloColors.TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = GamiloColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
