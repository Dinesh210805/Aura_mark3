from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from langsmith import Client, traceable
from langchain_core.tracers.langchain import LangChainTracer
from typing import Dict, Any, Literal
import logging
import time
import traceback
import json
import os

from nodes import (
    stt_node, intent_node, ui_check_node, 
    vlm_node, action_planner_node, tts_node
)

logger = logging.getLogger(__name__)

class AuraGraph:
    """LangGraph orchestrator for AURA voice assistant with LangSmith tracing"""
    
    def __init__(self):
        # Initialize LangSmith if configured
        self._setup_langsmith()
        self.graph = self._build_graph()
        logger.info("AURA Graph initialized with LangGraph orchestration and LangSmith tracing")
        
    def _setup_langsmith(self):
        """Setup LangSmith tracing if API key is provided"""
        try:
            langsmith_key = os.getenv("LANGCHAIN_API_KEY")
            if langsmith_key and langsmith_key != "your_langsmith_api_key_here":
                # Set environment variables for automatic tracing FIRST
                os.environ["LANGCHAIN_TRACING_V2"] = "true"
                os.environ["LANGCHAIN_ENDPOINT"] = "https://api.smith.langchain.com"
                os.environ["LANGCHAIN_API_KEY"] = langsmith_key
                os.environ["LANGCHAIN_PROJECT"] = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
                
                # Initialize client after setting environment
                self.langsmith_client = Client(api_key=langsmith_key)
                self.project_name = os.environ["LANGCHAIN_PROJECT"]
                
                logger.info(f"LangSmith tracing enabled for project: {self.project_name}")
                logger.info("Environment variables set for automatic LangGraph tracing")
            else:
                logger.info("LangSmith API key not configured - tracing disabled")
                self.langsmith_client = None
                self.project_name = None
        except Exception as e:
            logger.warning(f"Failed to setup LangSmith: {e}")
            self.langsmith_client = None
            self.project_name = None
        
    def _build_graph(self):
        """Build the LangGraph workflow"""
        
        # Create state graph with proper typing
        workflow = StateGraph(dict)
        
        # Add all nodes
        workflow.add_node("stt", stt_node.run)
        workflow.add_node("intent", intent_node.run)
        workflow.add_node("ui_check", ui_check_node.run)
        workflow.add_node("vlm", vlm_node.run)
        workflow.add_node("action_planner", action_planner_node.run)
        workflow.add_node("tts", tts_node.run)
        
        # Set entry point
        workflow.set_entry_point("stt")
        
        # Add sequential edges
        workflow.add_edge("stt", "intent")
        workflow.add_edge("intent", "ui_check")
        
        # Add conditional routing from ui_check
        workflow.add_conditional_edges(
            "ui_check",
            self._route_after_ui_check,
            {
                "use_vlm": "vlm",
                "has_action_plan": "tts",
                "error": "tts"
            }
        )
        
        # VLM path
        workflow.add_edge("vlm", "action_planner")
        workflow.add_edge("action_planner", "tts")
        
        # Final edge
        workflow.add_edge("tts", END)
        
        # Add memory for conversation state
        memory = MemorySaver()
        
        # Compile with memory and automatic LangSmith tracing
        # The environment variables set in _setup_langsmith() enable automatic tracing
        compiled_graph = workflow.compile(checkpointer=memory)
        
        if self.langsmith_client:
            logger.info("Graph compiled with LangSmith automatic tracing enabled")
        else:
            logger.info("Graph compiled without LangSmith tracing")
            
        return compiled_graph
    
    def _route_after_ui_check(self, state: Dict[str, Any]) -> Literal["use_vlm", "has_action_plan", "error"]:
        """Determine next step after UI check"""
        
        logger.info(f"🔀 Routing: UI check completed, analyzing state...")
        logger.info(f"🔀 Routing: State keys: {list(state.keys())}")
        logger.info(f"🔀 Routing: Error: {state.get('error')}")
        logger.info(f"🔀 Routing: Action plan: {state.get('action_plan')}")
        logger.info(f"🔀 Routing: Use VLM: {state.get('use_vlm', False)}")
        
        # Check for errors first
        if state.get("error"):
            logger.info("🔀 Routing: Error detected, going to TTS")
            return "error"
        
        # Check if we already have an action plan from UI tree
        if state.get("action_plan"):
            logger.info("🔀 Routing: Action plan available from UI tree, going to TTS")
            return "has_action_plan"
        
        # Check if we should use VLM
        if state.get("use_vlm", False):
            logger.info("🔀 Routing: UI check requires VLM analysis")
            return "use_vlm"
        
        # Default to VLM if uncertain
        logger.info("🔀 Routing: Default path - using VLM")
        return "use_vlm"
    
    @traceable(name="aura_graph_process")
    async def process(self, state: Dict[str, Any], session_id: str = "default") -> Dict[str, Any]:
        """Process a request through the LangGraph"""
        start_time = time.time()
        
        # Ensure session_id is a valid UUID for LangGraph checkpointer
        import uuid
        try:
            # Validate if it's already a valid UUID
            uuid.UUID(session_id)
            graph_session_id = session_id
        except ValueError:
            # Generate a valid UUID if the session_id is not UUID format
            graph_session_id = str(uuid.uuid4())
            logger.info(f"🔄 Generated UUID for graph session: {session_id} -> {graph_session_id}")
        
        try:
            # Add processing metadata
            state["processing_start_time"] = start_time
            state["session_id"] = session_id  # Keep original session_id in state
            state["graph_session_id"] = graph_session_id  # Store UUID for graph
            state["node_execution_times"] = {}
            
            # Configure session for LangGraph checkpointer
            config = {
                "configurable": {"thread_id": graph_session_id},  # Use UUID for checkpointer
            }
            
            # The LangSmith tracing is handled automatically via environment variables
            # No need to manually add LangChainTracer as callback when LANGCHAIN_TRACING_V2=true
            
            logger.info(f"🚀 Graph: Starting processing for session {session_id}")
            logger.info(f"🚀 Graph: Initial state keys: {list(state.keys())}")
            
            # Clean state for logging (exclude bytes data)
            clean_state = {k: str(v)[:100] if isinstance(v, str) else v 
                          for k, v in state.items() 
                          if k not in ['_audio_bytes', '_screenshot_bytes']}
            logger.info(f"🚀 Graph: Initial state: {json.dumps(clean_state, indent=2)}")
            
            # Execute the graph with tracing
            logger.info("🚀 Graph: Executing LangGraph workflow...")
            result = await self.graph.ainvoke(state, config=config)
            
            # Clean up temporary bytes storage from final result
            if "_audio_bytes" in result:
                del result["_audio_bytes"]
            if "_screenshot_bytes" in result:
                del result["_screenshot_bytes"]
            
            # Add total processing time
            total_time = time.time() - start_time
            result["total_processing_time"] = total_time
            
            logger.info(f"✅ Graph: Processing completed for session {session_id} in {total_time:.2f}s")
            logger.info(f"✅ Graph: Final result keys: {list(result.keys())}")
            logger.info(f"✅ Graph: Response text: '{result.get('response_text', 'None')}'")
            logger.info(f"✅ Graph: Intent: '{result.get('intent', 'None')}'")
            
            return result
            
        except Exception as e:
            error_time = time.time() - start_time
            error_trace = traceback.format_exc()
            logger.error(f"❌ Graph: Processing error for session {session_id}: {str(e)}")
            logger.error(f"❌ Graph: Error traceback: {error_trace}")
            
            return {
                **state,
                "error": f"Graph processing failed: {str(e)}",
                "complete": True,
                "total_processing_time": error_time,
                "success": False
            }
    
    async def get_conversation_history(self, session_id: str) -> Dict[str, Any]:
        """Get conversation history for a session"""
        try:
            config = {"configurable": {"thread_id": session_id}}
            # This would require implementing history retrieval from checkpointer
            # For now, return empty history
            return {"session_id": session_id, "history": []}
        except Exception as e:
            logger.error(f"History retrieval error: {str(e)}")
            return {"error": str(e)}
    
    def get_graph_info(self) -> Dict[str, Any]:
        """Get information about the graph structure"""
        return {
            "nodes": ["stt", "intent", "ui_check", "vlm", "action_planner", "tts"],
            "entry_point": "stt",
            "conditional_edges": {
                "ui_check": ["use_vlm", "has_action_plan", "error"]
            },
            "description": "AURA voice assistant processing pipeline"
        }

# Global instance
aura_graph = AuraGraph()
