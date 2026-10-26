# Resolve the saved active editor file to a URL within the /machiriku context.
# Keep this separate from deployment so resolving a page never starts Tomcat.

function Get-ServletPagePath {
    param([string]$JavaFile, [string]$ProjectRoot)

    $source = Get-Content -LiteralPath $JavaFile -Raw -Encoding UTF8
    $annotation = [regex]::Match($source, '(?s)@(?:jakarta\.servlet\.annotation\.)?WebServlet\s*\((.*?)\)')
    if ($annotation.Success) {
        $arguments = $annotation.Groups[1].Value.Trim()
        $patterns = [regex]::Match($arguments, '(?s)\b(?:urlPatterns|value)\s*=\s*(\{.*?\}|"[^"]*")')
        $values = if ($patterns.Success) { $patterns.Groups[1].Value } elseif ($arguments.StartsWith('"')) { $arguments } else { '' }
        foreach ($match in [regex]::Matches($values, '"(/[^"*]*)"')) {
            return $match.Groups[1].Value
        }
    }

    # Also support servlet mappings declared in web.xml (with its XML namespace).
    $webXml = Join-Path $ProjectRoot 'src\main\webapp\WEB-INF\web.xml'
    if (Test-Path -LiteralPath $webXml) {
        [xml]$descriptor = Get-Content -LiteralPath $webXml -Raw -Encoding UTF8
        $package = [regex]::Match($source, '(?m)^\s*package\s+([\w.]+)\s*;').Groups[1].Value
        $className = [System.IO.Path]::GetFileNameWithoutExtension($JavaFile)
        $qualifiedName = if ($package) { "$package.$className" } else { $className }
        foreach ($servlet in $descriptor.SelectNodes('/*[local-name()="web-app"]/*[local-name()="servlet"]')) {
            if ($servlet.'servlet-class' -ne $qualifiedName) { continue }
            foreach ($mapping in $descriptor.SelectNodes('/*[local-name()="web-app"]/*[local-name()="servlet-mapping"]')) {
                if ($mapping.'servlet-name' -ne $servlet.'servlet-name') { continue }
                foreach ($pattern in @($mapping.'url-pattern')) {
                    if ($pattern.StartsWith('/') -and -not $pattern.Contains('*')) { return $pattern }
                }
            }
        }
    }
}

function Get-ActivePagePath {
    param(
        [string]$ProjectRoot,
        [string]$ActiveFile,
        [string]$FallbackPath = '/machiriku/hello'
    )

    if ([string]::IsNullOrWhiteSpace($ActiveFile)) { return $FallbackPath }
    $root = [System.IO.Path]::GetFullPath($ProjectRoot).TrimEnd('\', '/')
    $file = [System.IO.Path]::GetFullPath($ActiveFile)
    if (-not $file.StartsWith($root + '\', [System.StringComparison]::OrdinalIgnoreCase) -or
        -not (Test-Path -LiteralPath $file -PathType Leaf)) {
        Write-Warning "Active file is outside this project or is not saved. Opening $FallbackPath"
        return $FallbackPath
    }

    $webRoot = Join-Path $root 'src\main\webapp'
    $javaRoot = Join-Path $root 'src\main\java'
    $route = $null
    if ($file.StartsWith($webRoot + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
        $relative = $file.Substring($webRoot.Length).Replace('\', '/')
        if ($relative -notmatch '^/(WEB-INF|META-INF)(/|$)') {
            $route = $relative
        }
        elseif ($relative -match '^/WEB-INF/.*\.jspx?$') {
            # Protected JSPs must be opened through the servlet that forwards to them.
            $dispatcher = 'getRequestDispatcher\s*\(\s*"' + [regex]::Escape($relative) + '"\s*\)'
            $routes = @(Get-ChildItem -LiteralPath $javaRoot -Filter '*.java' -Recurse | ForEach-Object {
                $source = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
                if ($source -match $dispatcher) {
                    Get-ServletPagePath -JavaFile $_.FullName -ProjectRoot $root
                }
            } | Sort-Object -Unique)
            if ($routes.Count -eq 1) { $route = $routes[0] }
        }
    }
    elseif ($file.StartsWith($javaRoot + '\', [System.StringComparison]::OrdinalIgnoreCase) -and
        [System.IO.Path]::GetExtension($file) -eq '.java') {
        $route = Get-ServletPagePath -JavaFile $file -ProjectRoot $root
    }

    if ($null -eq $route) {
        Write-Warning "No page URL could be determined for the active file. Opening $FallbackPath"
        return $FallbackPath
    }
    $escaped = ($route.Split('/') | ForEach-Object { [System.Uri]::EscapeDataString($_) }) -join '/'
    return '/machiriku' + $escaped
}
