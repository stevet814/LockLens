package com.richfieldlabs.locklens.auth

enum class AuthFallbackMode(val storageValue: String) {
    DEVICE_CREDENTIAL("device_credential"),
    APP_PIN("app_pin");

    companion object {
        fun fromStorageValue(value: String?): AuthFallbackMode? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}
