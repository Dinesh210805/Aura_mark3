# AURA Agent LangSmith Visualization Setup Guide

## Overview

LangSmith provides powerful visualization and debugging capabilities for your AURA agent. You can see the execution flow, timing, errors, and performance metrics of your LangGraph orchestrated agent.

## Setup Steps

### 1. Get LangSmith API Key

1. Go to [https://smith.langchain.com](https://smith.langchain.com)
2. Sign up or log in to your LangChain account
3. Navigate to Settings → API Keys
4. Create a new API key
5. Copy the API key

### 2. Update Environment Configuration

Update your `.env` file with your LangSmith API key:

```env
# LangSmith Configuration (for agent visualization)
LANGCHAIN_TRACING_V2=true
LANGCHAIN_ENDPOINT=https://api.smith.langchain.com
LANGCHAIN_API_KEY=ls_your_actual_api_key_here
LANGCHAIN_PROJECT=aura-agent-visualization
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

### 4. Restart Your Server

```bash
python run.py
```

## Using LangSmith Visualization

### API Endpoints

Your AURA backend now includes these LangSmith endpoints:

#### Check Configuration Status
```bash
curl http://localhost:8000/langsmith/status
```

#### Get Recent Traces
```bash
# Get traces from last hour
curl http://localhost:8000/langsmith/traces

# Get traces from last 4 hours
curl "http://localhost:8000/langsmith/traces?hours=4&limit=50"
```

#### Analyze Specific Trace
```bash
curl http://localhost:8000/langsmith/traces/{trace_id}/analysis
```

#### Generate Execution Report
```bash
curl http://localhost:8000/langsmith/report?hours=24
```

#### Get Dashboard URL
```bash
curl http://localhost:8000/langsmith/dashboard
```

### Web Dashboard

Once configured, you can view visual traces at:
- **Dashboard URL**: https://smith.langchain.com/projects/p/aura-agent-visualization
- **Direct Access**: Visit the URL returned by `/langsmith/dashboard`

## What You'll See

### 1. **Execution Flow Visualization**
- See how your AURA agent moves through each node:
  - STT (Speech-to-Text)
  - Intent Analysis
  - UI Check
  - VLM (Vision Language Model)
  - Action Planning
  - TTS (Text-to-Speech)

### 2. **Performance Metrics**
- **Execution time** for each node
- **Total request latency**
- **Success/failure rates**
- **Bottleneck identification**

### 3. **Error Analysis**
- **Failed nodes** and error messages
- **Retry patterns**
- **Provider fallback usage**

### 4. **Input/Output Inspection**
- **Raw inputs** to each node
- **Intermediate outputs** between nodes
- **Final responses** from the agent

## Example Trace Analysis

After running some requests, you'll see traces like:

```json
{
  "node_sequence": ["stt", "intent", "ui_check", "vlm", "action_planner", "tts"],
  "execution_time_ms": 2450,
  "node_execution_times": {
    "stt": 890,
    "intent": 340,
    "ui_check": 45,
    "vlm": 780,
    "action_planner": 290,
    "tts": 105
  },
  "success": true,
  "errors": []
}
```

## Usage Examples

### Testing with Visualization

1. **Start your server** with LangSmith enabled
2. **Make some requests** to your AURA agent:
   ```bash
   curl -X POST http://localhost:8000/chat \
     -H "Content-Type: application/json" \
     -d '{"text": "Open WhatsApp", "session_id": "test"}'
   ```

3. **Check the traces**:
   ```bash
   curl http://localhost:8000/langsmith/traces
   ```

4. **View in dashboard** - Open the URL from `/langsmith/dashboard`

### Debugging Performance Issues

If your agent is slow:

1. **Get execution report**:
   ```bash
   curl http://localhost:8000/langsmith/report?hours=1
   ```

2. **Identify bottlenecks** from `node_execution_times`

3. **Optimize slow nodes** (e.g., switch to faster models)

### Monitoring Production

For production monitoring:

1. **Set up periodic reporting**:
   ```bash
   # Check last 24 hours
   curl http://localhost:8000/langsmith/report?hours=24
   ```

2. **Monitor success rates** and **common errors**

3. **Track performance trends** over time

## Troubleshooting

### LangSmith Not Working

1. **Check status**:
   ```bash
   curl http://localhost:8000/langsmith/status
   ```

2. **Verify API key** in `.env` file

3. **Check logs** for LangSmith connection errors

### No Traces Appearing

1. **Ensure tracing is enabled**: `LANGCHAIN_TRACING_V2=true`
2. **Check project name** matches your LangSmith project
3. **Make some requests** to generate traces
4. **Wait a few seconds** for traces to appear

### Dashboard Access Issues

1. **Use the API endpoints** to verify data exists
2. **Check your LangSmith account** permissions
3. **Verify project name** matches the URL

## Benefits

- **🔍 Debug Complex Flows**: See exactly where your agent fails
- **⚡ Optimize Performance**: Identify slow nodes and bottlenecks  
- **📊 Monitor Production**: Track success rates and error patterns
- **🧪 Compare Changes**: A/B test different model configurations
- **👥 Team Collaboration**: Share traces with your team for debugging

---

**Your AURA agent is now ready for advanced visualization with LangSmith!** 🚀
