param(
  [string]$Server = "localhost,1433",
  [string]$Database = "RestaurantDB_FlywayCheck",
  [string]$Username = $env:DB_USERNAME,
  [string]$Password = $env:DB_PASSWORD,
  [switch]$Seed,
  [switch]$ReplaceExisting
)

$ErrorActionPreference = "Stop"

if ($Database -eq "RestaurantDB") {
  throw "Refusing to bootstrap RestaurantDB. Use a separate empty database name for this verification."
}
if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
  throw "DB username/password are required. Pass -Username/-Password or set DB_USERNAME/DB_PASSWORD."
}
if (-not (Get-Command sqlcmd -ErrorAction SilentlyContinue)) {
  throw "sqlcmd was not found. Install SQL Server command-line tools first."
}

$root = Split-Path -Parent $PSScriptRoot
$schemaSource = Join-Path $root "sql\01_create_schema.sql"
$reservationSource = Join-Path $root "sql\04_upgrade_reservations.sql"
$reservationUtf8Source = Join-Path $root "database\utf8-reservation-payment-upgrade\02_utf8_schema_fix.sql"
$paymentIntentSource = Join-Path $root "database\utf8-reservation-payment-upgrade\04_payment_intents.sql"
$seedSource = Join-Path $root "sql\02_seed_data.sql"

foreach ($path in @($schemaSource, $reservationSource, $reservationUtf8Source, $paymentIntentSource, $seedSource)) {
  if (-not (Test-Path -LiteralPath $path)) {
    throw "Required bootstrap script is missing: $path"
  }
}

$existenceQuery = "SET NOCOUNT ON; SELECT CASE WHEN DB_ID(N'$Database') IS NULL THEN 0 ELSE 1 END;"
$databaseExists = (& sqlcmd -S $Server -d master -U $Username -P $Password -C -h -1 -W -Q $existenceQuery |
  Select-Object -Last 1).Trim()
if ($databaseExists -eq "1" -and -not $ReplaceExisting) {
  throw "Database $Database already exists. Choose another name or pass -ReplaceExisting after verifying it is disposable."
}
if ($databaseExists -eq "1") {
  $dropQuery = "ALTER DATABASE [$Database] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [$Database];"
  & sqlcmd -S $Server -d master -U $Username -P $Password -C -b -Q $dropQuery
  if ($LASTEXITCODE -ne 0) { throw "Could not remove disposable database $Database." }
}

function Invoke-DatabaseScript {
  param([string]$Source, [string]$Label)

  $tempScript = Join-Path ([System.IO.Path]::GetTempPath()) ("mocvi-$Database-$Label.sql")
  try {
    # The legacy scripts are intentionally reused only on a disposable database.
    $sessionOptions = @"
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;

"@
    $content = $sessionOptions + (Get-Content -LiteralPath $Source -Raw -Encoding UTF8)
    $content = $content.Replace("RestaurantDB", $Database)
    if ($Label -eq "reservations") {
      # SQL Server compiles a batch before ALTER TABLE columns are visible to UPDATE.
      $content = $content -replace "(?m)^UPDATE dbo\.restaurant_table", "GO`r`n`r`nUPDATE dbo.restaurant_table"
    }
    [System.IO.File]::WriteAllText($tempScript, $content, [System.Text.UTF8Encoding]::new($false))
    $initialDatabase = if ($Label -eq "schema") { "master" } else { $Database }
    & sqlcmd -S $Server -d $initialDatabase -U $Username -P $Password -C -b -f 65001 -i $tempScript
    if ($LASTEXITCODE -ne 0) { throw "$Label bootstrap failed." }
  } finally {
    Remove-Item -LiteralPath $tempScript -Force -ErrorAction SilentlyContinue
  }
}

Invoke-DatabaseScript -Source $schemaSource -Label "schema"
Invoke-DatabaseScript -Source $reservationSource -Label "reservations"
Invoke-DatabaseScript -Source $reservationUtf8Source -Label "reservation-utf8"
Invoke-DatabaseScript -Source $paymentIntentSource -Label "payment-intents"
if ($Seed) {
  Invoke-DatabaseScript -Source $seedSource -Label "seed"
}

Write-Host "Bootstrap completed for disposable database $Database." -ForegroundColor Green
Write-Host "Run Spring Boot with DB_URL targeting this database to verify Flyway V003 and later." -ForegroundColor Cyan
