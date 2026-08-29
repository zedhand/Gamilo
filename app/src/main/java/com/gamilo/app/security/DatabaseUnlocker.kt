package com.gamilo.app.security

import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed class UnlockResult {
    data class Ready(val passphrase: ByteArray) : UnlockResult()
    object Cancelled : UnlockResult()
    data class Failed(val message: String) : UnlockResult()
}

/**
 * Orchestrates first-run passphrase generation and every-cold-start unlock of the SQLCipher
 * database passphrase. The passphrase never touches disk in plaintext — only its ciphertext
 * (via [passphraseStore]) does. When [biometricGate] is non-null, the device must be unlocked
 * (any method) before the Keystore key in [keyManager] will do anything at all — it isn't
 * bound to the biometric prompt via a CryptoObject; it just requires "unlocked within the
 * last few seconds", which the prompt's success guarantees.
 *
 * [biometricGate] is null exactly when [DbKeyManager] was built with
 * `requireUserAuthentication = false` (instrumented tests, or a device with nothing enrolled)
 * — in that case the Cipher operations run immediately with no prompt at all.
 */
class DatabaseUnlocker(
    private val keyManager: DbKeyManager,
    private val passphraseStore: PassphraseStore,
    private val biometricGate: BiometricGate?,
) {
    suspend fun unlock(): UnlockResult {
        if (biometricGate != null && !awaitAuthentication(biometricGate)) {
            return UnlockResult.Cancelled
        }

        val stored = passphraseStore.load()
        return if (stored != null) {
            val passphrase = runCatching { keyManager.newDecryptCipher(stored.iv).doFinal(stored.ciphertext) }
                .getOrElse { return UnlockResult.Failed(it.message ?: "Could not unlock database") }
            UnlockResult.Ready(passphrase)
        } else {
            val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val saved = runCatching {
                val cipher = keyManager.newEncryptCipher()
                EncryptedPassphrase(cipher.doFinal(newPassphrase), cipher.iv)
            }.getOrElse { return UnlockResult.Failed(it.message ?: "Could not secure a new passphrase") }
            passphraseStore.save(saved)
            UnlockResult.Ready(newPassphrase)
        }
    }

    private suspend fun awaitAuthentication(gate: BiometricGate): Boolean =
        suspendCancellableCoroutine { cont ->
            gate.authenticate { outcome ->
                if (cont.isActive) cont.resume(outcome is BiometricOutcome.Success)
            }
        }
}
