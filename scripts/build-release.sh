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
cp docs/design/migrations/19-生产发布基线.md "$RELEASE_DIR/"
cp docs/design/migrations/22-OSS对象存储部署说明.md "$RELEASE_DIR/"
cp docs/design/migrations/23-微信支付配置.md "$RELEASE_DIR/"
for migration in docs/design/migrations/24-*.sql docs/design/migrations/25-*.sql docs/design/migrations/26-*.sql docs/design/migrations/27-*.sql docs/design/migrations/28-*.sql; do
  cp "$migration" "$RELEASE_DIR/"
done

(
  cd "$RELEASE_DIR"
  shasum -a 256 "th4-decoboon-${VERSION}-build.tar.gz" > SHA256SUMS
)

echo "[release] artifact: $RELEASE_DIR/th4-decoboon-${VERSION}-build.tar.gz"
echo "[release] checksum: $RELEASE_DIR/SHA256SUMS"
