package com.richfieldlabs.locklens.crypto

import javax.crypto.SecretKey

fun interface SecretKeyProvider {
    fun getOrCreateKey(): SecretKey
}

