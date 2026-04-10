package com.richfieldlabs.locklens.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.richfieldlabs.locklens.auth.AuthFallbackMode
import com.richfieldlabs.locklens.auth.LockTimeout
import com.richfieldlabs.locklens.auth.PinLockoutPolicy
import com.richfieldlabs.locklens.auth.PinResult
import com.richfieldlabs.locklens.auth.PinSecurity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AuthPreferences(
    val isVaultInitialized: Boolean = false,
    val hasRealPin: Boolean = false,
    val hasDecoyPin: Boolean = false,
    val biometricEnabled: Boolean = true,
    val fallbackMode: AuthFallbackMode = AuthFallbackMode.DEVICE_CREDENTIAL,
    val isProUnlocked: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATE,
    val failedPinAttempts: Int = 0,
)

data class FailedPinAttemptResult(
    val totalAttempts: Int,
    val lockoutRemainingMillis: Long,
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
            lockTimeout = LockTimeout.fromStorageValue(preferences[LOCK_TIMEOUT]),
            failedPinAttempts = preferences[FAILED_PIN_ATTEMPTS] ?: 0,
        )
    }

    suspend fun markVaultInitialized() {
        dataStore.edit { preferences ->
            preferences[VAULT_INITIALIZED] = true
        }
    }

    suspend fun setRealPin(pin: String) {
        val storedHash = withContext(Dispatchers.Default) { PinSecurity.hash(pin) }
        dataStore.edit { preferences ->
            preferences[REAL_PIN_HASH] = storedHash
        }
    }

    suspend fun setDecoyPin(pin: String) {
        val storedHash = withContext(Dispatchers.Default) { PinSecurity.hash(pin) }
        dataStore.edit { preferences ->
            preferences[DECOY_PIN_HASH] = storedHash
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

    suspend fun setLockTimeout(timeout: LockTimeout) {
        dataStore.edit { preferences ->
            preferences[LOCK_TIMEOUT] = timeout.storageValue
        }
    }

    suspend fun registerFailedPinAttempt(
        nowMillis: Long = System.currentTimeMillis(),
    ): FailedPinAttemptResult {
        var newCount = 0
        var lockoutRemainingMillis = 0L
        dataStore.edit { preferences ->
            clearExpiredLockout(preferences, nowMillis)
            newCount = (preferences[FAILED_PIN_ATTEMPTS] ?: 0) + 1
            preferences[FAILED_PIN_ATTEMPTS] = newCount

            val lockoutDurationMillis = PinLockoutPolicy.lockoutDurationMillis(newCount)
            val lockoutUntilMillis = if (lockoutDurationMillis > 0L) {
                nowMillis + lockoutDurationMillis
            } else {
                0L
            }
            preferences[PIN_LOCKOUT_UNTIL_MILLIS] = lockoutUntilMillis
            lockoutRemainingMillis = (lockoutUntilMillis - nowMillis).coerceAtLeast(0L)
        }
        return FailedPinAttemptResult(
            totalAttempts = newCount,
            lockoutRemainingMillis = lockoutRemainingMillis,
        )
    }

    suspend fun getRemainingPinLockoutMillis(
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
        var remainingMillis = 0L
        dataStore.edit { preferences ->
            if (clearExpiredLockout(preferences, nowMillis)) {
                remainingMillis = 0L
            } else {
                val lockoutUntilMillis = preferences[PIN_LOCKOUT_UNTIL_MILLIS] ?: 0L
                remainingMillis = (lockoutUntilMillis - nowMillis).coerceAtLeast(0L)
            }
        }
        return remainingMillis
    }

    suspend fun resetFailedAttempts() {
        dataStore.edit { preferences ->
            preferences[FAILED_PIN_ATTEMPTS] = 0
            preferences[PIN_LOCKOUT_UNTIL_MILLIS] = 0L
        }
    }

    suspend fun checkPin(entered: String): PinResult {
        val preferences = dataStore.data.first()
        val realPinHash = preferences[REAL_PIN_HASH] ?: return PinResult.UNSET
        val decoyPinHash = preferences[DECOY_PIN_HASH]

        val realMatches = verifyPinAgainstStoredHash(entered, realPinHash)
        if (realMatches) {
            maybeUpgradePinHash(REAL_PIN_HASH, realPinHash, entered)
            return PinResult.REAL
        }

        if (decoyPinHash != null) {
            val decoyMatches = verifyPinAgainstStoredHash(entered, decoyPinHash)
            if (decoyMatches) {
                maybeUpgradePinHash(DECOY_PIN_HASH, decoyPinHash, entered)
                return PinResult.DECOY
            }
        }

        return PinResult.WRONG
    }

    fun hashPin(pin: String): String = PinSecurity.hash(pin)

    suspend fun isRealPin(pin: String): Boolean {
        val storedHash = dataStore.data.first()[REAL_PIN_HASH] ?: return false
        return verifyPinAgainstStoredHash(pin, storedHash)
    }

    suspend fun isDecoyPin(pin: String): Boolean {
        val storedHash = dataStore.data.first()[DECOY_PIN_HASH] ?: return false
        return verifyPinAgainstStoredHash(pin, storedHash)
    }

    private suspend fun maybeUpgradePinHash(
        key: Preferences.Key<String>,
        storedHash: String,
        plainPin: String,
    ) {
        if (!PinSecurity.needsRehash(storedHash)) {
            return
        }

        val upgradedHash = withContext(Dispatchers.Default) { PinSecurity.hash(plainPin) }
        dataStore.edit { preferences ->
            preferences[key] = upgradedHash
        }
    }

    private suspend fun verifyPinAgainstStoredHash(
        pin: String,
        storedHash: String,
    ): Boolean {
        return withContext(Dispatchers.Default) {
            PinSecurity.verify(pin, storedHash)
        }
    }

    private fun clearExpiredLockout(
        preferences: MutablePreferences,
        nowMillis: Long,
    ): Boolean {
        val lockoutUntilMillis = preferences[PIN_LOCKOUT_UNTIL_MILLIS] ?: 0L
        if (lockoutUntilMillis == 0L || lockoutUntilMillis > nowMillis) {
            return false
        }

        preferences[FAILED_PIN_ATTEMPTS] = 0
        preferences[PIN_LOCKOUT_UNTIL_MILLIS] = 0L
        return true
    }

    private companion object {
        val VAULT_INITIALIZED = booleanPreferencesKey("pref_vault_initialized")
        val REAL_PIN_HASH = stringPreferencesKey("pref_real_pin_hash")
        val DECOY_PIN_HASH = stringPreferencesKey("pref_decoy_pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("pref_biometric_enabled")
        val AUTH_FALLBACK_MODE = stringPreferencesKey("pref_auth_fallback_mode")
        val PRO_UNLOCKED = booleanPreferencesKey("pref_pro_unlocked")
        val LOCK_TIMEOUT = stringPreferencesKey("pref_lock_timeout")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("pref_failed_pin_attempts")
        val PIN_LOCKOUT_UNTIL_MILLIS = longPreferencesKey("pref_pin_lockout_until_millis")
    }
}
