"""
Generate sample traces for LangSmith visualization testing
"""

import requests
import time
import json
import uuid

BASE_URL = "http://localhost:8000"

def test_chat_endpoint():
    """Generate multiple chat traces for LangSmith"""
    
    test_messages = [
        {"text": "Hello, what can you help me with?", "session_id": str(uuid.uuid4())},
        {"text": "Open WhatsApp please", "session_id": str(uuid.uuid4())},
        {"text": "Take a screenshot", "session_id": str(uuid.uuid4())},
        {"text": "What's the weather like?", "session_id": str(uuid.uuid4())},
        {"text": "Help me navigate to settings", "session_id": str(uuid.uuid4())}
    ]
    
    print("🚀 Generating LangSmith traces...")
    
    for i, message in enumerate(test_messages, 1):
        try:
            print(f"📤 Test {i}/5: {message['text']}")
            
            response = requests.post(
                f"{BASE_URL}/chat",
                headers={"Content-Type": "application/json"},
                json=message,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ Response: {result.get('response', 'No response')}")
            else:
                print(f"❌ Error {response.status_code}: {response.text}")
                
            # Wait between requests to ensure traces are processed
            time.sleep(2)
            
        except Exception as e:
            print(f"❌ Request failed: {e}")
    
    print("\n📊 Checking trace generation...")
    
    # Check for traces
    try:
        response = requests.get(f"{BASE_URL}/langsmith/traces?limit=10")
        if response.status_code == 200:
            traces = response.json()
            print(f"📈 Found {traces.get('count', 0)} traces")
            
            if traces.get('traces'):
                print("✅ Sample traces:")
                for trace in traces['traces'][:3]:
                    print(f"  - {trace.get('name', 'Unknown')} | Status: {trace.get('status', 'Unknown')} | Duration: {trace.get('total_time', 'Unknown')}ms")
            else:
                print("⚠️  No traces found yet - they may still be processing")
        else:
            print(f"❌ Failed to fetch traces: {response.status_code}")
            
    except Exception as e:
        print(f"❌ Failed to check traces: {e}")
    
    # Get dashboard URL
    try:
        response = requests.get(f"{BASE_URL}/langsmith/dashboard")
        if response.status_code == 200:
            dashboard = response.json()
            print(f"\n🎯 View your traces at: {dashboard.get('url', 'Dashboard not available')}")
        else:
            print("❌ Could not get dashboard URL")
    except Exception as e:
        print(f"❌ Dashboard URL error: {e}")

if __name__ == "__main__":
    test_chat_endpoint()
