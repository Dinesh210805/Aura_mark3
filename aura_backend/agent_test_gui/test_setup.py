#!/usr/bin/env python3
"""
Quick test script for AURA Test GUI components
"""

import sys
from pathlib import Path

def test_imports():
    """Test all required imports"""
    print("🧪 Testing imports...")
    
    try:
        import streamlit
        print("✅ Streamlit available")
    except ImportError:
        print("❌ Streamlit not installed")
        return False
    
    try:
        import requests
        print("✅ Requests available")
    except ImportError:
        print("❌ Requests not installed")
        return False
    
    try:
        from PIL import Image
        print("✅ Pillow available")
    except ImportError:
        print("❌ Pillow not installed")
        return False
    
    try:
        import soundfile
        print("✅ SoundFile available")
    except ImportError:
        print("❌ SoundFile not installed")
        return False
    
    try:
        import numpy
        print("✅ NumPy available")
    except ImportError:
        print("❌ NumPy not installed")
        return False
    
    return True

def test_utilities():
    """Test local utility modules"""
    print("\n🔧 Testing utility modules...")
    
    try:
        from audio_utils import create_sample_audio, TEST_PHRASES
        print("✅ Audio utilities working")
        
        # Test audio creation
        temp_audio = create_sample_audio("Test", 1.0)
        if Path(temp_audio).exists():
            print("✅ Sample audio creation working")
            Path(temp_audio).unlink()  # Clean up
        else:
            print("❌ Sample audio creation failed")
            return False
            
    except Exception as e:
        print(f"❌ Audio utilities failed: {e}")
        return False
    
    try:
        from screenshot_utils import create_sample_screenshot, SAMPLE_SCREENSHOTS
        print("✅ Screenshot utilities working")
        
        # Test screenshot creation
        img = create_sample_screenshot("Android Home Screen")
        if img.size[0] > 0 and img.size[1] > 0:
            print("✅ Sample screenshot creation working")
        else:
            print("❌ Sample screenshot creation failed")
            return False
            
    except Exception as e:
        print(f"❌ Screenshot utilities failed: {e}")
        return False
    
    return True

def test_backend_connection():
    """Test connection to AURA backend"""
    print("\n🌐 Testing backend connection...")
    
    try:
        import requests
        response = requests.get("http://localhost:8000/health", timeout=5)
        if response.status_code == 200:
            print("✅ AURA backend is running and accessible")
            health_data = response.json()
            print(f"   Status: {health_data.get('status', 'unknown')}")
            services = health_data.get('services', {})
            for service, status in services.items():
                icon = "✅" if status == "operational" else "❌"
                print(f"   {icon} {service.upper()}: {status}")
            return True
        else:
            print(f"❌ Backend responded with status {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Cannot connect to backend - is it running?")
        print("💡 Start it with: cd ../ && python run.py")
        return False
    except Exception as e:
        print(f"❌ Backend test failed: {e}")
        return False

def main():
    """Run all tests"""
    print("🚀 AURA Test GUI - Pre-flight Check")
    print("=" * 40)
    
    # Test imports
    if not test_imports():
        print("\n❌ Import tests failed. Install missing packages:")
        print("pip install streamlit requests Pillow soundfile numpy")
        return False
    
    # Test utilities
    if not test_utilities():
        print("\n❌ Utility tests failed. Check audio_utils.py and screenshot_utils.py")
        return False
    
    # Test backend connection
    backend_ok = test_backend_connection()
    
    print("\n" + "=" * 40)
    if backend_ok:
        print("🎉 All tests passed! Ready to launch AURA Test GUI")
        print("💡 Run: streamlit run streamlit_app.py")
    else:
        print("⚠️  Tests passed but backend is not running")
        print("💡 You can still use the GUI, but start the backend for full functionality")
    
    return True

if __name__ == "__main__":
    main()
