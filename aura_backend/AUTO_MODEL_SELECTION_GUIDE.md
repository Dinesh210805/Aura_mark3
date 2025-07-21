# AURA Auto Model Selection Guide

## Overview

AURA now features intelligent auto model selection that dynamically chooses the best AI model based on task complexity, performance requirements, and cost considerations - similar to Cursor and Perplexity's auto modes.

## Key Features

### 🧠 Intelligent Task Analysis
- **Complexity Detection**: Automatically determines if tasks are simple (translation, classification) or complex (coding, reasoning)
- **Content Analysis**: Detects coding, reasoning, multimodal, and other specialized requirements
- **Context-Aware**: Considers input length, modalities, and performance needs

### 🎯 Performance Optimization
- **Speed Mode**: Prioritizes fastest response times
- **Balanced Mode**: Optimizes for speed-quality balance
- **Quality Mode**: Uses most capable models for best results  
- **Cost Mode**: Selects most cost-effective options

### 📊 Smart Model Selection

#### For Simple Tasks (Translation, Classification, Basic Q&A)
- **Speed**: `groq/llama-3.1-8b-instant`
- **Balanced**: `gemini/gemini-1.5-flash`
- **Quality**: `gemini/gemini-1.5-pro-latest`
- **Cost**: `gemini/gemini-1.5-flash-8b` (best cost-efficiency)

#### For Complex Tasks (Coding, Advanced Reasoning)
- **Speed**: `gemini/gemini-1.5-flash`
- **Balanced**: `gemini/gemini-1.5-pro-latest`
- **Quality**: `gemini/gemini-1.5-pro-latest` (most capable current model)
- **Cost**: `groq/llama-3.3-70b-versatile`

#### For Vision Tasks
- **Speed**: `gemini/gemini-1.5-flash`
- **Balanced**: `groq/llama-4-maverick-17b-128e-instruct`
- **Quality**: `gemini/gemini-1.5-pro-latest`
- **Cost**: `gemini/gemini-1.5-flash-8b`

## New Gemini 2.5 Models (July 2025)

### Gemini 2.5 Pro (`gemini-2.5-pro`)
- **Best For**: Advanced reasoning, coding, research, high-stakes production
- **Context**: Very large (2M+ tokens)
- **Strengths**: State-of-the-art on benchmarks, "thinking" model, multimodal understanding
- **Cost**: Moderate (premium for quality)

### Gemini 2.5 Flash (`gemini-2.5-flash`)  
- **Best For**: High-speed general-purpose with programmable thinking depth
- **Context**: 1M tokens
- **Strengths**: Programmable "thinking budget", rapid response, efficient orchestration
- **Cost**: Very good (excellent speed/cost ratio)

### Gemini 2.5 Flash-Lite (`gemini-2.5-flash-lite`)
- **Best For**: Translation, classification, batch tasks at massive volume
- **Context**: 1M tokens  
- **Strengths**: Fastest, lowest-cost, can toggle "Thinking" mode for accuracy
- **Cost**: Best (extreme cost-efficiency)

### Gemini 2.0 Flash (`gemini-2.0-flash`)
- **Best For**: General tasks, voice, multimodal, large context
- **Context**: 2M tokens
- **Strengths**: Handles large files, pre-built agent support, strong performance
- **Cost**: Very good

## API Usage

### 1. Auto Mode in Standard Endpoints

```bash
# Auto selection is enabled by default when no provider/model specified
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"text": "Debug this Python code", "session_id": "test"}'

# Process with auto selection
curl -X POST http://localhost:8000/process \
  -F "audio=@sample.wav" \
  -F "screenshot=@screen.png"
```

### 2. Auto Selection API Endpoints

#### Get Model Recommendation
```bash
curl -X POST http://localhost:8000/providers/auto-select/recommend \
  -H "Content-Type: application/json" \
  -d '{
    "service_type": "llm",
    "text": "Write a function to implement quicksort",
    "performance_mode": "quality",
    "cost_sensitive": false
  }'
```

#### Configure Auto Mode
```bash
curl -X POST http://localhost:8000/providers/auto-select/config \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "default_performance_mode": "balanced",
    "cost_sensitivity": true
  }'
```

#### Get Selection Explanation
```bash
curl -X POST http://localhost:8000/providers/auto-select/explain \
  -H "Content-Type: application/json" \
  -d '{
    "service_type": "llm",
    "text": "Translate hello to French",
    "performance_mode": "cost"
  }'
```

### 3. Per-Request Performance Tuning

```bash
# Speed-optimized request
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "text": "What is AI?",
    "session_id": "fast"
  }' \
  --data-urlencode "performance_mode=speed"

# Quality-optimized request  
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Explain quantum entanglement",
    "session_id": "quality"
  }' \
  --data-urlencode "performance_mode=quality"
```

## Task Complexity Analysis

The auto selector analyzes your input to determine complexity:

### Simple Tasks
- **Keywords**: translate, convert, classify, identify, list, count
- **Examples**: "Translate hello to Spanish", "List the files", "Count the words"
- **Models**: Fast, cost-effective models (Flash-Lite, 8b-instant)

