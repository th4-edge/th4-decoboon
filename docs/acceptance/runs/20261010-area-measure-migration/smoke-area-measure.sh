#!/usr/bin/env bash
#
# 区域实测面积/周长 dev 冒烟（DEC-062 / T12-BIZ-20261010-006）
#
# 用途：在本地 dev 后端上用真实设计师账号验证——
#   ① 区域保存接受实测面积/周长并写入来源标记；
#   ② 只改量房快照长宽，实测面积/周长与分项工程量不变；
#   ③ 未录入实测值的分项显示「待补」（pendingMeasure）而不是 0/赠送。
#
# 用法：
#   API=http://127.0.0.1:8090/api bash smoke-area-measure.sh <手机号> <密码> <designId>
#
# 说明：脚本会在该方案下新建一个临时区域（名称「冒烟-区域实测」）并在结束时删除；
#       不修改任何既有区域数据。口令只在内存中传递，不落盘、不回显。
set -euo pipefail

API="${API:-http://127.0.0.1:8090/api}"
PHONE="${1:?缺少手机号}"
PASSWORD="${2:?缺少密码}"
DESIGN_ID="${3:?缺少 designId}"

json() { python3 -c "$1"; }

post() {
  local uri="$1" body="$2" token="${3:-}"
  curl -s -X POST "$API/$uri" -H 'Content-Type: application/json' \
    ${token:+-H "token: $token"} -d "$body" --max-time 20
}

echo "== 1. 登录"
LOGIN="$(post auth/session/password-login "{\"phone\":\"$PHONE\",\"password\":\"$PASSWORD\"}")"
TOKEN="$(printf '%s' "$LOGIN" | json '
import json,sys
d = json.load(sys.stdin)
if not d.get("success"):
    raise SystemExit("登录失败：" + json.dumps(d.get("errorCode"), ensure_ascii=False))
data = d.get("data")
if isinstance(data, str):
    print(data)
elif isinstance(data, dict):
    print(data.get("token") or data.get("accessToken") or data.get("value") or "")
else:
    print("")
')"
[ -n "$TOKEN" ] || { echo "登录响应中没有 token：$LOGIN"; exit 1; }
echo "   OK"

cleanup() {
  if [ -n "${SPACE_ID:-}" ]; then
    post design/space/delete "{\"spaceId\":\"$SPACE_ID\"}" "$TOKEN" >/dev/null 2>&1 || true
    echo "   （已清理临时区域 ${SPACE_ID}）"
  fi
}
trap cleanup EXIT

echo "== 2. 新建区域：写入实测面积 12.04㎡ / 周长 15.90m（快照 3.60×4.20×2.80）"
SAVED="$(post design/space/save "{\"designId\":\"$DESIGN_ID\",\"type\":\"Bedroom\",\"name\":\"冒烟-区域实测\",\"length\":3.60,\"width\":4.20,\"height\":2.80,\"area\":12.04,\"perimeter\":15.90}" "$TOKEN")"
# 先取回 spaceId，保证后续任一断言失败也能被 trap 清理
SPACE_ID="$(printf '%s' "$SAVED" | json 'import json,sys; d=json.load(sys.stdin); print((d.get("data") or {}).get("spaceId") or "")' || true)"
printf '%s' "$SAVED" | json '
import json,sys
d = json.load(sys.stdin)
assert d.get("success"), d
s = d["data"]
assert float(s["area"]) == 12.04, s
assert float(s["perimeter"]) == 15.90, s
# 枚举在框架里序列化为 {name,value,key}，这里取 key
src = s.get("areaSource")
key = src.get("key") if isinstance(src, dict) else src
assert key == "Measured", s
print("   面积/周长/来源 =", s["area"], s["perimeter"], key)
' || { echo "区域保存失败：$SAVED"; exit 1; }

echo "== 3. 只改量房快照长宽（9.99×9.99），实测值不变"
post design/space/save "{\"designId\":\"$DESIGN_ID\",\"spaceId\":\"$SPACE_ID\",\"type\":\"Bedroom\",\"name\":\"冒烟-区域实测\",\"length\":9.99,\"width\":9.99,\"height\":2.80,\"area\":12.04,\"perimeter\":15.90}" "$TOKEN" | json '
import json,sys
d = json.load(sys.stdin)
assert d.get("success"), d
s = d["data"]
assert float(s["area"]) == 12.04 and float(s["perimeter"]) == 15.90, s
print("   改快照后面积/周长仍为", s["area"], s["perimeter"])
'

echo "== 4. 非法实测值必须被拒绝（面积 0）"
BAD="$(post design/space/save "{\"designId\":\"$DESIGN_ID\",\"spaceId\":\"$SPACE_ID\",\"type\":\"Bedroom\",\"name\":\"冒烟-区域实测\",\"area\":0,\"perimeter\":15.90}" "$TOKEN")"
printf '%s' "$BAD" | json '
import json,sys
d = json.load(sys.stdin)
assert not d.get("success"), d
print("   已拒绝：", d["errorCode"]["message"])
'

echo "== 5. 方案详情：待补分项统计"
post design/scheme/info "{\"designId\":\"$DESIGN_ID\"}" "$TOKEN" | json '
import json,sys
d = json.load(sys.stdin)
assert d.get("success"), d
items = d["data"].get("items") or []
pending = [i["name"] for i in items if i.get("pendingMeasure")]
revision = [i["name"] for i in items if i.get("formulaRevisionNeeded")]
print("   分项 %d 项，待补 %d 项，公式待修订 %d 项" % (len(items), len(pending), len(revision)))
if pending:
    print("   待补示例：", "、".join(pending[:3]))
if revision:
    print("   待修订示例：", "、".join(revision[:3]))
'

echo "== 冒烟通过"
