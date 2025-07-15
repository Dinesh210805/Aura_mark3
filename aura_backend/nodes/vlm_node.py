from groq.vlm import groq_vlm
import logging
import time

logger = logging.getLogger(__name__)

class VLMNode:
    """Vision-Language Model node for UI element detection"""
    
    def __init__(self):
        self.name = "vlm"
    
    async def run(self, state: dict) -> dict:
        """Use VLM to locate UI elements on screen"""
        start_time = time.time()
        logger.info("VLM Node: Starting vision-language model analysis")
        
        screenshot = state.get("_screenshot_bytes")
        intent = state.get("intent", "")
        
        # Check if screenshot is available
        if not screenshot:
            logger.error("VLM Node: No screenshot data available")
            return {
                **state,
                "error": "No screenshot data available for VLM analysis",
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
            
        try:
            # Use VLM to locate UI elements
            vlm_result = await groq_vlm.locate_ui_element(screenshot, intent)
            
            # Check if VLM found the element
            if vlm_result.get("found"):
                logger.info(f"VLM Node: Successfully located UI element - {vlm_result.get('element_description', 'Unknown')}")
                
                return {
                    **state,
                    "ui_element_coords": vlm_result.get("coordinates"),
                    "vlm_confidence": vlm_result.get("confidence", 0.5),
                    "element_description": vlm_result.get("element_description"),
                    "vlm_reasoning": vlm_result.get("reasoning"),
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
            else:
                logger.warning("VLM Node: Could not locate UI element")
                error_msg = vlm_result.get("error", "UI element not found by VLM")
                
                return {
                    **state,
                    "error": error_msg,
                    "vlm_attempted": True,
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
                
        except Exception as e:
            logger.error(f"VLM Node error: {str(e)}")
            return {
                **state,
                "error": f"VLM processing failed: {str(e)}",
                "vlm_attempted": True,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }

# Create node instance
vlm_node = VLMNode()
