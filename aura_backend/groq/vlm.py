import httpx
import os
import json
import base64
from typing import Optional, Dict, Any
import logging
import asyncio

logger = logging.getLogger(__name__)

class GroqVLM:
    """Groq Vision-Language Model API wrapper"""
    
    def __init__(self):
        self.api_key = os.getenv("GROQ_API_KEY")
        self.base_url = "https://api.groq.com/openai/v1"
        self.model = "llama-4-maverick-17b-128e-instruct"  # VLM model
        
        if not self.api_key:
            logger.error("GROQ_API_KEY not found in environment variables")
        
    async def locate_ui_element(self, screenshot: bytes, intent: str) -> Optional[Dict[str, Any]]:
        """Use VLM to locate UI elements on screen"""
        if not self.api_key:
            logger.error("VLM: No API key available")
            return {"found": False, "error": "No API key configured"}
        
        # Convert image to base64
        try:
            image_b64 = base64.b64encode(screenshot).decode('utf-8')
        except Exception as e:
            logger.error(f"VLM: Failed to encode image: {str(e)}")
            return {"found": False, "error": "Image encoding failed"}
        
        system_prompt = """You are a UI element locator. Analyze the screenshot and find the UI element 
        that matches the user's intent. Look for buttons, text fields, icons, or other interactive elements.
        
        Return coordinates in JSON format:
        {
            "found": true/false,
            "coordinates": {"x": int, "y": int, "width": int, "height": int},
            "confidence": 0.0-1.0,
            "element_description": "what you found",
            "reasoning": "why you chose this element"
        }
        
        If multiple elements match, choose the most likely one based on context."""
        
        user_prompt = f"""Find the UI element for this action: "{intent}"

Look for:
- Buttons with relevant text
- Input fields if typing is needed
- Icons or images that match the intent
- Navigation elements
- Any clickable areas

Provide exact pixel coordinates for the center of the element."""
        
        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                payload = {
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {
                            "role": "user", 
                            "content": [
                                {"type": "text", "text": user_prompt},
                                {
                                    "type": "image_url",
                                    "image_url": {
                                        "url": f"data:image/png;base64,{image_b64}"
                                    }
                                }
                            ]
                        }
                    ],
                    "temperature": 0.1,
                    "max_tokens": 500,
                    "response_format": {"type": "json_object"}
                }
                
                headers = {
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json"
                }
                
                logger.info("VLM: Analyzing screenshot for UI elements")
                response = await client.post(
                    f"{self.base_url}/chat/completions",
                    json=payload,
                    headers=headers
                )
                
                if response.status_code == 200:
                    result = response.json()
                    content = result["choices"][0]["message"]["content"]
                    try:
                        vlm_result = json.loads(content)
                        if vlm_result.get("found"):
                            logger.info(f"VLM: Found UI element - {vlm_result.get('element_description', 'Unknown')}")
                        else:
                            logger.warning("VLM: No matching UI element found")
                        return vlm_result
                    except json.JSONDecodeError:
                        logger.warning("VLM: Failed to parse JSON response")
                        return {"found": False, "error": "Could not parse VLM response"}
                else:
                    logger.error(f"VLM Error: {response.status_code} - {response.text}")
                    return {"found": False, "error": f"VLM API error: {response.status_code}"}
                    
        except asyncio.TimeoutError:
            logger.error("VLM: Request timeout")
            return {"found": False, "error": "VLM request timeout"}
        except Exception as e:
            logger.error(f"VLM Exception: {str(e)}")
            return {"found": False, "error": str(e)}

    async def analyze_screen_context(self, screenshot: bytes) -> Dict[str, Any]:
        """Analyze overall screen context and available elements"""
        if not self.api_key:
            return {"error": "No API key configured"}
            
        try:
            image_b64 = base64.b64encode(screenshot).decode('utf-8')
            
            system_prompt = """Analyze this Android screenshot and describe:
            1. What app/screen is displayed
            2. Main UI elements visible
            3. Possible actions user can take
            
            Return JSON format:
            {
                "app_name": "detected app",
                "screen_type": "main/settings/dialog/etc",
                "ui_elements": ["button1", "input_field", "menu"],
                "suggestions": ["possible actions"]
            }"""
            
            async with httpx.AsyncClient(timeout=30.0) as client:
                payload = {
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {
                            "role": "user",
                            "content": [
                                {"type": "text", "text": "Analyze this screen"},
                                {
                                    "type": "image_url",
                                    "image_url": {
                                        "url": f"data:image/png;base64,{image_b64}"
                                    }
                                }
                            ]
                        }
                    ],
                    "temperature": 0.2,
                    "max_tokens": 300
                }
                
                headers = {
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json"
                }
                
                response = await client.post(
                    f"{self.base_url}/chat/completions",
                    json=payload,
                    headers=headers
                )
                
                if response.status_code == 200:
                    result = response.json()
                    content = result["choices"][0]["message"]["content"]
                    try:
                        return json.loads(content)
                    except json.JSONDecodeError:
                        return {"screen_analysis": content}
                else:
                    return {"error": f"Screen analysis failed: {response.status_code}"}
                    
        except Exception as e:
            logger.error(f"Screen analysis error: {str(e)}")
            return {"error": str(e)}

# Global instance
groq_vlm = GroqVLM()
