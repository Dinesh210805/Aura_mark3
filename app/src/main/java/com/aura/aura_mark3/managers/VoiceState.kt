package com.aura.aura_mark3.managers

/**
 * Represents the current state of the voice system
 */
enum class VoiceState {
    IDLE,                    // Not doing anything
    LISTENING_WAKE_WORD,     // Listening for "Hey Aura"
    RECORDING_MANUAL,        // User pressed button to record
    RECORDING_COMMAND,       // Recording after wake word detected
    SPEAKING                 // AURA is currently speaking
}

/**
 * Status information for recording button UI
 */
data class RecordingStatus(
    val state: VoiceState,
    val buttonText: String,
    val canStartRecording: Boolean,
    val canStopRecording: Boolean,
    val statusMessage: String
) {
    companion object {
        fun fromState(
            voiceState: VoiceState,
            isSpeaking: Boolean = false,
            isManualRecording: Boolean = false,
            isWakeWordListening: Boolean = false,
            isCommandRecording: Boolean = false
        ): RecordingStatus {
            
            val actualState = when {
                isSpeaking -> VoiceState.SPEAKING
                isManualRecording -> VoiceState.RECORDING_MANUAL
                isCommandRecording -> VoiceState.RECORDING_COMMAND
                isWakeWordListening -> VoiceState.LISTENING_WAKE_WORD
                else -> VoiceState.IDLE
            }
            
            return when (actualState) {
                VoiceState.IDLE -> RecordingStatus(
                    state = actualState,
                    canStartRecording = true,
                    canStopRecording = false,
                    buttonText = "Tap to Talk",
                    statusMessage = "Ready! Say 'Hey Aura' or tap to talk"
                )
                VoiceState.RECORDING_MANUAL -> RecordingStatus(
                    state = actualState,
                    canStartRecording = false,
                    canStopRecording = true,
                    buttonText = "Stop Recording",
                    statusMessage = "🎤 Recording... Speak now"
                )
                VoiceState.LISTENING_WAKE_WORD -> RecordingStatus(
                    state = actualState,
                    canStartRecording = true,
                    canStopRecording = false,
                    buttonText = "Tap to Talk",
                    statusMessage = "👂 Listening for 'Hey Aura'..."
                )
                VoiceState.RECORDING_COMMAND -> RecordingStatus(
                    state = actualState,
                    canStartRecording = false,
                    canStopRecording = true,
                    buttonText = "Stop Recording",
                    statusMessage = "🗣️ Recording command..."
                )
                VoiceState.SPEAKING -> RecordingStatus(
                    state = actualState,
                    canStartRecording = false,
                    canStopRecording = false,
                    buttonText = "AURA Speaking",
                    statusMessage = "🗣️ AURA is responding..."
                )
            }
        }
    }
}
