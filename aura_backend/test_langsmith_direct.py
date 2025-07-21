"""
Test LangSmith integration directly
"""

import os
from langsmith import Client, traceable
from dotenv import load_dotenv

# Load environment
load_dotenv()

@traceable(name="test_langsmith_integration")
def test_function():
    """Simple test function to verify LangSmith tracing"""
    print("Testing LangSmith integration...")
    return {"status": "success", "message": "LangSmith trace generated!"}

def main():
    # Check environment
    api_key = os.getenv("LANGCHAIN_API_KEY")
    project = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
    tracing = os.getenv("LANGCHAIN_TRACING_V2")
    
    print(f"API Key: {api_key[:20]}..." if api_key else "No API key")
    print(f"Project: {project}")
    print(f"Tracing: {tracing}")
    
    if not api_key:
        print("❌ No LangSmith API key found")
        return
    
    try:
        # Initialize client
        client = Client(api_key=api_key)
        print("✅ LangSmith client initialized")
        
        # Test the traced function
        result = test_function()
        print(f"✅ Function executed: {result}")
        
        print(f"🎯 Check your traces at: https://smith.langchain.com/projects/p/{project}")
        
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
