#!/bin/bash
# 2026-10-13 增减项存量 Approved（旧「待客户确认」）· 只读一键干跑
#
# 目的：在生产环境**一次性只读**完成清点，产出
#   ① 清点结果.txt   —— 人读报告（§1 规模 / §3 风险A / §4 风险B / §5 关联完整性）
#   ② plan.csv       —— 机读清单，直接喂给 2026-10-13-增减项存量Approved收口.py
#   ③ 屏幕摘要       —— 待收口单数、金额合计、风险单数
#
# 安全边界（务必遵守）：
#   * 本脚本**只执行 SELECT**，不包含任何 INSERT/UPDATE/DELETE/DDL；
#   * 不硬编码任何生产地址或口令：主机/库名由参数传入，口令走 DB_PASSWORD 环境变量或交互输入；
#   * 不连接、不修改开发/test 环境；
#   * 建议执行前先 mysqldump 备份 db_contract_change / db_project_contract / db_finance_receivable。
#
# 用法：
#   DB_PASSWORD='***' ./2026-10-13-增减项存量Approved清点-只读一键干跑.sh \
#       --host 127.0.0.1 --user root --db decoboon --out-dir /tmp/approve-settle
#   不带 DB_PASSWORD 时 mysql 会交互提示输入口令。
#
# 后续：
#   1) 人工审阅 plan.csv（剔除风险单：§3 合同额转负、§4 已完工/合同关闭）
#   2) python3 2026-10-13-增减项存量Approved收口.py ... （先 dry-run，确认后再 --apply --backup）

set -euo pipefail

HOST=""
USER="root"
DB=""
OUT_DIR="."
SQL_FILE="$(cd "$(dirname "$0")" && pwd)/2026-10-13-增减项存量Approved清点.sql"

while [ $# -gt 0 ]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --user) USER="$2"; shift 2 ;;
    --db)   DB="$2";   shift 2 ;;
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    --sql) SQL_FILE="$2"; shift 2 ;;
    -h|--help) sed -n '2,26p' "$0"; exit 0 ;;
    *) echo "未知参数: $1"; exit 2 ;;
  esac
done

[ -n "$HOST" ] || { echo "缺少 --host"; exit 2; }
[ -n "$DB" ]   || { echo "缺少 --db"; exit 2; }
[ -f "$SQL_FILE" ] || { echo "找不到清点 SQL: $SQL_FILE"; exit 2; }
mkdir -p "$OUT_DIR"

PW_ARGS=()
if [ -n "${DB_PASSWORD:-}" ]; then PW_ARGS=(-p"$DB_PASSWORD"); else PW_ARGS=(-p); fi
MYSQL=(mysql -h "$HOST" -u "$USER" "${PW_ARGS[@]}" --batch --raw --skip-column-names "$DB")

MYSQL_TABLE=(mysql -h "$HOST" -u "$USER" "${PW_ARGS[@]}" --table "$DB")

extract() {  # $1=标记名：抽取 SQL 文件中 -- [name:begin] ... -- [name:end] 之间语句
  awk -v b="-- [$1:begin]" -v e="-- [$1:end]" \
      'index($0,b){f=1;next} index($0,e){f=0} f' "$SQL_FILE"
}

section() {  # $1=标记名  $2=标题
  local out
  out="$(extract "$1" | "${MYSQL_TABLE[@]}" 2>/dev/null || true)"
  if [ -z "$out" ]; then
    echo "【$2】（无记录）"
  else
    echo "【$2】"
    echo "$out"
  fi
  echo
}

echo "[1/4] 只读清点 → $OUT_DIR/清点结果.txt（共 5 段，空结果会显式标注「无记录」）"
{
  echo "======== 存量 Approved 增减项清点报告 ========"
  echo "数据库: $USER@$HOST/$DB    时间: $(date '+%Y-%m-%d %H:%M:%S')"
  echo
  section stat          "§1 按状态分布（规模概览）"
  section risk-negative "§3 风险A：收口后合同额转负（不可直接收口）"
  section risk-closed   "§4 风险B：项目已完工/合同关闭/关联缺失（业务上不应收口）"
  section integrity     "§5 关联完整性：已存在应收 / 项目任务数 / 材料计划数"
  echo "§2 逐单清单已单独导出为 plan.csv（供收口脚本使用）"
} > "$OUT_DIR/清点结果.txt"

echo "[2/4] 导出逐单清单 → $OUT_DIR/plan.csv（供收口脚本使用）"
{
  echo "change_id,change_no,com_id,project_id,contract_id,变更类型,变更金额,成本差额,收口前合同额,收口后合同额,项目状态,合同状态,明细条数,审批时间,审批人ID,历史确认标记,历史确认时间"
  # mysql --batch 输出为 TAB 分隔；转成逗号 CSV（NULL → 空；字段内逗号 → 分号，避免破列）
  extract plan-export | "${MYSQL[@]}" \
    | awk -F'\t' 'BEGIN{OFS=","} {for(i=1;i<=NF;i++){ if($i=="NULL") $i=""; gsub(/,/,";",$i) } $1=$1; print}'
} > "$OUT_DIR/plan.csv"

echo "[3/4] 统计风险单（只读）"
RISK_NEG=$(extract risk-negative | "${MYSQL[@]}" | grep -c . || true)
RISK_CLOSED=$(extract risk-closed | "${MYSQL[@]}" | grep -c . || true)

echo "[4/4] 摘要"
TOTAL=$(($(wc -l < "$OUT_DIR/plan.csv") - 1))
[ "$TOTAL" -lt 0 ] && TOTAL=0
AMOUNT_SUM=$(awk -F',' 'NR==1{for(i=1;i<=NF;i++) if($i=="变更金额") col=i} NR>1 && col && $col ~ /^-?[0-9.]+$/ {s+=$col} END{printf "%.2f", s+0}' "$OUT_DIR/plan.csv")
{
  echo "================ 存量 Approved 增减项清点摘要 ================"
  echo "数据库            : $USER@$HOST/$DB"
  echo "待收口单数        : $TOTAL"
  echo "待收口金额合计    : $AMOUNT_SUM"
  echo "风险A（收口后合同额转负）: $RISK_NEG 单  → 见 清点结果.txt §3"
  echo "风险B（已完工/合同关闭）  : $RISK_CLOSED 单  → 见 清点结果.txt §4"
  echo "清单              : $OUT_DIR/plan.csv"
  echo "人读报告          : $OUT_DIR/清点结果.txt"
  echo "=============================================================="
  echo "下一步：人工审阅 plan.csv 后，先跑收口脚本的 dry-run，再决定 --apply。"
} | tee "$OUT_DIR/清点摘要.txt"
