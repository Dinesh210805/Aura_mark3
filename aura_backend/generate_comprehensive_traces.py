"""
Enhanced trace generation for LangSmith visualization
"""

import requests
import time
import json
import uuid

BASE_URL = "http://localhost:8000"

def generate_comprehensive_traces():
    """Generate a variety of traces to showcase different agent capabilities"""
    
    test_scenarios = [
        {
            "name": "Greeting & Capabilities",
            "requests": [
                "Hello, what can you help me with?",
                "What are your capabilities?",
                "How do you work?"
            ]
        },
        {
            "name": "App Control",
            "requests": [
                "Open WhatsApp",
                "Launch the camera app",
                "Open Settings",
                "Start the music player"
            ]
        },
        {
            "name": "Screen Interaction",
            "requests": [
                "What's on my screen?",
                "Take a screenshot",
                "Scroll down",
                "Tap the back button"
            ]
        },
        {
            "name": "Information Queries",
            "requests": [
                "What time is it?",
                "Check the weather",
                "Read my notifications",
                "What's my battery level?"
            ]
        }
    ]
    
    print("🚀 Generating Comprehensive Traces for LangSmith...")
    print("=" * 60)
    
    total_requests = 0
    successful_requests = 0
    
    for scenario in test_scenarios:
        print(f"\n📱 {scenario['name']} Scenarios:")
        
        for request_text in scenario['requests']:
            try:
                session_id = str(uuid.uuid4())
                
                print(f"   📤 {request_text}")
                
                response = requests.post(
                    f"{BASE_URL}/chat",
                    headers={"Content-Type": "application/json"},
                    json={
                        "text": request_text,
                        "session_id": session_id
                    },
                    timeout=30
                )
                
                total_requests += 1
                
                if response.status_code == 200:
                    result = response.json()
                    print(f"      ✅ Intent: {result.get('intent', 'Unknown')}")
                    print(f"      💬 Response: {result.get('response', 'No response')[:60]}...")
                    successful_requests += 1
                else:
                    print(f"      ❌ Error {response.status_code}: {response.text[:100]}")
                
                # Wait between requests to ensure traces are processed
                time.sleep(1.5)
                
            except Exception as e:
                print(f"      ❌ Request failed: {e}")
                total_requests += 1
    
    print("\n" + "=" * 60)
    print(f"📊 Trace Generation Summary:")
    print(f"   Total Requests: {total_requests}")
    print(f"   Successful: {successful_requests}")
    print(f"   Success Rate: {(successful_requests/total_requests)*100:.1f}%")
    
    # Wait for traces to propagate
    print(f"\n⏳ Waiting 15 seconds for traces to propagate to LangSmith...")
    time.sleep(15)
    
    # Check for traces
    try:
        response = requests.get(f"{BASE_URL}/langsmith/traces?hours=1&limit=20")
        if response.status_code == 200:
            traces = response.json()
            trace_count = traces.get('count', 0)
            print(f"📈 Found {trace_count} traces in LangSmith")
            
            if trace_count > 0:
                print("✅ Traces are being captured! Check your dashboard:")
                print("🌐 https://smith.langchain.com/projects/p/aura-agent-visualization")
                
                # Show some trace details
                for i, trace in enumerate(traces.get('traces', [])[:5]):
                    print(f"   {i+1}. {trace.get('name', 'Unknown')} - Status: {trace.get('status', 'Unknown')}")
            else:
                print("⚠️  No traces found yet. Possible reasons:")
                print("   1. Project doesn't exist in LangSmith dashboard")
                print("   2. Traces are still processing (can take a few minutes)")
                print("   3. API key permissions issue")
        else:
            print(f"❌ Could not check traces: {response.status_code}")
    except Exception as e:
        print(f"❌ Trace check failed: {e}")
    
    print(f"\n🎯 Next Steps:")
    print(f"1. Visit: https://smith.langchain.com/projects/p/aura-agent-visualization")
    print(f"2. Create the project if it doesn't exist")
    print(f"3. Wait a few minutes for traces to appear")
    print(f"4. Explore the execution flows and performance metrics!")

if __name__ == "__main__":
    generate_comprehensive_traces()
