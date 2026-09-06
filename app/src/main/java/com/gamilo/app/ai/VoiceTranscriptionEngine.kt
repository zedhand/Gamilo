package com.gamilo.app.ai

sealed class TranscriptionResult {
    data class Success(val text: String) : TranscriptionResult()
    data class Failure(val message: String) : TranscriptionResult()
}

/**
 * Offline speech-to-text. [WhisperTranscriptionEngine] is the only implementation — a bundled
 * Whisper tiny.en model run through TensorFlow Lite, chosen over the platform's own on-device
 * [android.speech.SpeechRecognizer] for reliability on short, name-heavy job dictation and to
 * avoid a network dependency. Kept as an interface so a different engine can be swapped in
 * later without touching any caller.
 */
interface VoiceTranscriptionEngine {
    suspend fun transcribeOnce(): TranscriptionResult
}
