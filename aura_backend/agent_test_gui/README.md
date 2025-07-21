# AURA Agent Test GUI

A comprehensive Streamlit-based testing interface for the AURA Android accessibility assistant.

## Features

### 💬 Text Chat Interface
- Quick testing with text-only commands
- Real-time intent analysis
- Session management
- Provider selection (Groq, Gemini)

### 🎤 Voice Processing Interface
- Audio file upload support
- Screenshot analysis testing
- Full LangGraph pipeline testing
- Advanced provider/model configuration

### 📊 Graph Monitoring
- LangGraph structure visualization
- Session history inspection
- Real-time processing insights

### 🔬 LangSmith Integration
- Trace visualization
- Performance metrics
- Debugging support
- Dashboard links

### 🧪 Testing Scenarios
- Pre-defined test cases
- Common AURA use cases
- Quick validation testing

## Setup

1. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

2. **Start the AURA backend:**
   ```bash
   cd ../
   python run.py
   ```

3. **Launch the test GUI:**
   ```bash
   streamlit run streamlit_app.py
   ```

4. **Open in browser:**
   ```
   http://localhost:8501
   ```

## Usage

### Quick Testing
1. Check backend status in sidebar
2. Use "Text Chat" tab for basic testing
3. Enter commands like "Hello" or "Open WhatsApp"

### Advanced Testing
1. Switch to "Voice Processing" tab
2. Upload audio files or create samples
3. Add screenshots for VLM testing
4. Configure provider preferences

### Monitoring
1. Use "Graph Monitoring" for session inspection
2. Check "LangSmith" tab for detailed traces
3. Monitor performance and debug issues

## Test Scenarios

### Basic Commands
- "Hello, how are you?"
- "What time is it?"
- "Tell me a joke"

### App Interactions
- "Open WhatsApp"
- "Launch Gmail"
- "Go to settings"

### UI Actions (requires screenshots)
- "Click the send button"
- "Tap the back arrow"
- "Press the menu icon"

### Complex Workflows
- "Send an email to John"
- "Post a message on WhatsApp"
- "Turn on WiFi in settings"

## Debugging

### Backend Issues
- Red status in sidebar = backend offline
- Check if `python run.py` is running
- Verify environment variables

### Processing Errors
- Check LangSmith traces for node failures
- Verify audio file formats (WAV preferred)
- Ensure screenshots are clear Android UI

### Performance Issues
- Monitor node execution times
- Use auto provider selection for optimization
- Check network connectivity

## API Testing

The GUI provides a visual interface for these API endpoints:

- `POST /chat` - Text conversations
- `POST /process` - Voice + screenshot processing
- `GET /health` - Backend health check
- `GET /graph/info` - Graph structure
- `GET /session/{id}/history` - Session history

## Files

- `streamlit_app.py` - Main Streamlit application
- `requirements.txt` - Python dependencies
- `README.md` - This documentation

## Tips

1. **Start Simple**: Use text chat before voice processing
2. **Check Health**: Always verify backend status first
3. **Use Sessions**: Maintain session IDs for conversation context
4. **Monitor Traces**: Use LangSmith for debugging complex issues
5. **Test Incrementally**: Start with basic commands, add complexity gradually
