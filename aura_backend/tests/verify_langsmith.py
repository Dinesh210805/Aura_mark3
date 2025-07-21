"""
LangSmith project verification and trace test
"""

import os
import time
from langsmith import Client, traceable
from dotenv import load_dotenv

# Load environment
load_dotenv()

@traceable(name="aura_test_trace")
def test_aura_function(user_input: str) -> dict:
    """Test function that mimics AURA agent behavior"""
    print(f"Processing: {user_input}")
    
    # Simulate processing steps
    steps = [
        {"step": "stt", "result": "transcribed", "duration": 0.1},
        {"step": "intent", "result": "greeting", "duration": 0.05},
        {"step": "response", "result": "Hello! How can I help?", "duration": 0.02}
    ]
    
    return {
        "input": user_input,
        "steps": steps,
        "final_response": "Hello! How can I help you today?",
        "success": True
    }

def main():
    print("🧪 Testing LangSmith Integration for AURA")
    print("=" * 50)
    
    # Check environment
    api_key = os.getenv("LANGCHAIN_API_KEY")
    project = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
    
    if not api_key:
        print("❌ No LangSmith API key found!")
        return
    
    print(f"✅ API Key: {api_key[:20]}...")
    print(f"📊 Project: {project}")
    
    try:
        # Initialize client
        client = Client()
        
        # Check if project exists
        try:
            projects = list(client.list_projects())
            project_names = [p.name for p in projects]
            print(f"📋 Available projects: {project_names}")
            
            if project in project_names:
                print(f"✅ Project '{project}' exists")
            else:
                print(f"⚠️ Project '{project}' not found - will be created automatically")
                
        except Exception as e:
            print(f"⚠️ Could not list projects: {e}")
        
        # Generate test traces
        print("\n🔄 Generating test traces...")
        
        test_inputs = [
            "Hello AURA",
            "What can you do?",
            "Open WhatsApp",
            "Take a screenshot",
            "Help me with settings"
        ]
        
        for i, test_input in enumerate(test_inputs, 1):
            print(f"   Trace {i}: {test_input}")
            result = test_aura_function(test_input)
            print(f"   ✅ Success: {result['final_response']}")
            time.sleep(0.5)  # Brief pause between traces
        
        print("\n⏳ Waiting for traces to be processed...")
        time.sleep(3)
        
        # Try to retrieve recent runs
        try:
            runs = list(client.list_runs(project_name=project, limit=10))
            print(f"📈 Found {len(runs)} recent runs in project")
            
            for run in runs[:3]:  # Show first 3
                print(f"   • {run.name} | Status: {run.status} | Duration: {run.total_time}ms")
                
        except Exception as e:
            print(f"⚠️ Could not retrieve runs: {e}")
        
        print(f"\n🎯 Dashboard: https://smith.langchain.com/projects/p/{project}")
        print("✅ Test completed!")
        
    except Exception as e:
        print(f"❌ Error during test: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
