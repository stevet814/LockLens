package com.richfieldlabs.locklens.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun expiredLockoutResetsEscalationBeforeNextFailure() = runBlocking {
        val tempDir = Files.createTempDirectory("locklens-auth-test")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { tempDir.resolve("auth.preferences_pb").toFile() },
        )
        val repository = AuthRepository(dataStore)

        repeat(5) { attempt ->
            repository.registerFailedPinAttempt(nowMillis = attempt.toLong())
        }

        assertEquals(5, repository.authPreferences.first().failedPinAttempts)
        assertEquals(0L, repository.getRemainingPinLockoutMillis(nowMillis = 30_005L))
        assertEquals(0, repository.authPreferences.first().failedPinAttempts)

        val nextFailure = repository.registerFailedPinAttempt(nowMillis = 30_005L)
        assertEquals(1, nextFailure.totalAttempts)
        assertEquals(0L, nextFailure.lockoutRemainingMillis)
    }

    @Test
    fun realAndDecoyPinsAreCheckedIndependently() = runBlocking {
        val tempDir = Files.createTempDirectory("locklens-auth-test")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { tempDir.resolve("auth.preferences_pb").toFile() },
        )
        val repository = AuthRepository(dataStore)

        repository.setRealPin("1234")
        repository.setDecoyPin("5678")

        assertTrue(repository.isRealPin("1234"))
        assertFalse(repository.isRealPin("5678"))
        assertTrue(repository.isDecoyPin("5678"))
        assertFalse(repository.isDecoyPin("1234"))
    }
}
