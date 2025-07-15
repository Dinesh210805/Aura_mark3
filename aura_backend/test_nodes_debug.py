"""
Standalone node testing for AURA Backend
Run from aura_backend directory
"""
import asyncio
import os
from dotenv import load_dotenv

# Try multiple .env locations
env_paths = [
    ".env",  # Current directory
    "../.env",  # Parent directory
    "d:/PROJECTS/Aura_mark3/.env",  # Absolute path
    "d:/PROJECTS/Aura_mark3/aura_backend/.env"  # Backend .env
]

for env_path in env_paths:
    if os.path.exists(env_path):
        print(f"📁 Loading environment from: {env_path}")
        load_dotenv(dotenv_path=env_path)
        break
else:
    print("⚠️ No .env file found in any of the expected locations")

import logging
import json

# Set up detailed logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

async def test_groq_llm_directly():
    """Test Groq LLM directly"""
    print("=" * 50)
    print("Testing Groq LLM API Directly")
    print("=" * 50)
    
    from groq.llm import groq_llm
    
    print(f"API Key configured: {bool(groq_llm.api_key)}")
    print(f"API Key (first 10 chars): {groq_llm.api_key[:10] if groq_llm.api_key else 'None'}...")
    
    if groq_llm.api_key:
        try:
            result = await groq_llm.analyze_intent("Hello, tell me the current time")
            print(f"LLM Result: {json.dumps(result, indent=2)}")
            return result
        except Exception as e:
            print(f"❌ LLM Error: {str(e)}")
            import traceback
            traceback.print_exc()
            return {"error": str(e)}
    else:
        print("❌ No API key configured")
        return {"error": "No API key"}

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
    
    try:
        result = await intent_node.run(state)
        print(f"Output state: {json.dumps(result, indent=2)}")
        print(f"Success: {not result.get('error')}")
        print(f"Intent: {result.get('intent')}")
        return result
    except Exception as e:
        print(f"❌ Intent Node Error: {str(e)}")
        import traceback
        traceback.print_exc()
        return {"error": str(e)}

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
    
    try:
        result = await tts_node.run(state)
        print(f"Output state: {json.dumps({k: v for k, v in result.items() if k != 'tts_audio'}, indent=2)}")
        print(f"Success: {not result.get('error')}")
        print(f"Response text: {result.get('response_text')}")
        print(f"Has TTS audio: {bool(result.get('tts_audio'))}")
        return result
    except Exception as e:
        print(f"❌ TTS Node Error: {str(e)}")
        import traceback
        traceback.print_exc()
        return {"error": str(e)}

async def test_environment_vars():
    """Test environment variables"""
    print("\n" + "=" * 50)
    print("Testing Environment Variables")
    print("=" * 50)
    
    groq_key = os.getenv("GROQ_API_KEY")
    print(f"GROQ_API_KEY: {'✅ Set' if groq_key else '❌ Missing'}")
    if groq_key:
        print(f"Key starts with: {groq_key[:10]}...")
    
    other_vars = [
        "BACKEND_SECRET_KEY",
        "ALLOWED_ORIGINS", 
        "HOST",
        "PORT",
        "LOG_LEVEL"
    ]
    
    for var in other_vars:
        value = os.getenv(var)
        print(f"{var}: {'✅ Set' if value else '❌ Missing'} - {value}")

async def main():
    """Run all standalone tests"""
    print("🧪 AURA Backend Node Testing")
    print("Testing individual components...")
    
    try:
        # Test environment first
        await test_environment_vars()
        
        # Test LLM directly
        llm_result = await test_groq_llm_directly()
        
        # Test intent node (which uses LLM)
        intent_result = await test_intent_node()
        
        # Test TTS node
        tts_result = await test_tts_node()
        
        print("\n" + "=" * 50)
        print("📊 Test Summary")
        print("=" * 50)
        
        # Analyze results
        llm_ok = llm_result and not llm_result.get('error')
        intent_ok = intent_result and not intent_result.get('error') and intent_result.get('intent')
        tts_ok = tts_result and not tts_result.get('error') and tts_result.get('response_text')
        
        print(f"LLM API: {'✅ Working' if llm_ok else '❌ Failed'}")
        print(f"Intent Node: {'✅ Working' if intent_ok else '❌ Failed'}")
        print(f"TTS Node: {'✅ Working' if tts_ok else '❌ Failed'}")
        
        if llm_ok and intent_ok and tts_ok:
            print("\n🎉 All core components are working!")
            print("The issue might be in the LangGraph orchestration.")
        else:
            print("\n❌ Some components have issues that need fixing.")
            
    except Exception as e:
        print(f"❌ Test error: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())
