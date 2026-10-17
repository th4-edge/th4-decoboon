import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('/Users/zhangwei/.codex/skills/guizang-social-card-skill/node_modules/playwright')

const API = process.env.B66_API || 'http://127.0.0.1:8091/api'
const APP = process.env.B66_APP || 'http://127.0.0.1:8080'
const WEB = process.env.B66_WEB || 'http://127.0.0.1:4175'
const OUT = process.env.B66_OUT || path.resolve(process.cwd(), 'docs/product/domains/material/evidence/B6.6')
const SHOTS = path.join(OUT, 'screenshots')
const PASSWORD = process.env.B66_PASSWORD
const DB_PASSWORD = process.env.B66_DB_PASSWORD
const DB_USER = process.env.B66_DB_USER || 'root'
const DB_NAME = process.env.B66_DB_NAME || 'decoboon'
const PLAN_ID = '235566303270666240'
const PROJECT_ID = '235520576473530368'
const TODAY = '2026-10-17'
const PERIOD = '2026-10'
const ROLE_ACCOUNTS = [
  ['Boss', process.env.B66_PHONE_BOSS],
  ['StoreManager', process.env.B66_PHONE_STORE],
  ['Designer', process.env.B66_PHONE_DESIGNER],
  ['Pm', process.env.B66_PHONE_PM],
  ['Finance', process.env.B66_PHONE_FINANCE],
  ['CrossBoss', process.env.B66_PHONE_CROSS]
]

if (!PASSWORD) throw new Error('B66_PASSWORD is required')
if (!DB_PASSWORD) throw new Error('B66_DB_PASSWORD is required')
for (const [role, phone] of ROLE_ACCOUNTS) if (!phone) throw new Error(`Missing phone env for ${role}`)
fs.mkdirSync(SHOTS, { recursive: true })

function sql(query) {
  return execFileSync('mysql', [
    '-h127.0.0.1', '-P3306', `-u${DB_USER}`, `-p${DB_PASSWORD}`, '-N', '-B', DB_NAME, '-e', query
  ], { encoding: 'utf8' }).trim()
}
function sqlOne(query) {
  return sql(query).split('\n')[0]?.trim() || ''
}
function normalized(value) {
  if (value == null) return null
  if (typeof value === 'string') return value
  return value.key ?? value.value ?? value.name ?? String(value)
}
function errorCode(response) {
  return response?.errorCode?.code ?? response?.errorCode ?? ''
}
function errorMessage(response) {
  return response?.errorCode?.message ?? response?.message ?? ''
}
function dataList(response) {
  const data = response?.data
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  return []
}
function idOf(value) { return value == null ? null : String(value) }
function quoteIds(ids) { return [...ids].map(String).filter(Boolean).join(',') }
function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)) }

