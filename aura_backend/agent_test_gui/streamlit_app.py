#!/usr/bin/env python3
"""
AURA Agent Testing GUI using Streamlit
Interactive testing interface for the AURA Android accessibility assistant
"""

import streamlit as st
import requests
import json
import base64
import io
import time
import os
import sys
from datetime import datetime
from pathlib import Path
import tempfile
from PIL import Image

# Add parent directory to sys.path to import AURA modules
sys.path.append(str(Path(__file__).parent.parent))

# Import local utilities
from audio_utils import create_sample_audio, validate_audio_file, get_audio_info, TEST_PHRASES
from screenshot_utils import create_sample_screenshot, SAMPLE_SCREENSHOTS, create_sample_screenshot_file

try:
    from aura_graph import aura_graph
    from models.request_models import ProcessResponse, ChatRequest
    BACKEND_AVAILABLE = True
except ImportError:
    BACKEND_AVAILABLE = False

# Configuration
BACKEND_URL = "http://localhost:8000"
SAMPLE_AUDIO_PATH = Path(__file__).parent / "sample_audio"
SAMPLE_IMAGES_PATH = Path(__file__).parent / "sample_screenshots"

def setup_page():
    """Configure Streamlit page"""
    st.set_page_config(
        page_title="AURA Agent Test GUI",
        page_icon="🎤",
        layout="wide",
        initial_sidebar_state="expanded"
    )
    
    st.title("🎤 AURA Agent Testing Interface")
    st.markdown("""
    **AURA** - *Autonomous UI Reader and Agent*  
    Test the intelligent Android accessibility assistant with voice commands and visual understanding.
    """)

def check_backend_health():
    """Check if the AURA backend is running"""
    try:
        response = requests.get(f"{BACKEND_URL}/health", timeout=5)
        if response.status_code == 200:
            return True, response.json()
        else:
            return False, f"Backend returned status {response.status_code}"
    except requests.exceptions.ConnectionError:
        return False, "Cannot connect to backend - is it running?"
    except Exception as e:
        return False, f"Health check failed: {str(e)}"

def create_sample_audio_file(text, duration=2.0):
    """Create a sample audio file for testing"""
    return create_sample_audio(text, duration)

def display_backend_status():
    """Display backend status in sidebar"""
    st.sidebar.header("🔧 Backend Status")
    
    is_healthy, status_info = check_backend_health()
    
    if is_healthy:
        st.sidebar.success("✅ Backend Online")
        if isinstance(status_info, dict):
            services = status_info.get("services", {})
            for service, status in services.items():
                icon = "✅" if status == "operational" else "❌"
                st.sidebar.text(f"{icon} {service.upper()}: {status}")
    else:
        st.sidebar.error("❌ Backend Offline")
        st.sidebar.text(status_info)
        st.sidebar.markdown("**Start the backend:**")
        st.sidebar.code("cd aura_backend && python run.py")

def text_chat_interface():
    """Text-only chat interface for quick testing"""
    st.header("💬 Text Chat Interface")
    st.markdown("Test AURA's intent understanding and responses without audio.")
    
    col1, col2 = st.columns([3, 1])
    
    with col1:
        user_input = st.text_input(
            "Enter your command:",
            placeholder="e.g., 'Open WhatsApp', 'What time is it?', 'Take a screenshot'"
        )
    
    with col2:
        session_id = st.text_input(
            "Session ID:",
            value="streamlit-test",
            help="Maintain conversation context"
        )
    
    # Provider selection for LLM
    with st.expander("🔧 Advanced Settings"):
        col1, col2 = st.columns(2)
        with col1:
            llm_provider = st.selectbox("LLM Provider", ["auto", "groq", "gemini"], index=0)
        with col2:
            llm_model = st.text_input("LLM Model (optional)", placeholder="e.g., llama-3.3-70b-versatile")
    
    if st.button("💬 Send Message", type="primary"):
        if user_input.strip():
            with st.spinner("🤔 AURA is thinking..."):
                try:
                    # Prepare request
                    payload = {
                        "text": user_input,
                        "session_id": session_id
                    }
                    
                    params = {}
                    if llm_provider != "auto":
                        params["llm_provider"] = llm_provider
                    if llm_model:
                        params["llm_model"] = llm_model
                    
                    # Send to backend
                    response = requests.post(
                        f"{BACKEND_URL}/chat",
                        json=payload,
                        params=params,
                        timeout=30
                    )
                    
                    if response.status_code == 200:
                        result = response.json()
                        
                        # Display results
                        if result.get("success"):
                            st.success("✅ Response received!")
                            
                            col1, col2 = st.columns(2)
                            with col1:
                                st.markdown("**AURA's Response:**")
                                st.info(result.get("response", "No response"))
                            
                            with col2:
                                st.markdown("**Detected Intent:**")
                                st.code(result.get("intent", "Unknown"))
                        else:
                            st.error("❌ Processing failed")
                    else:
                        st.error(f"❌ Backend error: {response.status_code}")
                        
                except Exception as e:
                    st.error(f"❌ Request failed: {str(e)}")
        else:
            st.warning("⚠️ Please enter a command")

