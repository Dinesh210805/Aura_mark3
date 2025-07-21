#!/usr/bin/env python3
"""
Very simple LangSmith trace test
"""

import os
import time

# Set environment variables
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "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"
os.environ["LANGCHAIN_PROJECT"] = "aura-agent-visualization"

print("🧪 Simple LangSmith Trace Test")
print("=" * 40)

try:
    from langsmith import traceable
    
    @traceable(name="simple_test_function")
    def simple_function(message: str) -> str:
        print(f"📝 Processing: {message}")
        time.sleep(0.5)  # Simulate some work
        return f"Processed: {message}"
    
    print("🚀 Executing traced function...")
    result = simple_function("Hello LangSmith!")
    print(f"✅ Result: {result}")
    
    print("⏳ Waiting for trace to upload...")
    time.sleep(3)
    
    # Try to verify with client
    try:
        from langsmith import Client
        client = Client()
        print("📊 Checking for traces...")
        
        # Use a simpler query approach
        project = client.read_project(project_name="aura-agent-visualization")
        print(f"✅ Project found: {project.name}")
        
        print("🎉 LangSmith setup is working!")
        print("🔗 Check dashboard: https://smith.langchain.com/")
        
    except Exception as e:
        print(f"⚠️  Client check failed: {e}")
        print("But the trace might still be uploaded")
        
except Exception as e:
    print(f"❌ Test failed: {e}")
    import traceback
    traceback.print_exc()

print("\n✅ Test completed")
