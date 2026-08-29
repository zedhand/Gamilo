package com.gamilo.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.repo.AttachmentRepository
import com.gamilo.app.data.repo.ExpenseRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.ui.screens.expenses.ExpensesScreen
import com.gamilo.app.ui.screens.expenses.ExpensesViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The sticky global job/date filter, exercised against a real screen rather than in isolation. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class GlobalFilterScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingAJobInTheFilterBar_hidesEntriesForOtherJobs() {
        val jobDao = screenRule.database.jobDao()
        val jobAId: Long
        runBlocking {
            jobAId = jobDao.insert(
                JobEntity(clientName = "Jane Smith", title = "Replace faucet", status = JobStatus.ACTIVE, notes = "", createdAt = 1_000L, updatedAt = 1_000L, deletedAt = null),
            )
            jobDao.insert(
                JobEntity(clientName = "Bob Jones", title = "Fix drywall", status = JobStatus.ACTIVE, notes = "", createdAt = 1_000L, updatedAt = 1_000L, deletedAt = null),
            )
        }

        val viewModel = ExpensesViewModel(
            ExpenseRepository(screenRule.database.expenseDao(), SystemClock),
            AttachmentRepository(screenRule.database.attachmentDao(), SystemClock),
            screenRule.settingsStore,
            JobRepository(jobDao, SystemClock),
        )
        composeRule.setContent {
            GamiloTheme {
                var filter by remember { mutableStateOf(GlobalFilter()) }
                ExpensesScreen(viewModel, filter, onFilterChange = { filter = it })
            }
        }
        composeRule.waitForIdle()

        // Add one expense linked to "Replace faucet" and one linked to "Fix drywall".
        val list = composeRule.onNodeWithTag("expenses_form_list")
        composeRule.onNodeWithTag("expense_description_input").performTextInput("Faucet cartridge")
        composeRule.onNodeWithTag("expense_cost_input").performTextInput("14.50")
        list.performScrollToNode(hasText("Replace faucet", substring = true))
        composeRule.onNodeWithText("Replace faucet — Jane Smith").performClick()
        list.performScrollToNode(hasText("ADD EXPENSE"))
        composeRule.onNodeWithText("ADD EXPENSE").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("expense_description_input").performTextInput("Drywall patch")
        composeRule.onNodeWithTag("expense_cost_input").performTextInput("9.00")
        list.performScrollToNode(hasText("Fix drywall", substring = true))
        composeRule.onNodeWithText("Fix drywall — Bob Jones").performClick()
        list.performScrollToNode(hasText("ADD EXPENSE"))
        composeRule.onNodeWithText("ADD EXPENSE").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Faucet cartridge").assertExists()
        composeRule.onNodeWithText("Drywall patch").assertExists()

        // Filter down to just the "Replace faucet" job via the sticky filter bar.
        composeRule.onNodeWithTag("filter_job_chip").performClick()
        composeRule.onNodeWithText("REPLACE FAUCET (JANE SMITH)").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Faucet cartridge").assertExists()
        composeRule.onNodeWithText("Drywall patch").assertDoesNotExist()
    }
}