def voice_processing_interface():
    """Voice + Screenshot processing interface"""
    st.header("🎤 Voice + Screenshot Processing")
    st.markdown("Test AURA's full capabilities with voice commands and visual understanding.")
    
    col1, col2 = st.columns(2)
    
    with col1:
        st.subheader("🎙️ Audio Input")
        
        # Audio upload
        audio_file = st.file_uploader(
            "Upload audio file",
            type=["wav", "mp3", "m4a"],
            help="Voice command audio file"
        )
        
        if not audio_file:
            # Sample audio generation
            st.markdown("**Or create sample audio:**")
            sample_text = st.selectbox(
                "Choose sample command:",
                TEST_PHRASES
            )
            if st.button("🔊 Create Sample Audio"):
                with st.spinner("Creating sample audio..."):
                    temp_audio = create_sample_audio_file(sample_text)
                    st.success(f"✅ Sample audio created")
                    st.audio(temp_audio)
                    
                    # Store in session state for processing
                    st.session_state['sample_audio_path'] = temp_audio
                    st.session_state['sample_audio_text'] = sample_text
    
    with col2:
        st.subheader("📱 Screenshot Input")
        
        # Screenshot upload
        screenshot_file = st.file_uploader(
            "Upload screenshot",
            type=["png", "jpg", "jpeg"],
            help="Android screen capture"
        )
        
        if screenshot_file:
            image = Image.open(screenshot_file)
            st.image(image, caption="Uploaded Screenshot", width=400)
        else:
            # Sample screenshot option
            st.markdown("**Sample Screenshots:**")
            selected_sample = st.selectbox("Choose sample screenshot:", ["None"] + list(SAMPLE_SCREENSHOTS.keys()))
            
            if selected_sample != "None":
                # Create sample screenshot
                sample_img = create_sample_screenshot(selected_sample)
                st.image(sample_img, caption=f"Sample: {selected_sample}", width=400)
                st.session_state['sample_screenshot'] = selected_sample
                st.session_state['sample_screenshot_img'] = sample_img
    
    # Advanced settings
    with st.expander("🔧 Advanced Processing Settings"):
        col1, col2, col3 = st.columns(3)
        
        with col1:
            st.markdown("**STT Settings**")
            stt_provider = st.selectbox("STT Provider", ["auto", "groq"], index=0)
            stt_model = st.text_input("STT Model", placeholder="whisper-large-v3-turbo")
        
        with col2:
            st.markdown("**VLM Settings**")
            vlm_provider = st.selectbox("VLM Provider", ["auto", "groq", "gemini"], index=0)
            vlm_model = st.text_input("VLM Model", placeholder="llama-vision-free")
        
        with col3:
            st.markdown("**TTS Settings**")
            tts_provider = st.selectbox("TTS Provider", ["auto", "groq"], index=0)
            tts_voice = st.text_input("TTS Voice", placeholder="alloy")
    
    # Process button
    session_id = st.text_input("Session ID:", value="streamlit-voice-test")
    
    if st.button("🚀 Process Voice Command", type="primary"):
        # Determine audio source
        audio_to_process = None
        if audio_file:
            audio_to_process = audio_file
        elif 'sample_audio_path' in st.session_state:
            audio_to_process = st.session_state['sample_audio_path']
        
        if audio_to_process:
            with st.spinner("🧠 AURA is processing your command..."):
                try:
                    # Prepare multipart form data
                    files = {}
                    
                    # Add audio
                    if isinstance(audio_to_process, str):  # Sample audio path
                        with open(audio_to_process, 'rb') as f:
                            audio_data = f.read()
                        files['audio'] = ('audio.wav', audio_data, 'audio/wav')
                    else:  # Uploaded file
                        files['audio'] = ('audio.wav', audio_to_process.read(), 'audio/wav')
                    
                    # Add screenshot if available
                    if screenshot_file:
                        screenshot_file.seek(0)  # Reset file pointer
                        files['screenshot'] = ('screenshot.png', screenshot_file.read(), 'image/png')
                    elif 'sample_screenshot_img' in st.session_state:
                        # Convert PIL image to bytes
                        img_bytes = io.BytesIO()
                        st.session_state['sample_screenshot_img'].save(img_bytes, format='PNG')
                        img_bytes.seek(0)
                        files['screenshot'] = ('screenshot.png', img_bytes.read(), 'image/png')
                    
                    # Form data
                    data = {'session_id': session_id}
                    
                    # Add provider preferences
                    if stt_provider != "auto":
                        data['stt_provider'] = stt_provider
                    if stt_model:
                        data['stt_model'] = stt_model
                    if vlm_provider != "auto":
                        data['vlm_provider'] = vlm_provider
                    if vlm_model:
                        data['vlm_model'] = vlm_model
                    if tts_provider != "auto":
                        data['tts_provider'] = tts_provider
                    if tts_voice:
                        data['tts_voice'] = tts_voice
                    
                    # Send request
                    start_time = time.time()
                    response = requests.post(
                        f"{BACKEND_URL}/process",
                        files=files,
                        data=data,
                        timeout=120  # Longer timeout for complex processing
                    )
                    processing_time = time.time() - start_time
                    
                    if response.status_code == 200:
                        result = response.json()
                        display_processing_results(result, processing_time)
                    else:
                        st.error(f"❌ Processing failed: {response.status_code}")
                        st.text(response.text)
                        
                except Exception as e:
                    st.error(f"❌ Processing error: {str(e)}")
        else:
            st.warning("⚠️ Please provide audio input")

