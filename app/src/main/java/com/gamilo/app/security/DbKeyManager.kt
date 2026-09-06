package com.gamilo.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "gamilo_db_passphrase_key"
private const val GCM_TAG_LENGTH_BITS = 128
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/** A positive validity window (rather than 0/per-operation) is what lets the key accept ANY successful device unlock — biometric OR PIN/pattern/password — not just a fingerprint/face CryptoObject session. */
private const val AUTH_VALIDITY_SECONDS = 30

/**
 * Owns the AndroidKeyStore AES key that encrypts the SQLCipher database passphrase at rest
 * (see PassphraseStore). The key material never leaves the hardware-backed keystore — callers
 * only ever get a [Cipher] initialized with it, and in a real build that Cipher can't complete
 * an operation until the user has unlocked the device (any method — fingerprint, face,
 * pattern, PIN) within the last [AUTH_VALIDITY_SECONDS]. That's what makes the on-disk
 * database genuinely unreadable without a live device-unlock check — not just gated behind an
 * app-launch screen — while still matching "whatever the device's system default unlock is",
 * the same behavior as the sibling StudioFlow app.
 *
 * [requireUserAuthentication] is false only for instrumented test runs
 * (`BuildConfig.SKIP_BIOMETRIC_FOR_TESTS`) or when this device has no biometric/PIN enrolled
 * at all (an auth-required key could never be satisfied on such a device) — production and
 * manual debug installs on a device with a real lock screen always set it true.
 */
class DbKeyManager(private val requireUserAuthentication: Boolean) {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val specBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        if (requireUserAuthentication) {
            specBuilder.setUserAuthenticationRequired(true)
            // setUserAuthenticationValidityDurationSeconds is deprecated in favor of
            // setUserAuthenticationParameters(duration, authenticatorTypes) — but that
            // replacement needs API 30, and minSdk here is 26. The deprecated call is the
            // only one that gets "any positive duration accepts biometric OR device
            // credential" behavior on API 26-29; the newer one is used purely to be explicit
            // about which authenticator types qualify on API 30+.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                specBuilder.setUserAuthenticationParameters(
                    AUTH_VALIDITY_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
            } else {
                @Suppress("DEPRECATION")
                specBuilder.setUserAuthenticationValidityDurationSeconds(AUTH_VALIDITY_SECONDS)
            }
            specBuilder.setInvalidatedByBiometricEnrollment(true)
        }
        keyGenerator.init(specBuilder.build())
        return keyGenerator.generateKey()
    }

    /** Only callable within [AUTH_VALIDITY_SECONDS] of a successful device unlock when auth is required. */
    fun newEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher
    }

    fun newDecryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher
    }
}
