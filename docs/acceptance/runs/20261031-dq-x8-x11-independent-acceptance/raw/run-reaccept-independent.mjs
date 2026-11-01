import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const API = process.env.API_BASE || 'http://127.0.0.1:8092/api'
const OUT = path.dirname(fileURLToPath(import.meta.url))
const projectId = '241940507872198656'
const contractId = '241939522005237760'
const users = {
  boss: { phone: '13910002001', password: process.env.TH4_BOSS_PASSWORD },
  storeManager: { phone: '13910002002', password: process.env.TH4_STORE_PASSWORD },
  designer: { phone: '13910002004', password: process.env.TH4_DESIGNER_PASSWORD }
}
const checks = []
const responses = {}

function check(id, pass, expected, actual, detail = '') {
  checks.push({ id, pass: Boolean(pass), expected, actual, detail })
}
function num(v) { return v == null ? null : Number(v) }
function ids(data) { return (data?.sources ?? []).map((row) => row.sourceLineId ?? row.sourceChangeItemId) }
function sql(query) {
  const result = spawnSync('mysql', ['-h127.0.0.1', '-uroot', '-N', '-B', 'decoboon', '-e', query], {
    encoding: 'utf8', env: { ...process.env, MYSQL_PWD: process.env.MYSQL_PWD || '' }
  })
  if (result.status !== 0) throw new Error(result.stderr || 'mysql failed')
  return result.stdout.trim().split('\n').filter(Boolean)
}
async function post(route, body, token = '') {
  const started = performance.now()
  const response = await fetch(`${API}/${route}`, {
    method: 'POST', headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const text = await response.text()
  let json = null
  try { json = JSON.parse(text) } catch {}
  return { status: response.status, ms: Number((performance.now() - started).toFixed(2)), bytes: Buffer.byteLength(text), json }
}
async function login(role) {
  if (!users[role].password) throw new Error(`missing password env for ${role}`)
  const result = await post('auth/session/password-login', users[role])
  const token = result.json?.data?.token
  check(`LOGIN-${role.toUpperCase()}`, result.status === 200 && result.json?.success === true && Boolean(token), 'success token', { status: result.status, success: result.json?.success, hasToken: Boolean(token) })
  return token
}
const request = (extra = {}) => ({ contractId, current: 1, size: 20, ...extra })

const tokens = {}
for (const role of ['boss', 'storeManager', 'designer']) tokens[role] = await login(role)

const expectedLines = sql(`SELECT line_id FROM db_contract_line WHERE contract_id=${contractId} ORDER BY sort,line_id`)
const expectedEffective = sql(`SELECT ci.change_item_id FROM db_contract_change_item ci JOIN db_contract_change c ON c.change_id=ci.change_id WHERE c.contract_id=${contractId} AND c.status='CustomerConfirmed' ORDER BY ci.change_item_id`)
const expectedOrder = [...expectedLines, ...expectedEffective]
check('EXPECTED-ORDER-SOURCE-DB', expectedOrder.length === 7 && new Set(expectedOrder).size === 7, { count: 7, unique: 7 }, { count: expectedOrder.length, unique: new Set(expectedOrder).size, expectedOrder })

const defaultBoss = await post('project/contract-change/range', request(), tokens.boss)
responses.defaultBoss = defaultBoss
check('DEFAULT-CURRENT', num(defaultBoss.json?.data?.current) === 1, 1, num(defaultBoss.json?.data?.current))
check('DEFAULT-SIZE', num(defaultBoss.json?.data?.size) === 20, 20, num(defaultBoss.json?.data?.size))
check('DEFAULT-TOTAL-7', num(defaultBoss.json?.data?.total) === 7, 7, num(defaultBoss.json?.data?.total))
check('DEFAULT-7-ROWS', ids(defaultBoss.json?.data).length === 7, 7, ids(defaultBoss.json?.data).length)
check('DEFAULT-CONTRACT-EFFECTIVE', num(defaultBoss.json?.data?.contractTotal) === 6 && num(defaultBoss.json?.data?.effectiveTotal) === 1, { contract: 6, effective: 1 }, { contract: num(defaultBoss.json?.data?.contractTotal), effective: num(defaultBoss.json?.data?.effectiveTotal) })
check('DEFAULT-FACETS', JSON.stringify(Object.fromEntries((defaultBoss.json?.data?.itemTypes ?? []).map((x) => [x.key, num(x.count)]))) === JSON.stringify({Construction:3,General:1,Material:3}), {Construction:3,General:1,Material:3}, defaultBoss.json?.data?.itemTypes)

const pages = []
const pageSizes = []
const pageHasMore = []
for (let page = 1; page <= 4; page++) {
  const result = await post('project/contract-change/range', request({ current: page, size: 2 }), tokens.boss)
  responses[`page${page}`] = result
  pages.push(...ids(result.json?.data))
  pageSizes.push(ids(result.json?.data).length)
  pageHasMore.push(result.json?.data?.hasMore)
  check(`PAGE-${page}-HTTP`, result.status === 200 && result.json?.success === true, '200/success', { status: result.status, success: result.json?.success })
  check(`PAGE-${page}-META`, num(result.json?.data?.current) === page && num(result.json?.data?.size) === 2 && num(result.json?.data?.total) === 7, { current: page, size: 2, total: 7 }, { current: num(result.json?.data?.current), size: num(result.json?.data?.size), total: num(result.json?.data?.total) })
}
check('PAGE-SIZES-2-2-2-1', JSON.stringify(pageSizes) === JSON.stringify([2,2,2,1]), [2,2,2,1], pageSizes)
check('PAGE-HAS-MORE-TRUE-TRUE-TRUE-FALSE', JSON.stringify(pageHasMore) === JSON.stringify([true,true,true,false]), [true,true,true,false], pageHasMore)
check('PAGE-TRAVERSAL-ORDER', JSON.stringify(pages) === JSON.stringify(expectedOrder), expectedOrder, pages)
check('PAGE-TRAVERSAL-7-UNIQUE', pages.length === 7 && new Set(pages).size === 7, { count: 7, unique: 7 }, { count: pages.length, unique: new Set(pages).size, pages })

const material = await post('project/contract-change/range', request({ itemType: 'Material' }), tokens.boss)
responses.material = material
check('MATERIAL-TOTAL-3', num(material.json?.data?.total) === 3, 3, num(material.json?.data?.total))
check('MATERIAL-ROWS-ALL-MATERIAL', material.json?.data?.sources?.length === 3 && material.json.data.sources.every((x) => x.itemType === 'Material'), '3 Material', material.json?.data?.sources?.map((x) => x.itemType))
const keyword = await post('project/contract-change/range', request({ keyword: '全屋保洁' }), tokens.boss)
responses.keyword = keyword
check('KEYWORD-TOTAL-1', num(keyword.json?.data?.total) === 1, 1, num(keyword.json?.data?.total))
check('KEYWORD-MATCH', keyword.json?.data?.sources?.length === 1 && keyword.json.data.sources[0].name.includes('全屋保洁'), 'name contains 全屋保洁', keyword.json?.data?.sources?.map((x) => x.name))
const capped = await post('project/contract-change/range', request({ size: 500 }), tokens.boss)
responses.capped = capped
check('SIZE500-CAP-50', num(capped.json?.data?.size) === 50, 50, num(capped.json?.data?.size))
check('SIZE500-CONTENT-7', capped.json?.data?.sources?.length === 7, 7, capped.json?.data?.sources?.length)

const roleBossIds = ids(defaultBoss.json?.data)
for (const [role, token] of Object.entries(tokens)) {
  const result = role === 'boss' ? defaultBoss : await post('project/contract-change/range', request(), token)
  responses[`role_${role}`] = result
  const roleIds = ids(result.json?.data)
  check(`ROLE-${role.toUpperCase()}-TOTAL-7`, result.json?.success === true && num(result.json?.data?.total) === 7, 7, num(result.json?.data?.total))
  check(`ROLE-${role.toUpperCase()}-SAME-IDS`, JSON.stringify(roleIds) === JSON.stringify(roleBossIds), roleBossIds, roleIds)
}

const summary = {
  generatedAt: new Date().toISOString(),
  api: API,
  contractId,
  expectedOrder,
  pages: Object.fromEntries([1,2,3,4].map((p) => [p, { ids: ids(responses[`page${p}`].json?.data), size: pageSizes[p-1], hasMore: pageHasMore[p-1] }])),
  passed: checks.filter((x) => x.pass).length,
  failed: checks.filter((x) => !x.pass).length,
  total: checks.length,
  outcome: checks.every((x) => x.pass) ? 'PASS' : 'FAIL',
  checks,
  raw: responses
}
fs.writeFileSync(path.join(OUT, 'independent-reaccept-api.json'), JSON.stringify(summary, null, 2) + '\n')
console.log(JSON.stringify({ outcome: summary.outcome, passed: summary.passed, failed: summary.failed, total: summary.total, pageSizes, pageHasMore, pages, failedChecks: checks.filter((x) => !x.pass) }, null, 2))
if (summary.failed) process.exitCode = 1