def display_processing_results(result, processing_time):
    """Display the results from voice processing"""
    st.success(f"✅ Processing completed in {processing_time:.2f} seconds")
    
    # Main results
    col1, col2 = st.columns(2)
    
    with col1:
        st.markdown("### 📝 Transcript")
        transcript = result.get("transcript", "No transcript")
        st.info(transcript)
        
        st.markdown("### 🎯 Intent Analysis")
        intent = result.get("intent", "Unknown intent")
        st.code(intent)
    
    with col2:
        st.markdown("### 🗣️ AURA's Response")
        response_text = result.get("response_text", "No response")
        st.info(response_text)
        
        # TTS Audio if available
        if result.get("tts_audio"):
            st.markdown("### 🔊 TTS Audio")
            try:
                audio_data = base64.b64decode(result["tts_audio"])
                st.audio(audio_data, format="audio/wav")
            except Exception as e:
                st.warning(f"Could not decode TTS audio: {e}")
    
    # Action Plan
    action_plan = result.get("action_plan", [])
    if action_plan:
        st.markdown("### 🎯 Action Plan")
        
        for i, action in enumerate(action_plan, 1):
            with st.expander(f"Step {i}: {action.get('description', 'Unknown action')}"):
                col1, col2, col3 = st.columns(3)
                
                with col1:
                    st.markdown("**Action Type:**")
                    st.code(action.get("type", "unknown"))
                    
                    if action.get("confidence"):
                        st.markdown("**Confidence:**")
                        st.progress(action["confidence"])
                
                with col2:
                    if action.get("x") is not None and action.get("y") is not None:
                        st.markdown("**Coordinates:**")
                        st.code(f"({action['x']}, {action['y']})")
                    
                    if action.get("text"):
                        st.markdown("**Text:**")
                        st.code(action["text"])
                
                with col3:
                    if action.get("source"):
                        st.markdown("**Source:**")
                        st.code(action["source"])
                    
                    if action.get("method"):
                        st.markdown("**Method:**")
                        st.code(action["method"])
    
    # Raw JSON response
    with st.expander("🔍 Raw Response Data"):
        st.json(result)

