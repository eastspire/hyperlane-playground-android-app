# Simple Android Environment Setup

Write-Host "Android Environment Setup" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

# Find Android SDK
$sdkPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"

if (Test-Path $sdkPath) {
    Write-Host "[OK] Found Android SDK" -ForegroundColor Green
    
    # Add ADB to PATH
    $platformTools = "$sdkPath\platform-tools"
    if (Test-Path $platformTools) {
        $env:Path += ";$platformTools"
        Write-Host "[OK] ADB configured" -ForegroundColor Green
    }
} else {
    Write-Host "[ERROR] Android SDK not found at: $sdkPath" -ForegroundColor Red
    Write-Host "Please install Android Studio first" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit
}

# Download Gradle Wrapper
$wrapperJar = "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    Write-Host ""
    Write-Host "Downloading Gradle Wrapper..." -ForegroundColor Yellow
    
    $wrapperDir = "gradle\wrapper"
    if (-not (Test-Path $wrapperDir)) {
        New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
    }
    
    $url = "https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar"
    
    try {
        Invoke-WebRequest -Uri $url -OutFile $wrapperJar -UseBasicParsing
        Write-Host "[OK] Gradle Wrapper downloaded" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Download failed" -ForegroundColor Red
        Write-Host "Please use Android Studio to open project" -ForegroundColor Yellow
    }
} else {
    Write-Host "[OK] Gradle Wrapper exists" -ForegroundColor Green
}

# Check devices
Write-Host ""
Write-Host "Checking devices..." -ForegroundColor Cyan
adb devices

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "  .\gradlew.bat installDebug" -ForegroundColor Cyan
Write-Host "  adb shell am start -n com.example.demoapp/.MainActivity" -ForegroundColor Cyan
Write-Host ""

Read-Host "Press Enter to continue"
