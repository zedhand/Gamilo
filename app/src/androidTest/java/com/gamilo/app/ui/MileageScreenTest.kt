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
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.MileageRepository
import com.gamilo.app.ui.screens.mileage.MileageScreen
import com.gamilo.app.ui.screens.mileage.MileageViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MileageScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addingATrip_showsInList() {
        val viewModel = MileageViewModel(
            MileageRepository(screenRule.database.mileageDao(), SystemClock),
            screenRule.settingsStore,
            JobRepository(screenRule.database.jobDao(), SystemClock),
        )
        composeRule.setContent { GamiloTheme { MileageScreen(viewModel, GlobalFilter(), onFilterChange = {}) } }

        composeRule.onNodeWithTag("mileage_origin_input").performTextInput("Shop")
        composeRule.onNodeWithTag("mileage_destination_input").performTextInput("Client site")
        composeRule.onNodeWithTag("mileage_distance_input").performTextInput("12.5")
        val list = composeRule.onNodeWithTag("mileage_form_list")
        list.performScrollToNode(hasText("ADD TRIP"))
        composeRule.onNodeWithText("ADD TRIP").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Shop -> Client site").assertExists()
    }
}
