@echo off
setlocal
cd /d "%~dp0"
python "%~dp0build_all.py" %*
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed with code %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
