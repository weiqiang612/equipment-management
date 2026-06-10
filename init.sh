#!/bin/bash
# =============================================================================
# init.sh — AI Agent environment bootstrap (UNIX / macOS)
# Run at the start of every Agent session: bash init.sh
# =============================================================================
# ⚙️  CONFIGURATION — review before first run
APP_PORT=8080
HEALTH_CHECK_URL="http://localhost:8080/"
STARTUP_COMMAND="cd equipment_system_management && mvn spring-boot:run"
ROOT_DIR=$(pwd)
LOG_FILE="$ROOT_DIR/logs/dev_server.log"
HEALTH_TIMEOUT=60
# =============================================================================

set -euo pipefail
echo "[init] Bootstrapping equipment-management..."

# 1. Verify required tools
MISSING=()
command -v java &>/dev/null || MISSING+=("java")
command -v mvn  &>/dev/null || MISSING+=("mvn")
command -v node &>/dev/null || MISSING+=("node")
command -v npm  &>/dev/null || MISSING+=("npm")

if [ ${#MISSING[@]} -gt 0 ]; then
  echo "[ERROR] Missing tools: ${MISSING[*]}. Install them and re-run."
  exit 1
fi
echo "[init] ✓ Tools OK"

# 2. Extract Environment Variables from IDEA workspace.xml (Zero-maintenance Auto-Extraction)
WORKSPACE_XML=$(find . -maxdepth 3 -name "workspace.xml" -path "*/.idea/workspace.xml" | head -n 1)
if [ -n "$WORKSPACE_XML" ] && [ -f "$WORKSPACE_XML" ] && command -v python3 &>/dev/null; then
  echo "[init] Detected IDEA configuration. Auto-extracting env variables from $WORKSPACE_XML..."
  mkdir -p "$ROOT_DIR/logs"
  python3 << 'EOF' > "$ROOT_DIR/logs/env_vars.sh"
import re
import sys
try:
    xml_path = sys.argv[1]
    with open(xml_path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    # Find all configuration blocks containing envs
    config_matches = re.findall(r'<configuration[^>]*>.*?<envs>.*?</envs>.*?</configuration>', content, re.DOTALL)
    for config_block in config_matches:
        config_name_match = re.search(r'name=\"([^\"]+)\"', config_block)
        if config_name_match:
            config_name = config_name_match.group(1)
            env_matches = re.findall(r'<env name=\"([^\"]+)\" value=\"([^\"]+)\"', config_block)
            for name, val in env_matches:
                print(f'export {name}="{val}"')
                print(f'echo "[init]   Loaded env: {name} (from {config_name})"')
except Exception as e:
    pass
EOF "$WORKSPACE_XML"
  if [ -f "$ROOT_DIR/logs/env_vars.sh" ]; then
    source "$ROOT_DIR/logs/env_vars.sh"
    rm -f "$ROOT_DIR/logs/env_vars.sh"
  fi
fi

# 3. Clear port if occupied
PORT_PID=$(lsof -t -i:"$APP_PORT" 2>/dev/null || true)
if [ -n "$PORT_PID" ]; then
  echo "[init] Releasing port $APP_PORT (PID $PORT_PID)..."
  kill -9 "$PORT_PID" 2>/dev/null || true
  sleep 1
fi

# 4. Start dev server in background
mkdir -p "$ROOT_DIR/logs"
nohup sh -c "$STARTUP_COMMAND" > "$LOG_FILE" 2>&1 &
echo "[init] Server starting (PID $!, logs → $LOG_FILE)"

# 5. Health check loop
echo "[init] Waiting for server (timeout: ${HEALTH_TIMEOUT}s)..."
COUNT=0
until [ $COUNT -ge $HEALTH_TIMEOUT ]; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL" 2>/dev/null || echo "000")
  [ "$STATUS" = "200" ] && { echo "[init] ✓ Server ready at $HEALTH_CHECK_URL"; break; }
  sleep 1; COUNT=$((COUNT+1))
  [ $((COUNT % 10)) -eq 0 ] && echo "[init]   Waiting... ${COUNT}s"
done
if [ $COUNT -ge $HEALTH_TIMEOUT ]; then
  echo "[ERROR] Server not healthy after ${HEALTH_TIMEOUT}s — check $LOG_FILE"
  exit 1
fi

# 6. Git context (Robust multi-repo detection)
echo ""
WORKSPACE_XML=$(find . -maxdepth 3 -name ".git" -type d | head -n 1)
GIT_REPO_PATH=$(find . -maxdepth 3 -name ".git" -type d | head -n 1)
if [ -n "$GIT_REPO_PATH" ]; then
  GIT_REPO_DIR=$(dirname "$GIT_REPO_PATH")
  echo "[init] ── Git status ($(basename "$GIT_REPO_DIR")) ───────────────────"
  cd "$GIT_REPO_DIR"
  git status -s
  echo ""
  echo "[init] ── Recent commits ($(basename "$GIT_REPO_DIR")) ──────────────"
  git log -n 3 --oneline
  cd "$ROOT_DIR"
else
  echo "[init] ── Git status ─────────────────────────────────"
  git status -s
  echo ""
  echo "[init] ── Recent commits ────────────────────────────"
  git log -n 3 --oneline
fi
echo ""
echo "[init] ✓ Environment ready."
