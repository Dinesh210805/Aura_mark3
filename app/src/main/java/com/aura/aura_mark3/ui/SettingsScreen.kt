package com.aura.aura_mark3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    ttsFeedbackEnabled: Boolean,
    onTtsFeedbackChange: (Boolean) -> Unit,
    ttsLanguage: String,
    onTtsLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    accessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    selectedVoice: String,
    onVoiceChange: (String) -> Unit
) {
    val languages = listOf("en", "fr", "es", "de")
    val voices = listOf(
        "Arista-PlayAI", "Atlas-PlayAI", "Basil-PlayAI", "Briggs-PlayAI", "Calum-PlayAI", "Celeste-PlayAI",
        "Cheyenne-PlayAI", "Chip-PlayAI", "Cillian-PlayAI", "Deedee-PlayAI", "Fritz-PlayAI", "Gail-PlayAI",
        "Indigo-PlayAI", "Mamaw-PlayAI", "Mason-PlayAI", "Mikail-PlayAI", "Mitch-PlayAI", "Quinn-PlayAI", "Thunder-PlayAI"
    )
    var langExpanded by remember { mutableStateOf(false) }
    var voiceExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        // Permission status
        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Microphone")
            Spacer(modifier = Modifier.width(8.dp))
            if (micPermissionGranted) {
                Text("Granted", color = Color(0xFF388E3C), modifier = Modifier.background(Color(0x22388E3C)).padding(4.dp))
            } else {
                Text("Missing", color = Color.Red, modifier = Modifier.background(Color(0x22FF0000)).padding(4.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onRequestMicPermission) { Text("Grant") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Accessibility Service")
            Spacer(modifier = Modifier.width(8.dp))
            if (accessibilityEnabled) {
                Text("Enabled", color = Color(0xFF388E3C), modifier = Modifier.background(Color(0x22388E3C)).padding(4.dp))
            } else {
                Text("Disabled", color = Color.Red, modifier = Modifier.background(Color(0x22FF0000)).padding(4.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onOpenAccessibilitySettings) { Text("Open Settings") }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TTS Feedback")
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = ttsFeedbackEnabled,
                onCheckedChange = onTtsFeedbackChange
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("TTS Language")
        Box {
            Button(onClick = { langExpanded = true }) {
                Text(ttsLanguage)
            }
            DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                languages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang) },
                        onClick = {
                            onTtsLanguageChange(lang)
                            langExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("PlayAI Voice")
        Box {
            Button(onClick = { voiceExpanded = true }) {
                Text(selectedVoice)
            }
            DropdownMenu(expanded = voiceExpanded, onDismissRequest = { voiceExpanded = false }) {
                voices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice) },
                        onClick = {
                            onVoiceChange(voice)
                            voiceExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
} 