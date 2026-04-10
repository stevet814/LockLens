package com.richfieldlabs.locklens.auth

enum class LockTimeout(
    val milliseconds: Long,
    val displayName: String,
    val storageValue: String,
) {
    IMMEDIATE(0L, "Immediately", "immediate"),
    THIRTY_SECONDS(30_000L, "30 seconds", "30s"),
    ONE_MINUTE(60_000L, "1 minute", "1min"),
    FIVE_MINUTES(300_000L, "5 minutes", "5min");

    companion object {
        fun fromStorageValue(value: String?): LockTimeout =
            entries.firstOrNull { it.storageValue == value } ?: IMMEDIATE
    }
}
