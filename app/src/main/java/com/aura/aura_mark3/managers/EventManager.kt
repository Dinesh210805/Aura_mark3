package com.aura.aura_mark3.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aura.aura_mark3.accessibility.AuraAccessibilityService
import com.aura.aura_mark3.accessibility.AuraAccessibilityService.Companion.ACTION_ACTION_RESULT
import com.aura.aura_mark3.accessibility.AuraAccessibilityService.Companion.EXTRA_ACTION_RESULT_SUCCESS
import com.aura.aura_mark3.accessibility.AuraAccessibilityService.Companion.EXTRA_ACTION_RESULT_MESSAGE
import com.aura.aura_mark3.accessibility.AuraAccessibilityService.Companion.ACTION_FOREGROUND_PACKAGE
import com.aura.aura_mark3.accessibility.AuraAccessibilityService.Companion.EXTRA_FOREGROUND_PACKAGE
import com.aura.aura_mark3.audio.AudioRecorderService
import com.aura.aura_mark3.audio.EnhancedVoiceService

/**
 * Manages broadcast receivers and event handling
 */
class EventManager(
    private val context: Context,
    private val voiceManager: VoiceManager,
    private val aiManager: AIManager,
    private val systemManager: SystemManager
) {
    // Voice service states
    var isListeningForWakeWord by mutableStateOf(false)
    var isRecordingCommand by mutableStateOf(false)
    var currentAudioLevel by mutableIntStateOf(0)
    var listeningType by mutableStateOf("stopped") // "wake_word", "command", "stopped"
    
    // Receivers
    private val actionResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val success = intent?.getBooleanExtra(EXTRA_ACTION_RESULT_SUCCESS, false) ?: false
            val message = intent?.getStringExtra(EXTRA_ACTION_RESULT_MESSAGE)
            
            // Only show meaningful messages, ignore empty or default ones
            if (!message.isNullOrBlank() && message != "Unknown result") {
                Log.i("AURA_ACTION", "Action result: success=$success, message=$message")
                voiceManager.statusMessage = if (success) "✅ $message" else "❌ $message"
            } else {
                Log.d("AURA_ACTION", "Ignoring empty action result message")
            }
        }
    }

    private val transcriptionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val transcription = intent?.getStringExtra(AudioRecorderService.EXTRA_TRANSCRIPTION) ?: ""
            Log.i("AURA_STT", "Received transcription: \"$transcription\"")
            
            // Don't process transcriptions when AURA is speaking to prevent feedback
            if (voiceManager.isSpeaking) {
                Log.i("AURA_STT", "Ignoring transcription while AURA is speaking to prevent feedback")
                return
            }
            
            if (transcription.isNotBlank()) {
                val filteredTranscription = filterOutAuraVoice(transcription)
                Log.i("AURA_STT", "Filtered transcription: \"$filteredTranscription\"")
                
                if (filteredTranscription.isNotBlank()) {
                    aiManager.animateTyping(filteredTranscription) {
                        aiManager.processVoiceCommand(filteredTranscription) { actions ->
                            if (actions.isNotEmpty()) {
                                systemManager.startActionQueue(actions)
                            }
                        }
                    }
                } else {
                    Log.i("AURA_STT", "Transcription filtered out as AURA's own voice")
                    voiceManager.statusMessage = "Ready for next command"
                }
            }
        }
    }
    
    private fun filterOutAuraVoice(transcription: String): String {
        val auraIndicators = listOf(
            "hello joyboy", "i'm aura", "aura here", "voice assistant",
            "how can i help", "what can i do", "i can help you",
            "absolutely", "right away", "got it", "sure thing",
            "of course", "certainly", "no problem", "i'm ready to help",
            "amazing voice assistant", "let's make some magic", "thrill",
            "ready to help you today", "say hey aura", "tap the microphone",
            "grant microphone permission", "failed to connect to server",
            "try again", "speech recognition failed", "processing your request",
            "i'm sorry", "couldn't process", "having trouble"
        )
        
        val lowerTranscription = transcription.lowercase().trim()
        
        // More sophisticated filtering
        val containsAuraIndicator = auraIndicators.any { indicator ->
            lowerTranscription.contains(indicator)
        }
        
        // Filter very short transcriptions that are likely TTS artifacts
        val tooShort = lowerTranscription.length < 3
        
        // Filter if it sounds like computer-generated speech patterns
        val computerSpeechPatterns = listOf("uh", "um", "er", "ah", "oh")
        val onlyFillerWords = computerSpeechPatterns.any { pattern ->
            lowerTranscription == pattern || lowerTranscription.split(" ").all { word ->
                computerSpeechPatterns.contains(word.trim())
            }
        }
        
        return if (containsAuraIndicator || tooShort || onlyFillerWords) {
            Log.i("AURA_FILTER", "Filtering out potential AURA voice/artifact: '$transcription'")
            ""
        } else {
            transcription
        }
    }

    private val foregroundPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra(EXTRA_FOREGROUND_PACKAGE) ?: ""
            aiManager.currentForegroundPackage = packageName
            Log.i("AURA_FOREGROUND", "Foreground package: $packageName")
        }
    }

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i("AURA_VOICE", "Wake word detected! Starting command recording...")
            voiceManager.statusMessage = "👂 Wake word detected! Listening for command..."
            listeningType = "command"
            isListeningForWakeWord = false
            isRecordingCommand = true
            
            if (!aiManager.hasGreeted) {
                aiManager.hasGreeted = true
            }
        }
    }

    private val voiceTranscriptionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val transcription = intent?.getStringExtra(EnhancedVoiceService.EXTRA_TRANSCRIPTION) ?: ""
            Log.i("AURA_VOICE", "Enhanced Voice Service transcription: \"$transcription\"")
            
            // Don't process transcriptions when AURA is speaking to prevent feedback
            if (voiceManager.isSpeaking) {
                Log.i("AURA_VOICE", "Ignoring enhanced voice transcription while AURA is speaking")
                return
            }
            
            if (transcription.isNotBlank()) {
                val filteredTranscription = filterOutAuraVoice(transcription)
                if (filteredTranscription.isNotBlank()) {
                    aiManager.animateTyping(filteredTranscription) {
                        aiManager.processVoiceCommand(filteredTranscription) { actions ->
                            if (actions.isNotEmpty()) {
                                systemManager.startActionQueue(actions)
                            }
                        }
                    }
                }
            }
        }
    }

    private val listeningStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra(EnhancedVoiceService.EXTRA_LISTENING_TYPE) ?: "stopped"
            Log.i("AURA_VOICE", "Listening state changed: $state")
            
            when (state) {
                "wake_word" -> {
                    isListeningForWakeWord = true
                    isRecordingCommand = false
                    listeningType = "wake_word"
                    voiceManager.statusMessage = "👂 Listening for 'Hey Aura'..."
                }
                "command" -> {
                    isListeningForWakeWord = false
                    isRecordingCommand = true
                    listeningType = "command"
                    voiceManager.statusMessage = "🎤 Recording command..."
                }
                "stopped" -> {
                    isListeningForWakeWord = false
                    isRecordingCommand = false
                    listeningType = "stopped"
                    if (voiceManager.statusMessage.contains("Recording") || voiceManager.statusMessage.contains("Listening")) {
                        voiceManager.statusMessage = "Ready for commands"
                    }
                }
            }
        }
    }

    private val audioLevelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(EnhancedVoiceService.EXTRA_AUDIO_LEVEL, 0) ?: 0
            currentAudioLevel = level
        }
    }
    
    fun registerReceivers() {
        try {
            val filter = IntentFilter().apply {
                addAction(AuraAccessibilityService.ACTION_ACTION_RESULT)
                addAction(AudioRecorderService.ACTION_TRANSCRIPTION)
                addAction(AuraAccessibilityService.ACTION_FOREGROUND_PACKAGE)
                addAction(EnhancedVoiceService.ACTION_WAKE_WORD_DETECTED)
                addAction(EnhancedVoiceService.ACTION_VOICE_TRANSCRIPTION)
                addAction(EnhancedVoiceService.ACTION_LISTENING_STATE)
                addAction(EnhancedVoiceService.ACTION_AUDIO_LEVEL)
            }
            
            // Register receivers with NOT_EXPORTED flag (required for API 33+)
            context.registerReceiver(actionResultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(transcriptionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(foregroundPackageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(wakeWordReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(voiceTranscriptionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            
            Log.i("AURA_EVENT", "All broadcast receivers registered successfully")
        } catch (e: Exception) {
            Log.e("AURA_EVENT", "Failed to register broadcast receivers", e)
        }
    }
    
    fun unregisterReceivers() {
        try {
            context.unregisterReceiver(actionResultReceiver)
            context.unregisterReceiver(transcriptionReceiver)
            context.unregisterReceiver(foregroundPackageReceiver)
            context.unregisterReceiver(wakeWordReceiver)
            context.unregisterReceiver(voiceTranscriptionReceiver)
            Log.i("AURA_EVENT", "All broadcast receivers unregistered")
        } catch (e: Exception) {
            Log.e("AURA_EVENT", "Error unregistering broadcast receivers", e)
        }
    }
}
