package com.rhishabh.myapplication

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val AES_KEY_SIZE = 128
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 128

    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, SecureRandom())
        return keyGen.generateKey()
    }

    fun encrypt(data: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_SIZE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String, key: SecretKey): String {
        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = decoded.copyOfRange(0, GCM_IV_SIZE)
        val cipherText = decoded.copyOfRange(GCM_IV_SIZE, decoded.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    fun keyFromString(base64Key: String): SecretKey {
        val decoded = Base64.decode(base64Key, Base64.DEFAULT)
        return SecretKeySpec(decoded, "AES")
    }

    fun keyToString(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.DEFAULT)
    }
}
