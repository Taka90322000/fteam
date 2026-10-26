# -*- coding: utf-8 -*-
# Build exploded webapp, launch Tomcat if needed, then check the endpoint.
# Save as UTF-8 with BOM for Windows PowerShell 5.1.

param(
    [switch]$NoBrowser,
    [string]$HealthPath = '/machiriku/hello',
    [string]$ActiveFile
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'resolve-page.ps1')
$browserPath = Get-ActivePagePath -ProjectRoot $projectRoot -ActiveFile $ActiveFile -FallbackPath $HealthPath
$tomcatHome = $env:CATALINA_HOME

if ([string]::IsNullOrWhiteSpace($tomcatHome)) {
    throw 'CATALINA_HOME is not set. Restart VS Code after updating environment variables.'
}
$startup = Join-Path $tomcatHome 'bin\startup.bat'
if (-not (Test-Path $startup)) {
    throw "startup.bat not found: $startup"
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'mvn not found on PATH.'
}

$contextFile = Join-Path $tomcatHome 'conf\Catalina\localhost\machiriku.xml'
if (-not (Test-Path $contextFile)) {
    throw "machiriku.xml not found. Run scripts/setup-dev.ps1 first: $contextFile"
}

$expectedDocBase = (Join-Path $projectRoot 'target\machiriku').Replace('\', '/')
try {
    [xml]$config = Get-Content -LiteralPath $contextFile -Raw -Encoding UTF8
}
catch {
    throw "machiriku.xml is not valid XML: $($_.Exception.Message)"
}
$configuredDocBase = $config.DocumentElement.GetAttribute('docBase').Replace('\', '/')
if ($configuredDocBase -ne $expectedDocBase) {
    throw "docBase mismatch: $configuredDocBase . Expected: $expectedDocBase . Run setup after fixing/removing old context."
}

Push-Location $projectRoot
try {
    Write-Host 'Building Maven exploded application...' -ForegroundColor Cyan
    & mvn compile war:exploded
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

$explodedWebInf = Join-Path $projectRoot 'target\machiriku\WEB-INF'
if (-not (Test-Path $explodedWebInf)) {
    throw "Exploded web application not found: $explodedWebInf . Check pom.xml <finalName>machiriku</finalName>."
}

# This tests only whether port 8080 is being listened to, not whether it belongs to Tomcat.
$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    Write-Host 'Starting Tomcat...' -ForegroundColor Cyan
    & $startup
    if ($LASTEXITCODE -ne 0) {
        throw "Tomcat startup.bat failed with code $LASTEXITCODE"
    }
}
else {
    Write-Host 'Port 8080 is already in use. Checking the application URL...' -ForegroundColor Yellow
}

if (-not $HealthPath.StartsWith('/')) {
    throw "HealthPath must begin with / : $HealthPath"
}
$url = "http://localhost:8080$HealthPath"
$ready = $false
$lastError = ''

for ($i = 0; $i -lt 60; $i++) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200) {
            $ready = $true
            break
        }
    }
    catch {
        $lastError = $_.Exception.Message
    }
    Start-Sleep -Seconds 1
}

if (-not $ready) {
    throw "Application startup check failed: $url`nLast error: $lastError`nCheck Tomcat logs and confirm the health endpoint exists."
}

Write-Host "Application ready: $url" -ForegroundColor Green
if (-not $NoBrowser) {
    $browserUrl = "http://localhost:8080$browserPath"
    Write-Host "Opening: $browserUrl" -ForegroundColor Cyan
    Start-Process $browserUrl
}
