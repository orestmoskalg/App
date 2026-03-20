# Build AAB for Google Play Store. Run: .\build_playstore.ps1

$ErrorActionPreference = "Stop"
$projectDir = $PSScriptRoot
$desktopPath = [Environment]::GetFolderPath("Desktop")

# Read version from build.gradle.kts
$gradleContent = Get-Content (Join-Path $projectDir "app\build.gradle.kts") -Raw
$versionName = "1.1"
if ($gradleContent -match 'versionName = "([^"]+)"') {
    $versionName = $matches[1]
}

$outputDir = Join-Path $desktopPath "PlayStore_MyApplication2_v$versionName"

Write-Host "Building AAB for Play Store (v$versionName)..." -ForegroundColor Cyan
Set-Location $projectDir

# Build
& .\gradlew bundleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Create versioned folder on Desktop
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

# Copy AAB
$aabPath = Join-Path $projectDir "app\build\outputs\bundle\release\app-release.aab"
$destAab = Join-Path $outputDir "MyApplication2-release-v$versionName.aab"

Copy-Item $aabPath $destAab -Force
Write-Host "AAB copied: $destAab" -ForegroundColor Green

# Copy instructions
$instructionsPath = Join-Path $projectDir "PLAY_STORE_ІНСТРУКЦІЯ.md"
if (Test-Path $instructionsPath) {
    Copy-Item $instructionsPath (Join-Path $outputDir "PLAY_STORE_ІНСТРУКЦІЯ.md") -Force
}

Write-Host "`nDone! Files on Desktop: $outputDir" -ForegroundColor Green
explorer $outputDir
