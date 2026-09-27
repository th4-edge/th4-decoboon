#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

VERSION="$(sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -1)"
STAMP="$(date +%Y%m%d-%H%M%S)"
RELEASE_DIR="$ROOT_DIR/dist/releases/$STAMP"
ARCHIVE="$ROOT_DIR/target/th4-decoboon-${VERSION}-build.tar.gz"

if [ -z "$VERSION" ]; then
  echo "Unable to detect project version from pom.xml" >&2
  exit 1
fi

echo "[release] building th4-decoboon $VERSION"
mvn -o -q clean package -DskipTests

if [ ! -f "$ARCHIVE" ]; then
  echo "Release archive not found: $ARCHIVE" >&2
  exit 1
fi

mkdir -p "$RELEASE_DIR"
cp "$ARCHIVE" "$RELEASE_DIR/"
cp .env.prod.example "$RELEASE_DIR/"
cp docs/deploy/PRODUCTION.md "$RELEASE_DIR/"
cp docs/design/migrations/00-迁移账本.sql "$RELEASE_DIR/"
cp docs/design/migrations/01-空库基线.sql "$RELEASE_DIR/"
cp docs/design/migrations/19-生产发布基线.md "$RELEASE_DIR/"
cp docs/design/migrations/22-OSS对象存储部署说明.md "$RELEASE_DIR/"
cp docs/design/migrations/23-微信支付配置.md "$RELEASE_DIR/"
# 增量迁移范围：新迁移必须同步上调 MIGRATION_END，否则 DBA 会拿不到待执行脚本。
# 2026-09-24 修正：发布包必须覆盖 24-62，包含业主闭环、整改、意见、设计状态与 craft 快照。
MIGRATION_START=24
MIGRATION_END=63
for number in $(seq "$MIGRATION_START" "$MIGRATION_END"); do
  matches=(docs/design/migrations/"${number}"-*.sql)
  if [ ! -e "${matches[0]}" ]; then
    echo "[release] missing migration ${number}" >&2
    exit 1
  fi
  cp "${matches[@]}" "$RELEASE_DIR/"
done

# 并行工作流迁移按实际存在文件复制：64 官网线索、65 AI 点数、66 企业入驻服务台账。
# 本功能分支必须包含 66；64/65 合并到 dev 后由同一脚本自动带入。
REQUIRED_MIGRATION_66=(docs/design/migrations/66-*.sql)
[[ -e "${REQUIRED_MIGRATION_66[0]}" ]] || { echo "[release] missing migration 66" >&2; exit 1; }
for number in 64 65 66; do
  matches=(docs/design/migrations/"${number}"-*.sql)
  if [ -e "${matches[0]}" ]; then
    cp "${matches[@]}" "$RELEASE_DIR/"
  fi
done

(
  cd "$RELEASE_DIR"
  shasum -a 256 "th4-decoboon-${VERSION}-build.tar.gz" > SHA256SUMS
)

echo "[release] artifact: $RELEASE_DIR/th4-decoboon-${VERSION}-build.tar.gz"
echo "[release] checksum: $RELEASE_DIR/SHA256SUMS"
