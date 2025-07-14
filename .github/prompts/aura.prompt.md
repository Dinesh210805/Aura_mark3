---
mode: agent
---

# PROMPT

---

# GitHub Copilot Agent Prompt for AURA Development

## Project Overview for Copilot Agent

You are tasked with helping develop **AURA: Autonomous UI Reader and Assistant**, an Android accessibility app that enables full voice control of Android devices. This is a complex project that requires deep integration with Android's accessibility framework and AI services.

## Project Context and Goals

**Primary Purpose**: Create an Android app that allows users with visual impairments, motor impairments, or users who are multitasking to control their entire Android device using voice commands.

**Core Workflow**:

1. **Voice Input** → STT (Speech-to-Text) processing
2. **Intent Understanding** → LLM processes command meaning
3. **UI Analysis** → Accessibility Services + VLM (Vision Language Model) analyze screen
4. **Action Planning** → Create step-by-step procedures
5. **Execution** → Simulate touches, swipes, typing via Accessibility APIs
6. **Feedback** → TTS (Text-to-Speech) informs user of progress

**Technical Requirements**:

- Android Native Development (Kotlin/Java) - NOT Flutter
- Minimum API Level 24 (Android 7.0)
- Integration with Groq Cloud AI services
- Real-time voice processing and screen interaction
- Accessibility service implementation
- Background service architecture

## Technology Stack

**AI Services (Groq Cloud)**:

- STT: whisper-large-v3-turbo
- LLM: llama-3.3-70b-versatile
- VLM: meta-llama/llama-4-maverick-17b-128e-instruct
- TTS: playai-tts

**Android Components**:

- AccessibilityService API (core requirement)
- MediaProjection API (for screenshots)
- AudioRecord/MediaRecorder (for voice input)
- Foreground Services (for background operation)
- Gesture APIs (for touch simulation)

## Required Directory Structure

```
app/src/main/java/com/yourcompany/aura/
├── core/
│   ├── AccessibilityCore.java        # Main accessibility interface
│   ├── VoiceCore.java               # Voice processing coordination
│   ├── AICore.java                  # AI services coordinator
│   └── GestureCore.java             # Touch/gesture simulation
├── services/
│   ├── AuraAccessibilityService.java # Accessibility service implementation
│   ├── VoiceListenerService.java    # Background voice monitoring
│   └── OverlayService.java          # UI overlay management
├── ai/
│   ├── GroqClient.java              # Groq API integration
│   ├── STTProcessor.java            # Speech-to-text handler
│   ├── LLMProcessor.java            # Language model interface
│   ├── VLMProcessor.java            # Vision model interface
│   └── TTSProcessor.java            # Text-to-speech handler
├── ui/
│   ├── MainActivity.java            # Main app interface
│   ├── SetupActivity.java           # Initial setup and onboarding
│   ├── PermissionActivity.java      # Permission management
│   └── SettingsActivity.java        # Configuration interface
├── utils/
│   ├── ScreenCapture.java           # Screenshot functionality
│   ├── AudioRecorder.java           # Audio recording utilities
│   ├── UIAnalyzer.java              # UI element analysis
│   └── CommandProcessor.java        # Command interpretation
└── models/
    ├── VoiceCommand.java            # Voice command data structure
    ├── UIElement.java               # UI element representation
    ├── ActionPlan.java              # Action sequence planning
    └── UserPreferences.java         # User configuration storage
```

## Critical Implementation Requirements

### 1. Accessibility Service Foundation

- Must extend AccessibilityService
- Require BIND_ACCESSIBILITY_SERVICE permission
- Handle AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
- Implement robust node tree traversal
- Support gesture simulation via AccessibilityService.GestureResultCallback

### 2. Voice Processing Architecture

- Implement continuous background listening
- Use AudioRecord for low-latency audio capture
- Integrate wake word detection
- Handle audio focus management
- Process audio in separate thread to avoid UI blocking

### 3. AI Integration Patterns

