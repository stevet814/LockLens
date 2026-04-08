package com.richfieldlabs.locklens.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.richfieldlabs.locklens.auth.AuthFallbackMode
import com.richfieldlabs.locklens.auth.PinResult
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AuthPreferences(
    val isVaultInitialized: Boolean = false,
    val hasRealPin: Boolean = false,
    val hasDecoyPin: Boolean = false,
    val biometricEnabled: Boolean = true,
    val fallbackMode: AuthFallbackMode = AuthFallbackMode.DEVICE_CREDENTIAL,
    val isProUnlocked: Boolean = false,
)

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val authPreferences: Flow<AuthPreferences> = dataStore.data.map { preferences ->
        val hasRealPin = preferences[REAL_PIN_HASH] != null
        AuthPreferences(
            isVaultInitialized = preferences[VAULT_INITIALIZED] ?: false,
            hasRealPin = hasRealPin,
            hasDecoyPin = preferences[DECOY_PIN_HASH] != null,
            biometricEnabled = preferences[BIOMETRIC_ENABLED] ?: true,
            fallbackMode = AuthFallbackMode.fromStorageValue(preferences[AUTH_FALLBACK_MODE])
                ?: if (hasRealPin) AuthFallbackMode.APP_PIN else AuthFallbackMode.DEVICE_CREDENTIAL,
            isProUnlocked = preferences[PRO_UNLOCKED] ?: false,
        )
    }

    suspend fun markVaultInitialized() {
        dataStore.edit { preferences ->
            preferences[VAULT_INITIALIZED] = true
        }
    }

    suspend fun setRealPin(pin: String) {
        dataStore.edit { preferences ->
            preferences[REAL_PIN_HASH] = sha256(pin)
        }
    }

    suspend fun setDecoyPin(pin: String) {
        dataStore.edit { preferences ->
            preferences[DECOY_PIN_HASH] = sha256(pin)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setFallbackMode(mode: AuthFallbackMode) {
        dataStore.edit { preferences ->
            preferences[AUTH_FALLBACK_MODE] = mode.storageValue
        }
    }

    suspend fun setProUnlocked(unlocked: Boolean) {
        dataStore.edit { preferences ->
            preferences[PRO_UNLOCKED] = unlocked
        }
    }

    suspend fun checkPin(entered: String): PinResult {
        val preferences = dataStore.data.first()
        val hash = sha256(entered)
        val realPinHash = preferences[REAL_PIN_HASH]
        val decoyPinHash = preferences[DECOY_PIN_HASH]

        if (realPinHash == null) {
            return PinResult.UNSET
        }

        return when (hash) {
            realPinHash -> PinResult.REAL
            decoyPinHash -> PinResult.DECOY
            else -> PinResult.WRONG
        }
    }

    fun hashPin(pin: String): String = sha256(pin)

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private companion object {
        val VAULT_INITIALIZED = booleanPreferencesKey("pref_vault_initialized")
        val REAL_PIN_HASH = stringPreferencesKey("pref_real_pin_hash")
        val DECOY_PIN_HASH = stringPreferencesKey("pref_decoy_pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("pref_biometric_enabled")
        val AUTH_FALLBACK_MODE = stringPreferencesKey("pref_auth_fallback_mode")
        val PRO_UNLOCKED = booleanPreferencesKey("pref_pro_unlocked")
    }
}
