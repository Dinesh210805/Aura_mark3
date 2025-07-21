#!/usr/bin/env python3
"""
Optimized Intent Recognition System for AURA
Enhanced prompt templates and model selection for better performance and reduced latency
"""

import json
import logging
import time
from typing import Dict, Any, Optional, List
from dataclasses import dataclass
from enum import Enum

try:
    from performance_monitor import performance_monitor
    PERFORMANCE_TRACKING = True
except ImportError:
    PERFORMANCE_TRACKING = False

logger = logging.getLogger(__name__)

class IntentCategory(Enum):
    """Intent categories for optimized routing"""
    NAVIGATION = "navigation"          # Open app, go back, navigate
    UI_INTERACTION = "ui_interaction"  # Tap, scroll, type, swipe
    SYSTEM_CONTROL = "system_control"  # Settings, wifi, bluetooth
    INFORMATION = "information"        # What's on screen, read content
    COMMUNICATION = "communication"    # Send message, make call
    UTILITY = "utility"               # Take screenshot, set timer
    GREETING = "greeting"             # Hello, hi, how are you
    HELP = "help"                     # What can you do, help me

@dataclass
class OptimizedPromptTemplate:
    """Template for different intent analysis scenarios"""
    category: IntentCategory
    system_prompt: str
    max_tokens: int
    temperature: float
    expected_fields: List[str]

