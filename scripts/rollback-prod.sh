#!/usr/bin/env bash
set -euo pipefail

HOST="${DECOBOON_PROD_HOST:-120.55.15.159}"
USER_NAME="${DECOBOON_PROD_USER:-root}"
SSH_KEY="${DECOBOON_PROD_SSH_KEY:-$HOME/.ssh/id_ed25519}"
SERVICE="${DECOBOON_SERVICE:-decoboon}"
API_URL="${DECOBOON_API_URL:-https://decoboon.joytsing.com}"
TARGET="${1:-}"

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes \
    -o ConnectTimeout=15 -o PasswordAuthentication=no -i "$SSH_KEY" \
    "$USER_NAME@$HOST" "TARGET='$TARGET' SERVICE='$SERVICE' API_URL='$API_URL' bash -s" <<'REMOTE'
set -euo pipefail
current_link=/opt/decoboon/th4-decoboon
previous_file=/opt/decoboon/releases/.previous
target="$TARGET"
if [ -z "$target" ]; then
  target="$(cat "$previous_file" 2>/dev/null || true)"
fi
if [ -z "$target" ] || [ ! -d "$target" ]; then
  echo "Rollback target not found: ${target:-<empty>}" >&2
  exit 2
fi
current="$(readlink -f "$current_link" 2>/dev/null || true)"
ln -sfn "$target" "$current_link"
systemctl restart "$SERVICE"
ready=false
http_code=""
body=""
for _ in $(seq 1 180); do
  if systemctl is-active --quiet "$SERVICE"; then
    http_code="$(curl -sS -o /tmp/decoboon-rollback-health.json -w '%{http_code}' "$API_URL/api/auth/session/info" 2>/dev/null || true)"
    body="$(head -c 1000 /tmp/decoboon-rollback-health.json 2>/dev/null || true)"
    if [ "$http_code" = 200 ] && printf '%s' "$body" | grep -q 'tokenIsUndefined'; then
      ready=true
      break
    fi
  fi
  sleep 1
done
rm -f /tmp/decoboon-rollback-health.json
if [ "$ready" != true ]; then
  echo "Rollback readiness check failed: http=$http_code body=$body" >&2
  exit 1
fi
printf 'previous_release=%s\nrollback_to=%s\nhealth_http=%s\n' "$current" "$target" "$http_code"
REMOTE
