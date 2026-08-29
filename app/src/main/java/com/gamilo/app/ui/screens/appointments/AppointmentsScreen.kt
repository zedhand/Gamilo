package com.gamilo.app.ui.screens.appointments

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import com.gamilo.app.data.entity.AppointmentEntity
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.JobPickerSection
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.MonospaceNumeric
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(viewModel: AppointmentsViewModel) {
    val appointments by viewModel.appointments.collectAsState(initial = emptyList())
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val jobsById = remember(jobs) { jobs.associateBy { it.id } }

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf("1") }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }
    var selectedDateMillisUtc by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background)
            .padding(GamiloDimens.ScreenPadding)
            .testTag("appointments_form_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BorderedTextField(label = "Title", value = title, onValueChange = { title = it }, testTag = "appointment_title_input")
        }
        item { SectionLabel("Date & Time") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamiloButton(
                    label = selectedDateMillisUtc?.let { formatUtcMidnightAsDate(it) } ?: "Select Date",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).testTag("appointment_date_button"),
                )
                GamiloButton(
                    label = formatHourMinute(selectedHour, selectedMinute),
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f).testTag("appointment_time_button"),
                )
            }
        }
        item {
            BorderedTextField(
                label = "Duration (hours)",
                value = durationHours,
                onValueChange = { durationHours = it },
                isNumeric = true,
                testTag = "appointment_duration_input",
            )
        }
        item { BorderedTextField(label = "Location (optional)", value = location, onValueChange = { location = it }) }
        item { BorderedTextField(label = "Notes (optional)", value = notes, onValueChange = { notes = it }) }
        item { SectionLabel("Job") }
        item { JobPickerSection(jobs = jobs, selectedJobId = selectedJobId, onSelect = { selectedJobId = it }) }
        item {
            GamiloButton(
                label = "Add Appointment",
                onClick = {
                    val dateMillisUtc = selectedDateMillisUtc
                    val duration = durationHours.toBigDecimalOrNull()
                    if (title.isNotBlank() && dateMillisUtc != null && duration != null && duration > BigDecimal.ZERO) {
                        val startAt = combineDateAndTime(dateMillisUtc, selectedHour, selectedMinute)
                        val endAt = startAt + duration.multiply(BigDecimal(3_600_000)).toLong()
                        viewModel.addAppointment(title, startAt, endAt, selectedJobId, location, notes)
                        title = ""
                        location = ""
                        notes = ""
                        selectedJobId = null
                        selectedDateMillisUtc = null
                        durationHours = "1"
                    }
                },
                modifier = Modifier.testTag("appointment_add_button"),
            )
        }

        if (appointments.isEmpty()) {
            item { Text(text = "No appointments yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp) }
        } else {
            items(appointments, key = { it.id }) { appointment ->
                AppointmentRow(
                    appointment,
                    jobTitle = appointment.jobId?.let { jobsById[it]?.title },
                    onDelete = { viewModel.deleteAppointment(appointment.id) },
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillisUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { selectedDateMillisUtc = state.selectedDateMillis; showDatePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { selectedHour = state.hour; selectedMinute = state.minute; showTimePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

/** [utcMidnightMillis] is a DatePicker selection: the start of the chosen day in UTC. */
private fun combineDateAndTime(utcMidnightMillis: Long, hour: Int, minute: Int): Long {
    val date = Instant.ofEpochMilli(utcMidnightMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun formatUtcMidnightAsDate(utcMidnightMillis: Long): String {
    val date = Instant.ofEpochMilli(utcMidnightMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}

private fun formatHourMinute(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when (hour % 12) { 0 -> 12; else -> hour % 12 }
    return "%d:%02d %s".format(hour12, minute, amPm)
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = GamiloColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
private fun AppointmentRow(appointment: AppointmentEntity, jobTitle: String?, onDelete: () -> Unit) {
    ListRowCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appointment.title, color = GamiloColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = formatStartEnd(appointment.startAt, appointment.endAt) + (jobTitle?.let { " · $it" } ?: "") +
                        (appointment.location.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    color = GamiloColors.TextSecondary,
                    fontFamily = MonospaceNumeric,
                    fontSize = 12.sp,
                )
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

private fun formatStartEnd(startAt: Long, endAt: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startAt).atZone(zone).format(formatter)
    val end = Instant.ofEpochMilli(endAt).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    return "$start – $end"
}
