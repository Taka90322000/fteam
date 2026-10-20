# -*- coding: utf-8 -*-
# Local Tomcat configuration generator for Machiriku.
# This file is UTF-8 with BOM so Windows PowerShell 5.1 can read Japanese if added.

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
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

$docBase = (Join-Path $projectRoot 'target\machiriku').Replace('\', '/')
$webSource = (Join-Path $projectRoot 'src\main\webapp').Replace('\', '/')
$contextFile = Join-Path $tomcatHome 'conf\Catalina\localhost\machiriku.xml'

if (Test-Path $contextFile) {
    try {
        [xml]$existing = Get-Content -LiteralPath $contextFile -Raw -Encoding UTF8
    }
    catch {
        throw "Existing machiriku.xml is invalid. Correct or back it up first: $contextFile`n$($_.Exception.Message)"
    }
    $currentDocBase = $existing.DocumentElement.GetAttribute('docBase').Replace('\', '/')
    $preNode = $existing.SelectSingleNode('/Context/Resources/PreResources')
    $currentWebSource = if ($null -ne $preNode) { $preNode.GetAttribute('base').Replace('\', '/') } else { '' }
    if (($currentDocBase -ne $docBase) -or ($currentWebSource -ne $webSource)) {
        throw "Existing machiriku.xml points somewhere else or lacks PreResources. Not overwritten. Back it up or remove it while Tomcat is stopped, then run setup again: $contextFile"
    }
    Write-Host "Tomcat context already configured: $contextFile" -ForegroundColor Green
}
else {
    $contextDir = Split-Path -Path $contextFile -Parent
    New-Item -ItemType Directory -Path $contextDir -Force | Out-Null

    # Construct XML using XmlDocument/XmlWriter. XML declaration will be first.
    $doc = New-Object System.Xml.XmlDocument
    [void]$doc.AppendChild($doc.CreateXmlDeclaration('1.0', 'UTF-8', $null))

    $context = $doc.CreateElement('Context')
    $context.SetAttribute('docBase', $docBase)
    $context.SetAttribute('reloadable', 'true')
    [void]$doc.AppendChild($context)

    $resources = $doc.CreateElement('Resources')
    $resources.SetAttribute('className', 'org.apache.catalina.webresources.StandardRoot')
    [void]$context.AppendChild($resources)

    $preResources = $doc.CreateElement('PreResources')
    $preResources.SetAttribute('className', 'org.apache.catalina.webresources.DirResourceSet')
    $preResources.SetAttribute('base', $webSource)
    $preResources.SetAttribute('webAppMount', '/')
    $preResources.SetAttribute('internalPath', '/')
    $preResources.SetAttribute('readOnly', 'true')
    [void]$resources.AppendChild($preResources)

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

$oldWar = Join-Path $tomcatHome 'webapps\machiriku.war'
$oldExpanded = Join-Path $tomcatHome 'webapps\machiriku'
foreach ($item in @($oldWar, $oldExpanded)) {
    if (Test-Path $item) {
        Write-Warning "Old deployment found: $item . Stop Tomcat, back it up, and remove it before using exploded development mode."
    }
}

Write-Host 'Setup completed. Use Ctrl+Shift+B in VS Code to build and launch.' -ForegroundColor Cyan
