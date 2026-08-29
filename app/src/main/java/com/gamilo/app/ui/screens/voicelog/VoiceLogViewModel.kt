package com.gamilo.app.ui.screens.voicelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamilo.app.ai.JobVoiceExtractor
import com.gamilo.app.ai.TranscriptionResult
import com.gamilo.app.ai.VoiceTranscriptionEngine
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.repo.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class VoiceLogState {
    object Idle : VoiceLogState()
    object Listening : VoiceLogState()
    data class Review(val draft: JobVoiceExtractor.Draft) : VoiceLogState()
    data class Error(val message: String) : VoiceLogState()
    object Saved : VoiceLogState()
}

/**
 * Drives the Phase 2 voice accelerator end to end: capture -> local rule-based extraction ->
 * mandatory user review -> manual confirm. Nothing here ever writes to the database without the
 * Review step's explicit confirm — AI and voice are strictly local accelerators that auto-fill
 * a form for user review, never auto-committing what was heard.
 */
class VoiceLogViewModel(
    private val transcriptionEngine: VoiceTranscriptionEngine,
    private val jobRepository: JobRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<VoiceLogState>(VoiceLogState.Idle)
    val state: StateFlow<VoiceLogState> = _state.asStateFlow()

    fun startCapture() {
        _state.value = VoiceLogState.Listening
        viewModelScope.launch {
            when (val result = transcriptionEngine.transcribeOnce()) {
                is TranscriptionResult.Success -> _state.value = VoiceLogState.Review(JobVoiceExtractor.extract(result.text))
                is TranscriptionResult.Failure -> _state.value = VoiceLogState.Error(result.message)
            }
        }
    }

    fun confirm(clientName: String, title: String, rawText: String) {
        viewModelScope.launch {
            jobRepository.create(
                JobEntity(
                    clientName = clientName,
                    title = title,
                    status = JobStatus.ACTIVE,
                    notes = "Voice: \"$rawText\"",
                    createdAt = 0,
                    updatedAt = 0,
                    deletedAt = null,
                ),
            )
            _state.value = VoiceLogState.Saved
        }
    }

    fun dismiss() {
        _state.value = VoiceLogState.Idle
    }
}
