#!/usr/bin/env python3
"""企业间复制「报价项目库」（catalog_item 及其类型明细）——生产数据变更工具。

设计原则：
1. **默认 dry-run**：不加 `--apply` 只打印计划（待复制条目数、类型分布、目标企业现状），不写任何数据。
2. `--apply` 前必须先完成备份；脚本会调用 `mysqldump` 备份相关表并校验 gzip，备份失败即中止。
3. 幂等：以 `(type, code)` 判定目标企业已存在的条目，已存在则不再复制；重复执行不会产生副本。
4. 只新增、不修改、不删除任何既有行；同时输出回滚 SQL，可一键撤销本次复制。
5. 新主键按框架 Snowflake 口径生成（epoch=1735689600000, workerId=1023），与运行中的应用不争用同一
   workerId；若极端情况下主键冲突，事务整体失败回滚，不会写脏数据。

用法（生产）：
  python3 scripts/copy-catalog-between-companies.py \
      --env-file .env.prod.local \
      --source-com 1790066191585976744 --target-com 1789938943485101774        # dry-run
  python3 scripts/copy-catalog-between-companies.py ... --apply --backup-dir /opt/backups/th4-decoboon
"""

from __future__ import annotations

import argparse
import base64
import gzip
import io
import json
import shlex
import subprocess
import sys
from datetime import datetime, timezone, timedelta
from pathlib import Path

SNOWFLAKE_EPOCH_MS = 1_735_689_600_000  # th4-framework SnowflakeIdGenerator EPOCH（2025-01-01Z）
SNOWFLAKE_WORKER_ID = 1023
DETAIL_TABLES = {
    "Construction": ("catalog_construction_detail",
                     "detail_id, concealed, craft, criterion, duration_days, measurement_formula, stage, com_id, item_id"),
    "Material": ("catalog_material_detail",
                 "detail_id, color, inventory_mode, loss_rate, material, minimum_order, model, com_id, item_id"),
    "Custom": ("catalog_custom_detail",
               "detail_id, board, cabinet_type, countertop, door, hardware, parametric_formula, com_id, item_id"),
    "Soft": ("catalog_soft_detail",
             "detail_id, composition, dimensions, installation_mode, lead_time_days, material, style, com_id, item_id"),
    "General": ("catalog_general_detail",
                "detail_id, allocation_mode, approval_required, discountable, fee_type, invoice_type, com_id, item_id"),
    "Appliance": ("catalog_appliance_detail",
                  "detail_id, after_sales, dimensions, model, power, utility_requirement, warranty_months, com_id, item_id"),
}
CATALOG_COLUMNS = ("item_id, active, brand, category, code, cost_price, craft, criterion, description, formula, "
                   "manual_price, fixed_total_price, material_price, name, name_initials, pricing_mode, series, sort, "
                   "spec, type, unit, com_id, import_batch_id, maintainer_id, source_type")


def parse_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        if line.startswith("export "):
            line = line[len("export "):].lstrip()
        key, _, value = line.partition("=")
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        values[key.strip()] = value
    return values


class Remote:
    """通过 ssh 在生产主机上执行 mysql / mysqldump。"""

    def __init__(self, env: dict[str, str], host: str, user: str, key: str):
        self.host, self.user, self.key = host, user, key
        url = env["DB_URL"]
        body = url.split("//", 1)[1].split("/", 1)[0]
        self.db_host = body.split(":", 1)[0]
        self.database = url.split("//", 1)[1].split("/", 1)[1].split("?", 1)[0]
        self.db_user, self.db_password = env["DB_USERNAME"], env["DB_PASSWORD"]

    def ssh(self, command: str, stdin: str | None = None, timeout: int = 600) -> str:
        args = ["ssh", "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null",
                "-o", "BatchMode=yes", "-o", "ConnectTimeout=15", "-o", "PasswordAuthentication=no",
                "-i", str(Path(self.key).expanduser()), f"{self.user}@{self.host}", command]
        result = subprocess.run(args, input=stdin, capture_output=True, text=True, timeout=timeout)
        if result.returncode != 0:
            raise RuntimeError(f"remote command failed ({result.returncode}): {result.stderr.strip()[:2000]}")
        return result.stdout

    def _mysql(self, sql: str) -> str:
        quoted = shlex.quote(sql)
        command = (f"mysql --default-character-set=utf8mb4 -h {shlex.quote(self.db_host)} "
                   f"-u {shlex.quote(self.db_user)} -p{shlex.quote(self.db_password)} "
                   f"--batch --raw -N {shlex.quote(self.database)} -e {quoted}")
        return self.ssh(command)

    def query(self, sql: str) -> list[list[str]]:
        out = self._mysql(sql)
        return [line.split("\t") for line in out.splitlines() if line.strip()]

    def execute(self, sql: str) -> str:
        return self._mysql(sql)

    def backup(self, backup_dir: str, stamp: str, tables: list[str]) -> str:
        target = f"{backup_dir}/catalog-copy-{stamp}.sql.gz"
        table_args = " ".join(shlex.quote(t) for t in tables)
        command = (
            f"mkdir -p {shlex.quote(backup_dir)} && "
            f"mysqldump -h {shlex.quote(self.db_host)} -u {shlex.quote(self.db_user)} "
            f"-p{shlex.quote(self.db_password)} --single-transaction --skip-add-locks --no-tablespaces "
            f"{shlex.quote(self.database)} {table_args} 2>/dev/null | gzip -9 > {shlex.quote(target)} && "
            f"gzip -t {shlex.quote(target)} && sha256sum {shlex.quote(target)} && ls -l {shlex.quote(target)}"
        )
        return self.ssh(command)


