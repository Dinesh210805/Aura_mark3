#!/usr/bin/env python3
"""
Simple debug script to test the backend directly
"""
import asyncio
import requests
import json
import sys
import os

def test_simple_api_call():
    """Test a simple API call to Groq"""
    print("🧪 Testing direct Groq API call...")
    
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key:
        print("❌ No API key found")
        return False
    
    url = "https://api.groq.com/openai/v1/chat/completions"
    
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "model": "llama-3.3-70b-versatile",
        "messages": [
            {"role": "system", "content": "You are a helpful assistant. Respond in JSON format with just {\"status\": \"working\"}"},
            {"role": "user", "content": "Say hello"}
        ],
        "temperature": 0.1,
        "max_tokens": 100,
        "response_format": {"type": "json_object"}
    }
    
    try:
        print("📡 Making API call...")
        response = requests.post(url, json=payload, headers=headers, timeout=10)
        
        print(f"Status: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ API Response: {result}")
            return True
        else:
            print(f"❌ API Error: {response.status_code} - {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ API Exception: {str(e)}")
        return False

def test_backend_health():
    """Test backend health"""
    print("\n🏥 Testing backend health...")
    
    try:
        response = requests.get("http://localhost:8000/health", timeout=5)
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ Health check: {result.get('status', 'unknown')}")
            return True
        else:
            print(f"❌ Health check failed: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Health check exception: {str(e)}")
        return False

def test_chat_endpoint():
    """Test chat endpoint"""
    print("\n💬 Testing chat endpoint...")
    
    url = "http://localhost:8000/chat"
    data = {
        "text": "Hello",
        "session_id": "debug_test"
    }
    
    try:
        response = requests.post(url, json=data, timeout=30)
        
        print(f"Status: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"Response: {json.dumps(result, indent=2)}")
            
            if result.get("success") and result.get("response") != "No response generated":
                print("✅ Chat working properly")
                return True
            else:
                print("⚠️ Chat returns empty response")
                return False
        else:
            print(f"❌ Chat error: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Chat exception: {str(e)}")
        return False

def main():
    """Main debug function"""
    from dotenv import load_dotenv
    load_dotenv(dotenv_path=".env")
    
    print("🔍 AURA Backend Debug")
    print("=" * 30)
    
    # Test direct API call first
    api_ok = test_simple_api_call()
    
    # Test backend health
    health_ok = test_backend_health()
    
    # Test chat endpoint
    if health_ok:
        chat_ok = test_chat_endpoint()
    else:
        chat_ok = False
    
    print("\n" + "=" * 30)
    print("Results:")
    print(f"Direct API: {'✅' if api_ok else '❌'}")
    print(f"Health: {'✅' if health_ok else '❌'}")
    print(f"Chat: {'✅' if chat_ok else '❌'}")
    
    if api_ok and health_ok and not chat_ok:
        print("\n🔍 API works but chat doesn't - likely a node execution issue")
    elif not api_ok:
        print("\n🔍 API issue - check API key or network")
    elif not health_ok:
        print("\n🔍 Backend not running properly")

if __name__ == "__main__":
    main()
