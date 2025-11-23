package com.rhishabh.myapplication

import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object BackupManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Generate AES key from user UID
    private fun generateKey(uid: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(uid.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    private fun encrypt(data: String, key: SecretKeySpec): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    private fun decrypt(data: String, key: SecretKeySpec): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decoded = Base64.decode(data, Base64.DEFAULT)
        return String(cipher.doFinal(decoded), Charsets.UTF_8)
    }

    fun backupPasswords(
        context: Context,
        passwordList: List<PasswordItem>,
        scope: CoroutineScope,
        dataStoreKey: String = "saved_passwords"
    ) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Guests cannot backup data", Toast.LENGTH_SHORT).show()
            return
        }

        if (passwordList.isEmpty()) {
            Toast.makeText(context, "No passwords available to backup", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonData = Json.encodeToString(passwordList)
        val encryptedData = encrypt(jsonData, generateKey(user.uid))

        db.collection("users")
            .document(user.uid)
            .set(mapOf("passwords" to encryptedData))
            .addOnSuccessListener {
                Toast.makeText(context, "Backup successful", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
            }
    }


    fun restorePasswords(
        context: Context,
        passwordList: MutableList<PasswordItem>,
        scope: CoroutineScope,
        dataStoreKey: String = "saved_passwords"
    ) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Guests cannot restore data", Toast.LENGTH_SHORT).show()
            return
        }

        val key = stringPreferencesKey(dataStoreKey)

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val encryptedData = document.getString("passwords")
                if (encryptedData != null) {
                    try {
                        val decryptedJson = decrypt(encryptedData, generateKey(user.uid))
                        val restored = Json.decodeFromString<List<PasswordItem>>(decryptedJson)

                        // Merge restored passwords with existing local passwords
                        val existingSet = passwordList.map { it.title to it.username }.toSet()
                        restored.forEach { item ->
                            if ((item.title to item.username) !in existingSet) {
                                passwordList.add(item)
                            }
                        }

                        // Persist merged list
                        scope.launch {
                            context.dataStore.edit {
                                it[key] = Json.encodeToString(passwordList.toList())
                            }
                        }

                        Toast.makeText(context, "Restored successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to decrypt backup", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(context, "No backup found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
            }
    }
}