from groq.tts import playai_tts, fallback_tts
from groq.llm import groq_llm
import logging
import time
import traceback
import json

logger = logging.getLogger(__name__)

class TTSNode:
    """Text-to-Speech node using Play.ai"""
    
    def __init__(self):
        self.name = "tts"
    
    async def run(self, state: dict) -> dict:
        """Generate TTS audio and finalize response"""
        start_time = time.time()
        logger.info("🔊 TTS Node: Starting text-to-speech generation")
        logger.info(f"🔊 TTS Node: Input state keys: {list(state.keys())}")
        logger.info(f"🔊 TTS Node: Intent: '{state.get('intent', 'None')}'")
        logger.info(f"🔊 TTS Node: Action plan: {state.get('action_plan', [])}")
        
        try:
            action_plan = state.get("action_plan", [])
            intent = state.get("intent", "")
            error = state.get("error")
            
            logger.info(f"🔊 TTS Node: Processing - Error: {error}, Intent: {intent}")
            
            # Generate appropriate response text
            # Check if this is a simple action that doesn't need screen
            action_plan = state.get("action_plan", [])
            is_simple_action = any(
                action.get("requires_screen") == False 
                for action in action_plan
            ) if action_plan else False
            
            if error and not is_simple_action:
                response_text = self._generate_error_response(error, intent)
                logger.info(f"🔊 TTS Node: Generated error response: '{response_text}'")
            else:
                response_text = await self._generate_success_response(action_plan, intent)
                logger.info(f"🔊 TTS Node: Generated success response: '{response_text}'")
                # Clear error for simple actions that don't need screen
                if is_simple_action:
                    error = None
            
            # Generate TTS audio
            logger.info("🔊 TTS Node: Calling Play.ai TTS...")
            tts_audio = await playai_tts.generate_speech(response_text)
            
            # If Play.ai fails, try fallback (or skip TTS)
            if not tts_audio:
                logger.warning("⚠️ TTS Node: Play.ai TTS failed, using fallback")
                tts_audio = await fallback_tts.generate_speech(response_text)
            
            # Build final response
            result = {
                **state,
                "response_text": response_text,
                "complete": True,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
            
            # Handle TTS audio separately - don't store bytes in state
            if tts_audio:
                # Store audio metadata instead of raw bytes
                result["tts_audio_available"] = True
                result["tts_audio_size"] = len(tts_audio)
                logger.info(f"✅ TTS Node: TTS generation successful ({len(tts_audio)} bytes)")
                
                # Store audio in a separate cache/storage if needed
                # For now, we'll just indicate it's available
            else:
                logger.warning("⚠️ TTS Node: TTS generation failed, text-only response")
                result["tts_audio_available"] = False
                result["tts_error"] = "TTS generation failed"
            
            logger.info(f"✅ TTS Node: Final result keys: {list(result.keys())}")
            return result
                
        except Exception as e:
            logger.error(f"TTS Node error: {str(e)}")
            return {
                **state,
                "response_text": "I encountered an issue processing your request.",
                "error": f"TTS processing failed: {str(e)}",
                "complete": True,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
    
    def _generate_error_response(self, error: str, intent: str) -> str:
        """Generate appropriate error response"""
        if "timeout" in error.lower():
            return "I'm sorry, the request is taking longer than expected. Please try again."
        elif "api" in error.lower() or "key" in error.lower():
            return "I'm having trouble connecting to my services. Please check your connection and try again."
        elif "audio" in error.lower():
            return "I couldn't hear you clearly. Please try speaking again."
        elif "screenshot" in error.lower() or "image" in error.lower():
            return "I need to see the screen to help you with that. Please make sure screen sharing is enabled."
        else:
            return f"I'm sorry, I couldn't complete that action. {intent}"
    
    async def _generate_success_response(self, action_plan: list, intent: str) -> str:
        """Generate appropriate success response"""
        if not action_plan:
            return "I'm ready to help, but I need more information about what you'd like me to do."
        
        # Check if this is a fallback response
        has_fallback = any(step.get("fallback") for step in action_plan)
        if has_fallback:
            # Use the fallback text if available
            fallback_step = next((step for step in action_plan if step.get("fallback")), {})
            return fallback_step.get("text", f"I'll help you with {intent}.")
        
        # Check action types
        speak_actions = [step for step in action_plan if step.get("type") == "speak"]
        if speak_actions:
            return speak_actions[0].get("text", f"Executing {intent}")
        
        # Check for coordinate-based actions
        coordinate_actions = [step for step in action_plan if step.get("x") is not None]
        if coordinate_actions:
            return f"I found the element on screen. Executing your request: {intent}"
        
        # Check for app launches
        app_actions = [step for step in action_plan if step.get("type") == "open_app"]
        if app_actions:
            app_name = app_actions[0].get("app_name", "the app")
            return f"Opening {app_name} for you."
        
        # Check for system commands
        system_actions = [step for step in action_plan if step.get("type") == "system_command"]
        if system_actions:
            # Special handling for common requests
            if "time" in intent.lower():
                import datetime
                current_time = datetime.datetime.now().strftime("%I:%M %p")
                return f"The current time is {current_time}."
            elif "date" in intent.lower():
                import datetime
                current_date = datetime.datetime.now().strftime("%A, %B %d, %Y")
                return f"Today is {current_date}."
            elif "weather" in intent.lower():
                return "I would need to check the weather app for current weather information."
            else:
                return f"I can help you with {intent}. Let me process that for you."
        
        # Default success response
        return f"Executing your request: {intent}"

# Create node instance
tts_node = TTSNode()
