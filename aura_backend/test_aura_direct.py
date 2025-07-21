#!/usr/bin/env python3
"""
Test AURA Graph import and basic functionality
"""

import os
import sys
import time

# Set environment variables first
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "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"
os.environ["LANGCHAIN_PROJECT"] = "aura-agent-visualization"

# Add current directory to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

print("🧪 Testing AURA Graph Import and Basic Functionality")
print("=" * 60)

try:
    print("📦 Importing AURA Graph...")
    
    # Import with try-catch to see where it might fail
    try:
        from aura_graph import AuraGraph
        print("✅ AURA Graph imported successfully")
    except Exception as e:
        print(f"❌ Import failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    
    print("🏗️  Initializing AURA Graph...")
    
    try:
        aura = AuraGraph()
        print("✅ AURA Graph initialized successfully")
        print(f"📊 LangSmith client: {'Configured' if aura.langsmith_client else 'Not configured'}")
        print(f"📊 Project name: {getattr(aura, 'project_name', 'Not set')}")
    except Exception as e:
        print(f"❌ Initialization failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    
    print("🎯 Testing simple state processing...")
    
    # Create a minimal test state
    test_state = {
        "user_input": "hello test",
        "complete": False
    }
    
    print(f"📝 Test state: {test_state}")
    print("🚀 Starting AURA processing (this should create LangSmith traces)...")
    
    # This should work and create traces if properly configured
    import asyncio
    
    async def run_test():
        try:
            result = await aura.process(test_state, session_id="test-direct-import")
            return result
        except Exception as e:
            print(f"❌ Processing failed: {e}")
            import traceback
            traceback.print_exc()
            return None
    
    # Run the test with a timeout
    start_time = time.time()
    try:
        result = asyncio.run(asyncio.wait_for(run_test(), timeout=30.0))
        execution_time = time.time() - start_time
        
        if result:
            print(f"✅ Processing completed in {execution_time:.2f}s")
            print(f"📊 Result keys: {list(result.keys())}")
            print(f"📊 Response: {result.get('response_text', 'No response')}")
            print(f"📊 Intent: {result.get('intent', 'No intent')}")
            print(f"📊 Complete: {result.get('complete', False)}")
            print("\n🎉 SUCCESS: AURA Graph executed successfully!")
            print("📈 LangSmith traces should be created")
        else:
            print("❌ Processing returned None")
            
    except asyncio.TimeoutError:
        print("⏱️  Timeout: Processing took longer than 30 seconds")
        print("🔍 This suggests an issue with the graph execution")
    except Exception as e:
        print(f"❌ Execution error: {e}")
        import traceback
        traceback.print_exc()
    
    print(f"\n🔗 Check LangSmith dashboard: https://smith.langchain.com/")
    print(f"📊 Project: aura-agent-visualization")
    
except Exception as e:
    print(f"❌ Test failed: {e}")
    import traceback
    traceback.print_exc()

print("\n🏁 Test completed")