- Implement async HTTP clients for Groq API calls
- Use proper JSON parsing for API responses
- Handle rate limiting and error recovery
- Implement request queuing for multiple concurrent operations
- Cache frequently used responses

### 4. UI Analysis System

- Combine AccessibilityNodeInfo with screenshot analysis
- Implement coordinate mapping between accessibility tree and screen pixels
- Handle dynamic content that accessibility services might miss
- Support multiple screen densities and orientations

### 5. Security and Privacy

- Implement secure storage for API keys
- Handle sensitive voice data appropriately
- Provide clear user consent mechanisms
- Implement data retention policies

## Development Phases

### Phase 1: Core Infrastructure

**Goal**: Basic app with accessibility service and voice recording

**Key Components to Implement**:

- Basic Android project structure
- Accessibility service registration and basic functionality
- Simple voice recording and playback
- Permission handling system
- Basic UI for setup and configuration

**Success Criteria**:

- App can register as accessibility service
- Can capture and play back voice recordings
- Basic UI navigation works
- All required permissions are properly requested

### Phase 2: AI Service Integration

**Goal**: Connect to Groq Cloud and process basic commands

**Key Components to Implement**:

- GroqClient with proper authentication
- STT integration for voice-to-text conversion
- LLM integration for basic command understanding
- TTS integration for user feedback
- Error handling and retry mechanisms

**Success Criteria**:

- Voice commands convert to text accurately
- Basic commands (like "what's on screen") work
- Text-to-speech provides clear feedback
- Network errors are handled gracefully

### Phase 3: Screen Understanding

**Goal**: Analyze and interact with UI elements

**Key Components to Implement**:

- Screen capture functionality
- Accessibility tree parsing
- VLM integration for visual analysis
- Basic click and touch simulation
- UI element coordinate mapping

**Success Criteria**:

- Can describe what's currently on screen
- Can identify and click specific UI elements
- Screenshots are properly analyzed by VLM
- Touch simulation works across different apps

### Phase 4: Advanced Interactions

**Goal**: Handle complex commands and multi-step actions

**Key Components to Implement**:

- Complex gesture simulation (scrolling, swiping)
- Multi-step action planning
- Context-aware command processing
- Error recovery and fallback mechanisms
- Performance optimization

**Success Criteria**:

- Can handle complex commands like "scroll down and find the settings button"
- Works reliably across different apps
- Recovers gracefully from errors
- Performs efficiently without draining battery

## Key Implementation Patterns

### Accessibility Service Pattern

```java
public class AuraAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle UI changes and window state updates
        // Update current screen understanding
        // Trigger actions if needed
    }

    @Override
    public void onInterrupt() {
        // Handle service interruption
        // Clean up resources
        // Attempt reconnection
    }
}
```

### Voice Processing Pattern

```java
public class VoiceListenerService extends Service {
    private AudioRecord audioRecord;
    private boolean isListening = false;

    private void startListening() {
        // Initialize AudioRecord
        // Start background thread for continuous listening
        // Implement wake word detection
        // Process voice commands when detected
    }
}
```

### AI Integration Pattern

```java
public class GroqClient {
    private OkHttpClient httpClient;
    private String apiKey;

    public void sendSTTRequest(byte[] audioData, Callback callback) {
        // Build multipart request with audio data
        // Send to Groq STT endpoint
        // Handle response asynchronously
        // Parse and return text result
    }
}
```

## Testing Strategy

### Unit Testing Focus

- Test AI API integration with mock responses
- Test voice processing with sample audio files
- Test UI analysis with known screen layouts
- Test command parsing with various input formats

### Integration Testing Focus

- Test complete voice-to-action workflows
- Test accessibility service integration
- Test error handling and recovery
- Test performance under various conditions

### Real-World Testing Requirements

- Test with multiple Android versions and devices
- Test with different apps and UI layouts
- Test with various speech patterns and accents
- Test battery usage and performance impact

