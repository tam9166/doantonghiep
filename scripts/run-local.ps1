param(
  [int]$Port = 8080,
  [string]$DbUrl = "jdbc:sqlserver://localhost:1433;databaseName=RestaurantDB;encrypt=true;trustServerCertificate=true;characterEncoding=UTF-8;sendStringParametersAsUnicode=true",
  [string]$DbUsername = $env:DB_USERNAME,
  [string]$DbPassword = $env:DB_PASSWORD,
  [string]$JwtSecret = $env:JWT_SECRET,
  [string]$PaymentWebhookSecret = $env:PAYMENT_WEBHOOK_SECRET,
  [string]$BankCode = "MB",
  [string]$BankLabel = "MB Bank",
  [string]$BankAccountNumber = $env:RESTAURANT_PAYMENT_ACCOUNT_NUMBER,
  [string]$BankAccountName = $env:RESTAURANT_PAYMENT_ACCOUNT_HOLDER,
  [switch]$BuildFrontend,
  [switch]$RunMigrations,
  [switch]$UseNextFreePort,
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function New-RandomSecret {
  $bytes = New-Object byte[] 48
  $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
  try {
    $generator.GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
  } finally {
    $generator.Dispose()
  }
}

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root "Frontend\nha-hang-frontend"
$backend = Join-Path $root "quanlynhahang"

function Test-PortInUse {
  param([int]$PortToCheck)
  $connection = Get-NetTCPConnection -LocalPort $PortToCheck -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  return $null -ne $connection
}

if ([string]::IsNullOrWhiteSpace($DbUsername) -or [string]::IsNullOrWhiteSpace($DbPassword)) {
  throw "DB credentials are required. Pass -DbUsername/-DbPassword or set DB_USERNAME/DB_PASSWORD."
}

if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
  $JwtSecret = New-RandomSecret
}
if ([string]::IsNullOrWhiteSpace($PaymentWebhookSecret)) {
  $PaymentWebhookSecret = New-RandomSecret
}
if ([string]::IsNullOrWhiteSpace($BankAccountNumber)) {
  $BankAccountNumber = "00000000"
}
if ([string]::IsNullOrWhiteSpace($BankAccountName)) {
  $BankAccountName = "DEMO ACCOUNT"
}

if (Test-PortInUse -PortToCheck $Port) {
  if (-not $UseNextFreePort) {
    $ownerPid = (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
    $owner = if ($ownerPid) { Get-Process -Id $ownerPid -ErrorAction SilentlyContinue } else { $null }
    $ownerName = if ($owner) { "$($owner.ProcessName) pid=$ownerPid" } else { "pid=$ownerPid" }
    throw "Port $Port is already in use by $ownerName. Stop that app or rerun with -UseNextFreePort."
  }

  while (Test-PortInUse -PortToCheck $Port) {
    $Port += 1
  }
}

$env:SERVER_PORT = "$Port"
$env:DB_URL = $DbUrl
$env:DB_USERNAME = $DbUsername
$env:DB_PASSWORD = $DbPassword
$env:JWT_SECRET = $JwtSecret
$env:PAYMENT_WEBHOOK_SECRET = $PaymentWebhookSecret
$env:RESTAURANT_PAYMENT_BANK_CODE = $BankCode
$env:RESTAURANT_PAYMENT_ACCOUNT_NUMBER = $BankAccountNumber
$env:RESTAURANT_PAYMENT_ACCOUNT_HOLDER = $BankAccountName
$env:VITE_BANK_CODE = $BankCode
$env:VITE_BANK_LABEL = $BankLabel
$env:VITE_BANK_ACCOUNT_NUMBER = $BankAccountNumber
$env:VITE_BANK_ACCOUNT_NAME = $BankAccountName
$env:ALLOWED_ORIGINS = "http://localhost:$Port,http://localhost:5173,http://localhost:3000"
$env:RATE_LIMIT_ENABLED = "true"
$env:CAPTCHA_ENABLED = if ($env:CAPTCHA_ENABLED) { $env:CAPTCHA_ENABLED } else { "false" }
$env:CAPTCHA_PROVIDER = if ($env:CAPTCHA_PROVIDER) { $env:CAPTCHA_PROVIDER } else { "mock" }
$env:JPA_SHOW_SQL = if ($env:JPA_SHOW_SQL) { $env:JPA_SHOW_SQL } else { "false" }
$env:SECURITY_LOG_LEVEL = if ($env:SECURITY_LOG_LEVEL) { $env:SECURITY_LOG_LEVEL } else { "INFO" }
$env:HIBERNATE_DEPRECATION_LOG_LEVEL = if ($env:HIBERNATE_DEPRECATION_LOG_LEVEL) { $env:HIBERNATE_DEPRECATION_LOG_LEVEL } else { "ERROR" }

Write-Host "Local configuration ready:" -ForegroundColor Cyan
Write-Host "  URL: http://localhost:$Port"
Write-Host "  DB : $DbUrl"
Write-Host "  User: $DbUsername"

if ($DryRun) {
  Write-Host "Dry run completed. Spring Boot was not started." -ForegroundColor Yellow
  return
}

if ($RunMigrations) {
  & (Join-Path $PSScriptRoot "run-db-upgrade.ps1") -Mode Latest -Server "localhost,1433" -Database "RestaurantDB" -Username $DbUsername -Password $DbPassword
}

if ($BuildFrontend) {
  Push-Location $frontend
  npm install
  npm run lint
  npm run test
  npm run build
  Pop-Location
}

Push-Location $backend
try {
  .\mvnw.cmd spring-boot:run
} finally {
  Pop-Location
}
