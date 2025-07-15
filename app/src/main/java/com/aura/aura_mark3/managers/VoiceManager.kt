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
    
    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val ttsQueue = mutableListOf<String>()
    
    // Timeout handling
    private var speakingTimeoutHandler: Handler? = null
    private val SPEAKING_TIMEOUT = 30000L // 30 seconds timeout
    
    // Settings
    var ttsFeedbackEnabled by mutableStateOf(true)
    var ttsLanguage by mutableStateOf("en")
    var selectedVoice by mutableStateOf("Arista-PlayAI")
    var ttsSpeed by mutableFloatStateOf(1.2f)
    
    // Conversation flow
    var shouldStartListeningAfterSpeech by mutableStateOf(false)
    var conversationMode by mutableStateOf(false)
    
    fun initializeTts(onReady: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                tts?.language = Locale.forLanguageTag(ttsLanguage)
                Log.i("AURA_TTS", "VoiceManager TTS initialized successfully.")
                processTtsQueue()
                onReady()
            } else {
                Log.e("AURA_TTS", "VoiceManager TTS initialization failed with status: $status")
                ttsReady = false
            }
        }
    }
    
    fun speakWithPlayAITts(message: String, onDone: (() -> Unit)? = null) {
        if (!ttsFeedbackEnabled) {
            onDone?.invoke()
            return
        }

        // Set speaking state to prevent recording interference
        isSpeaking = true
        startSpeakingTimeout()
        assistantSpeech = message

        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            Log.w("AURA_TTS", "No API key found, falling back to local TTS")
            speakingTimeoutHandler?.removeCallbacksAndMessages(null)
            speakIfReady(message)
            assistantSpeech = ""
            isSpeaking = false
            onDone?.invoke()
            return
        }

        val playAITtsApi = providePlayAITtsApi()
        val request = PlayAITtsRequest(
            input = message,
            voice = selectedVoice,
            response_format = "mp3",
            speed = ttsSpeed
        )

        playAITtsApi.synthesizeSpeech(apiKey, request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        AudioPlayer.playAudioFromResponse(context, responseBody, onComplete = {
                            speakingTimeoutHandler?.removeCallbacksAndMessages(null)
                            assistantSpeech = ""
                            isSpeaking = false
                            
                            if (shouldStartListeningAfterSpeech && conversationMode) {
                                shouldStartListeningAfterSpeech = false
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startManualRecordingForConversation()
                                }, 500)
                            }
                            
                            onDone?.invoke()
                        }, onError = {
                            Log.e("AURA_TTS", "Error playing PlayAI TTS audio, falling back to local TTS")
                            speakingTimeoutHandler?.removeCallbacksAndMessages(null)
                            speakIfReady(message)
                            assistantSpeech = ""
                            isSpeaking = false
                            onDone?.invoke()
                        })
                    } ?: run {
                        Log.e("AURA_TTS", "PlayAI TTS response body is null")
                        speakingTimeoutHandler?.removeCallbacksAndMessages(null)
                        speakIfReady(message)
                        assistantSpeech = ""
                        isSpeaking = false
                        onDone?.invoke()
                    }
                } else {
                    Log.e("AURA_TTS", "PlayAI TTS API error: ${response.code()} ${response.message()}")
                    speakingTimeoutHandler?.removeCallbacksAndMessages(null)
                    speakIfReady(message)
                    assistantSpeech = ""
                    isSpeaking = false
                    onDone?.invoke()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("AURA_TTS", "PlayAI TTS API call failed: ${t.localizedMessage}")
                speakingTimeoutHandler?.removeCallbacksAndMessages(null)
                speakIfReady(message)
                assistantSpeech = ""
                isSpeaking = false
                onDone?.invoke()
            }
        })
    }
    
    /**
     * Reset speaking state - force stop TTS and reset flags
     */
    fun resetSpeakingState() {
        Log.i("AURA_VOICE", "Force resetting speaking state")
        isSpeaking = false
        assistantSpeech = ""
        speakingTimeoutHandler?.removeCallbacksAndMessages(null)
        
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w("AURA_TTS", "Error stopping TTS: ${e.message}")
        }
    }
    
    /**
     * Start speaking timeout - automatically reset speaking state after timeout
     */
    private fun startSpeakingTimeout() {
        speakingTimeoutHandler?.removeCallbacksAndMessages(null)
        speakingTimeoutHandler = Handler(Looper.getMainLooper())
        speakingTimeoutHandler?.postDelayed({
            if (isSpeaking) {
                Log.w("AURA_VOICE", "Speaking timeout reached, force resetting state")
                resetSpeakingState()
                statusMessage = "Ready to listen"
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
            Log.i("AURA_VOICE", "Enhanced Voice Service stopped")
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Error stopping Enhanced Voice Service", e)
        }
    }
    
    /**
     * Start manual recording
     */
    fun startManualRecording() {
        if (isSpeaking || isRecording) {
            Log.w("AURA_VOICE", "Cannot start recording: isSpeaking=$isSpeaking, isRecording=$isRecording")
            return
        }
        
        if (!micPermissionProvider()) {
            statusMessage = "Microphone permission required"
            speakWithPlayAITts("Please grant microphone permission to use voice commands.")
            return
        }
        
        try {
            conversationMode = true
            Log.i("AURA_VOICE", "Starting manual recording")
            
            statusMessage = "🎤 Starting recording..."
            
            val recordIntent = Intent(context, AudioRecorderService::class.java)
            recordIntent.putExtra("isManualActivation", true)
            
            val componentName = run {
                Log.i("AURA_VOICE", "Using startForegroundService for Android O+")
                context.startForegroundService(recordIntent)
            }
            
            if (componentName != null) {
                isRecording = true
                statusMessage = "🎤 Listening... Speak now"
                Log.i("AURA_VOICE", "Recording service started successfully - componentName: $componentName")
            } else {
                Log.e("AURA_VOICE", "Failed to start recording service - componentName is null")
                statusMessage = "Failed to start recording"
            }
            
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Exception starting recording service", e)
            statusMessage = "Recording service error: ${e.message}"
        }
    }
    
    /**
     * Stop manual recording
     */
    fun stopManualRecording() {
        Log.i("AURA_VOICE", "Stopping manual recording")
        val stopIntent = Intent(context, AudioRecorderService::class.java)
        context.stopService(stopIntent)
        isRecording = false
        statusMessage = "Processing..."
    }
    
    /**
     * Start manual recording for continuous conversation
     */
    private fun startManualRecordingForConversation() {
        if (isSpeaking || isRecording) {
            Log.w("AURA_VOICE", "Cannot start recording: isSpeaking=$isSpeaking, isRecording=$isRecording")
            return
        }
        
        if (!micPermissionProvider()) {
            statusMessage = "Microphone permission required"
            speakWithPlayAITts("Please grant microphone permission to use voice commands.")
            return
        }
        
        try {
            Log.i("AURA_VOICE", "Starting conversation recording")
            val recordIntent = Intent(context, AudioRecorderService::class.java)
            
            val componentName = context.startForegroundService(recordIntent)
            
            if (componentName != null) {
                isRecording = true
                statusMessage = "🎤 Listening..."
                Log.i("AURA_VOICE", "Conversation recording started successfully")
            } else {
                Log.e("AURA_VOICE", "Failed to start conversation recording service")
                statusMessage = "Failed to start listening"
            }
            
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Exception starting conversation recording", e)
            statusMessage = "Recording error"
        }
    }
    
    private fun speakIfReady(message: String) {
        if (ttsReady && ttsFeedbackEnabled) {
            speakInternal(message)
        } else if (ttsFeedbackEnabled) {
            ttsQueue.add(message)
        }
    }

    private fun speakInternal(message: String) {
        try {
            if (tts != null && ttsReady && ttsFeedbackEnabled) {
                val result = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "AURA_ACTION_RESULT")
                if (result == TextToSpeech.ERROR) {
                    Log.e("AURA_TTS", "VoiceManager TTS speak failed for message: \"$message\"")
                } else {
                    Log.i("AURA_TTS", "VoiceManager TTS speak successful for message: \"$message\"")
                }
            } else {
                Log.w("AURA_TTS", "VoiceManager TTS not available for message: \"$message\"")
            }
        } catch (e: Exception) {
            Log.e("AURA_TTS", "VoiceManager TTS speak exception: ${e.localizedMessage}")
        }
    }

    private fun processTtsQueue() {
        while (ttsQueue.isNotEmpty() && ttsReady) {
            val message = ttsQueue.removeAt(0)
            speakInternal(message)
        }
    }
    
    /**
     * Preview the selected voice
     */
    fun previewVoice(voice: String) {
        try {
            val previewText = "Hello, this is how I sound with this voice setting."
            speakInternal(previewText)
            Log.d("AURA_TTS", "Playing voice preview for: $voice")
        } catch (e: Exception) {
            Log.e("AURA_TTS", "Error playing voice preview: ${e.localizedMessage}")
        }
    }
    
    fun cleanup() {
        speakingTimeoutHandler?.removeCallbacksAndMessages(null)
        speakingTimeoutHandler = null
        tts?.shutdown()
        if (isVoiceServiceRunning) {
            stopEnhancedVoiceService()
        }
    }
}
