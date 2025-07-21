"""Simple test for conversational enhancement without complex dependencies"""
import sys
import os
import asyncio
import logging

# Suppress warnings
logging.getLogger().setLevel(logging.ERROR)

# Add parent directory to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

async def test_conversational_responses():
    """Test the enhanced conversational capabilities directly"""
    print("🧪 AURA Conversational Enhancement Test")
    print("=" * 50)
    
    try:
        from optimized_intent_analyzer import OptimizedIntentAnalyzer
        from nodes.action_planner_node import ActionPlannerNode
        
        intent_analyzer = OptimizedIntentAnalyzer()
        action_planner = ActionPlannerNode()
        
        # Test key conversational phrases
        test_cases = [
            "hey aura what can you do",
            "what can you do", 
            "hello aura",
            "help me"
        ]
        
        for transcript in test_cases:
            print(f"\n📝 Testing: '{transcript}'")
            
            # Analyze intent
            intent_result = await intent_analyzer.analyze_intent(transcript)
            print(f"   🎯 Intent: {intent_result.get('intent', 'N/A')}")
            print(f"   📊 Confidence: {intent_result.get('confidence', 'N/A')}")
            print(f"   🏷️ Response Type: {intent_result.get('_response_type', 'None')}")
            
            # Create action plan
            action_plan = await action_planner.create_action_plan(intent_result, None)
            response = action_plan.get('response', 'No response generated')
            
            # Show response preview
            if len(response) > 100:
                preview = response[:100] + "..."
            else:
                preview = response
            
            print(f"   💬 Response: {preview}")
            print("   " + "-" * 40)
        
        print("\n✅ Conversational enhancement test completed!")
        
    except Exception as e:
        print(f"❌ Test failed with error: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_conversational_responses())
