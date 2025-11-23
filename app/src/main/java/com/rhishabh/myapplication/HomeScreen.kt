package com.rhishabh.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    passwords: List<PasswordItem>,
    onGenerateClick: () -> Unit,
    onItemClick: (PasswordItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("KeyNest Password Manager") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onGenerateClick() }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)) {

            Text("Saved Passwords", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(passwords) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onItemClick(item) },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "••••••••••",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
