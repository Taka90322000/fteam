# -*- coding: utf-8 -*-
# マチリクのローカル開発用に、Tomcatのコンテキスト設定を作成する。
# Windows PowerShell 5.1でも日本語を読み取れるよう、UTF-8（BOM付き）で保存する。

# Tomcat・Maven・Javaとプロジェクト構成を確認し、不備があれば設定を作成しない。
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'dev-context.ps1')
$tomcatHome = $env:CATALINA_HOME

if ([string]::IsNullOrWhiteSpace($tomcatHome)) {
    throw 'CATALINA_HOME is not set. Set it to the Tomcat installation folder, then restart VS Code.'
}
if (-not (Test-Path (Join-Path $tomcatHome 'bin\startup.bat'))) {
    throw "Tomcat startup.bat not found: $tomcatHome"
}
if (-not (Test-Path (Join-Path $projectRoot 'pom.xml'))) {
    throw "pom.xml not found: $projectRoot"
}
if (-not (Test-Path (Join-Path $projectRoot 'src\main\webapp'))) {
    throw "src/main/webapp not found: $projectRoot"
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'mvn not found. Install Maven and add its bin directory to PATH.'
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java not found. Install JDK 21 and check JAVA_HOME / PATH.'
}

# 初回配備で更新するコンテキストを用意する。
$docBase = (Join-Path $projectRoot 'target\machiriku').Replace('\', '/')
$contextFile = Join-Path $tomcatHome 'conf\Catalina\localhost\machiriku.xml'

# 既存の設定は内容が一致する場合だけ再利用し、別プロジェクトの設定を上書きしない。
if (Test-Path $contextFile) {
    Read-MachirikuContext -ContextFile $contextFile -ProjectRoot $projectRoot | Out-Null
    Write-Host "Tomcat context already configured: $contextFile" -ForegroundColor Green
}
else {
    $contextDir = Split-Path -Path $contextFile -Parent
    New-Item -ItemType Directory -Path $contextDir -Force | Out-Null

    # XML宣言が先頭に来るよう、XmlDocumentとXmlWriterで設定ファイルを組み立てる。
    $doc = New-Object System.Xml.XmlDocument
    [void]$doc.AppendChild($doc.CreateXmlDeclaration('1.0', 'UTF-8', $null))

    $context = $doc.CreateElement('Context')
    $context.SetAttribute('docBase', $docBase)
    $context.SetAttribute('reloadable', 'true')
    [void]$doc.AppendChild($context)

    $settings = [System.Xml.XmlWriterSettings]::new()
    $settings.Encoding = [System.Text.UTF8Encoding]::new($false)
    $settings.Indent = $true
    $writer = [System.Xml.XmlWriter]::Create($contextFile, $settings)
    try {
        $doc.Save($writer)
    }
    finally {
        $writer.Dispose()
    }
    Write-Host "Tomcat context created: $contextFile" -ForegroundColor Green
}

# 古いWARや展開済みアプリが残っている場合は、配置先の競合を避けるため案内する。
$oldWar = Join-Path $tomcatHome 'webapps\machiriku.war'
$oldExpanded = Join-Path $tomcatHome 'webapps\machiriku'
foreach ($item in @($oldWar, $oldExpanded)) {
    if (Test-Path $item) {
        Write-Warning "Old deployment found: $item . Stop Tomcat, back it up, and remove it before using exploded development mode."
    }
}

Write-Host 'Setup completed. Use Ctrl+Shift+B in VS Code to build and launch.' -ForegroundColor Cyan
