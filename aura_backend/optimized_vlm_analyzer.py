#!/usr/bin/env python3
"""
Optimized VLM Prompt System for AURA
Enhanced UI element detection and screen analysis with faster, more accurate prompts
"""

import json
import logging
from typing import Dict, Any, Optional, List
from dataclasses import dataclass
from enum import Enum

logger = logging.getLogger(__name__)

class VLMTaskType(Enum):
    """VLM task types for optimized prompt selection"""
    ELEMENT_LOCATION = "element_location"    # Find specific UI element
    SCREEN_READING = "screen_reading"        # Read screen content
    UI_DESCRIPTION = "ui_description"        # Describe UI layout
    ELEMENT_ANALYSIS = "element_analysis"    # Analyze specific elements
    QUICK_SCAN = "quick_scan"               # Fast overview scan

@dataclass
class VLMPromptTemplate:
    """Template for VLM tasks"""
    task_type: VLMTaskType
    system_prompt: str
    max_tokens: int
    temperature: float

class OptimizedVLMAnalyzer:
    """Optimized VLM analyzer with task-specific prompts"""
    
    def __init__(self):
        self.prompt_templates = {
            VLMTaskType.ELEMENT_LOCATION: VLMPromptTemplate(
                task_type=VLMTaskType.ELEMENT_LOCATION,
                system_prompt="""AURA UI locator. Find element FAST.

ANALYZE screenshot, find matching element.

RETURN ONLY JSON (no explanations):
{
    "found": true,
    "coordinates": {"x": 123, "y": 456},
    "confidence": 0.95,
    "element_description": "blue Send button",
    "element_type": "button|text_field|image|icon",
    "reasoning": "Located blue button at bottom right"
}

If NOT found:
{
    "found": false,
    "confidence": 0.0,
    "reasoning": "No matching element visible"
}

BE PRECISE with coordinates.""",
                max_tokens=200,
                temperature=0.0
            ),
            
            VLMTaskType.SCREEN_READING: VLMPromptTemplate(
                task_type=VLMTaskType.SCREEN_READING,
                system_prompt="""AURA screen reader. Extract text content FAST.

READ screenshot, provide structured content.

RETURN JSON (no explanations):
{
    "screen_content": "All visible text in order",
    "app_name": "Current app",
    "screen_type": "chat|settings|home|list|form|browser",
    "main_elements": [{"type": "button", "text": "Send"}],
    "confidence": 0.9,
    "actionable_elements": ["Send button", "message field"]
}

FOCUS on INTERACTIVE text. Be COMPREHENSIVE but CONCISE.""",
                max_tokens=300,
                temperature=0.1
            ),
            
            VLMTaskType.UI_DESCRIPTION: VLMPromptTemplate(
                task_type=VLMTaskType.UI_DESCRIPTION,
                system_prompt="""You are AURA's UI analyzer. Describe the interface layout and navigation options.

ANALYZE the screenshot for navigation and interaction possibilities.

RETURN THIS JSON:
{
    "layout_description": "Brief overview of UI layout",
    "current_app": "App name",
    "screen_title": "Screen/page title",
    "navigation_options": ["back", "home", "menu", "settings"],
    "interaction_zones": [
        {"area": "top-bar", "elements": ["back button", "title"]},
        {"area": "main-content", "elements": ["message list", "input field"]},
        {"area": "bottom-bar", "elements": ["send button", "attachment"]}
    ],
    "scroll_available": true,
    "confidence": 0.85
}

FOCUS on HOW the user can navigate and interact. Be STRUCTURAL.""",
                max_tokens=400,
                temperature=0.1
            ),
            
            VLMTaskType.QUICK_SCAN: VLMPromptTemplate(
                task_type=VLMTaskType.QUICK_SCAN,
                system_prompt="""AURA quick scanner. Identify key elements FAST.

SCAN screenshot for PRIMARY interactive elements ONLY.

RETURN JSON (no explanations):
{
    "primary_buttons": ["Send", "Back", "Menu"],
    "text_fields": ["Message input"],
    "key_content": "Brief main content description",
    "app_context": "messaging|social|browser|settings|games",
    "quick_actions": ["tap send", "tap back"],
    "confidence": 0.8
}

BE FAST, identify ONLY most important interactive elements.""",
                max_tokens=120,
                temperature=0.0
            )
        }

    def select_vlm_task_type(self, intent: str, action_type: str) -> VLMTaskType:
        """Select optimal VLM task type based on intent"""
        intent_lower = intent.lower()
        action_lower = action_type.lower()
        
        # Element location for specific interactions
        if any(keyword in intent_lower for keyword in ['tap', 'click', 'press', 'button', 'find']):
            return VLMTaskType.ELEMENT_LOCATION
        
        # Screen reading for information requests
        elif any(keyword in intent_lower for keyword in ['read', 'what', 'tell me', 'describe', 'show']):
            if 'layout' in intent_lower or 'interface' in intent_lower:
                return VLMTaskType.UI_DESCRIPTION
            else:
                return VLMTaskType.SCREEN_READING
        
        # Quick scan for navigation and general actions
        elif action_lower in ['navigate', 'scroll', 'swipe']:
            return VLMTaskType.QUICK_SCAN
        
        # Default to element location for most interactions
        else:
            return VLMTaskType.ELEMENT_LOCATION

    def get_optimized_vlm_model(self, task_type: VLMTaskType) -> tuple[str, str]:
        """Select optimal VLM model for task type with enhanced speed focus"""
        if task_type == VLMTaskType.QUICK_SCAN:
            # Fastest model for quick scans - prioritize speed
            return "gemini", "gemini-2.5-flash-lite"
        elif task_type == VLMTaskType.ELEMENT_LOCATION:
            # Balance speed and accuracy for UI element detection
            return "gemini", "gemini-2.5-flash"
        elif task_type in [VLMTaskType.SCREEN_READING, VLMTaskType.UI_DESCRIPTION]:
            # Fast model for reading and description tasks
            return "gemini", "gemini-2.5-flash-lite"
        else:
            # Default to fast balanced model
            return "gemini", "gemini-2.5-flash"

    def build_vlm_prompt(self, intent: str, task_type: VLMTaskType, action_type: str = "") -> Dict[str, Any]:
        """Build optimized VLM prompt for specific task"""
        template = self.prompt_templates[task_type]
        
        # Build user prompt based on task type
        if task_type == VLMTaskType.ELEMENT_LOCATION:
            user_prompt = f"User wants to: {intent}\nAction: {action_type}\n\nFind the UI element for this action."
        elif task_type == VLMTaskType.SCREEN_READING:
            user_prompt = f"User wants to: {intent}\n\nRead all visible text and describe the screen content."
        elif task_type == VLMTaskType.UI_DESCRIPTION:
            user_prompt = f"User wants to: {intent}\n\nDescribe the UI layout and navigation options."
        elif task_type == VLMTaskType.QUICK_SCAN:
            user_prompt = f"User wants to: {intent}\n\nQuickly identify key interactive elements."
        else:
            user_prompt = f"User intent: {intent}\nAction: {action_type}"
        
        return {
            "system_prompt": template.system_prompt,
            "user_prompt": user_prompt,
            "max_tokens": template.max_tokens,
            "temperature": template.temperature
        }

    def validate_vlm_result(self, result: Dict[str, Any], task_type: VLMTaskType) -> Dict[str, Any]:
        """Validate and enhance VLM result based on task type"""
        if task_type == VLMTaskType.ELEMENT_LOCATION:
            # Ensure element location has required fields
            if result.get("found", False):
                coords = result.get("coordinates", {})
                if not all(key in coords for key in ["x", "y"]):
                    result["found"] = False
                    result["reasoning"] = "Invalid coordinates provided"
            
            # Ensure confidence is reasonable
            if "confidence" not in result:
                result["confidence"] = 0.5 if result.get("found") else 0.0
        
        elif task_type == VLMTaskType.SCREEN_READING:
            # Ensure screen reading has content
            if not result.get("screen_content"):
                result["screen_content"] = "No readable content detected"
            if "confidence" not in result:
                result["confidence"] = 0.6
        
        return result

    async def analyze_screenshot_optimized(
        self,
        screenshot_bytes: bytes,
        intent: str,
        action_type: str = "",
        vlm_service = None
    ) -> Dict[str, Any]:
        """
        Optimized screenshot analysis with task-specific prompts
        """
        if not screenshot_bytes:
            return {
                "found": False,
                "error": "No screenshot provided",
                "confidence": 0.0
            }
        
        try:
            # Select optimal task type and model
            task_type = self.select_vlm_task_type(intent, action_type)
            provider, model = self.get_optimized_vlm_model(task_type)
            
            # Build optimized prompt
            prompt_config = self.build_vlm_prompt(intent, task_type, action_type)
            
            logger.info(f"VLM Analysis: task={task_type.value}, model={provider}/{model}")
            
            # Call VLM service based on task type
            if task_type == VLMTaskType.ELEMENT_LOCATION and vlm_service:
                result = await vlm_service.locate_ui_element(
                    screenshot=screenshot_bytes,
                    intent=f"{intent} ({action_type})",
                    provider=provider,
                    model=model
                )
            elif vlm_service:
                # Use general screen analysis for other tasks
                result = await vlm_service.analyze_screen_context(
                    screenshot=screenshot_bytes,
                    provider=provider,
                    model=model,
                    custom_prompt=prompt_config["system_prompt"] + "\n\n" + prompt_config["user_prompt"]
                )
            else:
                return {"error": "No VLM service available", "found": False}
            
            # Validate and enhance result
            result = self.validate_vlm_result(result, task_type)
            
            # Add metadata
            result["_task_type"] = task_type.value
            result["_model_used"] = f"{provider}/{model}"
            
            logger.info(f"VLM Analysis complete: task={task_type.value}, "
                       f"found={result.get('found', False)}, "
                       f"confidence={result.get('confidence', 0):.2f}")
            
            return result
            
        except Exception as e:
            logger.error(f"VLM analysis error: {e}")
            return {
                "found": False,
                "error": f"VLM analysis failed: {str(e)}",
                "confidence": 0.0
            }

# Global instance
optimized_vlm_analyzer = OptimizedVLMAnalyzer()
