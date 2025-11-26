package com.rhishabh.myapplication

import androidx.compose.material3.*
// Add Firebase imports and other necessary imports
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import androidx.compose.runtime.mutableIntStateOf
// Firebase imports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider

val Context.dataStore by preferencesDataStore(name = "settings")

@Serializable
data class PasswordItem(val title: String, val username: String, val password: String)

fun generateRandomPassword(length: Int = 10): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#&*-_"
    return (1..length).map { chars.random() }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Theme
    val themeKey = booleanPreferencesKey("dark_mode")
    val isDarkMode = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val themeValue = context.dataStore.data.map { it[themeKey] ?: false }.first()
        isDarkMode.value = themeValue
    }

    // Firebase Auth
    val auth = FirebaseAuth.getInstance()

    // Login/signup state
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var guestMode by remember { mutableStateOf(false) }
    var showLoginScreen by remember { mutableStateOf(true) }
    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var selectedScreen by remember { mutableStateOf("home") }

    // Password list
    val passwordsKey = stringPreferencesKey("saved_passwords")
    val passwordList = remember { mutableStateListOf<PasswordItem>() }
    LaunchedEffect(Unit) {
        val raw = context.dataStore.data.map { it[passwordsKey] ?: "[]" }.first()
        try { passwordList.addAll(Json.decodeFromString(raw)) } catch (_: Exception) {}
    }

    // Color scheme
    val colorScheme = if (isDarkMode.value) {
        darkColorScheme(
            primary = Color(0xFF00E5FF),
            secondary = Color(0xFF4FC3F7),
            background = Color(0xFF12171C),
            onBackground = Color.White,
            surface = Color(0xFF10151B),
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0099BB),
            secondary = Color(0xFF26C6DA),
            background = Color(0xFFCBDADF),
            onBackground = Color.Black,
            surface = Color(0xFFD6E6EA),
            onSurface = Color.Black
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        if (!isLoggedIn && !guestMode) {
            // LOGIN / SIGNUP SCREEN
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Welcome Back",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = authEmail,
                        onValueChange = { authEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier
                            .width(350.dp) // fixed width in dp
                    )

                    OutlinedTextField(
                        value = authPassword,
                        onValueChange = { authPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .width(350.dp) // fixed width in dp
                    )


                    if (!showLoginScreen) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.width(350.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showLoginScreen) {
                        Button(
                            onClick = {
                                if (authEmail.isBlank() || authPassword.isBlank()) {
                                    Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                auth.signInWithEmailAndPassword(authEmail.trim(), authPassword)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            isLoggedIn = true
                                            guestMode = false
                                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, task.exception?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            },
                            modifier = Modifier.width(350.dp)
                        ) {
                            Text("Login")
                        }

                        // ---------- FORGOT PASSWORD ----------
                        TextButton(onClick = { showResetDialog = true }) {
                            Text("Forgot Password?")
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showLoginScreen = false }) { Text("Create account") }
                            TextButton(onClick = { guestMode = true }) { Text("Continue as Guest") }
                        }

                    } else {

                        // ---------- SIGNUP BUTTON ----------
                        Button(
                            onClick = {
                                if (authEmail.isBlank() || authPassword.isBlank() || confirmPassword.isBlank()) {
                                    Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (authPassword != confirmPassword) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                auth.createUserWithEmailAndPassword(authEmail.trim(), authPassword)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            isLoggedIn = true
                                            guestMode = false
                                            Toast.makeText(context, "Account created. Logged in.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, task.exception?.message ?: "Signup failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            },
                            modifier = Modifier.width(350.dp)
                        ) {
                            Text("Sign Up")
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showLoginScreen = true }) { Text("Back to Login") }
                            TextButton(onClick = { guestMode = true }) { Text("Continue as Guest") }
                        }
                    }

                }
                if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Reset Password") },
                    text = {
                        Column {
                            Text("Enter your email to receive a reset link.")
                            Spacer(Modifier.height(8.dp))

                            TextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                label = { Text("Email") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (resetEmail.isBlank()) {
                                Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }

                            auth.sendPasswordResetEmail(resetEmail.trim())
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Reset link sent", Toast.LENGTH_SHORT).show()
                                        showResetDialog = false
                                    } else {
                                        Toast.makeText(context, task.exception?.message ?: "Error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }) {
                            Text("Send")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

                // Theme toggle button
                IconButton(
                    onClick = {
                        scope.launch {
                            isDarkMode.value = !isDarkMode.value
                            context.dataStore.edit { it[themeKey] = isDarkMode.value }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkMode.value) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle Theme",
                        modifier = Modifier.size(25.dp),
                        tint = if (isDarkMode.value) Color.White else Color.Black // set icon color explicitly
                    )
                }

            }
        }
        // MAIN APP (AFTER LOGIN)
            else {

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scopeDrawer = rememberCoroutineScope()
                var showDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var currentTab by remember { mutableStateOf("home") }
                val generatedPasswords = remember { mutableStateListOf<String>() }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(
                                modifier = Modifier.fillMaxHeight().padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Menu", style = MaterialTheme.typography.titleMedium)

                                    NavigationDrawerItem(
                                        label = { Text("Toggle Dark Mode") },
                                        selected = false,
                                        onClick = {
                                            scope.launch {
                                                isDarkMode.value = !isDarkMode.value
                                                context.dataStore.edit {
                                                    it[themeKey] = isDarkMode.value
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isDarkMode.value) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                contentDescription = "Theme Toggle"
                                            )
                                        }
                                    )
                                    NavigationDrawerItem(
                                        label = { Text("Backup Passwords") },
                                        selected = false,
                                        onClick = {
                                            if (!guestMode) {
                                                BackupManager.backupPasswords(context, passwordList, scope)
                                            } else {
                                                Toast.makeText(context, "Guests cannot backup data", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.CloudUpload, contentDescription = "Backup") }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("Restore Passwords") },
                                        selected = false,
                                        onClick = {
                                            if (!guestMode) {
                                                BackupManager.restorePasswords(context, passwordList, scope)
                                            } else {
                                                Toast.makeText(context, "Guests cannot restore data", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.CloudDownload, contentDescription = "Restore") }
                                    )

                                    NavigationDrawerItem(
                                        label = { Text("Settings") },
                                        selected = (selectedScreen == "settings"),
                                        onClick = {
                                            selectedScreen = "settings"
                                            scope.launch { drawerState.close() }
                                        },
                                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                                    )



                                }

                                Button(
                                    onClick = {
                                        // Logout (Firebase sign out + local state)
                                        auth.signOut()
                                        isLoggedIn = false
                                        guestMode = false

                                        // Clear all login/signup fields
                                        authEmail = ""
                                        authPassword = ""
                                        confirmPassword = ""
                                        showLoginScreen = true  // optionally, go back to login screen
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Logout", color = Color.White)
                                }

                            }
                        }
                    }
                ) {
                    Scaffold(
                        floatingActionButton = {
                            if (currentTab == "home") {
                                FloatingActionButton(
                                    onClick = { showDialog = true },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 0.dp) // lifts above nav bar
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add Password",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        floatingActionButtonPosition = FabPosition.Center,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search by title or username") },
                                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scopeDrawer.launch { drawerState.open() } }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                IconButton(
                                    onClick = { currentTab = "generated" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Generated")
                                }
                                IconButton(
                                    onClick = { currentTab = "home" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Home, contentDescription = "Home")
                                }
                                IconButton(
                                    onClick = { currentTab = "quiz" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Help,
                                        contentDescription = "Quiz"
                                    )
                                }
                            }
                        }
                    ) { padding ->

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {

                            // GENERATED PASSWORD TAB
                            when (currentTab) {
                                "generated" -> {
                                    var showEdit by remember { mutableStateOf(false) }
                                    var editedPassword by remember { mutableStateOf("") }
                                    var editIndex by remember { mutableIntStateOf(-1) }

                                    Column(Modifier.fillMaxSize().padding(16.dp)) {

                                        Text(
                                            "Generated Passwords",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Spacer(Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                val newPass = generateRandomPassword()
                                                generatedPasswords.add(newPass)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Generate New Password") }

                                        Spacer(Modifier.height(16.dp))

                                        LazyColumn {
                                            itemsIndexed(generatedPasswords) { index, pass ->

                                                Card(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth()
                                                            .padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {

                                                        Text(pass, modifier = Modifier.weight(1f))

                                                        Row {

                                                            IconButton(onClick = {
                                                                val clipboard =
                                                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                clipboard.setPrimaryClip(
                                                                    ClipData.newPlainText(
                                                                        "Password",
                                                                        pass
                                                                    )
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "Copied",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }) {
                                                                Icon(
                                                                    Icons.Filled.ContentCopy,
                                                                    contentDescription = "Copy"
                                                                )
                                                            }

                                                            IconButton(onClick = {
                                                                editedPassword = pass
                                                                editIndex = index
                                                                showEdit = true
                                                            }) {
                                                                Icon(
                                                                    Icons.Filled.Edit,
                                                                    contentDescription = "Edit"
                                                                )
                                                            }

                                                            IconButton(onClick = {
                                                                generatedPasswords.removeAt(
                                                                    index
                                                                )
                                                            }) {
                                                                Icon(
                                                                    Icons.Filled.Delete,
                                                                    contentDescription = "Delete",
                                                                    tint = Color.Red
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (showEdit && editIndex >= 0) {
                                        AlertDialog(
                                            onDismissRequest = { showEdit = false },
                                            title = { Text("Edit Password") },
                                            text = {
                                                OutlinedTextField(
                                                    value = editedPassword,
                                                    onValueChange = { editedPassword = it },
                                                    label = { Text("Password") }
                                                )
                                            },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    generatedPasswords[editIndex] = editedPassword
                                                    showEdit = false
                                                }) { Text("Save") }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = {
                                                    showEdit = false
                                                }) { Text("Cancel") }
                                            }
                                        )
                                    }
                                }
                                // HOME TAB (Saved passwords)
                                "home" -> {
                                    val filteredList =
                                        if (searchQuery.isBlank()) passwordList
                                        else passwordList.filter {
                                            it.title.contains(searchQuery, true) ||
                                                    it.username.contains(searchQuery, true)
                                        }

                                    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                                        itemsIndexed(filteredList) { index, item ->

                                            // Store the confirmation password per item
                                            var showDelete by remember { mutableStateOf(false) }
                                            var confirm by remember { mutableStateOf("") }

                                            Card(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("Title: ${item.title}")
                                                        Text("Username: ${item.username}")
                                                        Text("Password: ${item.password}")
                                                    }

                                                    IconButton(onClick = {
                                                        // Reset the confirmation password when opening the dialog
                                                        confirm = ""
                                                        showDelete = true
                                                    }) {
                                                        Icon(
                                                            Icons.Filled.Delete,
                                                            contentDescription = "Delete",
                                                            tint = Color.Red
                                                        )
                                                    }
                                                }
                                            }

                                            if (showDelete) {
                                                if (!guestMode) {
                                                    AlertDialog(
                                                        onDismissRequest = { showDelete = false },
                                                        title = { Text("Confirm Delete") },
                                                        text = {
                                                            Column {
                                                                Text("Enter your login password:")
                                                                OutlinedTextField(
                                                                    value = confirm,
                                                                    onValueChange = { confirm = it },
                                                                    label = { Text("Password") },
                                                                    visualTransformation = PasswordVisualTransformation()
                                                                )
                                                            }
                                                        },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                val user = auth.currentUser
                                                                if (user?.email == null) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "No authenticated user",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    return@TextButton
                                                                }

                                                                val credential = EmailAuthProvider.getCredential(user.email!!, confirm)
                                                                user.reauthenticate(credential)
                                                                    .addOnSuccessListener {
                                                                        // Delete this item
                                                                        passwordList.remove(item)
                                                                        showDelete = false

                                                                        scope.launch {
                                                                            context.dataStore.edit {
                                                                                it[passwordsKey] = Json.encodeToString(passwordList.toList())
                                                                            }
                                                                        }

                                                                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    .addOnFailureListener {
                                                                        Toast.makeText(context, "Wrong password", Toast.LENGTH_SHORT).show()
                                                                    }
                                                            }) { Text("Confirm", color = Color.Red) }
                                                        },
                                                        dismissButton = {
                                                            TextButton(onClick = { showDelete = false }) { Text("Cancel") }
                                                        }
                                                    )
                                                } else {
                                                    AlertDialog(
                                                        onDismissRequest = { showDelete = false },
                                                        title = { Text("Delete Password?") },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                passwordList.remove(item)
                                                                showDelete = false
                                                                scope.launch {
                                                                    context.dataStore.edit {
                                                                        it[passwordsKey] = Json.encodeToString(passwordList.toList())
                                                                    }
                                                                }
                                                            }) { Text("Yes", color = Color.Red) }
                                                        },
                                                        dismissButton = {
                                                            TextButton(onClick = { showDelete = false }) { Text("No") }
                                                        },
                                                        text = { Text("Delete this saved password?") }
                                                    )
                                                }
                                            }
                                        }
                                    }


                                    if (showDialog) {

                                        var title by remember { mutableStateOf("") }
                                        var username by remember { mutableStateOf("") }
                                        var password by remember { mutableStateOf("") }
                                        var lastGenerated by remember { mutableStateOf("") }

                                        AlertDialog(
                                            onDismissRequest = { showDialog = false },
                                            title = { Text("Add New Password") },
                                            text = {

                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(
                                                        value = title,
                                                        onValueChange = { title = it },
                                                        label = { Text("Title / App") })
                                                    OutlinedTextField(
                                                        value = username,
                                                        onValueChange = { username = it },
                                                        label = { Text("Username") })
                                                    OutlinedTextField(
                                                        value = password,
                                                        onValueChange = { password = it },
                                                        label = { Text("Password") })

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {

                                                        Button(onClick = {
                                                            val newP = generateRandomPassword()
                                                            password = newP
                                                            lastGenerated = newP
                                                        }) {
                                                            Icon(
                                                                Icons.Filled.AutoAwesome,
                                                                contentDescription = "Generate"
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text("Generate")
                                                        }

                                                        if (lastGenerated.isNotBlank()) {
                                                            Button(onClick = {
                                                                val clipboard =
                                                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                clipboard.setPrimaryClip(
                                                                    ClipData.newPlainText(
                                                                        "Password",
                                                                        lastGenerated
                                                                    )
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "Copied",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }) {
                                                                Icon(
                                                                    Icons.Filled.ContentCopy,
                                                                    contentDescription = "Copy"
                                                                )
                                                                Spacer(Modifier.width(6.dp))
                                                                Text("Copy")
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(onClick = {

                                                    if (title.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                                        val newItem =
                                                            PasswordItem(title, username, password)
                                                        passwordList.add(newItem)
                                                        showDialog = false

                                                        scope.launch {
                                                            context.dataStore.edit {
                                                                it[passwordsKey] =
                                                                    Json.encodeToString(passwordList.toList())
                                                            }
                                                        }

                                                        Toast.makeText(
                                                            context,
                                                            "Password added",
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Fill all fields",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

                                                }) { Text("Save") }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDialog = false }) {
                                                    Text(
                                                        "Cancel"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                                // QUIZ TAB
                                "quiz" -> {
                                    QuizScreen(onClose = { currentTab = "home" })
                                }
                            }
                        }
                    }
                }
            }
        }
    }