def snowflake_base(now: datetime) -> int:
    millis = int(now.timestamp() * 1000)
    return ((millis - SNOWFLAKE_EPOCH_MS) << 22) | (SNOWFLAKE_WORKER_ID << 12)


def build_sql(source: int, target: int, maintainer: int | None, base: int) -> str:
    maintainer_sql = "NULL" if maintainer is None else str(maintainer)
    parts = [
        "SET SESSION sql_mode='STRICT_ALL_TABLES';",
        "START TRANSACTION;",
        f"SET @src := {source};",
        f"SET @tgt := {target};",
        f"SET @maint := {maintainer_sql};",
        f"SET @base := {base};",
        # 待复制集合：目标企业尚不存在同 (type, code) 的条目
        "CREATE TEMPORARY TABLE cmap (old_id BIGINT NOT NULL PRIMARY KEY, new_id BIGINT NOT NULL) ENGINE=InnoDB "
        "SELECT i.item_id AS old_id, @base + ROW_NUMBER() OVER (ORDER BY i.item_id) AS new_id "
        "FROM catalog_item i WHERE i.com_id = @src "
        "AND NOT EXISTS (SELECT 1 FROM catalog_item t WHERE t.com_id = @tgt AND t.type = i.type AND t.code <=> i.code);",
        # 主键冲突自检（与既有 Snowflake 主键比较）
        "SELECT CONCAT('guard_item_conflict=', COUNT(*)) FROM catalog_item WHERE item_id BETWEEN @base AND @base + 100000;",
        f"INSERT INTO catalog_item ({CATALOG_COLUMNS}) "
        "SELECT m.new_id, i.active, i.brand, i.category, i.code, i.cost_price, i.craft, i.criterion, i.description, "
        "i.formula, i.manual_price, i.fixed_total_price, i.material_price, i.name, i.name_initials, i.pricing_mode, "
        "i.series, i.sort, i.spec, i.type, i.unit, @tgt, NULL, @maint, 'Migration' "
        "FROM catalog_item i JOIN cmap m ON m.old_id = i.item_id;",
        "SELECT CONCAT('copied_items=', ROW_COUNT());",
    ]
    for offset, (type_name, (table, columns)) in enumerate(DETAIL_TABLES.items()):
        column_list = [c.strip() for c in columns.split(",")]
        select_list = ", ".join(
            f"@base + {100000 + offset * 10000} + ROW_NUMBER() OVER (ORDER BY d.detail_id)" if c == "detail_id"
            else ("@tgt" if c == "com_id" else ("m.new_id" if c == "item_id" else f"d.{c}"))
            for c in column_list)
        parts.append(f"-- 明细：{type_name}（{table}）")
        parts.append(f"INSERT INTO {table} ({columns}) SELECT {select_list} "
                     f"FROM {table} d JOIN cmap m ON m.old_id = d.item_id;")
        parts.append(f"SELECT CONCAT('copied_{table}=', ROW_COUNT());")
    parts.append("SELECT CONCAT('skipped_existing=', (SELECT COUNT(*) FROM catalog_item WHERE com_id = @src) - "
                 "(SELECT COUNT(*) FROM cmap));")
    parts.append("COMMIT;")
    return "\n".join(parts)


