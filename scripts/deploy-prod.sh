#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${DECOBOON_PROD_HOST:-120.55.15.159}"
USER_NAME="${DECOBOON_PROD_USER:-root}"
SSH_KEY="${DECOBOON_PROD_SSH_KEY:-$HOME/.ssh/id_ed25519}"
ENV_FILE="${DECOBOON_ENV_FILE:-/etc/decoboon/th4-decoboon.env}"
SERVICE="${DECOBOON_SERVICE:-decoboon}"
API_URL="${DECOBOON_API_URL:-https://decoboon.joytsing.com}"
RELEASE_DIR=""
RELEASE_STAMP="${DECOBOON_RELEASE_STAMP:-$(date +%Y%m%d-%H%M%S)}"

usage() {
  cat <<'EOF'
Usage:
  scripts/deploy-prod.sh --release-dir <dist/releases/YYYYmmdd-HHMMSS>

The release directory must contain one build archive and SHA256SUMS.
The script uploads, extracts, validates, switches the current symlink,
restarts systemd, performs an HTTPS smoke test, and rolls back on failure.
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --release-dir) RELEASE_DIR="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
done

if [ -z "$RELEASE_DIR" ] || [ ! -d "$RELEASE_DIR" ]; then
  usage >&2
  exit 2
fi

ARCHIVE="$(find "$RELEASE_DIR" -maxdepth 1 -type f -name '*.tar.gz' -print -quit)"
if [ -z "$ARCHIVE" ]; then
  echo "Release archive not found in $RELEASE_DIR" >&2
  exit 2
fi
if [ ! -f "$RELEASE_DIR/SHA256SUMS" ]; then
  echo "SHA256SUMS not found in $RELEASE_DIR" >&2
  exit 2
fi

# 防止绕过标准构建、直接上传 Spring Boot repackage JAR。
python3 "$ROOT_DIR/scripts/verify-release-boot-jar.py" "$ARCHIVE"
python3 "$ROOT_DIR/scripts/verify-release-contracts.py" "$ARCHIVE"

(
  cd "$RELEASE_DIR"
  shasum -a 256 -c SHA256SUMS
)

ARCHIVE_NAME="$(basename "$ARCHIVE")"
REMOTE_TMP="/tmp/$ARCHIVE_NAME.$RELEASE_STAMP"
NEW_RELEASE="/opt/decoboon/releases/$RELEASE_STAMP"
BACKUP_DIR="/opt/backups/th4-decoboon/$RELEASE_STAMP"

echo "[deploy] uploading $ARCHIVE_NAME to $USER_NAME@$HOST"
scp -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes \
    -o ConnectTimeout=15 -o PasswordAuthentication=no -i "$SSH_KEY" \
    "$ARCHIVE" "$USER_NAME@$HOST:$REMOTE_TMP"

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes \
    -o ConnectTimeout=15 -o PasswordAuthentication=no -i "$SSH_KEY" \
    "$USER_NAME@$HOST" "NEW_RELEASE='$NEW_RELEASE' REMOTE_TMP='$REMOTE_TMP' ENV_FILE='$ENV_FILE' SERVICE='$SERVICE' API_URL='$API_URL' BACKUP_DIR='$BACKUP_DIR' bash -s" <<'REMOTE'
set -euo pipefail

current_link=/opt/decoboon/th4-decoboon
previous_file=/opt/decoboon/releases/.previous
previous="$(readlink -f "$current_link" 2>/dev/null || true)"

rollback() {
  echo "[deploy] smoke check failed; rolling back" >&2
  if [ -n "$previous" ] && [ -d "$previous" ]; then
    ln -sfn "$previous" "$current_link"
    systemctl restart "$SERVICE" || true
    for _ in $(seq 1 180); do
      rollback_code="$(curl -sS -o /tmp/decoboon-rollback-check.json -w '%{http_code}' "$API_URL/api/auth/session/info" 2>/dev/null || true)"
      rollback_body="$(head -c 500 /tmp/decoboon-rollback-check.json 2>/dev/null || true)"
      if [ "$rollback_code" = 200 ] && printf '%s' "$rollback_body" | grep -q 'tokenIsUndefined'; then
        rm -f /tmp/decoboon-rollback-check.json
        break
      fi
      sleep 1
    done
    rm -f /tmp/decoboon-rollback-check.json
    systemctl --no-pager --lines=80 status "$SERVICE" >&2 || true
  fi
  exit 1
}

mkdir -p "$NEW_RELEASE" "$BACKUP_DIR" /opt/decoboon/releases
tar -xzf "$REMOTE_TMP" -C "$NEW_RELEASE" --strip-components=1

if [ -n "$previous" ] && [ -d "$previous/config/wechat-pay" ]; then
  mkdir -p "$NEW_RELEASE/config/wechat-pay"
  cp -a "$previous/config/wechat-pay/." "$NEW_RELEASE/config/wechat-pay/"
fi

chown -R decoboon:decoboon "$NEW_RELEASE"
chmod 750 "$NEW_RELEASE/config/wechat-pay" 2>/dev/null || true
chmod 600 "$NEW_RELEASE/config/wechat-pay/apiclient_key.pem" 2>/dev/null || true
chmod 644 "$NEW_RELEASE/config/wechat-pay/pub_key.pem" 2>/dev/null || true

sudo -u decoboon env DECOBOON_ENV_FILE="$ENV_FILE" "$NEW_RELEASE/bin/cmd.sh" check

if [ -n "$previous" ]; then
  printf '%s\n' "$previous" >"$previous_file"
fi

ln -sfn "$NEW_RELEASE" "$current_link"
systemctl restart "$SERVICE"

ready=false
http_code=""
health_body=""
for _ in $(seq 1 180); do
  if systemctl is-active --quiet "$SERVICE"; then
    http_code="$(curl -sS -o /tmp/decoboon-deploy-health.json -w '%{http_code}' "$API_URL/api/auth/session/info" 2>/dev/null || true)"
    health_body="$(head -c 1000 /tmp/decoboon-deploy-health.json 2>/dev/null || true)"
    if [ "$http_code" = 200 ] && printf '%s' "$health_body" | grep -q 'tokenIsUndefined'; then
      ready=true
      break
    fi
  fi
  sleep 1
done
rm -f /tmp/decoboon-deploy-health.json
if [ "$ready" != true ]; then
  echo "[deploy] readiness check failed after 180s: http=$http_code body=$health_body" >&2
  systemctl --no-pager --lines=120 status "$SERVICE" >&2 || true
  tail -120 "$NEW_RELEASE/logs/stdout.log" >&2 2>/dev/null || true
  rollback
fi

cat >"$NEW_RELEASE/RELEASE_INFO" <<EOF
deployed_at=$(date -Is)
release_dir=$NEW_RELEASE
previous_release=$previous
source_archive=$REMOTE_TMP
health_http=$http_code
EOF
chown decoboon:decoboon "$NEW_RELEASE/RELEASE_INFO"
rm -f "$REMOTE_TMP"
echo "[deploy] release activated: $NEW_RELEASE"
echo "[deploy] previous release: ${previous:-none}"
REMOTE
