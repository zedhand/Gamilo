package com.gamilo.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gamilo.app.ai.TranscriptionResult
import com.gamilo.app.ai.VoiceTranscriptionEngine
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.ui.screens.voicelog.VoiceLogSheet
import com.gamilo.app.ui.screens.voicelog.VoiceLogViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Whisper inference can't run in an instrumented test (no microphone input, and the
 * ~40MB model would make every CI run slow) — the same Test Automation Limit as the camera/OCR
 * flow. [VoiceTranscriptionEngine] being an interface makes the rest of the pipeline (capture ->
 * extraction -> review -> confirm -> DB write) fully testable with a fake standing in for the
 * real transcription step. Real on-device transcription is verified manually — see PROGRESS.md.
 */
private class FakeTranscriptionEngine(private val result: TranscriptionResult) : VoiceTranscriptionEngine {
    override suspend fun transcribeOnce(): TranscriptionResult = result
}

@RunWith(AndroidJUnit4::class)
@LargeTest
class VoiceLogSheetTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successfulTranscription_reviewEditConfirm_createsJob() {
        val engine = FakeTranscriptionEngine(TranscriptionResult.Success("new job for Jane Smith to replace the kitchen faucet"))
        val jobRepository = JobRepository(screenRule.database.jobDao(), SystemClock)
        val viewModel = VoiceLogViewModel(engine, jobRepository)

        composeRule.setContent { GamiloTheme { VoiceLogSheet(viewModel, onDismiss = {}) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("REVIEW BEFORE SAVING").assertExists()
        composeRule.onNodeWithTag("voice_client_input").assertExists()
        composeRule.onNodeWithText("Jane Smith").assertExists()
        composeRule.onNodeWithText("replace the kitchen faucet").assertExists()

        composeRule.onNodeWithTag("voice_confirm_button").performClick()
        composeRule.waitForIdle()

        val jobs = runBlocking { jobRepository.observeAll().first() }
        assertEquals(1, jobs.size)
        assertEquals("Jane Smith", jobs[0].clientName)
        assertEquals("replace the kitchen faucet", jobs[0].title)
        assertEquals("Voice: \"new job for Jane Smith to replace the kitchen faucet\"", jobs[0].notes)
    }

    @Test
    fun reviewFields_areEditableBeforeConfirm() {
        val engine = FakeTranscriptionEngine(TranscriptionResult.Success("replace the garbage disposal"))
        val jobRepository = JobRepository(screenRule.database.jobDao(), SystemClock)
        val viewModel = VoiceLogViewModel(engine, jobRepository)

        composeRule.setContent { GamiloTheme { VoiceLogSheet(viewModel, onDismiss = {}) } }
        composeRule.waitForIdle()

        // No "for X to Y" phrasing in this transcript, so the client field starts empty —
        // the extractor treats the whole utterance as the title instead.
        composeRule.onNodeWithText("replace the garbage disposal").assertExists()
        composeRule.onNodeWithTag("voice_client_input").performTextInput("Bob Jones")
        composeRule.onNodeWithTag("voice_confirm_button").performClick()
        composeRule.waitForIdle()

        val jobs = runBlocking { jobRepository.observeAll().first() }
        assertEquals(1, jobs.size)
        assertEquals("Bob Jones", jobs[0].clientName)
        assertEquals("replace the garbage disposal", jobs[0].title)
    }

    @Test
    fun failedTranscription_showsErrorAndCanBeClosedWithoutCreatingAJob() {
        val engine = FakeTranscriptionEngine(TranscriptionResult.Failure("No speech detected."))
        val jobRepository = JobRepository(screenRule.database.jobDao(), SystemClock)
        val viewModel = VoiceLogViewModel(engine, jobRepository)
        var dismissed = false

        composeRule.setContent { GamiloTheme { VoiceLogSheet(viewModel, onDismiss = { dismissed = true }) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("COULD NOT TRANSCRIBE").assertExists()
        composeRule.onNodeWithText("No speech detected.").assertExists()
        composeRule.onNodeWithText("CLOSE").performClick()
        composeRule.waitForIdle()

        assert(dismissed) { "Expected onDismiss to be invoked" }
        val jobs = runBlocking { jobRepository.observeAll().first() }
        assertEquals(0, jobs.size)
    }

    @Test
    fun blankClientOrTitle_confirmButtonDoesNothing() {
        val engine = FakeTranscriptionEngine(TranscriptionResult.Success("new job for Jane Smith to replace the kitchen faucet"))
        val jobRepository = JobRepository(screenRule.database.jobDao(), SystemClock)
        val viewModel = VoiceLogViewModel(engine, jobRepository)

        composeRule.setContent { GamiloTheme { VoiceLogSheet(viewModel, onDismiss = {}) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("voice_client_input").performTextClearance()
        composeRule.onNodeWithTag("voice_confirm_button").performClick()
        composeRule.waitForIdle()

        // Still on the review screen — a blank client name must not save.
        composeRule.onNodeWithText("REVIEW BEFORE SAVING").assertExists()
        val jobs = runBlocking { jobRepository.observeAll().first() }
        assertEquals(0, jobs.size)
    }
}
