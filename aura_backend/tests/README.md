# AURA Backend Test Suite

This folder contains various test scripts used to verify and debug the AURA backend functionality.

## Test Categories

### 🔍 LangSmith Integration Tests
- `test_langsmith.py` - Main LangSmith integration test
- `test_langsmith_comprehensive.py` - Comprehensive LangSmith tracing verification
- `test_langsmith_direct.py` - Direct LangSmith client testing
- `test_langsmith_direct_trace.py` - Direct trace creation test
- `test_langsmith_simple.py` - Simple LangSmith connectivity test
- `verify_langsmith.py` - LangSmith project and tracing verification

### 🤖 Agent Functionality Tests
- `test_aura_direct.py` - Direct AURA agent testing
- `test_aura_auto_tracing.py` - Auto-tracing functionality test
- `test_endpoint_tracing.py` - HTTP endpoint tracing test

### 📊 LangGraph Tests
- `test_langgraph_tracing.py` - LangGraph execution tracing
- `test_simple_langgraph.py` - Simple LangGraph workflow test

### 🔄 Trace Generation
- `generate_traces.py` - Generate sample traces for visualization
- `generate_comprehensive_traces.py` - Generate comprehensive test traces
- `simple_trace_test.py` - Simple trace generation test

## Running Tests

### Quick LangSmith Verification
```bash
cd tests
python verify_langsmith.py
```

### Generate Sample Traces
```bash
cd tests
python generate_traces.py
```

### Comprehensive Integration Test
```bash
cd tests
python test_langsmith_comprehensive.py
```

### Test Agent Endpoints
```bash
cd tests
python test_endpoint_tracing.py
```

## Test Requirements

All tests require:
- AURA backend server running on localhost:8000
- Valid API keys in .env file
- LangSmith project configured (if testing LangSmith features)

## Environment Variables Required

```bash
GROQ_API_KEY=your_groq_api_key
GEMINI_API_KEY=your_gemini_api_key
LANGCHAIN_API_KEY=your_langsmith_api_key
LANGCHAIN_PROJECT=aura-agent-visualization
LANGCHAIN_TRACING_V2=true
```

## Notes

- These test files were created during development and debugging
- They can be used for troubleshooting integration issues
- Some tests may require the server to be running
- Check the individual test files for specific requirements and usage instructions
