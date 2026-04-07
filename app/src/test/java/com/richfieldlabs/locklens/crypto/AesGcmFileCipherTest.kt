package com.richfieldlabs.locklens.crypto

import java.io.ByteArrayInputStream
import java.nio.file.Files
import javax.crypto.KeyGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AesGcmFileCipherTest {
    @Test
    fun encryptThenDecrypt_returnsOriginalBytes() {
        val secretKey = KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()
        val cipher = AesGcmFileCipher(SecretKeyProvider { secretKey })
        val plaintext = "LockLens keeps this local.".toByteArray()
        val outputFile = Files.createTempFile("locklens-", ".enc").toFile()

        val iv = cipher.encrypt(ByteArrayInputStream(plaintext), outputFile)
        val decrypted = cipher.decrypt(outputFile, iv)

        assertArrayEquals(plaintext, decrypted)

        outputFile.delete()
    }
}
