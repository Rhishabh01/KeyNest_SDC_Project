package com.yourname.passwordmanager.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun SettingsScreen(
    onDestroyPasswords: () -> Unit,
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State variables
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePass1 by remember { mutableStateOf("") }
    var deletePass2 by remember { mutableStateOf("") }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Delete all passwords (local + cloud)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Destroy All Saved Passwords")
                IconButton(onClick = { onDestroyPasswords() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Destroy Passwords")
                }
            }
        }

        // Change Password
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Change Password")
                IconButton(onClick = { showChangePasswordDialog = true }) {
                    Icon(Icons.Filled.Lock, contentDescription = "Change Password")
                }
            }
        }

        // Delete Account

        Spacer(Modifier.height(30.dp))

        // Back Button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            Spacer(Modifier.width(8.dp))
            Text("Back")
        }
    }


    // ---------------- CHANGE PASSWORD DIALOG ----------------
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Old Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val user = auth.currentUser ?: return@TextButton

                    if (oldPassword.isBlank() || newPassword.isBlank() || confirmNewPassword.isBlank()) {
                        Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    if (newPassword != confirmNewPassword) {
                        Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)

                    user.reauthenticate(credential)
                        .addOnSuccessListener {
                            user.updatePassword(newPassword)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Password Updated", Toast.LENGTH_SHORT).show()
                                    showChangePasswordDialog = false
                                    oldPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Old password incorrect", Toast.LENGTH_SHORT).show()
                        }

                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePasswordDialog = false
                }) { Text("Cancel") }
            }
        )
    }
}
