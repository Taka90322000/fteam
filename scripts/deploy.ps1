# -*- coding: utf-8 -*-
# 展開形式のWebアプリをビルドし、必要な場合にTomcatを起動して応答を確認する。
# Windows PowerShell 5.1でも日本語を読み取れるよう、UTF-8（BOM付き）で保存する。

param(
    [switch]$NoBrowser,
    [string]$HealthPath = '/machiriku/index.jsp',
    [string]$ActiveFile
)

# 必要な設定やビルドに失敗した場合は、後続の起動処理へ進まず停止する。
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

# 誤った配置先へデプロイしないよう、Tomcat側のコンテキスト設定を確認する。
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

# Javaをコンパイルし、Tomcatが参照するtarget/machirikuへWebアプリを展開する。
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

# ここでは8080番ポートの待受だけを確認する。Tomcatのものとは限らないため、後続でアプリのURLへの応答を確認する。
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

# 起動には時間がかかるため、設定された確認用URLが200を返すまで繰り返し待機する。
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
# 起動確認用URLと、編集していたファイルに対応する表示先URLを分けて扱う。
if (-not $NoBrowser) {
    $browserUrl = "http://localhost:8080$browserPath"
    Write-Host "Opening: $browserUrl" -ForegroundColor Cyan
    Start-Process $browserUrl
}
