# =============================================================================
# init.ps1 — AI Agent environment bootstrap (Windows PowerShell)
# Run at the start of every Agent session: .\init.ps1
# =============================================================================
# ⚙️  CONFIGURATION — review before first run
$APP_PORT = 8080
$HEALTH_CHECK_URL = "http://localhost:8080/"
$STARTUP_COMMAND = "cd equipment_system_management && mvn spring-boot:run"
$ROOT_DIR = Get-Location
$LOG_FILE = "$ROOT_DIR\logs\dev_server.log"
$HEALTH_TIMEOUT = 60
# =============================================================================

$ErrorActionPreference = "Stop"
Write-Host "[init] Bootstrapping equipment-management..."

# 1. Verify required tools
$Missing = @()
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { $Missing += "java" }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { $Missing += "mvn" }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { $Missing += "node" }
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { $Missing += "npm" }

if ($Missing.Count -gt 0) {
  Write-Host "[ERROR] Missing tools: $($Missing -join ', '). Install them and re-run." -ForegroundColor Red
  Exit 1
}
Write-Host "[init] ✓ Tools OK"

# 2. Extract Environment Variables from IDEA workspace.xml (Zero-maintenance Auto-Extraction)
$WorkspaceXmlPath = Get-ChildItem -Path $ROOT_DIR -Filter "workspace.xml" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Where-Object { $_.FullName -like "*\.idea\workspace.xml" } | Select-Object -First 1 -ExpandProperty FullName
if ($WorkspaceXmlPath -and (Test-Path $WorkspaceXmlPath)) {
  Write-Host "[init] Detected IDEA configuration ($WorkspaceXmlPath). Auto-extracting env variables..." -ForegroundColor Cyan
  try {
    $Content = Get-Content $WorkspaceXmlPath -Raw
    # Find all configuration blocks containing envs
    $ConfigMatches = [regex]::Matches($Content, '(?s)<configuration[^>]*>.*?</configuration>')
    foreach ($Match in $ConfigMatches) {
      $ConfigBlock = $Match.Value
      if ($ConfigBlock -like "*<envs>*") {
        $ConfigName = if ($ConfigBlock -match 'name="([^"]+)"') { $Matches[1] } else { "Unknown" }
        $EnvMatches = [regex]::Matches($ConfigBlock, '<env name="([^"]+)" value="([^"]+)"')
        foreach ($EnvMatch in $EnvMatches) {
          $Name = $EnvMatch.Groups[1].Value
          $Value = $EnvMatch.Groups[2].Value
          if ($Name) {
            # Inject into the process environment
            [System.Environment]::SetEnvironmentVariable($Name, $Value, [System.EnvironmentVariableTarget]::Process)
            Write-Host "[init]   Loaded env: $Name (from $ConfigName)" -ForegroundColor DarkCyan
          }
        }
      }
    }
  } catch {
    Write-Host "[WARNING] Failed to parse workspace.xml: $_. Continuing with defaults." -ForegroundColor Yellow
  }
}

# 3. Clear port if occupied
$PortOwner = Get-NetTCPConnection -LocalPort $APP_PORT -ErrorAction SilentlyContinue
if ($PortOwner) {
  $PIDToKill = $PortOwner.OwningProcess | Select-Object -Unique
  if ($PIDToKill) {
    Write-Host "[init] Releasing port $APP_PORT (PID $PIDToKill)..."
    Stop-Process -Id $PIDToKill -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
  }
}

# 4. Start dev server in background
if (-not (Test-Path "$ROOT_DIR\logs")) {
  New-Item -ItemType Directory -Path "$ROOT_DIR\logs" | Out-Null
}
$CommandToRun = "$STARTUP_COMMAND *>&1 > `"$LOG_FILE`""
Start-Process powershell -ArgumentList "-NoProfile -Command `"$CommandToRun`"" -WindowStyle Hidden
Write-Host "[init] Server starting in background (logs → $LOG_FILE)"

# 5. Health check loop
Write-Host "[init] Waiting for server (timeout: $($HEALTH_TIMEOUT)s)..."
$Count = 0
$Success = $false
while ($Count -lt $HEALTH_TIMEOUT) {
  try {
    $Response = Invoke-WebRequest -Uri $HEALTH_CHECK_URL -UseBasicParsing -TimeoutSec 1 -ErrorAction Stop
    if ($Response.StatusCode -eq 200) {
      Write-Host "[init] ✓ Server ready at $HEALTH_CHECK_URL" -ForegroundColor Green
      $Success = $true
      break
    }
  } catch {
    # Ignore connection errors during startup
  }
  Start-Sleep -Seconds 1
  $Count++
  if ($Count % 10 -eq 0) {
    Write-Host "[init]   Waiting... $($Count)s"
  }
}

if (-not $Success) {
  Write-Host "[ERROR] Server not healthy after $($HEALTH_TIMEOUT)s — check $LOG_FILE" -ForegroundColor Red
  Exit 1
}

# 6. Git context (Robust multi-repo detection)
Write-Host ""
$GitRepoPath = Get-ChildItem -Path $ROOT_DIR -Filter ".git" -Directory -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Parent
if ($GitRepoPath) {
  Write-Host "[init] ── Git status ($($GitRepoPath.Name)) ───────────────────"
  Set-Location $GitRepoPath.FullName
  git status -s
  Write-Host ""
  Write-Host "[init] ── Recent commits ($($GitRepoPath.Name)) ──────────────"
  git log -n 3 --oneline
  Set-Location $ROOT_DIR
} else {
  Write-Host "[init] ── Git status ─────────────────────────────────"
  git status -s
  Write-Host ""
  Write-Host "[init] ── Recent commits ────────────────────────────"
  git log -n 3 --oneline
}
Write-Host ""
Write-Host "[init] ✓ Environment ready."
