import httpx
import os
from typing import Optional
import logging
import asyncio
import json

logger = logging.getLogger(__name__)

class GroqPlayAITTS:
    """Groq PlayAI Text-to-Speech API wrapper"""
    
    def __init__(self):
        self.api_key = os.getenv("GROQ_API_KEY")
        self.base_url = "https://api.groq.com/openai/v1"
        self.model = "playai-tts"
        self.default_voice = "Arista-PlayAI"  # High-quality voice
        
        if not self.api_key:
            logger.error("GROQ_API_KEY not found in environment variables")
        
    async def generate_speech(self, text: str, voice: str = None) -> Optional[bytes]:
        """Generate TTS audio using Play.ai"""
        if not self.api_key:
            logger.error("TTS: No API key available")
            return None
            
        if not text or not text.strip():
            logger.warning("TTS: Empty text provided")
            return None
            
        voice = voice or self.default_voice
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                payload = {
                    "model": self.model,
                    "input": text.strip(),
                    "voice": voice,
                    "response_format": "wav"
                }
                
                headers = {
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json"
                }
                
                logger.info(f"TTS: Generating speech for '{text[:50]}...'")
                response = await client.post(
                    f"{self.base_url}/audio/speech",
                    json=payload,
                    headers=headers
                )
                
                if response.status_code == 200:
                    logger.info("TTS: Speech generation successful")
                    return response.content
                else:
                    logger.error(f"TTS Error: {response.status_code} - {response.text}")
                    return None
                    
        except asyncio.TimeoutError:
            logger.error("TTS: Request timeout")
            return None
        except Exception as e:
            logger.error(f"TTS Exception: {str(e)}")
            return None

    async def get_available_voices(self) -> Optional[list]:
        """Get list of available voices for PlayAI TTS via Groq"""
        # Based on documentation, return known available voices
        english_voices = [
            "Arista-PlayAI", "Atlas-PlayAI", "Basil-PlayAI", "Briggs-PlayAI", 
            "Calum-PlayAI", "Celeste-PlayAI", "Cheyenne-PlayAI", "Chip-PlayAI", 
            "Cillian-PlayAI", "Deedee-PlayAI", "Fritz-PlayAI", "Gail-PlayAI", 
            "Indigo-PlayAI", "Mamaw-PlayAI", "Mason-PlayAI", "Mikail-PlayAI", 
            "Mitch-PlayAI", "Quinn-PlayAI", "Thunder-PlayAI"
        ]
        
        return {
            "english": english_voices,
            "model": "playai-tts"
        }

# Fallback TTS using system or simpler API
class FallbackTTS:
    """Fallback TTS implementation"""
    
    async def generate_speech(self, text: str) -> Optional[bytes]:
        """Generate simple TTS audio as fallback"""
        logger.info("Using fallback TTS (returns None - implement system TTS if needed)")
        # Could implement system TTS, espeak, or other fallback here
        return None

# Global instances
playai_tts = GroqPlayAITTS()
fallback_tts = FallbackTTS()
