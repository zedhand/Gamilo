package com.gamilo.app.ui.screens.hours

import android.text.format.DateFormat
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.HourEntity
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.FilterBarHost
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.JobPickerSection
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.MonospaceNumeric
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

@Composable
fun HoursScreen(viewModel: HoursViewModel, filter: GlobalFilter, onFilterChange: (GlobalFilter) -> Unit) {
    val settings by viewModel.settings.collectAsState(initial = null)
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val jobsById = remember(jobs) { jobs.associateBy { it.id } }
    val entries = viewModel.entries.collectAsLazyPagingItems()

    var hoursText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var rateInitialized by remember { mutableStateOf(false) }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(settings) {
        val s = settings
        if (s != null && !rateInitialized) {
            rateText = s.defaultHourlyRate.toPlainString()
            rateInitialized = true
        }
    }
    LaunchedEffect(filter) { viewModel.setFilter(filter) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FilterBarHost(filter = filter, jobs = jobs, onFilterChange = onFilterChange)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GamiloColors.Background)
                    .padding(GamiloDimens.ScreenPadding)
                    .testTag("hours_form_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    BorderedTextField(
                        label = "Hours worked",
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        isNumeric = true,
                        testTag = "hours_input",
                    )
                }
                item {
                    BorderedTextField(
                        label = "Hourly rate (${settings?.baseCurrencyCode ?: "CAD"})",
                        value = rateText,
                        onValueChange = { rateText = it },
                        isNumeric = true,
                        testTag = "hourly_rate_input",
                    )
                }
                item { SectionLabel("Job") }
                item { JobPickerSection(jobs = jobs, selectedJobId = selectedJobId, onSelect = { selectedJobId = it }) }
                item {
                    GamiloButton(
                        label = "Add Entry",
                        onClick = {
                            val hours = hoursText.toBigDecimalOrNull()
                            val rate = rateText.toBigDecimalOrNull()
                            if (hours != null && hours > BigDecimal.ZERO && rate != null) {
                                viewModel.addManualEntry(hours, rate, selectedJobId)
                                hoursText = ""
                                selectedJobId = null
                            }
                        },
                    )
                }

                if (entries.itemCount == 0) {
                    item { Text(text = "No hours logged yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp) }
                } else {
                    items(count = entries.itemCount, key = entries.itemKey { it.id }) { index ->
                        entries[index]?.let { entry ->
                            HourRow(entry, jobTitle = entry.jobId?.let { jobsById[it]?.title }, onDelete = { viewModel.deleteEntry(entry.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = GamiloColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
private fun HourRow(entry: HourEntity, jobTitle: String?, onDelete: () -> Unit) {
    ListRowCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val durationHours = if (entry.endedAt != null) {
                BigDecimal(entry.endedAt - entry.startedAt)
                    .divide(BigDecimal(TimeUnit.HOURS.toMillis(1)), 2, RoundingMode.HALF_UP)
            } else {
                null
            }
            Column {
                Text(text = DateFormat.format("yyyy-MM-dd", entry.startedAt).toString(), color = GamiloColors.TextPrimary, fontSize = 13.sp)
                Text(
                    text = (if (durationHours != null) "${durationHours}h @ ${entry.hourlyRate} ${entry.currencyCode}" else "IN PROGRESS") +
                        (jobTitle?.let { " · $it" } ?: ""),
                    color = GamiloColors.TextSecondary,
                    fontFamily = MonospaceNumeric,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "DELETE",
                color = GamiloColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
            )
        }
    }
}
