"""
Simple test for AURA Backend chat endpoint
"""
import requests
import json

def test_health():
    """Test health endpoint"""
    try:
        response = requests.get("http://localhost:8000/health", timeout=10)
        print(f"Health Status: {response.status_code}")
        if response.status_code == 200:
            result = response.json()
            print(f"Health Response: {json.dumps(result, indent=2)}")
            return True
        else:
            print(f"Health Error: {response.text}")
            return False
    except Exception as e:
        print(f"Health Error: {str(e)}")
        return False

def test_chat():
    """Test chat endpoint"""
    try:
        url = "http://localhost:8000/chat"
        data = {
            "text": "Hello, tell me the current time",
            "session_id": "test_session_123"
        }
        
        print("Testing chat endpoint...")
        print(f"Request: {json.dumps(data, indent=2)}")
        
        response = requests.post(url, json=data, timeout=30)
        print(f"Chat Status: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"Chat Response: {json.dumps(result, indent=2)}")
            
            if result.get("success") and result.get("response"):
                print("\n✅ Chat endpoint is working!")
                return True
            else:
                print("\n❌ Chat endpoint returned success but missing response")
                return False
        else:
            print(f"Chat Error: {response.text}")
            return False
            
    except Exception as e:
        print(f"Chat Error: {str(e)}")
        return False

def main():
    """Main test function"""
    print("=" * 50)
    print("AURA Backend Simple Test")
    print("=" * 50)
    
    # Test health
    health_ok = test_health()
    print("\n" + "-" * 50 + "\n")
    
    # Test chat if health is ok
    if health_ok:
        chat_ok = test_chat()
    else:
        print("❌ Skipping chat test due to health check failure")
        chat_ok = False
    
    print("\n" + "=" * 50)
    if health_ok and chat_ok:
        print("🎉 All tests passed!")
    else:
        print("❌ Some tests failed")

if __name__ == "__main__":
    main()
