import logging
import time

logger = logging.getLogger(__name__)

class ActionPlannerNode:
    """Create detailed action plan based on analysis results"""
    
    def __init__(self):
        self.name = "action_planner"
    
    async def run(self, state: dict) -> dict:
        """Create detailed action plan based on VLM or UI tree results"""
        start_time = time.time()
        logger.info("Action Planner Node: Creating action plan")
        
        try:
            intent_data = state.get("intent_data", {})
            ui_coords = state.get("ui_element_coords")
            intent = state.get("intent", "Unknown action")
            
            # Create action plan based on available information
            if ui_coords:
                # Create plan based on VLM coordinates
                action_plan = self._create_coordinate_based_plan(intent_data, ui_coords, intent)
                logger.info("Action Planner Node: Created coordinate-based action plan")
            else:
                # Create plan based on intent only (fallback)
                action_plan = self._create_intent_based_plan(intent_data, intent)
                logger.info("Action Planner Node: Created intent-based fallback plan")
            
            logger.info(f"Action Planner Node: Action plan created with {len(action_plan)} steps")
            
            return {
                **state,
                "action_plan": action_plan,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
            
        except Exception as e:
            logger.error(f"Action Planner Node error: {str(e)}")
            return {
                **state,
                "error": f"Action planning failed: {str(e)}",
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
    
    def _create_coordinate_based_plan(self, intent_data: dict, coords: dict, intent: str) -> list:
        """Create action plan using specific coordinates from VLM"""
        action_type = intent_data.get("action_type", "tap")
        confidence = intent_data.get("confidence", 0.7)
        
        # Primary action with coordinates
        plan = [{
            "type": action_type,
            "x": coords.get("x"),
            "y": coords.get("y"),
            "width": coords.get("width"),
            "height": coords.get("height"),
            "description": f"Tap on UI element at ({coords.get('x')}, {coords.get('y')}) to {intent}",
            "confidence": confidence,
            "source": "vlm",
            "method": "coordinate_based"
        }]
        
        # Add text input if needed
        parameters = intent_data.get("parameters", {})
        if action_type == "type" and "text" in parameters:
            plan.append({
                "type": "type",
                "text": parameters["text"],
                "description": f"Type '{parameters['text']}' into the field",
                "confidence": confidence,
                "source": "intent"
            })
        
        return plan
    
    def _create_intent_based_plan(self, intent_data: dict, intent: str) -> list:
        """Create action plan based on intent analysis only (fallback)"""
        action_type = intent_data.get("action_type", "speak")
        confidence = intent_data.get("confidence", 0.3)
        category = intent_data.get("_category", "")
        
        # Handle greetings and conversational responses
        if action_type == "respond" or category == "greeting" or category == "help":
            response_type = intent_data.get("_response_type", "simple_greeting")
            
            if response_type == "capabilities_explanation":
                response = """Hi there! I'm AURA, your Android accessibility assistant! 🎤 Here's what I can do for you:

📱 **App Control**: Open any app, switch between apps, or close them
👆 **Screen Interaction**: Tap buttons, scroll, swipe, type text, and navigate menus  
🔧 **System Control**: Adjust WiFi, Bluetooth, volume, brightness, and other settings
👁️ **Screen Reading**: Tell you what's on your screen, read notifications, or describe content
💬 **Smart Conversations**: Chat naturally - I understand context and can help with complex tasks
📸 **Screenshots**: Capture and analyze your screen to provide better assistance

Just speak naturally! Try saying things like:
• "Open WhatsApp and send a message to John"
• "What's on my screen right now?"
• "Turn on WiFi" or "Increase the volume"
• "Help me navigate to Settings"

I'm designed to make your Android experience more accessible and intuitive. What would you like me to help you with today?"""
                
            elif response_type == "greeting_with_capabilities":
                response = """Hey there! Great to hear from you! I'm AURA, your intelligent Android assistant. 👋

I'm here to help you control your device with just your voice! I can open apps, interact with your screen, adjust settings, read content aloud, and even have fun conversations like this one.

Think of me as your personal Android companion - whether you need to send a quick message, check what's on your screen, or navigate through complex menus, I've got you covered!

What would you like to try first? 😊"""
                
            elif response_type == "time_based_greeting":
                if "morning" in intent.lower():
                    response = "Good morning! ☀️ I'm AURA, ready to help you start your day right! Whether you need to check messages, open apps, or navigate your device, I'm here to assist. How can I make your morning smoother?"
                elif "afternoon" in intent.lower():
                    response = "Good afternoon! 🌤️ I'm AURA, your Android accessibility assistant. Hope you're having a great day! I'm ready to help with any app launches, screen interactions, or device controls you need."
                elif "evening" in intent.lower():
                    response = "Good evening! 🌙 I'm AURA, here to help you with your Android device. Winding down for the day? I can help you check messages, adjust settings, or navigate through any apps you need."
                else:
                    response = "Hello! I'm AURA, your Android accessibility assistant. I'm ready to help you navigate, control, and interact with your device using just your voice!"
                    
            else:  # simple_greeting
                greetings = [
                    "Hey! 👋 I'm AURA, your voice-powered Android assistant! I can help you control apps, navigate screens, and make your device more accessible. What can I do for you?",
                    "Hi there! I'm AURA! 🎤 Think of me as your personal Android companion - I can open apps, read your screen, adjust settings, and chat with you. How can I help today?",
                    "Hello! Great to meet you! I'm AURA, your intelligent Android accessibility assistant. I make controlling your device as easy as having a conversation. What would you like to try?",
                    "Hey! I'm AURA! ✨ I'm designed to make your Android experience smoother and more accessible through natural voice commands. Ready to see what I can do?"
                ]
                
                # Choose a greeting based on some variation (could be random or based on time)
                import hashlib
                hash_obj = hashlib.md5(intent.encode())
                greeting_index = int(hash_obj.hexdigest(), 16) % len(greetings)
                response = greetings[greeting_index]
            
            return [{
                "type": "speak",
                "text": response,
                "description": f"Engaging conversational response: {response_type}",
                "confidence": 0.98,
                "source": "conversational",
                "method": "personalized_response",
                "requires_screen": False
            }]
        
        elif action_type == "speak" or not intent_data:
            # Provide helpful feedback when we can't execute the action
            return [{
                "type": "speak",
                "text": f"I understand you want to {intent}, but I need a clearer view of the current screen to help you.",
                "description": "Provide feedback to user about needing more information",
                "confidence": 0.9,
                "source": "fallback",
                "method": "intent_based"
            }]
        elif action_type == "open_app":
            # App opening doesn't require coordinates
            app_name = intent_data.get("parameters", {}).get("app_name", "")
            return [{
                "type": "open_app",
                "app_name": app_name,
                "description": f"Open {app_name or 'requested application'}",
                "confidence": confidence,
                "source": "intent",
                "method": "app_launch"
            }]
        elif action_type == "system_command":
            # System commands like volume, brightness, etc.
            command = intent_data.get("parameters", {}).get("command", "")
            return [{
                "type": "system_command",
                "command": command,
                "description": f"Execute system command: {command}",
                "confidence": confidence,
                "source": "intent",
                "method": "system_action"
            }]
        else:
            # Generic fallback with suggestion
            return [{
                "type": "speak",
                "text": f"I'll help you {intent}. Please make sure the relevant screen is visible and try again.",
                "description": f"Attempt to execute {intent} with guidance",
                "confidence": confidence,
                "source": "fallback",
                "method": "guided_retry",
                "fallback": True
            }]

# Create node instance
action_planner_node = ActionPlannerNode()
