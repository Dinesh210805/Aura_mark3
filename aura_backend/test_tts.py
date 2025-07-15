#!/usr/bin/env python3
"""
Test script for AURA Backend TTS functionality
"""

import asyncio
import sys
import os
from dotenv import load_dotenv

# Load environment variables first
load_dotenv()

sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from groq.tts import playai_tts

async def test_tts():
    """Test Groq PlayAI TTS functionality"""
    print("🧪 Testing Groq PlayAI TTS...")
    
    test_text = "Hello! This is AURA, your intelligent voice assistant. I'm ready to help you with your tasks."
    
    try:
        audio = await playai_tts.generate_speech(test_text)
        
        if audio:
            print(f"✅ TTS Success: Generated {len(audio)} bytes of audio")
            
            # Save test audio file
            with open("test_tts_output.wav", "wb") as f:
                f.write(audio)
            print("📁 Audio saved as 'test_tts_output.wav'")
            
            return True
        else:
            print("❌ TTS Failed: No audio generated")
            return False
            
    except Exception as e:
        print(f"❌ TTS Error: {str(e)}")
        return False

async def test_voices():
    """Test available voices"""
    print("\n🎭 Testing available voices...")
    try:
        voices = await playai_tts.get_available_voices()
        if voices:
            english_voices = voices.get("english", [])
            print(f"📢 Available English voices ({len(english_voices)}):")
            for i, voice in enumerate(english_voices[:5], 1):  # Show first 5
                print(f"  {i}. {voice}")
            if len(english_voices) > 5:
                print(f"  ... and {len(english_voices) - 5} more")
        else:
            print("❌ Could not retrieve voice list")
    except Exception as e:
        print(f"❌ Voice list error: {str(e)}")

def main():
    """Main test function"""
    # Environment already loaded at module level
    
    # Check API key
    if not os.getenv("GROQ_API_KEY"):
        print("❌ GROQ_API_KEY not found in environment")
        print("Please set it in your .env file")
        return
    
    print("🚀 AURA Backend TTS Test")
    print("=" * 40)
    
    # Run tests
    result = asyncio.run(test_tts())
    asyncio.run(test_voices())
    
    print("\n" + "=" * 40)
    if result:
        print("🎉 All TTS tests passed!")
    else:
        print("❌ TTS tests failed")

if __name__ == "__main__":
    main()
