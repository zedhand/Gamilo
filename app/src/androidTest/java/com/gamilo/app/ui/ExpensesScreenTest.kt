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

@RunWith(AndroidJUnit4::class)
@LargeTest
class ExpensesScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addingAnExpense_showsInList() {
        val viewModel = ExpensesViewModel(
            ExpenseRepository(screenRule.database.expenseDao(), SystemClock),
            AttachmentRepository(screenRule.database.attachmentDao(), SystemClock),
            screenRule.settingsStore,
            JobRepository(screenRule.database.jobDao(), SystemClock),
        )
        composeRule.setContent { GamiloTheme { ExpensesScreen(viewModel, GlobalFilter(), onFilterChange = {}) } }

        composeRule.onNodeWithTag("expense_description_input").performTextInput("2x4 lumber")
        composeRule.onNodeWithTag("expense_cost_input").performTextInput("19.99")
        val list = composeRule.onNodeWithTag("expenses_form_list")
        list.performScrollToNode(hasText("ADD EXPENSE"))
        composeRule.onNodeWithText("ADD EXPENSE").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("2x4 lumber").assertExists()
    }

    @Test
    fun selectingAJob_linksTheExpenseToIt() {
        runBlocking {
            screenRule.database.jobDao().insert(
                JobEntity(
                    clientName = "Jane Smith",
                    title = "Replace faucet",
                    status = JobStatus.ACTIVE,
                    notes = "",
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    deletedAt = null,
                ),
            )
        }
        val viewModel = ExpensesViewModel(
            ExpenseRepository(screenRule.database.expenseDao(), SystemClock),
            AttachmentRepository(screenRule.database.attachmentDao(), SystemClock),
            screenRule.settingsStore,
            JobRepository(screenRule.database.jobDao(), SystemClock),
        )
        composeRule.setContent { GamiloTheme { ExpensesScreen(viewModel, GlobalFilter(), onFilterChange = {}) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("expense_description_input").performTextInput("Faucet cartridge")
        composeRule.onNodeWithTag("expense_cost_input").performTextInput("14.50")
        val list = composeRule.onNodeWithTag("expenses_form_list")
        list.performScrollToNode(hasText("Replace faucet", substring = true))
        composeRule.onNodeWithText("Replace faucet — Jane Smith").performClick()
        list.performScrollToNode(hasText("ADD EXPENSE"))
        composeRule.onNodeWithText("ADD EXPENSE").performClick()
        composeRule.waitForIdle()

        list.performScrollToNode(hasText("Faucet cartridge", substring = true))
        composeRule.onNodeWithText("Faucet cartridge").assertExists()
        // The row's subtitle includes the linked job's title if jobId round-tripped correctly.
        // ("Replace faucet" alone would also match the still-visible job picker option row.)
        composeRule.onNodeWithText("14.50 CAD · Replace faucet").assertExists()
    }
}
