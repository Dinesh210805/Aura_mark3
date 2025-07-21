"""
Simple LangSmith trace test - creates a trace directly
"""

import os
from langsmith import Client, traceable
from dotenv import load_dotenv

load_dotenv()

@traceable(name="simple_test_trace", project_name="aura-agent-visualization")
def simple_test():
    """Create a simple test trace"""
    print("Creating test trace...")
    return {"message": "Test trace created successfully", "timestamp": "2025-07-21"}

def main():
    api_key = os.getenv("LANGCHAIN_API_KEY")
    project = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
    
    print(f"API Key: {api_key[:20]}..." if api_key else "No API key")
    print(f"Project: {project}")
    print(f"Tracing: {os.getenv('LANGCHAIN_TRACING_V2')}")
    
    if not api_key:
        print("❌ No LangSmith API key found")
        return
    
    try:
        # Test client
        client = Client(api_key=api_key)
        print("✅ LangSmith client created")
        
        # Create test trace
        result = simple_test()
        print(f"✅ Test trace result: {result}")
        
        print(f"🌐 Check traces at: https://smith.langchain.com/projects/p/{project}")
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
