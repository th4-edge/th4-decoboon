#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${DECOBOON_PROD_HOST:-120.55.15.159}"
USER_NAME="${DECOBOON_PROD_USER:-root}"
SSH_KEY="${DECOBOON_PROD_SSH_KEY:-$HOME/.ssh/id_ed25519}"
ENV_FILE="${DECOBOON_ENV_FILE:-/etc/decoboon/th4-decoboon.env}"
SERVICE="${DECOBOON_SERVICE:-decoboon}"
API_URL="${DECOBOON_API_URL:-https://decoboon.joytsing.com}"
REPORT_ROOT="$ROOT_DIR/docs/operations/incidents"

usage() {
  cat <<'EOF'
Usage:
  scripts/prod-monitor.sh status
  scripts/prod-monitor.sh collect [since]
  scripts/prod-monitor.sh latest
  scripts/prod-monitor.sh snapshot-source
  scripts/prod-monitor.sh mark-fixed <incident-id> --release <path> [--note <text>]
  scripts/prod-monitor.sh help

Environment:
  DECOBOON_PROD_HOST      default 120.55.15.159
  DECOBOON_PROD_USER      default root
  DECOBOON_PROD_SSH_KEY   default ~/.ssh/id_ed25519
  DECOBOON_ENV_FILE       default /etc/decoboon/th4-decoboon.env
  DECOBOON_SERVICE        default decoboon
  DECOBOON_API_URL        default https://decoboon.joytsing.com
EOF
}

ssh_run() {
  ssh -o StrictHostKeyChecking=no \
      -o UserKnownHostsFile=/dev/null \
      -o BatchMode=yes \
      -o ConnectTimeout=15 \
      -o PasswordAuthentication=no \
      -i "$SSH_KEY" \
      "$USER_NAME@$HOST" "$@"
}

validate_since() {
  local value="$1"
  if [[ ! "$value" =~ ^[0-9]{1,3}\ (minute|minutes|hour|hours|day|days)\ ago$ ]]; then
    echo "Invalid since value: $value" >&2
    echo "Expected values such as: 30 minutes ago, 2 hours ago, 1 day ago" >&2
    exit 2
  fi
}

status() {
  ssh_run "SERVICE='$SERVICE' API_URL='$API_URL' bash -s" <<'REMOTE'
set -u
echo "host=$(hostname)"
echo "time=$(date -Is)"
echo "service_enabled=$(systemctl is-enabled "$SERVICE" 2>/dev/null || true)"
echo "service_active=$(systemctl is-active "$SERVICE" 2>/dev/null || true)"
systemctl show "$SERVICE" -p MainPID -p ActiveState -p SubState -p ExecMainStartTimestamp 2>/dev/null || true
echo "listen=$(ss -ltnp 2>/dev/null | grep -E ':(80|443|8090)[[:space:]]' | tr '\n' ';' || true)"
health_code="$(curl -sS -o /tmp/decoboon-monitor-health.$$.json -w '%{http_code}' "$API_URL/api/auth/session/info" 2>/dev/null || true)"
echo "health_http=$health_code"
head -c 300 /tmp/decoboon-monitor-health.$$.json 2>/dev/null || true
rm -f /tmp/decoboon-monitor-health.$$.json
REMOTE
}

