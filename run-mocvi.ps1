$ErrorActionPreference = 'Stop'

$taskCanonicalRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$taskExpectedRoot = [System.IO.Path]::GetFullPath('E:\DoAnTotNghiep')

if (-not $taskCanonicalRoot.Equals($taskExpectedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "This script must run from the canonical project root: $taskExpectedRoot"
}

$taskFrontendRoot = Join-Path $taskCanonicalRoot 'Frontend\nha-hang-frontend'
$taskBackendRoot = Join-Path $taskCanonicalRoot 'quanlynhahang'

if (-not (Test-Path -LiteralPath (Join-Path $taskFrontendRoot 'package.json') -PathType Leaf)) {
    throw "Canonical frontend was not found: $taskFrontendRoot"
}
if (-not (Test-Path -LiteralPath (Join-Path $taskBackendRoot 'pom.xml') -PathType Leaf)) {
    throw "Canonical backend was not found: $taskBackendRoot"
}

Write-Host 'Building canonical frontend...'
Push-Location $taskFrontendRoot
try {
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}

Write-Host 'Compiling canonical backend...'
Push-Location $taskBackendRoot
try {
    & .\mvnw.cmd compile
    if ($LASTEXITCODE -ne 0) { throw "Backend compile failed with exit code $LASTEXITCODE" }

    Write-Host 'Starting Mộc Vị on 8080...'
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Backend run failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}
