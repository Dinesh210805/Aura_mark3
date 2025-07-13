package com.aura.aura_mark3
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import com.aura.aura_mark3.ui.theme.Aura_mark3Theme
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.core.content.edit
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.util.Log
import android.Manifest
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.layout.*
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TouchApp


data class AuraAction(
    val action: String,
    val label: String = "",
    val text: String? = null,
    val direction: String? = null,
    val query: String? = null // For VLM actions
)

class MainActivity : ComponentActivity() {
    private var transcription by mutableStateOf("")
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


    // Add this launcher for multiple permissions
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isRecordAudioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        if (isRecordAudioGranted) {
            Log.i("AURA_MAIN", "RECORD_AUDIO permission granted.")
            speakWithPlayAITts("Listening.", selectedVoice, ttsSpeed)
            val intent = Intent(this, AudioRecorderService::class.java)
            startForegroundService(intent)
        } else {
            Log.e("AURA_MAIN", "RECORD_AUDIO permission was denied.")
            speakWithPlayAITts("I need microphone permission to listen for commands.", selectedVoice, ttsSpeed)
        }
    }

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
                val success = intent.getBooleanExtra(com.aura.aura_mark3.accessibility.AuraAccessibilityService.EXTRA_ACTION_RESULT_SUCCESS, false)
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

    // Update transcription receiver to always speak the transcribed text
    private val transcriptionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioRecorderService.ACTION_TRANSCRIPTION) {
                val text = intent.getStringExtra(AudioRecorderService.EXTRA_TRANSCRIPTION) ?: ""
                transcription = text
                isRecording = false
                if (text.isNotBlank()) {
                    Log.i("AURA_DEBUG", "Transcription received: $text")
                    Handler(Looper.getMainLooper()).postDelayed({
                        speakWithPlayAITts(text, selectedVoice, ttsSpeed)
                    }, 100)
                    // Auto-process with LLM
                    callLlmApi(text)
                } else {
                    Log.i("AURA_DEBUG", "No transcription received")
                    Handler(Looper.getMainLooper()).postDelayed({
                        speakWithPlayAITts("No transcription received.", selectedVoice, ttsSpeed)
                    }, 100)
                }
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
        }
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
            else -> {
                executeNextAction()
            }
        }
    }

    // Store a callback for VLM actions to continue the queue
    private var pendingVlmAction: ((Bitmap?) -> Unit)? = null

    /**
     * Loads the Groq API key from the api_keys.properties file in assets
     */
    private fun loadApiKey(): String {
        try {
            val properties = java.util.Properties()
            assets.open("api_keys.properties").use {
                properties.load(it)
            }
            return properties.getProperty("groq_api_key", "")
        } catch (e: Exception) {
            android.util.Log.e("AURA_API", "Failed to load API key", e)
            return ""  // Return empty token if API key can't be loaded
        }
    }

    private fun callLlmApi(userText: String) {
        val apiKey = loadApiKey()
        val llmApi = provideGroqLlmApi()
        val request = LlmRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(LlmMessage(role = "user", content = userText))
        )
        llmApi.chatCompletion(apiKey, request).enqueue(object : Callback<LlmResponse> {
            override fun onResponse(call: Call<LlmResponse>, response: Response<LlmResponse>) {
                if (response.isSuccessful) {
                    val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "(empty)"
                    llmResponse = reply
                    // JSON-based multi-step
                    if (reply.trim().startsWith("[")) {
                        try {
                            val type = object : TypeToken<List<AuraAction>>() {}.type
                            val actions: List<AuraAction> = Gson().fromJson(reply, type)
                            if (actions.isNotEmpty()) {
                                startActionQueue(actions)
                            }
                        } catch (e: Exception) {
                            actionResultMessage = "Failed to parse actions: ${e.localizedMessage}"
                        }
                    } else {
                        // Fallback: line-based
                        val actions = reply.split("\n").mapNotNull { line ->
                            val trimmed = line.trim()
                            when {
                                trimmed.startsWith("CLICK:", ignoreCase = true) -> {
                                    val label = trimmed.removePrefix("CLICK:").trim()
                                    if (label.isNotBlank()) AuraAction("CLICK", label) else null
                                }
                                trimmed.startsWith("INPUT:", ignoreCase = true) -> {
                                    val parts = trimmed.removePrefix("INPUT:").split(":", limit = 2)
                                    if (parts.size == 2) {
                                        val label = parts[0].trim()
                                        val text = parts[1].trim()
                                        if (label.isNotBlank() && text.isNotBlank()) AuraAction("INPUT", label, text) else null
                                    } else null
                                }
                                trimmed.startsWith("SCROLL:", ignoreCase = true) -> {
                                    val parts = trimmed.removePrefix("SCROLL:").split(":", limit = 2)
                                    if (parts.size == 2) {
                                        val label = parts[0].trim()
                                        val direction = parts[1].trim().uppercase()
                                        if (label.isNotBlank() && direction in setOf("UP", "DOWN", "LEFT", "RIGHT")) AuraAction("SCROLL", label, direction = direction) else null
                                    } else null
                                }
                                // Fallback for common phrases
                                else -> null
                            }
                        }
                        if (actions.isNotEmpty()) {
                            startActionQueue(actions)
                        }
                    }
                } else {
                    llmResponse = "LLM API error: ${response.code()} ${response.message()}"
                    speakWithPlayAITts(llmResponse, selectedVoice, ttsSpeed);
                }
            }
            override fun onFailure(call: Call<LlmResponse>, t: Throwable) {
                llmResponse = "LLM API call failed: ${t.localizedMessage}"
                speakWithPlayAITts(llmResponse, selectedVoice, ttsSpeed);
            }
        })
    }

    private fun isMicPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains("AuraAccessibilityService") }
    }

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
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        AudioRecorderControls(
                            modifier = Modifier.padding(innerPadding),
                            onStart = {
                                isRecording = true
                                speakWithPlayAITts("Listening.", selectedVoice, ttsSpeed)
                                val intent = Intent(this, AudioRecorderService::class.java)
                                startForegroundService(intent)
                            },
                            onStop = {
                                isRecording = false
                                speakWithPlayAITts("Stopped listening.", selectedVoice, ttsSpeed)
                                val intent = Intent(this, AudioRecorderService::class.java)
                                stopService(intent)
                            },
                            transcription = transcription,
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
                            isExecutingActions = isExecutingActions
                        )
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
        val apiKey = "Bearer ${loadApiKey()}" // Load API key from properties file
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

    private fun speakWithPlayAITts(message: String, voice: String, speed: Float) {
        if (!ttsFeedbackEnabled) return

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
                        playAudioFromResponse(this@MainActivity, responseBody)
                    }
                } else {
                    Log.e("AURA_TTS", "PlayAI TTS API error: ${response.code()} ${response.message()}")
                    // Fallback to local TTS
                    speakIfReady(message)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("AURA_TTS", "PlayAI TTS API call failed: ${t.localizedMessage}")
                // Fallback to local TTS
                speakIfReady(message)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(transcriptionReceiver)
            unregisterReceiver(actionResultReceiver)
        } catch (e: Exception) {
            Log.e("AURA_MAIN", "Error unregistering receivers: ${e.localizedMessage}")
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
    isExecutingActions: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
}