### Medium Tasks  
- **Characteristics**: General conversation, basic analysis, standard requests
- **Examples**: "Explain photosynthesis", "Write an email", "Summarize this article"
- **Models**: Balanced models (2.5-Flash, 3.3-70b-versatile)

### Complex Tasks
- **Keywords**: code, programming, algorithm, analyze, reasoning, research
- **Examples**: "Debug this code", "Design a database schema", "Compare philosophical theories"
- **Models**: Most capable models (2.5-Pro, 3.3-70b-versatile)

### Multimodal Tasks
- **Triggers**: Screenshots, images, audio, or video present
- **Examples**: "Find the button in this image", "Analyze this UI"
- **Models**: Vision-capable models (2.5-Pro, Llama-Vision, 2.5-Flash)

## Configuration Options

### Environment Variables
```bash
# Enable auto mode by default
ENABLE_AUTO_MODEL_SELECTION=true

# Default performance mode
DEFAULT_PERFORMANCE_MODE=balanced  # speed, balanced, quality, cost

# Cost sensitivity
DEFAULT_COST_SENSITIVE=true

# Auto mode overrides
AUTO_MODE_OVERRIDE_CODING=gemini-2.5-pro
AUTO_MODE_OVERRIDE_VISION=gemini-2.5-flash
```

### Per-Request Parameters
- `auto_mode`: Enable/disable auto selection
- `performance_mode`: speed, balanced, quality, cost
- `cost_sensitive`: true/false
- `provider_preferences`: Override specific services

## Advanced Features

### 1. Fallback Chain
If primary auto-selected model fails:
1. Try alternative models from same provider
2. Switch to different provider with similar capabilities
3. Fall back to most reliable general-purpose model

### 2. Context Length Optimization
- **Short text**: Use smaller, faster models
- **Medium content**: Balance context needs with speed
- **Long documents**: Automatically select models with large context windows (2M+ tokens)

### 3. Cost Optimization
- Simple tasks automatically use cheapest appropriate models
- Complex tasks balance cost with capability requirements
- Cost-sensitive mode prefers lite/efficient variants

### 4. Specialization Detection
- **Coding tasks**: Prefer models known for code generation
- **Vision tasks**: Auto-select multimodal models
- **Reasoning tasks**: Use models optimized for logic and analysis
- **Translation**: Use fastest, most cost-effective options

## Benefits

### 1. **Automatic Optimization**
- No manual model selection required
- Optimal performance for each task type
- Cost-efficiency without sacrificing quality

### 2. **Intelligent Scaling** 
- Simple tasks use fast, cheap models
- Complex tasks get powerful models
- Smooth scaling based on requirements

### 3. **Future-Proof**
- Easy to add new models and providers
- Selection logic adapts to new capabilities
- Provider-agnostic architecture

### 4. **User Experience**
- Zero configuration for most use cases
- Transparent selection with explanations
- Override capability when needed

## Testing

Run the comprehensive auto selection test:

```bash
cd d:\PROJECTS\Aura_mark3\aura_backend
python test_auto_selection.py
```

This validates:
- Task complexity analysis
- Performance mode selection
- Model recommendations
- Context length handling
- Service type coverage
- Cost optimization

## Best Practices

### 1. **Let Auto Mode Work**
- Don't specify providers/models unless you have specific requirements
- Trust the system to choose optimal combinations
- Use performance modes to guide selection

### 2. **Performance Mode Selection**
- **Speed**: Real-time applications, quick responses needed
- **Balanced**: General applications, good default choice
- **Quality**: Important tasks, research, complex analysis
- **Cost**: High-volume applications, budget-conscious scenarios

### 3. **Monitor and Adjust**
- Check auto selection explanations to understand choices
- Adjust performance mode based on results
- Override only when auto selection doesn't meet needs

### 4. **Cost Management**
- Enable cost sensitivity for budget-conscious applications
- Use speed/cost modes for high-volume scenarios  
- Reserve quality mode for critical tasks

## Troubleshooting

### Common Issues

1. **Unexpected Model Selection**
   - Check task complexity analysis with `/auto-select/explain`
   - Verify performance mode matches expectations
   - Consider adjusting input text to better reflect requirements

2. **Cost Higher Than Expected**
   - Enable `cost_sensitive: true`
   - Use `performance_mode: "cost"`
   - Check if tasks are being classified as complex when they're simple

3. **Quality Lower Than Expected**
   - Use `performance_mode: "quality"`
   - Disable cost sensitivity for important tasks
   - Add more context/detail to trigger complex task detection

4. **Auto Selection Disabled**
   - Check auto mode configuration with `/auto-select/config`
   - Ensure no explicit provider/model parameters override auto mode
   - Verify API keys for recommended providers

## Future Enhancements

The auto selection system is designed for easy extension:

- **Learning**: Track usage patterns and success rates
- **Custom Rules**: User-defined selection preferences
- **Load Balancing**: Distribute requests across providers
- **Model Performance Tracking**: Automatic quality assessment
- **Cost Tracking**: Real-time cost monitoring and optimization

---

🚀 **Your AURA system now has intelligent auto model selection like Cursor and Perplexity!**
