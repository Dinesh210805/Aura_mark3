"""
LangGraph orchestration debugging test
"""
import asyncio
import sys
import os
from dotenv import load_dotenv

# Try multiple .env locations
env_paths = [
    ".env",  # Current directory
    "../.env",  # Parent directory
    "d:/PROJECTS/Aura_mark3/.env",  # Absolute path
    "d:/PROJECTS/Aura_mark3/aura_backend/.env"  # Backend .env
]

for env_path in env_paths:
    if os.path.exists(env_path):
        print(f"📁 Loading environment from: {env_path}")
        load_dotenv(dotenv_path=env_path)
        break

import logging
import json
import traceback

# Set up detailed logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

async def test_langgraph_orchestration():
    """Test the full LangGraph orchestration"""
    print("=" * 60)
    print("Testing LangGraph Orchestration")
    print("=" * 60)
    
    from aura_graph import aura_graph
    
    # Test with text-only state (skip STT)
    initial_state = {
        "transcript": "Hello, tell me the current time",
        "session_id": "test_langgraph_debug"
    }
    
    print(f"📥 Initial state: {json.dumps(initial_state, indent=2)}")
    
    try:
        print("\n🚀 Starting LangGraph processing...")
        result = await aura_graph.process(initial_state, "test_langgraph_debug")
        
        print(f"\n📤 Final result: {json.dumps({k: v for k, v in result.items() if k not in ['tts_audio', 'audio_data', 'screenshot_data']}, indent=2)}")
        
        # Check key fields
        success = not result.get("error")
        has_response = bool(result.get("response_text"))
        has_intent = bool(result.get("intent"))
        
        print(f"\n📊 Result Analysis:")
        print(f"  Success: {'✅' if success else '❌'} {success}")
        print(f"  Has Response Text: {'✅' if has_response else '❌'} '{result.get('response_text', 'None')}'")
        print(f"  Has Intent: {'✅' if has_intent else '❌'} '{result.get('intent', 'None')}'")
        print(f"  Complete: {'✅' if result.get('complete') else '❌'} {result.get('complete')}")
        print(f"  Error: {'❌' if result.get('error') else '✅'} {result.get('error', 'None')}")
        
        # Show execution times
        if result.get("node_execution_times"):
            print(f"\n⏱️ Node Execution Times:")
            for node, time_taken in result["node_execution_times"].items():
                print(f"  {node}: {time_taken:.3f}s")
        
        return result
        
    except Exception as e:
        error_trace = traceback.format_exc()
        print(f"\n❌ LangGraph Error: {str(e)}")
        print(f"❌ Traceback: {error_trace}")
        return {"error": str(e)}

async def test_graph_structure():
    """Test the graph structure and routing"""
    print("\n" + "=" * 60)
    print("Testing Graph Structure and Routing")
    print("=" * 60)
    
    from aura_graph import aura_graph
    
    # Get graph info
    graph_info = aura_graph.get_graph_info()
    print(f"📋 Graph Info: {json.dumps(graph_info, indent=2)}")
    
    # Test routing logic with different states
    test_states = [
        {
            "transcript": "test",
            "intent": "test",
            "use_vlm": False,
            "description": "Should go to action planner (no VLM)"
        },
        {
            "transcript": "test", 
            "intent": "test",
            "use_vlm": True,
            "description": "Should go to VLM"
        },
        {
            "transcript": "test",
            "intent": "test", 
            "error": "test error",
            "description": "Should go to TTS (error path)"
        },
        {
            "transcript": "test",
            "intent": "test",
            "action_plan": [{"test": "plan"}],
            "description": "Should go to TTS (has action plan)"
        }
    ]
    
    print(f"\n🔀 Testing Routing Logic:")
    for i, state in enumerate(test_states):
        try:
            route = aura_graph._route_after_ui_check(state)
            print(f"  Test {i+1}: {state['description']} → {route}")
        except Exception as e:
            print(f"  Test {i+1}: {state['description']} → ERROR: {e}")

