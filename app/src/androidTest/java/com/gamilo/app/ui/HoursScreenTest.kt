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
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.ui.screens.hours.HoursScreen
import com.gamilo.app.ui.screens.hours.HoursViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HoursScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addingAManualEntry_showsInList() {
        val viewModel = HoursViewModel(
            HourRepository(screenRule.database.hourDao(), SystemClock),
            screenRule.settingsStore,
            JobRepository(screenRule.database.jobDao(), SystemClock),
        )
        composeRule.setContent { GamiloTheme { HoursScreen(viewModel, GlobalFilter(), onFilterChange = {}) } }
        composeRule.waitForIdle() // let the default-rate LaunchedEffect populate the rate field

        composeRule.onNodeWithTag("hours_input").performTextInput("2.5")
        val list = composeRule.onNodeWithTag("hours_form_list")
        list.performScrollToNode(hasText("ADD ENTRY"))
        composeRule.onNodeWithText("ADD ENTRY").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasText("@", substring = true)).assertExists()
    }
}