## Common Pitfalls to Avoid

1. **Accessibility Service Lifecycle Issues**: Services can be killed by system; implement robust reconnection
2. **Audio Resource Management**: Properly release AudioRecord resources to avoid crashes
3. **UI Thread Blocking**: Never perform network or AI operations on main thread
4. **Permission Handling**: Accessibility permissions require user to enable in system settings
5. **Memory Leaks**: Properly manage service lifecycle and callback references

## Success Metrics

- **Accuracy**: Voice commands executed correctly >90% of the time
- **Response Time**: Commands executed within 2-3 seconds
- **Reliability**: App maintains stable operation for extended periods
- **Battery Usage**: Minimal impact on device battery life
- **Compatibility**: Works across major Android apps and versions

## Getting Started Instructions

1. **Project Setup**: Create new Android project with minimum SDK 24
2. **Dependency Configuration**: Add required libraries for HTTP, JSON, audio processing
3. **Manifest Configuration**: Add all required permissions and service declarations
4. **Core Service Implementation**: Start with basic accessibility service
5. **Voice Integration**: Add simple voice recording and STT integration
6. **Iterative Development**: Build and test each component before moving to next

## Important Notes for Copilot Agent

- This is a beginner-friendly project that requires extensive explanation and commenting
- Focus on creating working, testable code at each phase
- Prioritize code clarity and maintainability over optimization initially
- Include comprehensive error handling and user feedback
- Follow Android development best practices and conventions
- Consider accessibility guidelines even while building an accessibility app

---

**Remember**: This is a complex project that combines Android system programming with cutting-edge AI. Take an iterative approach, build working components progressively, and test thoroughly at each stage. The end result will be a powerful tool that can genuinely help people with accessibility needs.

my thoughts:

hi there, copilot, we are going to develop a new project that will be very **useful** to people with visual impairments, motor impairments, even normal mob who are busy with their hands. The project name is **AURA: Autonomous UI Reader and Assistant**, so we are going to develop this together.

**About the idea:**

Here we are going to control the whole mobile with voice, so the main doubt is how are we gonna control the phone with voice? So the major doubts would be like **how?**

So I will give you my answer. That is, we are going to build an app only for Android — where we will interact with the Agent AURA with our voice command that will be processed by an **STT (speech-to-text model)** → which converts the voice into text. Then we must understand the user's intent, right!? So we will use **LLM (large language models)** to understand the intent of the command. Now comes the major part: **how** we will be doing the task? So here we go. Remember the project name “AURA”? Here you can see **Autonomous UI Reader** — this name shows what we are gonna do. Now we will use two approaches here: that is, **Android Accessibility Services** to locate the UI elements, and another one — big bro entry — is **VLM (vision language models)**. They come into play. They are powerful models that can understand images. The thing is, nowadays the data inside apps and the mobile are dynamic — only Android Accessibility Services won't be enough. So we are using the VLM in addition too. Now we understood the UI, and we must create step-by-step procedures to accomplish the user's goal mentioned in the voice. So since VLMs are multimodal (they can process both images and text), we will use them to locate the UI elements — like give us the coordinates (based on the **resolution** of the image, we can get the coordinates, right?) — and also we can use Android Accessibility Services and create step-by-step procedures and simulate the touch or swipe using Android Accessibility Services. And if the page changes, again use the Android **Accessibility Service** and VLM to understand and act, and use **TTS (text-to-speech)** to inform the users about the process. And for typing in fields, we can use LLMs and Android Accessibility Services.

---

So now tech stack — I'm not **confirmed**, but here is what I have in my mind:

For **GenAI stack**, we will be using models from **Groq Cloud**

(**STT (whisper-large-v3-turbo)**,

**LLM (llama-3.3-70b-versatile)**,

**VLM (meta-llama/llama-4-maverick-17b-128e-instruct)**,

**TTS (playai-tts)**)

And for the app, we can go Android app() dev or **Flutter**.

formatted prompt :

