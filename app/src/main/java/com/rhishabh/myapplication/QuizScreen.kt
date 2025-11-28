package com.rhishabh.myapplication


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(onClose: () -> Unit) {

    data class Question(val id: Int, val text: String, val options: List<String>, val correct: Int)

    val questions = listOf(
        Question(1, "What does 'VPN' stand for?", listOf(
            "Virtual Private Network",
            "Verified Private Network",
            "Visual Private Net",
            "Virtual Public Network"
        ), 0),

        Question(2, "Which is a strong password practice?", listOf(
            "Using 'password123'",
            "Reusing same password",
            "Using unique long passphrase",
            "Writing password on sticky notes"
        ), 2),

        Question(3, "What is phishing?", listOf(
            "A method of routing",
            "Tricking people to reveal sensitive info",
            "Speeding up downloads",
            "Antivirus type"
        ), 1),

        Question(4, "Which protocol is secure?", listOf("HTTP", "FTP", "SSH", "HTTPS"), 3),

        Question(5, "What to do if asked for password by email?", listOf(
            "Reply with password",
            "Open links and enter credentials",
            "Delete or verify sender",
            "Forward to contacts"
        ), 2)
    )

    val selections = remember { mutableStateMapOf<Int, Int>() }
    var submitted by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    val scrollState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Spacer(modifier = Modifier.height(12.dp))
        Text("Answer all 5 questions:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(state = scrollState, modifier = Modifier.weight(1f)) {
            itemsIndexed(questions) { _, q ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${q.id}. ${q.text}")

                        Spacer(Modifier.height(8.dp))

                        q.options.forEachIndexed { i, opt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !submitted) {
                                        if (!submitted) selections[q.id] = i
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selections[q.id] == i,
                                    onClick = {
                                        if (!submitted) selections[q.id] = i
                                    }
                                )

                                Spacer(Modifier.width(8.dp))
                                Text(opt)
                                Spacer(Modifier.weight(1f))

                                if (submitted) {
                                    val correct = q.correct == i
                                    val selected = selections[q.id] == i

                                    when {
                                        selected && correct -> Icon(Icons.Filled.CheckCircle, "correct", tint = Color(0xFF2E7D32))
                                        selected && !correct -> Icon(Icons.Filled.Cancel, "wrong", tint = Color(0xFFB00020))
                                        correct -> Icon(Icons.Filled.Check, "correct", tint = Color(0xFF2E7D32))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!submitted) {
            Button(
                onClick = {
                    score = questions.count { selections[it.id] == it.correct }
                    submitted = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Submit Answers") }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your score: $score / ${questions.size}")

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        selections.clear()
                        submitted = false
                        score = 0
                    }) { Text("Retry") }

                    Button(onClick = { onClose() }) { Text("Back") }
                }
            }
        }
    }
}
