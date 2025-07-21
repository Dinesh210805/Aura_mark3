#!/usr/bin/env python3
"""
Test AURA Graph with simplified LangSmith tracing
"""

import asyncio
import json
import os
import sys
import time

# Add the current directory to the path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Set up environment before any imports
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_ENDPOINT"] = "https://api.smith.langchain.com"
os.environ["LANGCHAIN_API_KEY"] = "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"
os.environ["LANGCHAIN_PROJECT"] = "aura-agent-visualization"

from langsmith import Client

async def test_aura_tracing():
    """Test AURA Graph with automatic LangSmith tracing"""
    print("🧪 Testing AURA Graph with Automatic LangSmith Tracing")
    print("=" * 60)
    
    try:
        # Check LangSmith connection
        client = Client()
        project_name = "aura-agent-visualization"
        
        # Get initial run count
        initial_runs = list(client.list_runs(project_name=project_name, limit=3))
        print(f"📈 Initial runs count: {len(initial_runs)}")
        
        # Import and initialize AURA Graph AFTER setting environment
        from aura_graph import AuraGraph
        
        print("🚀 Initializing AURA Graph...")
        aura = AuraGraph()
        
        # Prepare minimal test state
        test_state = {
            "user_input": "Hello test",
            "complete": False
        }
        
        print("🎯 Executing AURA Graph...")
        start_time = time.time()
        
        # Execute with automatic tracing
        result = await aura.process(test_state, session_id="test-auto-tracing")
        
        execution_time = time.time() - start_time
        print(f"✅ Execution completed in {execution_time:.2f}s")
        
        # Show key results
        print(f"📊 Response: {result.get('response_text', 'No response')}")
        print(f"📊 Intent: {result.get('intent', 'No intent')}")
        print(f"📊 Complete: {result.get('complete', False)}")
        
        # Wait for trace propagation
        print("\n⏳ Waiting for LangSmith trace propagation...")
        await asyncio.sleep(5)
        
        # Check for new traces
        final_runs = list(client.list_runs(project_name=project_name, limit=10))
        new_runs_count = len(final_runs) - len(initial_runs)
        
        print(f"📈 Final runs count: {len(final_runs)}")
        print(f"🆕 New runs created: {new_runs_count}")
        
        if new_runs_count > 0:
            print("\n🎉 SUCCESS: AURA Graph execution created LangSmith traces!")
            print("📊 Recent runs:")
            for i, run in enumerate(final_runs[:3]):
                print(f"  {i+1}. Name: {run.name}")
                print(f"     Status: {run.status}")
                print(f"     Start: {run.start_time}")
                print(f"     Duration: {run.end_time - run.start_time if run.end_time else 'Running'}")
                print()
        else:
            print("\n⚠️  No new traces detected")
            print("🔍 Possible issues:")
            print("  - LangGraph not configured for automatic tracing")
            print("  - Environment variables not properly set")
            print("  - Trace propagation delay")
        
        print(f"\n🔗 Check LangSmith dashboard: https://smith.langchain.com/")
        print(f"📊 Project: {project_name}")
        
        return result
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        import traceback
        print(f"🔍 Traceback: {traceback.format_exc()}")
        return None

if __name__ == "__main__":
    result = asyncio.run(test_aura_tracing())
    
    if result:
        print("\n✅ Test completed!")
    else:
        print("\n❌ Test failed!")