collect() {
  local since="${1:-30 minutes ago}"
  validate_since "$since"
  local stamp report_dir
  stamp="$(date +%Y%m%d-%H%M%S)"
  report_dir="$REPORT_ROOT/$(date +%Y-%m-%d)/$stamp"
  mkdir -p "$report_dir"

  set +e
  ssh_run "SINCE='$since' SERVICE='$SERVICE' ENV_FILE='$ENV_FILE' API_URL='$API_URL' bash -s" >"$report_dir/raw.txt" 2>&1 <<'REMOTE'
set -u
echo "== META =="
hostname
date -Is
uptime
echo "since=$SINCE"
echo "== SERVICE =="
systemctl is-enabled "$SERVICE" 2>&1 || true
systemctl is-active "$SERVICE" 2>&1 || true
systemctl show "$SERVICE" -p MainPID -p ActiveState -p SubState -p ExecMainStartTimestamp 2>&1 || true
echo "== RELEASE =="
readlink -f /opt/decoboon/th4-decoboon 2>&1 || true
readlink -f /opt/decoboon/releases/.previous 2>&1 || true
echo "== HEALTH =="
health_file="$(mktemp)"
http_code="$(curl -sS -o "$health_file" -w '%{http_code}' "$API_URL/api/auth/session/info" 2>/dev/null || true)"
echo "health_http=$http_code"
head -c 1200 "$health_file" 2>/dev/null || true
echo
rm -f "$health_file"
echo "== RESOURCES =="
df -P / /opt /var/log 2>&1 || true
free -m 2>&1 || true
echo "== LOG_ERROR =="
tail -n 1500 /var/log/th4-decoboon/log_error.log 2>&1 || true
echo
echo "== LOG_WARN =="
tail -n 500 /var/log/th4-decoboon/log_warn.log 2>&1 || true
echo
echo "== STDOUT_ERRORS =="
grep -nE 'ERROR|Exception|Caused by|Failed|failed' /opt/decoboon/th4-decoboon/logs/stdout.log 2>/dev/null | tail -n 1000 || true
echo
echo "== JOURNAL =="
journalctl -u "$SERVICE" --since "$SINCE" --no-pager -n 1200 2>&1 || true
REMOTE
  local ssh_status=$?
  set -e

  if [ "$ssh_status" -ne 0 ]; then
    {
      echo "# 生产监控采集失败"
      echo
      echo "- 采集时间：$(date -Is)"
      echo "- 目标：$USER_NAME@$HOST"
      echo "- SSH 退出码：$ssh_status"
      echo "- 原因：无法连接服务器或远程采集命令失败。"
    } >"$report_dir/summary.md"
    echo "Monitor collection failed: $report_dir/summary.md" >&2
    return 2
  fi

  set +e
  python3 "$ROOT_DIR/scripts/prod-monitor-analyze.py" "$report_dir/raw.txt" "$report_dir"
  local analyze_status=$?
  set -e

  local rel_dir="${report_dir#$ROOT_DIR/}"
  echo "report_dir=$report_dir"
  echo "relative_dir=$rel_dir"
  echo "analysis_exit=$analyze_status"
  if [ -f "$report_dir/summary.md" ]; then
    echo "--- summary ---"
    sed -n '1,220p' "$report_dir/summary.md"
  fi
  return "$analyze_status"
}

latest() {
  local latest_file
  latest_file="$(find "$REPORT_ROOT" -mindepth 3 -maxdepth 3 -name summary.md -print 2>/dev/null | sort | tail -1)"
  if [ -z "$latest_file" ]; then
    echo "No production monitor report found." >&2
    return 1
  fi
  echo "$latest_file"
  sed -n '1,260p' "$latest_file"
}

mark_fixed() {
  if [ "$#" -lt 1 ]; then
    echo "Usage: scripts/prod-monitor.sh mark-fixed <incident-id> --release <path> [--note <text>]" >&2
    exit 2
  fi
  local incident_id="$1"
  shift
  local release="" note="" fixed_at=""
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --release) release="${2:-}"; shift 2 ;;
      --note) note="${2:-}"; shift 2 ;;
      --at) fixed_at="${2:-}"; shift 2 ;;
      *) echo "Unknown mark-fixed option: $1" >&2; exit 2 ;;
    esac
  done
  if [ -z "$release" ]; then
    echo "mark-fixed requires --release <release path>" >&2
    exit 2
  fi
  python3 - "$REPORT_ROOT/registry.json" "$incident_id" "$release" "$note" "$fixed_at" <<'PY'
import json
import sys
from datetime import datetime
from pathlib import Path

path = Path(sys.argv[1])
incident_id = sys.argv[2]
release = sys.argv[3]
note = sys.argv[4]
fixed_at = sys.argv[5] or datetime.now().strftime('%Y-%m-%d %H:%M:%S')
if not path.exists():
    raise SystemExit(f'Registry not found: {path}')
data = json.loads(path.read_text(encoding='utf-8'))
matched = None
for item in data.get('incidents', []):
    if item.get('incidentId') == incident_id:
        item['status'] = 'fixed'
        item['actionable'] = False
        item['fixedAt'] = fixed_at
        item['fixedRelease'] = release
        item['fixSummary'] = note
        matched = item
        break
if not matched:
    raise SystemExit(f'Incident not found: {incident_id}')
data['updatedAt'] = datetime.now().astimezone().isoformat(timespec='seconds')
path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f"marked {incident_id} fixed at {fixed_at}, release={release}")
PY
}

snapshot_source() {
  local stamp archive
  stamp="$(date +%Y%m%d-%H%M%S)"
  archive="$ROOT_DIR/dist/source-backups/th4-decoboon-source-$stamp.tar.gz"
  mkdir -p "$(dirname "$archive")"
  tar --exclude='.git' \
      --exclude='target' \
      --exclude='dist' \
      --exclude='logs' \
      --exclude='node_modules' \
      --exclude='.env' \
      --exclude='.env.*.local' \
      --exclude='config/wechat-pay' \
      -czf "$archive" -C "$ROOT_DIR" .
  echo "source_backup=$archive"
  shasum -a 256 "$archive"
}

case "${1:-help}" in
  status) status ;;
  collect) shift; collect "${1:-30 minutes ago}" ;;
  latest) latest ;;
  snapshot-source) snapshot_source ;;
  mark-fixed) shift; mark_fixed "$@" ;;
  help|-h|--help) usage ;;
  *) usage; exit 2 ;;
esac
