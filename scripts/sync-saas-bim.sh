#!/usr/bin/env bash
#
# 将 saas 数据库中的 BIM 主数据同步到 decoboon，并按 SaaS 租户复制。
#
# 设计约束：
# 1. saas 只读，不改源库。
# 2. decoboon.db_tenant_company 同步 saas.oa_company。
# 3. BIM 父实体表包含 com_id；子集合通过父实体归属企业。
# 4. 第一个企业保留 saas 原主键，其余企业按企业 ID 生成独立主键段并重映射外键。
set -euo pipefail

SOURCE_HOST="${SOURCE_HOST:-127.0.0.1}"
SOURCE_PORT="${SOURCE_PORT:-3306}"
SOURCE_DB="${SOURCE_DB:-saas}"
TARGET_HOST="${TARGET_HOST:-127.0.0.1}"
TARGET_PORT="${TARGET_PORT:-3306}"
TARGET_DB="${TARGET_DB:-decoboon}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-mysqladmin}"
ID_SEGMENT="${ID_SEGMENT:-1000000}"

SOURCE_ARGS=(
  --host="${SOURCE_HOST}"
  --port="${SOURCE_PORT}"
  --user="${MYSQL_USER}"
  --protocol=TCP
  --single-transaction
  --skip-comments
  --compact
)
TARGET_ARGS=(
  --host="${TARGET_HOST}"
  --port="${TARGET_PORT}"
  --user="${MYSQL_USER}"
  --protocol=TCP
  --database="${TARGET_DB}"
)

export MYSQL_PWD="${MYSQL_PASSWORD}"

TABLES=(
  bim_construct_modular
  bim_construct_modular_criterions
  bim_construct_modular_tips
  bim_construct_construction
  bim_construct_construction_criterions
  bim_construct_construction_images
  bim_construct_construction_space_types
  bim_construct_construction_tips
  bim_construct_construction_upgrades
  bim_material_category
  bim_material_category_criterions
  bim_material_category_tips
  bim_material_brand
  bim_material_brand_criterions
  bim_material_brand_tips
  bim_material_series
  bim_material_series_criterions
  bim_material_series_spec
  bim_material_series_tips
  bim_material_material
  bim_material_material_criterions
  bim_material_material_images
  bim_material_material_space_types
  bim_material_material_styles
  bim_material_material_tips
  bim_material_material_upgrades
  bim_pack_gift_group
  bim_pack_gift_group_space_types
  bim_pack_gift_item
  bim_pack_gift_item_constructions
  bim_pack_gift_item_criterions
  bim_pack_gift_item_materials
  bim_pack_gift_pack
  bim_pack_gift_pack_criterions
  bim_pack_gift_pack_item
  bim_pack_gift_pack_items
  bim_pack_gift_pack_measures
  bim_pack_gift_pck_item_space_types
)

TENANT_TABLES=(
  bim_construct_modular
  bim_construct_construction
  bim_material_category
  bim_material_brand
  bim_material_series
  bim_material_material
  bim_pack_gift_group
  bim_pack_gift_item
  bim_pack_gift_pack
  bim_pack_gift_pack_item
)

mysql_target() {
  mysql "${TARGET_ARGS[@]}" "$@"
}

ensure_company_column() {
  local table="$1"
  local exists
  exists="$(mysql_target -N -e "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${TARGET_DB}' AND TABLE_NAME='${table}' AND COLUMN_NAME='com_id'")"
  if [ "${exists}" = "0" ]; then
    mysql_target -e "ALTER TABLE \`${table}\` ADD COLUMN com_id BIGINT NULL"
  fi
  # 同步源数据时允许短暂为空，回填后再保持逻辑非空。
  mysql_target -e "ALTER TABLE \`${table}\` MODIFY COLUMN com_id BIGINT NULL"
}

ensure_company_index() {
  local table="$1"
  local index_name="idx_${table}_com_id"
  local exists
  exists="$(mysql_target -N -e "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='${TARGET_DB}' AND TABLE_NAME='${table}' AND INDEX_NAME='${index_name}'")"
  if [ "${exists}" = "0" ]; then
    mysql_target -e "ALTER TABLE \`${table}\` ADD INDEX \`${index_name}\` (com_id)"
  fi
}

sync_companies() {
  mysql_target <<SQL
INSERT INTO db_tenant_company
(com_id, title, name, size, addr, enable_store,
 create_date_time, create_date, create_time, create_year, create_month, create_day,
 create_hour, create_minute, create_second, create_week)
SELECT com_id, name, LEFT(name, 12), 'A', addr, b'1',
       date_time, date, time, year, month, day, hour, minute, second, week
FROM ${SOURCE_DB}.oa_company
ON DUPLICATE KEY UPDATE
  title = VALUES(title), name = VALUES(name), size = VALUES(size),
  addr = VALUES(addr), enable_store = VALUES(enable_store);
SQL
  local count
  count="$(mysql_target -N -e "SELECT COUNT(*) FROM db_tenant_company")"
  if [ "${count}" = "0" ]; then
    mysql_target -e "INSERT INTO db_tenant_company (com_id,title,name,size,enable_store) VALUES (1,'装得清同步租户','同步租户','A',b'1')"
  fi
}

