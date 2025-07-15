#!/usr/bin/env python3
"""
Comprehensive Backend Test for AURA Integration
Tests all endpoints and functionality that your Android app will use
"""

import asyncio
import aiohttp
import base64
import json
import time
import os
from pathlib import Path

# Test configuration
BASE_URL = "http://localhost:8000"
TEST_SESSION_ID = "aura-integration-test"

class AuraBackendTester:
    def __init__(self):
        self.session = None
        self.results = {
            "health": False,
            "chat": False,
            "process": False,
            "graph_info": False,
            "session_management": False
        }
    
    async def setup(self):
        """Setup HTTP session"""
        self.session = aiohttp.ClientSession()
    
    async def cleanup(self):
        """Cleanup HTTP session"""
        if self.session:
            await self.session.close()
    
    async def test_health_endpoint(self):
        """Test /health endpoint"""
        print("🩺 Testing health endpoint...")
        try:
            async with self.session.get(f"{BASE_URL}/health") as response:
                if response.status == 200:
                    data = await response.json()
                    print(f"   ✅ Health check passed: {data.get('status')}")
                    print(f"   📊 Services: {data.get('services', {})}")
                    self.results["health"] = True
                    return True
                else:
                    print(f"   ❌ Health check failed: {response.status}")
                    return False
        except Exception as e:
            print(f"   ❌ Health check error: {str(e)}")
            return False
    
    async def test_chat_endpoint(self):
        """Test /chat endpoint (text-only)"""
        print("💬 Testing chat endpoint...")
        try:
            payload = {
                "text": "Hello AURA, can you help me open settings?",
                "session_id": TEST_SESSION_ID
            }
            
            async with self.session.post(
                f"{BASE_URL}/chat",
                json=payload,
                headers={"Content-Type": "application/json"}
            ) as response:
                if response.status == 200:
                    data = await response.json()
                    print(f"   ✅ Chat response: {data.get('response', '')[:100]}...")
                    print(f"   🎯 Intent: {data.get('intent', 'N/A')}")
                    self.results["chat"] = True
                    return True
                else:
                    text = await response.text()
                    print(f"   ❌ Chat failed: {response.status} - {text}")
                    return False
        except Exception as e:
            print(f"   ❌ Chat error: {str(e)}")
            return False
    
    async def test_process_endpoint_text_only(self):
        """Test /process endpoint with audio simulation"""
        print("🎤 Testing process endpoint (simulated audio)...")
        try:
            # Create a dummy WAV file for testing
            dummy_audio = self.create_dummy_wav()
            
            form_data = aiohttp.FormData()
            form_data.add_field('audio', dummy_audio, filename='test.wav', content_type='audio/wav')
            form_data.add_field('session_id', TEST_SESSION_ID)
            form_data.add_field('ui_tree', self.create_dummy_ui_tree())
            
            async with self.session.post(f"{BASE_URL}/process", data=form_data) as response:
                if response.status == 200:
                    data = await response.json()
                    print(f"   ✅ Process successful: {data.get('success')}")
                    print(f"   📝 Transcript: {data.get('transcript', 'N/A')}")
                    print(f"   🎯 Intent: {data.get('intent', 'N/A')}")
                    print(f"   📋 Action plan: {len(data.get('action_plan', []))} steps")
                    print(f"   🔊 TTS audio: {'Yes' if data.get('tts_audio') else 'No'}")
                    self.results["process"] = True
                    return True
                else:
                    text = await response.text()
                    print(f"   ❌ Process failed: {response.status} - {text}")
                    return False
        except Exception as e:
            print(f"   ❌ Process error: {str(e)}")
            return False
    
    async def test_graph_info(self):
        """Test /graph/info endpoint"""
        print("🧠 Testing graph info endpoint...")
        try:
            async with self.session.get(f"{BASE_URL}/graph/info") as response:
                if response.status == 200:
                    data = await response.json()
                    print(f"   ✅ Graph info retrieved")
                    print(f"   🔗 Nodes: {data.get('nodes', [])}")
                    print(f"   📍 Entry point: {data.get('entry_point')}")
                    self.results["graph_info"] = True
                    return True
                else:
                    print(f"   ❌ Graph info failed: {response.status}")
                    return False
        except Exception as e:
            print(f"   ❌ Graph info error: {str(e)}")
            return False
    
    async def test_session_management(self):
        """Test session management"""
        print("🔄 Testing session management...")
        try:
            # Test session history
            async with self.session.get(f"{BASE_URL}/session/{TEST_SESSION_ID}/history") as response:
                if response.status == 200:
                    data = await response.json()
                    print(f"   ✅ Session history retrieved")
                    
                    # Test session clearing
                    async with self.session.delete(f"{BASE_URL}/session/{TEST_SESSION_ID}") as clear_response:
                        if clear_response.status == 200:
                            print(f"   ✅ Session cleared successfully")
                            self.results["session_management"] = True
                            return True
                        else:
                            print(f"   ❌ Session clear failed: {clear_response.status}")
                            return False
                else:
                    print(f"   ❌ Session history failed: {response.status}")
                    return False
        except Exception as e:
            print(f"   ❌ Session management error: {str(e)}")
            return False
    
    def create_dummy_wav(self):
        """Create a minimal WAV file for testing"""
        # Minimal WAV header + some dummy audio data
        wav_header = b'RIFF'
        wav_header += (44 + 1000).to_bytes(4, 'little')  # File size
        wav_header += b'WAVE'
        wav_header += b'fmt '
        wav_header += (16).to_bytes(4, 'little')  # Fmt chunk size
        wav_header += (1).to_bytes(2, 'little')   # Audio format (PCM)
        wav_header += (1).to_bytes(2, 'little')   # Num channels
        wav_header += (16000).to_bytes(4, 'little')  # Sample rate
        wav_header += (32000).to_bytes(4, 'little')  # Byte rate
        wav_header += (2).to_bytes(2, 'little')   # Block align
        wav_header += (16).to_bytes(2, 'little')  # Bits per sample
        wav_header += b'data'
        wav_header += (1000).to_bytes(4, 'little')  # Data size
        
        # Add some dummy audio data (silence)
        dummy_data = b'\x00' * 1000
        
        return wav_header + dummy_data
    
    def create_dummy_ui_tree(self):
        """Create a dummy UI tree XML for testing"""
        return '''<?xml version="1.0" encoding="UTF-8"?>
<hierarchy>
    <node class="android.widget.FrameLayout" clickable="false" enabled="true">
        <node class="android.widget.TextView" text="Settings" clickable="true" enabled="true" bounds="[100,200][300,250]"/>
        <node class="android.widget.Button" text="OK" clickable="true" enabled="true" bounds="[400,500][500,550]"/>
        <node class="android.widget.EditText" text="" clickable="true" enabled="true" bounds="[50,100][350,150]"/>
    </node>
</hierarchy>'''
    
    async def run_all_tests(self):
        """Run all tests"""
        print("🚀 AURA Backend Integration Test")
        print("=" * 50)
        
        await self.setup()
        
        try:
            # Check if server is running
            print("🔍 Checking if server is running...")
            try:
                async with self.session.get(f"{BASE_URL}/") as response:
                    if response.status == 200:
                        data = await response.json()
                        print(f"   ✅ Server running: {data.get('message', '')}")
                    else:
                        print(f"   ❌ Server not responding properly: {response.status}")
                        return
            except Exception as e:
                print(f"   ❌ Server not running: {str(e)}")
                print(f"   💡 Please start the server with: python run.py")
                return
            
            print()
            
            # Run all tests
            await self.test_health_endpoint()
            print()
            await self.test_chat_endpoint()
            print()
            await self.test_process_endpoint_text_only()
            print()
            await self.test_graph_info()
            print()
            await self.test_session_management()
            
            # Summary
            print("\n" + "=" * 50)
            print("📊 Test Results Summary:")
            passed = sum(self.results.values())
            total = len(self.results)
            
            for test, result in self.results.items():
                status = "✅ PASS" if result else "❌ FAIL"
                print(f"   {test.replace('_', ' ').title()}: {status}")
            
            print(f"\n🎯 Overall: {passed}/{total} tests passed")
            
            if passed == total:
                print("🎉 All tests passed! Your backend is ready for AURA integration!")
                print("\n💡 Next Steps:")
                print("   1. Update your Android app to use http://localhost:8000")
                print("   2. Test with real audio files from your app")
                print("   3. Test with actual screenshots from accessibility service")
            else:
                print("❌ Some tests failed. Please check the server logs.")
        
        finally:
            await self.cleanup()

async def main():
    """Main test function"""
    tester = AuraBackendTester()
    await tester.run_all_tests()

if __name__ == "__main__":
    asyncio.run(main())
