package com.example.c2c.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_IS_PAIRED = booleanPreferencesKey("is_paired")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_PAIRING_CODE = stringPreferencesKey("pairing_code")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        private val KEY_AUTO_DIAL = booleanPreferencesKey("auto_dial")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    // Pairing Status
    val isPaired: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_PAIRED] ?: false
    }

    suspend fun setIsPaired(isPaired: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_PAIRED] = isPaired
        }
    }

    // Device ID
    val deviceId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEVICE_ID] ?: ""
    }

    suspend fun setDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVICE_ID] = deviceId
        }
    }

    // Pairing Code
    val pairingCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PAIRING_CODE] ?: ""
    }

    suspend fun setPairingCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PAIRING_CODE] = code
        }
    }

    // FCM Token
    val fcmToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_FCM_TOKEN] ?: ""
    }

    suspend fun setFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FCM_TOKEN] = token
        }
    }

    // Auto Dial Setting
    val autoDialEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_DIAL] ?: true
    }

    suspend fun setAutoDialEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_DIAL] = enabled
        }
    }

    // Notifications Setting
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // Generate a new pairing code
    fun generatePairingCode(): String {
        return (100000..999999).random().toString()
    }

    // Clear all data (sign out)
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
