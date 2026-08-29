package com.gamilo.app.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability { AVAILABLE, NONE_ENROLLED, UNAVAILABLE }

sealed class BiometricOutcome {
    object Success : BiometricOutcome()
    object Failed : BiometricOutcome()
    data class Error(val message: String) : BiometricOutcome()
}

/**
 * Wraps [BiometricPrompt] with a single authenticator set — BIOMETRIC_STRONG or
 * DEVICE_CREDENTIAL — used for both the cold-start unlock and the resume-from-background
 * re-lock. This accepts whatever the device's system default unlock is (fingerprint, face,
 * pattern, or PIN), matching the sibling StudioFlow app's behavior, rather than requiring a
 * specific biometric class. There's no CryptoObject binding: DbKeyManager's Keystore key uses
 * a validity-duration window instead of per-operation crypto binding specifically so it can
 * accept a DEVICE_CREDENTIAL unlock too — that combination isn't reliably supported through a
 * BiometricPrompt CryptoObject session on every API level this app supports (minSdk 26).
 * A negative button isn't set: when DEVICE_CREDENTIAL is in the allowed set, the system
 * supplies its own "Use PIN/pattern/password" affordance instead.
 */
class BiometricGate(private val activity: FragmentActivity) {

    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun checkAvailability(): BiometricAvailability {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun authenticate(onResult: (BiometricOutcome) -> Unit) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Gamilo")
            .setSubtitle("Verify your identity to continue")
            .setAllowedAuthenticators(authenticators)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(BiometricOutcome.Success)
            }

            override fun onAuthenticationFailed() {
                onResult(BiometricOutcome.Failed)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(BiometricOutcome.Error(errString.toString()))
            }
        }

        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(promptInfo)
    }
}
