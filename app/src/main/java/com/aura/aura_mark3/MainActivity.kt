package com.aura.aura_mark3

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aura.aura_mark3.managers.*
import com.aura.aura_mark3.ui.SettingsScreen
import com.aura.aura_mark3.ui.VoiceAssistantUI
import com.aura.aura_mark3.ui.theme.Aura_mark3Theme
import com.aura.aura_mark3.utils.ScreenshotHelper
import java.util.*

/**
 * Modularized MainActivity - delegates functionality to specialized managers
 */
class MainActivity : ComponentActivity() {
    // UI State
    private var inSettings by mutableStateOf(false)
    private var screenshotBitmap by mutableStateOf<Bitmap?>(null)
    
    // Managers
    private lateinit var voiceManager: VoiceManager
    private lateinit var aiManager: AIManager
    private lateinit var systemManager: SystemManager
    private lateinit var eventManager: EventManager
    
    // Utilities
    private lateinit var screenshotHelper: ScreenshotHelper
    private lateinit var prefs: SharedPreferences
    
    // Activity Result Launcher
    private val screenshotResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            screenshotHelper.handleScreenshotResult(result) { bitmap ->
                screenshotBitmap = bitmap
                // Handle VLM processing if needed
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        initializeComponents()
        setupManagers()
        initializeUI()
    }
    
    private fun initializeComponents() {
        screenshotHelper = ScreenshotHelper(this)
        prefs = getSharedPreferences("aura_prefs", MODE_PRIVATE)
    }
    
    private fun setupManagers() {
        // Initialize VoiceManager
        voiceManager = VoiceManager(
            context = this,
            apiKeyProvider = ::loadApiKey,
            micPermissionProvider = ::isMicPermissionGranted
        ).apply {
            // Load settings from preferences
            ttsFeedbackEnabled = prefs.getBoolean("tts_feedback_enabled", true)
            ttsLanguage = prefs.getString("tts_language", "en") ?: "en"
            selectedVoice = prefs.getString("playai_voice", "Arista-PlayAI") ?: "Arista-PlayAI"
            ttsSpeed = prefs.getFloat("tts_speed", 1.2f)
            statusMessage = "Initializing AURA voice assistant..."
            conversationMode = true // Enable conversation mode by default
        }
        
        // Initialize AIManager
        aiManager = AIManager(
            context = this,
            apiKeyProvider = ::loadApiKey,
            voiceManager = voiceManager
        )
        
        // Initialize SystemManager
        systemManager = SystemManager(
            context = this,
            voiceManager = voiceManager,
            screenshotHelper = screenshotHelper
        )
        
        // Initialize EventManager
        eventManager = EventManager(
            context = this,
            voiceManager = voiceManager,
            aiManager = aiManager,
            systemManager = systemManager
        )
        
        // Register receivers
        eventManager.registerReceivers()
        
        // Initialize TTS and start voice service
        voiceManager.initializeTts {
            performInitialGreeting()
            if (isMicPermissionGranted()) {
                voiceManager.startEnhancedVoiceService()
            } else {
                voiceManager.statusMessage = "Microphone permission required"
            }
        }
    }
    
