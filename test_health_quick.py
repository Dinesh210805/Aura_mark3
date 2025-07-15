#!/usr/bin/env python3
"""
Quick health check for AURA backend
"""
import requests
import json

def test_health():
    """Test the health endpoint"""
    url = "http://localhost:8000/health"
    
    print("🏥 Testing Health Endpoint...")
    print(f"URL: {url}")
    print("-" * 30)
    
    try:
        print("📡 Sending request...")
        response = requests.get(url, timeout=5)
        
        print(f"✅ Status Code: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Health Check Response:")
            print(json.dumps(result, indent=2))
            
            # Check if all services are operational
            services = result.get("services", {})
            all_operational = all(status == "operational" for status in services.values())
            
            if all_operational:
                print("\n🎯 All services are operational!")
                return True
            else:
                print(f"\n⚠️ Some services are not operational: {services}")
                return False
        else:
            print(f"❌ HTTP Error: {response.status_code}")
            print(f"Response: {response.text}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ Connection Error: Backend is not running on localhost:8000")
        return False
    except requests.exceptions.Timeout:
        print("❌ Timeout Error: Backend took too long to respond")
        return False
    except Exception as e:
        print(f"❌ Unexpected Error: {str(e)}")
        return False

if __name__ == "__main__":
    success = test_health()
    if success:
        print("\n🎉 Backend is ready for testing!")
    else:
        print("\n💥 Backend is not ready. Check the backend logs.")
