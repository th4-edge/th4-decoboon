import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const BASE = process.env.API_BASE || 'http://127.0.0.1:8090/api/'
const OUT = fileURLToPath(new URL('.', import.meta.url))
const COMPANY_ID = 1790000000000000001n
const STORE_ID = 1790000000000000002n
const DESIGNER_ID = 1790000000000000105n
const accounts = {
  boss: ['19900000001', '123456'],
  finance: ['19900000002', '123456'],
  storeManager: ['19900000003', '123456'],
  marketer: ['19900000004', '123456'],
  designer: ['19900000005', '123456'],
  pm: ['19900000007', '123456']
}
const results = []
let raw = {}
const activeCustomer = `c.status in ('New','Following','PendingMeasure','Quoted') and not exists (
  select 1 from cust_customer_loss cl
  where cl.com_id=q.com_id and cl.property_id=q.property_id and cl.restored_at is null
)`

function record(id, passed, expected, actual, detail = '') {
  results.push({ id, passed, expected, actual, detail })
  if (!passed) throw new Error(`${id} failed: expected=${JSON.stringify(expected)} actual=${JSON.stringify(actual)}`)
}

async function post(route, body = {}, token = '') {
  const started = performance.now()
  const response = await fetch(BASE + route, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const text = await response.text()
  let json
  try { json = JSON.parse(text) } catch { json = { parseError: true, text } }
  return { httpStatus: response.status, body: json, durationMs: performance.now() - started }
}

async function login([phone, password]) {
  const response = await post('auth/session/password-login', { phone, password })
  const token = response.body?.data?.token
  if (!token) throw new Error(`login failed for ${phone}: ${JSON.stringify(response.body)}`)
  return token
}

function sql(query) {
  const result = spawnSync('mysql', ['-h', '127.0.0.1', '-P', '3306', '-u', 'root', '-N', '-B', 'decoboon', '-e', query], {
    encoding: 'utf8',
    env: { ...process.env, MYSQL_PWD: process.env.MYSQL_PWD || 'mysqladmin' }
  })
  if (result.status !== 0) throw new Error(result.stderr || 'mysql failed')
  return result.stdout.trim()
}

function request(page = 1, size = 20, filter = 'all', keyword = '') {
  return { current: page, size, filter, keyword }
}

const tokens = {}
for (const [role, account] of Object.entries(accounts)) tokens[role] = await login(account)
const frozenLeader = await post('auth/session/password-login', { phone: '19900000009', password: '123456' })
raw.frozenTeamLeaderLogin = frozenLeader.body
record('DENY-TEAMLEADER-LOGIN', frozenLeader.body?.success === false && frozenLeader.body?.errorCode?.code === '403', { success: false, code: '403' }, frozenLeader.body?.errorCode)

const expected = {
  boss: Number(sql(`select count(*) from quote q join cust_customer c on c.cust_id=q.customer_id where q.com_id=${COMPANY_ID} and q.status in ('Draft','Submitted','Rejected') and ${activeCustomer}`)),
  store: Number(sql(`select count(*) from quote q join cust_customer c on c.cust_id=q.customer_id where q.com_id=${COMPANY_ID} and q.status in ('Draft','Submitted','Rejected') and ${activeCustomer} and q.store_id=${STORE_ID}`)),
  designer: Number(sql(`select count(*) from quote q join cust_customer c on c.cust_id=q.customer_id where q.com_id=${COMPANY_ID} and q.status in ('Draft','Submitted','Rejected') and ${activeCustomer} and q.emp_id=${DESIGNER_ID}`)),
  draft: Number(sql(`select count(*) from quote q join cust_customer c on c.cust_id=q.customer_id where q.com_id=${COMPANY_ID} and q.status='Draft' and ${activeCustomer}`))
}

const bossPage1 = await post('statistic/workbench/quote/page', request(1, 20), tokens.boss)
const bossPage2 = await post('statistic/workbench/quote/page', request(2, 20), tokens.boss)
raw.bossPage1 = bossPage1.body
raw.bossPage2 = bossPage2.body
record('BOSS-SCOPE-TOTAL', bossPage1.body?.data?.totalElements === String(expected.boss), String(expected.boss), bossPage1.body?.data?.totalElements)
record('BOSS-PAGE-SIZE', bossPage1.body?.data?.content?.length === Math.min(20, expected.boss), Math.min(20, expected.boss), bossPage1.body?.data?.content?.length)
const ids1 = new Set((bossPage1.body?.data?.content || []).map((item) => item.quoteId))
const ids2 = new Set((bossPage2.body?.data?.content || []).map((item) => item.quoteId))
record('BOSS-PAGE-NO-OVERLAP', [...ids1].every((id) => !ids2.has(id)), true, [...ids1].some((id) => ids2.has(id)))
record('BOSS-PAGE2-COUNT', bossPage2.body?.data?.content?.length === Math.max(0, expected.boss - 20), Math.max(0, expected.boss - 20), bossPage2.body?.data?.content?.length)

const capped = await post('statistic/workbench/quote/page', request(1, 500), tokens.boss)
raw.sizeCap = capped.body
record('SERVER-PAGE-CAP', capped.body?.data?.size === 100, 100, capped.body?.data?.size)

const draft = await post('statistic/workbench/quote/page', request(1, 20, 'draft'), tokens.boss)
raw.draft = draft.body
record('FILTER-DRAFT-TOTAL', draft.body?.data?.totalElements === String(expected.draft), String(expected.draft), draft.body?.data?.totalElements)
record('FILTER-DRAFT-ROWS', (draft.body?.data?.content || []).every((item) => item.quoteStatus === 'Draft'), true, draft.body?.data?.content?.map((item) => item.quoteStatus))

const sampleNo = bossPage1.body?.data?.content?.[0]?.quoteNo
const keyword = await post('statistic/workbench/quote/page', request(1, 20, 'all', sampleNo), tokens.boss)
raw.keyword = keyword.body
record('KEYWORD-SERVER-FILTER', keyword.body?.data?.totalElements === '1' && keyword.body?.data?.content?.[0]?.quoteNo === sampleNo, { total: '1', quoteNo: sampleNo }, { total: keyword.body?.data?.totalElements, quoteNo: keyword.body?.data?.content?.[0]?.quoteNo })

const store = await post('statistic/workbench/quote/page', request(1, 20), tokens.storeManager)
raw.storeManager = store.body
record('STORE-TOTAL', store.body?.data?.totalElements === String(expected.store), String(expected.store), store.body?.data?.totalElements)
const storeIds = (store.body?.data?.content || []).map((item) => item.quoteId).join(',')
if (storeIds) {
  const inStore = Number(sql(`select count(*) from quote where quote_id in (${storeIds}) and store_id=${STORE_ID}`))
  record('STORE-ROWS-IN-STORE', inStore === store.body.data.content.length, store.body.data.content.length, inStore)
}

const designer = await post('statistic/workbench/quote/page', request(1, 20), tokens.designer)
raw.designer = designer.body
record('DESIGNER-TOTAL', designer.body?.data?.totalElements === String(expected.designer), String(expected.designer), designer.body?.data?.totalElements)
const designerIds = (designer.body?.data?.content || []).map((item) => item.quoteId).join(',')
if (designerIds) {
  const ownCount = Number(sql(`select count(*) from quote where quote_id in (${designerIds}) and emp_id=${DESIGNER_ID}`))
  record('DESIGNER-ROWS-OWNED', ownCount === designer.body.data.content.length, designer.body.data.content.length, ownCount)
}

for (const role of ['finance', 'marketer', 'pm']) {
  const denied = await post('statistic/workbench/quote/page', request(), tokens[role])
  raw[`denied_${role}`] = denied.body
  record(`DENY-${role.toUpperCase()}`, denied.body?.success === false && denied.body?.errorCode?.code === '403', { success: false, code: '403' }, denied.body?.errorCode)
}

const timings = []
for (let i = 0; i < 10; i++) timings.push((await post('statistic/workbench/quote/page', request(1, 20), tokens.boss)).durationMs)
timings.sort((a, b) => a - b)
const p95 = timings[Math.min(timings.length - 1, Math.ceil(timings.length * 0.95) - 1)]
raw.timingsMs = timings
raw.p95Ms = p95
record('PERF-P95', p95 <= 500, '<=500ms', `${p95.toFixed(1)}ms`)

const passed = results.filter((item) => item.passed).length
const output = { base: BASE, expected, results, passed, total: results.length, timingsMs: timings, p95Ms: p95, raw }
fs.writeFileSync(path.join(OUT, 'http-results.json'), JSON.stringify(output, null, 2))
console.log(JSON.stringify({ passed, total: results.length, expected, p95Ms: Number(p95.toFixed(1)) }, null, 2))
