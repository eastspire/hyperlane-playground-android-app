@echo off
echo Building Android App...
echo.
gradlew.bat clean assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo.
    echo BUILD SUCCESSFUL!
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo BUILD FAILED!
)
pause