def graph_visualization_interface():
    """LangGraph visualization and monitoring"""
    st.header("📊 Graph Visualization & Monitoring")
    st.markdown("Monitor AURA's processing pipeline and LangGraph execution.")
    
    col1, col2 = st.columns(2)
    
    with col1:
        st.subheader("🔗 Graph Structure")
        try:
            response = requests.get(f"{BACKEND_URL}/graph/info", timeout=10)
            if response.status_code == 200:
                graph_info = response.json()
                
                st.markdown("**Nodes:**")
                for node in graph_info.get("nodes", []):
                    st.markdown(f"• {node}")
                
                st.markdown("**Entry Point:**")
                st.code(graph_info.get("entry_point", "unknown"))
                
                st.markdown("**Conditional Routing:**")
                for source, targets in graph_info.get("conditional_edges", {}).items():
                    st.markdown(f"• **{source}** → {targets}")
            else:
                st.error("Could not fetch graph info")
        except Exception as e:
            st.error(f"Graph info error: {e}")
    
    with col2:
        st.subheader("📈 Session History")
        
        session_id = st.text_input("Session ID to inspect:", value="streamlit-test")
        
        if st.button("📋 Get History"):
            try:
                response = requests.get(f"{BACKEND_URL}/session/{session_id}/history", timeout=10)
                if response.status_code == 200:
                    history = response.json()
                    if history:
                        st.json(history)
                    else:
                        st.info("No history found for this session")
                else:
                    st.error(f"Could not fetch history: {response.status_code}")
            except Exception as e:
                st.error(f"History fetch error: {e}")
        
        if st.button("🗑️ Clear Session"):
            try:
                response = requests.delete(f"{BACKEND_URL}/session/{session_id}", timeout=10)
                if response.status_code == 200:
                    st.success("✅ Session cleared")
                else:
                    st.error(f"Could not clear session: {response.status_code}")
            except Exception as e:
                st.error(f"Clear session error: {e}")

