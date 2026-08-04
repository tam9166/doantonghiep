param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$StaffUsername = "",
  [string]$StaffPassword = "",
  [string]$CustomerUsername = "",
  [string]$CustomerPassword = ""
)

$ErrorActionPreference = "Stop"

function Invoke-SmokeRequest {
  param(
    [string]$Name,
    [string]$Method = "GET",
    [string]$Url,
    [hashtable]$Headers = @{},
    [object]$Body = $null,
    [int[]]$ExpectedStatus = @(200)
  )

  try {
    $params = @{
      Method = $Method
      Uri = $Url
      Headers = $Headers
      UseBasicParsing = $true
      TimeoutSec = 20
    }
    if ($null -ne $Body) {
      $params.ContentType = "application/json; charset=utf-8"
      $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    $response = Invoke-WebRequest @params
    if ($ExpectedStatus -notcontains [int]$response.StatusCode) {
      throw "Expected HTTP $($ExpectedStatus -join '/') but got $($response.StatusCode)"
    }
    Write-Host "[PASS] $Name ($($response.StatusCode))" -ForegroundColor Green
    return $response
  } catch {
    $status = $null
    if ($_.Exception.Response) {
      $status = [int]$_.Exception.Response.StatusCode
    }
    if ($status -and ($ExpectedStatus -contains $status)) {
      Write-Host "[PASS] $Name ($status)" -ForegroundColor Green
      return $_.Exception.Response
    }
    Write-Host "[FAIL] $Name" -ForegroundColor Red
    throw
  }
}

function Convert-ResponseJson {
  param($Response)
  if ($Response.Content) {
    return $Response.Content | ConvertFrom-Json
  }
  $reader = [System.IO.StreamReader]::new($Response.GetResponseStream())
  try {
    return ($reader.ReadToEnd() | ConvertFrom-Json)
  } finally {
    $reader.Dispose()
  }
}

$BaseUrl = $BaseUrl.TrimEnd("/")
Write-Host "Smoke testing $BaseUrl" -ForegroundColor Cyan

$homeResponse = Invoke-SmokeRequest -Name "SPA/static home" -Url "$BaseUrl/"
if ($homeResponse.Content -notmatch "<html|<div id=`"app`"") {
  throw "Home page did not look like the built frontend."
}

$tablesResponse = Invoke-SmokeRequest -Name "Public tables API" -Url "$BaseUrl/api/tables"
$tables = Convert-ResponseJson $tablesResponse
if ($null -eq $tables) {
  throw "Tables API did not return JSON."
}
Write-Host "       tables returned: $(@($tables).Count)" -ForegroundColor DarkGray

Invoke-SmokeRequest -Name "Reservation lookup route fallback" -Url "$BaseUrl/reservation-lookup" | Out-Null

if ($StaffUsername -and $StaffPassword) {
  $loginResponse = Invoke-SmokeRequest `
    -Name "Staff login" `
    -Method "POST" `
    -Url "$BaseUrl/api/auth/staff/login" `
    -Body @{ username = $StaffUsername; password = $StaffPassword }
  $loginJson = Convert-ResponseJson $loginResponse
  if (-not $loginJson.token) {
    throw "Staff login response did not contain a JWT token."
  }
  $authHeaders = @{ Authorization = "Bearer $($loginJson.token)" }
  Invoke-SmokeRequest -Name "Authenticated admin orders API" -Url "$BaseUrl/api/admin/orders" -Headers $authHeaders | Out-Null
}

if ($CustomerUsername -and $CustomerPassword) {
  $loginResponse = Invoke-SmokeRequest `
    -Name "Customer login" `
    -Method "POST" `
    -Url "$BaseUrl/api/auth/login" `
    -Body @{ username = $CustomerUsername; password = $CustomerPassword }
  $loginJson = Convert-ResponseJson $loginResponse
  if (-not $loginJson.token) {
    throw "Customer login response did not contain a JWT token."
  }
  $authHeaders = @{ Authorization = "Bearer $($loginJson.token)" }
  Invoke-SmokeRequest -Name "Customer profile API" -Url "$BaseUrl/api/auth/profile" -Headers $authHeaders | Out-Null
}

Write-Host "Smoke test completed." -ForegroundColor Green
