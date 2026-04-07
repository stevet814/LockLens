package com.richfieldlabs.locklens.crypto

import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    keystoreHelper: KeystoreHelper,
) {
    private val cipher = AesGcmFileCipher(keystoreHelper)

    fun encrypt(inputStream: InputStream, outputFile: File): ByteArray {
        return cipher.encrypt(inputStream, outputFile)
    }

    fun decrypt(encryptedFile: File, iv: ByteArray): ByteArray {
        return cipher.decrypt(encryptedFile, iv)
    }
}

