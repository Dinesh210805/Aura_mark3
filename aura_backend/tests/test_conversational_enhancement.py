"""Test enhanced conversational capabilities of optimized AURA"""
import sys
import os
import asyncio
from datetime import datetime

# Add parent directory to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from optimized_intent_analyzer import OptimizedIntentAnalyzer
from nodes.action_planner_node import ActionPlannerNode

class TestConversationalEnhancement:
    def __init__(self):
        self.intent_analyzer = OptimizedIntentAnalyzer()
        self.action_planner = ActionPlannerNode()
    
    async def test_capability_questions(self):
        """Test various ways users might ask about capabilities"""
        test_cases = [
            "hey aura what can you do",
            "what can you do",
            "what are your capabilities", 
            "help me",
            "how can you help",
            "what do you do",
            "what are you capable of",
            "assist me"
        ]
        
        print("🧪 Testing Capability Questions:")
        print("=" * 50)
        
        for transcript in test_cases:
            print(f"\n📝 Input: '{transcript}'")
            
            # Test intent analysis
            intent_result = await self.intent_analyzer.analyze_intent(transcript)
            print(f"🎯 Intent: {intent_result.get('intent', 'N/A')}")
            print(f"📊 Confidence: {intent_result.get('confidence', 'N/A')}")
            print(f"🏷️ Response Type: {intent_result.get('_response_type', 'N/A')}")
            
            # Test action planning
            action_plan = await self.action_planner.create_action_plan(intent_result, None)
            print(f"📋 Action Type: {action_plan.get('action_type', 'N/A')}")
            
            # Get response preview (first 100 chars)
            response = action_plan.get('response', '')
            if response:
                preview = response[:100] + "..." if len(response) > 100 else response
                print(f"💬 Response Preview: {preview}")
            
            print("-" * 30)
    
    async def test_greetings_with_questions(self):
        """Test greetings combined with capability questions"""
        test_cases = [
            "hello aura what can you do",
            "hi what are your capabilities",
            "hey can you help me",
            "good morning what do you do",
            "hello how can you assist me"
        ]
        
        print("\n🧪 Testing Greetings with Questions:")
        print("=" * 50)
        
        for transcript in test_cases:
            print(f"\n📝 Input: '{transcript}'")
            
            intent_result = await self.intent_analyzer.analyze_intent(transcript)
            print(f"🎯 Intent: {intent_result.get('intent', 'N/A')}")
            print(f"🏷️ Response Type: {intent_result.get('_response_type', 'N/A')}")
            
            action_plan = await self.action_planner.create_action_plan(intent_result, None)
            response = action_plan.get('response', '')
            if response:
                preview = response[:150] + "..." if len(response) > 150 else response
                print(f"💬 Response Preview: {preview}")
            
            print("-" * 30)
    
    async def test_simple_greetings(self):
        """Test simple greetings without questions"""
        test_cases = [
            "hello aura",
            "hi",
            "good morning",
            "hey there"
        ]
        
        print("\n🧪 Testing Simple Greetings:")
        print("=" * 50)
        
        for transcript in test_cases:
            print(f"\n📝 Input: '{transcript}'")
            
            intent_result = await self.intent_analyzer.analyze_intent(transcript)
            print(f"🎯 Intent: {intent_result.get('intent', 'N/A')}")
            print(f"🏷️ Response Type: {intent_result.get('_response_type', 'N/A')}")
            
            action_plan = await self.action_planner.create_action_plan(intent_result, None)
            response = action_plan.get('response', '')
            if response:
                preview = response[:100] + "..." if len(response) > 100 else response
                print(f"💬 Response Preview: {preview}")
            
            print("-" * 30)
    
    async def test_performance_timing(self):
        """Test response timing for key interactions"""
        print("\n🧪 Testing Response Performance:")
        print("=" * 50)
        
        test_phrase = "hey aura what can you do"
        
        start_time = datetime.now()
        intent_result = await self.intent_analyzer.analyze_intent(test_phrase)
        intent_time = (datetime.now() - start_time).total_seconds()
        
        start_time = datetime.now()
        action_plan = await self.action_planner.create_action_plan(intent_result, None)
        action_time = (datetime.now() - start_time).total_seconds()
        
        total_time = intent_time + action_time
        
        print(f"📝 Test Phrase: '{test_phrase}'")
        print(f"⏱️ Intent Analysis: {intent_time:.3f}s")
        print(f"⏱️ Action Planning: {action_time:.3f}s")
        print(f"⏱️ Total Time: {total_time:.3f}s")
        
        # Check if it's fast enough (target < 1 second)
        if total_time < 1.0:
            print("✅ Performance: EXCELLENT (< 1s)")
        elif total_time < 2.0:
            print("✅ Performance: GOOD (< 2s)")
        else:
            print("⚠️ Performance: NEEDS IMPROVEMENT (> 2s)")
    
    async def run_all_tests(self):
        """Run comprehensive conversational enhancement tests"""
        print("🚀 AURA Conversational Enhancement Test Suite")
        print("=" * 60)
        print(f"📅 Test Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"🎯 Focus: Enhanced personality and capability explanations")
        
        await self.test_capability_questions()
        await self.test_greetings_with_questions()
        await self.test_simple_greetings()
        await self.test_performance_timing()
        
        print("\n🎉 Test Suite Complete!")
        print("=" * 60)

# Main execution
async def main():
    tester = TestConversationalEnhancement()
    await tester.run_all_tests()

if __name__ == "__main__":
    asyncio.run(main())
