"""
Audio utilities for AURA test GUI
Handles audio file creation and validation
"""

import numpy as np
import soundfile as sf
import tempfile
import io
from pathlib import Path

def create_sample_audio(text: str, duration: float = 2.0, sample_rate: int = 16000) -> str:
    """
    Create a sample audio file for testing purposes.
    
    Args:
        text: The text content (for reference, actual audio is a tone)
        duration: Duration in seconds
        sample_rate: Audio sample rate
    
    Returns:
        Path to the created temporary audio file
    """
    # Generate a simple sine wave as placeholder audio
    t = np.linspace(0, duration, int(sample_rate * duration), False)
    
    # Create different tones based on text length for variety
    frequency = 440 + (len(text) % 5) * 110  # Vary frequency based on text
    audio_data = 0.3 * np.sin(2 * np.pi * frequency * t)
    
    # Add some fade in/out for better audio quality
    fade_samples = int(0.1 * sample_rate)  # 0.1 second fade
    audio_data[:fade_samples] *= np.linspace(0, 1, fade_samples)
    audio_data[-fade_samples:] *= np.linspace(1, 0, fade_samples)
    
    # Save to temporary file
    temp_file = tempfile.NamedTemporaryFile(suffix=".wav", delete=False)
    sf.write(temp_file.name, audio_data, sample_rate)
    
    return temp_file.name

def validate_audio_file(file_data: bytes) -> bool:
    """
    Validate uploaded audio file format.
    
    Args:
        file_data: Raw audio file bytes
    
    Returns:
        True if valid audio format
    """
    try:
        # Try to read the audio data
        audio_buffer = io.BytesIO(file_data)
        data, samplerate = sf.read(audio_buffer)
        return len(data) > 0 and samplerate > 0
    except Exception:
        return False

def get_audio_info(file_data: bytes) -> dict:
    """
    Get information about an audio file.
    
    Args:
        file_data: Raw audio file bytes
    
    Returns:
        Dictionary with audio information
    """
    try:
        audio_buffer = io.BytesIO(file_data)
        data, samplerate = sf.read(audio_buffer)
        
        return {
            "duration": len(data) / samplerate,
            "sample_rate": samplerate,
            "channels": data.shape[1] if len(data.shape) > 1 else 1,
            "samples": len(data),
            "size_bytes": len(file_data)
        }
    except Exception as e:
        return {"error": str(e)}

# Common test phrases for sample audio generation
TEST_PHRASES = [
    "Hello AURA, how are you today?",
    "Open WhatsApp please",
    "What time is it?",
    "Click the send button",
    "Type hello world in the text field",
    "Go to settings",
    "Turn on WiFi",
    "Send an email to John",
    "Take a screenshot",
    "What apps are open?"
]
