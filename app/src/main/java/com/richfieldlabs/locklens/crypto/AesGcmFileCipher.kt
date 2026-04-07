package com.richfieldlabs.locklens.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec

class AesGcmFileCipher(
    private val keyProvider: SecretKeyProvider,
) {
    fun encrypt(inputStream: InputStream, outputFile: File): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreateKey())
        val iv = cipher.iv

        inputStream.use { input ->
            CipherOutputStream(FileOutputStream(outputFile), cipher).use { output ->
                input.copyTo(output)
            }
        }

        return iv
    }

    fun decrypt(encryptedFile: File, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyProvider.getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )

        return CipherInputStream(FileInputStream(encryptedFile), cipher).use { input ->
            input.readBytes()
        }
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}

