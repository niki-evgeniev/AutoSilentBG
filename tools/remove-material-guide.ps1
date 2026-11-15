[CmdletBinding(SupportsShouldProcess)]
param([string]$WorkspaceRoot = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $WorkspaceRoot).Path.TrimEnd('\')
if (-not (Test-Path -LiteralPath (Join-Path $root 'build.gradle'))) {
    throw 'Expected the AutoSilent project root (build.gradle is missing).'
}

# Only named guide files and marked integration lines are affected.
function ProjectPath([string]$relative) {
    $resolved = [IO.Path]::GetFullPath((Join-Path $root $relative))
    if (-not $resolved.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path outside the project: $resolved"
    }
    return $resolved
}
$headerPath = ProjectPath 'src/main/resources/templates/fragments/header.html'
$securityPath = ProjectPath 'src/main/java/nevg/autosilent/AppConfiguration/SpringSecurity.java'
$header = [IO.File]::ReadAllText($headerPath)
$security = [IO.File]::ReadAllText($securityPath)
$headerPattern = '(?ms)^[ \t]*<!-- material-guide:start -->\r?\n.*?^[ \t]*<!-- material-guide:end -->\r?\n'
$securityPattern = '(?m)^[ \t]*\.requestMatchers\("/koe-da-izpolzvam", "/koe-da-izpolzvam\.html"\)\.permitAll\(\) // material-guide\r?\n'
$newHeader = [regex]::Replace($header, $headerPattern, '')
$newSecurity = [regex]::Replace($security, $securityPattern, '')
$pageFiles = @(
    'src/main/java/nevg/autosilent/Controller/MaterialGuideController.java',
    'src/main/resources/static/koe-da-izpolzvam.html',
    'src/main/resources/static/css/material-guide.css',
    'src/main/resources/static/js/material-guide.js',
    'src/test/java/nevg/autosilent/Controller/MaterialGuidePageTest.java'
)
$existing = @($pageFiles | Where-Object { Test-Path -LiteralPath (ProjectPath $_) })
if ($existing.Count -eq 0 -and $newHeader -eq $header -and $newSecurity -eq $security) {
    Write-Host 'The material guide has already been removed.'
    exit 0
}
if (($header.Contains('material-guide:start') -and $newHeader -eq $header) -or
    ($security.Contains('// material-guide') -and $newSecurity -eq $security)) {
    throw 'Guide markers have changed. No files were changed; review the integration manually.'
}
if (-not $PSCmdlet.ShouldProcess($root, 'Back up and remove the material guide and its navigation link')) { return }

$backup = ProjectPath ('.guide-backups/material-guide-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $backup | Out-Null
$toBackup = @($existing)
if ($newHeader -ne $header) { $toBackup += 'src/main/resources/templates/fragments/header.html' }
if ($newSecurity -ne $security) { $toBackup += 'src/main/java/nevg/autosilent/AppConfiguration/SpringSecurity.java' }
foreach ($relative in $toBackup) {
    $destination = [IO.Path]::GetFullPath((Join-Path $backup $relative))
    if (-not $destination.StartsWith($backup + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid backup path.' }
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    Copy-Item -LiteralPath (ProjectPath $relative) -Destination $destination
}
$utf8 = New-Object System.Text.UTF8Encoding($false)
if ($newHeader -ne $header) { [IO.File]::WriteAllText($headerPath, $newHeader, $utf8) }
if ($newSecurity -ne $security) { [IO.File]::WriteAllText($securityPath, $newSecurity, $utf8) }
foreach ($relative in $existing) {
    $target = ProjectPath $relative
    Remove-Item -LiteralPath $target
}
Write-Host 'Material guide removed from the project. Clean rebuild/redeploy the application to update a running site.'
Write-Host "Backup: $backup"
