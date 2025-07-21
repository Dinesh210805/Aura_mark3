package com.aura.aura_mark3.managers

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aura.aura_mark3.utils.AudioFileUtils
import com.aura.aura_mark3.utils.ScreenshotHelper
import com.aura.aura_mark3.ai.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*

/**
 * ENHANCED AI Manager with AURA Backend Integration
 * Now uses intelligent backend orchestration instead of direct API calls
 */
class EnhancedAIManager(
    private val context: Context,
    private val voiceManager: VoiceManager,
    private val backendBaseUrl: String = "http://10.0.2.2:8000/",
    private var systemManager: SystemManager? = null
) {
    // UI State
    var isProcessingRequest by mutableStateOf(false)
        private set
    var hasGreeted by mutableStateOf(false)
    var userTranscription by mutableStateOf("")
    var typingAnimation by mutableStateOf("")
    
    // Screen and context
    var currentForegroundPackage by mutableStateOf("")
    var latestScreenDescription by mutableStateOf("")
    var vlmResultMessage by mutableStateOf("")
    
    // Backend API
    private val backendApi = provideAuraBackendApi(backendBaseUrl)
    private var currentSessionId = generateSessionId()
    
    init {
        // Set up transcription callback
        voiceManager.onTranscriptionReceived = { transcription: String ->
            handleTranscriptionReceived(transcription)
        }
    }
    
    /**
     * Generate unique session ID for conversation tracking
     */
    private fun generateSessionId(): String {
        return "android_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    /**
     * Handle transcription received from voice manager
     */
    private fun handleTranscriptionReceived(transcription: String) {
        Log.i("AURA_AI", "Received transcription: $transcription")
        
        if (transcription.isBlank()) {
            voiceManager.isProcessing = false
            voiceManager.updateVoiceStateExternal()
            voiceManager.statusMessage = "❌ No speech detected - try again"
            voiceManager.speakWithPlayAITts("I didn't hear anything. Could you try again?") {
                if (voiceManager.conversationMode) {
                    voiceManager.shouldStartListeningAfterSpeech = true
                }
            }
            return
        }
        
        // Animate typing and process command
        animateTyping(transcription) {
            processVoiceCommandWithBackend(transcription)
        }
    }
    
    /**
     * Enhanced greeting using backend
     */
    fun greetUserWithBackend() {
        if (isProcessingRequest) return
        
        isProcessingRequest = true
        voiceManager.isProcessing = true
        voiceManager.updateVoiceStateExternal()
        
        // Use backend for intelligent greeting
        val greetingRequest = BackendChatRequest(
            text = "Hello AURA, I just activated you. Give me an enthusiastic greeting!",
            session_id = currentSessionId
        )
        
        backendApi.chatWithText(greetingRequest).enqueue(object : Callback<BackendChatResponse> {
            override fun onResponse(call: Call<BackendChatResponse>, response: Response<BackendChatResponse>) {
                val reply = if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.response ?: getDefaultGreeting()
                } else {
                    Log.w("AURA_BACKEND", "Backend greeting failed: ${response.code()}")
                    getDefaultGreeting()
                }
                
                voiceManager.speakWithPlayAITts(reply) {
                    isProcessingRequest = false
                    hasGreeted = true
                    voiceManager.isProcessing = false
                }
            }
            
            override fun onFailure(call: Call<BackendChatResponse>, t: Throwable) {
                Log.e("AURA_BACKEND", "Backend greeting request failed", t)
                voiceManager.speakWithPlayAITts(getDefaultGreeting()) {
                    isProcessingRequest = false
                    hasGreeted = true
                    voiceManager.isProcessing = false
                }
            }
        })
    }
    
    /**
     * Process voice commands using AURA Backend
     */
    fun processVoiceCommandWithBackend(command: String) {
        if (isProcessingRequest) {
            Log.w("AURA_VOICE", "Already processing a request, ignoring: $command")
            return
        }
        
        Log.i("AURA_VOICE", "Processing command with backend: $command")
        isProcessingRequest = true
        voiceManager.isProcessing = true
        voiceManager.updateVoiceStateExternal()
        
        // For text-only commands, use chat endpoint
        val chatRequest = BackendChatRequest(
            text = command,
            session_id = currentSessionId
        )
        
        voiceManager.statusMessage = "🧠 AURA is thinking..."
        backendApi.chatWithText(chatRequest).enqueue(object : Callback<BackendChatResponse> {
            override fun onResponse(call: Call<BackendChatResponse>, response: Response<BackendChatResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val backendResponse = response.body()!!
                    Log.i("AURA_BACKEND", "Backend response: ${backendResponse.response}")
                    Log.i("AURA_BACKEND", "Intent: ${backendResponse.intent}")
                    
                    // Check if this requires screen interaction
                    if (requiresScreenInteraction(backendResponse.intent, command)) {
                        // Use full voice + screenshot processing
                        processWithScreenshot(command)
                    } else {
                        // Simple text response
                        voiceManager.speakWithPlayAITts(backendResponse.response) {
                            isProcessingRequest = false
                            voiceManager.isProcessing = false
                            voiceManager.updateVoiceStateExternal()
                            
                            if (voiceManager.conversationMode) {
                                voiceManager.shouldStartListeningAfterSpeech = true
                            }
                        }
                    }
                } else {
                    Log.e("AURA_BACKEND", "Backend API error: ${response.code()}")
                    handleBackendError("Backend processing failed. Please try again.")
                }
            }
            
            override fun onFailure(call: Call<BackendChatResponse>, t: Throwable) {
                Log.e("AURA_BACKEND", "Backend request failed", t)
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Request timed out. Please check your connection and try again."
                    t.message?.contains("connection", ignoreCase = true) == true ->
                        "Cannot connect to AURA backend. Please ensure the backend is running."
                    else -> "Backend connection failed. Please try again."
                }
                handleBackendError(errorMessage)
            }
        })
    }
    
    /**
     * Process voice command with screenshot for screen interaction
     */
    private fun processWithScreenshot(command: String) {
        Log.i("AURA_BACKEND", "Processing with screenshot for: $command")
        
        // Take screenshot first
        ScreenshotHelper.takeScreenshot(context) { bitmap: Bitmap? ->
            if (bitmap != null) {
                // Save audio file (if available) and screenshot
                val audioFile = getLastAudioFile() // You'll need to implement this
                val screenshotFile = bitmapToJpegFile(bitmap, context)
                
                if (audioFile != null) {
                    // Use full process endpoint with audio + screenshot
                    processWithAudioAndScreenshot(audioFile, screenshotFile)
                } else {
                    // Create a temporary audio file with the command text for processing
                    // or handle text-only with screenshot
                    handleTextWithScreenshot(command, screenshotFile)
                }
            } else {
                Log.e("AURA_BACKEND", "Failed to capture screenshot")
                handleBackendError("I need to see the screen to help with that action.")
            }
        }
    }
    
    /**
     * Process audio + screenshot through backend
     */
    private fun processWithAudioAndScreenshot(audioFile: File, screenshotFile: File) {
        val audioPart = audioFileToMultipart(audioFile)
        val screenshotPart = screenshotFileToMultipart(screenshotFile)
        val sessionIdBody = currentSessionId.toRequestBody("text/plain".toMediaTypeOrNull())
        
        voiceManager.statusMessage = "📱 Analyzing screen and voice..."
        backendApi.processVoiceAndScreen(audioPart, screenshotPart, sessionIdBody)
            .enqueue(object : Callback<BackendProcessResponse> {
                override fun onResponse(call: Call<BackendProcessResponse>, response: Response<BackendProcessResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val backendResponse = response.body()!!
                        handleBackendProcessResponse(backendResponse)
                    } else {
                        Log.e("AURA_BACKEND", "Process endpoint error: ${response.code()}")
                        handleBackendError("Screen analysis failed. Please try again.")
                    }
                }
                
                override fun onFailure(call: Call<BackendProcessResponse>, t: Throwable) {
                    Log.e("AURA_BACKEND", "Process request failed", t)
                    handleBackendError("Screen analysis failed. Please check your connection.")
                }
            })
    }
    
    /**
     * Handle text command with screenshot
     */
    private fun handleTextWithScreenshot(command: String, screenshotFile: File) {
        // Create a simple wav file with silence or use text-to-speech to create audio
        // For now, fall back to text-only processing
        Log.i("AURA_BACKEND", "Falling back to text-only processing for: $command")
        
        val chatRequest = BackendChatRequest(
            text = "$command (screen interaction needed)",
            session_id = currentSessionId
        )
        
        backendApi.chatWithText(chatRequest).enqueue(object : Callback<BackendChatResponse> {
            override fun onResponse(call: Call<BackendChatResponse>, response: Response<BackendChatResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val reply = response.body()!!.response
                    voiceManager.speakWithPlayAITts(reply) {
                        isProcessingRequest = false
                        voiceManager.isProcessing = false
                        voiceManager.updateVoiceStateExternal()
                    }
                } else {
                    handleBackendError("I couldn't process that screen action.")
                }
            }
            
            override fun onFailure(call: Call<BackendChatResponse>, t: Throwable) {
                handleBackendError("Screen action processing failed.")
            }
        })
        
        // Clean up screenshot file
        screenshotFile.delete()
    }
    
    /**
     * Handle successful backend process response with action plan
     */
    private fun handleBackendProcessResponse(response: BackendProcessResponse) {
        Log.i("AURA_BACKEND", "Processing backend response with ${response.action_plan?.size ?: 0} actions")
        
        // Convert backend action steps to Android actions
        val androidActions = response.action_plan?.map { step ->
            convertBackendActionToAndroid(step)
        } ?: emptyList()
        
        // Speak the response
        voiceManager.speakWithPlayAITts(response.response) {
            isProcessingRequest = false
            voiceManager.isProcessing = false
            voiceManager.updateVoiceStateExternal()
            
            // Execute actions
            if (androidActions.isNotEmpty()) {
                executeAndroidActions(androidActions)
            }
            
            if (voiceManager.conversationMode) {
                voiceManager.shouldStartListeningAfterSpeech = true
            }
        }
    }
    
    /**
     * Convert backend action step to Android action
     */
    private fun convertBackendActionToAndroid(step: ActionStep): AuraAction {
        return when (step.type) {
            "tap" -> AuraAction(
                action = "VLM_ACTION",
                label = "tap",
                text = "${step.x},${step.y}" // Pass coordinates as text
            )
            "type" -> AuraAction(
                action = "VLM_ACTION", 
                label = "type",
                text = step.text ?: ""
            )
            "open_app" -> AuraAction(
                action = "LAUNCH_APP",
                label = step.app_name ?: ""
            )
            "system_command" -> AuraAction(
                action = "SYSTEM_ACTION",
                label = step.description ?: ""
            )
            else -> AuraAction(
                action = "SPEAK",
                label = "",
                text = step.description
            )
        }
    }
    
    /**
     * Execute Android actions
     */
    private fun executeAndroidActions(actions: List<AuraAction>) {
        Log.i("AURA_BACKEND", "Executing ${actions.size} Android actions")
        
        // Execute actions through SystemManager if available
        systemManager?.let { sm ->
            sm.startActionQueue(actions)
        } ?: run {
            Log.w("AURA_BACKEND", "SystemManager not available, logging actions only")
            actions.forEach { action ->
                Log.d("AURA_BACKEND", "Action: ${action.action} - ${action.label}")
            }
        }
    }
    
    /**
     * Set system manager for action execution
     */
    fun setSystemManager(systemManager: SystemManager) {
        this.systemManager = systemManager
    }
    
    /**
     * Check if command requires screen interaction
     */
    private fun requiresScreenInteraction(intent: String?, command: String): Boolean {
        val screenKeywords = listOf(
            "tap", "click", "press", "button", "menu", "settings",
            "open", "navigate", "scroll", "swipe", "type", "enter",
            "compose", "send", "email", "message", "app"
        )
        
        val commandLower = command.lowercase()
        val intentLower = intent?.lowercase() ?: ""
        
        return screenKeywords.any { keyword ->
            commandLower.contains(keyword) || intentLower.contains(keyword)
        }
    }
    
    /**
     * Get the last recorded audio file
     */
    private fun getLastAudioFile(): File? {
        return AudioFileUtils.getLastAudioFile(context)
    }
    
    /**
     * Create a bitmap to JPEG file for backend processing
     */
    private fun bitmapToJpegFile(bitmap: Bitmap, context: Context): File {
        return AudioFileUtils.bitmapToJpegFile(bitmap, context)
    }
    
    /**
     * Handle backend errors
     */
    private fun handleBackendError(message: String) {
        voiceManager.speakWithPlayAITts(message) {
            isProcessingRequest = false
            voiceManager.isProcessing = false
            voiceManager.updateVoiceStateExternal()
            
            if (voiceManager.conversationMode) {
                voiceManager.shouldStartListeningAfterSpeech = true
            }
        }
    }
    
    /**
     * Get default greeting
     */
    private fun getDefaultGreeting(): String {
        return "Hello! I'm AURA, your enhanced voice assistant with intelligent backend processing! I can help you with device control, app management, and much more. What would you like me to do?"
    }
    
    /**
     * Animate typing effect for user input
     */
    fun animateTyping(text: String, onComplete: () -> Unit) {
        var currentIndex = 0
        val handler = Handler(Looper.getMainLooper())
        
        fun typeNextCharacter() {
            if (currentIndex < text.length) {
                typingAnimation = text.substring(0, currentIndex + 1)
                currentIndex++
                handler.postDelayed({ typeNextCharacter() }, 50)
            } else {
                userTranscription = text
                typingAnimation = ""
                handler.postDelayed(onComplete, 300)
            }
        }
        
        typeNextCharacter()
    }
    
    /**
     * Check backend health
     */
    fun checkBackendHealth(onResult: (Boolean) -> Unit) {
        backendApi.getHealth().enqueue(object : Callback<BackendHealthResponse> {
            override fun onResponse(call: Call<BackendHealthResponse>, response: Response<BackendHealthResponse>) {
                val isHealthy = response.isSuccessful && 
                               response.body()?.status == "operational"
                onResult(isHealthy)
            }
            
            override fun onFailure(call: Call<BackendHealthResponse>, t: Throwable) {
                onResult(false)
            }
        })
    }
    
    /**
     * Set processing state
     */
    private fun setProcessingState(processing: Boolean) {
        isProcessingRequest = processing
        voiceManager.isProcessing = processing
        voiceManager.updateVoiceStateExternal()
    }
}

// Keep the original AuraAction for compatibility
data class AuraAction(
    val action: String,
    val label: String = "",
    val text: String? = null,
    val direction: String? = null,
    val query: String? = null
)
