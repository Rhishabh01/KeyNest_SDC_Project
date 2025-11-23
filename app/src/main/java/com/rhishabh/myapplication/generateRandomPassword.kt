package com.rhishabh.myapplication


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun GeneratedPasswordsScreen(
    generatedPassword: String,
    onSaveClick: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxSize()
    ) {
        Text("Generated Password", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Enter Name (e.g. Instagram)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Text(
            generatedPassword,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("password", generatedPassword)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy Password")
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                if (title.isNotEmpty()) {
                    onSaveClick(title, generatedPassword)
                    onBack()
                } else {
                    Toast.makeText(context, "Enter a name first", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Password")
        }
    }
}
