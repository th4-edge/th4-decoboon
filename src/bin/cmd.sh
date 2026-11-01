#!/bin/sh
set -eu

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${DECOBOON_ENV_FILE:-/etc/decoboon/th4-decoboon.env}"
JAVA_HOME="${JAVA_HOME:-/opt/java/jdk-22.0.2+9}"
JAVA_BIN="$JAVA_HOME/bin/java"
if [ -z "${BOOT_JAR:-}" ]; then
  set -- "$APP_HOME"/boot/th4-decoboon-*.jar
  if [ "$#" -ne 1 ] || [ ! -r "$1" ]; then
    echo "Expected exactly one readable boot JAR under $APP_HOME/boot" >&2
    exit 2
  fi
  BOOT_JAR="$1"
fi
LOADER_PATH="$APP_HOME/lib,$APP_HOME/config"
PID_DIR="$APP_HOME/run"
PID_FILE="$PID_DIR/decoboon.pid"
LOG_DIR="${LOG_DIR:-${LOG_PATH:-$APP_HOME/logs}}"
STDOUT_LOG="$LOG_DIR/stdout.log"
DEFAULT_JAVA_OPTS="-Xms1536m -Xmx3584m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

usage() {
  echo "Usage: $0 {start|stop|restart|status|check}"
  exit 1
}

load_env() {
  if [ ! -f "$ENV_FILE" ]; then
    echo "Environment file not found: $ENV_FILE" >&2
    exit 2
  fi
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
}

require_var() {
  eval "value=\${$1:-}"
  if [ -z "$value" ]; then
    echo "Required environment variable is missing: $1" >&2
    exit 2
  fi
}

require_file() {
  if [ ! -r "$1" ]; then
    echo "Required file is not readable: $1" >&2
    exit 2
  fi
}

validate() {
  require_file "$JAVA_BIN"
  require_file "$BOOT_JAR"
  require_var DB_URL
  require_var DB_USERNAME
  require_var DB_PASSWORD
  require_var REDIS_HOST
  require_var REDIS_PASSWORD
  require_var TH4_FILE_UPLOAD_DIR
  require_var TH4_FILE_PUBLIC_DIR
  require_var TH4_FILE_SIGNATURE_SECRET
  require_file "${TH4_SITE_DIR:-$APP_HOME/site}/index.html"
  require_var WECHAT_MINI_APP_ID
  require_var WECHAT_MINI_APP_SECRET

  if [ "${TH4_STORAGE_PROVIDER:-oss}" = "oss" ]; then
    require_var ALIYUN_OSS_ENDPOINT
    require_var ALIYUN_OSS_BUCKET
    require_var ALIYUN_OSS_ACCESS_KEY_ID
    require_var ALIYUN_OSS_ACCESS_KEY_SECRET
  fi

  if [ "${WECHAT_PAY_ENABLED:-false}" = "true" ]; then
    require_var WECHAT_PAY_APP_ID
    require_var WECHAT_PAY_MCH_ID
    require_var WECHAT_PAY_MCH_SERIAL_NO
    require_var WECHAT_PAY_PRIVATE_KEY_PATH
    require_var WECHAT_PAY_API_V3_KEY
    require_var WECHAT_PAY_NOTIFY_URL
    require_var WECHAT_PAY_PLATFORM_PUBLIC_KEY_ID
    require_var WECHAT_PAY_PLATFORM_PUBLIC_KEY_PATH
    require_file "$WECHAT_PAY_PRIVATE_KEY_PATH"
    require_file "$WECHAT_PAY_PLATFORM_PUBLIC_KEY_PATH"
  fi
}

is_exist() {
  if [ ! -f "$PID_FILE" ]; then
    return 1
  fi
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

start() {
  load_env
  validate
  if is_exist; then
    echo "th4-decoboon is already running, PID=$pid"
    return 0
  fi

  mkdir -p "$PID_DIR" "$LOG_DIR" "$TH4_FILE_UPLOAD_DIR" "$TH4_FILE_PUBLIC_DIR"
  cd "$APP_HOME"
  # shellcheck disable=SC2086
  nohup "$JAVA_BIN" ${JAVA_OPTS:-$DEFAULT_JAVA_OPTS} \
    -Dloader.path="$LOADER_PATH" \
    -Dspring.config.additional-location="file:$APP_HOME/config/" \
    -Dlogging.config="file:$APP_HOME/config/logback-spring.xml" \
    -jar "$BOOT_JAR" --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" \
    >> "$STDOUT_LOG" 2>&1 &
  pid=$!
  echo "$pid" > "$PID_FILE"
  sleep 3
  if kill -0 "$pid" 2>/dev/null; then
    echo "th4-decoboon started, PID=$pid, log=$STDOUT_LOG"
  else
    echo "th4-decoboon failed to start; inspect $STDOUT_LOG" >&2
    tail -80 "$STDOUT_LOG" >&2 || true
    rm -f "$PID_FILE"
    exit 1
  fi
}

stop() {
  if ! is_exist; then
    rm -f "$PID_FILE"
    echo "th4-decoboon is not running"
    return 0
  fi
  kill "$pid" 2>/dev/null || true
  count=0
  while kill -0 "$pid" 2>/dev/null && [ "$count" -lt 30 ]; do
    sleep 1
    count=$((count + 1))
  done
  if kill -0 "$pid" 2>/dev/null; then
    kill -9 "$pid" 2>/dev/null || true
    echo "th4-decoboon force stopped, PID=$pid"
  else
    echo "th4-decoboon stopped, PID=$pid"
  fi
  rm -f "$PID_FILE"
}

status() {
  if is_exist; then
    echo "th4-decoboon is running, PID=$pid"
  else
    echo "th4-decoboon is not running"
    exit 1
  fi
}

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) status ;;
  check) load_env; validate; echo "Production configuration validation passed" ;;
  *) usage ;;
esac
