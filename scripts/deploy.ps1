# -*- coding: utf-8 -*-
# 展開形式のWebアプリを別の場所でビルドし、Tomcatの参照先を切り替える。

param(
    [switch]$NoBrowser,
    [string]$HealthPath = '/machiriku/index.jsp',
    [string]$ActiveFile
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'resolve-page.ps1')
. (Join-Path $PSScriptRoot 'dev-context.ps1')
if (-not $HealthPath.StartsWith('/')) {
    throw "HealthPath must begin with / : $HealthPath"
}
$browserPath = Get-ActivePagePath -ProjectRoot $projectRoot -ActiveFile $ActiveFile -FallbackPath $HealthPath

$tomcatHome = $env:CATALINA_HOME
if ([string]::IsNullOrWhiteSpace($tomcatHome)) {
    throw 'CATALINA_HOME is not set. Restart VS Code after updating environment variables.'
}
$startup = Join-Path $tomcatHome 'bin\startup.bat'
if (-not (Test-Path -LiteralPath $startup)) {
    throw "startup.bat not found: $startup"
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'mvn not found on PATH.'
}

$contextFile = Join-Path $tomcatHome 'conf\Catalina\localhost\machiriku.xml'
$url = "http://localhost:8080$HealthPath"
$deployRoot = Join-Path $projectRoot 'target\dev-deploy'
New-Item -ItemType Directory -Path $deployRoot -Force | Out-Null

# 同じ作業ツリーで同時に配備しても、コンテキストの切替処理が重ならないようにする。
$deployLock = [System.IO.File]::Open((Join-Path $deployRoot 'deploy.lock'), 'OpenOrCreate', 'Write', 'None')
try {
    $config = Read-MachirikuContext -ContextFile $contextFile -ProjectRoot $projectRoot
    $previousDocBase = $config.DocumentElement.GetAttribute('docBase').Replace('\', '/')
    $buildId = [guid]::NewGuid().ToString('N')
    $buildDirectory = Join-Path $deployRoot $buildId
    $docBase = (Join-Path $buildDirectory 'machiriku').Replace('\', '/')

    # 実行中のファイルから離れた場所でコンパイルと展開を完了する。
    Push-Location $projectRoot
    try {
        Write-Host 'Building Maven exploded application...' -ForegroundColor Cyan
        & mvn "-Dmachiriku.build.directory=$buildDirectory" compile war:exploded
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed with code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
    if (-not (Test-Path -LiteralPath (Join-Path $docBase 'WEB-INF\classes') -PathType Container) -or
        -not (Test-Path -LiteralPath (Join-Path $docBase 'WEB-INF\views') -PathType Container)) {
        throw "Exploded web application is incomplete: $docBase"
    }
    $markerName = '__machiriku_dev_build.txt'
    [System.IO.File]::WriteAllText((Join-Path $docBase $markerName), $buildId)

    # 旧設定のソース直参照を外し、切替後はクラスとJSPを同じビルドから読む。
    $source = (Join-Path $projectRoot 'src\main\webapp').Replace('\', '/')
    foreach ($overlay in @($config.SelectNodes('/Context/Resources/PreResources[@webAppMount="/" and @internalPath="/"]'))) {
        if ($overlay.GetAttribute('base').Replace('\', '/') -eq $source) {
            [void]$overlay.ParentNode.RemoveChild($overlay)
        }
    }
    $config.DocumentElement.SetAttribute('docBase', $docBase)
    $stagedContext = "$contextFile.$buildId.new"
    $previousContext = "$contextFile.$buildId.backup"
    $config.Save($stagedContext)
    [System.IO.File]::Replace($stagedContext, $contextFile, $previousContext)

    try {
        if (-not (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
            Write-Host 'Starting Tomcat...' -ForegroundColor Cyan
            & $startup
            if ($LASTEXITCODE -ne 0) {
                throw "Tomcat startup.bat failed with code $LASTEXITCODE"
            }
        }

        # 配備前のアプリの200応答を、新しいビルドの起動成功と見なさない。
        $markerUrl = "http://localhost:8080/machiriku/$($markerName)?build=$buildId"
        $ready = $false
        $lastError = 'Waiting for Tomcat to load the new build.'
        for ($i = 0; $i -lt 60; $i++) {
            try {
                $marker = Invoke-WebRequest -Uri $markerUrl -UseBasicParsing -TimeoutSec 3
                if ($marker.StatusCode -eq 200 -and $marker.Content.Trim() -eq $buildId) {
                    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
                    if ($response.StatusCode -eq 200) {
                        $ready = $true
                        break
                    }
                }
            }
            catch {
                $lastError = $_.Exception.Message
            }
            Start-Sleep -Seconds 1
        }
        if (-not $ready) {
            throw "New application startup check failed: $url. Last error: $lastError"
        }
    }
    catch {
        $failedContext = "$contextFile.$buildId.failed"
        [System.IO.File]::Replace($previousContext, $contextFile, $failedContext)
        Remove-Item -LiteralPath $failedContext -ErrorAction SilentlyContinue
        Write-Warning 'Restored the previous Tomcat context.'
        throw
    }
    Remove-Item -LiteralPath $previousContext -ErrorAction SilentlyContinue

    # 現行版と直前の版を残し、このスクリプト用の古い出力を整理する。
    $deployRootItem = Get-Item -LiteralPath $deployRoot
    if (-not ($deployRootItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -and
        -not ($deployRootItem.Parent.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
        foreach ($oldBuild in Get-ChildItem -LiteralPath $deployRootItem.FullName -Directory) {
            if ($oldBuild.Name -notmatch '^[0-9a-f]{32}$' -or
                ($oldBuild.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) { continue }
            $oldPath = (Resolve-Path -LiteralPath $oldBuild.FullName).ProviderPath
            if ((Split-Path -Parent $oldPath) -ne $deployRootItem.FullName) { continue }
            $oldDocBase = (Join-Path $oldPath 'machiriku').Replace('\', '/')
            if ($oldDocBase -eq $docBase -or $oldDocBase -eq $previousDocBase) { continue }
            Remove-Item -LiteralPath $oldPath -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}
finally {
    $deployLock.Dispose()
}

Write-Host "Application ready: $url" -ForegroundColor Green
# 起動確認用URLと、アクティブファイルのURLを別に扱う。
if (-not $NoBrowser) {
    $browserUrl = "http://localhost:8080$browserPath"
    Write-Host "Opening: $browserUrl" -ForegroundColor Cyan
    Start-Process $browserUrl
}