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

SITE_DIR="$(cd "$ROOT_DIR/../../前端代码/th4-decoboon-site" && pwd)"
if [ ! -f "$SITE_DIR/package.json" ]; then
  echo "Site project not found: $SITE_DIR" >&2
  exit 1
fi

echo "[release] building th4-decoboon-site"
(cd "$SITE_DIR" && npm run build)

echo "[release] building th4-decoboon $VERSION"
mvn -o -q clean package -DskipTests -Dsite.dist.dir="$SITE_DIR/dist"

if [ ! -f "$ARCHIVE" ]; then
  echo "Release archive not found: $ARCHIVE" >&2
  exit 1
fi

mkdir -p "$RELEASE_DIR"
cp "$ARCHIVE" "$RELEASE_DIR/"
cp .env.prod.example "$RELEASE_DIR/"
cp docs/deploy/PRODUCTION.md "$RELEASE_DIR/"
cp docs/deploy/www.joytsing.com.http.conf "$RELEASE_DIR/"
cp docs/deploy/www.joytsing.com.conf "$RELEASE_DIR/"
cp docs/deploy/admin-decoboon.joytsing.com.conf "$RELEASE_DIR/"
cp docs/deploy/certbot-renew.service "$RELEASE_DIR/"
cp docs/deploy/certbot-renew.timer "$RELEASE_DIR/"
cp docs/design/migrations/00-迁移账本.sql "$RELEASE_DIR/"
# 2026-10-15 补：回滚脚本必须随发布包一同交付（DDL 变更要求可回滚；此前发布包只有正迁移、无回滚脚本）
if [ -d docs/design/migrations/rollback ]; then
  mkdir -p "$RELEASE_DIR/rollback"
  cp docs/design/migrations/rollback/*.sql "$RELEASE_DIR/rollback/"
fi
cp docs/design/migrations/01-空库基线.sql "$RELEASE_DIR/"
cp docs/design/migrations/19-生产发布基线.md "$RELEASE_DIR/"
cp docs/design/migrations/22-OSS对象存储部署说明.md "$RELEASE_DIR/"
cp docs/design/migrations/23-微信支付配置.md "$RELEASE_DIR/"
# 增量迁移范围：新迁移必须同步上调 MIGRATION_END，否则 DBA 会拿不到待执行脚本。
# 2026-10-10 修正：发布包覆盖 24-105；同号迁移由下方守卫拦截。
# 2026-10-12 事故（v2026.10.12.1 发布记录）：迁移 108 未进发布包，生产 ddl-auto: validate
#   缺列 → 实例启动失败并回滚（期间 502 约 18 分钟）。凡新增迁移必须先上调本范围再发版。
# 2026-10-13 上调：发布包覆盖 24-109（新增 106 区域实测口径 / 107 方案面积对齐 /
#   108 模版区域参考实测值 / 109 量房原始区域与方案区域来源）。
# 2026-10-13 上调：发布包覆盖 24-110（新增 110 门店启用/停用与排序 = P0D-04，DDL）。
# 2026-10-14 上调：发布包覆盖 24-111（新增 111 阶段状态补充本项不涉及 = GAP-054，DDL）。
# 2026-10-14 上调：发布包覆盖 24-112（新增 112 报价接受登记人 = DEC-071，DDL）。
# 2026-10-14 上调：发布包覆盖 24-113（新增 113 企业品牌与署名配置 = USER-DECISION-20261014-002，DDL）。
# 2026-10-15 上调：发布包覆盖 24-114（新增 114 企业对外信息 = USER-DECISION-20261014-002，DDL）。
# 2026-10-15 上调：发布包覆盖 24-115（新增 115 项目动态业主可见摘要 = USER-DECISION-20261014-002，DDL）。
# 2026-10-15 上调：发布包覆盖 24-116（新增 116 项目分享链接 = 亲友免登录只读通道，DDL）。
# 2026-10-15 上调：发布包覆盖 24-117（新增 117 主材下单作业单 = SOL-014/OBJ-008/BR-044/BR-045/DEC-072/DEC-073，DDL）。
# 2026-10-15 上调：发布包覆盖 24-118（新增 118 主材采购结算 = BR-045/DEC-072/DEC-073，DDL）。
# 2026-10-15 上调：发布包覆盖 24-119（新增 119 结算撤销留痕 = 用户裁决「结算撤销本轮做」，DDL）。
#   注意：deploy-prod.sh 不执行迁移；迁移由 DBA 按 docs/design/migrations/19-生产发布基线.md 单独执行并登记账本。
MIGRATION_START=24
MIGRATION_END=119
# 已知历史同号迁移：内容不同、早已应用且被文档引用，保持原编号不改名。
# 新增迁移若与既有编号重复，必须立刻改名，不得加入本清单；否则下列守卫会直接终止构建。
KNOWN_SHARED_NUMBERS="24 25"
for number in $(seq "$MIGRATION_START" "$MIGRATION_END"); do
  matches=(docs/design/migrations/"${number}"-*.sql)
  if [ ! -e "${matches[0]}" ]; then
    echo "[release] missing migration ${number}" >&2
    exit 1
  fi
  if [ "${#matches[@]}" -gt 1 ] && ! printf ' %s ' "$KNOWN_SHARED_NUMBERS" | grep -q " ${number} "; then
    echo "[release] duplicate migration number ${number}: ${matches[*]}" >&2
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
