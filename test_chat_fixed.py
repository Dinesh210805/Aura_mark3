"""
Test script for the fixed chat endpoint
"""
import asyncio
import httpx
import json

async def test_chat_endpoint():
    """Test the chat endpoint with text-only input"""
    
    url = "http://localhost:8000/chat"
    
    test_data = {
        "text": "Hello, tell me the current time",
        "session_id": "test_session_123"
    }
    
    print("Testing chat endpoint...")
    print(f"Request: {json.dumps(test_data, indent=2)}")
    
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(url, json=test_data)
            
            print(f"\nStatus Code: {response.status_code}")
            
            if response.status_code == 200:
                result = response.json()
                print(f"Response: {json.dumps(result, indent=2)}")
                
                # Check if we got proper fields
                if result.get("success") and result.get("response"):
                    print("\n✅ Chat endpoint is working properly!")
                else:
                    print("\n❌ Chat endpoint returned success but missing response")
            else:
                print(f"Error: {response.text}")
                
    except Exception as e:
        print(f"Error testing chat endpoint: {str(e)}")

async def test_health_endpoint():
    """Test the health endpoint"""
    
    url = "http://localhost:8000/health"
    
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(url)
            
            print(f"Health Status Code: {response.status_code}")
            
            if response.status_code == 200:
                result = response.json()
                print(f"Health Response: {json.dumps(result, indent=2)}")
            else:
                print(f"Health Error: {response.text}")
                
    except Exception as e:
        print(f"Error testing health endpoint: {str(e)}")

async def main():
    """Run all tests"""
    print("=" * 50)
    print("AURA Backend Chat Endpoint Test")
    print("=" * 50)
    
    # Test health first
    await test_health_endpoint()
    print("\n" + "-" * 50 + "\n")
    
    # Test chat endpoint
    await test_chat_endpoint()
    
    print("\n" + "=" * 50)

if __name__ == "__main__":
    asyncio.run(main())
