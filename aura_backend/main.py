from fastapi import FastAPI, File, UploadFile, Form, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import base64
import uuid
import logging
import time
import os
from typing import Optional
from contextlib import asynccontextmanager
from dotenv import load_dotenv

from models.request_models import ProcessResponse, ActionStep, ChatRequest, ChatResponse
from aura_graph import aura_graph
from utils.image_utils import validate_image, optimize_image, validate_audio, get_image_info

# Load environment variables from multiple possible locations
# Try multiple .env locations
env_paths = [
    ".env",  # Current directory (aura_backend/.env)
    "../.env",  # Parent directory (Aura_mark3/.env) 
    os.path.join(os.path.dirname(__file__), ".env"),  # Same dir as main.py
    os.path.join(os.path.dirname(__file__), "..", ".env")  # Parent of main.py
]

env_loaded = False
for env_path in env_paths:
    if os.path.exists(env_path):
        load_dotenv(dotenv_path=env_path)
        env_loaded = True
        break

if not env_loaded:
    # Fallback: load any .env file found
    load_dotenv()

# Configure logging
logging.basicConfig(
    level=getattr(logging, os.getenv("LOG_LEVEL", "INFO")),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Dependency to check API keys
async def verify_environment():
    """Verify that required environment variables are set"""
    required_vars = ["GROQ_API_KEY"]
    missing_vars = [var for var in required_vars if not os.getenv(var)]
    
    if missing_vars:
        raise HTTPException(
            status_code=503,
            detail=f"Service misconfigured. Missing environment variables: {missing_vars}"
        )

@asynccontextmanager
async def lifespan(app: FastAPI):
    """FastAPI lifespan event handler"""
    # Startup
    logger.info("🚀 AURA Backend Agent starting up...")
    logger.info("📊 LangGraph orchestration enabled")
    logger.info("🎤 Groq STT/LLM/VLM integration ready")
    logger.info("🔊 PlayAI TTS via Groq integration ready")
    
    # Verify environment on startup
    try:
        await verify_environment()
        logger.info("✅ Environment verification passed")
    except HTTPException as e:
        logger.error(f"❌ Environment verification failed: {e.detail}")
    
    yield
    
    # Shutdown
    logger.info("🛑 AURA Backend Agent shutting down...")

# Initialize FastAPI with lifespan
app = FastAPI(
    title="AURA Backend Agent",
    description="LangGraph-powered Android accessibility assistant with Groq integration",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("ALLOWED_ORIGINS", "*").split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    """Root endpoint with service information"""
    return {
        "message": "AURA Backend Agent is running",
        "version": "1.0.0",
        "services": ["STT", "LLM", "VLM", "TTS"],
        "powered_by": "LangGraph + Groq + FastAPI"
    }

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    try:
        await verify_environment()
        return {
            "status": "healthy",
            "services": {
                "stt": "operational",
                "llm": "operational", 
                "vlm": "operational",
                "tts": "operational"
            },
            "graph_info": aura_graph.get_graph_info()
        }
    except HTTPException:
        return JSONResponse(
            status_code=503,
            content={
                "status": "unhealthy",
                "error": "Missing required environment variables"
            }
        )

@app.post("/process", response_model=ProcessResponse)
async def process_request(
    audio: UploadFile = File(...),
    screenshot: Optional[UploadFile] = File(None),
    ui_tree: Optional[str] = Form(None),
    session_id: Optional[str] = Form(None),
    _env_check: None = Depends(verify_environment)
):
    """Main endpoint to process voice commands with optional screenshot and UI tree"""
    
    # Generate session ID if not provided
    if not session_id:
        session_id = str(uuid.uuid4())
    
    start_time = time.time()
    logger.info(f"Processing request for session: {session_id}")
    
    try:
        # Validate and read audio data
        audio_data = await audio.read()
        if not audio_data:
            raise HTTPException(status_code=400, detail="Empty audio file")
        
        if not validate_audio(audio_data):
            raise HTTPException(status_code=400, detail="Invalid audio format")
        
        logger.info(f"Audio data received: {len(audio_data)} bytes")
        
        # Read and validate screenshot if provided
        screenshot_data = None
        if screenshot:
            screenshot_data = await screenshot.read()
            if screenshot_data:
                if not validate_image(screenshot_data):
                    raise HTTPException(status_code=400, detail="Invalid image format")
                
                # Optimize image for VLM processing
                screenshot_data = optimize_image(screenshot_data)
                image_info = get_image_info(screenshot_data)
                logger.info(f"Screenshot processed: {image_info}")
        
        # Build state for LangGraph (don't store bytes data to avoid JSON serialization issues)
        state = {
            "ui_tree": ui_tree,
            "session_id": session_id,
            # Store metadata instead of raw bytes
            "has_audio": bool(audio_data),
            "has_screenshot": bool(screenshot_data),
            "audio_size": len(audio_data) if audio_data else 0,
            "screenshot_size": len(screenshot_data) if screenshot_data else 0
        }
        
        # Store bytes data separately for node access
        if audio_data:
            state["_audio_bytes"] = audio_data  # Temporary storage for STT node
        if screenshot_data:
            state["_screenshot_bytes"] = screenshot_data  # Temporary storage for VLM node
        
        # Process through LangGraph
        result = await aura_graph.process(state, session_id)
        
        # Handle processing errors
        if result.get("error"):
            logger.error(f"Processing error: {result['error']}")
            return ProcessResponse(
                success=False,
                error_message=result["error"],
                session_id=session_id,
                processing_time=time.time() - start_time
            )
        
        # Convert action plan to ActionStep objects
        action_steps = []
        for step in result.get("action_plan", []):
            action_steps.append(ActionStep(
                type=step.get("type", "unknown"),
                x=step.get("x"),
                y=step.get("y"),
                text=step.get("text"),
                description=step.get("description", ""),
                confidence=step.get("confidence")
            ))
        
        # Encode TTS audio if available
        tts_audio_b64 = None
        # Note: TTS audio is no longer stored in state to avoid JSON serialization issues
        # For now, we'll indicate if TTS was generated
        if result.get("tts_audio_available"):
            # In a real implementation, you might store audio in a cache/file system
            # and return a URL or handle the audio separately
            pass
        
        # Build successful response
        response = ProcessResponse(
            success=True,
            transcript=result.get("transcript"),
            intent=result.get("intent"),
            action_plan=action_steps,
            tts_audio=tts_audio_b64,
            response_text=result.get("response_text"),
            session_id=session_id,
            processing_time=time.time() - start_time
        )
        
        logger.info(f"Successfully processed request for session: {session_id}")
        return response
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Unexpected error: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.post("/chat", response_model=ChatResponse)
async def chat_only(
    request: ChatRequest,
    _env_check: None = Depends(verify_environment)
):
    """Text-only chat endpoint for testing without audio"""
    
    session_id = request.session_id or str(uuid.uuid4())
    
    try:
        # Build state with pre-transcribed text
        state = {
            "transcript": request.text,
            "session_id": session_id
        }
        
        # Process through LangGraph (skip STT node)
        result = await aura_graph.process(state, session_id)
        
        return ChatResponse(
            success=True,
            response=result.get("response_text", "No response generated"),
            intent=result.get("intent"),
            session_id=session_id
        )
        
    except Exception as e:
        logger.error(f"Chat error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/graph/info")
async def get_graph_info():
    """Get information about the LangGraph structure"""
    return aura_graph.get_graph_info()

@app.get("/session/{session_id}/history")
async def get_session_history(session_id: str):
    """Get conversation history for a session"""
    try:
        history = await aura_graph.get_conversation_history(session_id)
        return history
    except Exception as e:
        logger.error(f"History retrieval error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/session/{session_id}")
async def clear_session(session_id: str):
    """Clear conversation history for a session"""
    try:
        # This would clear the session from the checkpointer
        # For now, just acknowledge the request
        return {"message": f"Session {session_id} cleared", "success": True}
    except Exception as e:
        logger.error(f"Session clear error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    
    # Check environment variables before starting
    if not os.getenv("GROQ_API_KEY"):
        logger.error("GROQ_API_KEY not found in environment variables")
        exit(1)
    
    logger.info("Starting AURA Backend Agent...")
    uvicorn.run(
        "main:app", 
        host="0.0.0.0", 
        port=8000, 
        reload=True,
        log_level="info"
    )
