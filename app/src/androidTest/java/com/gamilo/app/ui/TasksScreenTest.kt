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
import com.gamilo.app.data.repo.TaskRepository
import com.gamilo.app.ui.screens.tasks.TasksScreen
import com.gamilo.app.ui.screens.tasks.TasksViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addingATask_showsItInTheList_andDeletingRemovesIt() {
        val viewModel = TasksViewModel(TaskRepository(screenRule.database.taskDao(), SystemClock), JobRepository(screenRule.database.jobDao(), SystemClock))
        composeRule.setContent { GamiloTheme { TasksScreen(viewModel, GlobalFilter(), onFilterChange = {}) } }

        composeRule.onNodeWithTag("task_title_input").performTextInput("Buy lumber")
        val list = composeRule.onNodeWithTag("tasks_list")
        list.performScrollToNode(hasText("ADD TASK"))
        composeRule.onNodeWithText("ADD TASK").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Buy lumber").assertExists()

        composeRule.onNodeWithText("DELETE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Buy lumber").assertDoesNotExist()
    }
}
