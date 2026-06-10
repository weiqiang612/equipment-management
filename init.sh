#!/usr/bin/env bash
# Run on demand: first-time setup or when the dev server needs restarting.
# NOT run every session — the SessionStart hook handles lightweight context injection.
set -e

echo "==> Starting equipment-management dev environment"

# ── Backend ──────────────────────────────────────────────────────────────────
BACKEND_DIR="$(dirname "$0")/equipment_system_management"

echo "[backend] Building..."
cd "$BACKEND_DIR"
mvn clean compile -q

echo "[backend] Starting Spring Boot on port 8080..."
nohup mvn spring-boot:run > /tmp/backend.log 2>&1 &
BACKEND_PID=$!
echo "[backend] PID=$BACKEND_PID"

echo "[backend] Waiting for server to be ready..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:8080/ > /dev/null 2>&1; then
    echo "[backend] Server is up."
    break
  fi
  sleep 2
  if [ $i -eq 30 ]; then
    echo "[backend] ERROR: Server did not start in 60s. Check /tmp/backend.log"
    exit 1
  fi
done

# ── Frontend ──────────────────────────────────────────────────────────────────
FRONTEND_DIR="$(dirname "$0")/equipment-web"

echo "[frontend] Installing dependencies..."
cd "$FRONTEND_DIR"
npm install --silent

echo "[frontend] Starting Vue dev server on port 8081..."
nohup npm run serve > /tmp/frontend.log 2>&1 &
echo "[frontend] PID=$!"

echo ""
echo "==> Dev environment ready"
echo "    Backend:  http://localhost:8080"
echo "    Frontend: http://localhost:8081"
echo "    Logs:     /tmp/backend.log | /tmp/frontend.log"
