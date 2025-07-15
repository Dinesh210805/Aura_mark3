#!/usr/bin/env python3
"""
Debug script to trace the graph execution
"""
import asyncio
import httpx
import json

async def test_graph_execution():
    """Test and trace graph execution"""
    print("🔍 Testing LangGraph execution...")
    
    # First test the graph info endpoint
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get("http://localhost:8000/graph/info")
            
            if response.status_code == 200:
                graph_info = response.json()
                print(f"✅ Graph Info: {json.dumps(graph_info, indent=2)}")
            else:
                print(f"❌ Graph Info Error: {response.status_code}")
                
    except Exception as e:
        print(f"❌ Graph Info Exception: {str(e)}")
    
    # Test chat with detailed error reporting
    print("\n💬 Testing chat with debugging...")
    
    url = "http://localhost:8000/chat"
    data = {
        "text": "Hello, tell me the current time",
        "session_id": "debug_trace_session"
    }
    
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(url, json=data)
            
            print(f"Status: {response.status_code}")
            
            if response.status_code == 200:
                result = response.json()
                print(f"Response: {json.dumps(result, indent=2)}")
                
                # Analyze the response
                success = result.get("success", False)
                response_text = result.get("response", "")
                intent = result.get("intent")
                session_id = result.get("session_id")
                
                print(f"\nAnalysis:")
                print(f"Success: {success}")
                print(f"Response: '{response_text}'")
                print(f"Intent: {intent}")
                print(f"Session: {session_id}")
                
                if response_text == "No response generated":
                    print("❌ Issue: LangGraph execution didn't set response_text")
                elif not intent:
                    print("⚠️ Issue: Intent analysis failed")
                else:
                    print("✅ Execution seems successful")
                    
            else:
                print(f"❌ HTTP Error: {response.text}")
                
    except Exception as e:
        print(f"❌ Chat Exception: {str(e)}")

async def test_session_history():
    """Test session history to see if state was saved"""
    print("\n📚 Testing session history...")
    
    session_id = "debug_trace_session"
    url = f"http://localhost:8000/session/{session_id}/history"
    
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(url)
            
            if response.status_code == 200:
                history = response.json()
                print(f"Session History: {json.dumps(history, indent=2)}")
            else:
                print(f"❌ History Error: {response.status_code} - {response.text}")
                
    except Exception as e:
        print(f"❌ History Exception: {str(e)}")

async def main():
    """Main debug function"""
    print("🧠 AURA LangGraph Debug")
    print("=" * 40)
    
    await test_graph_execution()
    await test_session_history()
    
    print("\n" + "=" * 40)

if __name__ == "__main__":
    asyncio.run(main())
