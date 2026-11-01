import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { fileURLToPath } from 'node:url'

const API = process.env.API_BASE || 'http://127.0.0.1:8092/api'
const OUT = path.dirname(fileURLToPath(import.meta.url))
const RAW_DIR = path.join(OUT, 'api')
fs.mkdirSync(RAW_DIR, { recursive: true })

const projectId = '241940507872198656'
const contractId = '241939522005237760'
const accounts = {
  boss: { phone: '13910002001', password: process.env.TH4_BOSS_PASSWORD },
  storeManager: { phone: '13910002002', password: process.env.TH4_STORE_PASSWORD },
  designer: { phone: '13910002004', password: process.env.TH4_DESIGNER_PASSWORD }
}

const results = []
const tokenHashes = {}
let sequence = 0

function check(id, pass, expected, actual, detail = '') {
  results.push({ id, pass: Boolean(pass), expected, actual, detail })
}
function number(value) { return value === null || value === undefined ? null : Number(value) }
function idsOfSources(data) {
  return (data?.sources ?? []).map((row) => row.sourceLineId ?? row.sourceChangeItemId)
}
function idsOfLines(data) {
  return (data?.content ?? []).map((row) => row.lineId)
}
function hasKeyRecursive(value, key) {
  if (Array.isArray(value)) return value.some((item) => hasKeyRecursive(item, key))
  if (value && typeof value === 'object') {
    return Object.prototype.hasOwnProperty.call(value, key) || Object.values(value).some((item) => hasKeyRecursive(item, key))
  }
  return false
}
function sortedBySortThenId(rows) {
  return rows.every((row, index) => {
    if (index === 0) return true
    const prev = rows[index - 1]
    const ps = number(prev.sort)
    const cs = number(row.sort)
    if (ps !== cs) return ps < cs
    return BigInt(prev.lineId) < BigInt(row.lineId)
  })
}