def langsmith_monitoring_interface():
    """LangSmith tracing and monitoring"""
    st.header("🔬 LangSmith Monitoring")
    st.markdown("View traces, performance metrics, and debugging information.")
    
    try:
        # Check if LangSmith is available
        response = requests.get(f"{BACKEND_URL}/langsmith/status", timeout=10)
        if response.status_code == 200:
            langsmith_status = response.json()
            
            if langsmith_status.get("available"):
                st.success("✅ LangSmith tracing enabled")
                
                col1, col2 = st.columns(2)
                
                with col1:
                    st.subheader("📊 Recent Traces")
                    
                    hours = st.slider("Hours to look back:", 1, 24, 1)
                    
                    if st.button("🔍 Get Traces"):
                        try:
                            response = requests.get(
                                f"{BACKEND_URL}/langsmith/traces",
                                params={"hours": hours},
                                timeout=30
                            )
                            if response.status_code == 200:
                                traces = response.json()
                                
                                if traces.get("traces"):
                                    st.markdown(f"**Found {len(traces['traces'])} traces**")
                                    
                                    for trace in traces["traces"][:10]:  # Show first 10
                                        with st.expander(f"Trace: {trace.get('name', 'Unknown')}"):
                                            st.json(trace)
                                else:
                                    st.info("No traces found")
                            else:
                                st.error(f"Could not fetch traces: {response.status_code}")
                        except Exception as e:
                            st.error(f"Traces fetch error: {e}")
                
                with col2:
                    st.subheader("📈 Performance Report")
                    
                    if st.button("📊 Generate Report"):
                        try:
                            response = requests.get(
                                f"{BACKEND_URL}/langsmith/report",
                                params={"hours": 1},
                                timeout=30
                            )
                            if response.status_code == 200:
                                report = response.json()
                                
                                # Display metrics
                                if report.get("metrics"):
                                    metrics = report["metrics"]
                                    
                                    st.metric("Total Executions", metrics.get("total_executions", 0))
                                    st.metric("Average Duration", f"{metrics.get('avg_duration', 0):.2f}s")
                                    st.metric("Success Rate", f"{metrics.get('success_rate', 0):.1%}")
                                
                                # Node performance
                                if report.get("node_performance"):
                                    st.markdown("**Node Performance:**")
                                    for node, perf in report["node_performance"].items():
                                        st.markdown(f"• **{node}**: {perf:.2f}s avg")
                                
                            else:
                                st.error(f"Could not generate report: {response.status_code}")
                        except Exception as e:
                            st.error(f"Report error: {e}")
                
                # Dashboard link
                try:
                    dashboard_response = requests.get(f"{BACKEND_URL}/langsmith/dashboard", timeout=10)
                    if dashboard_response.status_code == 200:
                        dashboard_info = dashboard_response.json()
                        if dashboard_info.get("dashboard_url"):
                            st.markdown("---")
                            st.markdown(f"🔗 [Open LangSmith Dashboard]({dashboard_info['dashboard_url']})")
                except:
                    pass
                    
            else:
                st.warning("⚠️ LangSmith not configured")
                st.markdown("Set `LANGCHAIN_API_KEY` to enable tracing")
        else:
            st.error("Could not check LangSmith status")
    except Exception as e:
        st.error(f"LangSmith monitoring error: {e}")

def testing_scenarios():
    """Pre-defined testing scenarios"""
    st.header("🧪 Testing Scenarios")
    st.markdown("Quick tests with common AURA use cases.")
    
    scenarios = {
        "Simple Greeting": {
            "command": "Hello, how are you today?",
            "description": "Basic conversational response test",
            "requires_screenshot": False
        },
        "App Launch": {
            "command": "Open WhatsApp",
            "description": "Test app launching intent detection",
            "requires_screenshot": False
        },
        "UI Interaction": {
            "command": "Click the send button",
            "description": "Test UI element detection with screenshot",
            "requires_screenshot": True
        },
        "Text Input": {
            "command": "Type 'Hello world' in the text field",
            "description": "Test text input with VLM coordination",
            "requires_screenshot": True
        },
        "Complex Workflow": {
            "command": "Send an email to John saying I'll be late",
            "description": "Multi-step action planning test",
            "requires_screenshot": True
        },
        "System Command": {
            "command": "Turn on WiFi",
            "description": "System settings interaction",
            "requires_screenshot": False
        }
    }
    
    col1, col2 = st.columns([1, 2])
    
    with col1:
        st.subheader("📋 Available Scenarios")
        selected_scenario = st.selectbox("Choose a test scenario:", list(scenarios.keys()))
        
        scenario = scenarios[selected_scenario]
        st.markdown(f"**Description:** {scenario['description']}")
        st.markdown(f"**Requires Screenshot:** {'Yes' if scenario['requires_screenshot'] else 'No'}")
        
        if st.button("🚀 Run Scenario", type="primary"):
            st.session_state['run_scenario'] = selected_scenario
    
    with col2:
        st.subheader("📊 Test Results")
        
        if 'run_scenario' in st.session_state:
            scenario_name = st.session_state['run_scenario']
            scenario = scenarios[scenario_name]
            
            st.info(f"🧪 Running scenario: {scenario_name}")
            
            with st.spinner("Processing..."):
                try:
                    # Run as text chat for simplicity
                    payload = {
                        "text": scenario["command"],
                        "session_id": f"scenario-{scenario_name.lower().replace(' ', '-')}"
                    }
                    
                    response = requests.post(
                        f"{BACKEND_URL}/chat",
                        json=payload,
                        timeout=30
                    )
                    
                    if response.status_code == 200:
                        result = response.json()
                        
                        if result.get("success"):
                            st.success("✅ Scenario completed successfully!")
                            
                            col1_result, col2_result = st.columns(2)
                            with col1_result:
                                st.markdown("**Command:**")
                                st.code(scenario["command"])
                                
                                st.markdown("**Response:**")
                                st.info(result.get("response", "No response"))
                            
                            with col2_result:
                                st.markdown("**Detected Intent:**")
                                st.code(result.get("intent", "Unknown"))
                                
                                st.markdown("**Session:**")
                                st.code(result.get("session_id", "Unknown"))
                        else:
                            st.error("❌ Scenario failed")
                    else:
                        st.error(f"❌ Request failed: {response.status_code}")
                        
                except Exception as e:
                    st.error(f"❌ Scenario error: {str(e)}")
            
            # Clear the scenario from session state
            del st.session_state['run_scenario']

