import httpx
import os
import json
from typing import Optional, Dict, Any
import logging
import asyncio

logger = logging.getLogger(__name__)

class GroqLLM:
    """Groq Large Language Model API wrapper"""
    
    def __init__(self):
        self.api_key = os.getenv("GROQ_API_KEY")
        self.base_url = "https://api.groq.com/openai/v1"
        self.model = "llama-3.3-70b-versatile"
        
        if not self.api_key:
            logger.error("GROQ_API_KEY not found in environment variables")
        
    async def analyze_intent(self, transcript: str, ui_tree: Optional[str] = None) -> Dict[str, Any]:
        """Analyze user intent and determine required actions"""
        if not self.api_key:
            logger.error("LLM: No API key available")
            return {"error": "No API key configured"}
        
        system_prompt = """You are AURA, an intelligent Android accessibility assistant. 
        Analyze the user's voice command and determine:
        1. The specific intent/goal
        2. What UI elements need to be interacted with
        3. The sequence of actions required
        4. Whether screen analysis is needed

        Respond in JSON format:
        {
            "intent": "brief description of what user wants",
            "target_elements": ["element1", "element2"],
            "requires_screen_analysis": true/false,
            "action_type": "tap|swipe|type|navigate|open_app|system_command",
            "parameters": {"key": "value"},
            "confidence": 0.0-1.0
        }"""
        
        user_prompt = f"User said: '{transcript}'"
        if ui_tree:
            user_prompt += f"\n\nAvailable UI elements: {ui_tree[:2000]}..."  # Truncate if too long
            
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                payload = {
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt}
                    ],
                    "temperature": 0.1,
                    "max_tokens": 1000,
                    "response_format": {"type": "json_object"}
                }
                
                headers = {
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json"
                }
                
                logger.info("LLM: Analyzing intent")
                response = await client.post(
                    f"{self.base_url}/chat/completions",
                    json=payload,
                    headers=headers
                )
                
                if response.status_code == 200:
                    result = response.json()
                    content = result["choices"][0]["message"]["content"]
                    try:
                        intent_data = json.loads(content)
                        logger.info(f"LLM: Intent analyzed - {intent_data.get('intent', 'Unknown')}")
                        return intent_data
                    except json.JSONDecodeError:
                        logger.warning("LLM: Failed to parse JSON response")
                        return {
                            "intent": content,
                            "requires_screen_analysis": True,
                            "action_type": "tap",
                            "confidence": 0.5
                        }
                else:
                    logger.error(f"LLM Error: {response.status_code} - {response.text}")
                    return {"error": f"LLM API error: {response.status_code}"}
                    
        except asyncio.TimeoutError:
            logger.error("LLM: Request timeout")
            return {"error": "LLM request timeout"}
        except Exception as e:
            logger.error(f"LLM Exception: {str(e)}")
            return {"error": str(e)}

    async def generate_response(self, intent: str, action_plan: list, success: bool = True) -> str:
        """Generate natural language response for user"""
        try:
            if success and action_plan:
                # Check the intent type for more natural responses
                intent_lower = intent.lower()
                
                if "greeting" in intent_lower:
                    return "Hello! I'm AURA, your accessibility assistant. How can I help you today?"
                elif "open" in intent_lower and "application" in intent_lower:
                    return "I'll open that app for you right away!"
                elif "time" in intent_lower:
                    from datetime import datetime
                    current_time = datetime.now().strftime("%I:%M %p")
                    return f"The current time is {current_time}."
                elif "screenshot" in intent_lower or "capture" in intent_lower:
                    return "I'll take a screenshot for you."
                elif "help" in intent_lower or "capability" in intent_lower:
                    return "I can help you with opening apps, taking screenshots, reading UI elements, and interacting with your device. What would you like me to do?"
                elif any(step.get("fallback") for step in action_plan):
                    return f"I understand you want to {intent}. Let me help you with that."
                else:
                    return f"I'll help you with {intent}."
            else:
                return "I'm sorry, I couldn't understand what you want me to do. Could you please try again? You can ask me to open apps, take screenshots, or help with UI interactions."
                
        except Exception as e:
            logger.error(f"Response generation error: {str(e)}")
            return "I encountered an issue processing your request. Please try again."

# Global instance
groq_llm = GroqLLM()
