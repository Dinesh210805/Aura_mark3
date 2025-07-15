import httpx
import os
from typing import Optional
import logging
import asyncio

logger = logging.getLogger(__name__)

class GroqSTT:
    """Groq Speech-to-Text API wrapper using Whisper"""
    
    def __init__(self):
        self.api_key = os.getenv("GROQ_API_KEY")
        self.base_url = "https://api.groq.com/openai/v1"
        self.model = "whisper-large-v3-turbo"
        
        if not self.api_key:
            logger.error("GROQ_API_KEY not found in environment variables")
        
    async def transcribe(self, audio_data: bytes) -> Optional[str]:
        """Convert audio to text using Groq Whisper"""
        if not self.api_key:
            logger.error("STT: No API key available")
            return None
            
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                # Prepare multipart form data
                files = {
                    "file": ("audio.wav", audio_data, "audio/wav"),
                    "model": (None, self.model),
                    "response_format": (None, "text"),
                    "language": (None, "en")  # Optional: specify language
                }
                
                headers = {
                    "Authorization": f"Bearer {self.api_key}"
                }
                
                logger.info("STT: Sending audio for transcription")
                response = await client.post(
                    f"{self.base_url}/audio/transcriptions",
                    files=files,
                    headers=headers
                )
                
                if response.status_code == 200:
                    transcript = response.text.strip()
                    logger.info(f"STT: Transcription successful - '{transcript[:50]}...'")
                    return transcript
                else:
                    logger.error(f"STT Error: {response.status_code} - {response.text}")
                    return None
                    
        except asyncio.TimeoutError:
            logger.error("STT: Request timeout")
            return None
        except Exception as e:
            logger.error(f"STT Exception: {str(e)}")
            return None

# Global instance
groq_stt = GroqSTT()
