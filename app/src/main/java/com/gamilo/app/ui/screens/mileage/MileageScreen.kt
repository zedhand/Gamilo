package com.gamilo.app.ui.screens.mileage

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
import com.gamilo.app.data.entity.MileageEntity
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.FilterBarHost
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.JobPickerSection
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.MonospaceNumeric
import java.math.BigDecimal

@Composable
fun MileageScreen(viewModel: MileageViewModel, filter: GlobalFilter, onFilterChange: (GlobalFilter) -> Unit) {
    val settings by viewModel.settings.collectAsState(initial = null)
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val jobsById = remember(jobs) { jobs.associateBy { it.id } }
    val entries = viewModel.entries.collectAsLazyPagingItems()

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("") }
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
                    .testTag("mileage_form_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { BorderedTextField(label = "Origin", value = origin, onValueChange = { origin = it }, testTag = "mileage_origin_input") }
                item { BorderedTextField(label = "Destination", value = destination, onValueChange = { destination = it }, testTag = "mileage_destination_input") }
                item {
                    BorderedTextField(
                        label = "Distance (km)",
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        isNumeric = true,
                        testTag = "mileage_distance_input",
                    )
                }
                item {
                    Text(
                        text = "Rate: ${settings?.defaultMileageRatePerKm ?: "—"} ${settings?.baseCurrencyCode ?: ""}/km",
                        color = GamiloColors.TextSecondary,
                        fontFamily = MonospaceNumeric,
                        fontSize = 12.sp,
                    )
                }
                item { SectionLabel("Job") }
                item { JobPickerSection(jobs = jobs, selectedJobId = selectedJobId, onSelect = { selectedJobId = it }) }
                item {
                    GamiloButton(
                        label = "Add Trip",
                        onClick = {
                            val distance = distanceText.toBigDecimalOrNull()
                            if (origin.isNotBlank() && destination.isNotBlank() && distance != null) {
                                viewModel.addTrip(origin, destination, distance, selectedJobId)
                                origin = ""
                                destination = ""
                                distanceText = ""
                                selectedJobId = null
                            }
                        },
                    )
                }

                if (entries.itemCount == 0) {
                    item { Text(text = "No trips logged yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp) }
                } else {
                    items(count = entries.itemCount, key = entries.itemKey { it.id }) { index ->
                        entries[index]?.let { trip ->
                            MileageRow(trip, jobTitle = trip.jobId?.let { jobsById[it]?.title }, onDelete = { viewModel.deleteTrip(trip.id) })
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
private fun MileageRow(trip: MileageEntity, jobTitle: String?, onDelete: () -> Unit) {
    ListRowCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "${trip.originLabel} -> ${trip.destinationLabel}", color = GamiloColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "${trip.distanceKm} km @ ${trip.mileageRateApplied}" + (jobTitle?.let { " · $it" } ?: ""),
                    color = GamiloColors.TextSecondary,
                    fontFamily = MonospaceNumeric,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "DELETE",
                color = GamiloColors.TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
            )
        }
    }
}
