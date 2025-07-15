# AURA Android Backend Integration

## 🚀 Overview

This integration connects your Android AURA app with the intelligent Python backend, replacing direct API calls with orchestrated workflows powered by LangGraph.

## 📁 New Files Added

### Core Integration
- `AuraBackendApi.kt` - Retrofit interface for backend communication
- `EnhancedAIManager.kt` - New AI manager with backend integration
- `AudioFileUtils.kt` - Audio file management utilities
- `AuraBackendIntegration.kt` - Integration helper and migration guide

### Modified Files
- `AudioRecorderService.kt` - Now saves audio files for backend processing

## 🎯 Key Features

### Backend-Powered Intelligence
- **LangGraph Orchestration**: 6-node workflow (STT → Intent → UI Check → VLM → Action Planner → TTS)
- **Intelligent Routing**: Commands automatically routed to appropriate processing nodes
- **Multi-Step Actions**: Complex workflows like email automation
- **Conversation State**: Persistent conversation memory across interactions

### Enhanced Capabilities
- **Screen Interaction**: Send screenshots + voice for visual context
- **Action Plans**: Receive step-by-step action instructions
- **Error Resilience**: Robust error handling and fallback mechanisms
- **Session Management**: Unique session IDs for conversation tracking

## 🔧 Integration Steps

### 1. Start the Backend
```bash
cd aura_backend
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 2. Update Your MainActivity
```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var voiceManager: VoiceManager
    private lateinit var enhancedAI: EnhancedAIManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize voice manager (existing)
        voiceManager = VoiceManager(this)
        
        // NEW: Create enhanced AI manager
        enhancedAI = AuraBackendIntegration.createEnhancedAIManager(
            context = this,
            voiceManager = voiceManager
        )
        
        // Check backend health
        AuraBackendIntegration.checkBackendHealth(this) { isHealthy, message ->
            if (isHealthy) {
                enhancedAI.greetUserWithBackend()
            }
        }
    }
}
```

### 3. Update Your UI Components
Replace existing AIManager references with EnhancedAIManager:
```kotlin
// OLD
aiManager.processVoiceCommand(command)

// NEW  
enhancedAI.processVoiceCommandWithBackend(command)
```

## 🌐 Backend Endpoints

### `/chat` - Text-only conversations
- **Input**: `{ "text": "your command", "session_id": "unique_id" }`
- **Output**: `{ "success": true, "response": "AURA's reply", "intent": "classified_intent" }`

### `/process` - Voice + Screenshot processing
- **Input**: Multipart form (audio file + screenshot + session_id)
- **Output**: Response + action plan for complex workflows

### `/health` - Backend status check
- **Output**: `{ "status": "operational", "timestamp": "..." }`

## 🎮 Example Commands

### Simple Conversations
- "What time is it?" → Intelligent time response
- "Tell me a joke" → Backend-generated humor
- "How are you?" → Contextual responses

### Screen Interactions  
- "Tap the send button" → Screenshot analysis + tap coordinates
- "Open Gmail" → App launch + UI navigation
- "Send email to John" → Multi-step email composition workflow

### Complex Workflows
- "Send an email to my boss saying I'll be late" → 
  1. Analyze screen context
  2. Open email app
  3. Compose message
  4. Execute send action

## 🔍 Backend URL Configuration

### Development (Android Emulator)
```kotlin
const val BACKEND_URL = "http://10.0.2.2:8000/"
```

### Real Device (Same Network)
```kotlin
const val BACKEND_URL = "http://YOUR_COMPUTER_IP:8000/"
```

### Production
```kotlin
const val BACKEND_URL = "https://your-aura-backend.com/"
```

## 📊 Processing Flow

### Text-Only Commands
```
User Speech → AudioRecorderService → EnhancedAIManager 
→ Backend /chat → LangGraph Processing → Response
```

### Screen Interaction Commands
```
User Speech + Screenshot → EnhancedAIManager 
→ Backend /process → LangGraph (STT+VLM+Action Planning) 
→ Response + Action Steps → Android Execution
```

## 🛠️ Debugging

### Check Backend Connection
```kotlin
enhancedAI.checkBackendHealth { isHealthy ->
    Log.d("AURA", "Backend healthy: $isHealthy")
}
```

### Monitor Logs
- **Android**: Filter by "AURA_BACKEND", "AURA_AI"
- **Backend**: See comprehensive processing logs

### Common Issues
1. **"Backend unreachable"** → Check URL and ensure backend is running
2. **"No audio file"** → Verify AudioRecorderService is saving files
3. **"Screen analysis failed"** → Ensure screenshot permissions

## 🚀 Advanced Features

### Session Management
Each conversation gets a unique session ID for context continuity:
```kotlin
private var currentSessionId = generateSessionId()
```

### Audio File Management
Automatic cleanup of old audio files:
```kotlin
AudioFileUtils.cleanupOldAudioFiles(context)
```

### Fallback Mechanisms
If backend fails, graceful degradation to direct API calls:
```kotlin
private fun handleBackendError(message: String) {
    // Fallback to direct Groq API or offline responses
}
```

## 📈 Benefits Over Direct API

### Intelligence
- ✅ Context-aware responses vs. ❌ Isolated API calls
- ✅ Multi-step workflows vs. ❌ Single-action responses  
- ✅ Screen understanding vs. ❌ Voice-only processing

### Reliability
- ✅ Orchestrated error handling vs. ❌ Individual API failures
- ✅ Conversation memory vs. ❌ Stateless interactions
- ✅ Intelligent routing vs. ❌ Manual API selection

### Extensibility
- ✅ Easy to add new capabilities vs. ❌ Complex Android modifications
- ✅ Backend logic updates vs. ❌ App store releases
- ✅ Centralized intelligence vs. ❌ Distributed complexity

## 🔄 Migration Checklist

- [ ] Backend server running and accessible
- [ ] EnhancedAIManager integrated in MainActivity
- [ ] UI components updated to use new manager
- [ ] Backend health check implemented
- [ ] Audio file saving verified
- [ ] Screenshot functionality tested
- [ ] Error handling and fallbacks confirmed
- [ ] Session management working
- [ ] Complex workflows tested (email automation)

Your AURA app now has intelligent backend orchestration! 🎉
