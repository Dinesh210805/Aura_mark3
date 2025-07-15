# AURA Android Integration Guide

## Backend Status ✅
Your backend is successfully running and all tests pass! Here's how to integrate it with your Android app.

## 1. Backend Endpoints for Your Android App

### Base URL
```
http://localhost:8000
```

### Main Processing Endpoint
```kotlin
// POST /process
// For voice commands with optional screenshot and UI tree
POST http://localhost:8000/process
Content-Type: multipart/form-data

Fields:
- audio: UploadFile (WAV, MP3 audio from microphone)
- screenshot: UploadFile (optional PNG from accessibility service)  
- ui_tree: String (optional XML from accessibility service)
- session_id: String (optional, for conversation continuity)
```

### Text Chat Endpoint (for testing)
```kotlin
// POST /chat  
POST http://localhost:8000/chat
Content-Type: application/json

Body:
{
    "text": "Open settings",
    "session_id": "user-123"
}
```

## 2. Update Your Android AIManager

Replace your current `processVoiceCommand()` method:

```kotlin
// In AIManager.kt
class AIManager(private val context: Context) {
    private val baseUrl = "http://localhost:8000" // Change for production
    private val httpClient = OkHttpClient()
    
    suspend fun processVoiceCommand(
        audioFile: File,
        screenshot: File? = null,
        uiTree: String? = null,
        sessionId: String = UUID.randomUUID().toString()
    ): AuraResponse {
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio", "audio.wav",
                audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            )
            
        // Add screenshot if available
        screenshot?.let {
            requestBody.addFormDataPart(
                "screenshot", "screen.png",
                it.asRequestBody("image/png".toMediaTypeOrNull())
            )
        }
        
        // Add UI tree if available
        uiTree?.let {
            requestBody.addFormDataPart("ui_tree", it)
        }
        
        requestBody.addFormDataPart("session_id", sessionId)
        
        val request = Request.Builder()
            .url("$baseUrl/process")
            .post(requestBody.build())
            .build()
            
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    parseAuraResponse(responseBody)
                } else {
                    AuraResponse.error("Backend error: ${response.code}")
                }
            } catch (e: Exception) {
                AuraResponse.error("Network error: ${e.message}")
            }
        }
    }
    
    private fun parseAuraResponse(jsonResponse: String?): AuraResponse {
        // Parse the JSON response from backend
        val gson = Gson()
        val response = gson.fromJson(jsonResponse, BackendResponse::class.java)
        
        return AuraResponse(
            success = response.success,
            transcript = response.transcript,
            intent = response.intent,
            actionPlan = response.action_plan,
            ttsAudio = response.tts_audio?.let { Base64.decode(it, Base64.DEFAULT) },
            responseText = response.response_text
        )
    }
}

// Data classes for backend communication
data class BackendResponse(
    val success: Boolean,
    val transcript: String?,
    val intent: String?, 
    val action_plan: List<ActionStep>,
    val tts_audio: String?, // Base64 encoded
    val response_text: String?,
    val session_id: String?
)

data class ActionStep(
    val type: String, // "tap", "swipe", "type", "speak", etc.
    val x: Int?,
    val y: Int?, 
    val text: String?,
    val description: String,
    val confidence: Float?
)
```

## 3. Update Your VoiceManager

Modify VoiceManager to use the new AIManager:

```kotlin
// In VoiceManager.kt
suspend fun processVoiceInput(audioFile: File) {
    try {
        // Get screenshot if needed
        val screenshot = systemManager.takeScreenshot()
        
        // Get UI tree if available  
        val uiTree = systemManager.getAccessibilityTree()
        
        // Process with backend
        val response = aiManager.processVoiceCommand(
            audioFile = audioFile,
            screenshot = screenshot,
            uiTree = uiTree,
            sessionId = currentSessionId
        )
        
        if (response.success) {
            // Play TTS response
            response.ttsAudio?.let { audio ->
                playTTSAudio(audio)
            }
            
            // Execute actions
            executeActionPlan(response.actionPlan)
        } else {
            handleError(response.errorMessage)
        }
        
    } catch (e: Exception) {
        Log.e("VoiceManager", "Voice processing failed", e)
        speakError("Sorry, I couldn't process that request")
    }
}

private suspend fun executeActionPlan(actionPlan: List<ActionStep>) {
    for (action in actionPlan) {
        when (action.type) {
            "tap" -> {
                action.x?.let { x ->
                    action.y?.let { y ->
                        systemManager.performTap(x, y)
                    }
                }
            }
            "type" -> {
                action.text?.let { text ->
                    systemManager.typeText(text)
                }
            }
            "speak" -> {
                action.text?.let { text ->
                    ttsManager.speak(text)
                }
            }
            "open_app" -> {
                action.text?.let { appName ->
                    systemManager.openApp(appName)
                }
            }
            // Add more action types as needed
        }
        delay(500) // Small delay between actions
    }
}
```

## 4. Test Your Integration

### Step 1: Run the Backend Test
```bash
cd d:\PROJECTS\Aura_mark3\aura_backend
python test_integration.py
```

### Step 2: Start Your Backend
```bash
python run.py
```

### Step 3: Test from Android
Update your Android app with the new AIManager code above, then test with:
1. Simple voice commands: "Hello", "What time is it?"
2. UI interactions: "Open settings", "Click the back button"
3. App navigation: "Open WhatsApp", "Go to camera"

## 5. Expected Response Format

Your Android app will receive responses like this:

```json
{
    "success": true,
    "transcript": "Open WhatsApp",
    "intent": "Launch WhatsApp application",
    "action_plan": [
        {
            "type": "open_app",
            "text": "WhatsApp",
            "description": "Open WhatsApp application",
            "confidence": 0.9
        }
    ],
    "tts_audio": "base64_encoded_audio_data",
    "response_text": "Opening WhatsApp for you",
    "session_id": "user-session-123"
}
```

## 6. Troubleshooting

### Backend Not Responding
- Check if server is running: `curl http://localhost:8000/health`
- Check firewall settings
- Verify .env file has correct GROQ_API_KEY

### Audio Issues
- Ensure audio files are WAV format
- Check file size limits (max 10MB recommended)
- Verify audio encoding (16kHz, 16-bit recommended)

### Network Issues from Android
- Use `10.0.2.2:8000` for Android emulator
- Use your computer's IP address for physical devices
- Add INTERNET permission in AndroidManifest.xml

## 7. Production Deployment

For production, deploy your backend to:
- **Cloud**: AWS, Google Cloud, Azure
- **VPS**: DigitalOcean, Linode
- **Containerized**: Docker + Kubernetes

Update the `baseUrl` in your Android app accordingly.

---

🎉 **Your AURA backend is now ready for Android integration!**
