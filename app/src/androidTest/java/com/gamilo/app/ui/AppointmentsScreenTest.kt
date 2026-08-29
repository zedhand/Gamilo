package com.gamilo.app.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.entity.AppointmentEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.repo.AppointmentRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.ui.screens.appointments.AppointmentsScreen
import com.gamilo.app.ui.screens.appointments.AppointmentsViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Selecting an actual day/time from the real Material3 DatePicker/TimePicker isn't exercised
 * here — their internal calendar-grid semantics are fragile to target reliably and would add
 * flakiness disproportionate to what it proves. That interaction is verified manually on-device
 * (see PROGRESS.md), matching the project's Test Automation Limits for other system-styled
 * pickers (SAF file picker, biometric prompt). What's covered here instead: an appointment
 * already in the database renders correctly (including its linked job title) and can be
 * deleted, and the Add Appointment button's validation gate (no date selected yet) really does
 * nothing rather than silently creating a malformed row.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppointmentsScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appointmentLinkedToAJob_rendersWithJobTitleAndCanBeDeleted() {
        val jobDao = screenRule.database.jobDao()
        val jobId: Long
        val startAt = 1_788_000_000_000L
        val endAt = 1_788_003_600_000L
        runBlocking {
            jobId = jobDao.insert(
                JobEntity(clientName = "Jane Smith", title = "Replace faucet", status = JobStatus.ACTIVE, notes = "", createdAt = 1_000L, updatedAt = 1_000L, deletedAt = null),
            )
            screenRule.database.appointmentDao().insert(
                AppointmentEntity(
                    jobId = jobId, title = "Site visit", startAt = startAt, endAt = endAt,
                    location = "123 Main St", notes = "", createdAt = 1_000L, updatedAt = 1_000L, deletedAt = null,
                ),
            )
        }

        val viewModel = AppointmentsViewModel(
            AppointmentRepository(screenRule.database.appointmentDao(), SystemClock),
            JobRepository(jobDao, SystemClock),
        )
        composeRule.setContent { GamiloTheme { AppointmentsScreen(viewModel) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Site visit").assertExists()
        val zone = ZoneId.systemDefault()
        val startText = Instant.ofEpochMilli(startAt).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val endText = Instant.ofEpochMilli(endAt).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
        val expectedSubtitle = "$startText – $endText · Replace faucet · 123 Main St"

        // The still-visible JobPickerSection option row ("Replace faucet — Jane Smith") also
        // contains the substring "Replace faucet" — assert the exact row subtitle instead.
        val list = composeRule.onNodeWithTag("appointments_form_list")
        list.performScrollToNode(hasText(startText, substring = true))
        composeRule.onNodeWithText(expectedSubtitle).assertExists()

        list.performScrollToNode(hasText("DELETE"))
        composeRule.onNodeWithText("DELETE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Site visit").assertDoesNotExist()
    }

    @Test
    fun addAppointment_withoutSelectingADate_doesNothing() {
        val viewModel = AppointmentsViewModel(
            AppointmentRepository(screenRule.database.appointmentDao(), SystemClock),
            JobRepository(screenRule.database.jobDao(), SystemClock),
        )
        composeRule.setContent { GamiloTheme { AppointmentsScreen(viewModel) } }

        composeRule.onNodeWithTag("appointment_title_input").performTextInput("Site visit")
        val list = composeRule.onNodeWithTag("appointments_form_list")
        list.performScrollToNode(hasText("ADD APPOINTMENT"))
        composeRule.onNodeWithText("ADD APPOINTMENT").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No appointments yet.").assertExists()
    }
}
