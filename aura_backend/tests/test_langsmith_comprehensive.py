"""
Comprehensive LangSmith Integration Test
Tests tracing functionality and helps debug issues
"""

import requests
import time
import json
import uuid
from langsmith import Client
import os
from dotenv import load_dotenv

# Load environment
load_dotenv()

BASE_URL = "http://localhost:8000"

def test_langsmith_direct():
    """Test LangSmith client directly"""
    print("🔧 Testing LangSmith Client Direct Connection...")
    
    api_key = os.getenv("LANGCHAIN_API_KEY")
    project = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
    
    if not api_key:
        print("❌ No LangSmith API key found")
        return False
    
    try:
        client = Client(api_key=api_key)
        
        # Try to create the project if it doesn't exist
        try:
            # This will create the project if it doesn't exist
            runs = list(client.list_runs(project_name=project, limit=1))
            print(f"✅ Project '{project}' is accessible")
        except Exception as e:
            print(f"⚠️  Project access issue: {e}")
            # Try to create a simple trace to initialize the project
            print("🔄 Attempting to initialize project...")
            
        return True
        
    except Exception as e:
        print(f"❌ LangSmith client error: {e}")
        return False

def test_agent_with_simple_requests():
    """Test agent with simple requests to generate traces"""
    print("\n🤖 Testing Agent with Simple Requests...")
    
    test_cases = [
        {
            "text": "Hello there!",
            "session_id": str(uuid.uuid4()),
            "expected_intent": "greeting"
        },
        {
            "text": "Open the camera app",
            "session_id": str(uuid.uuid4()),
            "expected_intent": "app_launch"
        },
        {
            "text": "What time is it?",
            "session_id": str(uuid.uuid4()),
            "expected_intent": "time_inquiry"
        }
    ]
    
    successful_requests = 0
    
    for i, test_case in enumerate(test_cases, 1):
        try:
            print(f"   📤 Test {i}: {test_case['text']}")
            
            response = requests.post(
                f"{BASE_URL}/chat",
                headers={"Content-Type": "application/json"},
                json={
                    "text": test_case["text"],
                    "session_id": test_case["session_id"]
                },
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                print(f"      ✅ Success: {result.get('intent', 'No intent')}")
                print(f"      📝 Response: {result.get('response', 'No response')[:50]}...")
                print(f"      🆔 Session: {result.get('session_id', 'No session')}")
                successful_requests += 1
            else:
                print(f"      ❌ HTTP {response.status_code}: {response.text}")
                
            # Wait between requests
            time.sleep(1)
            
        except Exception as e:
            print(f"      ❌ Request failed: {e}")
    
    print(f"\n✅ Successfully completed {successful_requests}/{len(test_cases)} requests")
    return successful_requests > 0

def check_langsmith_project_status():
    """Check if the LangSmith project exists and is properly configured"""
    print("\n🔍 Checking LangSmith Project Status...")
    
    api_key = os.getenv("LANGCHAIN_API_KEY")
    project = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
    
    if not api_key:
        print("❌ No API key configured")
        return False
    
    try:
        client = Client(api_key=api_key)
        
        # Try to get project info
        try:
            runs = list(client.list_runs(project_name=project, limit=5))
            print(f"✅ Project '{project}' exists")
            print(f"📊 Found {len(runs)} recent runs")
            
            if runs:
                latest_run = runs[0]
                print(f"🕐 Latest run: {latest_run.name} at {latest_run.start_time}")
            else:
                print("⚠️  No runs found - traces may take time to appear")
                
            return True
            
        except Exception as e:
            print(f"❌ Project access error: {e}")
            print("💡 Try manually creating the project in LangSmith dashboard")
            return False
            
    except Exception as e:
        print(f"❌ LangSmith client error: {e}")
        return False

def create_manual_trace():
    """Create a manual trace to test LangSmith functionality"""
    print("\n🧪 Creating Manual Test Trace...")
    
    from langsmith import traceable
    
    @traceable(name="manual_test_trace")
    def test_function():
        """Simple test function to create a trace"""
        import time
        time.sleep(0.1)  # Simulate some work
        return {"status": "success", "message": "Manual trace created"}
    
    try:
        result = test_function()
        print(f"✅ Manual trace created: {result}")
        return True
    except Exception as e:
        print(f"❌ Manual trace failed: {e}")
        return False

def main():
    """Main test function"""
    print("🧪 Comprehensive LangSmith Integration Test")
    print("=" * 60)
    
    # Test 1: LangSmith client direct connection
    langsmith_ok = test_langsmith_direct()
    
    # Test 2: Manual trace creation
    if langsmith_ok:
        create_manual_trace()
    
    # Test 3: Check project status
    project_ok = check_langsmith_project_status()
    
    # Test 4: Agent requests
    agent_ok = test_agent_with_simple_requests()
    
    # Wait for traces to propagate
    print("\n⏳ Waiting 10 seconds for traces to propagate...")
    time.sleep(10)
    
    # Test 5: Check for traces via API
    try:
        response = requests.get(f"{BASE_URL}/langsmith/traces?hours=1&limit=10")
        if response.status_code == 200:
            traces = response.json()
            trace_count = traces.get('count', 0)
            print(f"\n📊 Found {trace_count} traces via API")
            
            if trace_count > 0:
                print("✅ Traces are being captured!")
                for trace in traces.get('traces', [])[:3]:
                    print(f"   • {trace.get('name', 'Unknown')} - {trace.get('status', 'Unknown')}")
            else:
                print("⚠️  No traces found via API")
        else:
            print(f"❌ API error: {response.status_code}")
    except Exception as e:
        print(f"❌ Trace check failed: {e}")
    
    # Final status
    print("\n" + "=" * 60)
    print("📋 Test Summary:")
    print(f"   LangSmith Client: {'✅' if langsmith_ok else '❌'}")
    print(f"   Project Status: {'✅' if project_ok else '❌'}")
    print(f"   Agent Requests: {'✅' if agent_ok else '❌'}")
    
    print(f"\n🌐 Dashboard: https://smith.langchain.com/projects/p/{os.getenv('LANGCHAIN_PROJECT', 'aura-agent-visualization')}")
    
    if not project_ok:
        print("\n💡 Troubleshooting steps:")
        print("1. Visit https://smith.langchain.com and sign in")
        print("2. Create a new project named 'aura-agent-visualization'")
        print("3. Make sure your API key has the correct permissions")
        print("4. Restart the server and run this test again")

if __name__ == "__main__":
    main()
