param(
  [string]$Root = (Split-Path -Parent $PSScriptRoot),
  [string[]]$Paths = @()
)

$ErrorActionPreference = 'Stop'
$patterns = @(
  'AIza[0-9A-Za-z_-]{20,}',
  'sk_(live|test)_[0-9A-Za-z]{16,}',
  'ghp_[0-9A-Za-z]{30,}',
  'xox[baprs]-[0-9A-Za-z-]{10,}',
  '-----BEGIN( RSA| EC| OPENSSH)? ?PRIVATE KEY-----'
)
$binaryExtensions = @('.png', '.jpg', '.jpeg', '.gif', '.pdf', '.docx', '.xlsx', '.jar', '.class')
$findings = [System.Collections.Generic.List[string]]::new()

Push-Location $Root
try {
  $scanPaths = $Paths
  if (-not $scanPaths -or $scanPaths.Count -eq 0) {
    $scanPaths = & git -c core.quotepath=false ls-files
    if ($LASTEXITCODE -ne 0) { throw 'Could not enumerate tracked files for secret scan.' }
  }
  foreach ($relativePath in $scanPaths) {
    if ($relativePath -match '(^|/)\.env($|\.)' -and $relativePath -notmatch '\.example$') {
      $findings.Add("Tracked environment file: $relativePath")
      continue
    }

    $extension = [System.IO.Path]::GetExtension($relativePath).ToLowerInvariant()
    if ($binaryExtensions -contains $extension) { continue }

    $fullPath = Join-Path $Root $relativePath
    if (-not [System.IO.File]::Exists($fullPath)) { continue }
    $content = [System.IO.File]::ReadAllText($fullPath)
    foreach ($pattern in $patterns) {
      if ([regex]::IsMatch($content, $pattern)) {
        $findings.Add("Potential secret in tracked file: $relativePath")
        break
      }
    }
  }

  $frontendEnvFiles = $scanPaths | Where-Object { $_ -match '^Frontend/nha-hang-frontend/\.env' }
  foreach ($relativePath in $frontendEnvFiles) {
    $content = [System.IO.File]::ReadAllText((Join-Path $Root $relativePath))
    if ($content -match '(?mi)^VITE_[A-Z0-9_]*(SECRET|PASSWORD|PRIVATE|API_KEY)\s*=') {
      $findings.Add("Sensitive VITE_ variable is public in the browser bundle: $relativePath")
    }
  }
} finally {
  Pop-Location
}

if ($findings.Count -gt 0) {
  $findings | ForEach-Object { Write-Error $_ }
  exit 1
}

Write-Host 'Secret scan passed: no known credential patterns or public VITE secrets found.'
