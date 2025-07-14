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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
            Log.i("AURA_STT", "Transcription: $transcription")
            userTranscription = transcription
            isRecording = false
            if (transcription.isNotBlank()) {
                // Optionally trigger LLM or other logic here
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
     */
    private fun startListeningForCommand() {
        isRecording = true
        val intent = Intent(this, AudioRecorderService::class.java)
        startForegroundService(intent)
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
     */
    fun startDynamicVoiceAssistant() {
        greetUserWithLlm {
            startListeningForCommand()
        }
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
        }

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                tts?.language = Locale.forLanguageTag(ttsLanguage)
                Log.i("AURA_TTS", "MainActivity TTS initialized successfully.")

                // Process any queued messages
                processTtsQueue()
            } else {
                Log.e("AURA_TTS", "MainActivity TTS initialization failed with status: $status")
                ttsReady = false
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
                            AudioRecorderControls(
                                modifier = Modifier.padding(innerPadding),
                                onStart = { startDynamicVoiceAssistant() },
                                onStop = {
                                    isRecording = false
                                    speakWithPlayAITts("Stopped listening.", selectedVoice, ttsSpeed)
                                    val intent = Intent(context, AudioRecorderService::class.java)
                                    context.stopService(intent)
                                },
                                transcription = userTranscription,
                                llmResponse = llmResponse,
                                actionResultMessage = actionResultMessage,
                                currentStep = currentStep,
                                onScreenshot = {
                                    screenshotHelper.requestScreenshotPermission()
                                },
                                screenshotBitmap = screenshotBitmap,
                                onVlmDemo = {
                                    screenshotHelper.requestScreenshotPermission()
                                    vlmResultMessage = "Requesting screenshot for VLM..."
                                },
                                vlmResultMessage = vlmResultMessage,
                                onSettings = { inSettings = true },
                                isRecording = isRecording,
                                isExecutingActions = isExecutingActions,
                                userTranscription = userTranscription,
                                assistantSpeech = assistantSpeech,
                                statusMessage = statusMessage
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
        if (!ttsFeedbackEnabled) return

        assistantSpeech = message // Always show the message in the caption bar

        val apiKey = loadApiKey()
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
                            assistantSpeech = ""
                            onDone?.invoke()
                        })
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
        } catch (e: Exception) {
            Log.e("AURA_MAIN", "Error unregistering receivers: ", e)
        }
        tts?.shutdown()
    }
}

@Composable
fun AudioRecorderControls(
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onStop: () -> Unit,
    transcription: String,
    llmResponse: String,
    actionResultMessage: String,
    currentStep: String,
    onScreenshot: () -> Unit,
    screenshotBitmap: Bitmap?,
    onVlmDemo: () -> Unit,
    vlmResultMessage: String,
    onSettings: () -> Unit,
    isRecording: Boolean,
    isExecutingActions: Boolean,
    userTranscription: String,
    assistantSpeech: String,
    statusMessage: String
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chat bubbles for user, assistant, and status
            if (userTranscription.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("You:", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        Spacer(Modifier.height(4.dp))
                        Text(userTranscription, color = Color(0xFF1976D2))
                    }
                }
            }
            if (assistantSpeech.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Assistant:", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                        Spacer(Modifier.height(4.dp))
                        Text(assistantSpeech, color = Color(0xFFF57C00))
                    }
                }
            }
            if (statusMessage.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Status:", fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                        Spacer(Modifier.height(4.dp))
                        Text(statusMessage, color = Color(0xFF388E3C))
                    }
                }
            }

            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AURA Assistant",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Voice-Controlled AI Assistant",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Status indicator
            if (isRecording || isExecutingActions) {
                val statusColor by animateColorAsState(
                    targetValue = if (isRecording) Color.Red else Color.Green,
                    animationSpec = tween(300)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRecording) "🎤 Listening..." else "⚡ Executing Actions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start Recording Button
                FilledTonalButton(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    enabled = !isRecording && !isExecutingActions,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Listen")
                }

                // Stop Recording Button
                FilledTonalButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    enabled = isRecording,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }

            // Secondary controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Screenshot Button
                OutlinedCard(
                    onClick = onScreenshot,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Screenshot")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Screenshot", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // VLM Demo Button
                OutlinedCard(
                    onClick = onVlmDemo,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "VLM Demo")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("VLM Demo", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Settings Button
                OutlinedCard(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Settings", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Screenshot display
            screenshotBitmap?.let { bitmap ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📸 Screenshot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Transcription display
            if (transcription.isNotBlank()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Transcription",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Transcription",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = transcription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // LLM Response display
            if (llmResponse.isNotBlank()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = "AI Response",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Response",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = llmResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Current Step display
            if (currentStep.isNotBlank()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Current Step",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current Step",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentStep,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Action Result display
            if (actionResultMessage.isNotBlank()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = "Action Result",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Action Result",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = actionResultMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // VLM Result display
            if (vlmResultMessage.isNotBlank()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = "VLM Result",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VLM Result",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = vlmResultMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bottom spacer
            Spacer(modifier = Modifier.height(16.dp))
        }
        // Persistent caption bar at the bottom
        // CaptionBar(
        //     userTranscription = userTranscription,
        //     assistantSpeech = assistantSpeech,
        //     modifier = Modifier.align(Alignment.BottomCenter)
        // )
    }
}