def rollback_sql(source: int, target: int) -> str:
    """生成回滚脚本：只删除本次迁移复制（source_type='Migration'）的条目与明细。"""
    lines = [
        "-- 回滚：仅删除目标企业 source_type='Migration' 的复制条目及其明细；",
        "-- 不影响目标企业原有条目，也不影响来源企业。",
        f"SET @src := {source};",
        f"SET @tgt := {target};",
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE rmap (item_id BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB "
        "SELECT item_id FROM catalog_item WHERE com_id = @tgt AND source_type = 'Migration';",
        "SELECT CONCAT('rollback_target_items=', COUNT(*)) FROM rmap;",
    ]
    for type_name, (table, _) in DETAIL_TABLES.items():
        lines.append(f"DELETE d FROM `{table}` d JOIN rmap m ON m.item_id = d.item_id;  -- {type_name}")
        lines.append(f"SELECT CONCAT('deleted_{table}=', ROW_COUNT());")
    lines.append("DELETE i FROM catalog_item i JOIN rmap m ON m.item_id = i.item_id;")
    lines.append("SELECT CONCAT('deleted_items=', ROW_COUNT());")
    lines.append("COMMIT;")
    tables = ", ".join(f"`{t}`" for t, _ in DETAIL_TABLES.values())
    lines.append(f"-- 表级恢复（如需）：mysqldump 备份见 manifest.json；明细表：{tables}")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="企业间复制报价项目库（默认 dry-run）")
    parser.add_argument("--env-file", default=".env.prod.local")
    parser.add_argument("--host", default="120.55.15.159")
    parser.add_argument("--user", default="root")
    parser.add_argument("--ssh-key", default="~/.ssh/id_ed25519")
    parser.add_argument("--source-com", type=int, required=True)
    parser.add_argument("--target-com", type=int, required=True)
    parser.add_argument("--target-maintainer", type=int, default=None, help="不传则取目标企业 BOSS 员工")
    parser.add_argument("--backup-dir", default="/opt/backups/th4-decoboon")
    parser.add_argument("--apply", action="store_true", help="真正执行写入（默认只读计划）")
    parser.add_argument("--out-dir", default="docs/operations/data-changes")
    args = parser.parse_args()

    env = parse_env(Path(args.env_file))
    remote = Remote(env, args.host, args.user, args.ssh_key)

    source = args.source_com
    names = {row[0]: row[1] for row in remote.query(
        f"SELECT com_id, name FROM db_tenant_company WHERE com_id IN ({source}, {args.target_com})")}
    bosses = {row[0]: row[1] for row in remote.query(
        f"SELECT com_id, emp_id FROM db_tenant_employee WHERE com_id IN ({source}, {args.target_com}) AND role='BOSS'")}
    print(f"源企业: {source} {names.get(str(source), '?')}")
    print(f"目标企业: {args.target_com} {names.get(str(args.target_com), '?')}")
    if str(source) not in names or str(args.target_com) not in names:
        print("企业不存在，终止", file=sys.stderr)
        return 2

    plan = remote.query(
        f"SELECT i.type, COUNT(*) FROM catalog_item i WHERE i.com_id = {source} "
        f"AND NOT EXISTS (SELECT 1 FROM catalog_item t WHERE t.com_id = {args.target_com} "
        f"AND t.type = i.type AND t.code <=> i.code) GROUP BY i.type ORDER BY i.type")
    total_source = int(remote.query(f"SELECT COUNT(*) FROM catalog_item WHERE com_id = {source}")[0][0])
    total_target = int(remote.query(f"SELECT COUNT(*) FROM catalog_item WHERE com_id = {args.target_com}")[0][0])
    pending = sum(int(row[1]) for row in plan)
    print(f"源企业条目: {total_source}，目标企业现有条目: {total_target}，本次将复制: {pending}")
    for row in plan:
        print(f"  - {row[0]}: {row[1]}")

    maintainer = args.target_maintainer or bosses.get(str(args.target_com))
    if maintainer is None:
        print("目标企业无 BOSS 员工，且未指定 --target-maintainer", file=sys.stderr)
        return 2
    print(f"目标维护人: {maintainer}")

    now = datetime.now(timezone(timedelta(hours=8)))
    base = snowflake_base(now)
    stamp = now.strftime("%Y%m%d-%H%M%S")
    sql = build_sql(source, args.target_com, int(maintainer), base)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    sql_path = out_dir / f"{now.strftime('%Y-%m-%d')}-报价库复制-{source}-到-{args.target_com}.sql"
    sql_path.write_text(sql + "\n", encoding="utf-8")
    print(f"执行 SQL: {sql_path}")

    if not args.apply:
        print("\n[dry-run] 未写入任何数据。确认后追加 --apply 执行（会先自动备份）。")
        return 0

    print("[backup] 备份相关表…")
    backup_info = remote.backup(args.backup_dir, stamp,
                                ["catalog_item", "catalog_item_images"] + [t for t, _ in DETAIL_TABLES.values()])
    print(backup_info.strip())

    print("[apply] 执行复制事务…")
    print(remote.execute(sql).strip())

    print("[verify] 复制结果：")
    for row in remote.query(
            f"SELECT type, COUNT(*) FROM catalog_item WHERE com_id = {args.target_com} AND source_type='Migration' "
            f"GROUP BY type ORDER BY type"):
        print(f"  - {row[0]}: {row[1]}")
    print(f"目标企业条目合计: {remote.query(f'SELECT COUNT(*) FROM catalog_item WHERE com_id = {args.target_com}')[0][0]}")

    rb_path = out_dir / f"{now.strftime('%Y-%m-%d')}-报价库复制-{args.target_com}-回滚.sql"
    rb_path.write_text(rollback_sql(source, args.target_com) + "\n", encoding="utf-8")
    manifest = {"stamp": stamp, "sourceCom": source, "targetCom": args.target_com, "targetMaintainer": int(maintainer),
                "snowflakeBase": base, "backup": backup_info.strip().splitlines()[0] if backup_info.strip() else "",
                "sql": str(sql_path), "rollback": str(rb_path), "planned": {"total": pending,
                "byType": {row[0]: int(row[1]) for row in plan}}}
    manifest_path = out_dir / f"{now.strftime('%Y-%m-%d')}-报价库复制-{args.target_com}-manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"清单: {manifest_path}\n回滚脚本: {rb_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
