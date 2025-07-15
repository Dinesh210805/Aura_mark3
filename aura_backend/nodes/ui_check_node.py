import logging
import xml.etree.ElementTree as ET
import json
import time

logger = logging.getLogger(__name__)

class UICheckNode:
    """Check if UI tree has required elements for direct action"""
    
    def __init__(self):
        self.name = "ui_check"
    
    async def run(self, state: dict) -> dict:
        """Check UI tree for target elements and create action plan if possible"""
        start_time = time.time()
        logger.info("UI Check Node: Starting UI tree analysis")
        
        ui_tree = state.get("ui_tree")
        intent_data = state.get("intent_data", {})
        requires_screen_analysis = intent_data.get("requires_screen_analysis", True)
        
        # If intent doesn't require screen analysis, create simple action plan
        if not requires_screen_analysis:
            logger.info("UI Check Node: Intent doesn't require screen analysis, creating simple action plan")
            action_plan = self._create_simple_action_plan(intent_data)
            return {
                **state,
                "action_plan": action_plan,
                "use_vlm": False,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
        
        # If no UI tree available, fall back to VLM
        if not ui_tree:
            logger.info("UI Check Node: No UI tree available, will use VLM")
            return {
                **state,
                "use_vlm": True,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
            
        try:
            # Parse UI tree and look for target elements
            target_elements = intent_data.get("target_elements", [])
            
            if target_elements and self._find_elements_in_tree(ui_tree, target_elements):
                # Create action plan from UI tree
                action_plan = self._create_action_plan_from_tree(ui_tree, intent_data)
                logger.info("UI Check Node: Found elements in UI tree, created action plan")
                return {
                    **state,
                    "action_plan": action_plan,
                    "use_vlm": False,
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
            else:
                logger.info("UI Check Node: Target elements not found in UI tree, will use VLM")
                return {
                    **state,
                    "use_vlm": True,
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
                
        except Exception as e:
            logger.error(f"UI Check Node error: {str(e)}")
            # Fallback to VLM on any error
            return {
                **state,
                "use_vlm": True,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
    
    def _find_elements_in_tree(self, ui_tree: str, target_elements: list) -> bool:
        """Search for target elements in UI tree"""
        try:
            tree_text_lower = ui_tree.lower()
            
            # Look for any of the target elements
            for element in target_elements:
                if element.lower() in tree_text_lower:
                    logger.info(f"UI Check Node: Found target element '{element}' in UI tree")
                    return True
                    
            # Also check for common UI patterns
            common_patterns = ["button", "click", "tap", "edit", "input", "text"]
            intent_lower = str(target_elements).lower()
            
            for pattern in common_patterns:
                if pattern in intent_lower and pattern in tree_text_lower:
                    logger.info(f"UI Check Node: Found common pattern '{pattern}' in UI tree")
                    return True
                    
            return False
            
        except Exception as e:
            logger.error(f"UI tree search error: {str(e)}")
            return False
    
    def _create_simple_action_plan(self, intent_data: dict) -> list:
        """Create simple action plan for non-screen actions"""
        try:
            action_type = intent_data.get("action_type", "system_command")
            intent = intent_data.get("intent", "Execute action")
            confidence = intent_data.get("confidence", 0.9)
            
            # Create simple action plan for system commands, information requests, etc.
            action_plan = [{
                "type": action_type,
                "description": f"Execute: {intent}",
                "method": "system_action",
                "confidence": confidence,
                "source": "intent",
                "requires_screen": False
            }]
            
            logger.info(f"UI Check Node: Created simple action plan for '{intent}'")
            return action_plan
            
        except Exception as e:
            logger.error(f"Simple action plan creation error: {str(e)}")
            return []

    def _create_action_plan_from_tree(self, ui_tree: str, intent_data: dict) -> list:
        """Create action plan based on UI tree analysis"""
        try:
            action_type = intent_data.get("action_type", "tap")
            intent = intent_data.get("intent", "Execute action")
            
            # Create basic action plan
            action_plan = [{
                "type": action_type,
                "description": f"Execute {intent} using UI tree information",
                "method": "ui_tree_based",
                "confidence": 0.8,
                "source": "ui_tree"
            }]
            
            # Add parameters if available
            parameters = intent_data.get("parameters", {})
            if parameters:
                action_plan[0]["parameters"] = parameters
            
            return action_plan
            
        except Exception as e:
            logger.error(f"Action plan creation error: {str(e)}")
            return []

# Create node instance
ui_check_node = UICheckNode()
