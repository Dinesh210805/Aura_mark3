package com.aura.aura_mark3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
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
    onVoiceChange: (String) -> Unit,
    ttsSpeed: Float,
    onTtsSpeedChange: (Float) -> Unit,
    onVoicePreview: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth >= 600

    val languages = listOf(
        "en" to "English",
        "fr" to "Français",
        "es" to "Español",
        "de" to "Deutsch"
    )

    val voices = listOf(
        "Arista-PlayAI", "Atlas-PlayAI", "Basil-PlayAI", "Briggs-PlayAI", "Calum-PlayAI",
        "Celeste-PlayAI", "Cheyenne-PlayAI", "Chip-PlayAI", "Cillian-PlayAI", "Deedee-PlayAI",
        "Fritz-PlayAI", "Gail-PlayAI", "Indigo-PlayAI", "Mamaw-PlayAI", "Mason-PlayAI",
        "Mikail-PlayAI", "Mitch-PlayAI", "Quinn-PlayAI", "Thunder-PlayAI"
    )

    var langExpanded by remember { mutableStateOf(false) }
    var voiceExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 32.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                PermissionsSection(
                    micPermissionGranted = micPermissionGranted,
                    onRequestMicPermission = onRequestMicPermission,
                    accessibilityEnabled = accessibilityEnabled,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    isTablet = isTablet
                )
            }

            item {
                TTSSection(
                    ttsFeedbackEnabled = ttsFeedbackEnabled,
                    onTtsFeedbackChange = onTtsFeedbackChange,
                    ttsLanguage = ttsLanguage,
                    onTtsLanguageChange = onTtsLanguageChange,
                    selectedVoice = selectedVoice,
                    onVoiceChange = onVoiceChange,
                    ttsSpeed = ttsSpeed,
                    onTtsSpeedChange = onTtsSpeedChange,
                    onVoicePreview = onVoicePreview,
                    languages = languages,
                    voices = voices,
                    langExpanded = langExpanded,
                    onLangExpandedChange = { langExpanded = it },
                    voiceExpanded = voiceExpanded,
                    onVoiceExpandedChange = { voiceExpanded = it },
                    isTablet = isTablet
                )
            }

            // Bottom padding for better scroll experience
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PermissionsSection(
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    accessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    isTablet: Boolean
) {
    SettingsCard(
        title = "Permissions",
        icon = Icons.Default.Settings,
        isTablet = isTablet
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionRow(
                icon = Icons.Outlined.Mic,
                title = "Microphone",
                isGranted = micPermissionGranted,
                onActionClick = onRequestMicPermission,
                actionText = "Grant Permission"
            )

            PermissionRow(
                icon = Icons.Outlined.Accessibility,
                title = "Accessibility Service",
                isGranted = accessibilityEnabled,
                onActionClick = onOpenAccessibilitySettings,
                actionText = "Open Settings"
            )
        }
    }
}

@Composable
private fun TTSSection(
    ttsFeedbackEnabled: Boolean,
    onTtsFeedbackChange: (Boolean) -> Unit,
    ttsLanguage: String,
    onTtsLanguageChange: (String) -> Unit,
    selectedVoice: String,
    onVoiceChange: (String) -> Unit,
    ttsSpeed: Float,
    onTtsSpeedChange: (Float) -> Unit,
    onVoicePreview: (String) -> Unit,
    languages: List<Pair<String, String>>,
    voices: List<String>,
    langExpanded: Boolean,
    onLangExpandedChange: (Boolean) -> Unit,
    voiceExpanded: Boolean,
    onVoiceExpandedChange: (Boolean) -> Unit,
    isTablet: Boolean
) {
    SettingsCard(
        title = "Text-to-Speech",
        icon = Icons.Default.VolumeUp,
        isTablet = isTablet
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // TTS Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Enable TTS Feedback",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Switch(
                    checked = ttsFeedbackEnabled,
                    onCheckedChange = onTtsFeedbackChange
                )
            }

            if (ttsFeedbackEnabled) {
                // Language Selection
                DropdownSection(
                    title = "Language",
                    selectedValue = languages.find { it.first == ttsLanguage }?.second ?: ttsLanguage,
                    expanded = langExpanded,
                    onExpandedChange = onLangExpandedChange,
                    items = languages,
                    onItemSelected = { onTtsLanguageChange(it.first) },
                    itemText = { it.second }
                )

                // Voice Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownSection(
                            title = "Voice",
                            selectedValue = selectedVoice,
                            expanded = voiceExpanded,
                            onExpandedChange = onVoiceExpandedChange,
                            items = voices,
                            onItemSelected = onVoiceChange,
                            itemText = { it }
                        )
                    }

                    OutlinedButton(
                        onClick = { onVoicePreview(selectedVoice) },
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview")
                    }
                }

                // Speed Control
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Speech Speed",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )

                    Text(
                        "${String.format("%.1f", ttsSpeed)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Slider(
                        value = ttsSpeed,
                        onValueChange = onTtsSpeedChange,
                        valueRange = 0.8f..2.0f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Slower",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Faster",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    isTablet: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 24.dp else 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            content()
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    actionText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                isGranted = isGranted,
                grantedText = if (title.contains("Microphone")) "Granted" else "Enabled",
                deniedText = if (title.contains("Microphone")) "Missing" else "Disabled"
            )

            if (!isGranted) {
                FilledTonalButton(
                    onClick = onActionClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        actionText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    isGranted: Boolean,
    grantedText: String,
    deniedText: String
) {
    val backgroundColor = if (isGranted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val textColor = if (isGranted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (isGranted) grantedText else deniedText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor
        )
    }
}

@Composable
private fun <T> DropdownSection(
    title: String,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    itemText: (T) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )

        Box {
            OutlinedButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    selectedValue,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemText(item)) },
                        onClick = {
                            onItemSelected(item)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}