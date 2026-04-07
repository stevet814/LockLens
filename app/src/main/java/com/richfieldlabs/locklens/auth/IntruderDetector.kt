package com.richfieldlabs.locklens.auth

import com.richfieldlabs.locklens.data.db.IntruderDao
import com.richfieldlabs.locklens.data.model.IntruderEvent
import com.richfieldlabs.locklens.data.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntruderDetector @Inject constructor(
    private val authRepository: AuthRepository,
    private val intruderDao: IntruderDao,
) {
    suspend fun recordFailedAttempt(attemptedPin: String) {
        intruderDao.insert(
            IntruderEvent(
                attemptedPin = authRepository.hashPin(attemptedPin),
            ),
        )
    }
}

