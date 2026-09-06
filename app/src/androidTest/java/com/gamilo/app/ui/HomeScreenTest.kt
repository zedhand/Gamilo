package com.gamilo.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.TaskRepository
import com.gamilo.app.ui.screens.home.HomeScreen
import com.gamilo.app.ui.screens.home.HomeViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingStartShift_flipsButtonToEndShift() {
        val viewModel = HomeViewModel(
            JobRepository(screenRule.database.jobDao(), SystemClock),
            TaskRepository(screenRule.database.taskDao(), SystemClock),
            HourRepository(screenRule.database.hourDao(), SystemClock),
            screenRule.settingsStore,
        )

        composeRule.setContent { GamiloTheme { HomeScreen(viewModel, isVoiceLogEligible = false, onStartVoiceLog = {}) } }

        composeRule.onNodeWithText("START SHIFT").assertExists()
        composeRule.onNodeWithText("START SHIFT").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("END SHIFT").assertExists()
    }
}
