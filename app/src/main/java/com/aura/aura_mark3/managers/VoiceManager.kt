package com.aura.aura_mark3.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aura.aura_mark3.ai.PlayAITtsRequest
import com.aura.aura_mark3.ai.providePlayAITtsApi
import com.aura.aura_mark3.audio.AudioRecorderService
import com.aura.aura_mark3.audio.EnhancedVoiceService
import com.aura.aura_mark3.utils.AudioPlayer
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * Manages voice-related functionality including TTS, recording, and voice service
 */
class VoiceManager(
    private val context: Context,
    private val apiKeyProvider: () -> String,
    private val micPermissionProvider: () -> Boolean
) {
    // Voice states
    var isSpeaking by mutableStateOf(false)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var isVoiceServiceRunning by mutableStateOf(false)
        private set
    var assistantSpeech by mutableStateOf("")
    var statusMessage by mutableStateOf("")
    
    // Current voice state for proper UI control
    var currentVoiceState by mutableStateOf(VoiceState.IDLE)
        private set
    
    // Manual recording state tracking
    var isManualRecording by mutableStateOf(false)
        private set
    
    // Conversation mode for continuous listening
    var conversationMode by mutableStateOf(false)
    
    // TTS Settings
    var ttsFeedbackEnabled by mutableStateOf(true)
    var ttsLanguage by mutableStateOf("en")
    var selectedVoice by mutableStateOf("Arista-PlayAI")
    var ttsSpeed by mutableFloatStateOf(1.2f)
    
    // Cooldown for API calls to prevent 429 errors
    private var lastApiCallTime = 0L
    private val API_COOLDOWN_MS = 2000L // 2 seconds between API calls
    
    // TTS components
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val audioPlayer = AudioPlayer(context)
    
    // Handlers for timeout management
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speakingTimeoutHandler: Handler? = null
    private val SPEAKING_TIMEOUT = 30000L // 30 seconds max speaking time
    
    /**
     * Initialize TTS with callback for when ready
     */
    fun initializeTts(onInitialized: () -> Unit) {
        Log.i("AURA_TTS", "Initializing TTS...")
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                Log.i("AURA_TTS", "TTS initialized successfully")
                updateVoiceState()
                onInitialized()
            } else {
                Log.e("AURA_TTS", "TTS initialization failed")
                statusMessage = "TTS initialization failed"
            }
        }
    }
    
    /**
     * Check if we can make an API call (rate limiting)
     */
    private fun canMakeApiCall(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastApiCallTime >= API_COOLDOWN_MS
    }
    
    /**
     * Update the current voice state based on various conditions
     */
    private fun updateVoiceState() {
        val newState = when {
            isSpeaking -> VoiceState.SPEAKING
            isRecording && isManualRecording -> VoiceState.RECORDING_MANUAL
            isRecording -> VoiceState.RECORDING_COMMAND
            isVoiceServiceRunning -> VoiceState.LISTENING_WAKE_WORD
            else -> VoiceState.IDLE
        }
        
        if (currentVoiceState != newState) {
            Log.d("AURA_STATE", "Voice state changed: ${currentVoiceState} -> $newState")
            currentVoiceState = newState
        }
    }
    
    /**
     * Speak text using PlayAI TTS with callback
     */
    fun speakWithPlayAITts(text: String, onComplete: (() -> Unit)? = null) {
        if (!ttsFeedbackEnabled) {
            Log.i("AURA_TTS", "TTS feedback disabled, skipping speech")
            onComplete?.invoke()
            return
        }
        
        if (text.isBlank()) {
            Log.w("AURA_TTS", "Empty text provided for TTS")
            onComplete?.invoke()
            return
        }
        
        // Check cooldown
        if (!canMakeApiCall()) {
            Log.w("AURA_TTS", "API call too soon, using fallback TTS")
            speakWithFallbackTts(text, onComplete)
            return
        }
        
        isSpeaking = true
        assistantSpeech = text
        updateVoiceState()
        
        // Start speaking timeout
        startSpeakingTimeout()
        
        lastApiCallTime = System.currentTimeMillis()
        
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            Log.w("AURA_TTS", "No API key available, using fallback TTS")
            speakWithFallbackTts(text, onComplete)
            return
        }
        
        val request = PlayAITtsRequest(
            text = text,
            voice = selectedVoice,
            speed = ttsSpeed.toString()
        )
        
        val ttsApi = providePlayAITtsApi()
        ttsApi.textToSpeech(apiKey, request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { audioData ->
                        audioPlayer.playAudio(audioData.bytes()) {
                            finishSpeaking(onComplete)
                        }
                    } ?: run {
                        Log.e("AURA_TTS", "Empty response body from PlayAI")
                        speakWithFallbackTts(text, onComplete)
                    }
                } else {
                    Log.e("AURA_TTS", "PlayAI TTS failed: ${response.code()}")
                    speakWithFallbackTts(text, onComplete)
                }
            }
            
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("AURA_TTS", "PlayAI TTS request failed", t)
                speakWithFallbackTts(text, onComplete)
            }
        })
    }
    
    /**
     * Fallback TTS using system TTS
     */
    private fun speakWithFallbackTts(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsInitialized || tts == null) {
            Log.w("AURA_TTS", "TTS not initialized, cannot speak")
            finishSpeaking(onComplete)
            return
        }
        
        isSpeaking = true
        assistantSpeech = text
        updateVoiceState()
        
        try {
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AURA_SPEECH")
            if (result == TextToSpeech.SUCCESS) {
                // Estimate speaking time (roughly 150 words per minute)
                val estimatedDuration = (text.split(" ").size * 400L) + 1000L
                mainHandler.postDelayed({
                    finishSpeaking(onComplete)
                }, estimatedDuration)
            } else {
                Log.e("AURA_TTS", "System TTS speak failed")
                finishSpeaking(onComplete)
            }
        } catch (e: Exception) {
            Log.e("AURA_TTS", "Error in fallback TTS", e)
            finishSpeaking(onComplete)
        }
    }
    
    /**
     * Complete speaking process and reset state
     */
    private fun finishSpeaking(onComplete: (() -> Unit)? = null) {
        mainHandler.post {
            isSpeaking = false
            assistantSpeech = ""
            updateVoiceState()
            speakingTimeoutHandler?.removeCallbacksAndMessages(null)
            
            if (conversationMode && !isRecording) {
                statusMessage = "Ready to listen"
                startManualRecordingForConversation()
            } else {
                statusMessage = "Ready for commands"
            }
            
            onComplete?.invoke()
        }
    }
    
    /**
     * Start speaking timeout to prevent stuck states
     */
    private fun startSpeakingTimeout() {
        speakingTimeoutHandler?.removeCallbacksAndMessages(null)
        speakingTimeoutHandler = Handler(Looper.getMainLooper())
        speakingTimeoutHandler?.postDelayed({
            if (isSpeaking) {
                Log.w("AURA_VOICE", "Speaking timeout reached, force resetting state")
                resetSpeakingState()
            }
        }, SPEAKING_TIMEOUT)
    }
    
    /**
     * Start Enhanced Voice Service for continuous listening
     */
    fun startEnhancedVoiceService() {
        try {
            if (isVoiceServiceRunning) {
                Log.i("AURA_VOICE", "Enhanced Voice Service already running")
                return
            }
            
            val serviceIntent = Intent(context, EnhancedVoiceService::class.java)
            val result = context.startForegroundService(serviceIntent)
            Log.i("AURA_VOICE", "Enhanced Voice Service startForegroundService result: $result")
            isVoiceServiceRunning = true
            statusMessage = "Voice service starting..."
            updateVoiceState()
            Log.i("AURA_VOICE", "Enhanced Voice Service start requested")
            
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Failed to start Enhanced Voice Service", e)
            isVoiceServiceRunning = false
            statusMessage = "Voice service failed to start"
        }
    }
    
    /**
     * Stop Enhanced Voice Service
     */
    fun stopEnhancedVoiceService() {
        try {
            val serviceIntent = Intent(context, EnhancedVoiceService::class.java)
            context.stopService(serviceIntent)
            isVoiceServiceRunning = false
            statusMessage = "Voice service stopped"
            updateVoiceState()
            Log.i("AURA_VOICE", "Enhanced Voice Service stopped")
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Error stopping Enhanced Voice Service", e)
        }
    }
    
    /**
     * Start manual recording initiated by user button press
     */
    fun startManualRecording() {
        if (isSpeaking) {
            Log.w("AURA_VOICE", "Cannot start recording while AURA is speaking")
            statusMessage = "Please wait for AURA to finish speaking"
            return
        }
        
        if (isManualRecording || isRecording) {
            Log.w("AURA_VOICE", "Already recording")
            return
        }
        
        if (!micPermissionProvider()) {
            statusMessage = "Microphone permission required"
            speakWithPlayAITts("Please grant microphone permission to use voice commands.")
            return
        }
        
        // Check API cooldown
        if (!canMakeApiCall()) {
            statusMessage = "Please wait a moment before recording again"
            return
        }
        
        try {
            conversationMode = true
            isManualRecording = true
            isRecording = true
            updateVoiceState()
            
            Log.i("AURA_VOICE", "Starting manual recording")
            statusMessage = "🎤 Starting recording..."
            
            val recordIntent = Intent(context, AudioRecorderService::class.java)
            recordIntent.putExtra("isManualActivation", true)
            
            val componentName = context.startForegroundService(recordIntent)
            Log.i("AURA_VOICE", "AudioRecorderService started: $componentName")
            
            statusMessage = "🎤 Recording - speak now"
            
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Failed to start manual recording", e)
            isManualRecording = false
            isRecording = false
            updateVoiceState()
            statusMessage = "Failed to start recording"
        }
    }
    
    /**
     * Stop manual recording with proper cleanup
     */
    fun stopManualRecording() {
        Log.i("AURA_VOICE", "Stopping manual recording")
        try {
            val stopIntent = Intent(context, AudioRecorderService::class.java)
            context.stopService(stopIntent)
            isRecording = false
            isManualRecording = false
            conversationMode = false
            updateVoiceState()
            statusMessage = "Recording stopped"
            Log.i("AURA_VOICE", "Manual recording stopped successfully")
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Error stopping manual recording", e)
            isRecording = false
            isManualRecording = false
            updateVoiceState()
            statusMessage = "Recording stopped"
        }
    }
    
    /**
     * Start recording for conversation mode after AURA finishes speaking
     */
    private fun startManualRecordingForConversation() {
        if (!conversationMode || isRecording || isSpeaking) {
            return
        }
        
        Log.d("AURA_VOICE", "Starting conversation recording")
        startManualRecording()
    }
    
    /**
     * Reset speaking state - force stop TTS and reset flags
     */
    fun resetSpeakingState() {
        Log.i("AURA_VOICE", "Force resetting speaking state")
        isSpeaking = false
        assistantSpeech = ""
        speakingTimeoutHandler?.removeCallbacksAndMessages(null)
        updateVoiceState()
        
        try {
            tts?.stop()
            audioPlayer.stop()
        } catch (e: Exception) {
            Log.w("AURA_TTS", "Error stopping TTS: ${e.message}")
        }
        
        statusMessage = "Ready for commands"
    }
    
    /**
     * Handle when wake word is detected
     */
    fun onWakeWordDetected() {
        if (isSpeaking) {
            Log.i("AURA_VOICE", "Wake word detected while speaking - interrupting")
            resetSpeakingState()
        }
        
        Log.i("AURA_VOICE", "Wake word detected - ready for command")
        statusMessage = "🎤 Wake word detected - speak your command"
    }
    
    /**
     * Preview a voice by speaking a test phrase
     */
    fun previewVoice(voice: String) {
        val originalVoice = selectedVoice
        selectedVoice = voice
        speakWithPlayAITts("This is how I sound with the $voice voice.") {
            // Restore original voice if user doesn't change it
            if (selectedVoice == voice) {
                selectedVoice = originalVoice
            }
        }
    }
    
    /**
     * Get current recording status for UI button state
     */
    fun getRecordingStatus(): RecordingStatus {
        return when (currentVoiceState) {
            VoiceState.IDLE -> RecordingStatus(
                state = currentVoiceState,
                buttonText = "Tap to Talk",
                canStartRecording = !isSpeaking,
                canStopRecording = false,
                statusMessage = "Ready to listen"
            )
            VoiceState.LISTENING_WAKE_WORD -> RecordingStatus(
                state = currentVoiceState,
                buttonText = "Tap to Talk",
                canStartRecording = !isSpeaking,
                canStopRecording = false,
                statusMessage = "Listening for wake word"
            )
            VoiceState.RECORDING_MANUAL -> RecordingStatus(
                state = currentVoiceState,
                buttonText = "Stop Recording",
                canStartRecording = false,
                canStopRecording = true,
                statusMessage = "Recording your command"
            )
            VoiceState.RECORDING_COMMAND -> RecordingStatus(
                state = currentVoiceState,
                buttonText = "Stop Recording",
                canStartRecording = false,
                canStopRecording = true,
                statusMessage = "Recording command"
            )
            VoiceState.SPEAKING -> RecordingStatus(
                state = currentVoiceState,
                buttonText = "Interrupt",
                canStartRecording = false,
                canStopRecording = false,
                statusMessage = "AURA is speaking"
            )
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.i("AURA_VOICE", "Cleaning up VoiceManager")
        
        try {
            // Stop all services
            stopEnhancedVoiceService()
            stopManualRecording()
            
            // Stop TTS
            tts?.stop()
            tts?.shutdown()
            
            // Stop audio player
            audioPlayer.stop()
            
            // Clear handlers
            speakingTimeoutHandler?.removeCallbacksAndMessages(null)
            mainHandler.removeCallbacksAndMessages(null)
            
            // Reset states
            isSpeaking = false
            isRecording = false
            isVoiceServiceRunning = false
            isManualRecording = false
            conversationMode = false
            currentVoiceState = VoiceState.IDLE
            
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Error during cleanup", e)
        }
    }
}
