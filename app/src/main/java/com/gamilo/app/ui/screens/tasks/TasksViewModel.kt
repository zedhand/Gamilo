package com.gamilo.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamilo.app.core.DateRangePreset
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.entity.TaskEntity
import com.gamilo.app.data.model.TaskPriority
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TasksViewModel(private val taskRepository: TaskRepository, private val jobRepository: JobRepository) : ViewModel() {

    private val filterFlow = MutableStateFlow(GlobalFilter())
    fun setFilter(filter: GlobalFilter) { filterFlow.value = filter }

    val jobs: Flow<List<JobEntity>> = jobRepository.observeActive()

    /**
     * Date-range filtering applies to [TaskEntity.dueAt], which is nullable — a task with no
     * due date only matches the ALL_TIME preset, never a specific range, since there's no date
     * to test against.
     */
    val tasks: Flow<List<TaskEntity>> = combine(taskRepository.observeAll(), filterFlow) { tasks, filter ->
        val range = filter.dateRange()
        tasks.filter { t ->
            filter.jobFilter.matches(t.jobId) && (t.dueAt?.let { it in range } ?: (filter.dateRangePreset == DateRangePreset.ALL_TIME))
        }
    }

    fun addTask(title: String, jobId: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            taskRepository.create(
                TaskEntity(
                    jobId = jobId,
                    title = trimmed,
                    notes = "",
                    priority = TaskPriority.NORMAL,
                    isDone = false,
                    doneAt = null,
                    dueAt = null,
                    createdAt = 0,
                    updatedAt = 0,
                    deletedAt = null,
                ),
            )
        }
    }

    fun toggleDone(task: TaskEntity) = viewModelScope.launch { taskRepository.setDone(task, !task.isDone) }

    fun deleteTask(id: Long) = viewModelScope.launch { taskRepository.softDelete(id) }
}
