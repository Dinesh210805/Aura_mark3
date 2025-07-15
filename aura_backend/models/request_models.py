from pydantic import BaseModel
from typing import Optional, List, Dict, Any

class ProcessRequest(BaseModel):
    """Request model for voice processing"""
    audio_data: Optional[str] = None  # base64 encoded
    screenshot_data: Optional[str] = None  # base64 encoded
    ui_tree: Optional[str] = None  # XML or JSON string
    session_id: Optional[str] = None

class UIElement(BaseModel):
    """UI element coordinates and metadata"""
    x: int
    y: int
    width: int
    height: int
    text: Optional[str] = None
    class_name: Optional[str] = None
    clickable: bool = True

class ActionStep(BaseModel):
    """Individual action step in a plan"""
    type: str  # "tap", "swipe", "type", "scroll", "speak", "open_app"
    x: Optional[int] = None
    y: Optional[int] = None
    text: Optional[str] = None
    description: str
    confidence: Optional[float] = None

class ProcessResponse(BaseModel):
    """Response model for processed requests"""
    success: bool
    transcript: Optional[str] = None
    intent: Optional[str] = None
    action_plan: List[ActionStep] = []
    tts_audio: Optional[str] = None  # base64 encoded
    response_text: Optional[str] = None
    error_message: Optional[str] = None
    session_id: Optional[str] = None
    processing_time: Optional[float] = None

class ChatRequest(BaseModel):
    """Text-only chat request"""
    text: str
    session_id: Optional[str] = None

class ChatResponse(BaseModel):
    """Text-only chat response"""
    success: bool
    response: str
    intent: Optional[str] = None
    session_id: Optional[str] = None
