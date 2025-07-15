#!/usr/bin/env python3
"""
AURA Backend Agent Startup Script
Runs the FastAPI server with LangGraph orchestration
"""

import uvicorn
import os
import sys
from dotenv import load_dotenv

def main():
    """Main startup function"""
    
    # Load environment variables
    load_dotenv()
    
    # Check required environment variables
    required_vars = ["GROQ_API_KEY"]
    missing_vars = [var for var in required_vars if not os.getenv(var)]
    
    if missing_vars:
        print(f"❌ Missing required environment variables: {missing_vars}")
        print("Please set them in your .env file or environment")
        print("\nExample .env file:")
        print("GROQ_API_KEY=your_groq_api_key_here")
        print("# PlayAI TTS is accessed through Groq API")
        sys.exit(1)
    
    # Display startup information
    print("🚀 Starting AURA Backend Agent...")
    print("=" * 50)
    print("📊 LangGraph orchestration enabled")
    print("🎤 Groq STT/LLM/VLM integration ready")
    print("🔊 PlayAI TTS via Groq integration ready")
    print("🌐 FastAPI server starting on http://localhost:8000")
    print("📚 API documentation: http://localhost:8000/docs")
    print("=" * 50)
    
    # Get configuration from environment
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", 8000))
    reload = os.getenv("RELOAD", "true").lower() == "true"
    log_level = os.getenv("LOG_LEVEL", "info").lower()
    
    # Start the server
    try:
        uvicorn.run(
            "main:app",
            host=host,
            port=port,
            reload=reload,
            log_level=log_level,
            access_log=True
        )
    except KeyboardInterrupt:
        print("\n🛑 AURA Backend Agent stopped by user")
    except Exception as e:
        print(f"❌ Failed to start server: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