async function rawPost(name, route, body, token = '') {
  const started = performance.now()
  const response = await fetch(`${API}/${route}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const text = await response.text()
  let json = null
  try { json = JSON.parse(text) } catch {}
  const elapsedMs = Number((performance.now() - started).toFixed(2))
  const record = {
    request: { route, body },
    response: { status: response.status, elapsedMs, bytes: Buffer.byteLength(text), json }
  }
  const filename = `${String(++sequence).padStart(3, '0')}-${name.replaceAll(/[^a-zA-Z0-9_-]/g, '-')}.json`
  fs.writeFileSync(path.join(RAW_DIR, filename), JSON.stringify(record, null, 2) + '\n')
  return { ...record.response, json, filename }
}
async function login(role) {
  const account = accounts[role]
  const response = await rawPost(`login-${role}`, 'auth/session/password-login', { phone: account.phone, password: account.password })
  const token = response.json?.data?.token
  check(`LOGIN-${role.toUpperCase()}`, response.status === 200 && response.json?.success === true && Boolean(token), 'success token', { status: response.status, success: response.json?.success, hasToken: Boolean(token) })
  if (token) tokenHashes[role] = crypto.createHash('sha256').update(token).digest('hex').slice(0, 16)
  return token
}

const tokens = {}
for (const role of Object.keys(accounts)) tokens[role] = await login(role)

const noToken = await rawPost('range-no-token', 'project/contract-change/range', { contractId, current: 1, size: 20 })
check('RANGE-NO-TOKEN-DENIED', noToken.json?.success === false && noToken.json?.errorCode?.code === 'tokenIsUndefined', 'tokenIsUndefined', noToken.json?.errorCode?.code)

const rangeBody = (extra = {}) => ({ contractId, current: 1, size: 20, ...extra })
const rangeRole = {}
for (const role of ['boss', 'storeManager', 'designer']) {
  rangeRole[role] = await rawPost(`range-default-${role}`, 'project/contract-change/range', rangeBody(), tokens[role])
  const data = rangeRole[role].json?.data
  check(`RANGE-ROLE-${role.toUpperCase()}-SUCCESS`, rangeRole[role].json?.success === true, true, rangeRole[role].json?.success)
  check(`RANGE-ROLE-${role.toUpperCase()}-TOTAL-7`, number(data?.total) === 7, 7, number(data?.total))
  check(`RANGE-ROLE-${role.toUpperCase()}-SOURCE-IDS`, idsOfSources(data).length === 7 && new Set(idsOfSources(data)).size === 7, 7, idsOfSources(data).length)
}

const range = rangeRole.boss.json.data
check('RANGE-DEFAULT-CURRENT', number(range.current) === 1, 1, number(range.current))
check('RANGE-DEFAULT-SIZE', number(range.size) === 20, 20, number(range.size))
check('RANGE-DEFAULT-TOTAL', number(range.total) === 7, 7, number(range.total))
check('RANGE-DEFAULT-HAS-MORE', range.hasMore === false, false, range.hasMore)
check('RANGE-DEFAULT-CONTRACT-TOTAL', number(range.contractTotal) === 6, 6, number(range.contractTotal))
check('RANGE-DEFAULT-EFFECTIVE-TOTAL', number(range.effectiveTotal) === 1, 1, number(range.effectiveTotal))
check('RANGE-DEFAULT-GROUPS', range.sources.filter((row) => row.group === 'contract').length === 6 && range.sources.filter((row) => row.group === 'effective').length === 1, '6 contract / 1 effective', range.sources.reduce((acc, row) => (acc[row.group] = (acc[row.group] || 0) + 1, acc), {}))
check('RANGE-DEFAULT-NO-DUP', new Set(idsOfSources(range)).size === idsOfSources(range).length, 7, new Set(idsOfSources(range)).size)
const facetMap = Object.fromEntries((range.itemTypes ?? []).map((facet) => [facet.key, number(facet.count)]))
check('RANGE-FACET-ITEM-TYPES', JSON.stringify(facetMap) === JSON.stringify({ Construction: 3, General: 1, Material: 3 }), { Construction: 3, General: 1, Material: 3 }, facetMap)
check('RANGE-FACET-TOTAL', Object.values(facetMap).reduce((sum, count) => sum + count, 0) === 7, 7, Object.values(facetMap).reduce((sum, count) => sum + count, 0))
check('RANGE-FACET-SPACES', number(range.spaces?.[0]?.count) === 7 && range.spaces?.[0]?.key === '__none__', { key: '__none__', count: 7 }, range.spaces)

const pageMap = {}
for (const page of [1, 2, 3, 4]) {
  pageMap[page] = await rawPost(`range-size2-page${page}`, 'project/contract-change/range', rangeBody({ current: page, size: 2 }), tokens.boss)
}
const pageRows = Object.fromEntries([1, 2, 3, 4].map((page) => [page, pageMap[page].json?.data?.sources ?? []]))
check('RANGE-PAGE1-SIZE2', pageRows[1].length === 2, 2, pageRows[1].length)
check('RANGE-PAGE2-SIZE2', pageRows[2].length === 2, 2, pageRows[2].length)
check('RANGE-PAGE4-SIZE2', pageRows[4].length === 1, 1, pageRows[4].length)
check('RANGE-PAGE1-HAS-MORE', pageMap[1].json?.data?.hasMore === true, true, pageMap[1].json?.data?.hasMore)
check('RANGE-PAGE2-HAS-MORE', pageMap[2].json?.data?.hasMore === true, true, pageMap[2].json?.data?.hasMore)
check('RANGE-PAGE4-HAS-MORE', pageMap[4].json?.data?.hasMore === false, false, pageMap[4].json?.data?.hasMore)
const pageIds = [1, 2, 3, 4].flatMap((page) => idsOfSources(pageMap[page].json?.data))
check('RANGE-ALL-PAGES-7-UNIQUE', pageIds.length === 7 && new Set(pageIds).size === 7, { length: 7, unique: 7 }, { length: pageIds.length, unique: new Set(pageIds).size, ids: pageIds })

const cappedRange = await rawPost('range-size500', 'project/contract-change/range', rangeBody({ size: 500 }), tokens.boss)
check('RANGE-SIZE500-CAPPED-50', number(cappedRange.json?.data?.size) === 50, 50, number(cappedRange.json?.data?.size))
check('RANGE-SIZE500-CONTENT', cappedRange.json?.data?.sources?.length === 7, 7, cappedRange.json?.data?.sources?.length)

const keyword = await rawPost('range-keyword', 'project/contract-change/range', rangeBody({ keyword: '全屋保洁' }), tokens.boss)
check('RANGE-KEYWORD-TOTAL-1', number(keyword.json?.data?.total) === 1, 1, number(keyword.json?.data?.total))
check('RANGE-KEYWORD-MATCH', keyword.json?.data?.sources?.length === 1 && keyword.json.data.sources[0].name.includes('全屋保洁'), 'name contains 全屋保洁', keyword.json?.data?.sources?.map((row) => row.name))

const material = await rawPost('range-itemType-material', 'project/contract-change/range', rangeBody({ itemType: 'Material' }), tokens.boss)
check('RANGE-MATERIAL-TOTAL-3', number(material.json?.data?.total) === 3, 3, number(material.json?.data?.total))
check('RANGE-MATERIAL-ROWS', material.json?.data?.sources?.length === 3 && material.json.data.sources.every((row) => row.itemType === 'Material'), '3 Material', material.json?.data?.sources?.map((row) => row.itemType))

const space = await rawPost('range-space-none', 'project/contract-change/range', rangeBody({ spaceName: '__none__' }), tokens.boss)
check('RANGE-SPACE-NONE-TOTAL-7', number(space.json?.data?.total) === 7, 7, number(space.json?.data?.total))
const missingSpace = await rawPost('range-space-missing', 'project/contract-change/range', rangeBody({ spaceName: '不存在的区域' }), tokens.boss)
check('RANGE-SPACE-FILTER-ZERO', number(missingSpace.json?.data?.total) === 0 && missingSpace.json?.data?.sources?.length === 0, { total: 0, rows: 0 }, { total: number(missingSpace.json?.data?.total), rows: missingSpace.json?.data?.sources?.length })

const effective = await rawPost('range-group-effective', 'project/contract-change/range', rangeBody({ group: 'effective' }), tokens.boss)
check('RANGE-GROUP-EFFECTIVE', number(effective.json?.data?.total) === 1 && effective.json?.data?.sources?.length === 1 && effective.json.data.sources[0].group === 'effective', '1 effective', { total: number(effective.json?.data?.total), groups: effective.json?.data?.sources?.map((row) => row.group) })
const contractOnly = await rawPost('range-group-contract', 'project/contract-change/range', rangeBody({ group: 'contract' }), tokens.boss)
check('RANGE-GROUP-CONTRACT', number(contractOnly.json?.data?.total) === 6 && contractOnly.json?.data?.sources?.length === 6 && contractOnly.json.data.sources.every((row) => row.group === 'contract'), '6 contract', { total: number(contractOnly.json?.data?.total), groups: contractOnly.json?.data?.sources?.map((row) => row.group) })
const combined = await rawPost('range-material-effective', 'project/contract-change/range', rangeBody({ itemType: 'Material', group: 'effective' }), tokens.boss)
check('RANGE-COMBINED-FILTER', number(combined.json?.data?.total) === 1 && combined.json?.data?.sources?.[0]?.group === 'effective' && combined.json?.data?.sources?.[0]?.itemType === 'Material', '1 Material effective', { total: number(combined.json?.data?.total), source: combined.json?.data?.sources?.[0] })

const selectedLineId = range.sources.find((row) => row.group === 'contract')?.sourceLineId
const selected = await rawPost('range-selected-line', 'project/contract-change/range', rangeBody({ keyword: '绝对无匹配', sourceLineId: selectedLineId }), tokens.boss)
check('RANGE-SELECTED-INDEPENDENT', selected.json?.data?.selected?.sourceLineId === selectedLineId && selected.json?.data?.sources?.length === 0, selectedLineId, selected.json?.data?.selected?.sourceLineId)

const badType = await rawPost('range-invalid-itemType', 'project/contract-change/range', rangeBody({ itemType: 'NotAnItemType' }), tokens.boss)
check('RANGE-INVALID-TYPE-400', badType.status === 400 || badType.json?.errorCode?.code === '400', 'HTTP 400 or business 400', { status: badType.status, errorCode: badType.json?.errorCode, hasSources: Array.isArray(badType.json?.data?.sources) })
const badGroup = await rawPost('range-invalid-group', 'project/contract-change/range', rangeBody({ group: 'not-a-group' }), tokens.boss)
check('RANGE-INVALID-GROUP-400', badGroup.status === 400 || badGroup.json?.errorCode?.code === '400', 'HTTP 400 or business 400', { status: badGroup.status, errorCode: badGroup.json?.errorCode, hasSources: Array.isArray(badGroup.json?.data?.sources) })

const unknownContract = await rawPost('range-unknown-contract', 'project/contract-change/range', { contractId: '999999999999999999', current: 1, size: 20 }, tokens.boss)
check('RANGE-UNKNOWN-CONTRACT-404', unknownContract.status === 404 || unknownContract.json?.errorCode?.code === '404', 'HTTP 404 or business 404', { status: unknownContract.status, errorCode: unknownContract.json?.errorCode })

const lineRole = {}
for (const role of ['boss', 'storeManager', 'designer']) {
  lineRole[role] = await rawPost(`contract-line-default-${role}`, 'project/contract-line/page', { contractId, current: 1, size: 20 }, tokens[role])
  const data = lineRole[role].json?.data
  check(`CONTRACT-LINE-${role.toUpperCase()}-SUCCESS`, lineRole[role].json?.success === true, true, lineRole[role].json?.success)
  check(`CONTRACT-LINE-${role.toUpperCase()}-TOTAL-6`, number(data?.totalElements) === 6, 6, number(data?.totalElements))
  check(`CONTRACT-LINE-${role.toUpperCase()}-SIZE-20`, number(data?.size) === 20, 20, number(data?.size))
}
const bossLines = lineRole.boss.json.data
check('CONTRACT-LINE-DEFAULT-ROWS', bossLines.content?.length === 6, 6, bossLines.content?.length)
check('CONTRACT-LINE-STABLE-SORT', sortedBySortThenId(bossLines.content ?? []), 'sort asc then lineId asc', bossLines.content?.map((row) => ({ sort: row.sort, lineId: row.lineId })))
check('CONTRACT-LINE-BOSS-COST-PRESENT', hasKeyRecursive(bossLines.content, 'cost'), true, hasKeyRecursive(bossLines.content, 'cost'))
check('CONTRACT-LINE-STORE-COST-PRESENT', hasKeyRecursive(lineRole.storeManager.json.data.content, 'cost'), true, hasKeyRecursive(lineRole.storeManager.json.data.content, 'cost'))
check('CONTRACT-LINE-DESIGNER-COST-HIDDEN', hasKeyRecursive(lineRole.designer.json.data.content, 'cost') === false, false, hasKeyRecursive(lineRole.designer.json.data.content, 'cost'))

const linePageMap = {}
for (const page of [1, 2, 3, 4]) {
  linePageMap[page] = await rawPost(`contract-line-size2-page${page}`, 'project/contract-line/page', { contractId, current: page, size: 2 }, tokens.boss)
}
check('CONTRACT-LINE-PAGE1-SIZE2', linePageMap[1].json?.data?.content?.length === 2, 2, linePageMap[1].json?.data?.content?.length)
check('CONTRACT-LINE-PAGE2-SIZE2', linePageMap[2].json?.data?.content?.length === 2, 2, linePageMap[2].json?.data?.content?.length)
check('CONTRACT-LINE-PAGE4-EMPTY', linePageMap[4].json?.data?.content?.length === 0, 0, linePageMap[4].json?.data?.content?.length)
const lineIds = [1, 2, 3, 4].flatMap((page) => idsOfLines(linePageMap[page].json?.data))
check('CONTRACT-LINE-PAGES-6-UNIQUE', lineIds.length === 6 && new Set(lineIds).size === 6, { length: 6, unique: 6 }, { length: lineIds.length, unique: new Set(lineIds).size, ids: lineIds })
check('CONTRACT-LINE-PAGES-STABLE', [1, 2, 3].every((page) => sortedBySortThenId(linePageMap[page].json?.data?.content ?? [])), 'each page sorted', [1, 2, 3].map((page) => linePageMap[page].json?.data?.content?.map((row) => ({ sort: row.sort, lineId: row.lineId }))))

const cappedLine = await rawPost('contract-line-size500', 'project/contract-line/page', { contractId, current: 1, size: 500 }, tokens.boss)
check('CONTRACT-LINE-SIZE500-CAPPED-50', number(cappedLine.json?.data?.size) === 50, 50, number(cappedLine.json?.data?.size))
check('CONTRACT-LINE-SIZE500-CONTENT', cappedLine.json?.data?.content?.length === 6, 6, cappedLine.json?.data?.content?.length)
const zeroLine = await rawPost('contract-line-current-size-zero', 'project/contract-line/page', { contractId, current: 0, size: 0 }, tokens.boss)
check('CONTRACT-LINE-INVALID-PAGING-DEFAULTS', number(zeroLine.json?.data?.size) === 20 && idsOfLines(zeroLine.json?.data).join(',') === idsOfLines(bossLines).join(','), { size: 20, sameFirstPage: true }, { size: number(zeroLine.json?.data?.size), sameFirstPage: idsOfLines(zeroLine.json?.data).join(',') === idsOfLines(bossLines).join(',') })

const workbenches = {}
for (const role of ['boss', 'storeManager', 'designer']) {
  workbenches[role] = await rawPost(`contract-workbench-${role}`, 'project/contract/workbench', { contractId }, tokens[role])
  const data = workbenches[role].json?.data
  check(`WORKBENCH-${role.toUpperCase()}-SUCCESS`, workbenches[role].json?.success === true, true, workbenches[role].json?.success)
  check(`WORKBENCH-${role.toUpperCase()}-LINE-COUNT`, number(data?.lineCount) === 6, 6, number(data?.lineCount))
  check(`WORKBENCH-${role.toUpperCase()}-LINE-AMOUNT`, number(data?.lineAmount) === 977.8, 977.8, number(data?.lineAmount))
  check(`WORKBENCH-${role.toUpperCase()}-CHANGE-COUNT`, number(data?.change?.totalCount) === 1, 1, number(data?.change?.totalCount))
  check(`WORKBENCH-${role.toUpperCase()}-CHANGE-AMOUNT`, number(data?.change?.confirmedAmount) === -168, -168, number(data?.change?.confirmedAmount))
  check(`WORKBENCH-${role.toUpperCase()}-CURRENT-AMOUNT`, number(data?.contract?.currentAmount) === 858.69, 858.69, number(data?.contract?.currentAmount))
}
for (const role of ['boss', 'storeManager']) {
  const cost = workbenches[role].json?.data?.cost
  check(`WORKBENCH-${role.toUpperCase()}-COST-SUMMARY`, cost?.costComplete === false && number(cost?.estimatedCost) === 746.5 && number(cost?.actualCost) === 88 && number(cost?.incompleteItemCount) === 1, { costComplete: false, estimatedCost: 746.5, actualCost: 88, incompleteItemCount: 1 }, cost)
}
check('WORKBENCH-DESIGNER-COST-HIDDEN', !Object.prototype.hasOwnProperty.call(workbenches.designer.json?.data ?? {}, 'cost'), 'cost absent', Object.keys(workbenches.designer.json?.data ?? {}))

const sameRangeIds = ['boss', 'storeManager', 'designer'].every((role) => idsOfSources(rangeRole[role].json?.data).join(',') === idsOfSources(rangeRole.boss.json?.data).join(','))
check('RANGE-ROLE-SAME-SCOPE', sameRangeIds, true, sameRangeIds)
const sameLineIds = ['boss', 'storeManager', 'designer'].every((role) => idsOfLines(lineRole[role].json?.data).join(',') === idsOfLines(lineRole.boss.json?.data).join(','))
check('CONTRACT-LINE-ROLE-SAME-SCOPE', sameLineIds, true, sameLineIds)

const summary = {
  generatedAt: new Date().toISOString(),
  api: API,
  contractId,
  projectId,
  tokenHashes,
  passed: results.filter((item) => item.pass).length,
  failed: results.filter((item) => !item.pass).length,
  total: results.length,
  outcome: results.every((item) => item.pass) ? 'PASS' : 'FAIL',
  results
}
fs.writeFileSync(path.join(OUT, 'api-acceptance-results.json'), JSON.stringify(summary, null, 2) + '\n')
console.log(JSON.stringify({ outcome: summary.outcome, passed: summary.passed, failed: summary.failed, total: summary.total, failedChecks: results.filter((item) => !item.pass) }, null, 2))
if (summary.failed) process.exitCode = 1
