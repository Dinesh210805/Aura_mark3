#!/usr/bin/env python3
"""
Test LangGraph tracing with LangSmith
This test will verify that LangGraph execution is properly traced in LangSmith
"""

import asyncio
import json
import os
import sys
import time
from typing import Dict, Any

# Add the current directory to the path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from aura_graph import AuraGraph
from langsmith import Client

# Set up LangSmith environment
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_ENDPOINT"] = "https://api.smith.langchain.com"
os.environ["LANGCHAIN_API_KEY"] = "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"
os.environ["LANGCHAIN_PROJECT"] = "aura-agent-visualization"

async def test_langgraph_execution():
    """Test LangGraph execution with tracing"""
    print("🧪 Testing LangGraph Execution with LangSmith Tracing")
    print("=" * 60)
    
    try:
        # Initialize LangSmith client
        client = Client()
        project_name = "aura-agent-visualization"
        
        # Check project exists
        try:
            project = client.read_project(project_name=project_name)
            print(f"📊 Project '{project_name}' found: {project.name}")
        except Exception as e:
            print(f"❌ Project not found: {e}")
            return
            
        # Get initial run count
        initial_runs = list(client.list_runs(project_name=project_name, limit=5))
        initial_count = len(initial_runs)
        print(f"📈 Initial runs count: {initial_count}")
        
        # Initialize AURA Graph
        print("\n🚀 Initializing AURA Graph...")
        aura = AuraGraph()
        
        # Prepare test state
        test_state = {
            "user_input": "Hello, test message",
            "intent": None,
            "response_text": None,
            "complete": False,
            "processing_start_time": time.time(),
            "session_id": "test-langgraph-tracing",
            "node_execution_times": {}
        }
        
        print(f"📝 Test state prepared: {json.dumps({k: v for k, v in test_state.items() if k != 'processing_start_time'}, indent=2)}")
        
        # Execute the graph
        print("\n🎯 Executing LangGraph workflow...")
        start_time = time.time()
        
        result = await aura.process(test_state, session_id="test-langgraph-tracing")
        
        execution_time = time.time() - start_time
        print(f"✅ LangGraph execution completed in {execution_time:.2f}s")
        
        # Display result
        clean_result = {k: v for k, v in result.items() 
                       if k not in ['_audio_bytes', '_screenshot_bytes', 'processing_start_time']}
        print(f"📊 Result: {json.dumps(clean_result, indent=2)}")
        
        # Wait a moment for trace to propagate
        print("\n⏳ Waiting for trace to propagate to LangSmith...")
        await asyncio.sleep(3)
        
        # Check for new runs
        final_runs = list(client.list_runs(project_name=project_name, limit=10))
        final_count = len(final_runs)
        new_runs_count = final_count - initial_count
        
        print(f"📈 Final runs count: {final_count}")
        print(f"🆕 New runs created: {new_runs_count}")
        
        if new_runs_count > 0:
            print("\n🎉 SUCCESS: LangGraph execution created traces in LangSmith!")
            print("📊 Recent runs:")
            for i, run in enumerate(final_runs[:3]):
                print(f"  {i+1}. {run.name} - {run.status} - {run.start_time}")
        else:
            print("\n⚠️  WARNING: No new traces detected in LangSmith")
            print("🔍 This suggests LangGraph execution isn't being traced properly")
            
            # Show environment check
            print("\n🔧 Environment Check:")
            print(f"  LANGCHAIN_TRACING_V2: {os.environ.get('LANGCHAIN_TRACING_V2')}")
            print(f"  LANGCHAIN_PROJECT: {os.environ.get('LANGCHAIN_PROJECT')}")
            print(f"  LANGCHAIN_API_KEY: {'***' + os.environ.get('LANGCHAIN_API_KEY', '')[-10:]}")
        
        return result
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        import traceback
        print(f"🔍 Traceback: {traceback.format_exc()}")
        return None

if __name__ == "__main__":
    # Run the test
    result = asyncio.run(test_langgraph_execution())
    
    if result:
        print("\n✅ Test completed successfully!")
        print("🔗 Check your LangSmith dashboard at: https://smith.langchain.com/")
    else:
        print("\n❌ Test failed!")
