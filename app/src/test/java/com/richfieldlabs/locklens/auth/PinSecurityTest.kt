package com.richfieldlabs.locklens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    @Test
    fun hashRoundTrip_verifiesAndDoesNotNeedRehash() {
        val storedHash = PinSecurity.hash("1234")

        assertTrue(PinSecurity.verify("1234", storedHash))
        assertFalse(PinSecurity.verify("9999", storedHash))
        assertFalse(PinSecurity.needsRehash(storedHash))
    }

    @Test
    fun legacyHash_stillVerifiesAndRequestsUpgrade() {
        val legacyHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"

        assertTrue(PinSecurity.verify("1234", legacyHash))
        assertFalse(PinSecurity.verify("4321", legacyHash))
        assertTrue(PinSecurity.needsRehash(legacyHash))
    }

    @Test
    fun lockoutPolicy_escalatesAfterRepeatedFailures() {
        assertEquals(0L, PinLockoutPolicy.lockoutDurationMillis(4))
        assertEquals(30_000L, PinLockoutPolicy.lockoutDurationMillis(5))
        assertEquals(5 * 60_000L, PinLockoutPolicy.lockoutDurationMillis(8))
        assertEquals(15 * 60_000L, PinLockoutPolicy.lockoutDurationMillis(10))
        assertEquals(60 * 60_000L, PinLockoutPolicy.lockoutDurationMillis(12))
    }
}
