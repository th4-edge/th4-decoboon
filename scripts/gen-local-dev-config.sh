#!/usr/bin/env bash
#
# 生成本地开发密钥配置：config/application-dev.yml
#
# 背景：IDEA 直接运行 Application 主类不会加载 .env.*.local（shell 格式），
# 导致微信登录 503、OSS 走本地存储、微信支付不可用。
# Spring Boot 会自动加载 file:./config/application-dev.yml 且优先级高于 classpath，
# 因此把三个 .env 文件的白名单变量转换为 Spring 格式即可让 IDEA 直接可用。
#
# 安全约束：
#   1. 只提取白名单变量，绝不写入 DB_URL / DB_USERNAME / DB_PASSWORD / REDIS_* 等，
#      避免本地开发误连生产数据库。
#   2. 生成文件会被 .gitignore 忽略，且权限设为 600。
#   3. 值不回显到终端。
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

OUT="config/application-dev.yml"
ENV_DEV=".env.dev.local"
ENV_OSS=".env.oss.local"
ENV_PAY=".env.wechat-pay.local"

if ! grep -qxF 'config/application-dev.yml' .gitignore 2>/dev/null; then
  echo "[gen] 错误：.gitignore 未忽略 config/application-dev.yml，拒绝生成以免密钥入库" >&2
  exit 1
fi

if [ ! -f "$ENV_DEV" ]; then
  echo "[gen] 错误：缺少 $ENV_DEV（微信小程序密钥来源）" >&2
  exit 2
fi

# 按优先级加载：dev.local < oss.local < wechat-pay.local（后者覆盖前者）
set -a
. "./$ENV_DEV"
[ -f "$ENV_OSS" ] && . "./$ENV_OSS"
[ -f "$ENV_PAY" ] && . "./$ENV_PAY"
set +a

required=(WECHAT_MINI_APP_SECRET TH4_FILE_SIGNATURE_SECRET)
missing=()
for key in "${required[@]}"; do
  [ -n "${!key:-}" ] || missing+=("$key")
done
if [ "${#missing[@]}" -gt 0 ]; then
  echo "[gen] 错误：缺少必需变量：${missing[*]}" >&2
  echo "[gen] 请检查 $ENV_DEV" >&2
  exit 3
fi

mkdir -p config
umask 077
cat > "$OUT" <<YAML
# 本文件由 scripts/gen-local-dev-config.sh 从 .env.*.local 生成。
# 包含本地开发密钥，禁止提交（已在 .gitignore 中忽略）。
# 重新生成：bash scripts/gen-local-dev-config.sh
th4:
  edge:
    framework:
      file-signature-secret: "${TH4_FILE_SIGNATURE_SECRET:-}"
      file-signature-ttl-seconds: ${TH4_FILE_SIGNATURE_TTL_SECONDS:-300}
      security:
        password:
          bootstrap-token: "${TH4_PASSWORD_BOOTSTRAP_TOKEN:-}"
    components:
      storage:
        provider: "${TH4_STORAGE_PROVIDER:-local}"
        endpoint: "${ALIYUN_OSS_ENDPOINT:-}"
        bucket: "${ALIYUN_OSS_BUCKET:-decoboon}"
        access-key-id: "${ALIYUN_OSS_ACCESS_KEY_ID:-}"
        access-key-secret: "${ALIYUN_OSS_ACCESS_KEY_SECRET:-}"
        presign-ttl-seconds: ${ALIYUN_OSS_PRESIGN_TTL_SECONDS:-300}
        max-file-size-bytes: ${ALIYUN_OSS_MAX_FILE_SIZE_BYTES:-524288000}
        key-prefix: "${ALIYUN_OSS_KEY_PREFIX:-}"
      wechat:
        mini-program:
          enabled: ${WECHAT_MINI_ENABLED:-true}
          app-id: "${WECHAT_MINI_APP_ID:-}"
          app-secret: "${WECHAT_MINI_APP_SECRET:-}"
        pay:
          enabled: ${WECHAT_PAY_ENABLED:-false}
          app-id: "${WECHAT_PAY_APP_ID:-}"
          mch-id: "${WECHAT_PAY_MCH_ID:-}"
          mch-serial-no: "${WECHAT_PAY_MCH_SERIAL_NO:-}"
          private-key-path: "${WECHAT_PAY_PRIVATE_KEY_PATH:-}"
          api-v3-key: "${WECHAT_PAY_API_V3_KEY:-}"
          notify-url: "${WECHAT_PAY_NOTIFY_URL:-}"
          platform-public-key-id: "${WECHAT_PAY_PLATFORM_PUBLIC_KEY_ID:-}"
          platform-public-key-path: "${WECHAT_PAY_PLATFORM_PUBLIC_KEY_PATH:-}"
          connect-timeout-ms: ${WECHAT_PAY_CONNECT_TIMEOUT_MS:-3000}
          read-timeout-ms: ${WECHAT_PAY_READ_TIMEOUT_MS:-8000}
YAML
chmod 600 "$OUT"

echo "[gen] 已生成 ${OUT}（权限 600，已 gitignore）"
echo "[gen] 微信小程序: enabled=${WECHAT_MINI_ENABLED:-true} app-id=$([ -n "${WECHAT_MINI_APP_ID:-}" ] && echo 已配置 || echo 缺失) app-secret=$([ -n "${WECHAT_MINI_APP_SECRET:-}" ] && echo 已配置 || echo 缺失)"
echo "[gen] OSS: provider=${TH4_STORAGE_PROVIDER:-local} access-key-id=$([ -n "${ALIYUN_OSS_ACCESS_KEY_ID:-}" ] && echo 已配置 || echo 缺失)"
echo "[gen] 微信支付: enabled=${WECHAT_PAY_ENABLED:-false} mch-id=$([ -n "${WECHAT_PAY_MCH_ID:-}" ] && echo 已配置 || echo 缺失) api-v3-key=$([ -n "${WECHAT_PAY_API_V3_KEY:-}" ] && echo 已配置 || echo 缺失)"
