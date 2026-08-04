param(
  [ValidateSet("Latest", "Full", "AnalyticsSeed")]
  [string]$Mode = "Latest",
  [string]$Server = "localhost,1433",
  [string]$Database = "RestaurantDB",
  [string]$Username = $env:DB_USERNAME,
  [string]$Password = $env:DB_PASSWORD,
  [switch]$BackupFirst
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$migrationDir = Join-Path $root "database\advanced-reservation-upgrade"

if (-not (Get-Command sqlcmd -ErrorAction SilentlyContinue)) {
  throw "sqlcmd was not found. Install SQL Server command-line tools or run the SQL files manually in SSMS."
}

if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
  throw "DB username/password are required. Set DB_USERNAME and DB_PASSWORD, or pass -Username and -Password."
}

$scriptsByMode = @{
  Latest = @(
    "19_reservation_idempotency.sql",
    "20_reservation_waitlist.sql",
    "21_seed_admin_analytics_demo_ascii.sql",
    "22_repair_demo_product_category_vietnamese.sql"
  )
  Full = @(
    "02_realtime_and_status.sql",
    "03_payment_webhook.sql",
    "04_table_layout.sql",
    "05_smart_table_suggestion.sql",
    "06_deposit_policies.sql",
    "07_reservation_vouchers.sql",
    "08_customer_history.sql",
    "09_reviews.sql",
    "10_notification_channels.sql",
    "11_audit_logs.sql",
    "12_utf8_detection.sql",
    "13_utf8_repair.sql",
    "14_indexes_constraints.sql",
    "15_seed_test_data.sql",
    "18_seed_admin_analytics_demo_data.sql",
    "19_reservation_idempotency.sql",
    "20_reservation_waitlist.sql",
    "21_seed_admin_analytics_demo_ascii.sql",
    "22_repair_demo_product_category_vietnamese.sql",
    "16_verify_upgrade.sql"
  )
  AnalyticsSeed = @(
    "21_seed_admin_analytics_demo_ascii.sql"
  )
}

$selectedScripts = [System.Collections.Generic.List[string]]::new()
if ($BackupFirst) {
  $selectedScripts.Add("01_backup_database.sql")
}
$scriptsByMode[$Mode] | ForEach-Object { $selectedScripts.Add($_) }

foreach ($scriptName in $selectedScripts) {
  $scriptPath = Join-Path $migrationDir $scriptName
  if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "Missing migration script: $scriptPath"
  }

  Write-Host "Running $scriptName ..." -ForegroundColor Cyan
  & sqlcmd -S $Server -d $Database -U $Username -P $Password -C -b -f 65001 -i $scriptPath
  if ($LASTEXITCODE -ne 0) {
    throw "Migration failed: $scriptName"
  }
}

Write-Host "Database upgrade completed. Mode=$Mode Database=$Database Server=$Server" -ForegroundColor Green
