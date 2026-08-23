param(
  [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
  $OutputPath = Join-Path $root "release\doantonghiep-release.zip"
}
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)
$releaseDirectory = Split-Path -Parent $OutputPath
$staging = Join-Path ([System.IO.Path]::GetTempPath()) ("doantonghiep-release-" + [guid]::NewGuid().ToString("N"))

$allowedRoots = @(
  ".env.example",
  ".gitignore",
  "FINAL_FIX_REPORT.md",
  "README.md",
  "database_schema.sql",
  "Frontend/nha-hang-frontend/",
  "quanlynhahang/",
  "database/",
  "docs/",
  "scripts/",
  "sql/",
  "word/"
)
$forbidden = '(^|/)(\.git|node_modules|target|dist|coverage|backup_[^/]*|release)(/|$)|(^|/)\.env($|\.(?!example$))|\.bak$|\.log$|\.tmp$|\.temp$|\.zip$'

function Test-AllowedPath {
  param([string]$Path)

  $normalized = $Path.Replace('\', '/')
  if ($normalized -match $forbidden) { return $false }
  if ($normalized -match '(^|/)\.env\.example$') { return $true }
  foreach ($allowed in $allowedRoots) {
    if ($allowed.EndsWith('/')) {
      if ($normalized.StartsWith($allowed, [System.StringComparison]::Ordinal)) { return $true }
    } elseif ($normalized -eq $allowed) {
      return $true
    }
  }
  return $false
}

Push-Location $root
try {
  $files = & git -c core.quotepath=false ls-files --cached --others --exclude-standard
  if ($LASTEXITCODE -ne 0) { throw "Could not enumerate release files from Git." }
  $releaseFiles = $files | Where-Object { (Test-AllowedPath $_) -and (Test-Path -LiteralPath $_ -PathType Leaf) }
  if (-not $releaseFiles) { throw "Release manifest is empty." }
  & (Join-Path $PSScriptRoot "security-scan.ps1") -Root $root -Paths $releaseFiles
  if ($LASTEXITCODE -ne 0) { throw "Release-manifest secret scan failed." }

  New-Item -ItemType Directory -Path $staging -Force | Out-Null
  foreach ($relativePath in $releaseFiles) {
    $destination = Join-Path $staging $relativePath
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $root $relativePath) -Destination $destination
  }

  New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
  if (Test-Path -LiteralPath $OutputPath) { Remove-Item -LiteralPath $OutputPath -Force }
  Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $OutputPath -CompressionLevel Optimal

  $archive = [System.IO.Compression.ZipFile]::OpenRead($OutputPath)
  try {
    $unsafeEntries = $archive.Entries | Where-Object { $_.FullName.Replace('\', '/') -match $forbidden }
    if ($unsafeEntries) {
      throw "Release archive contains forbidden paths: $($unsafeEntries.FullName -join ', ')"
    }
    Write-Host "Release package created: $OutputPath" -ForegroundColor Green
    Write-Host "Files packaged: $($archive.Entries.Count)" -ForegroundColor Cyan
  } finally {
    $archive.Dispose()
  }
} finally {
  Pop-Location
  if (Test-Path -LiteralPath $staging) {
    Remove-Item -LiteralPath $staging -Recurse -Force
  }
}
