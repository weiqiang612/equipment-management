#!/bin/bash
# =============================================================================
# init.sh - AI Agent environment bootstrap
# macOS/Linux/WSL/Git Bash path. Windows PowerShell should use .\init.ps1.
# =============================================================================
APP_PORT=8080
HEALTH_CHECK_URL="http://localhost:8080/"
STARTUP_COMMAND="cd equipment_system_management && mvn spring-boot:run"
LOG_DIR="logs"
LOG_FILE="$LOG_DIR/dev_server.log"
HEALTH_TIMEOUT=60

set -euo pipefail
echo "[init] Bootstrapping equipment-management..."

MISSING=()
command -v java >/dev/null 2>&1 || MISSING+=("java")
command -v mvn >/dev/null 2>&1 || MISSING+=("mvn")
command -v node >/dev/null 2>&1 || MISSING+=("node")
command -v npm >/dev/null 2>&1 || MISSING+=("npm")
if [ ${#MISSING[@]} -gt 0 ]; then
  echo "[ERROR] Missing tools: ${MISSING[*]}"
  exit 1
fi
echo "[init] Tools OK"

PORT_PID=$(lsof -t -i:"$APP_PORT" 2>/dev/null || true)
if [ -n "$PORT_PID" ]; then
  echo "[init] Releasing port $APP_PORT (PID $PORT_PID)..."
  kill -9 "$PORT_PID" 2>/dev/null || true
  sleep 1
fi

mkdir -p "$LOG_DIR"
rm -f "$LOG_FILE"
nohup sh -c "$STARTUP_COMMAND" > "$LOG_FILE" 2>&1 &
echo "[init] Server starting (PID $!, logs -> $LOG_FILE)"

echo "[init] Waiting for server (timeout: ${HEALTH_TIMEOUT}s)..."
COUNT=0
until [ $COUNT -ge $HEALTH_TIMEOUT ]; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL" 2>/dev/null || echo "000")
  [ "$STATUS" != "000" ] && { echo "[init] Server reachable at $HEALTH_CHECK_URL (HTTP $STATUS)"; break; }
  sleep 1
  COUNT=$((COUNT+1))
  [ $((COUNT % 10)) -eq 0 ] && echo "[init]   Waiting... ${COUNT}s"
done

if [ $COUNT -ge $HEALTH_TIMEOUT ]; then
  echo "[ERROR] Server not healthy after ${HEALTH_TIMEOUT}s - check $LOG_FILE"
  exit 1
fi

echo ""
echo "[init] -- Git status --------------------------------"
git status -s
echo ""
echo "[init] -- Recent commits ----------------------------"
git log -n 3 --oneline
echo ""
echo "[init] Environment ready."
