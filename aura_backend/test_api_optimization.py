#!/usr/bin/env python3
"""
Test AURA optimization via HTTP API
"""
import requests
import json

def test_aura_optimization():
    """Test AURA with our optimization"""
    
    base_url = "http://localhost:8000"
    
    # Test cases
    test_cases = [
        {"text": "hello aura", "expected": "greeting"},
        {"text": "hi", "expected": "greeting"}, 
        {"text": "hey aura", "expected": "greeting"},
        {"text": "go back", "expected": "navigation"},
        {"text": "open settings", "expected": "general"}
    ]
    
    print("🧪 Testing AURA Optimization via API")
    print("=" * 50)
    
    for i, test_case in enumerate(test_cases, 1):
        text = test_case["text"]
        expected = test_case["expected"]
        
        print(f"\n{i}. Testing: '{text}'")
        
        try:
            # Make API request
            response = requests.post(
                f"{base_url}/chat",
                json={"text": text, "session_id": f"test-{i}"},
                timeout=10
            )
            
            if response.status_code == 200:
                result = response.json()
                
                print(f"   ✅ Success!")
                print(f"   📝 Intent: {result.get('intent')}")
                print(f"   💬 Response: {result.get('response')}")
                print(f"   🆔 Session: {result.get('session_id')}")
                
                # Check if this looks like an optimized response
                intent = result.get('intent', '').lower()
                if expected == "greeting" and ("greeting" in intent or "hello" in intent):
                    print(f"   🚀 Likely optimized (greeting detected)")
                elif expected == "navigation" and ("back" in intent or "navigate" in intent):
                    print(f"   🚀 Likely optimized (navigation detected)")
                else:
                    print(f"   ⚠️  May not be optimized (intent: {intent})")
                    
            else:
                print(f"   ❌ HTTP Error: {response.status_code}")
                print(f"   📄 Response: {response.text[:200]}")
                
        except Exception as e:
            print(f"   ❌ Request failed: {str(e)}")
    
    print("\n" + "=" * 50)
    print("💡 Check if responses are instant (< 100ms) for greetings!")

if __name__ == "__main__":
    test_aura_optimization()
