# AURA Backend Agent

## Setup Instructions

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Configure Environment Variables
Copy `.env.example` to `.env` and fill in your API keys:
```bash
cp .env.example .env
```

Edit `.env`:
```
GROQ_API_KEY=your_groq_api_key_here
# PlayAI TTS is accessed through Groq API using playai-tts model
```

### 3. Run the Backend
```bash
python run.py
```

The server will start at `http://localhost:8000`

## API Endpoints

### Process Voice Request
**POST** `/process`
- Upload audio file and optional screenshot
- Returns action plan and TTS audio

### Text Chat
**POST** `/chat`
- Send text directly (for testing)
- Returns text response

### Health Check
**GET** `/health`
- Check service status

### API Documentation
- Interactive docs: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

## Testing

### Using curl
```bash
# Process voice + screenshot
curl -X POST http://localhost:8000/process \
  -F "audio=@sample.wav" \
  -F "screenshot=@screen.png" \
  -F "session_id=test-session-1"

# Text-only chat
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"text": "Open settings", "session_id": "test"}'
```

## Architecture

```
Android App ←→ FastAPI Backend ←→ LangGraph Agent ←→ Groq APIs
```

### LangGraph Flow
1. **STT Node** - Groq Whisper speech-to-text
2. **Intent Node** - LLM intent classification  
3. **UI Check Node** - Analyze accessibility tree
4. **VLM Node** - Vision analysis if needed
5. **Action Planner** - Create action sequence
6. **TTS Node** - Generate response audio

## Project Structure
```
aura_backend/
├── main.py                    # FastAPI entrypoint
├── aura_graph.py             # LangGraph pipeline
├── run.py                    # Startup script
├── requirements.txt          # Dependencies
├── .env.example             # Environment template
├── nodes/                   # LangGraph nodes
│   ├── stt_node.py
│   ├── intent_node.py
│   ├── ui_check_node.py
│   ├── vlm_node.py
│   ├── action_planner_node.py
│   └── tts_node.py
├── groq/                    # API integrations
│   ├── stt.py
│   ├── llm.py
│   ├── vlm.py
│   └── tts.py
├── models/                  # Pydantic models
│   ├── request_models.py
│   └── action_plan.py
└── utils/                   # Utilities
    └── image_utils.py
```