async def test_individual_graph_nodes():
    """Test each node in the graph manually"""
    print("\n" + "=" * 60)
    print("Testing Individual Graph Nodes in Sequence")
    print("=" * 60)
    
    # Start with initial state
    state = {
        "transcript": "Hello, tell me the current time",
        "session_id": "test_sequence",
        "node_execution_times": {}
    }
    
    print(f"🏁 Starting state: {json.dumps(state, indent=2)}")
    
    try:
        # 1. STT Node (should pass through since transcript exists)
        print(f"\n1️⃣ Testing STT Node...")
        from nodes.stt_node import stt_node
        state = await stt_node.run(state)
        print(f"   Result: transcript='{state.get('transcript')}', error={state.get('error')}")
        
        if state.get("error"):
            print(f"   ❌ STT failed: {state['error']}")
            return state
        
        # 2. Intent Node
        print(f"\n2️⃣ Testing Intent Node...")
        from nodes.intent_node import intent_node
        state = await intent_node.run(state)
        print(f"   Result: intent='{state.get('intent')}', use_vlm={state.get('use_vlm')}, error={state.get('error')}")
        
        if state.get("error"):
            print(f"   ❌ Intent failed: {state['error']}")
            return state
        
        # 3. UI Check Node
        print(f"\n3️⃣ Testing UI Check Node...")
        from nodes.ui_check_node import ui_check_node
        state = await ui_check_node.run(state)
        print(f"   Result: use_vlm={state.get('use_vlm')}, action_plan={bool(state.get('action_plan'))}, error={state.get('error')}")
        
        if state.get("error"):
            print(f"   ❌ UI Check failed: {state['error']}")
            return state
        
        # 4. Based on routing, test next node
        if state.get("use_vlm", False) and not state.get("action_plan"):
            print(f"\n4️⃣ Testing VLM Node...")
            from nodes.vlm_node import vlm_node
            state = await vlm_node.run(state)
            print(f"   Result: action_plan={bool(state.get('action_plan'))}, error={state.get('error')}")
            
            if not state.get("error") and not state.get("action_plan"):
                print(f"\n5️⃣ Testing Action Planner Node...")
                from nodes.action_planner_node import action_planner_node
                state = await action_planner_node.run(state)
                print(f"   Result: action_plan={bool(state.get('action_plan'))}, error={state.get('error')}")
        
        # 5. TTS Node (final)
        print(f"\n🔊 Testing TTS Node...")
        from nodes.tts_node import tts_node
        state = await tts_node.run(state)
        print(f"   Result: response_text='{state.get('response_text')}', complete={state.get('complete')}, error={state.get('error')}")
        
        print(f"\n✅ Manual sequence completed!")
        print(f"📤 Final state keys: {list(state.keys())}")
        
        return state
        
    except Exception as e:
        error_trace = traceback.format_exc()
        print(f"\n❌ Manual sequence error: {str(e)}")
        print(f"❌ Traceback: {error_trace}")
        return {"error": str(e)}

async def main():
    """Run all LangGraph debugging tests"""
    print("🔍 AURA Backend LangGraph Debugging")
    print("Identifying the orchestration issue...")
    
    try:
        # Test graph structure
        await test_graph_structure()
        
        # Test manual sequence
        manual_result = await test_individual_graph_nodes()
        
        # Test LangGraph orchestration
        langgraph_result = await test_langgraph_orchestration()
        
        print("\n" + "=" * 60)
        print("🔬 Debugging Summary")
        print("=" * 60)
        
        manual_success = manual_result and not manual_result.get('error') and manual_result.get('response_text')
        langgraph_success = langgraph_result and not langgraph_result.get('error') and langgraph_result.get('response_text')
        
        print(f"Manual Sequence: {'✅ Working' if manual_success else '❌ Failed'}")
        print(f"LangGraph Orchestration: {'✅ Working' if langgraph_success else '❌ Failed'}")
        
        if manual_success and not langgraph_success:
            print("\n🎯 ISSUE IDENTIFIED: LangGraph orchestration problem")
            print("   All nodes work individually but not through LangGraph")
            print("   Possible issues:")
            print("   1. Graph compilation/setup problem")
            print("   2. State passing between nodes")
            print("   3. Conditional edge routing")
            print("   4. Memory checkpointer interference")
        elif manual_success and langgraph_success:
            print("\n✅ ISSUE RESOLVED: Both manual and LangGraph working!")
        else:
            print("\n❌ NODES HAVE ISSUES: Check individual node implementations")
            
    except Exception as e:
        print(f"❌ Debugging error: {str(e)}")
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())
