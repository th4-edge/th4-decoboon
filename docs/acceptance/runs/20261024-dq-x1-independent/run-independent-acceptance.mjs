import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const API = process.env.API_BASE || 'http://127.0.0.1:8090/api/'
const OUT = fileURLToPath(new URL('.', import.meta.url))
const accounts = {
  boss: '19900000001', finance: '19900000002', storeManager: '19900000003', marketer: '19900000004',
  designer: '19900000005', pm: '19900000007', otherBoss: '19900001001', otherStoreManager: '19900001002'
}
const results = []
const raw = {}

function sql(query) {
  const result = spawnSync('mysql', ['-h', '127.0.0.1', '-P', '3306', '-u', 'root', '-N', '-B', 'decoboon', '-e', query], {
    encoding: 'utf8', env: { ...process.env, MYSQL_PWD: process.env.MYSQL_PWD || 'mysqladmin' }
  })
  if (result.status !== 0) throw new Error(result.stderr || 'mysql failed')
  return result.stdout.trim()
}

function check(id, pass, expected, actual, detail = '') {
  results.push({ id, pass, expected, actual, detail })
  if (!pass) throw new Error(`${id}: expected=${JSON.stringify(expected)} actual=${JSON.stringify(actual)}`)
}

async function post(route, body, token = '') {
  const started = performance.now()
  const response = await fetch(API + route, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const json = await response.json()
  return { body: json, ms: performance.now() - started }
}

async function login(phone) {
  const result = await post('auth/session/password-login', { phone, password: '123456' })
  if (!result.body?.data?.token) throw new Error(`login failed: ${phone}`)
  return result.body.data.token
}

const tokens = {}
for (const [role, phone] of Object.entries(accounts)) tokens[role] = await login(phone)

const beforeCounts = sql('select (select count(*) from quote), (select count(*) from cust_customer), (select count(*) from db_property)')

const noToken = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' })
check('NO-TOKEN-DENIED', noToken.body?.success === false, false, noToken.body?.success)
raw.noToken = noToken.body

for (const role of ['finance', 'marketer', 'pm']) {
  const denied = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens[role])
  check(`DENY-${role.toUpperCase()}`, denied.body?.success === false && denied.body?.errorCode?.code === '403', '403', denied.body?.errorCode)
  raw[`denied_${role}`] = denied.body
}

const boss = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens.boss)
const boss2 = await post('statistic/workbench/quote/page', { current: 3, size: 20, filter: 'all' }, tokens.boss)
const store = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens.storeManager)
const designer = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens.designer)
const otherBoss = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens.otherBoss)
const otherStore = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens.otherStoreManager)
raw.boss = boss.body; raw.boss2 = boss2.body; raw.store = store.body; raw.designer = designer.body; raw.otherBoss = otherBoss.body; raw.otherStore = otherStore.body

const bossData = boss.body.data
const storeData = store.body.data
const designerData = designer.body.data
check('BOSS-TOTAL-22', bossData.totalElements === '22', '22', bossData.totalElements)
check('BOSS-PAGE-20', bossData.content.length === 20, 20, bossData.content.length)
check('BOSS-PAGE2-EMPTY', boss2.body.data.content.length === 0 && boss2.body.data.totalElements === '22', { rows: 0, total: '22' }, { rows: boss2.body.data.content.length, total: boss2.body.data.totalElements })
check('STORE-TOTAL-22', storeData.totalElements === '22', '22', storeData.totalElements)
check('DESIGNER-TOTAL-14', designerData.totalElements === '14', '14', designerData.totalElements)
check('OTHER-BOSS-NO-LEAK', otherBoss.body.success === true && otherBoss.body.data.totalElements === '0', '0', otherBoss.body.data.totalElements)
check('OTHER-STORE-NO-LEAK', otherStore.body.success === true && otherStore.body.data.totalElements === '0', '0', otherStore.body.data.totalElements)

const storeIds = storeData.content.map((row) => row.quoteId).join(',')
const storeOwned = storeIds ? Number(sql(`select count(*) from quote where quote_id in (${storeIds}) and store_id=1790000000000000002`)) : 0
check('STORE-ID-SCOPE', storeOwned === storeData.content.length, storeData.content.length, storeOwned)
const designerIds = designerData.content.map((row) => row.quoteId).join(',')
const designerOwned = designerIds ? Number(sql(`select count(*) from quote where quote_id in (${designerIds}) and emp_id=1790000000000000105`)) : 0
check('DESIGNER-ID-SCOPE', designerOwned === designerData.content.length, designerData.content.length, designerOwned)

const capped = await post('statistic/workbench/quote/page', { current: 1, size: 500, filter: 'all' }, tokens.boss)
check('PAGE-CAP-100', capped.body.data.size === 100, 100, capped.body.data.size)

// Enable long statement history only for one request, then disable immediately.
sql("update performance_schema.setup_consumers set enabled='YES' where name='events_statements_history_long'")
await new Promise((resolve) => setTimeout(resolve, 100))
const single = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens.boss)
await new Promise((resolve) => setTimeout(resolve, 200))
const statementRows = sql("select sql_text from performance_schema.events_statements_history_long where sql_text is not null and lower(sql_text) like '%quote%'")
sql("update performance_schema.setup_consumers set enabled='NO' where name='events_statements_history_long'")
const quoteStatements = statementRows.split('\n').filter(Boolean)
raw.singleRequest = single.body
raw.quoteStatements = quoteStatements
const selectStatements = quoteStatements.filter((text) => /select/i.test(text))
check('SQL-COUNT-FIXED', selectStatements.length <= 3, '<=3', selectStatements.length, quoteStatements.map((text) => text.slice(0, 180)))
check('NO-ROW-BY-ROW-FETCH', !quoteStatements.some((text) => /from cust_customer c\d+_0 where c\d+_0\.cust_id\s*=\s*\?/i.test(text)), false, true)

const afterCounts = sql('select (select count(*) from quote), (select count(*) from cust_customer), (select count(*) from db_property)')
check('NO-SIDE-EFFECTS', beforeCounts === afterCounts, beforeCounts, afterCounts)

const timings = []
for (let index = 0; index < 10; index++) timings.push((await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all' }, tokens.boss)).ms)
timings.sort((a, b) => a - b)
const p95 = timings[Math.min(9, Math.ceil(timings.length * 0.95) - 1)]
check('P95-UNDER-500MS', p95 <= 500, '<=500ms', `${p95.toFixed(1)}ms`)

const summary = { generatedAt: new Date().toISOString(), passed: results.filter((item) => item.pass).length, total: results.length, p95Ms: p95, beforeCounts, afterCounts, results, raw }
fs.writeFileSync(path.join(OUT, 'acceptance-results.json'), JSON.stringify(summary, null, 2))
console.log(JSON.stringify({ passed: summary.passed, total: summary.total, p95Ms: Number(p95.toFixed(1)), sqlStatements: selectStatements.length }, null, 2))
