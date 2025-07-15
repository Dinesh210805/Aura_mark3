@echo off
REM AURA Backend Agent Test Script for Windows

echo 🧪 Testing AURA Backend Agent...

set BASE_URL=http://localhost:8000

echo Checking if server is running at %BASE_URL%...
curl -s %BASE_URL%/ >nul 2>&1
if errorlevel 1 (
    echo ❌ Server is not running at %BASE_URL%
    echo Please start the server with: python run.py
    exit /b 1
)

echo ✅ Server is running
echo.

echo 🔍 Testing root endpoint...
curl -s %BASE_URL%/
echo.

echo 🔍 Testing health endpoint...
curl -s %BASE_URL%/health
echo.

echo 🔍 Testing graph info endpoint...
curl -s %BASE_URL%/graph/info
echo.

echo 🔍 Testing chat endpoint...
curl -s -X POST %BASE_URL%/chat ^
    -H "Content-Type: application/json" ^
    -d "{\"text\": \"Hello, can you help me?\", \"session_id\": \"test\"}"
echo.

echo.
echo 📊 Test Results Summary:
echo - All basic endpoints tested
echo - For full testing, upload audio/image files to /process endpoint
echo.
echo 💡 Next Steps:
echo 1. Test with actual audio files using the /process endpoint
echo 2. Check logs for detailed processing information  
echo 3. Visit http://localhost:8000/docs for interactive API testing

pause
