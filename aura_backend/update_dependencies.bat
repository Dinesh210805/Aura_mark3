@echo off
echo ==========================================
echo  AURA Backend - Dependency Update Script
echo ==========================================

echo.
echo [1/3] Upgrading pip...
python -m pip install --upgrade pip

echo.
echo [2/3] Installing updated dependencies...
pip install -r requirements.txt --upgrade

echo.
echo [3/3] Checking for potential issues...
pip check

echo.
echo ==========================================
echo  Update Complete!
echo ==========================================
echo.
echo Next steps:
echo 1. Restart the AURA backend server
echo 2. Test the agent functionality
echo 3. Check that LangChain warnings are resolved
echo.
pause
