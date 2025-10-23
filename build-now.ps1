# Quick Build and Install

Write-Host "Building Android App" -ForegroundColor Cyan
Write-Host "====================" -ForegroundColor Cyan
Write-Host ""

# Configure ADB
$sdkPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
$platformTools = "$sdkPath\platform-tools"
if (Test-Path $platformTools) {
    $env:Path += ";$platformTools"
}

# Build and install
Write-Host "Building..." -ForegroundColor Cyan
.\gradlew.bat installDebug --quiet

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host ""
    
    # Launch app
    Write-Host "Launching app..." -ForegroundColor Cyan
    adb shell am start -n com.example.demoapp/.MainActivity
    
    Write-Host ""
    Write-Host "App is now running on your device!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Features available:" -ForegroundColor White
    Write-Host "  - Button Demo" -ForegroundColor Gray
    Write-Host "  - Dialog Demo" -ForegroundColor Gray
    Write-Host "  - Input Demo" -ForegroundColor Gray
    Write-Host "  - List Demo" -ForegroundColor Gray
    Write-Host "  - Media Demo (image only, audio/video need files)" -ForegroundColor Gray
} else {
    Write-Host ""
    Write-Host "BUILD FAILED" -ForegroundColor Red
    Write-Host "Check error messages above" -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Press Enter to continue"
