#!/usr/bin/env python3
"""
Test script to verify optimization is working
"""

def test_instant_response():
    """Test instant response matching"""
    from optimized_intent_analyzer import optimized_intent_analyzer
    
    test_cases = [
        "hello",
        "hello aura", 
        "hi",
        "hi aura",
        "hey aura",
        "good morning",
        "go back",
        "home"
    ]
    
    print("🧪 Testing Instant Response System")
    print("=" * 50)
    
    for test_input in test_cases:
        result = optimized_intent_analyzer._check_instant_response(test_input)
        if result:
            print(f"✅ '{test_input}' -> {result['intent']} (category: {result['_category']})")
        else:
            print(f"❌ '{test_input}' -> No instant response")
    
    print("\n" + "=" * 50)

async def test_full_analysis():
    """Test full optimized analysis"""
    from optimized_intent_analyzer import optimized_intent_analyzer
    import asyncio
    
    print("🚀 Testing Full Optimized Analysis")
    print("=" * 50)
    
    test_cases = ["hello aura", "open settings", "click the button"]
    
    for test_input in test_cases:
        print(f"\n📝 Testing: '{test_input}'")
        try:
            result = await optimized_intent_analyzer.analyze_intent_optimized(test_input)
            print(f"   Intent: {result.get('intent')}")
            print(f"   Category: {result.get('_category')}")
            print(f"   Instant: {result.get('_instant_response', False)}")
            print(f"   Time: {result.get('_analysis_time', 0):.4f}s")
            print(f"   Confidence: {result.get('confidence')}")
        except Exception as e:
            print(f"   ❌ Error: {e}")

if __name__ == "__main__":
    # Test instant responses first
    test_instant_response()
    
    # Test full analysis
    import asyncio
    asyncio.run(test_full_analysis())