| Feature                                                | **Flutter**                                                  | **Native Android (Kotlin/Java)**               |
| ------------------------------------------------------ | ------------------------------------------------------------ | ---------------------------------------------- |
| 🔌 **AccessibilityService API**                        | ⚠️ Difficult (requires native channel + limited support)     | ✅ Full native support                         |
| 🎤 **Background STT/Voice Control**                    | ⚠️ Limited, workaround via native service                    | ✅ Reliable with `Service`/`ForegroundService` |
| 🖼️ **MediaProjection (for screenshot)**                | ⚠️ Hacky via native code                                     | ✅ Native support                              |
| 🔁 **Gesture simulation (Accessibility Gesture APIs)** | ❌ Not directly possible                                     | ✅ Fully supported                             |
| 🧠 **LLM/VLM/STT Integration (Cloud)**                 | ✅ Easier for fast prototyping UI, good Groq API integration | ✅ Also good, just more boilerplate            |
| 🧰 **Custom OS-level hooks & permissions**             | ❌ Limited control                                           | ✅ Full control                                |
| 📱 **UI Development & Animation**                      | ✅ Faster, beautiful UI, hot reload                          | 🟡 Slower, more verbose                        |
| ⚙️ **Multi-threading / Event Handling**                | ⚠️ Harder with Dart                                          | ✅ More predictable with Java/Kotlin           |
| 🌍 **Cross-platform possibility**                      | ✅ iOS + Android (but not needed here)                       | ❌ Android only                                |
| 🚀 **Performance for accessibility tasks**             | ⚠️ Needs native bridges                                      | ✅ Direct execution, no bridge overhead        |
| 🧩 **Plugin ecosystem for Accessibility**              | ⚠️ Very poor                                                 | ✅ Rich and fully native                       |

**Hi Copilot,**

We are building a new accessibility-focused project named **AURA: Autonomous UI Reader and Assistant**. This app will assist users with:

- Visual impairments
- Motor impairments
- Even general users who are multitasking or unable to touch their phones

**Goal:**

Enable full control of an Android phone using **voice commands**.

### 🔄 Workflow:

1. **Voice Command Input** → Processed by **STT** (e.g., `whisper-large-v3-turbo`)
2. **Intent Understanding** → Done via **LLM** (e.g., `llama-3.3-70b-versatile`)
3. **UI Element Detection:**
   - Use **Android Accessibility Services** to access active UI hierarchy.
   - If the UI is dynamic or image-based, use **VLM** (e.g., `llama-4-maverick-17b`) to analyze screenshots and locate UI elements.
4. **Action Execution:**
   - Based on intent and identified UI elements, simulate **clicks, swipes**, or **text input** via Accessibility APIs.
   - If page state changes, re-run UI analysis and respond accordingly.
5. **User Feedback:**
   - Use **TTS** (`playai-tts`) to inform the user of each step or result.

### 🧠 Tech Stack (Tentative):

- **STT**: `whisper-large-v3-turbo`
- **LLM**: `llama-3.3-70b-versatile`
- **VLM**: `llama-4-maverick-17b-128e-instruct`
- **TTS**: `playai-tts`

### 📱 App Platform:

- Android native (Java/Kotlin) or Flutter (cross-platform)

Let's proceed step by step — start by setting up **voice input** and **Groq Cloud model calls**.

use this documentations :

D:\AndroidStudioProjects\AURA\groq documentation
D:\AndroidStudioProjects\AURA\groq documentation\Agentic_tooling_doc.md
D:\AndroidStudioProjects\AURA\groq documentation\available_models.md
D:\AndroidStudioProjects\AURA\groq documentation\images_and_vision_doc.md
D:\AndroidStudioProjects\AURA\groq documentation\speech_to_text_doc.md
D:\AndroidStudioProjects\AURA\groq documentation\text_generation_doc.md
D:\AndroidStudioProjects\AURA\groq documentation\text_to_speech_doc.md
