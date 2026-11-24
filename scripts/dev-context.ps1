# Read and validate the local Tomcat context shared by setup and deployment.
function Read-MachirikuContext {
    param([string]$ContextFile, [string]$ProjectRoot)

    if (-not (Test-Path -LiteralPath $ContextFile -PathType Leaf)) {
        throw "machiriku.xml not found. Run scripts/setup-dev.ps1 first: $ContextFile"
    }
    try {
        [xml]$context = Get-Content -LiteralPath $ContextFile -Raw -Encoding UTF8
        $docBase = $context.DocumentElement.GetAttribute('docBase').Replace('\', '/')
    }
    catch {
        throw "machiriku.xml is not a valid development context: $ContextFile"
    }

    $target = (Join-Path $ProjectRoot 'target').Replace('\', '/')
    $versionedPath = '^' + [regex]::Escape($target) + '/dev-deploy/[0-9a-f]{32}/machiriku$'
    if ($docBase -ne "$target/machiriku" -and $docBase -notmatch $versionedPath) {
        throw "machiriku.xml points outside this project's deployment directories: $docBase"
    }
    return $context
}
