#!/usr/bin/env python3
"""
Simple launcher for AURA Test GUI
Installs dependencies and starts the Streamlit app
"""

import subprocess
import sys
import os
from pathlib import Path

def install_requirements():
    """Install required packages"""
    requirements_file = Path(__file__).parent / "requirements.txt"
    if requirements_file.exists():
        print("📦 Installing requirements...")
        subprocess.check_call([
            sys.executable, "-m", "pip", "install", "-r", str(requirements_file)
        ])
    else:
        print("⚠️ requirements.txt not found, installing basic packages...")
        packages = ["streamlit", "requests", "Pillow", "soundfile", "numpy"]
        for package in packages:
            subprocess.check_call([sys.executable, "-m", "pip", "install", package])

def check_backend():
    """Check if AURA backend is running"""
    try:
        import requests
        response = requests.get("http://localhost:8000/health", timeout=5)
        if response.status_code == 200:
            print("✅ AURA backend is running")
            return True
    except:
        pass
    
    print("⚠️  AURA backend not detected")
    print("💡 Start it with: cd ../ && python run.py")
    return False

def main():
    """Main launcher function"""
    print("🚀 AURA Agent Test GUI Launcher")
    print("=" * 40)
    
    # Install requirements
    try:
        install_requirements()
        print("✅ Requirements installed")
    except Exception as e:
        print(f"❌ Failed to install requirements: {e}")
        return
    
    # Check backend
    check_backend()
    
    # Launch Streamlit
    print("\n🌐 Launching Streamlit app...")
    print("📱 Open http://localhost:8501 in your browser")
    print("🛑 Press Ctrl+C to stop")
    print()
    
    try:
        app_file = Path(__file__).parent / "streamlit_app.py"
        subprocess.run([sys.executable, "-m", "streamlit", "run", str(app_file)])
    except KeyboardInterrupt:
        print("\n👋 AURA Test GUI stopped")
    except Exception as e:
        print(f"\n❌ Failed to start Streamlit: {e}")

if __name__ == "__main__":
    main()
