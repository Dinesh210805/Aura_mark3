import requests
import json

# Test the AURA chat endpoint
url = "http://localhost:8000/chat"
data = {
    "text": "test langsmith tracing",
    "session_id": "langsmith-test-session"
}

print("🧪 Testing AURA Chat Endpoint for LangSmith Tracing")
print("=" * 55)

try:
    response = requests.post(url, json=data)
    print(f"📊 Status Code: {response.status_code}")
    
    if response.status_code == 200:
        result = response.json()
        print(f"✅ Response received:")
        print(f"   Success: {result.get('success')}")
        print(f"   Response: {result.get('response')}")
        print(f"   Intent: {result.get('intent')}")
        print(f"   Session ID: {result.get('session_id')}")
    else:
        print(f"❌ Error: {response.text}")
        
except Exception as e:
    print(f"❌ Request failed: {e}")

print(f"\n🔗 If successful, check LangSmith dashboard for traces:")
print(f"   https://smith.langchain.com/")
print(f"   Project: aura-agent-visualization")
