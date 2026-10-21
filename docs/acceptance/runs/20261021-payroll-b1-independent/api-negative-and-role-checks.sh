#!/usr/bin/env bash
set -u
BASE='http://127.0.0.1:8080/api/'
EVIDENCE_DIR='docs/acceptance/runs/20261021-payroll-b1-independent'
TMP_DIR=$(mktemp -d /tmp/payroll-b1-api.XXXXXX)
RESULTS="$EVIDENCE_DIR/api-negative-and-role-results.jsonl"
: > "$RESULTS"

login() {
  local phone="$1" password="$2" actor="$3"
  curl -sS -X POST "${BASE}auth/session/password-login" \
    -H 'Content-Type: application/json' \
    --data "{\"phone\":\"$phone\",\"password\":\"$password\"}" \
    | jq -er '.data.token' > "$TMP_DIR/${actor}.token"
}

call_api() {
  local actor="$1" label="$2" uri="$3" json="$4" expected="$5"
  local token response http body success code message data_summary
  token=$(cat "$TMP_DIR/${actor}.token")
  response=$(curl -sS -w '\n%{http_code}' -X POST "${BASE}${uri}" \
    -H 'Content-Type: application/json' -H "token: $token" --data "$json")
  http=${response##*$'\n'}
  body=${response%$'\n'*}
  success=$(printf '%s' "$body" | jq -r '.success // false')
  code=$(printf '%s' "$body" | jq -r '.errorCode.code // empty')
  message=$(printf '%s' "$body" | jq -r '.errorCode.message // empty')
  data_summary=$(printf '%s' "$body" | jq -c 'if .success == true then (.data | if type == "object" then {schemeId,empId,name,baseSalary,scheduledDays,effectiveFrom,effectiveTo,status,locked,lockedReason,contentCount:(.content? // empty | if type=="array" then length else null end)} else . end) else null end' 2>/dev/null || echo null)
  local expected_ok=false
  if [[ "$code" == "$expected" ]]; then expected_ok=true; fi
  jq -nc \
    --arg actor "$actor" --arg label "$label" --arg uri "$uri" --arg request "$json" \
    --argjson http "$http" --argjson success "$success" --arg code "$code" \
    --arg message "$message" --arg expected "$expected" --argjson expectedOk "$expected_ok" \
    --argjson data "$data_summary" \
    '{actor:$actor,label:$label,uri:$uri,request:($request|fromjson),httpStatus:$http,success:$success,providerCode:$code,message:$message,expectedProviderCode:$expected,expectationMet:$expectedOk,data:$data}' >> "$RESULTS"
  printf '%s\t%s\thttp=%s success=%s code=%s expected=%s\n' "$actor" "$label" "$http" "$success" "$code" "$expected"
}

login "${PAYROLL_FINANCE_PHONE:?}" "${PAYROLL_TEST_PASSWORD:?}" finance
login "${PAYROLL_STOREMANAGER_PHONE:?}" "${PAYROLL_TEST_PASSWORD:?}" storemanager
login "${PAYROLL_BOSS_PHONE:?}" "${PAYROLL_TEST_PASSWORD:?}" boss

# StoreManager must not write.
call_api storemanager '店长新增薪酬方案被拒' 'payroll/compensation/save' \
  '{"empId":1790224826490830054,"baseSalary":1234,"scheduledDays":22,"effectiveFrom":"2026-12-31","remark":"独立验收-店长负向"}' '403'

# Finance store-dimension tampering: nonexistent and another company's store.
call_api finance '财务不存在storeId被拒' 'payroll/compensation/page' \
  '{"current":1,"size":20,"storeId":999999999999999999}' '403'
call_api finance '财务跨企业storeId-薪酬页被拒' 'payroll/compensation/page' \
  '{"current":1,"size":20,"storeId":1789989046751315279}' '403'
call_api finance '财务跨企业storeId-员工选择器被拒' 'payroll/employee/page' \
  '{"current":1,"size":200,"storeId":1789989046751315279}' '403'

# Locked facts: existing 吴濛濛 scheme must reject update/delete.
call_api finance '事实锁定方案更新被拒' 'payroll/compensation/update' \
  '{"schemeId":233529294909865984,"baseSalary":5001}' '409'
call_api finance '事实锁定方案删除被拒' 'payroll/compensation/delete' \
  '{"schemeId":233529294909865984}' '409'

# Server-side validation boundary checks on an employee without facts.
call_api finance '缺月应出勤被拒' 'payroll/compensation/save' \
  '{"empId":1790224826490830054,"baseSalary":5000,"effectiveFrom":"2026-12-01","remark":"独立验收-缺字段"}' '400'
call_api finance '工资为0被拒' 'payroll/compensation/save' \
  '{"empId":1790224826490830054,"baseSalary":0,"scheduledDays":22,"effectiveFrom":"2026-12-01","remark":"独立验收-零工资"}' '400'
call_api finance '应出勤32天被拒' 'payroll/compensation/save' \
  '{"empId":1790224826490830054,"baseSalary":5000,"scheduledDays":32,"effectiveFrom":"2026-12-01","remark":"独立验收-32天"}' '400'
call_api finance '失效日期早于生效被拒' 'payroll/compensation/save' \
  '{"empId":1790224826490830054,"baseSalary":5000,"scheduledDays":22,"effectiveFrom":"2026-12-31","effectiveTo":"2026-12-01","remark":"独立验收-倒置日期"}' '400'
call_api finance '同员工同生效日期重复被拒' 'payroll/compensation/save' \
  '{"empId":1790221992067828768,"baseSalary":5000,"scheduledDays":27,"effectiveFrom":"2026-07-15","remark":"独立验收-重复版本"}' '409'

# BOSS write path, then clean up through BOSS delete.
boss_token=$(cat "$TMP_DIR/boss.token")
boss_save_response=$(curl -sS -X POST "${BASE}payroll/compensation/save" \
  -H 'Content-Type: application/json' -H "token: $boss_token" \
  --data '{"empId":1790224826490830054,"baseSalary":1234,"scheduledDays":22,"effectiveFrom":"2026-11-01","remark":"独立验收-BOSS写入临时数据"}')
boss_save_success=$(printf '%s' "$boss_save_response" | jq -r '.success // false')
boss_scheme_id=$(printf '%s' "$boss_save_response" | jq -r '.data.schemeId // empty')
boss_update_response='{}'
boss_update_success=false
if [[ "$boss_save_success" == 'true' && -n "$boss_scheme_id" ]]; then
  boss_update_response=$(curl -sS -X POST "${BASE}payroll/compensation/update" \
    -H 'Content-Type: application/json' -H "token: $boss_token" \
    --data "{\"schemeId\":$boss_scheme_id,\"baseSalary\":2345,\"remark\":\"独立验收-BOSS更新临时数据\"}")
  boss_update_success=$(printf '%s' "$boss_update_response" | jq -r '.success // false')
fi
jq -nc --argjson saveSuccess "$boss_save_success" --arg schemeId "$boss_scheme_id" --argjson updateSuccess "$boss_update_success" \
  '{actor:"boss",label:"BOSS真实写入并更新临时方案",saveSuccess:$saveSuccess,updateSuccess:$updateSuccess,schemeId:$schemeId,expectationMet:($saveSuccess and $updateSuccess)}' >> "$RESULTS"

cleanup_code=''
if [[ -n "$boss_scheme_id" && "$boss_scheme_id" != 'null' ]]; then
  cleanup_response=$(curl -sS -X POST "${BASE}payroll/compensation/delete" \
    -H 'Content-Type: application/json' -H "token: $boss_token" \
    --data "{\"schemeId\":$boss_scheme_id}")
  cleanup_code=$(printf '%s' "$cleanup_response" | jq -r 'if .success then "DELETED" else (.errorCode.code // "ERROR") end')
fi
jq -nc --arg cleanupCode "$cleanup_code" --arg schemeId "$boss_scheme_id" \
  '{actor:"boss",label:"BOSS临时方案清理",cleanupCode:$cleanupCode,schemeId:$schemeId,expectationMet:($cleanupCode=="DELETED")}' >> "$RESULTS"

jq -s '.' "$RESULTS" > "$EVIDENCE_DIR/api-negative-and-role-results.json"
rm -rf "$TMP_DIR"
