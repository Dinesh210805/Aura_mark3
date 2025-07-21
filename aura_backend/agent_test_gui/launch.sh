#!/bin/bash
# AURA Agent Test GUI Launcher

echo "🚀 Starting AURA Agent Test GUI..."
echo "=================================="

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python -m venv venv
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "📚 Installing dependencies..."
pip install -r requirements.txt

# Check if AURA backend is running
echo "🔍 Checking AURA backend status..."
if curl -s http://localhost:8000/health > /dev/null; then
    echo "✅ AURA backend is running"
else
    echo "⚠️  AURA backend not detected"
    echo "💡 Start it with: cd ../ && python run.py"
    echo ""
fi

# Launch Streamlit app
echo "🌐 Launching Streamlit app..."
echo "📱 Open http://localhost:8501 in your browser"
echo ""

streamlit run streamlit_app.py
