#!/bin/bash
set -u
ROOT="$(cd "$(dirname "$0")/../../../../../.." && pwd)"
RUN_DIR="$ROOT/_系统/协作/lanes/development/workspace/maihong-full-business-2026-09-17"
OBS="$RUN_DIR/observer.log"
touch "$OBS"
echo "[$(date '+%F %T')] observer started" >> "$OBS"
tail -n0 -F "$ROOT/logs/log_error.log" "$ROOT/logs/log_warn.log" 2>/dev/null | while IFS= read -r line; do
  printf '[%s] %s\n' "$(date '+%F %T')" "$line" >> "$OBS"
done
