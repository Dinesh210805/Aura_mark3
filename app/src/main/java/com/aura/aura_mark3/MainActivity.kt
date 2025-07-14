package com.aura.aura_mark3
import androidx.compose.material3.MaterialTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.aura.aura_mark3.ui.theme.Aura_mark3Theme
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.aura.aura_mark3.audio.AudioRecorderService
import com.aura.aura_mark3.audio.EnhancedVoiceService
import com.aura.aura_mark3.ai.CompoundBetaApi
import com.aura.aura_mark3.ai.CompoundRequest
import com.aura.aura_mark3.ai.CompoundMessage
import com.aura.aura_mark3.ai.CompoundResponse
import com.aura.aura_mark3.ai.provideCompoundBetaApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import com.aura.aura_mark3.ai.LlmMessage
import com.aura.aura_mark3.ai.LlmRequest
import com.aura.aura_mark3.ai.LlmResponse
import com.aura.aura_mark3.ai.provideGroqLlmApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.aura.aura_mark3.accessibility.AuraAccessibilityService
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.graphics.Bitmap
import com.aura.aura_mark3.utils.ScreenshotHelper
import com.aura.aura_mark3.ai.VlmResponse
import com.aura.aura_mark3.ai.provideGroqVlmApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import com.aura.aura_mark3.ui.SettingsScreen
import com.aura.aura_mark3.ui.VoiceAssistantUI
import android.content.SharedPreferences
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aura.aura_mark3.ai.PlayAITtsRequest
import com.aura.aura_mark3.ai.providePlayAITtsApi
import com.aura.aura_mark3.ai.playAudioFromResponse
import okhttp3.ResponseBody
import android.os.Handler
import android.os.Looper
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.mutableStateOf
import com.aura.aura_mark3.ui.CaptionBar
import androidx.compose.ui.platform.LocalContext


data class AuraAction(
    val action: String,
    val label: String = "",
    val text: String? = null,
    val direction: String? = null,
    val query: String? = null // For VLM actions
)

class MainActivity : ComponentActivity() {
    private var llmResponse by mutableStateOf("")
    private var actionResultMessage by mutableStateOf("")
    private var tts: TextToSpeech? = null
    private var actionQueue: MutableList<AuraAction> = mutableListOf()
    private var currentStep by mutableStateOf("")
    private var isExecutingActions = false
    private var screenshotBitmap by mutableStateOf<Bitmap?>(null)
    private lateinit var screenshotHelper: ScreenshotHelper
    private var vlmResultMessage by mutableStateOf("")
    private var inSettings by mutableStateOf(false)
    private var ttsFeedbackEnabled by mutableStateOf(true)
    private var ttsLanguage by mutableStateOf("en")
    private lateinit var prefs: SharedPreferences
    private var ttsReady = false
    private val ttsQueue = mutableListOf<String>()
    private var selectedVoice by mutableStateOf("Arista-PlayAI")
    private var ttsSpeed by mutableStateOf(1.2f)
    private var isRecording by mutableStateOf(false)
    private var currentForegroundPackage by mutableStateOf("")
    private var latestScreenDescription by mutableStateOf("")

    // State for displaying user and assistant messages
    private var userTranscription by mutableStateOf("")
    private var assistantSpeech by mutableStateOf("")
    private var statusMessage by mutableStateOf("")

    // Enhanced Voice Service state variables
    private var isVoiceServiceRunning by mutableStateOf(false)
    private var isListeningForWakeWord by mutableStateOf(false)
    private var isRecordingCommand by mutableStateOf(false)
    private var currentAudioLevel by mutableStateOf(0)
    private var listeningType by mutableStateOf("stopped") // "wake_word", "command", "stopped"
    
    // Conversation state for advanced voice assistant
    private var conversationHistory = mutableListOf<CompoundMessage>()
    private var isProcessingRequest by mutableStateOf(false)
    private var hasGreeted by mutableStateOf(false)