async function post(route, body = {}, token) {
  const response = await fetch(`${API}/${route}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const json = await response.json().catch(() => ({}))
  return { httpStatus: response.status, ...json }
}

const tokens = {}
const sessions = {}
const results = []
const apiSamples = {}
const uiMetrics = {}
const owned = {
  partnerIds: new Set(), sheetIds: new Set(), purchaseIds: new Set(), settlementIds: new Set(), receiptIds: new Set()
}
const baseline = {}
let planOriginal = null
let projectOriginalManager = null
let tempPartnerId = null
let main = null

function check(id, category, title, condition, expected, actual, evidence = {}) {
  results.push({
    id, category, title, expected, actual,
    result: condition ? 'PASS' : 'FAIL',
    evidence
  })
  return condition
}
function requireSetup(condition, message) {
  if (!condition) throw new Error(`SETUP FAILED: ${message}`)
}

async function cleanupBusiness() {
  const sid = quoteIds(owned.settlementIds)
  if (sid) sql(`DELETE FROM decoboon.db_material_settlement_item WHERE settlement_id IN (${sid})`)
  if (sid) sql(`DELETE FROM decoboon.db_material_settlement WHERE settlement_id IN (${sid})`)
  const pid = quoteIds(owned.purchaseIds)
  if (pid) sql(`DELETE FROM decoboon.db_goods_receipt WHERE purchase_id IN (${pid})`)
  if (pid) sql(`DELETE FROM decoboon.db_finance_payable WHERE purchase_order_id IN (${pid})`)
  if (pid) sql(`DELETE FROM decoboon.db_purchase_order_item WHERE purchase_order_id IN (${pid})`)
  if (pid) sql(`DELETE FROM decoboon.db_purchase_order WHERE purchase_id IN (${pid})`)
  const shid = quoteIds(owned.sheetIds)
  if (shid) sql(`DELETE FROM decoboon.db_material_order_sheet_item WHERE sheet_id IN (${shid})`)
  if (shid) sql(`DELETE FROM decoboon.db_material_order_sheet WHERE sheet_id IN (${shid})`)
  owned.settlementIds.clear(); owned.purchaseIds.clear(); owned.receiptIds.clear(); owned.sheetIds.clear()
}

function restorePlanOriginal() {
  if (!planOriginal) return
  sql(`UPDATE decoboon.db_material_plan SET status='${planOriginal.status}', quantity=${planOriginal.quantity}, net_quantity=${planOriginal.net_quantity}, loss_rate=${planOriginal.loss_rate}, loss_quantity=${planOriginal.loss_quantity}, unit_price=${planOriginal.unit_price}, cost=${planOriginal.cost}, selection_id=${planOriginal.selection_id === 'NULL' ? 'NULL' : planOriginal.selection_id}, arrived_at=${planOriginal.arrived_at === 'NULL' ? 'NULL' : `'${planOriginal.arrived_at}'`} WHERE plan_id=${PLAN_ID}`)
}
function setPlanReady(quantity) {
  sql(`UPDATE decoboon.db_material_plan SET status='ReadyToOrder', quantity=${quantity}, net_quantity=${quantity}, loss_rate=${planOriginal.loss_rate}, loss_quantity=${planOriginal.loss_quantity}, unit_price=${planOriginal.unit_price}, cost=${planOriginal.cost}, selection_id=NULL, arrived_at=NULL WHERE plan_id=${PLAN_ID}`)
}

const baselineTables = [
  'db_material_order_sheet', 'db_material_order_sheet_item', 'db_purchase_order', 'db_purchase_order_item',
  'db_goods_receipt', 'db_material_settlement', 'db_material_settlement_item', 'db_finance_payable',
  'db_finance_payable_settlement', 'db_partner'
]

async function setupScenario(quantity, label) {
  setPlanReady(quantity)
  if (sessions.Pm?.empId) sql(`UPDATE decoboon.db_project SET manager_id=${sessions.Pm.empId} WHERE project_id=${PROJECT_ID}`)
  const beforePurchaseMax = sqlOne('SELECT COALESCE(MAX(purchase_id),0) FROM decoboon.db_purchase_order')
  const create = await post('material/sheet/save', {
    projectId: PROJECT_ID,
    sheetType: 'Tile',
    manufacturerName: 'B6.6独立复验厂家',
    items: [{
      materialPlan: { planId: PLAN_ID },
      materialName: '主材--抛釉砖 800x800',
      locationName: '客厅地砖',
      quantity,
      unit: 'Area'
    }]
  }, tokens.Designer)
  requireSetup(create.success, `material/sheet/save ${JSON.stringify(errorCode(create))} ${errorMessage(create)}`)
  const sheetId = idOf(create.data.sheetId)
  requireSetup(sheetId, 'sheet id missing')
  owned.sheetIds.add(sheetId)
  const createdInfo = await post('material/sheet/info', { sheetId }, tokens.Designer)
  requireSetup(createdInfo.success, `created sheet info ${JSON.stringify(errorCode(createdInfo))} ${errorMessage(createdInfo)}`)
  const itemId = idOf(createdInfo.data?.items?.[0]?.itemId)
  requireSetup(itemId, 'sheet/item id missing')

  const prematureSubmit = await post('material/sheet/submit', { sheetId: String(sheetId) }, tokens.Designer)
  check(`B66-NEG-01-${label}`, 'main-chain', '未测量提交请购被拦截', !prematureSubmit.success,
    '未填写必填测量时间时不得提交', { success: prematureSubmit.success, message: errorMessage(prematureSubmit) })

  const measure = await post('material/sheet/measure', {
    sheetId: String(sheetId), measureAt: TODAY, deliverAt: '2026-10-20', installAt: '2026-10-22'
  }, tokens.Pm)
  requireSetup(measure.success, `measure ${JSON.stringify(errorCode(measure))} ${errorMessage(measure)}`)
  const submit = await post('material/sheet/submit', { sheetId: String(sheetId) }, tokens.Designer)
  requireSetup(submit.success, `submit ${JSON.stringify(errorCode(submit))} ${errorMessage(submit)}`)
  return { sheetId, itemId, sheetNo: idOf(create.data.sheetNo), beforePurchaseMax }
}

async function order(scenario, body, key) {
  const payload = { sheetId: String(scenario.sheetId), deliveryMode: 'Direct', expectedAt: '2026-10-20', idempotencyKey: key, ...body }
  const response = await post('material/sheet/order', payload, tokens.Finance)
  return { payload, response }
}

async function syncPurchases(scenario) {
  const rows = sql(`SELECT purchase_id FROM decoboon.db_purchase_order WHERE material_plan_id=${PLAN_ID} AND purchase_id>${scenario.beforePurchaseMax} ORDER BY purchase_id`).split('\n').filter(Boolean)
  rows.forEach(id => owned.purchaseIds.add(id))
  return rows
}

async function sheetInfo(token = tokens.Finance, sheetId = main?.sheetId) {
  return post('material/sheet/info', { sheetId: String(sheetId) }, token)
}
async function sheetPage(token = tokens.Finance, sheetId = main?.sheetId) {
  const response = await post('material/sheet/page', {}, token)
  const sheet = dataList(response).find(row => idOf(row.sheetId) === String(sheetId))
  return { response, sheet }
}
function sheetProjection(response) {
  const sheet = response?.data
  const item = sheet?.items?.[0]
  return {
    sheetStatus: normalized(sheet?.status),
    sheetPriceState: normalized(sheet?.priceState),
    itemPriceState: normalized(item?.priceState),
    orderedQuantity: item?.orderedQuantity,
    purchasePrice: item?.purchasePrice,
    settlementPrice: item?.settlementPrice
  }
}
/**
 * 独立 UI 探针：在真实 Web 1440x900 页面读取目标作业单行、价格状态 pill、
 * DOM 尺寸与 Console，并对照服务端 priceState 断言文案与视觉状态。
 * 只读取页面/接口，不修改业务状态。
 */
async function captureWebOrderingState(tag, scenario, expectedState, expectedLabel) {
  const label = `B66-UI-WEB-${tag.toUpperCase()}`
  let browser
  try {
    browser = await chromium.launch({ headless: true })
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
    const page = await context.newPage()
    const consoleErrors = []
    let xhrPriceState = null
    let xhrSheetStatus = null
    page.on('console', msg => { if (msg.type() === 'error') consoleErrors.push(msg.text()) })
    page.on('response', async resp => {
      if (!resp.url().includes('/api/material/sheet/page')) return
      try {
        const json = await resp.json()
        const list = Array.isArray(json?.data) ? json.data : (Array.isArray(json?.data?.content) ? json.data.content : [])
        const found = list.find(r => idOf(r.sheetId) === String(scenario.sheetId))
        if (found) {
          xhrPriceState = normalized(found.priceState)
          xhrSheetStatus = normalized(found.status)
        }
      } catch {}
    })
    await page.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
    await page.evaluate(([key, value]) => localStorage.setItem(key, value), ['decoboon-web.token', tokens.Finance])
    await page.goto(`${WEB}/finance/material-ordering`, { waitUntil: 'networkidle' })
    await page.reload({ waitUntil: 'networkidle' })
    await page.waitForTimeout(1200)
    const body = await page.locator('body').innerText()
    const row = page.locator('li', { hasText: scenario.sheetNo }).first()
    const rowCount = await row.count()
    const rowText = rowCount ? await row.innerText() : ''
    const pill = rowCount ? row.locator('.mo__price-state').first() : null
    const pillCount = pill ? await pill.count() : 0
    const pillText = pillCount ? (await pill.innerText()).trim() : ''
    const pillClass = pillCount ? (await pill.getAttribute('class')) || '' : ''
    const pillStyle = pillCount ? await pill.evaluate(el => {
      const s = getComputedStyle(el)
      return { color: s.color, backgroundColor: s.backgroundColor, borderRadius: s.borderRadius, fontSize: s.fontSize, display: s.display }
    }) : null
    const metrics = await page.evaluate(() => ({
      innerWidth: window.innerWidth,
      innerHeight: window.innerHeight,
      scrollWidth: document.documentElement.scrollWidth,
      bodyScrollWidth: document.body.scrollWidth,
      scrollHeight: document.documentElement.scrollHeight,
      domNodeCount: document.querySelectorAll('*').length,
      htmlLength: document.documentElement.outerHTML.length
    }))
    await page.screenshot({ path: path.join(SHOTS, `b66-web-ordering-${tag}-1440x900.png`) })
    const api = await sheetPage(tokens.Finance, scenario.sheetId)
    const apiState = normalized(api.sheet?.priceState)
    const expectedLower = expectedState.toLowerCase()
    check(`${label}-TEXT`, 'ui', `Web ${tag} 行显示「${expectedLabel}」`, rowCount > 0 && rowText.includes(expectedLabel),
      `目标行含「${expectedLabel}」`, { sheetNo: scenario.sheetNo, rowCount, rowText, bodyExcerpt: body.slice(-700) })
    check(`${label}-PILL`, 'ui', `Web ${tag} 价格状态 pill 文案与视觉状态为 ${expectedState}`,
      pillCount > 0 && pillText === expectedLabel && pillClass.split(/\s+/).includes(`mo__price-state--${expectedLower}`),
      `pill 文案「${expectedLabel}」且 class=mo__price-state--${expectedLower}`, { pillText, pillClass, pillStyle })
    check(`${label}-SERVER`, 'ui', `Web ${tag} 价格状态来自服务端 priceState=${expectedState}`,
      api.response.success && apiState === expectedState && xhrPriceState === expectedState,
      `API/XHR priceState=${expectedState}`, { apiState, xhrPriceState, xhrSheetStatus, apiSuccess: api.response.success })
    check(`${label}-LAYOUT`, 'ui', `Web ${tag} 1440×900 无横向溢出且 Console Errors=0`,
      metrics.scrollWidth <= metrics.innerWidth && metrics.bodyScrollWidth <= metrics.innerWidth && consoleErrors.length === 0,
      'scrollWidth<=innerWidth; consoleErrors=0', { ...metrics, consoleErrors })
    uiMetrics[`webOrdering_${tag}`] = {
      sheetNo: scenario.sheetNo, sheetId: scenario.sheetId, ...metrics, consoleErrors,
      rowCount, rowText, pillText, pillClass, pillStyle, apiState, xhrPriceState, xhrSheetStatus,
      screenshot: `screenshots/b66-web-ordering-${tag}-1440x900.png`
    }
  } catch (error) {
    results.push({ id: `${label}-BLOCKED`, category: 'ui', title: `Web ${tag} 状态真实页面读取失败`, expected: '真实页面与接口可用', actual: error instanceof Error ? error.message : String(error), result: 'BLOCKED', evidence: {} })
  } finally {
    try { await browser?.close() } catch {}
  }
}

async function purchaseFacts(purchaseIds) {
  const ids = quoteIds(purchaseIds)
  if (!ids) return []
  return sql(`SELECT purchase_id,supplier_id,no,quantity,unit_price,amount,price_state,status FROM decoboon.db_purchase_order WHERE purchase_id IN (${ids}) ORDER BY purchase_id`).split('\n').filter(Boolean).map(line => {
    const [purchaseId,supplierId,no,quantity,unitPrice,amount,priceState,status] = line.split('\t')
    return { purchaseId, supplierId, no, quantity, unitPrice, amount, priceState, status }
  })
}
async function purchaseItemFacts(purchaseIds) {
  const ids = quoteIds(purchaseIds)
  if (!ids) return []
  return sql(`SELECT purchase_item_id,purchase_order_id,quantity,unit_price,received_quantity,price_state,settlement_price FROM decoboon.db_purchase_order_item WHERE purchase_order_id IN (${ids}) ORDER BY purchase_item_id`).split('\n').filter(Boolean).map(line => {
    const [purchaseItemId,purchaseOrderId,quantity,unitPrice,receivedQuantity,priceState,settlementPrice] = line.split('\t')
    return { purchaseItemId, purchaseOrderId, quantity, unitPrice, receivedQuantity, priceState, settlementPrice: settlementPrice === 'NULL' ? null : settlementPrice }
  })
}
async function payableFacts(purchaseIds) {
  const ids = quoteIds(purchaseIds)
  if (!ids) return []
  return sql(`SELECT payable_id,purchase_order_id,payee_partner_id,amount,estimated+0,settled_amount,status FROM decoboon.db_finance_payable WHERE purchase_order_id IN (${ids}) ORDER BY payable_id`).split('\n').filter(Boolean).map(line => {
    const [payableId,purchaseOrderId,payeePartnerId,amount,estimated,settledAmount,status] = line.split('\t')
    return { payableId, purchaseOrderId, payeePartnerId, amount, estimated, settledAmount: settledAmount === 'NULL' ? null : settledAmount, status }
  })
}
async function financePayables(partnerId) {
  return post('finance/payable/page', { current: 1, size: 200, partnerId: String(partnerId) }, tokens.Finance)
}
async function settlePreview(supplierId, scope, period, purchaseOrderId) {
  return post('material/settlement/preview', { supplierId: String(supplierId), scope, period, purchaseOrderId: purchaseOrderId || undefined }, tokens.Finance)
}
async function settleConfirm(supplierId, scope, period, items, remark = 'B6.6独立复验结算') {
  return post('material/settlement/confirm', { supplierId: String(supplierId), scope, period, remark, items }, tokens.Finance)
}
async function receipt(scenario, purchaseId, purchaseItemId, quantity) {
  const response = await post('project/goods-receipt/save', {
    purchaseId: String(purchaseId), purchaseItemId: String(purchaseItemId), quantity, arrivedAt: `${TODAY}T10:00:00`, remark: 'B6.6独立复验到场'
  }, tokens.Pm)
  if (response.success && response.data?.receiptId) owned.receiptIds.add(idOf(response.data.receiptId))
  return response
}

async function captureEvidence() {
  try {
    baseline.partners = Number(sqlOne('SELECT COUNT(*) FROM decoboon.db_partner'))
    baseline.tables = Object.fromEntries(baselineTables.map(table => [table, Number(sqlOne(`SELECT COUNT(*) FROM decoboon.${table}`))]))
    projectOriginalManager = sqlOne(`SELECT COALESCE(manager_id,'NULL') FROM decoboon.db_project WHERE project_id=${PROJECT_ID}`)
    const planRow = sqlOne(`SELECT status,quantity,net_quantity,loss_rate,loss_quantity,unit_price,cost,COALESCE(selection_id,'NULL'),COALESCE(DATE_FORMAT(arrived_at,'%Y-%m-%d'),'NULL') FROM decoboon.db_material_plan WHERE plan_id=${PLAN_ID}`)
    const [status,quantity,net_quantity,loss_rate,loss_quantity,unit_price,cost,selection_id,arrived_at] = planRow.split('\t')
    planOriginal = { status, quantity, net_quantity, loss_rate, loss_quantity, unit_price, cost, selection_id, arrived_at }
    apiSamples.environment = { backend: 'b66a33b:8091', app: '46a5be4:8080', web: '3ef4974:4175', database: '127.0.0.1:3306/decoboon' }
    apiSamples.preflight = {
      plan: { planId: PLAN_ID, projectId: PROJECT_ID, status, quantity, unit_price, cost },
      baselineCounts: baseline.tables,
      partnerCount: baseline.partners,
      projectManager: projectOriginalManager
    }

    // Sessions and live data scope
    for (const [role, phone] of ROLE_ACCOUNTS) {
      const login = await post('auth/session/password-login', { phone, password: PASSWORD })
      requireSetup(login.success && login.data?.token, `${role} login failed`)
      tokens[role] = login.data.token
      const info = await post('auth/session/info', {}, tokens[role])
      requireSetup(info.success && info.data?.employee, `${role} session info failed`)
      sessions[role] = { empId: idOf(info.data.employee.empId), role: normalized(info.data.employee.role), comId: idOf(info.data.employee.company?.comId) }
      check(`B66-LOGIN-${role}`, 'session', `${role} 登录与会话绑定`,
        Boolean(sessions[role].empId && sessions[role].role && sessions[role].comId),
        '登录成功且返回员工、角色、企业',
        sessions[role])
    }
    check('B66-LIVE-01', 'session', '实时查询可用 ReadyToOrder 材料计划',
      /ReadyToOrder|可下单/.test(sqlOne(`SELECT status FROM decoboon.db_material_plan WHERE plan_id=${PLAN_ID}`)),
      '当前企业 M 存在可下单材料计划', { planId: PLAN_ID, status: planOriginal.status })

    const partnerPage = await post('partner/page', { current: 1, size: 50, type: 'Supplier' }, tokens.Finance)
    const suppliers = dataList(partnerPage)
    const existingSupplier = suppliers.find(item => idOf(item.partnerId) === '1791842081487998780') || suppliers.find(item => item.status?.key === 'Active' || item.status === 'Active')
    requireSetup(existingSupplier, 'no active supplier available')
    const existingSupplierId = idOf(existingSupplier.partnerId)
    check('B66-LIVE-02', 'session', '实时查询可用供应商', suppliers.length > 0,
      '财务供应商列表非空', { supplierCount: suppliers.length, existingSupplierId })

    const tempName = `B6.6独立复验临时厂家-${Date.now()}`
    const partnerSave = await post('partner/save', { type: 'Supplier', name: tempName, remark: 'B6.6 independent verification temporary supplier' }, tokens.Finance)
    requireSetup(partnerSave.success, `partner/save ${JSON.stringify(errorCode(partnerSave))} ${errorMessage(partnerSave)}`)
    tempPartnerId = idOf(partnerSave.data.partnerId)
    owned.partnerIds.add(tempPartnerId)
    check('B66-LIVE-03', 'session', '创建第二临时供应商用于真实不同供应商拆单',
      Boolean(tempPartnerId), '第二供应商建档成功', { tempPartnerId, tempName })

    // Projection case 1: all Priced
    let scenario = await setupScenario(5, 'priced')
    let orderResult = await order(scenario, { supplierId: existingSupplierId, items: [{ itemId: scenario.itemId, quantity: 5, purchasePrice: 100 }] }, `b66-all-priced-${Date.now()}`)
    requireSetup(orderResult.response.success, `all-priced order ${JSON.stringify(errorCode(orderResult.response))}`)
    let purchaseIds = await syncPurchases(scenario)
    let info = await sheetInfo(tokens.Finance, scenario.sheetId)
    let pageResult = await sheetPage(tokens.Finance, scenario.sheetId)
    let projection = sheetProjection(info)
    let itemFacts = await purchaseItemFacts(purchaseIds)
    check('B66-PROJ-PRICED-I', 'b6.6-focus', '全有价：sheet/item priceState=Priced', projection.sheetPriceState === 'Priced' && projection.itemPriceState === 'Priced', 'Priced/Priced', projection, { sheetId: scenario.sheetId })
    check('B66-PROJ-PRICED-P', 'b6.6-focus', '全有价：page 投影同源为 Priced', normalized(pageResult.sheet?.priceState) === 'Priced' && normalized(pageResult.sheet?.items?.[0]?.priceState) === 'Priced', 'page sheet/item=Priced', { sheet: normalized(pageResult.sheet?.priceState), item: normalized(pageResult.sheet?.items?.[0]?.priceState) })
    check('B66-PROJ-PRICED-F', 'b6.6-focus', '全有价：采购明细事实为 Priced', itemFacts.length === 1 && itemFacts[0].priceState === 'Priced', '1 条 Priced 采购明细', itemFacts)
    apiSamples.allPriced = { sheetId: scenario.sheetId, projection, itemFacts }
    await captureWebOrderingState('priced', scenario, 'Priced', '已定价')
    await cleanupBusiness(); setPlanReady(5)

    // Projection case 2: all Unpriced
    scenario = await setupScenario(5, 'unpriced')
    orderResult = await order(scenario, { supplierId: tempPartnerId, items: [{ itemId: scenario.itemId, quantity: 5, purchasePrice: null }] }, `b66-all-unpriced-${Date.now()}`)
    requireSetup(orderResult.response.success, `all-unpriced order ${JSON.stringify(errorCode(orderResult.response))}`)
    purchaseIds = await syncPurchases(scenario)
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    pageResult = await sheetPage(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    itemFacts = await purchaseItemFacts(purchaseIds)
    let payables = await payableFacts(purchaseIds)
    check('B66-PROJ-UNPRICED-I', 'b6.6-focus', '全无价：sheet/item priceState=Unpriced', projection.sheetPriceState === 'Unpriced' && projection.itemPriceState === 'Unpriced', 'Unpriced/Unpriced', projection, { sheetId: scenario.sheetId })
    check('B66-PROJ-UNPRICED-P', 'b6.6-focus', '全无价：page 投影同源为 Unpriced', normalized(pageResult.sheet?.priceState) === 'Unpriced' && normalized(pageResult.sheet?.items?.[0]?.priceState) === 'Unpriced', 'page sheet/item=Unpriced', { sheet: normalized(pageResult.sheet?.priceState), item: normalized(pageResult.sheet?.items?.[0]?.priceState) })
    check('B66-PROJ-UNPRICED-PAY', 'b6.6-focus', '全无价形成暂估应付', payables.length === 1 && payables[0].estimated === '1', 'estimated=true', payables)
    apiSamples.allUnpriced = { sheetId: scenario.sheetId, projection, itemFacts, payables }
    await captureWebOrderingState('unpriced', scenario, 'Unpriced', '未定价 · 成本待结算')
    await cleanupBusiness(); setPlanReady(5)

    // Projection case 3: all Settled at same price
    scenario = await setupScenario(5, 'settled')
    orderResult = await order(scenario, { supplierId: existingSupplierId, items: [{ itemId: scenario.itemId, quantity: 5, purchasePrice: 100 }] }, `b66-all-settled-${Date.now()}`)
    requireSetup(orderResult.response.success, `all-settled order ${JSON.stringify(errorCode(orderResult.response))}`)
    purchaseIds = await syncPurchases(scenario)
    itemFacts = await purchaseItemFacts(purchaseIds)
    requireSetup(itemFacts.length === 1, 'all-settled purchase item missing')
    let receiveResp = await receipt(scenario, purchaseIds[0], itemFacts[0].purchaseItemId, 5)
    requireSetup(receiveResp.success, `all-settled receipt ${JSON.stringify(errorCode(receiveResp))}`)
    let preview = await settlePreview(existingSupplierId, 'PerPurchaseOrder', PERIOD, purchaseIds[0])
    requireSetup(preview.success && dataList(preview).length === 1, `all-settled preview ${JSON.stringify(errorCode(preview))}`)
    let confirm = await settleConfirm(existingSupplierId, 'PerPurchaseOrder', PERIOD, [{ purchaseItem: { purchaseItemId: itemFacts[0].purchaseItemId }, quantity: 5, settlementPrice: 100 }])
    requireSetup(confirm.success, `all-settled confirm ${JSON.stringify(errorCode(confirm))}`)
    owned.settlementIds.add(idOf(confirm.data.settlementId))
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    pageResult = await sheetPage(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    itemFacts = await purchaseItemFacts(purchaseIds)
    check('B66-PROJ-SETTLED-I', 'b6.6-focus', '全部 Settled（同价）：sheet/item priceState=Settled', projection.sheetPriceState === 'Settled' && projection.itemPriceState === 'Settled' && Number(projection.settlementPrice) === 100, 'Settled/Settled 且 settlementPrice=100', projection, { sheetId: scenario.sheetId })
    check('B66-PROJ-SETTLED-P', 'b6.6-focus', '全部 Settled：page 投影同源为 Settled', normalized(pageResult.sheet?.priceState) === 'Settled' && normalized(pageResult.sheet?.items?.[0]?.priceState) === 'Settled', 'page sheet/item=Settled', { sheet: normalized(pageResult.sheet?.priceState), item: normalized(pageResult.sheet?.items?.[0]?.priceState) })
    check('B66-PROJ-SETTLED-F', 'b6.6-focus', '全部 Settled：采购明细结算价回填', itemFacts.length === 1 && itemFacts[0].priceState === 'Settled' && Number(itemFacts[0].settlementPrice) === 100, 'Settled + settlementPrice=100', itemFacts)
    apiSamples.allSettled = { sheetId: scenario.sheetId, projection, itemFacts, settlementId: confirm.data.settlementId, totalAmount: confirm.data.totalAmount }
    await captureWebOrderingState('settled', scenario, 'Settled', '已结算')
    await cleanupBusiness(); setPlanReady(5)

    // Main focused B62-DEF-001 scenario
    scenario = await setupScenario(8, 'mixed')
    main = { ...scenario }
    const pricedKey = `b66-main-priced-${Date.now()}`
    const pricedPayload = { supplierId: existingSupplierId, items: [{ itemId: scenario.itemId, quantity: 3, purchasePrice: 100 }] }
    const firstOrder = await order(scenario, pricedPayload, pricedKey)
    requireSetup(firstOrder.response.success, `priced order ${JSON.stringify(errorCode(firstOrder.response))}`)
    const retryOrder = await order(scenario, pricedPayload, pricedKey)
    purchaseIds = await syncPurchases(scenario)
    const afterRetryCount = purchaseIds.length
    check('B66-IDEM-01', 'idempotency', '同幂等键同载荷重复下单只生成 1 张采购单', retryOrder.response.success && afterRetryCount === 1,
      '第二次返回同一结果且采购单 count=1', { firstSuccess: firstOrder.response.success, retrySuccess: retryOrder.response.success, purchaseCount: afterRetryCount, purchaseIds })
    const conflict = await order(scenario, { supplierId: existingSupplierId, items: [{ itemId: scenario.itemId, quantity: 4, purchasePrice: 101 }] }, pricedKey)
    purchaseIds = await syncPurchases(scenario)
    check('B66-IDEM-02', 'idempotency', '同幂等键异载荷返回 409 且不新增采购', !conflict.response.success && String(errorCode(conflict.response)).includes('409') && purchaseIds.length === 1,
      '409 + count 仍为 1', { success: conflict.response.success, errorCode: errorCode(conflict.response), message: errorMessage(conflict.response), purchaseCount: purchaseIds.length })
    let purchaseFactsAfterPriced = await purchaseFacts(purchaseIds)
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    check('B66-MAIN-01', 'b6.6-focus', '首次有价部分下单后 sheet/item=Priced', projection.sheetPriceState === 'Priced' && projection.itemPriceState === 'Priced' && Number(projection.orderedQuantity) === 3,
      'Priced/Priced, orderedQuantity=3', projection)
    check('B66-MAIN-02', 'b6.6-focus', '有价采购事实 3@100', purchaseFactsAfterPriced.length === 1 && Number(purchaseFactsAfterPriced[0].quantity) === 3 && Number(purchaseFactsAfterPriced[0].amount) === 300,
      'quantity=3, amount=300', purchaseFactsAfterPriced)

    const unpricedOrder = await order(scenario, { supplierId: tempPartnerId, items: [{ itemId: scenario.itemId, quantity: 5, purchasePrice: null }] }, `b66-main-unpriced-${Date.now()}`)
    requireSetup(unpricedOrder.response.success, `unpriced order ${JSON.stringify(errorCode(unpricedOrder.response))}`)
    purchaseIds = await syncPurchases(scenario)
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    pageResult = await sheetPage(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    const purchases = await purchaseFacts(purchaseIds)
    const items = await purchaseItemFacts(purchaseIds)
    const payablesAtOrder = await payableFacts(purchaseIds)
    check('B66-B62-I-01', 'b6.6-focus', '不同供应商拆单后 info sheet/item=Mixed', projection.sheetPriceState === 'Mixed' && projection.itemPriceState === 'Mixed' && Number(projection.orderedQuantity) === 8,
      'Mixed/Mixed, orderedQuantity=8', projection, { sheetId: scenario.sheetId })
    check('B66-B62-P-01', 'b6.6-focus', '不同供应商拆单后 page sheet/item=Mixed', normalized(pageResult.sheet?.priceState) === 'Mixed' && normalized(pageResult.sheet?.items?.[0]?.priceState) === 'Mixed',
      'page Mixed/Mixed', { sheet: normalized(pageResult.sheet?.priceState), item: normalized(pageResult.sheet?.items?.[0]?.priceState) })
    check('B66-B62-F-01', 'b6.6-focus', '两个供应商采购事实分别为 Priced 与 Unpriced', purchases.length === 2 && items.length === 2 && items.some(i => i.priceState === 'Priced' && Number(i.unitPrice) === 100) && items.some(i => i.priceState === 'Unpriced'),
      '2 purchases, Priced 100 + Unpriced', { purchases, items })
    check('B66-B62-PAY-01', 'b6.6-focus', '拆单应付事实：有价确定、无价暂估', payablesAtOrder.length === 2 && payablesAtOrder.some(p => p.estimated === '0' && Number(p.amount) === 300) && payablesAtOrder.some(p => p.estimated === '1'),
      '300 estimated=false + 0 estimated=true', payablesAtOrder)
    const mainPricedItem = items.find(i => i.priceState === 'Priced')
    const mainUnpricedItem = items.find(i => i.priceState === 'Unpriced')
    requireSetup(mainPricedItem && mainUnpricedItem, 'main purchase items missing')

    const unreceivedPreview = await settlePreview(tempPartnerId, 'Monthly', PERIOD)
    const unreceivedConfirm = await settleConfirm(tempPartnerId, 'Monthly', PERIOD, [{ purchaseItem: { purchaseItemId: mainUnpricedItem.purchaseItemId }, quantity: 5, settlementPrice: 120 }])
    check('B66-GATE-01', 'boundary', '未到场采购明细不进入结算 preview', unreceivedPreview.success && dataList(unreceivedPreview).length === 0,
      'preview=[]', { success: unreceivedPreview.success, rows: dataList(unreceivedPreview).length })
    check('B66-GATE-02', 'boundary', '未到场采购明细 confirm 被拒绝', !unreceivedConfirm.success && /尚未到场/.test(errorMessage(unreceivedConfirm)),
      '拒绝且提示尚未到场', { success: unreceivedConfirm.success, message: errorMessage(unreceivedConfirm) })

    const pricedReceipt = await receipt(scenario, purchases.find(p => p.supplierId === existingSupplierId)?.purchaseId, mainPricedItem.purchaseItemId, 2)
    requireSetup(pricedReceipt.success, `partial priced receipt ${JSON.stringify(errorCode(pricedReceipt))}`)
    const unpricedReceipt = await receipt(scenario, purchases.find(p => p.supplierId === tempPartnerId)?.purchaseId, mainUnpricedItem.purchaseItemId, 5)
    requireSetup(unpricedReceipt.success, `unpriced receipt ${JSON.stringify(errorCode(unpricedReceipt))}`)
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    check('B66-B62-PARTIAL-01', 'b6.6-focus', '部分到场后 sheet/item 仍为 Mixed', projection.sheetPriceState === 'Mixed' && projection.itemPriceState === 'Mixed' && projection.sheetStatus === 'PartiallyArrived',
      'PartiallyArrived + Mixed/Mixed', projection)

    const monthlyPreview = await settlePreview(tempPartnerId, 'Monthly', PERIOD)
    const previewRows = dataList(monthlyPreview)
    check('B66-SETTLE-01', 'main-chain', '到场后月结 preview 仅含无价部分 5@120', monthlyPreview.success && previewRows.length === 1 && Number(previewRows[0].receivedQuantity) === 5,
      '1 行，receivedQuantity=5', previewRows)
    const monthlyConfirm = await settleConfirm(tempPartnerId, 'Monthly', PERIOD, [{ purchaseItem: { purchaseItemId: mainUnpricedItem.purchaseItemId }, quantity: 5, settlementPrice: 120 }])
    requireSetup(monthlyConfirm.success, `monthly confirm ${JSON.stringify(errorCode(monthlyConfirm))}`)
    owned.settlementIds.add(idOf(monthlyConfirm.data.settlementId))
    check('B66-SETTLE-02', 'main-chain', '无价部分 5@120 结算金额=600', Number(monthlyConfirm.data.totalAmount) === 600,
      'totalAmount=600', { settlementId: monthlyConfirm.data.settlementId, totalAmount: monthlyConfirm.data.totalAmount })
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    pageResult = await sheetPage(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    const itemsAfterSettle = await purchaseItemFacts(purchaseIds)
    check('B66-B62-SETTLE-01', 'b6.6-focus', '无价部分结算后 info 不得回退为 Unpriced，应为 Mixed', projection.sheetPriceState === 'Mixed' && projection.itemPriceState === 'Mixed',
      'Mixed/Mixed', projection)
    check('B66-B62-SETTLE-02', 'b6.6-focus', '无价部分结算后 page 仍为 Mixed', normalized(pageResult.sheet?.priceState) === 'Mixed' && normalized(pageResult.sheet?.items?.[0]?.priceState) === 'Mixed',
      'page Mixed/Mixed', { sheet: normalized(pageResult.sheet?.priceState), item: normalized(pageResult.sheet?.items?.[0]?.priceState) })
    check('B66-B62-SETTLE-03', 'b6.6-focus', '采购明细事实为 Priced + Settled(120)', itemsAfterSettle.length === 2 && itemsAfterSettle.some(i => i.priceState === 'Priced') && itemsAfterSettle.some(i => i.priceState === 'Settled' && Number(i.settlementPrice) === 120),
      'Priced + Settled(120)', itemsAfterSettle)
    const postSettlePreview = await settlePreview(tempPartnerId, 'Monthly', PERIOD)
    const duplicateSettle = await settleConfirm(tempPartnerId, 'Monthly', PERIOD, [{ purchaseItem: { purchaseItemId: mainUnpricedItem.purchaseItemId }, quantity: 5, settlementPrice: 120 }])
    check('B66-SETTLE-03', 'idempotency', '已结算明细不再进入 preview 且重复 confirm 被拒', postSettlePreview.success && dataList(postSettlePreview).length === 0 && !duplicateSettle.success,
      'preview=[] + duplicate rejected', { previewRows: dataList(postSettlePreview).length, duplicateSuccess: duplicateSettle.success, duplicateMessage: errorMessage(duplicateSettle) })

    await captureWebOrderingState('mixed-settled', scenario, 'Mixed', '部分已结算 · 成本待结算')

    const remainingReceipt = await receipt(scenario, purchases.find(p => p.supplierId === existingSupplierId)?.purchaseId, mainPricedItem.purchaseItemId, 1)
    requireSetup(remainingReceipt.success, `remaining priced receipt ${JSON.stringify(errorCode(remainingReceipt))}`)
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    check('B66-MAIN-03', 'b6.6-focus', '全部到场后 sheet=Arrived 且仍 Mixed', projection.sheetStatus === 'Arrived' && projection.sheetPriceState === 'Mixed' && projection.itemPriceState === 'Mixed',
      'Arrived + Mixed/Mixed', projection)

    const cancel = await post('material/settlement/cancel', { settlementId: monthlyConfirm.data.settlementId, reason: 'B6.6独立复验撤销结算' }, tokens.Finance)
    requireSetup(cancel.success, `settlement cancel ${JSON.stringify(errorCode(cancel))}`)
    info = await sheetInfo(tokens.Finance, scenario.sheetId)
    pageResult = await sheetPage(tokens.Finance, scenario.sheetId)
    projection = sheetProjection(info)
    const itemsAfterCancel = await purchaseItemFacts(purchaseIds)
    const payablesAfterCancel = await payableFacts(purchaseIds)
    check('B66-B62-CANCEL-01', 'b6.6-focus', '撤销结算后 info 仍为 Mixed', projection.sheetPriceState === 'Mixed' && projection.itemPriceState === 'Mixed',
      'Mixed/Mixed', projection)
    check('B66-B62-CANCEL-02', 'b6.6-focus', '撤销结算后 page 仍为 Mixed', normalized(pageResult.sheet?.priceState) === 'Mixed' && normalized(pageResult.sheet?.items?.[0]?.priceState) === 'Mixed',
      'page Mixed/Mixed', { sheet: normalized(pageResult.sheet?.priceState), item: normalized(pageResult.sheet?.items?.[0]?.priceState) })
    check('B66-B62-CANCEL-03', 'b6.6-focus', '撤销后采购明细回到 Priced + Unpriced', itemsAfterCancel.length === 2 && itemsAfterCancel.some(i => i.priceState === 'Priced') && itemsAfterCancel.some(i => i.priceState === 'Unpriced' && i.settlementPrice == null),
      'Priced + Unpriced(null)', itemsAfterCancel)
    check('B66-PAY-01', 'boundary', '撤销后无价应付回到暂估', payablesAfterCancel.some(p => p.estimated === '1' && p.payeePartnerId === String(tempPartnerId)),
      'temp supplier payable estimated=true', payablesAfterCancel)
    const rePreview = await settlePreview(tempPartnerId, 'Monthly', PERIOD)
    check('B66-SETTLE-04', 'main-chain', '撤销后采购明细重新进入可结算集', rePreview.success && dataList(rePreview).length === 1,
      'preview 重新出现 1 行', dataList(rePreview))

    const payablesApi = await financePayables(tempPartnerId)
    const tempPayables = dataList(payablesApi)
    check('B66-PAY-02', 'boundary', 'finance/payable/page 暴露 estimated 且不伪装 0 元确定应付', tempPayables.length > 0 && tempPayables.every(p => p.estimated === true || p.estimated === 'true') && tempPayables.some(p => p.amount === 0 || p.amount === null),
      'estimated=true 且 amount=0/null', tempPayables.slice(0, 5).map(p => ({ payableId: p.payableId, amount: p.amount, estimated: p.estimated, settledAmount: p.settledAmount })))

    const health = await post('blueprint/check/report', { projectId: PROJECT_ID }, tokens.Finance)
    check('B66-COST-01', 'boundary', '成本不完整时利润为 null 而非 0', health.success && health.data?.costComplete === false && health.data?.profitAmount == null,
      'costComplete=false, profitAmount=null', health.success ? { costComplete: health.data.costComplete, profitAmount: health.data.profitAmount, actualCost: health.data.actualCost } : { errorCode: errorCode(health) })

    // Permission negatives while main data is present
    for (const role of ['Designer', 'Pm']) {
      const roleInfo = await sheetInfo(tokens[role], scenario.sheetId)
      const item = roleInfo.data?.items?.[0]
      const commercialAbsent = item && !Object.prototype.hasOwnProperty.call(item, 'quoteCostPrice')
        && !Object.prototype.hasOwnProperty.call(item, 'purchasePrice')
        && !Object.prototype.hasOwnProperty.call(item, 'settlementPrice')
        && !Object.prototype.hasOwnProperty.call(item, 'priceState')
      check(`B66-PERM-${role}-SHEET`, 'permission', `${role} 作业单不返回成本/价格字段`, roleInfo.success && Boolean(commercialAbsent),
        'success + 商业字段缺失', { success: roleInfo.success, keys: item ? Object.keys(item) : [], sheetPriceState: roleInfo.data?.priceState })
      const rolePage = await post('material/sheet/page', {}, tokens[role])
      const roleSheet = dataList(rolePage).find(row => idOf(row.sheetId) === String(scenario.sheetId))
      const rolePageItem = roleSheet?.items?.[0]
      const pageCommercialAbsent = rolePageItem && !Object.prototype.hasOwnProperty.call(rolePageItem, 'quoteCostPrice')
        && !Object.prototype.hasOwnProperty.call(rolePageItem, 'purchasePrice')
        && !Object.prototype.hasOwnProperty.call(rolePageItem, 'settlementPrice')
        && !Object.prototype.hasOwnProperty.call(rolePageItem, 'priceState')
      check(`B66-PERM-${role}-SHEETPAGE`, 'permission', `${role} 作业单列表不返回成本/价格字段`, rolePage.success && Boolean(pageCommercialAbsent),
        'success + 商业字段缺失', { success: rolePage.success, keys: rolePageItem ? Object.keys(rolePageItem) : [] })
      const purchasePage = await post('project/purchase/page', { projectId: PROJECT_ID }, tokens[role])
      const rolePurchase = dataList(purchasePage).find(row => purchaseIds.includes(idOf(row.purchaseId)))
      const purchasesMasked = rolePurchase && rolePurchase.amount == null && rolePurchase.unitPrice == null && rolePurchase.supplierName == null && rolePurchase.supplier == null && rolePurchase.items?.every(i => i.unitPrice == null && i.amount == null)
      check(`B66-PERM-${role}-PURCHASE`, 'permission', `${role} 采购不返回金额/单价/供应商`, purchasePage.success && Boolean(purchasesMasked),
        '履约字段保留，商业字段为空', rolePurchase ? { purchaseId: rolePurchase.purchaseId, amount: rolePurchase.amount, unitPrice: rolePurchase.unitPrice, supplierName: rolePurchase.supplierName, itemUnitPrice: rolePurchase.items?.[0]?.unitPrice } : { rows: dataList(purchasePage).length })
      const settlementPage = await post('material/settlement/page', { supplierId: tempPartnerId }, tokens[role])
      const settlementInfo = await post('material/settlement/info', { settlementId: monthlyConfirm.data.settlementId }, tokens[role])
      check(`B66-PERM-${role}-SETTLEMENT`, 'permission', `${role} 结算 page/info 被拒绝`, !settlementPage.success && !settlementInfo.success && String(errorCode(settlementPage)).includes('403') && String(errorCode(settlementInfo)).includes('403'),
        'page=403, info=403', { page: { success: settlementPage.success, errorCode: errorCode(settlementPage), message: errorMessage(settlementPage) }, info: { success: settlementInfo.success, errorCode: errorCode(settlementInfo), message: errorMessage(settlementInfo) } })
      const orderDenied = await post('material/sheet/order', { sheetId: scenario.sheetId, supplierId: existingSupplierId, deliveryMode: 'Direct', items: [{ itemId: scenario.itemId, quantity: 1, purchasePrice: 100 }], idempotencyKey: `b66-denied-order-${role}` }, tokens[role])
      check(`B66-PERM-${role}-ORDER`, 'permission', `${role} 下单被拒绝`, !orderDenied.success && String(errorCode(orderDenied)).includes('403'),
        '403', { success: orderDenied.success, errorCode: errorCode(orderDenied), message: errorMessage(orderDenied) })
      const confirmDenied = await post('material/settlement/confirm', { supplierId: tempPartnerId, scope: 'Monthly', period: PERIOD, items: [{ purchaseItem: { purchaseItemId: mainUnpricedItem.purchaseItemId }, quantity: 1, settlementPrice: 120 }] }, tokens[role])
      check(`B66-PERM-${role}-CONFIRM`, 'permission', `${role} 结算确认被拒绝`, !confirmDenied.success && String(errorCode(confirmDenied)).includes('403'),
        '403', { success: confirmDenied.success, errorCode: errorCode(confirmDenied), message: errorMessage(confirmDenied) })
    }

    for (const role of ['Boss', 'StoreManager', 'Finance']) {
      const infoAllowed = await sheetInfo(tokens[role], scenario.sheetId)
      const settlementPageAllowed = await post('material/settlement/page', { supplierId: tempPartnerId }, tokens[role])
      check(`B66-ALLOW-${role}`, 'permission', `${role} 保留作业单与结算读权限`, infoAllowed.success && settlementPageAllowed.success,
        'sheet info + settlement page success', { sheetInfo: infoAllowed.success, settlementPage: settlementPageAllowed.success, settlementError: errorCode(settlementPageAllowed) })
    }

    // Cross-tenant negatives
    const crossSheet = await sheetInfo(tokens.CrossBoss, scenario.sheetId)
    const crossPurchase = await post('project/purchase/page', { projectId: PROJECT_ID }, tokens.CrossBoss)
    const crossSettlement = await post('material/settlement/page', { supplierId: tempPartnerId }, tokens.CrossBoss)
    const crossPayable = await post('finance/payable/page', { current: 1, size: 50, partnerId: tempPartnerId }, tokens.CrossBoss)
    const crossPartner = await post('partner/page', { current: 1, size: 50, type: 'Supplier' }, tokens.CrossBoss)
    const crossHealth = await post('blueprint/check/report', { projectId: PROJECT_ID }, tokens.CrossBoss)
    const crossAssistant = await post('material/assistant/ask', { question: '这个项目材料业务现在缺什么？', projectId: PROJECT_ID, requestId: `b66-cross-${Date.now()}`, idempotencyKey: `b66-cross-${Date.now()}` }, tokens.CrossBoss)
    check('B66-CROSS-01', 'cross-tenant', '跨企业 Boss 作业单详情 404', !crossSheet.success && String(errorCode(crossSheet)).includes('404'), '404', { success: crossSheet.success, errorCode: errorCode(crossSheet) })
    check('B66-CROSS-02', 'cross-tenant', '跨企业 Boss 采购列表无 M 企业数据', (!crossPurchase.success) || dataList(crossPurchase).every(row => !purchaseIds.includes(idOf(row.purchaseId))), '无目标采购行', { success: crossPurchase.success, rows: dataList(crossPurchase).length, errorCode: errorCode(crossPurchase) })
    check('B66-CROSS-03', 'cross-tenant', '跨企业 Boss 结算列表无 M 企业结算', (!crossSettlement.success) || dataList(crossSettlement).every(row => idOf(row.settlementId) !== idOf(monthlyConfirm.data.settlementId)), '无目标结算', { success: crossSettlement.success, rows: dataList(crossSettlement).length, errorCode: errorCode(crossSettlement) })
    check('B66-CROSS-04', 'cross-tenant', '跨企业 Boss 应付无临时供应商数据', crossPayable.success && dataList(crossPayable).length === 0, '0 行', { success: crossPayable.success, rows: dataList(crossPayable).length })
    check('B66-CROSS-05', 'cross-tenant', '跨企业 Boss 供应商列表无临时供应商', crossPartner.success && !dataList(crossPartner).some(p => idOf(p.partnerId) === String(tempPartnerId)), '无临时供应商', { success: crossPartner.success, rows: dataList(crossPartner).length })
    check('B66-CROSS-06', 'cross-tenant', '跨企业 Boss 材料体检 404', !crossHealth.success && String(errorCode(crossHealth)).includes('404'), '404', { success: crossHealth.success, errorCode: errorCode(crossHealth) })
    check('B66-CROSS-07', 'cross-tenant', '跨企业 Boss 材料助手 404', !crossAssistant.success && String(errorCode(crossAssistant)).includes('404'), '404', { success: crossAssistant.success, errorCode: errorCode(crossAssistant) })

    // Old route and historical data
    const oldSave = await post('project/purchase/save', {}, tokens.Boss)
    const oldBatch = await post('project/purchase/batch', {}, tokens.Boss)
    const historical = await post('project/purchase/page', {}, tokens.Boss)
    const planWorkbench = await post('material/plan/workbench', {}, tokens.Boss)
    check('B66-OLD-01', 'old-route', '旧直采保存 Provider 不存在', !oldSave.success && String(errorCode(oldSave)).includes('canNotFoundHandler'), 'canNotFoundHandler', { success: oldSave.success, errorCode: errorCode(oldSave), message: errorMessage(oldSave) })
    check('B66-OLD-02', 'old-route', '旧直采批量 Provider 不存在', !oldBatch.success && String(errorCode(oldBatch)).includes('canNotFoundHandler'), 'canNotFoundHandler', { success: oldBatch.success, errorCode: errorCode(oldBatch), message: errorMessage(oldBatch) })
    check('B66-OLD-03', 'old-route', '历史采购仍可查询', historical.success && dataList(historical).length > 0, '历史采购非空', { success: historical.success, rows: dataList(historical).length })
    const workbenchText = JSON.stringify(planWorkbench.data ?? '')
    const directPurchaseHint = /直接采购|从材料计划下单|材料计划直采/.test(workbenchText)
    check('B66-OLD-04', 'old-route', '材料计划工作台不再提供计划直采动作', planWorkbench.success && !directPurchaseHint, '无直接采购/材料计划下单动作', { success: planWorkbench.success, directPurchaseHint })
    apiSamples.oldRoutes = { save: { success: oldSave.success, errorCode: errorCode(oldSave) }, batch: { success: oldBatch.success, errorCode: errorCode(oldBatch) }, historicalPurchaseRows: dataList(historical).length }

    // 50 golden questions
    const golden = JSON.parse(fs.readFileSync('/tmp/th4-b66-backend-b66a33b/src/main/resources/assistant/material-golden-questions.json', 'utf8'))
    let goldenPass = 0
    const goldenSummary = []
    for (const q of golden) {
      const requestId = `b66-golden-${q.id}-${Date.now()}`
      const response = await post('material/assistant/ask', {
        question: q.question, projectId: PROJECT_ID, sheetId: scenario.sheetId, requestId,
        idempotencyKey: `b66-golden-${q.id}-${Date.now()}`
      }, tokens.Boss)
      const answer = response.data
      const expectedIntent = q.expectedIntent
      const hasEvidenceOrRefusal = (answer?.citations?.length > 0) || ['REFUSAL', 'NO_EVIDENCE'].includes(answer?.answerType)
      const pass = response.success && answer?.intent === expectedIntent && answer?.readOnly === true && answer?.auditStatus === 'RECORDED' && hasEvidenceOrRefusal
      if (pass) goldenPass++
      const summary = { id: q.id, expectedIntent, actualIntent: answer?.intent, answerType: answer?.answerType, citations: answer?.citations?.length ?? 0, readOnly: answer?.readOnly, auditStatus: answer?.auditStatus, result: pass ? 'PASS' : 'FAIL' }
      goldenSummary.push(summary)
      check(`B66-AI-${q.id}`, 'assistant', `黄金问题 ${q.id}: ${q.question}`, pass,
        `${expectedIntent} + evidence/refusal + readOnly + RECORDED`, summary)
    }
    const idemQ = '暂估应付是什么意思？'
    const idemKey = `b66-assistant-idem-${Date.now()}`
    const aiFirst = await post('material/assistant/ask', { question: idemQ, projectId: PROJECT_ID, requestId: 'b66-assistant-same', idempotencyKey: idemKey }, tokens.Boss)
    const aiSecond = await post('material/assistant/ask', { question: idemQ, projectId: PROJECT_ID, requestId: 'b66-assistant-same', idempotencyKey: idemKey }, tokens.Boss)
    check('B66-AI-IDEM', 'idempotency', '助手 requestId+idempotencyKey 幂等返回同一审计', aiFirst.success && aiSecond.success && aiFirst.data?.auditId === aiSecond.data?.auditId,
      'same auditId', { first: aiFirst.data?.auditId, second: aiSecond.data?.auditId })
    const unknown = await post('material/assistant/ask', { question: '今天合肥天气怎么样？', requestId: 'b66-unknown', idempotencyKey: `b66-unknown-${Date.now()}` }, tokens.Boss)
    check('B66-AI-UNKNOWN', 'assistant', '无证据/越界问题拒答', unknown.success && ['NO_EVIDENCE','REFUSAL'].includes(unknown.data?.answerType) && unknown.data?.readOnly === true,
      'NO_EVIDENCE 或 REFUSAL + readOnly', { intent: unknown.data?.intent, answerType: unknown.data?.answerType, readOnly: unknown.data?.readOnly })
    const designerAi = await post('material/assistant/ask', { question: '这个项目材料业务现在缺什么？', projectId: PROJECT_ID, requestId: 'b66-designer-ai', idempotencyKey: `b66-designer-ai-${Date.now()}` }, tokens.Designer)
    const designerAnswer = designerAi.data?.answer || ''
    const designerHasMoney = /(¥|￥|\d+\.\d{2}|成本\s*[:：]?\s*\d|利润\s*[:：]?\s*\d)/.test(designerAnswer)
    check('B66-AI-PERM', 'assistant', 'Designer 项目材料回答不返回成本金额', designerAi.success && !designerHasMoney && /仅对 Boss|财务展示|履约状态/.test(designerAnswer),
      '仅履约状态，无成本/金额', { intent: designerAi.data?.intent, answer: designerAi.data?.answer })
    apiSamples.assistant = { golden: { pass: goldenPass, total: golden.length, failures: goldenSummary.filter(row => row.result === 'FAIL') }, idempotency: { first: aiFirst.data?.auditId, second: aiSecond.data?.auditId }, unknown: { intent: unknown.data?.intent, answerType: unknown.data?.answerType }, designer: { intent: designerAi.data?.intent, answer: designerAi.data?.answer } }

    // UI: Web and App
    const browser = await chromium.launch({ headless: true })
    try {
      const webContext = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
      const web = await webContext.newPage()
      let webErrors = []
      let orderingErrors = []
      let payableErrors = []
      web.on('console', msg => { if (msg.type() === 'error') webErrors.push(msg.text()) })
      await web.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
      await web.evaluate(([key, value]) => localStorage.setItem(key, value), ['decoboon-web.token', tokens.Finance])
      await web.goto(`${WEB}/finance/material-ordering`, { waitUntil: 'networkidle' })
      await web.reload({ waitUntil: 'networkidle' })
      await web.waitForTimeout(1200)
      orderingErrors = [...webErrors]
      webErrors = []
      const orderingBody = await web.locator('body').innerText()
      const orderingRow = web.locator('li', { hasText: main.sheetNo }).first()
      const orderingRowCount = await orderingRow.count()
      const orderingRowText = orderingRowCount ? await orderingRow.innerText() : ''
      const orderingMetrics = await web.evaluate(() => ({ innerWidth: window.innerWidth, innerHeight: window.innerHeight, scrollWidth: document.documentElement.scrollWidth, bodyScrollWidth: document.body.scrollWidth, scrollHeight: document.documentElement.scrollHeight, domNodeCount: document.querySelectorAll('*').length, htmlLength: document.documentElement.outerHTML.length }))
      const postCancelPill = orderingRowCount ? orderingRow.locator('.mo__price-state').first() : null
      const postCancelPillCount = postCancelPill ? await postCancelPill.count() : 0
      const postCancelPillText = postCancelPillCount ? (await postCancelPill.innerText()).trim() : ''
      const postCancelPillClass = postCancelPillCount ? (await postCancelPill.getAttribute('class')) || '' : ''
      const postCancelApi = await sheetPage(tokens.Finance, main.sheetId)
      const postCancelApiState = normalized(postCancelApi.sheet?.priceState)
      await web.screenshot({ path: path.join(SHOTS, 'b66-web-material-ordering-mixed-postcancel-1440x900.png') })
      const webMixedTextVisible = /部分已结算 · 成本待结算/.test(orderingRowText)
      check('B66-UI-WEB-SHEET-01', 'ui', 'Web 1440×900 主材下单页展示 Mixed 价格状态/成本待结算',
        orderingRowCount > 0 && webMixedTextVisible,
        '目标作业单行显示混合/部分已结算/成本待结算文案', { sheetId: main.sheetId, sheetNo: main.sheetNo, containsSheet: orderingRowCount > 0, mixedTextVisible: webMixedTextVisible, rowText: orderingRowText, excerpt: orderingBody.slice(-800) })
      check('B66-UI-WEB-MIXED-POSTCANCEL-PILL', 'ui', 'Web 撤销后 Mixed pill 仍为「部分已结算 · 成本待结算」',
        postCancelPillCount > 0 && postCancelPillText === '部分已结算 · 成本待结算' && postCancelPillClass.split(/\s+/).includes('mo__price-state--mixed') && postCancelApiState === 'Mixed',
        'pill 文案/class 正确且服务端 priceState=Mixed', { postCancelPillText, postCancelPillClass, postCancelApiState })
      check('B66-UI-WEB-SHEET-02', 'ui', 'Web 主材下单页无横向溢出且 Console Errors=0',
        orderingMetrics.scrollWidth <= orderingMetrics.innerWidth && orderingMetrics.bodyScrollWidth <= orderingMetrics.innerWidth && orderingErrors.length === 0,
        'scrollWidth<=innerWidth; errors=0', { ...orderingMetrics, consoleErrors: orderingErrors })

      webErrors = []
      await web.goto(`${WEB}/finance/payables?projectId=${PROJECT_ID}&partnerId=${tempPartnerId}`, { waitUntil: 'networkidle' })
      await web.reload({ waitUntil: 'networkidle' })
      await web.waitForTimeout(1000)
      payableErrors = [...webErrors]
      webErrors = []
      const payableBody = await web.locator('body').innerText()
      const payableMetrics = await web.evaluate(() => ({ innerWidth: window.innerWidth, scrollWidth: document.documentElement.scrollWidth, bodyScrollWidth: document.body.scrollWidth }))
      const payableRow = web.locator('tr', { hasText: tempName }).first()
      const rowCount = await payableRow.count()
      const rowText = rowCount ? await payableRow.innerText() : ''
      const payButton = rowCount ? payableRow.getByRole('button', { name: /安排付款/ }).first() : null
      const payButtonCount = payButton ? await payButton.count() : 0
      const payButtonDisabled = payButtonCount ? await payButton.isDisabled() : false
      await web.screenshot({ path: path.join(SHOTS, 'b66-web-payables-estimated-1440x900.png') })
      check('B66-UI-WEB-PAY-01', 'ui', 'Web 应付页暂估行显示“待结算”且不显示 ¥0.00', rowCount > 0 && /待结算/.test(rowText) && !/¥0\.00/.test(rowText),
        '目标行含待结算，不含 ¥0.00', { rowCount, rowText })
      check('B66-UI-WEB-PAY-02', 'ui', 'Web 暂估应付“安排付款”按钮禁用', payButtonCount > 0 && payButtonDisabled,
        '安排付款 disabled', { payButtonCount, payButtonDisabled })
      check('B66-UI-WEB-PAY-03', 'ui', 'Web 应付页无横向溢出且 Console Errors=0',
        payableMetrics.scrollWidth <= payableMetrics.innerWidth && payableMetrics.bodyScrollWidth <= payableMetrics.innerWidth && payableErrors.length === 0,
        'scrollWidth<=innerWidth; errors=0', { ...payableMetrics, consoleErrors: payableErrors })
      uiMetrics.web = { ordering: { ...orderingMetrics, consoleErrors: orderingErrors, mixedTextVisible: webMixedTextVisible, rowText: orderingRowText, pillText: postCancelPillText, pillClass: postCancelPillClass, apiState: postCancelApiState, bodyExcerpt: orderingBody.slice(-500) }, payables: { ...payableMetrics, consoleErrors: payableErrors, rowCount, rowText, payButtonDisabled } }
      await webContext.close()

      const appContext = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'zh-CN' })
      const app = await appContext.newPage()
      let appErrors = []
      let appSheetErrors = []
      let appPayErrors = []
      app.on('console', msg => { if (msg.type() === 'error') appErrors.push(msg.text()) })
      await app.goto(`${APP}/#/pages/auth/login?devToken=${encodeURIComponent(tokens.Finance)}`, { waitUntil: 'networkidle' })
      await app.waitForTimeout(1200)
      await app.goto(`${APP}/#/subpackages/project/pages/material-sheet-detail?sheetId=${main.sheetId}`, { waitUntil: 'domcontentloaded' })
      await app.waitForTimeout(1600)
      appErrors = []
      appSheetErrors = [...appErrors]
      const sheetBody = await app.locator('body').innerText()
      const sheetMetrics = await app.evaluate(() => ({ innerWidth: window.innerWidth, innerHeight: window.innerHeight, scrollWidth: document.documentElement.scrollWidth, bodyScrollWidth: document.body.scrollWidth, scrollHeight: document.documentElement.scrollHeight, domNodeCount: document.querySelectorAll('*').length, htmlLength: document.documentElement.outerHTML.length }))
      await app.screenshot({ path: path.join(SHOTS, 'b66-app-sheet-mixed-390x844.png') })
      check('B66-UI-APP-SHEET-01', 'ui', 'App H5 作业单页展示 Mixed/部分已结算价格状态',
        /混合|部分已结算|部分已定价/.test(sheetBody),
        '含 Mixed/部分已结算', { excerpt: sheetBody.slice(0, 900) })
      check('B66-UI-APP-SHEET-02', 'ui', 'App 作业单页无横向溢出且 Console Errors=0',
        sheetMetrics.scrollWidth <= sheetMetrics.innerWidth && sheetMetrics.bodyScrollWidth <= sheetMetrics.innerWidth && appSheetErrors.length === 0,
        'scrollWidth<=innerWidth; errors=0', { ...sheetMetrics, consoleErrors: appSheetErrors })

      appErrors = []
      await app.goto(`${APP}/#/subpackages/finance/pages/payables?projectId=${PROJECT_ID}`, { waitUntil: 'domcontentloaded' })
      await app.waitForTimeout(1400)
      appPayErrors = [...appErrors]
      appErrors = []
      const appPayBody = await app.locator('body').innerText()
      const appPayMetrics = await app.evaluate(() => ({ innerWidth: window.innerWidth, innerHeight: window.innerHeight, scrollWidth: document.documentElement.scrollWidth, bodyScrollWidth: document.body.scrollWidth, scrollHeight: document.documentElement.scrollHeight, domNodeCount: document.querySelectorAll('*').length, htmlLength: document.documentElement.outerHTML.length }))
      const appButtons = await app.locator('button').allInnerTexts().catch(() => [])
      const appHasPaymentButton = appButtons.some(text => /安排付款|确认付款/.test(text))
      await app.screenshot({ path: path.join(SHOTS, 'b66-app-payables-estimated-390x844.png') })
      check('B66-UI-APP-PAY-01', 'ui', 'App 应付页暂估行显示待定价/待结算且无提前付款按钮',
        /待定价|待结算/.test(appPayBody) && !appHasPaymentButton,
        '待定价/待结算且无付款按钮', { containsEstimated: /待定价|待结算/.test(appPayBody), buttons: appButtons })
      check('B66-UI-APP-PAY-02', 'ui', 'App 应付页无横向溢出且 Console Errors=0',
        appPayMetrics.scrollWidth <= appPayMetrics.innerWidth && appPayMetrics.bodyScrollWidth <= appPayMetrics.innerWidth && appPayErrors.length === 0,
        'scrollWidth<=innerWidth; errors=0', { ...appPayMetrics, consoleErrors: appPayErrors })

      appErrors = []
      await app.goto(`${APP}/#/subpackages/supply/pages/material-batch-order`, { waitUntil: 'domcontentloaded' })
      await app.waitForTimeout(1000)
      const oldBody = (await app.locator('body').innerText()).trim()
      const oldPath = app.url()
      const oldMetrics = await app.evaluate(() => ({ innerWidth: window.innerWidth, scrollWidth: document.documentElement.scrollWidth }))
      await app.screenshot({ path: path.join(SHOTS, 'b66-app-old-route-unreachable-390x844.png') })
      check('B66-UI-APP-OLD', 'ui', 'App 旧直采路由不可达/空白', oldBody.length === 0 && oldMetrics.scrollWidth <= oldMetrics.innerWidth,
        'bodyText 空且无横向溢出', { path: oldPath, bodyText: oldBody, ...oldMetrics })
      uiMetrics.app = { sheet: { ...sheetMetrics, consoleErrors: appSheetErrors, excerpt: sheetBody.slice(0, 600) }, payables: { ...appPayMetrics, consoleErrors: appPayErrors, hasPaymentButton: appHasPaymentButton, excerpt: appPayBody.slice(0, 900) }, oldRoute: { path: oldPath, bodyText: oldBody, ...oldMetrics } }
      await appContext.close()
    } finally {
      await browser.close()
    }

    apiSamples.main = {
      sheetId: main.sheetId,
      itemId: main.itemId,
      suppliers: { existingSupplierId, tempPartnerId },
      projectionAfterOrder: sheetProjection(await sheetInfo(tokens.Finance, main.sheetId)),
      projectionAfterCancel: sheetProjection(await sheetInfo(tokens.Finance, main.sheetId)),
      settlementId: monthlyConfirm.data.settlementId,
      totalAmount: monthlyConfirm.data.totalAmount,
      purchaseIds, itemsAfterCancel, payablesAfterCancel
    }

  } catch (error) {
    results.push({ id: 'B66-FATAL', category: 'runtime', title: '验证执行异常', expected: '无异常', actual: error instanceof Error ? error.message : String(error), result: 'BLOCKED', evidence: {} })
  } finally {
    try { await cleanupBusiness() } catch (error) { results.push({ id: 'B66-CLEANUP-ERROR', category: 'cleanup', title: '业务数据清理异常', expected: 'cleanup ok', actual: error instanceof Error ? error.message : String(error), result: 'FAIL', evidence: {} }) }
    try {
      if (tempPartnerId) sql(`DELETE FROM decoboon.db_partner_brand WHERE partner_id=${tempPartnerId}`)
      if (tempPartnerId) sql(`DELETE FROM decoboon.db_partner WHERE partner_id=${tempPartnerId}`)
    } catch (error) { results.push({ id: 'B66-CLEANUP-PARTNER', category: 'cleanup', title: '临时供应商清理异常', expected: 'delete ok', actual: error instanceof Error ? error.message : String(error), result: 'FAIL', evidence: {} }) }
    try { restorePlanOriginal() } catch (error) { results.push({ id: 'B66-CLEANUP-PLAN', category: 'cleanup', title: '材料计划恢复异常', expected: 'restore ok', actual: error instanceof Error ? error.message : String(error), result: 'FAIL', evidence: {} }) }
    try {
      if (projectOriginalManager) sql(`UPDATE decoboon.db_project SET manager_id=${projectOriginalManager === 'NULL' ? 'NULL' : projectOriginalManager} WHERE project_id=${PROJECT_ID}`)
    } catch (error) { results.push({ id: 'B66-CLEANUP-MANAGER', category: 'cleanup', title: '项目经理恢复异常', expected: 'restore ok', actual: error instanceof Error ? error.message : String(error), result: 'FAIL', evidence: {} }) }

    try {
      const finalTables = Object.fromEntries(baselineTables.map(table => [table, Number(sqlOne(`SELECT COUNT(*) FROM decoboon.${table}`))]))
      const restoredPlanRow = sqlOne(`SELECT status,quantity,net_quantity,loss_rate,loss_quantity,unit_price,cost,COALESCE(selection_id,'NULL'),COALESCE(DATE_FORMAT(arrived_at,'%Y-%m-%d'),'NULL') FROM decoboon.db_material_plan WHERE plan_id=${PLAN_ID}`)
      const finalPartnerCount = Number(sqlOne('SELECT COUNT(*) FROM decoboon.db_partner'))
      check('B66-CLEANUP-01', 'cleanup', '临时作业单/采购/到场/结算/应付清理完成', Object.entries(baseline.tables).filter(([k]) => !['db_partner','db_finance_payable_settlement'].includes(k)).every(([table, count]) => finalTables[table] === count), '业务表计数回到基线', { baseline: baseline.tables, final: finalTables })
      check('B66-CLEANUP-02', 'cleanup', '临时供应商清理完成', finalPartnerCount === baseline.partners, 'partner count 回到基线', { baseline: baseline.partners, final: finalPartnerCount })
      check('B66-CLEANUP-03', 'cleanup', '材料计划逐列恢复 ReadyToOrder 与原始数量/价格/成本', restoredPlanRow === [planOriginal.status,planOriginal.quantity,planOriginal.net_quantity,planOriginal.loss_rate,planOriginal.loss_quantity,planOriginal.unit_price,planOriginal.cost,planOriginal.selection_id,planOriginal.arrived_at].join('\t'), '逐列等于基线快照', { baseline: planOriginal, final: restoredPlanRow })
      const finalManager = sqlOne(`SELECT COALESCE(manager_id,'NULL') FROM decoboon.db_project WHERE project_id=${PROJECT_ID}`)
      check('B66-CLEANUP-04', 'cleanup', '项目经理恢复原负责人', finalManager === projectOriginalManager, 'manager_id 回到基线', { baseline: projectOriginalManager, final: finalManager })
      apiSamples.cleanup = { baseline: baseline.tables, final: finalTables, partnerCount: { baseline: baseline.partners, final: finalPartnerCount }, planBaseline: planOriginal, planFinal: restoredPlanRow, managerBaseline: projectOriginalManager, managerFinal: finalManager }
    } catch (error) {
      results.push({ id: 'B66-CLEANUP-VERIFY', category: 'cleanup', title: '清理校验异常', expected: 'verify ok', actual: error instanceof Error ? error.message : String(error), result: 'FAIL', evidence: {} })
    }
  }

  const categoryMap = new Map()
  for (const row of results) {
    if (!categoryMap.has(row.category)) categoryMap.set(row.category, { pass: 0, fail: 0, blocked: 0, total: 0 })
    const category = categoryMap.get(row.category)
    category.total++
    if (row.result === 'PASS') category.pass++
    else if (row.result === 'FAIL') category.fail++
    else category.blocked++
  }
  const byCategory = Object.fromEntries([...categoryMap.entries()].map(([key, value]) => [key, value]))
  const totals = Object.values(byCategory).reduce((acc, item) => ({
    total: acc.total + item.total, pass: acc.pass + item.pass, fail: acc.fail + item.fail, blocked: acc.blocked + item.blocked
  }), { total: 0, pass: 0, fail: 0, blocked: 0 })
  const defects = results.filter(row => row.result !== 'PASS').map(row => ({
    id: row.id, category: row.category, title: row.title, expected: row.expected, actual: row.actual, evidence: row.evidence, result: row.result
  }))
  const conclusion = totals.blocked > 0 ? 'BLOCKED' : totals.fail > 0 ? 'FAIL' : 'PASS'
  const focus = results.filter(row => row.category === 'b6.6-focus')
  fs.writeFileSync(path.join(OUT, 'verification-results.json'), JSON.stringify({ batch: 'B6.6', date: TODAY, environment: apiSamples.environment, totals, byCategory, results }, null, 2))
  fs.writeFileSync(path.join(OUT, 'api-samples.json'), JSON.stringify(apiSamples, null, 2))
  fs.writeFileSync(path.join(OUT, 'ui-metrics.json'), JSON.stringify(uiMetrics, null, 2))
  fs.writeFileSync(path.join(OUT, 'summary.json'), JSON.stringify({
    batch: 'B6.6', date: TODAY, conclusion, totals, byCategory,
    focusDefect: { id: 'B62-DEF-001', status: focus.every(row => row.result === 'PASS') ? 'CLOSED' : 'OPEN', checks: focus },
    defects,
    environment: apiSamples.environment,
    evidence: ['README.md', 'verification-results.json', 'api-samples.json', 'ui-metrics.json', 'screenshots/'],
    cleanup: apiSamples.cleanup,
    acceptanceRecommendation: conclusion === 'PASS' ? '满足进入用户最终 ACCEPTED 决策的独立证据条件' : '不满足进入用户最终 ACCEPTED 决策的独立证据条件',
    limitations: [
      'Only one active seeded supplier existed; a second temporary supplier was created and removed for the cross-supplier split.',
      'The single supplied StoreManager account belongs to a single-store tenant; same-tenant cross-store negative was limited to foreign-company/store scope.',
      'Assistant golden-question audit rows are append-only and intentionally retained; no question/answer body is stored by the audit ledger.'
    ]
  }, null, 2))
  console.log(JSON.stringify({ conclusion, totals, byCategory, defects: defects.map(d => ({ id: d.id, result: d.result, title: d.title })), summary: path.join(OUT, 'summary.json') }, null, 2))
}

await captureEvidence()
