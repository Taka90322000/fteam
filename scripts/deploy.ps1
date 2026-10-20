
$ErrorActionPreference = "Stop"

# Tomcatの配置先を取得
$tomcat = $env:CATALINA_HOME

if (-not $tomcat) {
    throw "CATALINA_HOMEが設定されていません"
}

$webapps = Join-Path $tomcat "webapps"

if (-not (Test-Path $webapps)) {
    throw "Tomcatのwebappsが見つかりません"
}

# プロジェクトのルートに移動
Set-Location (Join-Path $PSScriptRoot "..")

# Javaをコンパイルして展開形式で配置
Write-Host "Building application..." -ForegroundColor Cyan

& mvn compile war:exploded

if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed."
}

Write-Host "Build completed!" -ForegroundColor Green


# Tomcatにデプロイ
$war = "target\machiriku.war"
$destination = Join-Path $webapps "machiriku.war"

Copy-Item -LiteralPath $war -Destination $destination -Force

Write-Host "デプロイ完了！" -ForegroundColor Green


# Tomcatの起動確認
$port = 8080

$running = Get-NetTCPConnection `
    -LocalPort $port `
    -State Listen `
    -ErrorAction SilentlyContinue

if (-not $running) {
    Write-Host "Starting Tomcat..." -ForegroundColor Cyan

    $startup = Join-Path $tomcat "bin\startup.bat"
    & $startup
} else {
    Write-Host "Port 8080 is already in use." -ForegroundColor Yellow
}

# アプリケーションの起動を待機
$url = "http://localhost:8080/machiriku/hello"
$ready = $false

for ($i = 0; $i -lt 30; $i++) {
    try {
        $response = Invoke-WebRequest `
            -Uri $url `
            -UseBasicParsing `
            -TimeoutSec 2

        if ($response.StatusCode -eq 200) {
            $ready = $true
            break
        }
    }
    catch {
        Start-Sleep -Seconds 1
    }
}

if (-not $ready) {
    throw "Application startup failed: $url"
}

# ブラウザを開く
Write-Host "Opening browser..." -ForegroundColor Green
Start-Process $url