def main():
    """Main Streamlit application"""
    setup_page()
    
    # Display backend status
    display_backend_status()
    
    # Navigation tabs
    tab1, tab2, tab3, tab4, tab5, tab6 = st.tabs([
        "💬 Text Chat", 
        "🎤 Voice Processing", 
        "📊 Graph Monitoring",
        "🔬 LangSmith",
        "🧪 Test Scenarios",
        "📚 Documentation"
    ])
    
    with tab1:
        text_chat_interface()
    
    with tab2:
        voice_processing_interface()
    
    with tab3:
        graph_visualization_interface()
    
    with tab4:
        langsmith_monitoring_interface()
    
    with tab5:
        testing_scenarios()
    
    with tab6:
        st.header("📚 AURA Documentation")
        st.markdown("""
        ### 🎯 What is AURA?
        
        **AURA** (*Autonomous UI Reader and Agent*) is an intelligent Android accessibility assistant that combines:
        
        - **🎤 Voice Commands** - Natural language input via Groq Whisper STT
        - **👁️ Visual Understanding** - Screenshot analysis with Vision-Language Models
        - **🤖 Intent Analysis** - Smart command interpretation with LLM
        - **⚡ Action Execution** - Automated UI interactions and multi-step workflows
        - **🗣️ Voice Responses** - Natural TTS feedback via Groq/PlayAI
        
        ### 🔧 Architecture
        
        ```
        Voice Input → STT → Intent Analysis → UI Check → VLM Analysis → Action Planning → TTS Response
        ```
        
        ### 🚀 Key Features
        
        - **Multi-Provider AI**: Automatic model selection (Groq, Gemini)
        - **LangGraph Orchestration**: Intelligent workflow management
        - **Android Integration**: Direct accessibility service interaction
        - **Session Memory**: Persistent conversation context
        - **Visual Context**: Screenshot-based UI understanding
        
        ### 📖 Usage Examples
        
        | Command | Processing | Result |
        |---------|-----------|--------|
        | "Hello" | Text only | Greeting response |
        | "Open WhatsApp" | Intent + App launch | App opening |
        | "Click send button" | Screenshot + VLM | UI element tap |
        | "Send email to John" | Multi-step workflow | Email composition |
        
        ### 🛠️ Testing Tips
        
        1. **Start Simple**: Test with basic greetings first
        2. **Add Complexity**: Try app launches and UI interactions
        3. **Use Screenshots**: Upload Android screenshots for VLM testing
        4. **Monitor Performance**: Check LangSmith traces for debugging
        5. **Test Sessions**: Use consistent session IDs for conversation flow
        
        ### 🔍 Debugging
        
        - **Backend Offline**: Check if `python run.py` is running
        - **No Response**: Verify environment variables (GROQ_API_KEY)
        - **VLM Issues**: Ensure screenshots are clear Android UI captures
        - **Slow Processing**: Monitor node execution times in LangSmith
        
        ### 📡 API Endpoints
        
        - `POST /chat` - Text-only conversations
        - `POST /process` - Voice + screenshot processing
        - `GET /health` - Backend status check
        - `GET /docs` - Interactive API documentation
        """)

if __name__ == "__main__":
    main()
