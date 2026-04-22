package dev.wizard.meta.util

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {
    private const val VERSION: Byte = 1
    private val PEPPER: String? = System.getenv("WEBHOOK_PEPPER")
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_LEN = 128
    private const val ITERATIONS = 100000

    fun encrypt(plainText: String?): String? {
        if (plainText == null) return null
        return try {
            val random = SecureRandom()
            val salt = ByteArray(SALT_LEN)
            random.nextBytes(salt)

            val pass = "metacryptshift" + (PEPPER ?: "")
            val key = deriveKey(pass, salt)

            val iv = ByteArray(IV_LEN)
            random.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LEN, iv))

            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val buf = ByteBuffer.allocate(1 + SALT_LEN + IV_LEN + cipherText.size)
            buf.put(VERSION).put(salt).put(iv).put(cipherText)

            val b64 = Base64.getEncoder().encodeToString(buf.array())
            shuffle(b64)
        } catch (ignored: Exception) {
            null
        }
    }

    fun decrypt(shuffledBlob: String?): String? {
        if (shuffledBlob == null) return null
        return try {
            val b64 = deshuffle(shuffledBlob) ?: return null
            val all = Base64.getDecoder().decode(b64)
            val buf = ByteBuffer.wrap(all)

            val version = buf.get()
            if (version != VERSION) return null

            val salt = ByteArray(SALT_LEN)
            buf.get(salt)

            val iv = ByteArray(IV_LEN)
            buf.get(iv)

            val cipherText = ByteArray(buf.remaining())
            buf.get(cipherText)

            val pass = "metacryptshift" + (PEPPER ?: "")
            val key = deriveKey(pass, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LEN, iv))

            val plain = cipher.doFinal(cipherText)
            String(plain, StandardCharsets.UTF_8)
        } catch (ignored: Exception) {
            null
        }
    }

    private fun deriveKey(pass: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pass.toCharArray(), salt, ITERATIONS, 256)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun shuffle(input: String?): String? {
        if (input.isNullOrEmpty()) return input
        val sb = StringBuilder(input)
        val len = sb.length
        return when (len) {
            1 -> input
            2 -> {
                val temp = sb[0]
                sb.setCharAt(0, sb[1])
                sb.setCharAt(1, temp)
                sb.toString()
            }
            3 -> {
                val last = sb[len - 1]
                sb.deleteCharAt(len - 1)
                sb.insert(0, last)
                sb.toString()
            }
            else -> {
                val last = sb[len - 1]
                val firstThree = sb.substring(0, 3)
                sb.deleteCharAt(len - 1)
                sb.delete(0, 3)
                sb.insert(0, last)
                sb.append(firstThree)
                sb.toString()
            }
        }
    }

    fun deshuffle(input: String?): String? {
        if (input.isNullOrEmpty()) return input
        val sb = StringBuilder(input)
        val len = sb.length
        return when (len) {
            1 -> input
            2 -> {
                val temp = sb[0]
                sb.setCharAt(0, sb[1])
                sb.setCharAt(1, temp)
                sb.toString()
            }
            3 -> {
                val first = sb[0]
                sb.deleteCharAt(0)
                sb.append(first)
                sb.toString()
            }
            else -> {
                val first = sb[0]
                val lastThree = sb.substring(len - 3)
                sb.deleteCharAt(0)
                sb.delete(len - 4, len - 1)
                sb.insert(0, lastThree)
                sb.append(first)
                sb.toString()
            }
        }
    }
}
