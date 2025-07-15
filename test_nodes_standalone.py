"""
Standalone node testing for AURA Backend
"""
import asyncio
import sys
import os

# Add the parent directory to the path so we can import modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from dotenv import load_dotenv
load_dotenv(dotenv_path="../.env")

import logging
import json

# Set up detailed logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

async def test_stt_node():
    """Test STT node standalone"""
    print("=" * 50)
    print("Testing STT Node (Text Input Mode)")
    print("=" * 50)
    
    from nodes.stt_node import stt_node
    
    # Test with pre-existing transcript (chat mode)
    state = {
        "transcript": "Hello, tell me the current time",
        "session_id": "test_stt"
    }
    
    print(f"Input state: {json.dumps(state, indent=2)}")
    
    result = await stt_node.run(state)
    
    print(f"Output state: {json.dumps(result, indent=2)}")
    print(f"Success: {not result.get('error')}")
    
    return result

async def test_intent_node():
    """Test Intent node standalone"""
    print("\n" + "=" * 50)
    print("Testing Intent Node")
    print("=" * 50)
    
    from nodes.intent_node import intent_node
    
    # Test with transcript
    state = {
        "transcript": "Hello, tell me the current time",
        "session_id": "test_intent",
        "node_execution_times": {}
    }
    
    print(f"Input state: {json.dumps(state, indent=2)}")
    
    result = await intent_node.run(state)
    
    print(f"Output state: {json.dumps(result, indent=2)}")
    print(f"Success: {not result.get('error')}")
    print(f"Intent: {result.get('intent')}")
    
    return result

async def test_ui_check_node():
    """Test UI Check node standalone"""
    print("\n" + "=" * 50)
    print("Testing UI Check Node")
    print("=" * 50)
    
    from nodes.ui_check_node import ui_check_node
    
    # Test with intent data
    state = {
        "transcript": "Hello, tell me the current time",
        "intent": "User wants to know the current time",
        "intent_data": {"intent": "time_request", "requires_screen_analysis": False},
        "session_id": "test_ui_check",
        "node_execution_times": {}
    }
    
    print(f"Input state: {json.dumps(state, indent=2)}")
    
    result = await ui_check_node.run(state)
    
    print(f"Output state: {json.dumps(result, indent=2)}")
    print(f"Success: {not result.get('error')}")
    print(f"Use VLM: {result.get('use_vlm')}")
    
    return result

async def test_tts_node():
    """Test TTS node standalone"""
    print("\n" + "=" * 50)
    print("Testing TTS Node")
    print("=" * 50)
    
    from nodes.tts_node import tts_node
    
    # Test with full state
    state = {
        "transcript": "Hello, tell me the current time",
        "intent": "User wants to know the current time",
        "action_plan": [{"action": "respond", "text": "The current time is approximately now"}],
        "session_id": "test_tts",
        "node_execution_times": {}
    }
    
    print(f"Input state: {json.dumps(state, indent=2)}")
    
    result = await tts_node.run(state)
    
    print(f"Output state: {json.dumps({k: v for k, v in result.items() if k != 'tts_audio'}, indent=2)}")
    print(f"Success: {not result.get('error')}")
    print(f"Response text: {result.get('response_text')}")
    print(f"Has TTS audio: {bool(result.get('tts_audio'))}")
    
    return result

async def test_groq_llm_directly():
    """Test Groq LLM directly"""
    print("\n" + "=" * 50)
    print("Testing Groq LLM API Directly")
    print("=" * 50)
    
    from groq.llm import groq_llm
    
    print(f"API Key configured: {bool(groq_llm.api_key)}")
    
    if groq_llm.api_key:
        result = await groq_llm.analyze_intent("Hello, tell me the current time")
        print(f"LLM Result: {json.dumps(result, indent=2)}")
        return result
    else:
        print("❌ No API key configured")
        return {"error": "No API key"}

async def main():
    """Run all standalone tests"""
    print("🧪 AURA Backend Node Testing")
    print("Testing each node in isolation...")
    
    try:
        # Test each node
        await test_groq_llm_directly()
        await test_stt_node()
        intent_result = await test_intent_node()
        await test_ui_check_node()
        await test_tts_node()
        
        print("\n" + "=" * 50)
        print("✅ All standalone tests completed!")
        
        # Check if intent was properly extracted
        if intent_result and intent_result.get('intent') and not intent_result.get('error'):
            print("✅ Intent node is working properly")
        else:
            print("❌ Intent node has issues")
            
    except Exception as e:
        print(f"❌ Test error: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())
