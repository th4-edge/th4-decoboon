import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const API = process.env.API_BASE || 'http://127.0.0.1:8090/api/'
const APP_ROOT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const OUT = fileURLToPath(new URL('.', import.meta.url))
const requireApp = createRequire(path.join(APP_ROOT, 'package.json'))
const automator = requireApp('miniprogram-automator/out/index.js')
const results = []
const raw = {}

function check(id, pass, expected, actual, detail = '') {
  results.push({ id, pass, expected, actual, detail })
  if (!pass) throw new Error(`${id}: expected=${JSON.stringify(expected)} actual=${JSON.stringify(actual)}`)
}

async function tokenFromApp() {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9430' })
  try {
    const value = await mp.evaluate(() => wx.getStorageSync('jzb_token'))
    if (!value) throw new Error('App session token missing')
    return value
  } finally {
    await mp.disconnect()
  }
}

async function post(route, body, token) {
  const started = performance.now()
  const response = await fetch(API + route, {
    method: 'POST',
    headers: { 'content-type': 'application/json', token },
    body: JSON.stringify(body)
  })
  const text = await response.text()
  let json
  try { json = JSON.parse(text) } catch { json = { parseError: true, text } }
  return { http: response.status, body: json, ms: performance.now() - started }
}

function pageOf(body) {
  const data = body?.data
  const rows = Array.isArray(data?.content) ? data.content : Array.isArray(data?.customers) ? data.customers : null
  const total = data?.totalElements != null ? Number(data.totalElements) : data?.total != null ? Number(data.total) : null
  return { rows, total, current: data?.current ?? (data?.number != null ? Number(data.number) + 1 : 1), size: data?.size ?? null }
}

const token = process.env.API_TOKEN || await tokenFromApp()
const suites = [
  {
    key: 'followup', route: 'customer/my-customer/overview', base: { current: 1, size: 20, sort: 'due' },
    filters: { all: 'all', overdue: 'overdue', today: 'today', soon: 'soon', unplanned: 'unplanned' },
    payload: (value) => ({ followFilter: value })
  },
  {
    key: 'measure', route: 'statistic/designer/measure/page', base: { current: 1, size: 20 },
    filters: { all: null, overdue: 'overdue', today: 'today', scheduled: 'scheduled', notScheduled: 'notScheduled', handoff: 'handoff' },
    payload: (value) => value == null ? {} : { state: value }
  },
  {
    key: 'design', route: 'statistic/workbench/design/page', base: { current: 1, size: 20 },
    filters: { all: 'all', pending: 'Pending', doing: 'Doing', done: 'Done' },
    payload: (value) => ({ filter: value })
  },
  {
    key: 'quote', route: 'statistic/workbench/quote/page', base: { current: 1, size: 20 },
    filters: { all: 'all', draft: 'draft', submitted: 'submitted', rejected: 'rejected' },
    payload: (value) => ({ filter: value })
  },
  {
    key: 'sign', route: 'statistic/workbench/sign/page', base: { current: 1, size: 20 },
    filters: { all: 'all', toCreate: 'toCreate', waitingConfirm: 'waitingConfirm', effective: 'effective' },
    payload: (value) => ({ filter: value })
  }
]

for (const suite of suites) {
  raw[suite.key] = {}
  for (const [label, value] of Object.entries(suite.filters)) {
    const response = await post(suite.route, { ...suite.base, ...suite.payload(value) }, token)
    raw[suite.key][label] = response.body
    const page = pageOf(response.body)
    check(`${suite.key}-${label}-success`, response.body?.success === true, true, response.body?.success)
    check(`${suite.key}-${label}-page`, Array.isArray(page.rows) && Number.isFinite(page.total), true, { rows: page.rows?.length, total: page.total })
    check(`${suite.key}-${label}-size`, page.rows.length <= 20, '<=20', page.rows.length)
    check(`${suite.key}-${label}-latency`, response.ms <= 500, '<=500ms', `${response.ms.toFixed(1)}ms`)
  }
}

const quoteSearch = await post('statistic/workbench/quote/page', { current: 1, size: 20, filter: 'all', keyword: '周先生' }, token)
raw.quoteSearch = quoteSearch.body
const quoteSearchPage = pageOf(quoteSearch.body)
check('quote-keyword-server-side', quoteSearch.body?.success === true && quoteSearchPage.total === 0, 0, quoteSearchPage.total)

const signSearch = await post('statistic/workbench/sign/page', { current: 1, size: 20, filter: 'all', keyword: '王亮' }, token)
raw.signSearch = signSearch.body
const signSearchPage = pageOf(signSearch.body)
check('sign-keyword-server-side', signSearch.body?.success === true && signSearchPage.total > 0 && signSearchPage.rows.every((row) => String(row.customerName || '').includes('王亮')), { total: '>0', allNamesContain: '王亮' }, { total: signSearchPage.total, names: signSearchPage.rows.map((row) => row.customerName) })

for (const suite of suites) {
  const first = pageOf((await post(suite.route, { ...suite.base, ...suite.payload(Object.values(suite.filters)[0]), size: 1, current: 1 }, token)).body)
  const second = pageOf((await post(suite.route, { ...suite.base, ...suite.payload(Object.values(suite.filters)[0]), size: 1, current: 2 }, token)).body)
  const firstKey = first.rows[0]?.quoteId ?? first.rows[0]?.propertyId ?? first.rows[0]?.customerId ?? first.rows[0]?.custId
  const secondKey = second.rows[0]?.quoteId ?? second.rows[0]?.propertyId ?? second.rows[0]?.customerId ?? second.rows[0]?.custId
  check(`${suite.key}-pagination-total`, first.total === second.total, first.total, second.total)
  if (first.total > 1) check(`${suite.key}-pagination-no-overlap`, String(firstKey) !== String(secondKey), 'different', { firstKey, secondKey })
}

const summary = { generatedAt: new Date().toISOString(), passed: results.filter((item) => item.pass).length, total: results.length, results, raw }
fs.writeFileSync(path.join(OUT, 'api-results.json'), JSON.stringify(summary, null, 2))
console.log(JSON.stringify({ passed: summary.passed, total: summary.total }, null, 2))
