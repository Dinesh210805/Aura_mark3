#!/usr/bin/env python3
"""
Simple chat test script for AURA backend
"""
import requests
import json

def test_chat():
    """Test the chat endpoint"""
    url = "http://localhost:8000/chat"
    
    # Test data
    data = {
        "text": "Hello, what can you help me with?",
        "session_id": "test_session_001"
    }
    
    headers = {
        "Content-Type": "application/json"
    }
    
    print("🧪 Testing Chat Endpoint...")
    print(f"URL: {url}")
    print(f"Data: {json.dumps(data, indent=2)}")
    print("-" * 50)
    
    try:
        print("📡 Sending request...")
        response = requests.post(url, json=data, headers=headers, timeout=10)
        
        print(f"Status Code: {response.status_code}")
        print(f"Headers: {dict(response.headers)}")
        print("-" * 50)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Response received:")
            print(json.dumps(result, indent=2))
            
            if result.get("success"):
                print(f"\n✅ Chat Success!")
                print(f"Response: {result.get('response', 'No response text')}")
                print(f"Intent: {result.get('intent', 'No intent detected')}")
                print(f"Session ID: {result.get('session_id', 'No session ID')}")
            else:
                print(f"\n❌ Chat Failed: {result}")
        else:
            print(f"❌ HTTP Error: {response.status_code}")
            print(f"Response: {response.text}")
            
    except requests.exceptions.ConnectionError:
        print("❌ Connection Error: Is the backend running on localhost:8000?")
    except requests.exceptions.Timeout:
        print("❌ Timeout Error: Backend took too long to respond")
    except Exception as e:
        print(f"❌ Unexpected Error: {str(e)}")

if __name__ == "__main__":
    test_chat()
