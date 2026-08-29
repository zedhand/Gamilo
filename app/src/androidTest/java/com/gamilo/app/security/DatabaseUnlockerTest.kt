package com.gamilo.app.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Exercises the real AndroidKeyStore-backed encrypt/decrypt round trip end to end — with
 * `requireUserAuthentication = false`, matching exactly how a device with nothing enrolled
 * (or an instrumented test run) uses this class. The biometric-gated path itself can't be
 * driven by an automated test (see the master plan's Test Automation Limits) and was verified
 * manually on-device instead.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class DatabaseUnlockerTest {

    private lateinit var file: File
    private lateinit var passphraseStore: PassphraseStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        file = File(context.filesDir, "test_security_${UUID.randomUUID()}.preferences_pb")
        passphraseStore = PassphraseStore(PreferenceDataStoreFactory.create(produceFile = { file }))
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun firstUnlock_generatesAndPersistsANewPassphrase() = runTest {
        val unlocker = DatabaseUnlocker(DbKeyManager(requireUserAuthentication = false), passphraseStore, biometricGate = null)

        val result = unlocker.unlock()
        check(result is UnlockResult.Ready)
        assertFalse(result.passphrase.isEmpty())
    }

    @Test
    fun secondUnlock_recoversTheSamePassphraseAsTheFirst() = runTest {
        val unlocker = DatabaseUnlocker(DbKeyManager(requireUserAuthentication = false), passphraseStore, biometricGate = null)

        val first = unlocker.unlock() as UnlockResult.Ready
        // A fresh unlocker instance (as a real cold start would use) reading the same store.
        val secondUnlocker = DatabaseUnlocker(DbKeyManager(requireUserAuthentication = false), passphraseStore, biometricGate = null)
        val second = secondUnlocker.unlock() as UnlockResult.Ready

        assertArrayEquals(first.passphrase, second.passphrase)
    }

    @Test
    fun passphraseCiphertext_isNeverTheSameBytesAsThePlaintext() = runTest {
        val unlocker = DatabaseUnlocker(DbKeyManager(requireUserAuthentication = false), passphraseStore, biometricGate = null)
        val result = unlocker.unlock() as UnlockResult.Ready

        val stored = passphraseStore.load()!!
        // A ciphertext that happened to equal the plaintext would indicate encryption never ran.
        org.junit.Assert.assertFalse(stored.ciphertext.contentEquals(result.passphrase))
    }
}
