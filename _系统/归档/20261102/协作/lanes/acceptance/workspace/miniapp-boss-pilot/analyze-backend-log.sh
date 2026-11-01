#!/bin/bash
# 无第三参数时直接打印
# 用法: analyze-backend-log.sh <日志文件> <起始行号> [输出文件]
LOG="${1:-/tmp/decoboon-backend-run.log}"
FROM="${2:-1}"
OUT="${3:-}"
NOISE='HHH90000025|Buffer pool was not set|implements ApplicationListener|open-in-view is enabled|password setting is deprecated|MacOSDnsServerAddressStreamProvider|Appended trailing slash|Using generated security password'
{
  echo "# 后端异常分析（第 ${FROM} 行之后）"
  echo "日志文件: ${LOG}"
  echo
  echo "## 总量"
  awk -v from="$FROM" 'NR>from' "$LOG" | grep -cE "ERROR|Exception|Caused by" | sed 's/^/ERROR或异常行数: /'
  awk -v from="$FROM" 'NR>from' "$LOG" | grep -c "WARN " | sed 's/^/WARN行数: /'
  echo
  echo "## ERROR / 未捕获异常"
  awk -v from="$FROM" 'NR>from' "$LOG" | grep -nE "ERROR|Exception|Caused by" | grep -vE "$NOISE" | head -60 | cut -c1-300
  echo
  echo "## 业务失败响应（success:false / 失败）"
  awk -v from="$FROM" 'NR>from' "$LOG" | grep -E "处理失败|失败：|DefinedException|ProviderException|系统异常|系统错误" | grep -vE "$NOISE" | head -40 | cut -c1-300
  echo
  echo "## WARN（过滤框架噪音）"
  awk -v from="$FROM" 'NR>from' "$LOG" | grep "WARN " | grep -vE "$NOISE" | head -40 | cut -c1-300
}