    private fun initializeUI() {
        setContent {
            Aura_mark3Theme {
                val context = LocalContext.current
                if (inSettings) {
                    SettingsScreen(
                        ttsFeedbackEnabled = voiceManager.ttsFeedbackEnabled,
                        onTtsFeedbackChange = { voiceManager.ttsFeedbackEnabled = it },
                        ttsLanguage = voiceManager.ttsLanguage,
                        onTtsLanguageChange = { voiceManager.ttsLanguage = it },
                        selectedVoice = voiceManager.selectedVoice,
                        onVoiceChange = { voiceManager.selectedVoice = it },
                        ttsSpeed = voiceManager.ttsSpeed,
                        onTtsSpeedChange = { voiceManager.ttsSpeed = it },
                        onBack = { inSettings = false },
                        micPermissionGranted = isMicPermissionGranted(),
                        onRequestMicPermission = ::requestMicPermission,
                        accessibilityEnabled = isAccessibilityEnabled(),
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onVoicePreview = { voice -> voiceManager.previewVoice(voice) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            VoiceAssistantUI(
                                modifier = Modifier.padding(innerPadding),
                                isListening = eventManager.isListeningForWakeWord || eventManager.isRecordingCommand || voiceManager.isRecording,
                                listeningType = when {
                                    voiceManager.isRecording -> "manual"
                                    eventManager.isRecordingCommand -> "command"
                                    eventManager.isListeningForWakeWord -> "wake_word"
                                    else -> "stopped"
                                },
                                audioLevel = eventManager.currentAudioLevel,
                                userTranscription = aiManager.userTranscription,
                                assistantSpeech = voiceManager.assistantSpeech,
                                statusMessage = voiceManager.statusMessage,
                                onManualRecord = { 
                                    Log.i("AURA_VOICE", "=== BUTTON CLICKED - onManualRecord callback triggered ===")
                                    runOnUiThread {
                                        if (voiceManager.isSpeaking) {
                                            Toast.makeText(this@MainActivity, "Interrupting AURA...", Toast.LENGTH_SHORT).show()
                                            voiceManager.resetSpeakingState()
                                        } else {
                                            Toast.makeText(this@MainActivity, "Starting recording...", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    manualVoiceActivation()
                                },
                                onSettings = { inSettings = true }
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Manual voice activation - handles button press
     */
    private fun manualVoiceActivation() {
        Log.i("AURA_VOICE", "=== MANUAL VOICE ACTIVATION TRIGGERED ===")
        Log.i("AURA_VOICE", "Current state - isRecording: ${voiceManager.isRecording}, isSpeaking: ${voiceManager.isSpeaking}, conversationMode: ${voiceManager.conversationMode}")
        
        if (voiceManager.isRecording) {
            voiceManager.stopManualRecording()
        } else {
            // Check if AURA is speaking and allow force stop
            if (voiceManager.isSpeaking) {
                Log.w("AURA_VOICE", "AURA is speaking - forcing reset")
                voiceManager.resetSpeakingState()
                return
            }
            
            // Check permissions first
            val micPermission = isMicPermissionGranted()
            Log.i("AURA_VOICE", "Microphone permission granted: $micPermission")
            if (!micPermission) {
                voiceManager.statusMessage = "Microphone permission required"
                voiceManager.speakWithPlayAITts("Please grant microphone permission to use voice commands.")
                return
            }
            
            voiceManager.startManualRecording()
        }
        
        // Ensure greeting has been given
        if (!aiManager.hasGreeted && !voiceManager.isRecording) {
            aiManager.hasGreeted = true
        }
    }
    
    /**
     * LLM-powered initial greeting
     */
    private fun performInitialGreeting() {
        if (!aiManager.hasGreeted) {
            voiceManager.statusMessage = "AURA is coming online..."
            aiManager.greetUserWithCompoundBeta()
        }
    }
    
    /**
     * Diagnostic function to check system status
     */
    private fun performSystemDiagnostic(): String {
        val diagnostics = mutableListOf<String>()
        
        diagnostics.add("Mic Permission: ${isMicPermissionGranted()}")
        diagnostics.add("Accessibility: ${isAccessibilityEnabled()}")
        diagnostics.add("Voice Service Running: ${voiceManager.isVoiceServiceRunning}")
        diagnostics.add("API Key Available: ${loadApiKey().isNotBlank()}")
        
        val result = diagnostics.joinToString(", ")
        Log.i("AURA_DIAGNOSTIC", result)
        return result
    }
    
    /**
     * Load API key from environment or properties file
     */
    private fun loadApiKey(): String {
        // Try environment variable first
        val envKey = System.getenv("GROQ_API_KEY")
        if (!envKey.isNullOrBlank()) {
            return "Bearer $envKey"
        }
        // Fallback to properties file
        return try {
            val properties = Properties()
            assets.open("api_keys.properties").use {
                properties.load(it)
            }
            val fileKey = properties.getProperty("groq_api_key", "")
            if (fileKey.isNotBlank()) "Bearer $fileKey" else ""
        } catch (e: Exception) {
            Log.e("AURA_API", "Failed to load API key", e)
            ""
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
    
    private fun requestMicPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        eventManager.unregisterReceivers()
        voiceManager.cleanup()
    }
}
