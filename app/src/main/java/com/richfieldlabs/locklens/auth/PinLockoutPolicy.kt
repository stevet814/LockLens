package com.richfieldlabs.locklens.auth

object PinLockoutPolicy {
    fun lockoutDurationMillis(failedAttempts: Int): Long {
        return when {
            failedAttempts < 5 -> 0L
            failedAttempts < 8 -> 30_000L
            failedAttempts < 10 -> 5 * 60_000L
            failedAttempts < 12 -> 15 * 60_000L
            else -> 60 * 60_000L
        }
    }

    fun buildMessage(remainingMillis: Long): String {
        return "Too many incorrect PIN attempts. Try again in ${formatDuration(remainingMillis)}."
    }

    fun formatDuration(remainingMillis: Long): String {
        val roundedSeconds = ((remainingMillis + 999L) / 1_000L).coerceAtLeast(1L)
        return when {
            roundedSeconds < 60L -> formatUnit(roundedSeconds, "second")
            roundedSeconds < 3_600L -> formatUnit((roundedSeconds + 59L) / 60L, "minute")
            else -> formatUnit((roundedSeconds + 3_599L) / 3_600L, "hour")
        }
    }

    private fun formatUnit(value: Long, unit: String): String {
        val suffix = if (value == 1L) "" else "s"
        return "$value $unit$suffix"
    }
}
