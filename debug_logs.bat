@echo off
echo Starting AURA Debug Logging...
echo Press Ctrl+C to stop logging
echo.
adb logcat -c
adb logcat -v time | findstr "AURA\|EnhancedVoiceService\|VoiceManager\|EventManager\|SystemManager\|AIManager\|com.aura.aura_mark3"
