package com.gamilo.app.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gamilo.app.backup.BackupManager
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.repo.AppointmentRepository
import com.gamilo.app.data.repo.AttachmentRepository
import com.gamilo.app.data.repo.ExpenseRepository
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.MileageRepository
import com.gamilo.app.data.repo.ShippingRepository
import com.gamilo.app.data.repo.TaskRepository
import com.gamilo.app.export.DataExportService
import com.gamilo.app.ui.screens.settings.SettingsScreen
import com.gamilo.app.ui.screens.settings.SettingsViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Stops short of actually tapping "Confirm Wipe": a successful factory reset calls
     * Runtime.getRuntime().exit(0) to force a clean restart, which would kill this
     * instrumentation process — the same Test Automation Limit as the biometric-gated cold
     * start prompt and the camera/OCR flow. This proves the two gates (a fresh re-auth, then
     * an exact "DELETE" match) actually gate the destructive button; the wipe itself is
     * verified manually on-device (see PROGRESS.md).
     */
    @Test
    fun dangerZone_requiresFreshAuthThenExactTypedConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = screenRule.database
        val dataExportService = DataExportService(
            JobRepository(db.jobDao(), SystemClock),
            TaskRepository(db.taskDao(), SystemClock),
            HourRepository(db.hourDao(), SystemClock),
            ExpenseRepository(db.expenseDao(), SystemClock),
            MileageRepository(db.mileageDao(), SystemClock),
            ShippingRepository(db.shippingDao(), SystemClock),
            AttachmentRepository(db.attachmentDao(), SystemClock),
            AppointmentRepository(db.appointmentDao(), SystemClock),
        )
        val viewModel = SettingsViewModel(screenRule.settingsStore, BackupManager(context, SystemClock), db, dataExportService)
        var authRequests = 0
        composeRule.setContent {
            GamiloTheme {
                SettingsScreen(
                    viewModel,
                    onRequestFactoryResetAuth = { onAuthenticated ->
                        authRequests++
                        onAuthenticated()
                    },
                )
            }
        }

        val list = composeRule.onNodeWithTag("settings_list")
        list.performScrollToNode(hasText("FACTORY RESET", substring = true))
        composeRule.onNodeWithText("FACTORY RESET / WIPE ALL DATA").performClick()
        composeRule.waitForIdle()

        assertEquals(1, authRequests)

        list.performScrollToNode(hasTestTag("danger_zone_confirm_input"))
        composeRule.onNodeWithText("CONFIRM WIPE").assertIsNotEnabled()

        composeRule.onNodeWithTag("danger_zone_confirm_input").performTextInput("WRONG")
        composeRule.onNodeWithText("CONFIRM WIPE").assertIsNotEnabled()

        composeRule.onNodeWithTag("danger_zone_confirm_input").performTextClearance()
        composeRule.onNodeWithTag("danger_zone_confirm_input").performTextInput("DELETE")
        composeRule.onNodeWithText("CONFIRM WIPE").assertIsEnabled()
    }
}
