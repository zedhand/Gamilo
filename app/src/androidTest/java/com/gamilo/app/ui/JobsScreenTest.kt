package com.gamilo.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.ui.screens.jobs.JobsScreen
import com.gamilo.app.ui.screens.jobs.JobsViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class JobsScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addingAJob_showsInListAndCanChangeStatus() {
        val viewModel = JobsViewModel(JobRepository(screenRule.database.jobDao(), SystemClock))
        composeRule.setContent { GamiloTheme { JobsScreen(viewModel) } }

        composeRule.onNodeWithTag("job_client_input").performTextInput("Jane Smith")
        composeRule.onNodeWithTag("job_title_input").performTextInput("Replace faucet")
        composeRule.onNodeWithText("ADD JOB").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Replace faucet").assertExists()
        composeRule.onNodeWithText("Jane Smith").assertExists()

        composeRule.onNodeWithText("COMPLETED").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Replace faucet").assertExists()
    }
}
