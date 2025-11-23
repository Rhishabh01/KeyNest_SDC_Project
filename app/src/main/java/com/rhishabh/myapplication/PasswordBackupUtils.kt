// PasswordBackupUtils.kt
package com.rhishabh.myapplication

import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object PasswordBackupUtils {

    private const val COLLECTION_NAME = "passwords_backup"
    private const val AES_ALGORITHM = "AES"
    private const val SECRET_KEY = "YourSecretKey123" // Must be 16 chars for AES-128

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // ---------------- AES Encryption / Decryption ----------------
    private fun getKey(): Key {
        return SecretKeySpec(SECRET_KEY.toByteArray(StandardCharsets.UTF_8), AES_ALGORITHM)
    }

    fun encrypt(text: String): String {
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String): String {
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getKey())
        val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
        return String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
    }

    // ---------------- Firestore Backup ----------------
    suspend fun backupPassword(username: String, password: String): Boolean {
        return try {
            val encryptedPassword = encrypt(password)
            val data = hashMapOf(
                "username" to username,
                "password" to encryptedPassword
            )
            firestore.collection(COLLECTION_NAME)
                .document(username)
                .set(data)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ---------------- Firestore Restore ----------------
    suspend fun restorePassword(username: String): String? {
        return try {
            val docSnapshot = firestore.collection(COLLECTION_NAME)
                .document(username)
                .get()
                .await()
            if (docSnapshot.exists()) {
                val encryptedPassword = docSnapshot.getString("password")
                encryptedPassword?.let { decrypt(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
