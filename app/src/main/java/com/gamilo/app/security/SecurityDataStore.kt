package com.gamilo.app.security

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Separate from AppContainer's settings DataStore on purpose: this one holds only the
 * encrypted DB passphrase blob and must be readable before AppContainer (and thus the
 * encrypted database) can be built at all — see DatabaseUnlocker.
 */
val Context.securityDataStore by preferencesDataStore(name = "gamilo_security")
