package com.gamilo.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.core.DateRangePreset
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.core.JobFilterOption
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens

private fun JobFilterOption.label(jobs: List<JobEntity>): String = when (this) {
    JobFilterOption.All -> "All"
    JobFilterOption.Unassigned -> "Unassigned"
    is JobFilterOption.Specific -> jobs.firstOrNull { it.id == jobId }?.title ?: "Job #$jobId"
}

@Composable
private fun FilterChip(label: String, value: String, testTag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .testTag(testTag)
            .background(GamiloColors.Background)
            .border(GamiloDimens.BorderWidth, GamiloColors.Border)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = GamiloColors.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value.uppercase(), color = GamiloColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PickerScaffold(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background.copy(alpha = 0.94f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .padding(GamiloDimens.ScreenPadding)
                .background(GamiloColors.Surface)
                .border(GamiloDimens.BorderWidth, GamiloColors.Border)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        ) {
            Text(
                title.uppercase(),
                color = GamiloColors.Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(GamiloDimens.ScreenPadding),
            )
            content()
        }
    }
}

@Composable
private fun PickerRow(label: String, testTag: String, onClick: () -> Unit) {
    Text(
        text = label.uppercase(),
        color = GamiloColors.TextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = GamiloDimens.ScreenPadding, vertical = 14.dp),
    )
}

/**
 * Every data-heavy tab (Tasks, Hours, Expenses, Mileage, Shipping) embeds one of these right
 * below its header instead of duplicating the picker-overlay plumbing — [onFilterChange]
 * reports back into the app-wide [GlobalFilter] state hoisted in MainActivity, so all tabs stay
 * in sync ("sticky": switching tabs doesn't reset it).
 */
@Composable
fun FilterBarHost(
    filter: GlobalFilter,
    jobs: List<JobEntity>,
    onFilterChange: (GlobalFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showJobPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GamiloColors.Surface)
            .border(GamiloDimens.BorderWidth, GamiloColors.Border)
            .padding(horizontal = GamiloDimens.ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(label = "RANGE", value = filter.dateRangePreset.label, testTag = "filter_range_chip", onClick = { showDatePicker = true })
        FilterChip(label = "JOB", value = filter.jobFilter.label(jobs), testTag = "filter_job_chip", onClick = { showJobPicker = true })
    }

    if (showDatePicker) {
        PickerScaffold("Date Range", onDismiss = { showDatePicker = false }) {
            LazyColumn {
                items(DateRangePreset.entries) { preset ->
                    PickerRow(preset.label, testTag = "filter_range_option_${preset.name}") {
                        onFilterChange(filter.copy(dateRangePreset = preset))
                        showDatePicker = false
                    }
                }
            }
        }
    }
    if (showJobPicker) {
        PickerScaffold("Filter by Job", onDismiss = { showJobPicker = false }) {
            LazyColumn {
                item {
                    PickerRow("All jobs", testTag = "filter_job_option_all") {
                        onFilterChange(filter.copy(jobFilter = JobFilterOption.All))
                        showJobPicker = false
                    }
                }
                item {
                    PickerRow("Unassigned", testTag = "filter_job_option_unassigned") {
                        onFilterChange(filter.copy(jobFilter = JobFilterOption.Unassigned))
                        showJobPicker = false
                    }
                }
                items(jobs, key = { it.id }) { job ->
                    PickerRow("${job.title} (${job.clientName})", testTag = "filter_job_option_${job.id}") {
                        onFilterChange(filter.copy(jobFilter = JobFilterOption.Specific(job.id)))
                        showJobPicker = false
                    }
                }
            }
        }
    }
}
