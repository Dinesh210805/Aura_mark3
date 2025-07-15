import logging
import time

logger = logging.getLogger(__name__)

class ActionPlannerNode:
    """Create detailed action plan based on analysis results"""
    
    def __init__(self):
        self.name = "action_planner"
    
    async def run(self, state: dict) -> dict:
        """Create detailed action plan based on VLM or UI tree results"""
        start_time = time.time()
        logger.info("Action Planner Node: Creating action plan")
        
        try:
            intent_data = state.get("intent_data", {})
            ui_coords = state.get("ui_element_coords")
            intent = state.get("intent", "Unknown action")
            
            # Create action plan based on available information
            if ui_coords:
                # Create plan based on VLM coordinates
                action_plan = self._create_coordinate_based_plan(intent_data, ui_coords, intent)
                logger.info("Action Planner Node: Created coordinate-based action plan")
            else:
                # Create plan based on intent only (fallback)
                action_plan = self._create_intent_based_plan(intent_data, intent)
                logger.info("Action Planner Node: Created intent-based fallback plan")
            
            logger.info(f"Action Planner Node: Action plan created with {len(action_plan)} steps")
            
            return {
                **state,
                "action_plan": action_plan,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
            
        except Exception as e:
            logger.error(f"Action Planner Node error: {str(e)}")
            return {
                **state,
                "error": f"Action planning failed: {str(e)}",
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
    
    def _create_coordinate_based_plan(self, intent_data: dict, coords: dict, intent: str) -> list:
        """Create action plan using specific coordinates from VLM"""
        action_type = intent_data.get("action_type", "tap")
        confidence = intent_data.get("confidence", 0.7)
        
        # Primary action with coordinates
        plan = [{
            "type": action_type,
            "x": coords.get("x"),
            "y": coords.get("y"),
            "width": coords.get("width"),
            "height": coords.get("height"),
            "description": f"Tap on UI element at ({coords.get('x')}, {coords.get('y')}) to {intent}",
            "confidence": confidence,
            "source": "vlm",
            "method": "coordinate_based"
        }]
        
        # Add text input if needed
        parameters = intent_data.get("parameters", {})
        if action_type == "type" and "text" in parameters:
            plan.append({
                "type": "type",
                "text": parameters["text"],
                "description": f"Type '{parameters['text']}' into the field",
                "confidence": confidence,
                "source": "intent"
            })
        
        return plan
    
    def _create_intent_based_plan(self, intent_data: dict, intent: str) -> list:
        """Create action plan based on intent analysis only (fallback)"""
        action_type = intent_data.get("action_type", "speak")
        confidence = intent_data.get("confidence", 0.3)
        
        if action_type == "speak" or not intent_data:
            # Provide helpful feedback when we can't execute the action
            return [{
                "type": "speak",
                "text": f"I understand you want to {intent}, but I need a clearer view of the current screen to help you.",
                "description": "Provide feedback to user about needing more information",
                "confidence": 0.9,
                "source": "fallback",
                "method": "intent_based"
            }]
        elif action_type == "open_app":
            # App opening doesn't require coordinates
            app_name = intent_data.get("parameters", {}).get("app_name", "")
            return [{
                "type": "open_app",
                "app_name": app_name,
                "description": f"Open {app_name or 'requested application'}",
                "confidence": confidence,
                "source": "intent",
                "method": "app_launch"
            }]
        elif action_type == "system_command":
            # System commands like volume, brightness, etc.
            command = intent_data.get("parameters", {}).get("command", "")
            return [{
                "type": "system_command",
                "command": command,
                "description": f"Execute system command: {command}",
                "confidence": confidence,
                "source": "intent",
                "method": "system_action"
            }]
        else:
            # Generic fallback with suggestion
            return [{
                "type": "speak",
                "text": f"I'll help you {intent}. Please make sure the relevant screen is visible and try again.",
                "description": f"Attempt to execute {intent} with guidance",
                "confidence": confidence,
                "source": "fallback",
                "method": "guided_retry",
                "fallback": True
            }]

# Create node instance
action_planner_node = ActionPlannerNode()
