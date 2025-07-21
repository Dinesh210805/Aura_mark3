"""
Direct LangSmith tracing test to verify integration
"""

import os
from langsmith import Client, traceable
from dotenv import load_dotenv

# Load environment
load_dotenv()

@traceable(name="test_function")
def simple_test(message: str) -> str:
    """Simple function to test LangSmith tracing"""
    return f"Processed: {message}"

def main():
    # Check environment variables
    api_key = os.getenv("LANGCHAIN_API_KEY")
    project = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
    tracing = os.getenv("LANGCHAIN_TRACING_V2")
    endpoint = os.getenv("LANGCHAIN_ENDPOINT")
    
    print(f"🔑 API Key: {api_key[:20]}..." if api_key else "❌ No API key")
    print(f"📊 Project: {project}")
    print(f"🔍 Tracing: {tracing}")
    print(f"🌐 Endpoint: {endpoint}")
    
    if not api_key:
        print("❌ LangSmith API key not found!")
        return
    
    try:
        # Initialize client
        client = Client()
        print("✅ LangSmith client initialized")
        
        # Test traced function
        result = simple_test("Hello LangSmith!")
        print(f"✅ Function result: {result}")
        
        # Try to create a run manually
        with client.trace(name="manual_test_run", project_name=project) as run:
            run.inputs = {"test": "manual trace"}
            print("✅ Manual trace created")
            run.outputs = {"result": "success"}
        
        print(f"🎯 Check traces at: https://smith.langchain.com/projects/p/{project}")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
