package com.aura.aura_mark3.managers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aura.aura_mark3.ai.*
import com.aura.aura_mark3.utils.ScreenshotHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages AI-related functionality including LLM conversations and VLM processing
 */
class AIManager(
    private val context: Context,
    private val apiKeyProvider: () -> String,
    private val voiceManager: VoiceManager
) {
    // Conversation state
    var isProcessingRequest by mutableStateOf(false)
        private set
    var hasGreeted by mutableStateOf(false)
    var userTranscription by mutableStateOf("")
    var typingAnimation by mutableStateOf("")
    
    // Screen and context
    var currentForegroundPackage by mutableStateOf("")
    var latestScreenDescription by mutableStateOf("")
    var vlmResultMessage by mutableStateOf("")
    
    // Conversation history
    private val conversationHistory = mutableListOf<CompoundMessage>()
    
    /**
     * Enhanced greeting using Compound Beta API with real-world awareness
     */
    fun greetUserWithCompoundBeta() {
        if (isProcessingRequest) return
        
        isProcessingRequest = true
        voiceManager.statusMessage = "✨ Generating your greeting..."
        voiceManager.assistantSpeech = "Generating your greeting..."
        
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            Log.w("AURA_LLM", "No API key found, using fallback greeting")
            voiceManager.statusMessage = "No API key found. Using fallback greeting."
            voiceManager.speakWithPlayAITts("Hello, Joyboy! I'm AURA, your voice assistant. I'm excited to help you today! What would you like to do?") {
                isProcessingRequest = false
            }
            return
        }
        
        val compoundApi = provideCompoundBetaApi()
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val systemPrompt = """You are AURA, an enthusiastic and intelligent voice assistant for Joyboy. You have access to real-world information and can help with device control, app management, web search, and more. 

        Greet Joyboy warmly with personality and energy. Mention some of your capabilities briefly. Keep it conversational and exciting - make Joyboy feel like they have an amazing AI companion. Current time is $currentTime."""
        
        val request = CompoundRequest(
            model = "compound-beta",
            messages = listOf(
                CompoundMessage(role = "system", content = systemPrompt),
                CompoundMessage(role = "user", content = "Hello AURA, I just activated you. Give me an enthusiastic greeting!")
            ),
            maxTokens = 150,
            temperature = 0.9f
        )
        
        compoundApi.chatCompletion(apiKey, request).enqueue(object : Callback<CompoundResponse> {
            override fun onResponse(call: Call<CompoundResponse>, response: Response<CompoundResponse>) {
                val reply = if (response.isSuccessful) {
                    response.body()?.choices?.firstOrNull()?.message?.content ?: 
                    "Hello, Joyboy! I'm AURA, your amazing voice assistant! I'm absolutely thrilled to be here and ready to help you with anything you need. Let's make some magic happen!"
                } else {
                    Log.w("AURA_LLM", "Compound Beta API returned error: "+response.code())
                    "Sorry, I couldn't generate a greeting right now. Please try again later."
                }
                
                conversationHistory.add(CompoundMessage(role = "assistant", content = reply))
                voiceManager.statusMessage = "💬 Responding..."
                voiceManager.speakWithPlayAITts(reply) {
                    isProcessingRequest = false
                    voiceManager.statusMessage = "Ready for commands! Say 'Hey Aura' or tap microphone"
                }
            }
            
            override fun onFailure(call: Call<CompoundResponse>, t: Throwable) {
                Log.e("AURA_LLM", "Compound Beta greeting failed", t)
                voiceManager.statusMessage = "Greeting failed: ${t.localizedMessage}"
                voiceManager.speakWithPlayAITts("Sorry, I couldn't generate a greeting right now. Please try again later.") {
                    isProcessingRequest = false
                    voiceManager.statusMessage = "Ready for commands! Say 'Hey Aura' or tap microphone"
                }
            }
        })
    }
    
    /**
     * Process voice commands using Compound Beta API with real-world data access
     */
    fun processVoiceCommand(command: String, onActionsParsed: (List<AuraAction>) -> Unit) {
        if (isProcessingRequest) {
            Log.w("AURA_VOICE", "Already processing a request, ignoring: $command")
            voiceManager.statusMessage = "Busy processing previous request..."
            return
        }
        
        Log.i("AURA_VOICE", "Processing command: $command")
        isProcessingRequest = true
        voiceManager.statusMessage = "🤔 Thinking..."
        voiceManager.assistantSpeech = "Processing your command..."
        
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            Log.e("AURA_LLM", "No API key available")
            voiceManager.statusMessage = "No API key available."
            voiceManager.speakWithPlayAITts("I'm sorry, I can't process requests without an API key.") {
                isProcessingRequest = false
                voiceManager.statusMessage = "Ready for next command"
                
                if (voiceManager.conversationMode) {
                    voiceManager.shouldStartListeningAfterSpeech = true
                }
            }
            return
        }
        
        conversationHistory.add(CompoundMessage(role = "user", content = command))
        val compoundApi = provideCompoundBetaApi()
        val systemPrompt = buildSystemPromptWithContext()
        
        val messages = mutableListOf<CompoundMessage>().apply {
            add(CompoundMessage(role = "system", content = systemPrompt))
            addAll(conversationHistory.takeLast(8))
        }
        
        val request = CompoundRequest(
            model = "compound-beta",
            messages = messages,
            maxTokens = 500,
            temperature = 0.7f
        )
        
        Log.i("AURA_LLM", "Sending request to Compound Beta API")
        voiceManager.statusMessage = "🧠 Talking to my brain..."
        compoundApi.chatCompletion(apiKey, request).enqueue(object : Callback<CompoundResponse> {
            override fun onResponse(call: Call<CompoundResponse>, response: Response<CompoundResponse>) {
                if (response.isSuccessful) {
                    val reply = response.body()?.choices?.firstOrNull()?.message?.content
                    if (!reply.isNullOrBlank()) {
                        Log.i("AURA_LLM", "Received response: $reply")
                        conversationHistory.add(CompoundMessage(role = "assistant", content = reply))
                        
                        // Parse actions first
                        val actions = parseActions(reply)
                        onActionsParsed(actions)
                        
                        val cleanReply = reply.replace(Regex("\\[ACTION:[^\\]]*\\]"), "").trim()
                        voiceManager.statusMessage = "💬 Responding..."
                        
                        voiceManager.speakWithPlayAITts(cleanReply) {
                            isProcessingRequest = false
                            voiceManager.statusMessage = "Ready for next command"
                            
                            if (voiceManager.conversationMode) {
                                voiceManager.shouldStartListeningAfterSpeech = true
                            }
                        }
                    } else {
                        Log.w("AURA_LLM", "Empty response from API")
                        fallbackResponse()
                    }
                } else {
                    Log.e("AURA_LLM", "API error: ${response.code()} - ${response.message()}")
                    voiceManager.statusMessage = "Sorry, I couldn't process that. (${response.code()})"
                    fallbackResponse()
                }
            }
            
            override fun onFailure(call: Call<CompoundResponse>, t: Throwable) {
                Log.e("AURA_LLM", "Compound Beta API call failed", t)
                voiceManager.statusMessage = "Sorry, I couldn't process that. (${t.localizedMessage})"
                fallbackResponse()
            }
        })
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
     * Send a screenshot to VLM for analysis
     */
    fun sendScreenshotToVlm(
        bitmap: Bitmap,
        query: String,
        vlmAction: String = "CLICK",
        direction: String = "DOWN",
        text: String = "",
        onResult: (coordinates: VlmCoordinates?) -> Unit
    ) {
        val apiKey = apiKeyProvider()
        val vlmApi = provideGroqVlmApi()
        val file = bitmapToJpegFile(bitmap)
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val modelPart = "llama-4-maverick-17b-128e-instruct".toRequestBody("text/plain".toMediaTypeOrNull())
        val queryPart = query.toRequestBody("text/plain".toMediaTypeOrNull())
        
        vlmResultMessage = "Sending screenshot to VLM..."
        vlmApi.locateUiElement(apiKey, imagePart, modelPart, queryPart).enqueue(object : Callback<VlmResponse> {
            override fun onResponse(call: Call<VlmResponse>, response: Response<VlmResponse>) {
                if (response.isSuccessful) {
                    val coords = response.body()?.coordinates
                    if (coords != null) {
                        vlmResultMessage = "VLM found: x=${coords.x}, y=${coords.y}, w=${coords.width}, h=${coords.height}"
                        onResult(coords)
                    } else {
                        vlmResultMessage = "VLM: No coordinates found."
                        onResult(null)
                    }
                } else {
                    vlmResultMessage = "VLM API error: ${response.code()} ${response.message()}"
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<VlmResponse>, t: Throwable) {
                vlmResultMessage = "VLM API call failed: ${t.localizedMessage}"
                onResult(null)
            }
        })
    }
    
    /**
     * Get VLM screen description
     */
    fun getVlmScreenDescription(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val apiKey = apiKeyProvider()
        val vlmApi = provideGroqVlmApi()
        val file = bitmapToJpegFile(bitmap)
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val modelPart = "llama-4-maverick-17b-128e-instruct".toRequestBody("text/plain".toMediaTypeOrNull())
        val queryPart = "Describe the current screen".toRequestBody("text/plain".toMediaTypeOrNull())
        
        vlmApi.locateUiElement(apiKey, imagePart, modelPart, queryPart).enqueue(object : Callback<VlmResponse> {
            override fun onResponse(call: Call<VlmResponse>, response: Response<VlmResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val desc = try {
                        body?.javaClass?.getDeclaredField("description")?.let { field ->
                            field.isAccessible = true
                            field.get(body) as? String
                        }
                    } catch (e: Exception) { null }
                    onResult(desc ?: body?.toString())
                } else {
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<VlmResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }
    
    private fun fallbackResponse() {
        val responses = listOf(
            "I'm sorry, I couldn't process that request right now. Could you try asking me again?",
            "Hmm, I'm having a little trouble with that. Please try your request once more.",
            "Oops! Something went wrong on my end. Could you repeat that for me?",
            "I didn't quite catch that. Let's try again - what can I help you with?"
        )
        val randomResponse = responses.random()
        voiceManager.speakWithPlayAITts(randomResponse) {
            isProcessingRequest = false
            voiceManager.statusMessage = "Ready for next command"
            
            if (voiceManager.conversationMode) {
                voiceManager.shouldStartListeningAfterSpeech = true
            }
        }
    }
    
    private fun buildSystemPromptWithContext(): String {
        val currentApp = if (currentForegroundPackage.isNotBlank()) {
            "Currently active app: $currentForegroundPackage"
        } else {
            "No app information available"
        }
        
        val screenInfo = if (latestScreenDescription.isNotBlank()) {
            "Latest screen description: $latestScreenDescription"
        } else {
            "No screen information available"
        }
        
        return """You are AURA, a lively and intelligent voice assistant for Android devices. You're enthusiastic, helpful, and conversational. You have access to real-world information via web search and can help with:

                DEVICE CONTROL ACTIONS:
                - System settings: WiFi, Bluetooth, brightness, volume
                - App control: open apps, navigate interfaces
                - Screen interaction: click, scroll, input text

                INFORMATION ACCESS:
                - Web search for current information
                - Weather, news, facts, calculations
                - Screen analysis and description
                - Real-time data access

                CURRENT CONTEXT:
                - $currentApp
                - $screenInfo

                PERSONALITY:
                - Be enthusiastic and energetic in your responses
                - Show personality - be friendly, helpful, and sometimes playful
                - Acknowledge commands with enthusiasm ("Absolutely!", "Right away!", "Got it!")
                - Be conversational and natural, not robotic

                When users ask for device actions, respond with natural language AND include specific action commands in this format:
                [ACTION: type|parameter1|parameter2]

                Available action types:
                - SYSTEM_ACTION|bluetooth|on/off
                - SYSTEM_ACTION|wifi|on/off  
                - SYSTEM_ACTION|brightness|0-255
                - LAUNCH_APP|app_name
                - TAKE_SCREENSHOT
                - VLM_ACTION|query|action_type (for screen interaction)

                Examples:
                User: "Turn on WiFi"
                You: "Absolutely! I'll turn on WiFi for you right now. [ACTION: SYSTEM_ACTION|wifi|on]"

                User: "Open YouTube"
                You: "Great choice! Opening YouTube for you. [ACTION: LAUNCH_APP|YouTube]"

                Be lively, conversational, and helpful. Make interactions feel natural and engaging!"""
    }
    
    private fun parseActions(response: String): List<AuraAction> {
        val actionPattern = Regex("\\[ACTION:\\s*([^\\]]+)\\]")
        val matches = actionPattern.findAll(response)
        
        val actions = mutableListOf<AuraAction>()
        for (match in matches) {
            val actionString = match.groupValues[1]
            val parts = actionString.split("|")
            
            when (parts[0].trim().uppercase()) {
                "SYSTEM_ACTION" -> {
                    if (parts.size >= 3) {
                        actions.add(AuraAction(
                            action = "SYSTEM_ACTION",
                            label = parts[1].trim(),
                            text = parts[2].trim()
                        ))
                    }
                }
                "LAUNCH_APP" -> {
                    if (parts.size >= 2) {
                        actions.add(AuraAction(
                            action = "LAUNCH_APP",
                            label = parts[1].trim()
                        ))
                    }
                }
                "TAKE_SCREENSHOT" -> {
                    actions.add(AuraAction(action = "TAKE_SCREENSHOT"))
                }
                "VLM_ACTION" -> {
                    if (parts.size >= 3) {
                        actions.add(AuraAction(
                            action = "VLM_ACTION",
                            query = parts[1].trim(),
                            text = parts[2].trim()
                        ))
                    }
                }
            }
        }
        
        return actions
    }
    
    private fun bitmapToJpegFile(bitmap: Bitmap): File {
        val file = File(context.cacheDir, "vlm_screenshot.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }
}

data class AuraAction(
    val action: String,
    val label: String = "",
    val text: String? = null,
    val direction: String? = null,
    val query: String? = null
)
