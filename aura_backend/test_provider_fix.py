#!/usr/bin/env python3
"""
Test script to verify that the provider registry loads API keys correctly
"""

import os
import sys
from dotenv import load_dotenv

# Load environment variables explicitly
load_dotenv()

print("🔍 Testing Provider Registry API Key Loading...")
print("=" * 50)

# Check environment variables first
groq_key = os.getenv("GROQ_API_KEY")
gemini_key = os.getenv("GEMINI_API_KEY")

print(f"📋 Environment Variables:")
print(f"   GROQ_API_KEY: {'✅ Found' if groq_key else '❌ Not found'}")
print(f"   GEMINI_API_KEY: {'✅ Found' if gemini_key else '❌ Not found'}")
print()

# Now test the provider registry
try:
    print("🚀 Importing provider registry...")
    from providers.provider_registry import provider_registry
    
    print("✅ Provider registry imported successfully")
    print()
    
    # Check available providers
    print("📊 Available Providers:")
    for provider_name, provider in provider_registry.providers.items():
        print(f"   {provider_name}: ✅ Initialized")
    
    if not provider_registry.providers:
        print("   ❌ No providers initialized")
    
    print()
    print("🎯 Test completed!")
    
except Exception as e:
    print(f"❌ Error importing provider registry: {e}")
    import traceback
    traceback.print_exc()
