package com.gamilo.app.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Holds only the AES/GCM ciphertext + IV of the SQLCipher database passphrase — never the
 * plaintext passphrase itself. The ciphertext is safe to leave in a plain DataStore file: it's
 * unreadable without the AndroidKeyStore key in DbKeyManager, which (in a real build) demands
 * a live biometric check to use at all.
 */
class PassphraseStore(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val CIPHERTEXT = stringPreferencesKey("db_passphrase_ciphertext")
        val IV = stringPreferencesKey("db_passphrase_iv")
    }

    suspend fun load(): EncryptedPassphrase? {
        val prefs = dataStore.data.first()
        val ciphertext = prefs[Keys.CIPHERTEXT] ?: return null
        val iv = prefs[Keys.IV] ?: return null
        return EncryptedPassphrase(Base64.decode(ciphertext, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP))
    }

    suspend fun save(encrypted: EncryptedPassphrase) {
        dataStore.edit { prefs ->
            prefs[Keys.CIPHERTEXT] = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP)
            prefs[Keys.IV] = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP)
        }
    }

    /** Part of factory reset — forces a brand-new passphrase (and thus a fresh DbKeyManager key use) next unlock. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

data class EncryptedPassphrase(val ciphertext: ByteArray, val iv: ByteArray)
