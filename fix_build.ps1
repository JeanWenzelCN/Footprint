
Write-Host "Starting repair process..." -ForegroundColor Cyan

# Define local SDK paths
$sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
$adbPath = Join-Path $sdkPath "platform-tools\adb.exe"

if (-not (Test-Path $adbPath)) {
    Write-Host "Error: adb.exe not found at $adbPath" -ForegroundColor Red
    Write-Host "Trying to find adb in PATH..." -ForegroundColor Yellow
    $adbPath = "adb"
} else {
    Write-Host "Found adb at: $adbPath" -ForegroundColor Green
}

# 1. Stop all Java processes to release file locks
Write-Host "Stopping Java processes..." -ForegroundColor Yellow
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# 2. Force delete build folders and .gradle cache
$appBuildDir = "e:\Code\IDE\AndroidStudio\Footprint\app\build"
$rootBuildDir = "e:\Code\IDE\AndroidStudio\Footprint\build"
$gradleCacheDir = "e:\Code\IDE\AndroidStudio\Footprint\.gradle"

if (Test-Path $appBuildDir) {
    Write-Host "Deleting app build directory: $appBuildDir" -ForegroundColor Yellow
    Remove-Item -Path $appBuildDir -Recurse -Force -ErrorAction SilentlyContinue
}
if (Test-Path $rootBuildDir) {
    Write-Host "Deleting root build directory: $rootBuildDir" -ForegroundColor Yellow
    Remove-Item -Path $rootBuildDir -Recurse -Force -ErrorAction SilentlyContinue
}
if (Test-Path $gradleCacheDir) {
    Write-Host "Deleting project .gradle cache: $gradleCacheDir" -ForegroundColor Yellow
    Remove-Item -Path $gradleCacheDir -Recurse -Force -ErrorAction SilentlyContinue
}

# 3. Clean project
Write-Host "Running Gradle Clean..." -ForegroundColor Yellow
./gradlew clean

# 4. Uninstall app (try)
Write-Host "Uninstalling old app version..." -ForegroundColor Yellow
if ($adbPath -eq "adb") {
    adb uninstall com.footprint
} else {
    & $adbPath uninstall com.footprint
}

# 5. Rebuild and Iinstall
Write-Host "Building and Installing..." -ForegroundColor Yellow
./gradlew installDebug

Write-Host "Done! Please verify fix." -ForegroundColor Green
