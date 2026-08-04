$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root "Frontend\nha-hang-frontend"
$backend = Join-Path $root "quanlynhahang"

Push-Location $frontend
if (Test-Path "dist") { Remove-Item "dist" -Recurse -Force }
if (Test-Path "node_modules") {
  npm install
} else {
  npm ci
}
npm run lint
npm run test
npm run build
Pop-Location

Push-Location $backend
.\mvnw.cmd clean test
.\mvnw.cmd clean package
Pop-Location

Write-Host "Build completed."
