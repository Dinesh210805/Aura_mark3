#!/usr/bin/env python3
"""
Simplified LangGraph tracing test
"""

import asyncio
import os
import sys
import time
from typing import Dict, Any, Literal

# Add the current directory to the path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Set up LangSmith environment FIRST
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_ENDPOINT"] = "https://api.smith.langchain.com"
os.environ["LANGCHAIN_API_KEY"] = "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"
os.environ["LANGCHAIN_PROJECT"] = "aura-agent-visualization"

from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from langsmith import Client, traceable
from langchain_core.tracers.langchain import LangChainTracer

class TestState(Dict[str, Any]):
    pass

# Simple node functions
@traceable(name="simple_start_node")
def start_node(state: TestState) -> TestState:
    print("🟢 Start node executing")
    state["step"] = "started"
    state["message"] = "Hello from start node"
    return state

@traceable(name="simple_end_node") 
def end_node(state: TestState) -> TestState:
    print("🔴 End node executing")
    state["step"] = "completed"
    state["final_message"] = f"Processed: {state.get('message', 'no message')}"
    return state

async def test_simple_langgraph():
    """Test simple LangGraph with LangSmith tracing"""
    print("🧪 Testing Simple LangGraph with LangSmith")
    print("=" * 50)
    
    try:
        # Check LangSmith connection
        client = Client()
        project_name = "aura-agent-visualization"
        
        initial_runs = list(client.list_runs(project_name=project_name, limit=3))
        print(f"📈 Initial runs: {len(initial_runs)}")
        
        # Build simple graph
        workflow = StateGraph(TestState)
        workflow.add_node("start", start_node)
        workflow.add_node("end", end_node)
        
        workflow.set_entry_point("start")
        workflow.add_edge("start", "end")
        workflow.add_edge("end", END)
        
        # Compile with memory and explicit LangSmith tracing
        memory = MemorySaver()
        graph = workflow.compile(checkpointer=memory)
        
        print("📊 Graph compiled successfully")
        
        # Prepare config with LangSmith tracer
        config = {
            "configurable": {"thread_id": "test-simple-graph"},
            "callbacks": []
        }
        
        # Add LangSmith tracer
        tracer = LangChainTracer(project_name=project_name)
        config["callbacks"] = [tracer]
        
        print("🚀 Executing simple graph...")
        
        # Execute graph
        initial_state = {"user_input": "test message"}
        result = await graph.ainvoke(initial_state, config=config)
        
        print(f"✅ Graph execution completed")
        print(f"📊 Result: {result}")
        
        # Wait for trace propagation
        print("⏳ Waiting for trace propagation...")
        await asyncio.sleep(3)
        
        # Check for new runs
        final_runs = list(client.list_runs(project_name=project_name, limit=5))
        new_runs = len(final_runs) - len(initial_runs)
        
        print(f"📈 Final runs: {len(final_runs)}")
        print(f"🆕 New runs: {new_runs}")
        
        if new_runs > 0:
            print("🎉 SUCCESS: Simple LangGraph created traces!")
            for run in final_runs[:2]:
                print(f"  - {run.name}: {run.status}")
        else:
            print("⚠️  No new traces detected")
        
        return result
        
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    result = asyncio.run(test_simple_langgraph())
    print(f"\n📋 Final result: {result}")
