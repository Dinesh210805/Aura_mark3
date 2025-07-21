# AURA Agent Test GUI - Quick Start Guide

## 🚀 What You've Created

A comprehensive Streamlit-based testing interface for your AURA Android accessibility assistant with these features:

### 💬 Text Chat Interface
- Quick testing with voice commands as text
- Real-time intent analysis
- Session management
- Provider selection (Groq, Gemini)

### 🎤 Voice Processing Interface  
- Audio file upload support
- Sample audio generation with preset commands
- Screenshot analysis testing
- Full LangGraph pipeline testing
- Advanced provider/model configuration

### 📊 Monitoring & Debugging
- LangGraph structure visualization
- Session history inspection
- LangSmith trace viewing
- Performance metrics
- Real-time processing insights

### 🧪 Testing Scenarios
- Pre-defined test cases for common AURA use cases
- Quick validation testing
- Automated scenario execution

## 📁 Files Created

```
agent_test_gui/
├── streamlit_app.py       # Main Streamlit application
├── audio_utils.py         # Audio file creation and validation
├── screenshot_utils.py    # Sample screenshot generation
├── launcher.py           # Python launcher with dependency installation
├── test_setup.py         # Pre-flight testing script
├── requirements.txt      # Python dependencies
├── launch.bat           # Windows batch launcher
├── launch.sh            # Linux/Mac shell launcher
└── README.md            # Comprehensive documentation
```

## 🛠️ How to Use

### Method 1: Python Launcher (Recommended)
```bash
cd agent_test_gui
python launcher.py
```

### Method 2: Manual Setup
```bash
cd agent_test_gui
pip install -r requirements.txt
streamlit run streamlit_app.py
```

### Method 3: Platform Scripts
```bash
# Windows
launch.bat

# Linux/Mac  
./launch.sh
```

## 🔧 Before Starting

1. **Start the AURA backend:**
   ```bash
   cd aura_backend
   python run.py
   ```

2. **Verify setup:**
   ```bash
   cd agent_test_gui
   python test_setup.py
   ```

3. **Open in browser:**
   ```
   http://localhost:8501
   ```

## 🧪 Testing Workflow

### 1. Basic Testing
- Check backend status in sidebar (should show green ✅)
- Use "Text Chat" tab with simple commands like "Hello"
- Verify intent detection and responses

### 2. Voice Processing  
- Switch to "Voice Processing" tab
- Upload audio files or create sample audio
- Test with screenshot uploads for VLM analysis
- Monitor action plan generation

### 3. Advanced Testing
- Use "Test Scenarios" for automated testing
- Check "Graph Monitoring" for session inspection
- Monitor "LangSmith" traces for debugging

## 🎯 Sample Test Commands

### Simple Commands
- "Hello, how are you today?"
- "What time is it?"
- "Tell me a joke"

### App Interactions
- "Open WhatsApp"
- "Launch Gmail"  
- "Go to settings"

### UI Actions (with screenshots)
- "Click the send button"
- "Tap the back arrow"
- "Press the menu icon"

### Complex Workflows
- "Send an email to John saying I'll be late"
- "Post a message on WhatsApp"
- "Turn on WiFi in settings"

## 🔍 Debugging Tips

### Backend Issues
- Red status in sidebar = backend offline
- Check if `python run.py` is running in aura_backend directory
- Verify GROQ_API_KEY in environment variables

### Processing Errors
- Check LangSmith traces for node failures
- Verify audio file formats (WAV preferred)
- Ensure screenshots are clear Android UI captures

### Performance Issues
- Monitor node execution times in LangSmith tab
- Use auto provider selection for optimization
- Check network connectivity to APIs

## 🌟 Key Features Tested

✅ **STT Processing** - Voice to text conversion  
✅ **Intent Analysis** - Command understanding  
✅ **VLM Analysis** - Screenshot understanding  
✅ **Action Planning** - Multi-step automation  
✅ **TTS Generation** - Voice responses  
✅ **Session Management** - Conversation continuity  
✅ **Provider Selection** - Auto/manual model selection  
✅ **Error Handling** - Robust error recovery  

Your AURA agent testing GUI is now ready! 🎉

## 📞 Next Steps

1. Start the backend: `cd ../; python run.py`
2. Launch the GUI: `python launcher.py`  
3. Open browser: `http://localhost:8501`
4. Start testing with simple text commands
5. Progress to voice + screenshot testing
6. Monitor performance with LangSmith
7. Use for development and debugging

Happy testing! 🚀
