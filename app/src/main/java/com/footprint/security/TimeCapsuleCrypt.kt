package com.footprint.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object TimeCapsuleCrypt {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    /**
     * "在这封信件的文本或音频在写入数据库的瞬间，可以通过对称加密算法（如 AES）进行本地加密，
     *  密钥的盐值（Salt）可以直接绑定她未来的那个特定生日日期。"
     */
    fun encrypt(plainText: String, birthdayTimestamp: Long): String {
        val key = generateKey(birthdayTimestamp)
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(16) // Zero IV for deterministic crypto of the capsule, or randomize and store
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String, birthdayTimestamp: Long): String? {
        return try {
            val key = generateKey(birthdayTimestamp)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) {
            null // Fails to decrypt before time
        }
    }

    private fun generateKey(saltTimestamp: Long): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(saltTimestamp.toString().toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }
}
