import fs from 'node:fs'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const BASE = 'http://127.0.0.1:18090/api/'
const CNF = '/tmp/th4-owner-preview-acceptance.cnf'
const DB = 'decoboon_owner_preview_acceptance'
const PROJECT_ID = '232781889880981504'
const OUT = fileURLToPath(new URL('.', import.meta.url))
const results = []
const raw = {}

function sql(query) {
  const r = spawnSync('mysql', ['--defaults-extra-file=' + CNF, '--batch', '--skip-column-names', DB, '-e', query], { encoding: 'utf8' })
  if (r.status !== 0) throw new Error(r.stderr || 'mysql failed')
  return r.stdout.trim()
}

async function post(path, body = {}, token = '') {
  const response = await fetch(BASE + path, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const text = await response.text()
  let json
  try { json = JSON.parse(text) } catch { json = { parseError: true, text } }
  return { httpStatus: response.status, body: json }
}

async function login(phone) {
  const r = await post('auth/session/password-login', { phone, password: 'Maihong2026' })
  const token = r.body?.data?.token || ''
  return { phone, ok: !!token, token, loginResponse: r }
}

function record(id, expected, actual, detail, evidence) {
  const passed = typeof expected === 'function' ? expected(actual) : actual === expected
  results.push({ id, passed, expected: typeof expected === 'function' ? expected.toString() : expected, actual, detail, evidence })
  return passed
}

function walk(value, visitor, path = '$') {
  visitor(value, path)
  if (Array.isArray(value)) value.forEach((v, i) => walk(v, visitor, `${path}[${i}]`))
  else if (value && typeof value === 'object') Object.entries(value).forEach(([k, v]) => walk(v, visitor, `${path}.${k}`))
}

const accounts = {
  boss: '13910000001',
  storeManager: '13910000002',
  teamLeader: '13910000003',
  designer: '13910000004',
  pm: '13910000005',
  finance: '13910000006',
  marketer: '13910000007',
  nonMemberDesigner: '13910000008',
  crossCompanyBoss: '13910000009',
  otherTeamLeader: '13910000010',
  otherStoreManager: '13910000011'
}

const allowedRoles = ['boss', 'storeManager', 'teamLeader', 'designer', 'pm']
const forbiddenRoles = ['finance', 'marketer']
const deniedScopes = ['crossCompanyBoss', 'otherStoreManager', 'otherTeamLeader', 'nonMemberDesigner']

const sideEffectTables = [
  'db_project', 'db_project_stage', 'db_project_task', 'db_acceptance',
  'db_finance_receivable', 'db_finance_receipt', 'cust_customer',
  'cust_customer_access', 'cust_project_share_link'
]
const snapshotQuery = sideEffectTables.map(t => `CHECKSUM TABLE \`${t}\``).join('; ')
const beforeChecksums = sql(snapshotQuery)
const beforeCounts = sql(`
  SELECT 'project', COUNT(*) FROM db_project WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'stage', COUNT(*) FROM db_project_stage WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'task', COUNT(*) FROM db_project_task WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'acceptance', COUNT(*) FROM db_acceptance WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'receivable', COUNT(*) FROM db_finance_receivable WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'customer_access_all', COUNT(*) FROM cust_customer_access
  UNION ALL SELECT 'share_link_project', COUNT(*) FROM cust_project_share_link WHERE project_id=${PROJECT_ID};
`)
const beforeRows = sql(`
  SELECT 'project', project_id,status,progress,store_id,designer_id,manager_id,support_id,update_date_time FROM db_project WHERE project_id=${PROJECT_ID}
  UNION ALL
  SELECT 'stage', stage_id,status,CAST(progress AS CHAR),NULL,NULL,NULL,NULL,update_date_time FROM db_project_stage WHERE project_id=${PROJECT_ID}
  UNION ALL
  SELECT 'customer', cust_id,status,NULL,store_id,designer_id,emp_id,NULL,update_date_time FROM cust_customer WHERE cust_id=232781889855815680;
`)

const tokens = {}
for (const [key, phone] of Object.entries(accounts)) {
  const auth = await login(phone)
  raw[key + 'Login'] = auth.loginResponse
  tokens[key] = auth.token
  record(`LOGIN-${key}`, x => x === true, auth.ok, `账号 ${key} 密码登录`)
}
if (Object.values(tokens).some(t => !t)) {
  fs.writeFileSync(OUT + '09-api-results.json', JSON.stringify({ results, raw }, null, 2))
  throw new Error('测试账号登录失败，停止后续 API 用例')
}

const previews = {}
for (const role of allowedRoles) {
  const r = await post('project/owner-preview/info', { projectId: PROJECT_ID }, tokens[role])
  raw[role + 'Preview'] = r
  previews[role] = r.body?.data
  const d = r.body?.data || {}
  const sections = (d.sections || []).map(s => s.key)
  let forbiddenKey = ''
  walk(d, (v, p) => {
    if (!forbiddenKey && /(^|\.)(cost|costPrice|supplierPrice|purchasePrice|profit|internalRemark|supplier)(\.|\[|$)/i.test(p)) forbiddenKey = p
  })
  record(`ALLOW-${role}`, x => x === true, r.body?.success === true, `${role} 可预览指定项目`, role + 'Preview')
  record(`ACTIONS-${role}`, x => x === true, Array.isArray(d.actions) && d.actions.length === 0, 'actions=[]')
  record(`FEEDBACK-${role}`, x => x === true, !sections.includes('feedback'), '不含 feedback 分区：' + sections.join(','))
  record(`META-${role}`, x => x === true, d.previewMeta?.mode === 'EmployeeOwnerPreview' && d.previewMeta?.readOnly === true, 'previewMeta 只读标识')
  record(`WATERMARK-${role}`, x => x === true, /业主视角预览/.test(d.previewMeta?.watermark || '') === false && /工号/.test(d.previewMeta?.watermark || '') && /\d{4}-\d{2}-\d{2}/.test(d.previewMeta?.watermark || ''), '水印含员工工号与时间')
  record(`NO-INTERNAL-${role}`, x => x === true, forbiddenKey === '', forbiddenKey ? '发现内部字段路径 ' + forbiddenKey : '未发现成本/利润/供应商/内部备注键')
}

const reference = structuredClone(previews.boss || {})
delete reference.previewMeta
let consistencyIssue = ''
for (const role of allowedRoles.slice(1)) {
  const candidate = structuredClone(previews[role] || {})
  delete candidate.previewMeta
  if (JSON.stringify(candidate) !== JSON.stringify(reference)) consistencyIssue += `${role} differs;`
}
record('FIELD-CONSISTENCY', x => x === true, consistencyIssue === '', consistencyIssue || '五类允许角色返回同一业主投影字段与内容')

for (const role of forbiddenRoles) {
  const r = await post('project/owner-preview/info', { projectId: PROJECT_ID }, tokens[role])
  raw[role + 'Preview'] = r
  record(`DENY-${role}`, x => x === true, r.body?.success === false, `${role} 接口拒绝，code=${r.body?.errorCode?.code}`)
}

for (const role of deniedScopes) {
  const r = await post('project/owner-preview/info', { projectId: PROJECT_ID }, tokens[role])
  raw[role + 'Preview'] = r
  record(`DENY-SCOPE-${role}`, x => x === true, r.body?.success === false, `${role} 项目范围拒绝，code=${r.body?.errorCode?.code}`)
}

{
  const r = await post('customer/project/journey/info', { projectId: PROJECT_ID }, tokens.boss)
  raw.ownerEndpointWithEmployeeToken = r
  record('OWNER-ENDPOINT-EMPLOYEE-TOKEN', x => x === true, r.body?.success === false, `员工 token 调业主旅程接口被拒，code=${r.body?.errorCode?.code}`)
}
{
  const r = await post('customer/acceptance/update', { acceptanceId: 1, confirmed: true }, tokens.boss)
  raw.employeeAcceptanceWrite = r
  record('EMPLOYEE-ACCEPTANCE-WRITE-DENIED', x => x === true, r.body?.success === false, `员工 token 不可确认验收，code=${r.body?.errorCode?.code}`)
}
{
  const r = await post('customer/sharelink/save', { projectId: PROJECT_ID, scope: 'Family', showAmount: false }, tokens.boss)
  raw.employeeShareWrite = r
  record('EMPLOYEE-SHARE-WRITE-DENIED', x => x === true, r.body?.success === false, `员工 token 不可生成业主分享 token，code=${r.body?.errorCode?.code}`)
}
{
  const r = await post('customer/feedback/save', { projectId: PROJECT_ID, kind: 'Opinion', type: 'Other', title: '验收测试', content: '不应写入' }, tokens.boss)
  raw.employeeFeedbackWrite = r
  record('EMPLOYEE-FEEDBACK-WRITE-DENIED', x => x === true, r.body?.success === false, `员工 token 不可提交业主反馈，code=${r.body?.errorCode?.code}`)
}

// 离职/停用员工：先取得合法 token，再在隔离库停用，验证后续访问被拒。
sql("UPDATE db_tenant_employee SET status='Resigned',resigned_at=NOW(6) WHERE emp_id=1790224826490830054")
{
  const r = await post('project/owner-preview/info', { projectId: PROJECT_ID }, tokens.pm)
  raw.resignedPreview = r
  record('DENY-RESIGNED', x => x === true, r.body?.success === false, `离职员工访问被拒，code=${r.body?.errorCode?.code}`)
}
sql("UPDATE db_tenant_employee SET status='Active',resigned_at=NULL WHERE emp_id=1790224826490830054")

const afterChecksums = sql(snapshotQuery)
const afterCounts = sql(`
  SELECT 'project', COUNT(*) FROM db_project WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'stage', COUNT(*) FROM db_project_stage WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'task', COUNT(*) FROM db_project_task WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'acceptance', COUNT(*) FROM db_acceptance WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'receivable', COUNT(*) FROM db_finance_receivable WHERE project_id=${PROJECT_ID}
  UNION ALL SELECT 'customer_access_all', COUNT(*) FROM cust_customer_access
  UNION ALL SELECT 'share_link_project', COUNT(*) FROM cust_project_share_link WHERE project_id=${PROJECT_ID};
`)
const afterRows = sql(`
  SELECT 'project', project_id,status,progress,store_id,designer_id,manager_id,support_id,update_date_time FROM db_project WHERE project_id=${PROJECT_ID}
  UNION ALL
  SELECT 'stage', stage_id,status,CAST(progress AS CHAR),NULL,NULL,NULL,NULL,update_date_time FROM db_project_stage WHERE project_id=${PROJECT_ID}
  UNION ALL
  SELECT 'customer', cust_id,status,NULL,store_id,designer_id,emp_id,NULL,update_date_time FROM cust_customer WHERE cust_id=232781889855815680;
`)
record('NO-SIDE-EFFECT-CHECKSUM', x => x === true, beforeChecksums === afterChecksums && beforeCounts === afterCounts && beforeRows === afterRows, '业务表校验和、计数与关键行快照前后一致')
record('NO-CUSTOMER-ACCESS', x => x === true, (afterCounts.match(/customer_access_all\t0/) || []).length === 1, '未创建 CustomerAccess')
record('NO-SHARE-TOKEN', x => x === true, (afterCounts.match(/share_link_project\t0/) || []).length === 1, '未生成业主分享 token')

const auditRows = sql(`SELECT employee_role,result,COUNT(*),SUM(project_id IS NULL),SUM(requested_project_id=${PROJECT_ID}) FROM db_project_owner_preview_log GROUP BY employee_role,result ORDER BY employee_role,result`)
const auditColumns = sql(`SELECT column_name FROM information_schema.columns WHERE table_schema='${DB}' AND table_name='db_project_owner_preview_log' ORDER BY ordinal_position`)
const privacyForbidden = ['click','duration','stay','path','scroll','content','frequency','trajectory','read','viewed']
const privacyHits = privacyForbidden.filter(f => auditColumns.toLowerCase().split('\n').some(c => c.includes(f)))
raw.auditRows = auditRows
raw.auditColumns = auditColumns
record('AUDIT-HAS-SUCCESS', x => x === true, /Boss\tSuccess/.test(auditRows), '存在成功审计')
record('AUDIT-HAS-DENY', x => x === true, /(Finance|Marketer)\t/.test(auditRows) || /StoreManager\tNotFound/.test(auditRows) || /TeamLeader\tNotFound/.test(auditRows), '存在拒绝/无权审计')
record('AUDIT-PRIVACY', x => x === true, privacyHits.length === 0, privacyHits.length ? '敏感轨迹列：' + privacyHits.join(',') : '审计表无业主点击/停留/路径/正文/已读字段')

fs.writeFileSync(OUT + '04-api-results.json', JSON.stringify({
  environment: { base: BASE, database: DB, projectId: PROJECT_ID, osiv: false },
  results,
  audit: { rows: auditRows, columns: auditColumns.split('\n') },
  sideEffects: { beforeChecksums, afterChecksums, beforeCounts, afterCounts, beforeRows, afterRows },
  raw
}, null, 2))

const summary = results.map(r => `${r.passed ? 'PASS' : 'FAIL'}\t${r.id}\t${r.detail}`).join('\n')
fs.writeFileSync(OUT + '05-api-summary.txt', summary + '\n')
console.log(summary)
if (results.some(r => !r.passed)) process.exitCode = 1
