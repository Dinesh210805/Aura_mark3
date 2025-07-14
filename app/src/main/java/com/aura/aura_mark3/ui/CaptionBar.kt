package com.aura.aura_mark3.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CaptionBar(
    userTranscription: String,
    assistantSpeech: String,
    modifier: Modifier = Modifier
) {
    val captionText = when {
        userTranscription.isNotBlank() -> "You: $userTranscription"
        assistantSpeech.isNotBlank() -> "Assistant: $assistantSpeech"
        else -> ""
    }
    if (captionText.isNotBlank()) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            color = Color(0xFF212121).copy(alpha = 0.85f),
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = captionText,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
} 