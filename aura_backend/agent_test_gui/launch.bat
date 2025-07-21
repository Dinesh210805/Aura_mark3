@echo off
REM AURA Agent Test GUI Launcher for Windows

echo 🚀 Starting AURA Agent Test GUI...
echo ==================================

REM Check if virtual environment exists
if not exist venv (
    echo 📦 Creating virtual environment...
    python -m venv venv
)

REM Activate virtual environment
echo 🔧 Activating virtual environment...
call venv\Scripts\activate.bat

REM Install dependencies
echo 📚 Installing dependencies...
pip install -r requirements.txt

REM Check if AURA backend is running
echo 🔍 Checking AURA backend status...
curl -s http://localhost:8000/health >nul 2>&1
if %errorlevel%==0 (
    echo ✅ AURA backend is running
) else (
    echo ⚠️  AURA backend not detected
    echo 💡 Start it with: cd ../ ^&^& python run.py
    echo.
)

REM Launch Streamlit app
echo 🌐 Launching Streamlit app...
echo 📱 Open http://localhost:8501 in your browser
echo.

streamlit run streamlit_app.py
