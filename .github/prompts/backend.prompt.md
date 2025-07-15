Build a modular backend service for AURA, an Android accessibility assistant. This service must:

Accept voice input → perform STT using Groq

Understand intent using LLM (Groq)

Inspect screen via Accessibility tree or screenshot + VLM

Create step-by-step action plans

Return TTS audio or an action sequence to the app

🧱 STACK
LangGraph for multi-step, stateful agent orchestration

FastAPI for API layer

Groq API for:

whisper-large-v3-turbo (STT)

llama-3.3-70b-versatile (LLM)

llama-4-maverick-17b (VLM)

playai-tts (TTS)

Pydantic for request/response models

🧩 FOLDER STRUCTURE
bash
Copy
Edit
aura_backend/
├── main.py                    # FastAPI entrypoint
├── aura_graph.py             # LangGraph pipeline definition
├── nodes/
│   ├── stt_node.py
│   ├── intent_node.py
│   ├── ui_check_node.py
│   ├── vlm_node.py
│   ├── action_planner_node.py
│   └── tts_node.py
├── groq/
│   ├── stt.py
│   ├── llm.py
│   ├── vlm.py
│   └── tts.py
├── models/
│   ├── request_models.py
│   └── action_plan.py
└── utils/
    └── image_utils.py
✅ STEP-BY-STEP INSTRUCTIONS
1. Define Each LangGraph Node
Each node is a Python class with a .run(state: dict) → dict method.

nodes/stt_node.py
python
Copy
Edit
class STTNode:
    def run(self, state):
        audio = state["audio"]
        transcript = groq_stt(audio)
        return {"transcript": transcript}
nodes/intent_node.py
python
Copy
Edit
class IntentNode:
    def run(self, state):
        intent = groq_llm(state["transcript"])
        return {"intent": intent}
nodes/ui_check_node.py
python
Copy
Edit
class UICheckNode:
    def run(self, state):
        if state["ui_tree"]:
            return {"action_plan": build_plan_from_tree(state["ui_tree"], state["intent"])}
        return {"fallback": True}
nodes/vlm_node.py
python
Copy
Edit
class VLMNode:
    def run(self, state):
        image = state["screenshot"]
        coords = groq_vlm(image, state["intent"])
        return {"ui_element_coords": coords}
nodes/action_planner_node.py
python
Copy
Edit
class ActionPlannerNode:
    def run(self, state):
        return {
            "action_plan": [
                {"type": "tap", "x": state["ui_element_coords"]["x"], "y": state["ui_element_coords"]["y"]}
            ]
        }
nodes/tts_node.py
python
Copy
Edit
class TTSNode:
    def run(self, state):
        speech = groq_tts(f"Executing: {state['intent']}")
        return {"tts_audio": speech}
2. Connect the Nodes in LangGraph
aura_graph.py
python
Copy
Edit
from langgraph.graph import StateGraph
from nodes import *

graph = StateGraph()

graph.set_entry_node("stt")
graph.add_node("stt", STTNode())
graph.add_node("intent", IntentNode())
graph.add_node("ui_check", UICheckNode())
graph.add_node("vlm", VLMNode())
graph.add_node("planner", ActionPlannerNode())
graph.add_node("tts", TTSNode())

graph.add_edge("stt", "intent")
graph.add_edge("intent", "ui_check")
graph.add_conditional_edges("ui_check", {
    "fallback": "vlm",
    "action_plan": "tts"
})
graph.add_edge("vlm", "planner")
graph.add_edge("planner", "tts")

agent = graph.compile()
3. Expose the LangGraph Agent via FastAPI
main.py
python
Copy
Edit
from fastapi import FastAPI, File, UploadFile
from aura_graph import agent

app = FastAPI()

@app.post("/process")
async def process_audio(audio: UploadFile = File(...), screenshot: UploadFile = File(...)):
    state = {
        "audio": await audio.read(),
        "screenshot": await screenshot.read(),
        "ui_tree": None  # Optionally passed
    }
    result = agent.invoke(state)
    return result
🛡️ SECURITY CONSIDERATIONS
Store API keys securely (dotenv or Vault)

Use token authentication or IP filtering

Validate inputs strictly with Pydantic

Rate-limit endpoints

🧪 TESTING
Use curl or Postman to POST:

bash
Copy
Edit
curl -X POST http://localhost:8000/process \
  -F "audio=@sample.wav" \
  -F "screenshot=@screen.png"
✅ DELIVERABLES FOR BACKEND AGENT
Python 3.10+

FastAPI + LangGraph orchestrator

Modular nodes for STT, LLM, VLM, Planner, TTS

API route /process to accept audio/screenshot

Output: action plan + TTS binary

