package com.richfieldlabs.locklens.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {
    private const val FORMAT = "pbkdf2_sha256"
    private const val ITERATIONS = 200_000
    private const val SALT_LENGTH_BYTES = 16
    private const val KEY_LENGTH_BITS = 256
    private const val DELIMITER = "$"
    private val secureRandom = SecureRandom()

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        val derived = derive(pin, salt, ITERATIONS)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(derived)
        return listOf(FORMAT, ITERATIONS.toString(), saltBase64, hashBase64).joinToString(DELIMITER)
    }

    fun verify(pin: String, storedHash: String): Boolean {
        val parts = storedHash.split(DELIMITER)
        if (parts.size != 4 || parts[0] != FORMAT) {
            return legacySha256(pin).equals(storedHash, ignoreCase = true)
        }

        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = decode(parts[2]) ?: return false
        val expected = decode(parts[3]) ?: return false
        val actual = derive(pin, salt, iterations)
        return MessageDigest.isEqual(actual, expected)
    }

    fun needsRehash(storedHash: String): Boolean = !storedHash.startsWith("$FORMAT$")

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val password = pin.toCharArray()
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }

    private fun legacySha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun decode(value: String): ByteArray? {
        return try {
            Base64.getDecoder().decode(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