    // Modern Activity Result API launcher
    private val screenshotResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            screenshotHelper.onActivityResult(result.resultCode, result.data) { bitmap ->
                screenshotBitmap = bitmap
                // If VLM demo was requested, send to VLM
                if (bitmap != null && vlmResultMessage.startsWith("Requesting screenshot")) {
                    sendScreenshotToVlm(bitmap, "Find the Play button")
                }
                // If a VLM action is pending, call its callback
                pendingVlmAction?.let { cb ->
                    cb(bitmap)
                    pendingVlmAction = null
                }
            }
        }

    private val actionResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == com.aura.aura_mark3.accessibility.AuraAccessibilityService.ACTION_ACTION_RESULT) {
                val message = intent.getStringExtra(com.aura.aura_mark3.accessibility.AuraAccessibilityService.EXTRA_ACTION_RESULT_MESSAGE) ?: ""
                actionResultMessage = message
                // Speak the result with a small delay to ensure TTS is ready
                Handler(Looper.getMainLooper()).postDelayed({
                    speakWithPlayAITts(message, selectedVoice, ttsSpeed)
                }, 100)
                // Proceed to next action if any
                if (isExecutingActions) {
                    executeNextAction()
                }
            }
        }
    }

    // Update STT receiver to set userTranscription and log the value
    private val transcriptionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val transcription = intent?.getStringExtra(AudioRecorderService.EXTRA_TRANSCRIPTION) ?: ""
            Log.i("AURA_STT", "Transcription received: $transcription")
            userTranscription = transcription
            isRecording = false
            if (transcription.isNotBlank()) {
                // Process the voice command
                statusMessage = "Processing your command..."
                processVoiceCommand(transcription)
            } else {
                statusMessage = "No speech detected. Please try again."
                speakWithPlayAITts("I didn't hear anything. Please try speaking again.", selectedVoice, ttsSpeed)
            }
        }
    }

    // Receiver for foreground package name
    private val foregroundPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == com.aura.aura_mark3.accessibility.AuraAccessibilityService.ACTION_FOREGROUND_PACKAGE) {
                currentForegroundPackage = intent.getStringExtra(com.aura.aura_mark3.accessibility.AuraAccessibilityService.EXTRA_FOREGROUND_PACKAGE) ?: ""
            }
        }
    }

    // Enhanced Voice Service receivers
    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == EnhancedVoiceService.ACTION_WAKE_WORD_DETECTED) {
                Log.i("AURA_WAKE", "Wake word detected - starting conversation")
                statusMessage = "Wake word detected! Listening for your command..."
                if (!hasGreeted) {
                    greetUserWithCompoundBeta()
                    hasGreeted = true
                } else {
                    speakWithPlayAITts("Yes, I'm listening. How can I help you?", selectedVoice, ttsSpeed)
                }
            }
        }
    }

    private val voiceTranscriptionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == EnhancedVoiceService.ACTION_VOICE_TRANSCRIPTION) {
                val transcription = intent.getStringExtra(EnhancedVoiceService.EXTRA_TRANSCRIPTION) ?: ""
                Log.i("AURA_VOICE", "Voice command received: $transcription")
                userTranscription = transcription
                if (transcription.isNotBlank()) {
                    processVoiceCommand(transcription)
                }
            }
        }
    }

    private val listeningStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == EnhancedVoiceService.ACTION_LISTENING_STATE) {
                isListeningForWakeWord = intent.getBooleanExtra(EnhancedVoiceService.EXTRA_IS_LISTENING, false)
                listeningType = intent.getStringExtra(EnhancedVoiceService.EXTRA_LISTENING_TYPE) ?: "stopped"
                
                when (listeningType) {
                    "wake_word" -> {
                        statusMessage = "Listening for 'Hey Aura'..."
                        isRecordingCommand = false
                    }
                    "command" -> {
                        statusMessage = "Recording your command..."
                        isRecordingCommand = true
                    }
                    "stopped" -> {
                        statusMessage = "Voice service stopped"
                        isRecordingCommand = false
                    }
                }
            }
        }
    }

    private val audioLevelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == EnhancedVoiceService.ACTION_AUDIO_LEVEL) {
                currentAudioLevel = intent.getIntExtra(EnhancedVoiceService.EXTRA_AUDIO_LEVEL, 0)
            }
        }
    }

    private fun startActionQueue(actions: List<AuraAction>) {
        actionQueue.clear()
        actionQueue.addAll(actions)
        isExecutingActions = true
        currentStep = ""
        executeNextAction()
    }

    private fun executeNextAction() {
        if (actionQueue.isNotEmpty()) {
            val action = actionQueue.removeAt(0)
            currentStep = action.toString()
            executeAction(action)
        } else {
            isExecutingActions = false
            currentStep = "All steps complete."
            // Continuous listening: restart STT and prompt user
            Handler(Looper.getMainLooper()).postDelayed({
                speakWithPlayAITts("Ready for your next command.", selectedVoice, ttsSpeed)
                startListeningForCommand()
            }, 500)
        }
    }

    /**
     * Start the STT service and update UI state for continuous listening.
     * Note: Enhanced Voice Service handles continuous listening automatically.
     */
    private fun startListeningForCommand() {
        // Enhanced Voice Service automatically handles wake word detection and command recording
        // This method is kept for compatibility but the actual listening is managed by EnhancedVoiceService
        statusMessage = "Ready for voice commands. Say 'Hey Aura' to start."
    }

    private fun executeAction(action: AuraAction) {
        when (action.action.uppercase()) {
            "CLICK" -> {
                if (action.label.isNotBlank()) {
                    val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_CLICK).apply {
                        putExtra(AuraAccessibilityService.EXTRA_CLICK_LABEL, action.label)
                    }
                    sendBroadcast(intent)
                } else {
                    executeNextAction()
                }
            }
            "INPUT" -> {
                if (action.label.isNotBlank() && !action.text.isNullOrBlank()) {
                    val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_INPUT).apply {
                        putExtra(AuraAccessibilityService.EXTRA_INPUT_LABEL, action.label)
                        putExtra(AuraAccessibilityService.EXTRA_INPUT_TEXT, action.text)
                    }
                    sendBroadcast(intent)
                } else {
                    executeNextAction()
                }
            }
            "SCROLL" -> {
                if (action.label.isNotBlank() && !action.direction.isNullOrBlank()) {
                    val dir = action.direction.uppercase()
                    if (dir in setOf("UP", "DOWN", "LEFT", "RIGHT")) {
                        val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_SCROLL).apply {
                            putExtra(AuraAccessibilityService.EXTRA_SCROLL_LABEL, action.label)
                            putExtra(AuraAccessibilityService.EXTRA_SCROLL_DIRECTION, dir)
                        }
                        sendBroadcast(intent)
                    } else {
                        executeNextAction()
                    }
                } else {
                    executeNextAction()
                }
            }
            "LAUNCH_APP" -> {
                val label = action.label
                Log.i("AURA_LAUNCH_APP", "Requested to launch app with label: $label")
                statusMessage = "Searching for $label..."
                speakWithPlayAITts(statusMessage, selectedVoice, ttsSpeed) {
                    val pkg = label.takeIf { it.contains(".") } ?: findPackageByAppLabel(label)
                    Log.i("AURA_LAUNCH_APP", "Resolved package: $pkg for label: $label")
                    if (!pkg.isNullOrBlank()) {
                        statusMessage = "Opening $label..."
                        speakWithPlayAITts(statusMessage, selectedVoice, ttsSpeed) {
                            val intent = packageManager.getLaunchIntentForPackage(pkg)
                            if (intent != null) {
                                runOnUiThread { startActivity(intent) }
                            } else {
                                speakWithPlayAITts("Could not open $label.", selectedVoice, ttsSpeed)
                            }
                        }
                    } else {
                        speakWithPlayAITts("App $label not found.", selectedVoice, ttsSpeed)
                    }
                }
            }
            "VLM_CLICK" -> {
                val query = action.query ?: action.label
                if (query.isNotBlank()) {
                    vlmResultMessage = "Requesting screenshot for VLM_CLICK..."
                    pendingVlmAction = { bitmap ->
                        if (bitmap != null) {
                            sendScreenshotToVlm(bitmap, query, continueAfterVlm = true, vlmAction = "CLICK")
                        } else {
                            vlmResultMessage = "VLM_CLICK: Screenshot failed."
                            executeNextAction()
                        }
                    }
                    screenshotHelper.requestScreenshotPermission(screenshotResultLauncher)
                } else {
                    executeNextAction()
                }
            }
            "VLM_SCROLL" -> {
                val query = action.query ?: action.label
                val direction = action.direction ?: "DOWN"
                if (query.isNotBlank() && direction.isNotBlank()) {
                    vlmResultMessage = "Requesting screenshot for VLM_SCROLL..."
                    pendingVlmAction = { bitmap ->
                        if (bitmap != null) {
                            sendScreenshotToVlm(bitmap, query, continueAfterVlm = true, vlmAction = "SCROLL", direction = direction)
                        } else {
                            vlmResultMessage = "VLM_SCROLL: Screenshot failed."
                            executeNextAction()
                        }
                    }
                    screenshotHelper.requestScreenshotPermission(screenshotResultLauncher)
                } else {
                    executeNextAction()
                }
            }
            "VLM_INPUT" -> {
                val query = action.query ?: action.label
                val text = action.text ?: ""
                if (query.isNotBlank() && text.isNotBlank()) {
                    vlmResultMessage = "Requesting screenshot for VLM_INPUT..."
                    pendingVlmAction = { bitmap ->
                        if (bitmap != null) {
                            sendScreenshotToVlm(bitmap, query, continueAfterVlm = true, vlmAction = "INPUT", text = text)
                        } else {
                            vlmResultMessage = "VLM_INPUT: Screenshot failed."
                            executeNextAction()
                        }
                    }
                    screenshotHelper.requestScreenshotPermission(screenshotResultLauncher)
                } else {
                    executeNextAction()
                }
            }
            "SYSTEM_ACTION" -> {
                val systemAction = action.label.lowercase()
                when {
                    systemAction.contains("bluetooth") -> {
                        val enable = action.text?.contains("on", true) == true
                        setBluetoothEnabled(enable)
                    }
                    systemAction.contains("wifi") -> {
                        val enable = action.text?.contains("on", true) == true
                        setWifiEnabled(enable)
                    }
                    systemAction.contains("brightness") -> {
                        val value = action.text?.filter { it.isDigit() }?.toIntOrNull()
                        if (value != null) setScreenBrightness(value)
                        else speakWithPlayAITts("Please specify a brightness value.", selectedVoice, ttsSpeed)
                    }
                    else -> {
                        speakWithPlayAITts("System action not supported yet.", selectedVoice, ttsSpeed)
                    }
                }
                executeNextAction()
            }
            else -> {
                executeNextAction()
            }
        }
    }

    // --- System Action Handlers ---
    private fun setBluetoothEnabled(enable: Boolean) {
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                speakWithPlayAITts("Bluetooth not supported on this device.", selectedVoice, ttsSpeed)
                return
            }
            // Check BLUETOOTH_CONNECT permission (Android 12+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    speakWithPlayAITts("Bluetooth permission not granted. Please enable it in settings.", selectedVoice, ttsSpeed)
                    return
                }
            }
            if (enable && !bluetoothAdapter.isEnabled) {
                bluetoothAdapter.enable()
                speakWithPlayAITts("Turning Bluetooth on.", selectedVoice, ttsSpeed)
            } else if (!enable && bluetoothAdapter.isEnabled) {
                bluetoothAdapter.disable()
                speakWithPlayAITts("Turning Bluetooth off.", selectedVoice, ttsSpeed)
            } else {
                speakWithPlayAITts("Bluetooth is already ${if (enable) "on" else "off"}.", selectedVoice, ttsSpeed)
            }
        } catch (e: SecurityException) {
            speakWithPlayAITts("Bluetooth permission denied by system.", selectedVoice, ttsSpeed)
        } catch (e: Exception) {
            speakWithPlayAITts("Failed to change Bluetooth state.", selectedVoice, ttsSpeed)
        }
    }

    private fun setWifiEnabled(enable: Boolean) {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (wifiManager == null) {
                speakWithPlayAITts("WiFi not supported on this device.", selectedVoice, ttsSpeed)
                return
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Cannot change WiFi state directly on Android 10+
                speakWithPlayAITts("Cannot change WiFi state directly on this version of Android. Opening WiFi settings.", selectedVoice, ttsSpeed)
                startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            } else {
                wifiManager.isWifiEnabled = enable
                speakWithPlayAITts("Turning WiFi ${if (enable) "on" else "off"}.", selectedVoice, ttsSpeed)
            }
        } catch (e: Exception) {
            speakWithPlayAITts("Failed to change WiFi state.", selectedVoice, ttsSpeed)
        }
    }

    private fun setScreenBrightness(value: Int) {
        try {
            val brightness = value.coerceIn(0, 255)
            android.provider.Settings.System.putInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness)
            speakWithPlayAITts("Set screen brightness to $brightness.", selectedVoice, ttsSpeed)
        } catch (e: Exception) {
            speakWithPlayAITts("Failed to set screen brightness.", selectedVoice, ttsSpeed)
        }
    }

    /**
     * Find a package name by app label (case-insensitive, best effort, fuzzy/partial match).
     *
     * Android 11+ package visibility filtering:
     * By default, getInstalledApplications() and similar methods only return a filtered list of apps.
     * To allow your app to see other launchable apps, declare a <queries> block in AndroidManifest.xml:
     *
     * <queries>
     *     <intent>
     *         <action android:name="android.intent.action.MAIN" />
     *         <category android:name="android.intent.category.LAUNCHER" />
     *     </intent>
     * </queries>
     *
     * See: https://developer.android.com/training/package-visibility
     */
    private fun findPackageByAppLabel(label: String?): String? {
        if (label.isNullOrBlank()) return null
        val pm = packageManager
        val packages = pm.getInstalledApplications(0)
        // Try exact match first
        val exact = packages.firstOrNull {
            pm.getApplicationLabel(it).toString().equals(label, ignoreCase = true)
        }
        if (exact != null) return exact.packageName
        // Try partial (contains) match
        val partial = packages.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(label, ignoreCase = true)
        }
        if (partial != null) return partial.packageName
        // Try fuzzy match (ignore case, spaces, etc.)
        val normalizedLabel = label.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        val fuzzy = packages.firstOrNull {
            pm.getApplicationLabel(it).toString().replace("[^a-zA-Z0-9]".toRegex(), "").lowercase().contains(normalizedLabel)
        }
        if (fuzzy != null) return fuzzy.packageName
        // Suggest similar apps
        val suggestions = packages.map { pm.getApplicationLabel(it).toString() }
            .filter { it.contains(label.take(3), ignoreCase = true) }
        Log.w("AURA_LAUNCH_APP", "No match for label '$label'. Suggestions: $suggestions")
        if (suggestions.isNotEmpty()) {
            speakWithPlayAITts("Did you mean: ${suggestions.joinToString(", ")}?", selectedVoice, ttsSpeed)
        } else {
            speakWithPlayAITts("App $label not found.", selectedVoice, ttsSpeed)
        }
        return null
    }

    // Store a callback for VLM actions to continue the queue
    private var pendingVlmAction: ((Bitmap?) -> Unit)? = null

    /**
     * Loads the Groq API key from the environment variable GROQ_API_KEY, or from the api_keys.properties file in assets as fallback.
     */
    private fun loadApiKey(): String {
        // Try environment variable first
        val envKey = System.getenv("GROQ_API_KEY")
        if (!envKey.isNullOrBlank()) {
            return "Bearer $envKey"
        }
        // Fallback to properties file
        return try {
            val properties = java.util.Properties()
            assets.open("api_keys.properties").use {
                properties.load(it)
            }
            val fileKey = properties.getProperty("groq_api_key", "")
            if (fileKey.isNotBlank()) "Bearer $fileKey" else ""
        } catch (e: Exception) {
            android.util.Log.e("AURA_API", "Failed to load API key", e)
            ""
        }
    }

    /**
     * Call the LLM to generate a dynamic greeting for the user.
     */
    private fun greetUserWithLlm(onDone: () -> Unit) {
        val apiKey = loadApiKey()
        val llmApi = provideGroqLlmApi()
        val prompt = "Greet the user Joyboy as their friendly assistant and ask how you can help. Be warm, concise, and natural."
        val request = LlmRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(LlmMessage(role = "user", content = prompt))
        )
        llmApi.chatCompletion(apiKey, request).enqueue(object : Callback<LlmResponse> {
            override fun onResponse(call: Call<LlmResponse>, response: Response<LlmResponse>) {
                val reply = if (response.isSuccessful) {
                    response.body()?.choices?.firstOrNull()?.message?.content ?: "Hello, Joyboy! How can I help you today?"
                } else {
                    "Hello, Joyboy! How can I help you today?"
                }
                speakWithPlayAITts(reply, selectedVoice, ttsSpeed)
                onDone()
            }
            override fun onFailure(call: Call<LlmResponse>, t: Throwable) {
                speakWithPlayAITts("Hello, Joyboy! How can I help you today?", selectedVoice, ttsSpeed)
                onDone()
            }
        })
    }

    /**
     * Public function to greet the user dynamically and start listening.
     * Now uses Enhanced Voice Service for continuous listening.
     */
    fun startDynamicVoiceAssistant() {
        // Enhanced Voice Service handles continuous listening
        // Just ensure the service is running and give user feedback
        if (!isVoiceServiceRunning && isMicPermissionGranted()) {
            startEnhancedVoiceService()
        }
        statusMessage = "Voice assistant active. Say 'Hey Aura' to start."
        speakWithPlayAITts("Voice assistant is now active. Say 'Hey Aura' when you need me.", selectedVoice, ttsSpeed)
    }

    /**
     * Fallback greeting for when Enhanced Voice Service may not be working
     */
    private fun performInitialGreeting() {
        if (!hasGreeted) {
            statusMessage = "Initializing AURA..."
            speakWithPlayAITts("Hello! I'm AURA, your intelligent voice assistant. Tap the microphone to talk to me.", selectedVoice, ttsSpeed) {
                hasGreeted = true
                statusMessage = "Ready - Tap microphone or say 'Hey Aura'"
            }
        }
    }

    /**
     * Diagnostic function to check system status
     */
    private fun performSystemDiagnostic(): String {
        val diagnostics = mutableListOf<String>()
        
        diagnostics.add("TTS Ready: $ttsReady")
        diagnostics.add("Mic Permission: ${isMicPermissionGranted()}")
        diagnostics.add("Accessibility: ${isAccessibilityEnabled()}")
        diagnostics.add("Voice Service Running: $isVoiceServiceRunning")
        diagnostics.add("API Key Available: ${loadApiKey().isNotBlank()}")
        
        val result = diagnostics.joinToString(", ")
        Log.i("AURA_DIAGNOSTIC", result)
        return result
    }

    private fun isMicPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains("AuraAccessibilityService") }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize with a welcome status
        statusMessage = "Initializing AURA voice assistant..."
        
        screenshotHelper = ScreenshotHelper(this)
        prefs = getSharedPreferences("aura_prefs", MODE_PRIVATE)
        // Load persisted settings
        ttsFeedbackEnabled = prefs.getBoolean("tts_feedback_enabled", true)
        ttsLanguage = prefs.getString("tts_language", "en") ?: "en"
        selectedVoice = prefs.getString("playai_voice", "Arista-PlayAI") ?: "Arista-PlayAI"
        ttsSpeed = prefs.getFloat("tts_speed", 1.2f)

        fun requestMicPermission() {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
        fun openAccessibilitySettings() {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // Register receivers with proper Android 13+ compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                transcriptionReceiver,
                IntentFilter(AudioRecorderService.ACTION_TRANSCRIPTION),
                Context.RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                actionResultReceiver,
                IntentFilter(AuraAccessibilityService.ACTION_ACTION_RESULT),
                Context.RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                foregroundPackageReceiver,
                IntentFilter(com.aura.aura_mark3.accessibility.AuraAccessibilityService.ACTION_FOREGROUND_PACKAGE),
                Context.RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                wakeWordReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_WAKE_WORD_DETECTED),
                Context.RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                voiceTranscriptionReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_VOICE_TRANSCRIPTION),
                Context.RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                listeningStateReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_LISTENING_STATE),
                Context.RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                audioLevelReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_AUDIO_LEVEL),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                transcriptionReceiver,
                IntentFilter(AudioRecorderService.ACTION_TRANSCRIPTION)
            )
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                actionResultReceiver,
                IntentFilter(AuraAccessibilityService.ACTION_ACTION_RESULT)
            )
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                foregroundPackageReceiver,
                IntentFilter(com.aura.aura_mark3.accessibility.AuraAccessibilityService.ACTION_FOREGROUND_PACKAGE)
            )
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                wakeWordReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_WAKE_WORD_DETECTED)
            )
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                voiceTranscriptionReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_VOICE_TRANSCRIPTION)
            )
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                listeningStateReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_LISTENING_STATE)
            )
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                audioLevelReceiver,
                IntentFilter(EnhancedVoiceService.ACTION_AUDIO_LEVEL)
            )
        }

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                tts?.language = Locale.forLanguageTag(ttsLanguage)
                Log.i("AURA_TTS", "MainActivity TTS initialized successfully.")

                // Process any queued messages
                processTtsQueue()
                
                // Give initial greeting
                Handler(Looper.getMainLooper()).postDelayed({
                    performInitialGreeting()
                    // Start Enhanced Voice Service for continuous listening after greeting
                    if (isMicPermissionGranted()) {
                        startEnhancedVoiceService()
                    } else {
                        statusMessage = "Microphone permission required for voice assistant"
                        speakWithPlayAITts("Please grant microphone permission to use voice commands.", selectedVoice, ttsSpeed)
                    }
                }, 1000) // Wait 1 second before greeting
            } else {
                Log.e("AURA_TTS", "MainActivity TTS initialization failed with status: $status")
                ttsReady = false
                statusMessage = "TTS initialization failed"
                performSystemDiagnostic()
                // Try to start Enhanced Voice Service anyway
                if (isMicPermissionGranted()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startEnhancedVoiceService()
                        statusMessage = "Voice service ready (TTS unavailable)"
                    }, 2000)
                } else {
                    statusMessage = "Microphone permission required"
                }
            }
        }

        setContent {
            Aura_mark3Theme {
                val context = LocalContext.current
                if (inSettings) {
                    SettingsScreen(
                        ttsFeedbackEnabled = ttsFeedbackEnabled,
                        onTtsFeedbackChange = {
                            ttsFeedbackEnabled = it
                            prefs.edit().putBoolean("tts_feedback_enabled", it).apply()
                        },
                        ttsLanguage = ttsLanguage,
                        onTtsLanguageChange = {
                            ttsLanguage = it
                            prefs.edit().putString("tts_language", it).apply()
                            if (ttsReady) {
                                tts?.language = Locale.forLanguageTag(it)
                            }
                        },
                        onBack = { inSettings = false },
                        micPermissionGranted = isMicPermissionGranted(),
                        onRequestMicPermission = { requestMicPermission() },
                        accessibilityEnabled = isAccessibilityEnabled(),
                        onOpenAccessibilitySettings = { openAccessibilitySettings() },
                        selectedVoice = selectedVoice,
                        onVoiceChange = {
                            selectedVoice = it
                            prefs.edit().putString("playai_voice", it).apply()
                        },
                        ttsSpeed = ttsSpeed,
                        onTtsSpeedChange = {
                            ttsSpeed = it
                            prefs.edit().putFloat("tts_speed", it).apply()
                        },
                        onVoicePreview = { voice ->
                            speakWithPlayAITts("This is what I sound like!", voice, ttsSpeed)
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            VoiceAssistantUI(
                                modifier = Modifier.padding(innerPadding),
                                isListening = isListeningForWakeWord || isRecordingCommand || isRecording,
                                listeningType = when {
                                    isRecording -> "manual"
                                    isRecordingCommand -> "command"
                                    isListeningForWakeWord -> "wake_word"
                                    else -> "stopped"
                                },
                                audioLevel = currentAudioLevel,
                                userTranscription = userTranscription,
                                assistantSpeech = assistantSpeech,
                                statusMessage = statusMessage,
                                onManualRecord = { 
                                    // Manual activation for testing
                                    manualVoiceActivation()
                                },
                                onSettings = { inSettings = true }
                            )
                        }
                        // Only one CaptionBar at the bottom, animated
                        // AnimatedVisibility for smooth in/out
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = userTranscription.isNotBlank() || assistantSpeech.isNotBlank()
                            ) {
                                CaptionBar(
                                    userTranscription = userTranscription,
                                    assistantSpeech = assistantSpeech,
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun sendScreenshotToVlm(
        bitmap: Bitmap,
        query: String,
        continueAfterVlm: Boolean = false,
        vlmAction: String = "CLICK",
        direction: String = "DOWN",
        text: String = ""
    ) {
        val apiKey = loadApiKey() // Load API key from environment variable or properties
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
                        val cx = coords.x + coords.width / 2
                        val cy = coords.y + coords.height / 2
                        when (vlmAction) {
                            "CLICK" -> sendGestureAtCoordinates(cx, cy)
                            "SCROLL" -> sendScrollAtCoordinates(cx, cy, direction)
                            "INPUT" -> sendInputAtCoordinates(cx, cy, text)
                        }
                    } else {
                        vlmResultMessage = "VLM: No coordinates found."
                    }
                } else {
                    vlmResultMessage = "VLM API error: ${response.code()} ${response.message()}"
                }
                if (continueAfterVlm) executeNextAction()
            }
            override fun onFailure(call: Call<VlmResponse>, t: Throwable) {
                vlmResultMessage = "VLM API call failed: ${t.localizedMessage}"
                if (continueAfterVlm) executeNextAction()
            }
        })
    }

    private fun bitmapToJpegFile(bitmap: Bitmap): File {
        val file = File(cacheDir, "vlm_screenshot.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }

    private fun sendGestureAtCoordinates(x: Int, y: Int) {
        val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_GESTURE_AT_COORDS).apply {
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_X, x)
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_Y, y)
        }
        sendBroadcast(intent)
    }

    private fun sendScrollAtCoordinates(x: Int, y: Int, direction: String) {
        val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_SCROLL_AT_COORDS).apply {
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_X, x)
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_Y, y)
            putExtra(AuraAccessibilityService.EXTRA_SCROLL_DIRECTION, direction)
        }
        sendBroadcast(intent)
    }

    private fun sendInputAtCoordinates(x: Int, y: Int, text: String) {
        val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_INPUT_AT_COORDS).apply {
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_X, x)
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_Y, y)
            putExtra(AuraAccessibilityService.EXTRA_INPUT_TEXT, text)
        }
        sendBroadcast(intent)
    }

    private fun speakIfReady(message: String) {
        // This function is now only used as a fallback in speakWithPlayAITts
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
                    Log.e("AURA_TTS", "MainActivity TTS speak failed for message: \"$message\"")
                } else {
                    Log.i("AURA_TTS", "MainActivity TTS speak successful for message: \"$message\"")
                }
            } else {
                Log.w("AURA_TTS", "MainActivity TTS not available for message: \"$message\"")
            }
        } catch (e: Exception) {
            Log.e("AURA_TTS", "MainActivity TTS speak exception: ${e.localizedMessage}")
        }
    }

    private fun processTtsQueue() {
        while (ttsQueue.isNotEmpty() && ttsReady) {
            val message = ttsQueue.removeAt(0)
            speakInternal(message)
        }
    }

    private fun speakWithPlayAITts(message: String, voice: String, speed: Float, onDone: (() -> Unit)? = null) {
        if (!ttsFeedbackEnabled) {
            onDone?.invoke()
            return
        }

        assistantSpeech = message // Always show the message in the caption bar

        val apiKey = loadApiKey()
        if (apiKey.isBlank()) {
            Log.w("AURA_TTS", "No API key found, falling back to local TTS")
            speakIfReady(message)
            assistantSpeech = ""
            onDone?.invoke()
            return
        }

        val playAITtsApi = providePlayAITtsApi()
        val request = PlayAITtsRequest(
            input = message,
            voice = voice,
            response_format = "mp3",
            speed = speed
        )

        playAITtsApi.synthesizeSpeech(apiKey, request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        playAudioFromResponse(this@MainActivity, responseBody, onComplete = {
                            // Clear assistantSpeech after playback
                            assistantSpeech = ""
                            onDone?.invoke()
                        }, onError = {
                            Log.e("AURA_TTS", "Error playing PlayAI TTS audio, falling back to local TTS")
                            speakIfReady(message)
                            assistantSpeech = ""
                            onDone?.invoke()
                        })
                    } ?: run {
                        Log.e("AURA_TTS", "PlayAI TTS response body is null")
                        speakIfReady(message)
                        assistantSpeech = ""
                        onDone?.invoke()
                    }
                } else {
                    Log.e("AURA_TTS", "PlayAI TTS API error: ${response.code()} ${response.message()}")
                    // Fallback to local TTS
                    speakIfReady(message)
                    assistantSpeech = ""
                    onDone?.invoke()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("AURA_TTS", "PlayAI TTS API call failed: ${t.localizedMessage}")
                // Fallback to local TTS
                speakIfReady(message)
                assistantSpeech = ""
                onDone?.invoke()
            }
        })
    }

    /**
     * Take a screenshot and get a VLM description of the current screen.
     * Calls the callback after updating latestScreenDescription.
     */
    private fun takeScreenshotAndDescribe(onDone: () -> Unit) {
        screenshotHelper.requestScreenshotPermission(screenshotResultLauncher)
        // The screenshotResultLauncher callback will call this:
        pendingVlmAction = { bitmap ->
            if (bitmap != null) {
                getVlmScreenDescription(bitmap) { desc ->
                    latestScreenDescription = desc ?: ""
                    onDone()
                }
            } else {
                latestScreenDescription = ""
                onDone()
            }
        }
    }

    /**
     * Send a screenshot to the VLM with a generic query to describe the screen.
     */
    private fun getVlmScreenDescription(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val apiKey = loadApiKey() // Load API key from environment variable or properties
        val vlmApi = provideGroqVlmApi()
        val file = bitmapToJpegFile(bitmap)
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val modelPart = "llama-4-maverick-17b-128e-instruct".toRequestBody("text/plain".toMediaTypeOrNull())
        val queryPart = "Describe the current screen".toRequestBody("text/plain".toMediaTypeOrNull())
        vlmApi.locateUiElement(apiKey, imagePart, modelPart, queryPart).enqueue(object : Callback<VlmResponse> {
            override fun onResponse(call: Call<VlmResponse>, response: Response<VlmResponse>) {
                if (response.isSuccessful) {
                    // Use safe fallback if 'description' is missing
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(transcriptionReceiver)
            unregisterReceiver(actionResultReceiver)
            unregisterReceiver(foregroundPackageReceiver)
            unregisterReceiver(wakeWordReceiver)
            unregisterReceiver(voiceTranscriptionReceiver)
            unregisterReceiver(listeningStateReceiver)
            unregisterReceiver(audioLevelReceiver)
        } catch (e: Exception) {
            Log.e("AURA_MAIN", "Error unregistering receivers: ", e)
        }
        
        // Stop Enhanced Voice Service
        if (isVoiceServiceRunning) {
            stopEnhancedVoiceService()
        }
        
        tts?.shutdown()
    }

    /**
     * Enhanced greeting using Compound Beta API with real-world awareness
     */
    private fun greetUserWithCompoundBeta() {
        if (isProcessingRequest) return
        
        isProcessingRequest = true
        assistantSpeech = "Initializing AURA..."
        
        val apiKey = loadApiKey()
        if (apiKey.isBlank()) {
            Log.w("AURA_LLM", "No API key found, using fallback greeting")
            speakWithPlayAITts("Hello, Joyboy! I'm AURA, your voice assistant. How can I help you today?", selectedVoice, ttsSpeed) {
                isProcessingRequest = false
            }
            return
        }
        
        val compoundApi = provideCompoundBetaApi()
        val systemPrompt = """You are AURA, an advanced voice assistant for Joyboy. You have access to real-world information and can help with device control, app management, web search, and more. Greet Joyboy warmly and mention your capabilities briefly."""
        
        val request = CompoundRequest(
            model = "compound-beta",
            messages = listOf(
                CompoundMessage(role = "system", content = systemPrompt),
                CompoundMessage(role = "user", content = "Hello AURA, I just activated you with the wake word 'Hey Aura'")
            ),
            maxTokens = 150,
            temperature = 0.8f
        )
        
        compoundApi.chatCompletion(apiKey, request).enqueue(object : Callback<CompoundResponse> {
            override fun onResponse(call: Call<CompoundResponse>, response: Response<CompoundResponse>) {
                val reply = if (response.isSuccessful) {
                    response.body()?.choices?.firstOrNull()?.message?.content ?: 
                    "Hello, Joyboy! I'm AURA, your intelligent voice assistant. How can I help you today?"
                } else {
                    Log.w("AURA_LLM", "Compound Beta API returned error: ${response.code()}")
                    "Hello, Joyboy! I'm AURA, your intelligent voice assistant. How can I help you today?"
                }
                
                conversationHistory.add(CompoundMessage(role = "assistant", content = reply))
                speakWithPlayAITts(reply, selectedVoice, ttsSpeed) {
                    isProcessingRequest = false
                }
            }
            
            override fun onFailure(call: Call<CompoundResponse>, t: Throwable) {
                Log.e("AURA_LLM", "Compound Beta greeting failed", t)
                speakWithPlayAITts("Hello, Joyboy! I'm AURA, your intelligent voice assistant. How can I help you today?", selectedVoice, ttsSpeed) {
                    isProcessingRequest = false
                }
            }
        })
    }

    /**
     * Process voice commands using Compound Beta API with real-world data access
     */
    private fun processVoiceCommand(command: String) {
        if (isProcessingRequest) return
        
        isProcessingRequest = true
        statusMessage = "Processing your request..."
        assistantSpeech = "Thinking..."
        
        val apiKey = loadApiKey()
        if (apiKey.isBlank()) {
            speakWithPlayAITts("I'm sorry, I can't process requests without an API key.", selectedVoice, ttsSpeed)
            isProcessingRequest = false
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
        
        compoundApi.chatCompletion(apiKey, request).enqueue(object : Callback<CompoundResponse> {
            override fun onResponse(call: Call<CompoundResponse>, response: Response<CompoundResponse>) {
                if (response.isSuccessful) {
                    val reply = response.body()?.choices?.firstOrNull()?.message?.content
                    if (!reply.isNullOrBlank()) {
                        conversationHistory.add(CompoundMessage(role = "assistant", content = reply))
                        parseAndExecuteActions(reply)
                        speakWithPlayAITts(reply, selectedVoice, ttsSpeed) {
                            isProcessingRequest = false
                            statusMessage = "Ready for next command"
                        }
                    } else {
                        fallbackResponse()
                    }
                } else {
                    Log.e("AURA_LLM", "Compound Beta API error: ${response.code()}")
                    fallbackResponse()
                }
            }
            
            override fun onFailure(call: Call<CompoundResponse>, t: Throwable) {
                Log.e("AURA_LLM", "Compound Beta API call failed", t)
                fallbackResponse()
            }
        })
    }

    private fun fallbackResponse() {
        speakWithPlayAITts("I'm sorry, I couldn't process that request. Please try again.", selectedVoice, ttsSpeed) {
            isProcessingRequest = false
            statusMessage = "Ready for next command"
        }
    }

    /**
     * Build context-aware system prompt with current app and screen information
     */
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
        
        return """You are AURA, an intelligent voice assistant for Android devices. You have access to real-world information via web search and can help with:

                DEVICE CONTROL ACTIONS:
                - System settings: WiFi, Bluetooth, brightness, volume
                - App control: open apps, navigate interfaces
                - Screen interaction: click, scroll, input text

                INFORMATION ACCESS:
                - Web search for current information
                - Screen analysis and description
                - Real-time data access

                CURRENT CONTEXT:
                - $currentApp
                - $screenInfo

                When the user asks for device actions, respond with natural language AND include specific action commands in this format:
                [ACTION: type|parameter1|parameter2]

                Available action types:
                - SYSTEM_ACTION|bluetooth|on/off
                - SYSTEM_ACTION|wifi|on/off  
                - SYSTEM_ACTION|brightness|0-255
                - LAUNCH_APP|app_name
                - TAKE_SCREENSHOT
                - VLM_ACTION|query|action_type (for screen interaction)

                Be conversational, helpful, and use your real-world data access when needed. Always acknowledge the user's request clearly."""
    }

    /**
     * Parse assistant response for action commands and execute them
     */
    private fun parseAndExecuteActions(response: String) {
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
        
        if (actions.isNotEmpty()) {
            startActionQueue(actions)
        }
    }

    /**
     * Start Enhanced Voice Service for continuous listening
     */
    private fun startEnhancedVoiceService() {
        try {
            if (isVoiceServiceRunning) {
                Log.i("AURA_VOICE", "Enhanced Voice Service already running")
                return
            }
            
            val serviceIntent = Intent(this, EnhancedVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val result = startForegroundService(serviceIntent)
                Log.i("AURA_VOICE", "Enhanced Voice Service startForegroundService result: $result")
            } else {
                val result = startService(serviceIntent)
                Log.i("AURA_VOICE", "Enhanced Voice Service startService result: $result")
            }
            isVoiceServiceRunning = true
            statusMessage = "Voice service starting..."
            Log.i("AURA_VOICE", "Enhanced Voice Service start requested")
            
            // Set a timeout to check if service actually started
            Handler(Looper.getMainLooper()).postDelayed({
                if (isVoiceServiceRunning && statusMessage.contains("starting")) {
                    statusMessage = "Listening for 'Hey Aura'..."
                    Log.i("AURA_VOICE", "Voice service appears to be running")
                }
            }, 3000)
            
            // Set a longer timeout to provide fallback if service doesn't respond
            Handler(Looper.getMainLooper()).postDelayed({
                if (statusMessage.contains("starting") || statusMessage.contains("Voice service")) {
                    statusMessage = "Voice assistant ready - Tap microphone to talk"
                    Log.w("AURA_VOICE", "Enhanced Voice Service may not be responding, switching to manual mode")
                    speakWithPlayAITts("Voice assistant ready. Tap the microphone button to talk to me.", selectedVoice, ttsSpeed)
                }
            }, 8000)
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Failed to start Enhanced Voice Service", e)
            isVoiceServiceRunning = false
            statusMessage = "Voice service failed to start"
            speakWithPlayAITts("Voice service unavailable. You can still use manual controls.", selectedVoice, ttsSpeed)
        }
    }

    /**
     * Stop Enhanced Voice Service
     */
    private fun stopEnhancedVoiceService() {
        try {
            val serviceIntent = Intent(this, EnhancedVoiceService::class.java)
            stopService(serviceIntent)
            isVoiceServiceRunning = false
            statusMessage = "Voice service stopped"
            Log.i("AURA_VOICE", "Enhanced Voice Service stopped")
        } catch (e: Exception) {
            Log.e("AURA_VOICE", "Error stopping Enhanced Voice Service", e)
        }
    }

    /**
     * Manual activation for testing - simulates wake word detection and starts manual recording
     */
    fun manualVoiceActivation() {
        Log.i("AURA_VOICE", "Manual voice activation triggered")
        statusMessage = "Manual activation! Listening for your command..."
        
        if (isRecording) {
            // Stop manual recording
            val stopIntent = Intent(this, AudioRecorderService::class.java)
            stopService(stopIntent)
            isRecording = false
            statusMessage = "Recording stopped. Processing..."
        } else {
            // Start manual recording 
            if (isMicPermissionGranted()) {
                val recordIntent = Intent(this, AudioRecorderService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(recordIntent)
                } else {
                    startService(recordIntent)
                }
                isRecording = true
                statusMessage = "Recording... Speak your command"
                speakWithPlayAITts("I'm listening. Please speak your command.", selectedVoice, ttsSpeed)
            } else {
                statusMessage = "Microphone permission required"
                speakWithPlayAITts("Please grant microphone permission to use voice commands.", selectedVoice, ttsSpeed)
            }
        }
        
        if (!hasGreeted && !isRecording) {
            greetUserWithCompoundBeta()
            hasGreeted = true
        }
    }
}