class OptimizedIntentAnalyzer:
    """Optimized intent analyzer with specialized prompts and model selection"""
    
    def __init__(self):
        # Ultra-fast intent classification keywords with enhanced patterns
        self.quick_classifiers = {
            IntentCategory.GREETING: {
                'keywords': ['hello', 'hi', 'hey', 'good morning', 'good afternoon', 'how are you', 'what\'s up', 'greetings'],
                'confidence_threshold': 0.95
            },
            IntentCategory.NAVIGATION: {
                'keywords': ['open', 'launch', 'start', 'go to', 'navigate', 'back', 'home', 'switch', 'exit', 'close app'],
                'confidence_threshold': 0.85
            },
            IntentCategory.UI_INTERACTION: {
                'keywords': ['tap', 'click', 'press', 'scroll', 'swipe', 'type', 'enter', 'input', 'select', 'touch', 'drag'],
                'confidence_threshold': 0.90
            },
            IntentCategory.SYSTEM_CONTROL: {
                'keywords': ['wifi', 'bluetooth', 'settings', 'volume', 'brightness', 'airplane mode', 'silent', 'vibrate'],
                'confidence_threshold': 0.95
            },
            IntentCategory.INFORMATION: {
                'keywords': ['what', 'read', 'tell me', 'show me', 'describe', 'explain', "what's on", 'screen content'],
                'confidence_threshold': 0.80
            },
            IntentCategory.HELP: {
                'keywords': ['help', 'what can you do', 'how do', 'assist', 'support', 'tutorial', 'guide'],
                'confidence_threshold': 0.90
            },
            IntentCategory.COMMUNICATION: {
                'keywords': ['send message', 'call', 'text', 'email', 'whatsapp', 'telegram', 'sms'],
                'confidence_threshold': 0.85
            }
        }
        
        # Optimized prompt templates for each category
        self.prompt_templates = {
            IntentCategory.NAVIGATION: OptimizedPromptTemplate(
                category=IntentCategory.NAVIGATION,
                system_prompt="""AURA navigation analyzer. Extract app/action info FAST.

RESPOND WITH ONLY THIS JSON (no explanations):
{
    "intent": "open_app|navigate_back|go_home",
    "app_name": "app name or null",
    "action_type": "open_app|navigate|back|home",
    "confidence": 0.9,
    "requires_screen_analysis": false
}

KEYWORDS: open, launch, start, go back, home, switch to""",
                max_tokens=80,
                temperature=0.0,
                expected_fields=["intent", "app_name", "action_type", "confidence", "requires_screen_analysis"]
            ),
            
            IntentCategory.UI_INTERACTION: OptimizedPromptTemplate(
                category=IntentCategory.UI_INTERACTION,
                system_prompt="""AURA UI interaction analyzer. Extract interaction details FAST.

RESPOND WITH ONLY THIS JSON (no explanations):
{
    "intent": "brief description",
    "action_type": "tap|swipe|scroll|type|long_press",
    "target_element": "element description or null",
    "text_input": "text to type or null", 
    "direction": "up|down|left|right or null",
    "confidence": 0.85,
    "requires_screen_analysis": true
}

KEYWORDS: tap, click, press, scroll, swipe, type, enter""",
                max_tokens=120,
                temperature=0.0,
                expected_fields=["intent", "action_type", "target_element", "confidence", "requires_screen_analysis"]
            ),
            
            IntentCategory.SYSTEM_CONTROL: OptimizedPromptTemplate(
                category=IntentCategory.SYSTEM_CONTROL,
                system_prompt="""AURA system control analyzer. Extract system actions FAST.

RESPOND WITH ONLY THIS JSON (no explanations):
{
    "intent": "brief description",
    "action_type": "system_command",
    "system_action": "wifi_toggle|bluetooth_toggle|volume_up|volume_down|brightness_up|brightness_down|settings",
    "confidence": 0.9,
    "requires_screen_analysis": false
}

KEYWORDS: wifi, bluetooth, volume, brightness, settings""",
                max_tokens=70,
                temperature=0.0,
                expected_fields=["intent", "action_type", "system_action", "confidence", "requires_screen_analysis"]
            ),
            
            IntentCategory.INFORMATION: OptimizedPromptTemplate(
                category=IntentCategory.INFORMATION,
                system_prompt="""You are AURA's information request analyzer. Extract what user wants to know.

RESPOND WITH ONLY THIS JSON:
{
    "intent": "brief description of information request",
    "action_type": "read_screen|describe_ui|get_info",
    "info_type": "screen_content|app_info|element_details|general",
    "confidence": 0.0-1.0,
    "requires_screen_analysis": true
}""",
                max_tokens=150,
                temperature=0.0,
                expected_fields=["intent", "action_type", "info_type", "confidence", "requires_screen_analysis"]
            ),
            
            IntentCategory.GREETING: OptimizedPromptTemplate(
                category=IntentCategory.GREETING,
                system_prompt="""AURA greeting analyzer. Detect greetings FAST.

RESPOND WITH ONLY THIS JSON (no explanations):
{
    "intent": "greeting or conversation",
    "action_type": "respond",
    "greeting_type": "hello|how_are_you|good_morning|casual",
    "confidence": 0.95,
    "requires_screen_analysis": false
}

KEYWORDS: hello, hi, hey, good morning, how are you""",
                max_tokens=60,
                temperature=0.0,
                expected_fields=["intent", "action_type", "greeting_type", "confidence", "requires_screen_analysis"]
            ),
            
            IntentCategory.COMMUNICATION: OptimizedPromptTemplate(
                category=IntentCategory.COMMUNICATION,
                system_prompt="""AURA communication analyzer. Extract messaging/calling details FAST.

RESPOND WITH ONLY THIS JSON (no explanations):
{
    "intent": "brief description",
    "action_type": "send_message|make_call|open_chat",
    "app_name": "whatsapp|telegram|phone|messages|null",
    "recipient": "contact name or null",
    "message_text": "message content or null",
    "confidence": 0.85,
    "requires_screen_analysis": true
}

KEYWORDS: send, message, call, text, whatsapp, telegram""",
                max_tokens=100,
                temperature=0.0,
                expected_fields=["intent", "action_type", "app_name", "confidence", "requires_screen_analysis"]
            )
        }
        
        # Simple cache for frequently used intents (reduce API calls)
        self.intent_cache = {}
        self.cache_max_size = 50
        
        # Pre-computed responses for ultra-common intents
        self.instant_responses = {
            "hello": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "hello aura": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "hi": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "hi aura": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "hey": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "hey aura": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "good morning": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "good morning aura": {
                "intent": "greeting", "action_type": "respond", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "greeting"
            },
            "go back": {
                "intent": "navigate back", "action_type": "navigate", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "navigation"
            },
            "home": {
                "intent": "go home", "action_type": "navigate", "confidence": 0.95,
                "requires_screen_analysis": False, "_category": "navigation"
            }
        }
        
        # Fallback ultra-fast template for unclassified intents
        self.general_template = OptimizedPromptTemplate(
            category=IntentCategory.UTILITY,
            system_prompt="""AURA Android assistant. Analyze command EFFICIENTLY.

CATEGORIES: Navigation (open apps), UI (tap/scroll/type), System (wifi/volume), Info (read screen)

RESPOND WITH ONLY THIS JSON (no explanations):
{
    "intent": "1-sentence description",
    "action_type": "tap|swipe|type|navigate|open_app|system_command|read_screen",
    "confidence": 0.7,
    "requires_screen_analysis": true
}

BE FAST and ACCURATE.""",
            max_tokens=150,
            temperature=0.0,
            expected_fields=["intent", "action_type", "confidence", "requires_screen_analysis"]
        )

    def _check_instant_response(self, transcript_clean: str) -> Optional[Dict[str, Any]]:
        """Check for instant response matches with flexible pattern matching"""
        
        # Direct match first (fastest)
        if transcript_clean in self.instant_responses:
            return self.instant_responses[transcript_clean]
        
        # Normalize and clean the transcript more aggressively
        words = transcript_clean.replace(",", "").replace(".", "").replace("!", "").replace("?", "").split()
        
        # Flexible greeting patterns
        greeting_words = ["hello", "hi", "hey", "good", "morning", "afternoon", "evening"]
        aura_words = ["aura", "ora", "aurora"]
        capability_words = ["what", "can", "you", "do", "help", "capabilities", "assist", "support"]
        
        # Check for capability/help questions
        if any(word in words for word in capability_words):
            # Questions like "what can you do", "how can you help", etc.
            if any(combo in transcript_clean for combo in [
                "what can you do", "what can you", "how can you help", 
                "what are your capabilities", "what do you do", "help me",
                "what can aura do", "what are you capable of"
            ]):
                return {
                    "intent": "explain capabilities and features",
                    "action_type": "respond",
                    "confidence": 0.95,
                    "requires_screen_analysis": False,
                    "_category": "help",
                    "_response_type": "capabilities_explanation"
                }
        
        # Check if first word is a greeting
        if words and words[0] in greeting_words:
            # Handle "good morning" type greetings
            if words[0] == "good" and len(words) > 1 and words[1] in ["morning", "afternoon", "evening"]:
                return {
                    "intent": "greeting", 
                    "action_type": "respond", 
                    "confidence": 0.95,
                    "requires_screen_analysis": False, 
                    "_category": "greeting",
                    "_response_type": "time_based_greeting"
                }
            # Handle simple greetings like "hello", "hi", "hey"
            elif words[0] in ["hello", "hi", "hey"]:
                # If it's a complex greeting with questions, treat it as help request
                if len(words) > 4 or any(word in words for word in capability_words):
                    return {
                        "intent": "greeting with capability inquiry",
                        "action_type": "respond",
                        "confidence": 0.95,
                        "requires_screen_analysis": False,
                        "_category": "help",
                        "_response_type": "greeting_with_capabilities"
                    }
                # Simple greeting with optional "aura"
                elif len(words) <= 3 and (len(words) == 1 or any(word in aura_words for word in words)):
                    return {
                        "intent": "greeting", 
                        "action_type": "respond", 
                        "confidence": 0.95,
                        "requires_screen_analysis": False, 
                        "_category": "greeting",
                        "_response_type": "simple_greeting"
                    }
        
        # Navigation shortcuts
        if transcript_clean in ["back", "go back", "home", "go home"]:
            return {
                "intent": "navigate back" if "back" in transcript_clean else "go home",
                "action_type": "navigate", 
                "confidence": 0.95,
                "requires_screen_analysis": False, 
                "_category": "navigation"
            }
        
        # Common confirmations
        if transcript_clean in ["yes", "ok", "okay", "sure", "no"]:
            return {
                "intent": f"confirmation: {transcript_clean}",
                "action_type": "respond", 
                "confidence": 0.90,
                "requires_screen_analysis": False, 
                "_category": "confirmation"
            }
        
        return None

    def classify_intent_fast(self, transcript: str) -> Optional[IntentCategory]:
        """Ultra-fast intent classification using keyword matching"""
        transcript_lower = transcript.lower().strip()
        
        # Skip very short or empty transcripts
        if len(transcript_lower) < 2:
            return None
        
        best_category = None
        best_score = 0.0
        
        for category, classifier in self.quick_classifiers.items():
            score = 0.0
            keyword_count = 0
            
            for keyword in classifier['keywords']:
                if keyword in transcript_lower:
                    # Exact keyword match
                    score += 1.0
                    keyword_count += 1
                elif any(part in transcript_lower for part in keyword.split()):
                    # Partial keyword match
                    score += 0.5
                    keyword_count += 1
            
            # Normalize score by number of keywords in category
            if keyword_count > 0:
                normalized_score = score / len(classifier['keywords'])
                
                # Boost score if multiple keywords match
                if keyword_count > 1:
                    normalized_score *= 1.5
                
                # Check confidence threshold
                if (normalized_score > classifier['confidence_threshold'] and 
                    normalized_score > best_score):
                    best_score = normalized_score
                    best_category = category
        
        if best_category:
            logger.info(f"Quick classification: {transcript[:50]} -> {best_category.value} (score: {best_score:.2f})")
        
        return best_category

    def get_optimized_model_for_category(self, category: IntentCategory) -> tuple[str, str]:
        """Select optimal model for specific intent category"""
        # Ultra-speed optimized model selection for reduced latency
        if category in [IntentCategory.GREETING, IntentCategory.SYSTEM_CONTROL]:
            # Simple tasks - use fastest model with compound-beta for speed
            return "groq", "compound-beta-mini"
        elif category in [IntentCategory.NAVIGATION, IntentCategory.UTILITY]:
            # Medium complexity - use fast flash model
            return "gemini", "gemini-2.5-flash-lite"
        elif category in [IntentCategory.UI_INTERACTION]:
            # UI interactions need accuracy - use balanced fast model
            return "gemini", "gemini-2.5-flash"
        else:
            # Complex tasks - quality model
            return "groq", "llama-3.3-70b-versatile"

    def build_optimized_prompt(self, transcript: str, category: IntentCategory, ui_tree: Optional[str] = None) -> Dict[str, Any]:
        """Build optimized prompt for specific intent category"""
        template = self.prompt_templates.get(category, self.general_template)
        
        # Prepare user message - keep it minimal for speed
        user_content = f"'{transcript}'"
        
        # Add UI context only if essential and available (max 800 chars for speed)
        if (category in [IntentCategory.UI_INTERACTION, IntentCategory.INFORMATION] and 
            ui_tree and len(ui_tree) > 30):
            # Aggressively truncate UI tree for maximum speed
            ui_context = ui_tree[:800] + "..." if len(ui_tree) > 800 else ui_tree
            user_content += f"\nUI: {ui_context}"
        
        return {
            "messages": [
                {"role": "system", "content": template.system_prompt},
                {"role": "user", "content": user_content}
            ],
            "max_tokens": template.max_tokens,
            "temperature": template.temperature,
            "response_format": {"type": "json_object"},
            "expected_fields": template.expected_fields
        }

    def validate_and_enhance_result(self, result: Dict[str, Any], category: IntentCategory) -> Dict[str, Any]:
        """Validate and enhance the LLM result based on category"""
        template = self.prompt_templates.get(category, self.general_template)
        
        # Ensure required fields exist
        for field in template.expected_fields:
            if field not in result:
                if field == "confidence":
                    result[field] = 0.5
                elif field == "requires_screen_analysis":
                    result[field] = category in [IntentCategory.UI_INTERACTION, IntentCategory.INFORMATION]
                elif field == "action_type":
                    result[field] = "tap"  # Safe default
                else:
                    result[field] = None
        
        # Category-specific enhancements
        if category == IntentCategory.GREETING:
            result["requires_screen_analysis"] = False
            result["action_type"] = "respond"
        elif category == IntentCategory.SYSTEM_CONTROL:
            result["requires_screen_analysis"] = False
        elif category == IntentCategory.UI_INTERACTION:
            result["requires_screen_analysis"] = True
        
        # Ensure confidence is reasonable
        if result.get("confidence", 0) < 0.3:
            result["confidence"] = 0.5  # Minimum reasonable confidence
        
        return result

    async def analyze_intent_optimized(
        self, 
        transcript: str, 
        ui_tree: Optional[str] = None,
        llm_service = None
    ) -> Dict[str, Any]:
        """
        Optimized intent analysis with fast classification and specialized prompts
        """
        start_time = time.time()
        
        if not transcript or not transcript.strip():
            return {
                "error": "Empty transcript",
                "confidence": 0.0,
                "requires_screen_analysis": False
            }
        
        transcript_clean = transcript.lower().strip()
        
        # Step 0: Check for instant responses (ultra-fast)
        instant_match = self._check_instant_response(transcript_clean)
        if instant_match:
            result = instant_match.copy()
            result["_analysis_time"] = time.time() - start_time
            result["_instant_response"] = True
            
            # Record performance
            if PERFORMANCE_TRACKING:
                performance_monitor.record_operation(
                    "intent_analysis",
                    start_time,
                    time.time(),
                    provider="instant",
                    model="pre_computed",
                    success=True,
                    confidence=result.get("confidence", 0.95),
                    instant_response=True,
                    category=result.get("_category", "unknown")
                )
            
            logger.info(f"Instant response for '{transcript_clean}': {result['intent']}")
            return result
        
        # Step 1: Check cache
        cache_key = transcript_clean[:50]  # Limit cache key size
        if cache_key in self.intent_cache:
            result = self.intent_cache[cache_key].copy()
            result["_analysis_time"] = time.time() - start_time
            result["_from_cache"] = True
            
            # Record performance
            if PERFORMANCE_TRACKING:
                performance_monitor.record_operation(
                    "intent_analysis",
                    start_time,
                    time.time(),
                    provider="cache",
                    model="cached",
                    success=True,
                    confidence=result.get("confidence", 0.8),
                    cache_hit=True,
                    category=result.get("_category", "unknown")
                )
            
            logger.info(f"Cache hit for '{transcript_clean[:30]}': {result['intent']}")
            return result
        
        try:
            # Step 1: Fast classification
            category = self.classify_intent_fast(transcript)
            
            if not category:
                # Fallback to general analysis for unclassified intents
                category = IntentCategory.UTILITY
                logger.info(f"Using general analysis for: {transcript[:50]}")
            
            # Step 2: Get optimal model for this category
            provider, model = self.get_optimized_model_for_category(category)
            
            # Step 3: Build optimized prompt
            prompt_config = self.build_optimized_prompt(transcript, category, ui_tree)
            
            # Step 4: Call LLM with optimized parameters
            if llm_service:
                response = await llm_service.chat_completion(
                    messages=prompt_config["messages"],
                    provider=provider,
                    model=model,
                    temperature=prompt_config["temperature"],
                    max_tokens=prompt_config["max_tokens"],
                    response_format=prompt_config.get("response_format")
                )
                
                if response.get("success"):
                    try:
                        result = json.loads(response["content"])
                        result = self.validate_and_enhance_result(result, category)
                        
                        # Add performance metadata
                        result["_analysis_time"] = time.time() - start_time
                        result["_category"] = category.value
                        result["_model_used"] = f"{provider}/{model}"
                        
                        # Cache the result for future use
                        if len(self.intent_cache) >= self.cache_max_size:
                            # Remove oldest entry
                            oldest_key = next(iter(self.intent_cache))
                            del self.intent_cache[oldest_key]
                        self.intent_cache[cache_key] = result.copy()
                        
                        # Record performance
                        if PERFORMANCE_TRACKING:
                            performance_monitor.record_operation(
                                "intent_analysis",
                                start_time,
                                time.time(),
                                provider=provider,
                                model=model,
                                success=True,
                                confidence=result.get("confidence", 0.5),
                                cache_hit=False,
                                category=category.value
                            )
                        
                        logger.info(f"Optimized intent analysis completed in {result['_analysis_time']:.3f}s: "
                                   f"category={category.value}, confidence={result.get('confidence', 0):.2f}")
                        
                        return result
                        
                    except json.JSONDecodeError as e:
                        logger.error(f"JSON decode error: {e}")
                        return self._create_fallback_result(transcript, category)
                else:
                    logger.error(f"LLM error: {response.get('error')}")
                    return self._create_fallback_result(transcript, category)
            else:
                logger.error("No LLM service available")
                return self._create_fallback_result(transcript, category)
                
        except Exception as e:
            logger.error(f"Intent analysis error: {e}")
            
            # Before falling back completely, try one more time to see if this is a simple greeting/command
            # that we can handle without LLM
            simple_response = self._get_simple_response(transcript)
            if simple_response:
                simple_response["_analysis_time"] = time.time() - start_time
                simple_response["_simple_fallback"] = True
                logger.info(f"Using simple fallback for '{transcript}': {simple_response['intent']}")
                return simple_response
            
            return self._create_fallback_result(transcript, IntentCategory.UTILITY)

    def _get_simple_response(self, transcript: str) -> Optional[Dict[str, Any]]:
        """Get simple response for common patterns without LLM"""
        transcript_lower = transcript.lower().strip()
        
        # Capability questions (very flexible)
        capability_indicators = ["what can you do", "what do you do", "help me", "how can you help", 
                               "what are your capabilities", "what are you capable of", "assist me"]
        if any(phrase in transcript_lower for phrase in capability_indicators):
            return {
                "intent": "explain capabilities",
                "action_type": "respond",
                "confidence": 0.90,
                "requires_screen_analysis": False,
                "_category": "help",
                "_response_type": "capabilities_explanation"
            }
        
        # Greetings (very flexible)
        greeting_indicators = ["hello", "hi", "hey", "good morning", "good afternoon", "good evening"]
        if any(transcript_lower.startswith(word) for word in greeting_indicators):
            # Check if it's a greeting with capability question
            if any(word in transcript_lower for word in ["what", "can", "do", "help", "capable"]):
                return {
                    "intent": "greeting with capability inquiry",
                    "action_type": "respond",
                    "confidence": 0.88,
                    "requires_screen_analysis": False,
                    "_category": "help",
                    "_response_type": "greeting_with_capabilities"
                }
            else:
                return {
                    "intent": "greeting",
                    "action_type": "respond",
                    "confidence": 0.85,
                    "requires_screen_analysis": False,
                    "_category": "greeting",
                    "_response_type": "simple_greeting"
                }
        
        # Navigation
        if any(word in transcript_lower for word in ["back", "home", "return", "previous"]):
            return {
                "intent": "navigation command",
                "action_type": "navigate", 
                "confidence": 0.75,
                "requires_screen_analysis": False,
                "_category": "navigation"
            }
        
        # App opening
        if "open" in transcript_lower:
            return {
                "intent": "open application",
                "action_type": "open_app",
                "confidence": 0.70,
                "requires_screen_analysis": False,
                "_category": "app_control"
            }
        
        return None

    def _create_fallback_result(self, transcript: str, category: IntentCategory) -> Dict[str, Any]:
        """Create a fallback result when LLM analysis fails"""
        return {
            "intent": f"Process user request: {transcript[:100]}",
            "action_type": "tap",
            "confidence": 0.3,
            "requires_screen_analysis": True,
            "target_elements": [],
            "parameters": {},
            "_category": category.value,
            "_fallback": True
        }

# Global instance for use in the application
optimized_intent_analyzer = OptimizedIntentAnalyzer()
