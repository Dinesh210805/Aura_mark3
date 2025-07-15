from groq.llm import groq_llm
import logging
import time
import traceback
import json

logger = logging.getLogger(__name__)

class IntentNode:
    """Intent analysis node using Groq LLM"""
    
    def __init__(self):
        self.name = "intent"
    
    async def run(self, state: dict) -> dict:
        """Analyze user intent from transcript"""
        start_time = time.time()
        logger.info("🎯 Intent Node: Starting intent analysis")
        logger.info(f"🎯 Intent Node: Input state keys: {list(state.keys())}")
        logger.info(f"🎯 Intent Node: Transcript: '{state.get('transcript', 'None')}'")
        
        try:
            # Check if transcript is available
            transcript = state.get("transcript")
            if not transcript:
                logger.error("❌ Intent Node: No transcript available")
                return {
                    **state,
                    "error": "No transcript available for intent analysis",
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
                
            logger.info(f"🎯 Intent Node: Processing transcript: '{transcript}'")
            
            # Analyze intent using LLM
            intent_result = await groq_llm.analyze_intent(
                transcript, 
                state.get("ui_tree")
            )
            
            logger.info(f"🎯 Intent Node: LLM response: {json.dumps(intent_result, indent=2)}")
            
            # Check for API errors
            if "error" in intent_result:
                logger.error(f"❌ Intent Node: LLM error - {intent_result['error']}")
                return {
                    **state,
                    "error": intent_result["error"],
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
                
            # Extract intent information
            intent = intent_result.get("intent", "Unknown intent")
            requires_screen_analysis = intent_result.get("requires_screen_analysis", True)
            
            logger.info(f"✅ Intent Node: Intent analyzed - '{intent}'")
            logger.info(f"✅ Intent Node: Requires screen analysis - {requires_screen_analysis}")
            
            result_state = {
                **state,
                "intent": intent,
                "intent_data": intent_result,
                "use_vlm": requires_screen_analysis,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
            
            logger.info(f"✅ Intent Node: Output state keys: {list(result_state.keys())}")
            return result_state
            
        except Exception as e:
            error_trace = traceback.format_exc()
            logger.error(f"❌ Intent Node error: {str(e)}")
            logger.error(f"❌ Intent Node traceback: {error_trace}")
            return {
                **state,
                "error": f"Intent analysis failed: {str(e)}",
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }

# Global instance
intent_node = IntentNode()
# Create node instance
intent_node = IntentNode()
