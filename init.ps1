# =============================================================================
# init.ps1 - AI Agent environment bootstrap
# Windows path. Run on demand when the dev server needs starting.
# =============================================================================
$APP_PORT = 8080
$HEALTH_CHECK_URL = "http://localhost:8080/"
$STARTUP_COMMAND = "Set-Location -LiteralPath 'equipment_system_management'; mvn spring-boot:run"
$LOG_DIR = "logs"
$STDOUT_LOG = Join-Path $LOG_DIR "dev_server.out.log"
$STDERR_LOG = Join-Path $LOG_DIR "dev_server.err.log"
$HEALTH_TIMEOUT = 60

$ErrorActionPreference = "Stop"
Write-Host "[init] Bootstrapping equipment-management..."

$Missing = @()
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { $Missing += "java" }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { $Missing += "mvn" }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { $Missing += "node" }
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { $Missing += "npm" }
if ($Missing.Count -gt 0) {
  Write-Host "[ERROR] Missing tools: $($Missing -join ', ')"
  exit 1
}
Write-Host "[init] Tools OK"

$PortPids = @()
try {
  $PortPids = Get-NetTCPConnection -LocalPort $APP_PORT -State Listen -ErrorAction Stop |
    Select-Object -ExpandProperty OwningProcess -Unique
} catch {}

foreach ($PidToStop in $PortPids) {
  if ($PidToStop) {
    Write-Host "[init] Releasing port $APP_PORT (PID $PidToStop)..."
    try { Stop-Process -Id ([int]$PidToStop) -Force -ErrorAction Stop } catch {}
  }
}
Start-Sleep -Seconds 1

if (-not (Test-Path $LOG_DIR)) {
  New-Item -ItemType Directory -Path $LOG_DIR | Out-Null
}

Remove-Item -LiteralPath $STDOUT_LOG, $STDERR_LOG -Force -ErrorAction SilentlyContinue

$ShellCommand = Get-Command pwsh -ErrorAction SilentlyContinue
if (-not $ShellCommand) {
  $ShellCommand = Get-Command powershell -ErrorAction Stop
}

$Process = Start-Process `
  -FilePath $ShellCommand.Source `
  -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $STARTUP_COMMAND) `
  -WorkingDirectory (Get-Location).Path `
  -RedirectStandardOutput $STDOUT_LOG `
  -RedirectStandardError $STDERR_LOG `
  -WindowStyle Hidden `
  -PassThru

Write-Host "[init] Server starting (PID $($Process.Id), logs -> $STDOUT_LOG / $STDERR_LOG)"
Write-Host "[init] Waiting for server (timeout: ${HEALTH_TIMEOUT}s)..."

$Count = 0
while ($Count -lt $HEALTH_TIMEOUT) {
  try {
    $TcpClient = [System.Net.Sockets.TcpClient]::new()
    $Connect = $TcpClient.BeginConnect("localhost", $APP_PORT, $null, $null)
    if ($Connect.AsyncWaitHandle.WaitOne(1000, $false)) {
      $TcpClient.EndConnect($Connect)
      $TcpClient.Close()
      Write-Host "[init] Server reachable on port $APP_PORT"
      break
    }
    $TcpClient.Close()
  } catch {}
  Start-Sleep -Seconds 1
  $Count += 1
  if ($Count % 10 -eq 0) {
    Write-Host "[init]   Waiting... ${Count}s"
  }
}

if ($Count -ge $HEALTH_TIMEOUT) {
  Write-Host "[ERROR] Server not healthy after ${HEALTH_TIMEOUT}s."
  Write-Host "[ERROR] Check stderr first: $STDERR_LOG"
  Write-Host "[ERROR] Then check stdout: $STDOUT_LOG"
  exit 1
}

Write-Host ""
Write-Host "[init] -- Git status --------------------------------"
git status -s
Write-Host ""
Write-Host "[init] -- Recent commits ----------------------------"
git log -n 3 --oneline
Write-Host ""
Write-Host "[init] Environment ready."