clone_parent() {
  local table="$1"
  local id_column="$2"
  local cid="$3"
  local offset="$4"
  local base="$5"
  shift 5
  mysql_target <<SQL
DROP TEMPORARY TABLE IF EXISTS tmp_bim_clone;
CREATE TEMPORARY TABLE tmp_bim_clone AS SELECT * FROM \`${table}\` WHERE 1=0;
INSERT INTO tmp_bim_clone SELECT * FROM \`${table}\` WHERE com_id=${base};
UPDATE tmp_bim_clone SET \`${id_column}\` = \`${id_column}\` + ${offset}, com_id = ${cid};
DELETE FROM \`${table}\` WHERE com_id = ${cid};
INSERT INTO \`${table}\` SELECT * FROM tmp_bim_clone;
SQL
  local mapping fk parent parent_id
  for mapping in "$@"; do
    fk="${mapping%%:*}"
    parent="${mapping#*:}"
    parent_id="${parent%%:*}"
    parent="${parent#*:}"
    mysql_target -e "UPDATE \`${table}\` SET \`${fk}\` = \`${fk}\` + ${offset} WHERE com_id = ${cid}"
  done
}

clone_child() {
  local table="$1"
  local parent_column="$2"
  local parent_table="$3"
  local parent_id="$4"
  local cid="$5"
  local offset="$6"
  local base="$7"
  shift 7
  local set_clause="" extra
  for extra in "$@"; do
    set_clause="${set_clause}, \`${extra}\` = \`${extra}\` + ${offset}"
  done
  mysql_target <<SQL
DROP TEMPORARY TABLE IF EXISTS tmp_bim_clone;
CREATE TEMPORARY TABLE tmp_bim_clone AS
SELECT c.* FROM \`${table}\` c
JOIN \`${parent_table}\` p ON p.\`${parent_id}\` = c.\`${parent_column}\`
WHERE p.com_id = ${base};
UPDATE tmp_bim_clone
SET \`${parent_column}\` = \`${parent_column}\` + ${offset}${set_clause};
DELETE FROM \`${table}\`
WHERE \`${parent_column}\` >= ${offset} AND \`${parent_column}\` < ${offset} + ${ID_SEGMENT};
INSERT INTO \`${table}\` SELECT * FROM tmp_bim_clone;
SQL
}

clone_bridge() {
  local table="$1"
  local filter_column="$2"
  local parent_table="$3"
  local parent_id="$4"
  local cid="$5"
  local offset="$6"
  local base="$7"
  shift 7
  local set_clause="" column
  for column in "$@"; do
    set_clause="${set_clause}, \`${column}\` = \`${column}\` + ${offset}"
  done
  mysql_target <<SQL
DROP TEMPORARY TABLE IF EXISTS tmp_bim_clone;
CREATE TEMPORARY TABLE tmp_bim_clone AS
SELECT b.* FROM \`${table}\` b
JOIN \`${parent_table}\` p ON p.\`${parent_id}\` = b.\`${filter_column}\`
WHERE p.com_id = ${base};
UPDATE tmp_bim_clone SET \`${filter_column}\` = \`${filter_column}\` + ${offset}${set_clause};
DELETE FROM \`${table}\`
WHERE \`${filter_column}\` >= ${offset} AND \`${filter_column}\` < ${offset} + ${ID_SEGMENT};
INSERT INTO \`${table}\` SELECT * FROM tmp_bim_clone;
SQL
}

SCHEMA_SQL="$(mktemp)"
DATA_SQL="$(mktemp)"
trap 'rm -f "${SCHEMA_SQL}" "${DATA_SQL}"' EXIT

echo "重建 ${TARGET_DB} 的 BIM 目标表结构..."
DROP_SQL="SET FOREIGN_KEY_CHECKS=0;"
for table in "${TABLES[@]}"; do
  DROP_SQL="${DROP_SQL} DROP TABLE IF EXISTS \`${table}\`;"
done
DROP_SQL="${DROP_SQL} SET FOREIGN_KEY_CHECKS=1;"
mysql_target -e "${DROP_SQL}"

echo "同步 saas BIM 表结构到 ${TARGET_DB}..."
mysqldump "${SOURCE_ARGS[@]}" --no-data "${SOURCE_DB}" "${TABLES[@]}" \
  | sed -E 's/^CREATE TABLE /CREATE TABLE IF NOT EXISTS /' \
  > "${SCHEMA_SQL}"
mysql_target < "${SCHEMA_SQL}"

for table in "${TENANT_TABLES[@]}"; do
  ensure_company_column "${table}"
done

echo "同步 saas 企业到 ${TARGET_DB}.db_tenant_company..."
sync_companies

echo "同步 saas BIM 原始数据..."
{
  echo "SET FOREIGN_KEY_CHECKS=0;"
  mysqldump "${SOURCE_ARGS[@]}" --no-create-info --complete-insert --replace \
    --skip-add-locks --disable-keys "${SOURCE_DB}" "${TABLES[@]}"
  echo "SET FOREIGN_KEY_CHECKS=1;"
} > "${DATA_SQL}"
mysql_target < "${DATA_SQL}"

BASE_COMPANY_ID="$(mysql_target -N -e "SELECT MIN(com_id) FROM db_tenant_company")"
if [ -z "${BASE_COMPANY_ID}" ] || [ "${BASE_COMPANY_ID}" = "NULL" ]; then
  echo "错误：db_tenant_company 中没有可用的同步租户。" >&2
  exit 1
fi

echo "将 BIM 基准数据归属到企业 ${BASE_COMPANY_ID}..."
for table in "${TENANT_TABLES[@]}"; do
  mysql_target -e "UPDATE \`${table}\` SET com_id = ${BASE_COMPANY_ID} WHERE com_id IS NULL"
done

while IFS= read -r cid; do
  [ -z "${cid}" ] && continue
  [ "${cid}" = "${BASE_COMPANY_ID}" ] && continue
  offset=$((cid * ID_SEGMENT))
  echo "为企业 ${cid} 复制 BIM 主数据，主键偏移 ${offset}..."

  clone_parent bim_construct_modular mod_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_parent bim_construct_construction const_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" "mod_id:bim_construct_modular:mod_id"
  clone_parent bim_material_category cat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_parent bim_material_brand brand_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" "cat_id:bim_material_category:cat_id"
  clone_parent bim_material_series ser_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" "cat_id:bim_material_category:cat_id" "brand_id:bim_material_brand:brand_id"
  clone_parent bim_material_material mat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" "cat_id:bim_material_category:cat_id" "brand_id:bim_material_brand:brand_id" "ser_id:bim_material_series:ser_id"
  clone_parent bim_pack_gift_group group_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_parent bim_pack_gift_pack_item item_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_parent bim_pack_gift_item item_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" "group_id:bim_pack_gift_group:group_id"
  clone_parent bim_pack_gift_pack gift_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"

  clone_child bim_construct_modular_criterions mod_id bim_construct_modular mod_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_construct_modular_tips mod_id bim_construct_modular mod_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_construct_construction_criterions const_id bim_construct_construction const_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_construct_construction_images const_id bim_construct_construction const_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_construct_construction_space_types const_id bim_construct_construction const_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_construct_construction_tips const_id bim_construct_construction const_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_construct_construction_upgrades const_id bim_construct_construction const_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_category_criterions cat_id bim_material_category cat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_category_tips cat_id bim_material_category cat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_brand_criterions brand_id bim_material_brand brand_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_brand_tips brand_id bim_material_brand brand_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_series_criterions ser_id bim_material_series ser_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_series_spec ser_id bim_material_series ser_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_series_tips ser_id bim_material_series ser_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_material_criterions mat_id bim_material_material mat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_material_images mat_id bim_material_material mat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_material_space_types mat_id bim_material_material mat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_material_styles mat_id bim_material_material mat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_material_tips mat_id bim_material_material mat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_material_material_upgrades mat_id bim_material_material mat_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_pack_gift_group_space_types group_id bim_pack_gift_group group_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_pack_gift_item_constructions item_id bim_pack_gift_item item_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" const_id
  clone_child bim_pack_gift_item_criterions item_id bim_pack_gift_item item_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_pack_gift_item_materials item_id bim_pack_gift_item item_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" mat_id
  clone_child bim_pack_gift_pack_criterions gift_id bim_pack_gift_pack gift_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_child bim_pack_gift_pck_item_space_types item_id bim_pack_gift_pack_item item_id "${cid}" "${offset}" "${BASE_COMPANY_ID}"
  clone_bridge bim_pack_gift_pack_items gift_id bim_pack_gift_pack gift_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" item_id
  clone_bridge bim_pack_gift_pack_measures gift_id bim_pack_gift_pack gift_id "${cid}" "${offset}" "${BASE_COMPANY_ID}" item_id
done < <(mysql_target -N -e "SELECT com_id FROM db_tenant_company ORDER BY com_id")

# 数据已经完整回填，再恢复企业归属列的数据库非空约束。
for table in "${TENANT_TABLES[@]}"; do
  mysql_target -e "ALTER TABLE \`${table}\` MODIFY COLUMN com_id BIGINT NOT NULL"
  ensure_company_index "${table}"
done

echo "同步完成：${SOURCE_DB} -> ${TARGET_DB}，共 ${#TABLES[@]} 张 BIM 表，租户基准企业 ${BASE_COMPANY_ID}。"
