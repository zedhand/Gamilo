package com.gamilo.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one test that launches the real MainActivity — everything else in this suite talks
 * directly to isolated repositories/in-memory databases. This confirms the whole chain
 * actually boots on-device: BuildConfig.SKIP_BIOMETRIC_FOR_TESTS bypasses the biometric
 * prompt (which can't be driven by an automated test — see the master plan's Test Automation
 * Limits), DbKeyManager falls back to an unauthenticated AndroidKeyStore key, SQLCipher opens
 * the real encrypted database with the resulting passphrase, and the Home tab renders. The
 * actual biometric-gated path was verified manually on-device instead (see PROGRESS.md).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun debugBuild_bypassesBiometricPromptAndReachesHomeTab() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("START SHIFT").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("START SHIFT").assertExists()
    }
}
