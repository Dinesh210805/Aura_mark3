from pydantic import BaseModel
from typing import List, Dict, Any, Optional

class ActionPlan(BaseModel):
    """Detailed action plan with steps and metadata"""
    steps: List[Dict[str, Any]]
    confidence: float
    reasoning: str
    fallback_plan: Optional[List[Dict[str, Any]]] = None
    estimated_duration: Optional[float] = None

class GraphState(BaseModel):
    """LangGraph state model for conversation flow"""
    # Input data
    audio_data: Optional[bytes] = None
    screenshot_data: Optional[bytes] = None
    ui_tree: Optional[str] = None
    session_id: Optional[str] = None
    
    # Processing results
    transcript: Optional[str] = None
    intent: Optional[str] = None
    intent_data: Optional[Dict[str, Any]] = None
    ui_elements: List[Dict[str, Any]] = []
    ui_element_coords: Optional[Dict[str, Any]] = None
    action_plan: List[Dict[str, Any]] = []
    tts_audio: Optional[bytes] = None
    response_text: Optional[str] = None
    
    # Control flow
    use_vlm: bool = False
    vlm_confidence: Optional[float] = None
    element_description: Optional[str] = None
    error: Optional[str] = None
    complete: bool = False
    
    # Metadata
    processing_start_time: Optional[float] = None
    node_execution_times: Dict[str, float] = {}

class IntentClassification(BaseModel):
    """Intent classification result"""
    intent: str
    confidence: float
    target_elements: List[str] = []
    requires_screen_analysis: bool = False
    action_type: str = "tap"  # tap, swipe, type, navigate, open_app, system_command
    parameters: Dict[str, Any] = {}

class VLMResult(BaseModel):
    """Vision-Language Model analysis result"""
    found: bool
    coordinates: Optional[Dict[str, int]] = None
    confidence: float
    element_description: Optional[str] = None
    reasoning: Optional[str] = None
    alternative_elements: List[Dict[str, Any]] = []
