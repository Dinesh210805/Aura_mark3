from groq.stt import groq_stt
import logging
import time
import time

logger = logging.getLogger(__name__)

class STTNode:
    """Speech-to-Text node using Groq Whisper"""
    
    def __init__(self):
        self.name = "stt"
    
    async def run(self, state: dict) -> dict:
        """Process audio input and convert to text"""
        start_time = time.time()
        logger.info("STT Node: Starting speech-to-text processing")
        
        # Check if transcript is already provided (text-only mode)
        if state.get("transcript") and not state.get("_audio_bytes"):
            logger.info("STT Node: Text already provided, skipping STT processing")
            return {
                **state,
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
        
        # Check if audio data is available
        audio_data = state.get("_audio_bytes")
        if not audio_data:
            logger.error("STT Node: No audio data provided")
            return {
                **state,
                "error": "No audio data provided",
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }
            
        try:
            # Perform speech-to-text conversion
            transcript = await groq_stt.transcribe(audio_data)
            
            if transcript:
                logger.info(f"STT Node: Transcription successful - '{transcript[:100]}...'")
                return {
                    **state,
                    "transcript": transcript,
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
            else:
                logger.error("STT Node: Failed to transcribe audio")
                return {
                    **state,
                    "error": "STT failed to transcribe audio",
                    "node_execution_times": {
                        **state.get("node_execution_times", {}),
                        self.name: time.time() - start_time
                    }
                }
                
        except Exception as e:
            logger.error(f"STT Node error: {str(e)}")
            return {
                **state,
                "error": f"STT processing failed: {str(e)}",
                "node_execution_times": {
                    **state.get("node_execution_times", {}),
                    self.name: time.time() - start_time
                }
            }

# Create node instance
stt_node = STTNode()
