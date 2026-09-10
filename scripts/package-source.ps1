$ErrorActionPreference = 'Stop'
$workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$archive = Join-Path $workspace 'lumina-clinic-source.zip'
# Read Git's ignore rules without requiring a commit; include reviewed source only.
$paths = & git -C $workspace ls-files --cached --others --exclude-standard
if ($LASTEXITCODE -ne 0) { throw 'Initialize Git before packaging the source.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $archive) { Remove-Item -LiteralPath $archive }
$zip = [System.IO.Compression.ZipFile]::Open($archive, 'Create')
try {
    foreach ($relative in ($paths | Sort-Object -Unique)) {
        $full = [System.IO.Path]::GetFullPath((Join-Path $workspace $relative))
        if (-not $full.StartsWith($workspace + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing path outside the workspace: $relative"
        }
        if (Test-Path -LiteralPath $full -PathType Leaf) {
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $full, ('lumina-clinic/' + $relative.Replace('\', '/')), 'Optimal') | Out-Null
        }
    }
} finally { $zip.Dispose() }
Get-Item -LiteralPath $archive | Select-Object FullName, Length
