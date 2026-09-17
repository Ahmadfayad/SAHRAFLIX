package com.sahraflix.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SecurePreferences(context: Context, fileName: String) {
    private val preferences = EncryptedSharedPreferences.create(
        fileName,
        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getString(key: String): String? = preferences.getString(key, null)
    fun getLong(key: String): Long? = if (preferences.contains(key)) preferences.getLong(key, 0L) else null
    fun getBoolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    fun putString(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    fun putLong(key: String, value: Long) { preferences.edit().putLong(key, value).apply() }
    fun putBoolean(key: String, value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
    fun remove(key: String) { preferences.edit().remove(key).apply() }

    fun observeBoolean(key: String, default: Boolean): Flow<Boolean> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(getBoolean(key, default))
        }
        trySend(getBoolean(key, default))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun observeString(key: String, default: String): Flow<String> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(getString(key) ?: default)
        }
        trySend(getString(key) ?: default)
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
