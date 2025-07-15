package com.aura.aura_mark3.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aura.aura_mark3.accessibility.AuraAccessibilityService
import com.aura.aura_mark3.utils.ScreenshotHelper

/**
 * Manages system actions and device interactions
 */
class SystemManager(
    private val context: Context,
    private val voiceManager: VoiceManager,
    private val screenshotHelper: ScreenshotHelper
) {
    var currentStep by mutableStateOf("")
    var isExecutingActions = false
        private set
    var screenshotBitmap by mutableStateOf<Bitmap?>(null)
    
    private var actionQueue: MutableList<AuraAction> = mutableListOf()
    private var pendingVlmAction: ((Bitmap?) -> Unit)? = null
    
    fun startActionQueue(actions: List<AuraAction>) {
        actionQueue.clear()
        actionQueue.addAll(actions)
        isExecutingActions = true
        currentStep = ""
        executeNextAction()
    }

    private fun executeNextAction() {
        if (actionQueue.isNotEmpty()) {
            val action = actionQueue.removeAt(0)
            executeAction(action)
        } else {
            isExecutingActions = false
            currentStep = "All actions completed"
            Log.i("AURA_ACTION", "Action queue completed")
        }
    }

    private fun executeAction(action: AuraAction) {
        currentStep = "Executing: ${action.action}"
        Log.i("AURA_SYSTEM", "Executing action: ${action.action}")
        
        when (action.action) {
            "SYSTEM_ACTION" -> {
                when (action.label) {
                    "bluetooth" -> setBluetoothEnabled(action.text == "on")
                    "wifi" -> setWifiEnabled(action.text == "on")
                    "brightness" -> setScreenBrightness(action.text?.toIntOrNull() ?: 128)
                    else -> {
                        voiceManager.speakWithPlayAITts("System action '${action.label}' not supported")
                        executeNextAction()
                    }
                }
            }
            "LAUNCH_APP" -> launchApp(action.label)
            "TAKE_SCREENSHOT" -> takeScreenshot()
            "VLM_ACTION" -> {
                action.query?.let { query ->
                    takeScreenshotForVlm(query, action.action, action.direction ?: "DOWN", action.text ?: "")
                } ?: executeNextAction()
            }
            else -> {
                voiceManager.speakWithPlayAITts("Unknown action: ${action.action}")
                executeNextAction()
            }
        }
    }

    // --- System Action Handlers ---
    private fun setBluetoothEnabled(enable: Boolean) {
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                voiceManager.speakWithPlayAITts("Bluetooth is not available on this device.")
                return
            }
            
            val permission = context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                voiceManager.speakWithPlayAITts("Bluetooth permission is required.")
                return
            }
            
            if (enable && !bluetoothAdapter.isEnabled) {
                voiceManager.speakWithPlayAITts("Turning Bluetooth on.")
            } else if (!enable && bluetoothAdapter.isEnabled) {
                voiceManager.speakWithPlayAITts("Turning Bluetooth off.")
            } else {
                voiceManager.speakWithPlayAITts("Bluetooth is already ${if (enable) "on" else "off"}.")
            }
        } catch (e: SecurityException) {
            voiceManager.speakWithPlayAITts("Bluetooth permission denied by system.")
        } catch (e: Exception) {
            voiceManager.speakWithPlayAITts("Failed to change Bluetooth state.")
        }
    }

    private fun setWifiEnabled(enable: Boolean) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (wifiManager == null) {
                voiceManager.speakWithPlayAITts("WiFi is not available on this device.")
                return
            }
            
            // Modern approach - direct to WiFi settings
            context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            voiceManager.speakWithPlayAITts("Opening WiFi settings.")
        } catch (e: Exception) {
            voiceManager.speakWithPlayAITts("Failed to change WiFi state.")
        }
    }

    private fun setScreenBrightness(value: Int) {
        try {
            val brightness = value.coerceIn(0, 255)
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
            voiceManager.speakWithPlayAITts("Set screen brightness to $brightness.")
        } catch (e: Exception) {
            voiceManager.speakWithPlayAITts("Failed to set screen brightness.")
        }
    }

    private fun launchApp(appName: String) {
        val packageName = findPackageByAppLabel(appName)
        if (packageName != null) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    voiceManager.speakWithPlayAITts("Opening $appName.")
                } else {
                    voiceManager.speakWithPlayAITts("Cannot open $appName. No launch activity found.")
                }
            } catch (e: Exception) {
                voiceManager.speakWithPlayAITts("Failed to open $appName.")
            }
        }
    }

    private fun findPackageByAppLabel(label: String?): String? {
        if (label.isNullOrBlank()) return null
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
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
            voiceManager.speakWithPlayAITts("Did you mean: ${suggestions.joinToString(", ")}?")
        } else {
            voiceManager.speakWithPlayAITts("App $label not found.")
        }
        return null
    }

    private fun takeScreenshot() {
        // Implementation depends on your screenshot system
        voiceManager.speakWithPlayAITts("Taking screenshot.")
    }

    private fun takeScreenshotForVlm(query: String, action: String, direction: String = "DOWN", text: String = "") {
        // This would integrate with your VLM system
        voiceManager.speakWithPlayAITts("Analyzing screen for $query.")
        executeNextAction() // Continue for now
    }

    fun sendGestureAtCoordinates(x: Int, y: Int) {
        val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_GESTURE_AT_COORDS).apply {
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_X, x)
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_Y, y)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    fun sendScrollAtCoordinates(x: Int, y: Int, direction: String) {
        val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_SCROLL_AT_COORDS).apply {
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_X, x)
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_Y, y)
            putExtra(AuraAccessibilityService.EXTRA_SCROLL_DIRECTION, direction)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    fun sendInputAtCoordinates(x: Int, y: Int, text: String) {
        val intent = Intent(AuraAccessibilityService.ACTION_PERFORM_INPUT_AT_COORDS).apply {
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_X, x)
            putExtra(AuraAccessibilityService.EXTRA_GESTURE_Y, y)
            putExtra(AuraAccessibilityService.EXTRA_INPUT_TEXT, text)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
}
