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
# 2026-09-26 修正：发布包必须覆盖 24-64，包含 AI 调用与影子用量底账。
MIGRATION_START=24
MIGRATION_END=64
for number in $(seq "$MIGRATION_START" "$MIGRATION_END"); do
  matches=(docs/design/migrations/"${number}"-*.sql)
  if [ ! -e "${matches[0]}" ]; then
    echo "[release] missing migration ${number}" >&2
    exit 1
  fi
  cp "${matches[@]}" "$RELEASE_DIR/"
done

(
  cd "$RELEASE_DIR"
  shasum -a 256 "th4-decoboon-${VERSION}-build.tar.gz" > SHA256SUMS
)

echo "[release] artifact: $RELEASE_DIR/th4-decoboon-${VERSION}-build.tar.gz"
echo "[release] checksum: $RELEASE_DIR/SHA256SUMS"
