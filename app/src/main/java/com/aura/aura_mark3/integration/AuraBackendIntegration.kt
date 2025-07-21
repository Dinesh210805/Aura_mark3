package com.aura.aura_mark3.integration

import android.content.Context
import android.content.Intent
import com.aura.aura_mark3.managers.EnhancedAIManager
import com.aura.aura_mark3.managers.VoiceManager
import com.aura.aura_mark3.managers.SystemManager

/**
 * AURA Backend Integration Helper
 * This class helps you integrate the new backend-powered AI manager
 */
class AuraBackendIntegration {
    
    companion object {
        // Default backend URL - adjust this based on your setup
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000/"
        
        /**
         * Create an EnhancedAIManager instance with backend integration
         */
        fun createEnhancedAIManager(
            context: Context,
            voiceManager: VoiceManager,
            backendUrl: String = DEFAULT_BACKEND_URL,
            systemManager: SystemManager? = null
        ): EnhancedAIManager {
            val enhancedAI = EnhancedAIManager(context, voiceManager, backendUrl, systemManager)
            return enhancedAI
        }
        
        /**
         * Check if backend is reachable
         */
        fun checkBackendHealth(
            context: Context,
            backendUrl: String = DEFAULT_BACKEND_URL,
            onResult: (Boolean, String) -> Unit
        ) {
            val enhancedAI = EnhancedAIManager(
                context, 
                VoiceManager(context, { "" }, { true }), // Dummy providers for health check
                backendUrl
            )
            
            enhancedAI.checkBackendHealth { isHealthy ->
                val message = if (isHealthy) {
                    "✅ AURA Backend is operational and ready!"
                } else {
                    "❌ Backend unreachable. Please start the backend server."
                }
                onResult(isHealthy, message)
            }
        }
        
        /**
         * Migration guide from old AIManager to EnhancedAIManager
         */
        fun getMigrationInstructions(): String {
            return """
            🔄 AURA Backend Integration Migration Guide:
            
            1. Replace AIManager with EnhancedAIManager:
               OLD: AIManager(context, voiceManager)
               NEW: AuraBackendIntegration.createEnhancedAIManager(context, voiceManager)
            
            2. Update method calls:
               OLD: aiManager.greetUser()
               NEW: enhancedAI.greetUserWithBackend()
               
               OLD: aiManager.processVoiceCommand(command)
               NEW: enhancedAI.processVoiceCommandWithBackend(command)
            
            3. Benefits of the new backend:
               ✅ Intelligent command routing
               ✅ Multi-step action plans (like email automation)
               ✅ Better error handling
               ✅ Conversation state management
               ✅ Enhanced screen interaction capabilities
            
            4. Backend Setup:
               - Start the Python backend: uvicorn main:app --reload
               - Ensure backend URL is correct: ${DEFAULT_BACKEND_URL}
               - Check backend health before using
            """.trimIndent()
        }
    }
}

/**
 * Example MainActivity integration
 */
class ExampleMainActivityIntegration {
    
    fun integrateInMainActivity(): String {
        return """
        // In your MainActivity or main AURA activity:
        
        class MainActivity : ComponentActivity() {
            private lateinit var voiceManager: VoiceManager
            private lateinit var enhancedAI: EnhancedAIManager
            
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                
                // Initialize voice manager (existing code)
                voiceManager = VoiceManager(this)
                
                // NEW: Create enhanced AI manager with backend
                enhancedAI = AuraBackendIntegration.createEnhancedAIManager(
                    context = this,
                    voiceManager = voiceManager,
                    backendUrl = "http://10.0.2.2:8000/" // Adjust if needed
                )
                
                // Check backend health on startup
                AuraBackendIntegration.checkBackendHealth(this) { isHealthy, message ->
                    runOnUiThread {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        
                        if (isHealthy) {
                            // Backend is ready, greet user
                            enhancedAI.greetUserWithBackend()
                        } else {
                            // Fallback to direct API mode or show setup instructions
                            Toast.makeText(this, "Please start the AURA backend server", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
                setContent {
                    // Your existing Compose UI
                    AuraMainScreen(
                        voiceManager = voiceManager,
                        enhancedAI = enhancedAI // Pass the enhanced AI manager
                    )
                }
            }
        }
        
        // In your Compose UI:
        @Composable
        fun AuraMainScreen(
            voiceManager: VoiceManager,
            enhancedAI: EnhancedAIManager
        ) {
            // Your existing UI components
            
            // Voice command button example:
            Button(
                onClick = {
                    if (enhancedAI.isProcessingRequest) {
                        voiceManager.stopListening()
                    } else {
                        voiceManager.startListening()
                    }
                }
            ) {
                Text(
                    text = when {
                        enhancedAI.isProcessingRequest -> "🧠 AURA is thinking..."
                        voiceManager.isListening -> "🎙️ Listening..."
                        else -> "🎤 Talk to AURA"
                    }
                )
            }
        }
        """.trimIndent()
    }
}
