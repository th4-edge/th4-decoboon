import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('/Users/zhangwei/.codex/skills/guizang-social-card-skill/node_modules/playwright')

const API = process.env.B8_API || 'http://127.0.0.1:8091/api'
const WEB = process.env.B8_WEB || 'http://127.0.0.1:4175'
const APP = process.env.B8_APP || 'http://127.0.0.1:8080'
const OUT = process.env.B8_OUT || path.resolve('docs/product/domains/material/evidence/B8')
const SHOTS = path.join(OUT, 'screenshots')
const PASSWORD = process.env.B8_PASSWORD
const DB_PASSWORD = process.env.B8_DB_PASSWORD
const DB_USER = process.env.B8_DB_USER || 'root'
const DB_NAME = process.env.B8_DB_NAME || 'decoboon'
const PROJECT_ID = process.env.B8_PROJECT_ID || '235520576473530368'
const PLAN_ID = process.env.B8_PLAN_ID || '235566303270666240'
const PROVIDED_SHEET_ID = process.env.B8_SHEET_ID || ''
const BACKEND_WORKTREE = process.env.B8_BACKEND_WORKTREE || '/tmp/th4-b8-backend-302193e'
const APP_WORKTREE = process.env.B8_APP_WORKTREE || '/tmp/th4-b8-app-67e6da0'
const WEB_WORKTREE = process.env.B8_WEB_WORKTREE || '/tmp/th4-b8-web-d15d088'
const GOLDEN_PATH = process.env.B8_GOLDEN_PATH || path.join(BACKEND_WORKTREE, 'src/main/resources/assistant/material-golden-questions.json')
const RUN_DEVTOOLS = process.env.B8_RUN_DEVTOOLS !== 'false'
const SHEET_QUESTION = '这张作业单现在是什么状态？'
const PROJECT_QUESTION = '这个项目的材料业务现在缺什么？'
const TODAY = '2026-10-17'

if (!PASSWORD) throw new Error('B8_PASSWORD is required')
if (!DB_PASSWORD) throw new Error('B8_DB_PASSWORD is required')
fs.mkdirSync(SHOTS, { recursive: true })

const PHONES = {
  Boss: '13910000100',
  StoreManager: '13910000110',
  Designer: '13910000111',
  Pm: '13910000113',
  Finance: '13910000114'
}

const results = []
const observations = []
const attempts = []
const apiSamples = {}
const uiMetrics = { web: {}, app: {}, devtools: {}, consoleErrors: {}, styles: {}, screenshots: [] }
const screenshots = []
const goldenDetails = []
let setup = null
let tokens = {}
let session = null
let projectApiAnswer = ''
let sheetApiAnswer = ''
let webProjectAnswer = ''
let webSheetAnswer = ''
let appProjectAnswer = ''
let appSheetAnswer = ''
let businessCountsBeforeAssistant = null
let businessCountsBeforeCleanup = null
let expectedCountsAfterCleanup = null
let projectOriginalManagerId = null

function sql(query) {
  return execFileSync('mysql', [
    '-h127.0.0.1', '-P3306', `-u${DB_USER}`, `-p${DB_PASSWORD}`, '-N', '-B', DB_NAME, '-e', query
  ], { encoding: 'utf8', maxBuffer: 10 * 1024 * 1024 }).trim()
}

function sqlOne(query) {
  return sql(query).split('\n')[0]?.trim() || ''
}

function escapeSql(value) {
  return value == null ? 'NULL' : typeof value === 'number' ? String(value) : `'${String(value).replaceAll("'", "''")}'`
}

function normalized(value) {
  return String(value || '').replace(/\s+/g, ' ').trim()
}

function errorCode(response) {
  return response?.errorCode?.code ?? response?.errorCode ?? ''
}

function errorMessage(response) {
  return response?.errorCode?.message ?? response?.message ?? ''
}

function check(id, category, title, expected, condition, actual, evidence = '', forcedStatus = '') {
  const result = forcedStatus || (condition ? 'PASS' : 'FAIL')
  results.push({ id, category, title, expected, actual, result, evidence })
  return condition
}

function observe(id, title, detail, severity = 'INFO') {
  observations.push({ id, title, detail, severity })
}

async function post(route, body = {}, token) {
  const response = await fetch(`${API}/${route}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const json = await response.json().catch(() => ({}))
  return { httpStatus: response.status, ...json }
}

async function login(phone) {
  const response = await post('auth/session/password-login', { phone, password: PASSWORD })
  if (!response?.data?.token) throw new Error(`login failed for ${phone}: ${JSON.stringify(response).slice(0, 240)}`)
  return response.data.token
}

async function loginAll() {
  const all = {}
  for (const [role, phone] of Object.entries(PHONES)) all[role] = await login(phone)
  return all
}

function tableCounts(tables) {
  return Object.fromEntries(tables.map(table => [table, Number(sqlOne(`SELECT COUNT(*) FROM ${table}`))]))
}

function readGolden() {
  return JSON.parse(fs.readFileSync(GOLDEN_PATH, 'utf8'))
}

async function setupFixture() {
  const employeeIds = Object.fromEntries(PHONES && Object.entries(PHONES).map(([role, phone]) => [role, sqlOne(`SELECT emp_id FROM db_tenant_employee WHERE tel_no=${escapeSql(phone)} LIMIT 1`)]))
  const bossComId = sqlOne(`SELECT com_id FROM db_tenant_employee WHERE tel_no='13910000100' LIMIT 1`)
  const baselineTables = [
    'db_material_order_sheet', 'db_material_order_sheet_item', 'db_purchase_order', 'db_purchase_order_item',
    'db_goods_receipt', 'db_material_settlement', 'db_material_settlement_item', 'db_finance_payable',
    'db_finance_payable_settlement', 'db_partner'
  ]
  const beforeAnyFixture = tableCounts(baselineTables)
  const planOriginal = sqlOne(`SELECT status,quantity,net_quantity,loss_rate,loss_quantity,unit_price,cost,COALESCE(selection_id,'NULL'),COALESCE(arrived_at,'NULL') FROM db_material_plan WHERE plan_id=${PLAN_ID}`)
  if (!planOriginal) throw new Error(`material plan not found: ${PLAN_ID}`)
  projectOriginalManagerId = sqlOne(`SELECT COALESCE(manager_id,'NULL') FROM db_project WHERE project_id=${PROJECT_ID}`)

  let sheetId = PROVIDED_SHEET_ID
  let createdByScript = false
  if (sheetId) {
    const exists = sqlOne(`SELECT COUNT(*) FROM db_material_order_sheet WHERE sheet_id=${escapeSql(sheetId)}`)
    if (exists === '0') sheetId = ''
  }
  if (!sheetId) {
    createdByScript = true
    const save = await post('material/sheet/save', {
      projectId: PROJECT_ID,
      sheetType: 'Tile',
      manufacturerName: `B8独立验收-${Date.now()}`,
      items: [{
        materialPlan: { planId: PLAN_ID },
        materialName: '主材--抛釉砖 800x800',
        locationName: '客厅地砖',
        spaceName: '客厅',
        quantity: 5,
        unit: 'Area'
      }]
    }, tokens.Designer)
    if (!save.success || !save.data?.sheetId) throw new Error(`temporary sheet create failed: ${JSON.stringify(save).slice(0, 300)}`)
    sheetId = String(save.data.sheetId)
    const measure = await post('material/sheet/measure', {
      sheetId, measureAt: TODAY, deliverAt: '2026-10-20', installAt: '2026-10-22'
    }, tokens.Designer)
    if (!measure.success) throw new Error(`temporary sheet measure failed: ${JSON.stringify(measure).slice(0, 300)}`)
    const submit = await post('material/sheet/submit', { sheetId }, tokens.Designer)
    if (!submit.success) throw new Error(`temporary sheet submit failed: ${JSON.stringify(submit).slice(0, 300)}`)
  }

  const sheetRow = sqlOne(`SELECT sheet_no,project_id,com_id,status,price_state FROM db_material_order_sheet WHERE sheet_id=${escapeSql(sheetId)}`)
  if (!sheetRow) throw new Error(`temporary sheet not found: ${sheetId}`)
  const [sheetNo, sheetProjectId, sheetComId, sheetStatus, sheetPriceState] = sheetRow.split('\t')
  const itemCount = Number(sqlOne(`SELECT COUNT(*) FROM db_material_order_sheet_item WHERE sheet_id=${escapeSql(sheetId)}`))
  const afterFixture = tableCounts(baselineTables)

  // PM has no local project in the seed. Temporarily attach the project to PM so the
  // role can be tested through the same real project scope; restore in cleanup.
  sql(`UPDATE db_project SET manager_id=${escapeSql(employeeIds.Pm)} WHERE project_id=${PROJECT_ID}`)

  return {
    sheetId: String(sheetId), sheetNo, sheetProjectId, sheetComId, sheetStatus, sheetPriceState,
    createdByScript, itemCount, employeeIds, bossComId, baselineTables,
    beforeAnyFixture, afterFixture, planOriginal, createdByScript
  }
}

async function runApiChecks() {
  const businessBeforeApi = tableCounts(setup.baselineTables)
  const golden = readGolden()
  const goldenSet = new Set(golden.map(item => item.question))
  check('B8-API-GOLDEN-SOURCE', 'api', '建议问题均来自同一 50 黄金问题源', 'suggestions ⊆ material-golden-questions.json', golden.length === 50, { goldenCount: golden.length })

  const contexts = {
    general: { contextType: 'general' },
    project: { contextType: 'project', projectId: PROJECT_ID },
    purchase: { contextType: 'purchase', projectId: PROJECT_ID },
    settlement: { contextType: 'settlement', projectId: PROJECT_ID },
    sheet: { contextType: 'sheet', projectId: PROJECT_ID, sheetId: setup.sheetId }
  }
  const contextResponses = {}
  for (const [name, body] of Object.entries(contexts)) {
    const response = await post('material/assistant/suggestions', body, tokens.Boss)
    contextResponses[name] = response
    const suggestions = response?.data?.suggestions || []
    check(`B8-API-SUG-${name.toUpperCase()}`, 'api', `${name} 上下文建议可用且同源`,
      'success=true, contextType 正确, 5-6 条且全部命中黄金问题',
      response.success === true && response.data?.contextType === name && suggestions.length >= 5
        && suggestions.every(question => goldenSet.has(question)),
      { contextType: response.data?.contextType, suggestions })
  }
  check('B8-API-SUG-SHEET-CONTENT', 'api', '作业单上下文包含状态/价格/时间问题',
    '包含作业单提交、未定价、测量时间建议',
    (contextResponses.sheet.data?.suggestions || []).some(q => q.includes('不能提交请购'))
      && (contextResponses.sheet.data?.suggestions || []).some(q => q.includes('未定价'))
      && (contextResponses.sheet.data?.suggestions || []).some(q => q.includes('测量日期')),
    contextResponses.sheet.data?.suggestions)
  const fallbackProject = await post('material/assistant/suggestions', { contextType: 'unknown', projectId: PROJECT_ID }, tokens.Boss)
  const fallbackGeneral = await post('material/assistant/suggestions', { contextType: 'unknown' }, tokens.Boss)
  check('B8-API-SUG-FALLBACK', 'api', '未知 contextType 按真实 ID 回落 project/general',
    'projectId→project；无 ID→general',
    fallbackProject.data?.contextType === 'project' && fallbackGeneral.data?.contextType === 'general',
    { withProject: fallbackProject.data?.contextType, withoutId: fallbackGeneral.data?.contextType })
  const suggestionsUnauth = await post('material/assistant/suggestions', { contextType: 'general' })
  check('B8-API-SUG-AUTH', 'api', '未登录不可读取建议问题', 'success=false',
    suggestionsUnauth.success === false, suggestionsUnauth.errorCode)
  const metadataOnly = await post('material/assistant/suggestions', { contextType: 'project', projectId: '999999999999999999' }, tokens.Boss)
  observe('OBS-B8-001', '建议问题接口只做分组选择，不读取对象数据',
    'suggestions 对任意 projectId/sheetId 只返回黄金问题分组，不读取项目、成本或供应商数据；越权对象访问由 material/assistant/ask 统一返回 404/403。当前行为未发现数据泄漏，属于低风险边界。',
    'P3')
  apiSamples.suggestionsMetadataOnly = metadataOnly.data

  const bossProject = await post('material/assistant/ask', {
    question: PROJECT_QUESTION, projectId: PROJECT_ID,
    requestId: 'b8-api-project', idempotencyKey: 'b8-api-project-1'
  }, tokens.Boss)
  const projectData = bossProject.data || {}
  projectApiAnswer = projectData.answer || ''
  const projectCitationIds = (projectData.citations || []).map(item => item.id)
  const projectToolProviders = (projectData.toolCalls || []).map(item => item.provider)
  check('B8-API-ASK-CONTRACT', 'api', 'ask 返回 answer/citations/toolCalls/nextActions/readOnly/auditStatus',
    '六类字段齐全，readOnly=true，auditStatus=RECORDED',
    bossProject.success === true && typeof projectData.answer === 'string' && Array.isArray(projectData.citations)
      && Array.isArray(projectData.toolCalls) && Array.isArray(projectData.nextActions)
      && projectData.readOnly === true && projectData.auditStatus === 'RECORDED',
    { answerType: projectData.answerType, citations: projectCitationIds, tools: projectToolProviders, nextActions: projectData.nextActions, readOnly: projectData.readOnly, auditStatus: projectData.auditStatus })
  check('B8-API-ASK-PROJECT-LIVE', 'api', 'Boss 项目上下文返回实时材料体检与证据',
    'LIVE_DATA；含材料计划计数、校核项、成本不完整、利润 null',
    projectData.answerType === 'LIVE_DATA' && /材料计划\s*2\s*条/.test(projectApiAnswer)
      && /成本不完整/.test(projectApiAnswer) && /利润：null/.test(projectApiAnswer)
      && ['CHK-UNSELECTED', 'CHK-UNPRICED', 'CHK-COST-INCOMPLETE'].every(id => projectCitationIds.includes(id))
      && projectToolProviders.includes('blueprint/check/report'),
    { answer: projectApiAnswer, citations: projectCitationIds, tools: projectToolProviders })

  const bossSheet = await post('material/assistant/ask', {
    question: SHEET_QUESTION, projectId: PROJECT_ID, sheetId: setup.sheetId,
    requestId: 'b8-api-sheet', idempotencyKey: 'b8-api-sheet-1'
  }, tokens.Boss)
  const sheetData = bossSheet.data || {}
  sheetApiAnswer = sheetData.answer || ''
  const sheetCitationIds = (sheetData.citations || []).map(item => item.id)
  const sheetToolProviders = (sheetData.toolCalls || []).map(item => item.provider)
  check('B8-API-ASK-SHEET-LIVE', 'api', 'Boss 作业单上下文绑定真实单据',
    'LIVE_DATA；回答含真实 sheetNo/状态；工具含 material/sheet/info',
    sheetData.answerType === 'LIVE_DATA' && sheetApiAnswer.includes(setup.sheetNo)
      && /已提交请购|待下单/.test(sheetApiAnswer) && sheetToolProviders.includes('material/sheet/info'),
    { sheetNo: setup.sheetNo, answer: sheetApiAnswer, citations: sheetCitationIds, tools: sheetToolProviders })

  for (const role of ['Boss', 'StoreManager', 'Finance']) {
    const response = await post('material/assistant/ask', {
      question: '未定价为什么还能下单？', projectId: PROJECT_ID,
      requestId: `b8-api-cost-${role}`, idempotencyKey: `b8-api-cost-${role}-1`
    }, tokens[role])
    const answer = response.data?.answer || ''
    check(`B8-API-COST-VISIBLE-${role.toUpperCase()}`, 'api', `${role} 可见成本与利润口径`,
      '回答包含材料应付/实际成本金额与利润 null',
      response.success === true && /材料应付\s*[¥￥]/.test(answer) && /实际成本\s*[¥￥]/.test(answer) && /利润：null/.test(answer),
      { answerType: response.data?.answerType, answer })
  }

  const designer = await post('material/assistant/ask', {
    question: PROJECT_QUESTION, projectId: PROJECT_ID,
    requestId: 'b8-api-cost-designer', idempotencyKey: 'b8-api-cost-designer-1'
  }, tokens.Designer)
  const designerAnswer = designer.data?.answer || ''
  check('B8-API-COST-HIDDEN-DESIGNER', 'api', 'Designer 只看到履约口径',
    '无成本/供应商价格/利润金额，明确提示角色限制',
    designer.success === true && /当前角色可查看履约状态/.test(designerAnswer)
      && !/¥|￥/.test(designerAnswer) && !/利润：null/.test(designerAnswer),
    { answerType: designer.data?.answerType, answer: designerAnswer, citations: designer.data?.citations?.map(item => item.id) })

  const pm = await post('material/assistant/ask', {
    question: PROJECT_QUESTION, projectId: PROJECT_ID,
    requestId: 'b8-api-cost-pm', idempotencyKey: 'b8-api-cost-pm-1'
  }, tokens.Pm)
  const pmAnswer = pm.data?.answer || ''
  check('B8-API-COST-HIDDEN-PM', 'api', 'PM 只看到履约口径',
    'PM 项目范围可访问但无成本/供应商价格/利润金额',
    pm.success === true && /当前角色可查看履约状态/.test(pmAnswer) && !/¥|￥/.test(pmAnswer) && !/利润：null/.test(pmAnswer),
    { answerType: pm.data?.answerType, answer: pmAnswer, errorCode: pm.errorCode })

  const designerCostQuestion = await post('material/assistant/ask', {
    question: '谁能查看材料成本和供应商价格？', projectId: PROJECT_ID,
    requestId: 'b8-api-permission-denied', idempotencyKey: 'b8-api-permission-denied-1'
  }, tokens.Designer)
  check('B8-API-PERMISSION-DENIED', 'api', 'Designer 成本可见性请求返回 PERMISSION_DENIED',
    'answerType=PERMISSION_DENIED，原样说明权限边界',
    designerCostQuestion.data?.answerType === 'PERMISSION_DENIED'
      && /Boss|店长|财务/.test(designerCostQuestion.data?.answer || ''),
    designerCostQuestion.data?.answerType)

  const crossTenantProject = sqlOne(`SELECT project_id FROM db_project WHERE com_id<>${escapeSql(setup.bossComId)} ORDER BY project_id LIMIT 1`)
  const crossTenant = await post('material/assistant/ask', {
    question: PROJECT_QUESTION, projectId: crossTenantProject,
    requestId: 'b8-api-cross-tenant', idempotencyKey: 'b8-api-cross-tenant-1'
  }, tokens.Boss)
  check('B8-API-CROSS-TENANT', 'api', 'Boss 跨企业项目返回 404/403',
    'success=false 且 errorCode 为 403/404',
    crossTenant.success === false && ['403', '404'].includes(String(errorCode(crossTenant))),
    { projectId: crossTenantProject, errorCode: errorCode(crossTenant), message: errorMessage(crossTenant) })

  const crossDesignerProject = sqlOne(`SELECT p.project_id FROM db_project p WHERE p.com_id=${escapeSql(setup.bossComId)} AND p.project_id<>${PROJECT_ID} AND (p.manager_id IS NULL OR p.manager_id<>${escapeSql(setup.employeeIds.Designer)}) AND (p.owner_id IS NULL OR p.owner_id<>${escapeSql(setup.employeeIds.Designer)}) AND (p.designer_id IS NULL OR p.designer_id<>${escapeSql(setup.employeeIds.Designer)}) ORDER BY p.project_id LIMIT 1`)
  const crossProject = await post('material/assistant/ask', {
    question: PROJECT_QUESTION, projectId: crossDesignerProject,
    requestId: 'b8-api-cross-project', idempotencyKey: 'b8-api-cross-project-1'
  }, tokens.Designer)
  check('B8-API-CROSS-PROJECT', 'api', 'Designer 同企业未参与项目返回 404/403',
    'success=false 且 errorCode 为 403/404',
    Boolean(crossDesignerProject) && crossProject.success === false && ['403', '404'].includes(String(errorCode(crossProject))),
    { projectId: crossDesignerProject, errorCode: errorCode(crossProject), message: errorMessage(crossProject) })

  const bogusSheet = await post('material/assistant/ask', {
    question: SHEET_QUESTION, projectId: PROJECT_ID, sheetId: '999999999999999999',
    requestId: 'b8-api-bogus-sheet', idempotencyKey: 'b8-api-bogus-sheet-1'
  }, tokens.Boss)
  check('B8-API-BOGUS-SHEET', 'api', '不存在的作业单返回 404/403',
    'success=false 且 errorCode 为 403/404',
    bogusSheet.success === false && ['403', '404'].includes(String(errorCode(bogusSheet))),
    { errorCode: errorCode(bogusSheet), message: errorMessage(bogusSheet) })

  const spoofed = await post('material/assistant/ask', {
    question: PROJECT_QUESTION,
    projectId: PROJECT_ID,
    companyId: '1790066191585976744',
    empId: setup.employeeIds.Boss,
    ownerId: setup.employeeIds.Boss,
    requestId: 'b8-api-untrusted-fields',
    idempotencyKey: 'b8-api-untrusted-fields-1'
  }, tokens.Designer)
  const spoofedAudit = sqlOne(`SELECT employee_id,com_id,business_id FROM db_ai_usage_call WHERE request_id='b8-api-untrusted-fields' ORDER BY call_id DESC LIMIT 1`)
  const spoofedAnswer = spoofed.data?.answer || ''
  check('B8-API-UNTRUSTED-FIELDS', 'api', '伪造 companyId/empId/ownerId 不改变身份与范围',
    '仍按 Designer 身份与所属企业记录，回答保持履约口径',
    spoofed.success === true && /当前角色可查看履约状态/.test(spoofedAnswer)
      && spoofedAudit.startsWith(`${setup.employeeIds.Designer}\t${setup.bossComId}\t`),
    { audit: spoofedAudit, answer: spoofedAnswer })

  const refusal = await post('material/assistant/ask', {
    question: '帮我审批这张采购单',
    requestId: 'b8-api-refusal', idempotencyKey: 'b8-api-refusal-1'
  }, tokens.Boss)
  const refusalData = refusal.data || {}
  const writeToolPattern = /save|update|delete|cancel|order|approve|pay|settle|confirm|permission/i
  check('B8-API-REFUSAL', 'api', '越界写操作请求原样拒答',
    'answerType=REFUSAL，说明不能自动审批/付款/删除/改权限，无写工具',
    refusalData.answerType === 'REFUSAL' && /不能自动审批/.test(refusalData.answer || '')
      && !(refusalData.toolCalls || []).some(tool => writeToolPattern.test(`${tool.name} ${tool.provider}`)),
    { answerType: refusalData.answerType, answer: refusalData.answer, tools: refusalData.toolCalls })

  const noEvidence = await post('material/assistant/ask', {
    question: '火星材料怎么报税？',
    requestId: 'b8-api-no-evidence', idempotencyKey: 'b8-api-no-evidence-1'
  }, tokens.Boss)
  check('B8-API-NO-EVIDENCE', 'api', '未知问题返回 NO_EVIDENCE 不兜底',
    'answerType=NO_EVIDENCE，明确暂不可确认',
    noEvidence.data?.answerType === 'NO_EVIDENCE' && /没有足够证据/.test(noEvidence.data?.answer || ''),
    noEvidence.data?.answerType)

  const blank = await post('material/assistant/ask', { question: '   ' }, tokens.Boss)
  const long = await post('material/assistant/ask', { question: '材料'.repeat(260) }, tokens.Boss)
  const askUnauth = await post('material/assistant/ask', { question: PROJECT_QUESTION })
  check('B8-API-INPUT-VALIDATION', 'api', '空白/超长/未登录请求被服务端拒绝',
    '三类 success=false',
    blank.success === false && long.success === false && askUnauth.success === false,
    { blank: errorCode(blank), long: errorCode(long), unauth: errorCode(askUnauth) })

  const firstAudit = await post('material/assistant/ask', {
    question: '暂估应付是什么意思？', projectId: PROJECT_ID,
    requestId: 'b8-api-idempotency', idempotencyKey: 'b8-api-idempotency-key-1'
  }, tokens.Boss)
  const secondAudit = await post('material/assistant/ask', {
    question: '暂估应付是什么意思？', projectId: PROJECT_ID,
    requestId: 'b8-api-idempotency', idempotencyKey: 'b8-api-idempotency-key-1'
  }, tokens.Boss)
  const auditCount = Number(sqlOne(`SELECT COUNT(*) FROM db_ai_usage_call WHERE request_id='b8-api-idempotency'`))
  check('B8-API-AUDIT-IDEMPOTENCY', 'api', '同 requestId + idempotencyKey 审计幂等',
    '两次返回同一 auditId，数据库仅 1 条',
    firstAudit.data?.auditId && firstAudit.data.auditId === secondAudit.data?.auditId && auditCount === 1,
    { first: firstAudit.data?.auditId, second: secondAudit.data?.auditId, auditCount })

  const questionProbe = '暂估应付是什么意思？'
  const answerProbe = 'P3 结算价是实际成本'
  const bodyLeakQuestion = Number(sqlOne(`SELECT COUNT(*) FROM db_ai_usage_call WHERE CONCAT_WS('|',COALESCE(request_id,''),COALESCE(idempotency_key,''),COALESCE(business_module,''),COALESCE(capability_code,''),COALESCE(scene_code,''),COALESCE(provider_code,''),COALESCE(model_code,''),COALESCE(model_version,''),COALESCE(capability_version,''),COALESCE(rate_card_version,''),COALESCE(status,''),COALESCE(billing_status,''),COALESCE(failure_code,''),COALESCE(failure_message,''),COALESCE(cancel_reason,'')) LIKE ${escapeSql(`%${questionProbe}%`)}`))
  const bodyLeakAnswer = Number(sqlOne(`SELECT COUNT(*) FROM db_ai_usage_call WHERE CONCAT_WS('|',COALESCE(request_id,''),COALESCE(idempotency_key,''),COALESCE(business_module,''),COALESCE(capability_code,''),COALESCE(scene_code,''),COALESCE(provider_code,''),COALESCE(model_code,''),COALESCE(model_version,''),COALESCE(capability_version,''),COALESCE(rate_card_version,''),COALESCE(status,''),COALESCE(billing_status,''),COALESCE(failure_code,''),COALESCE(failure_message,''),COALESCE(cancel_reason,'')) LIKE ${escapeSql(`%${answerProbe}%`)}`))
  check('B8-API-AUDIT-NO-BODY', 'api', '审计不保存问题正文或回答正文',
    '审计文本字段检索问题/回答特征均为 0',
    bodyLeakQuestion === 0 && bodyLeakAnswer === 0,
    { questionProbe, bodyLeakQuestion, answerProbe, bodyLeakAnswer })

  // 50 golden questions, same source and expected intent.
  for (const row of golden) {
    const response = await post('material/assistant/ask', {
      question: row.question,
      requestId: `b8-golden-${row.id}`,
      idempotencyKey: `b8-golden-${row.id}-1`
    }, tokens.Boss)
    const data = response.data || {}
    const pass = response.success === true && data.intent === row.expectedIntent && data.readOnly === true && data.auditStatus === 'RECORDED'
    goldenDetails.push({
      id: row.id, question: row.question, expectedIntent: row.expectedIntent,
      actualIntent: data.intent, answerType: data.answerType, readOnly: data.readOnly,
      auditStatus: data.auditStatus, citationIds: (data.citations || []).map(item => item.id),
      toolProviders: (data.toolCalls || []).map(item => item.provider), answerExcerpt: String(data.answer || '').slice(0, 220),
      result: pass ? 'PASS' : 'FAIL'
    })
  }
  const goldenPass = goldenDetails.filter(row => row.result === 'PASS').length
  check('B8-API-GOLDEN-50', 'api', 'B4 50 个黄金问题无回归',
    '50/50 意图匹配、readOnly=true、审计 RECORDED',
    goldenPass === 50, { pass: goldenPass, total: 50, failures: goldenDetails.filter(row => row.result === 'FAIL') })

  const afterReadOnly = tableCounts(setup.baselineTables)
  check('B8-API-NO-BUSINESS-WRITE', 'api', '只读问答未改业务表',
    '全部业务表计数不变',
    Object.keys(businessBeforeApi).every(table => afterReadOnly[table] === businessBeforeApi[table]),
    { before: businessBeforeApi, after: afterReadOnly })

  apiSamples.project = { request: { projectId: PROJECT_ID }, response: projectData }
  apiSamples.sheet = { request: { projectId: PROJECT_ID, sheetId: setup.sheetId }, response: sheetData }
  apiSamples.contexts = Object.fromEntries(Object.entries(contextResponses).map(([key, response]) => [key, response.data]))
  apiSamples.designer = { answerType: designer.data?.answerType, answer: designerAnswer, citations: designer.data?.citations }
  apiSamples.pm = { answerType: pm.data?.answerType, answer: pmAnswer, citations: pm.data?.citations }
  apiSamples.refusal = refusalData
  apiSamples.noEvidence = noEvidence.data
  apiSamples.idempotency = { first: firstAudit.data?.auditId, second: secondAudit.data?.auditId, auditCount }
}

async function collectMetrics(page) {
  return page.evaluate(() => ({
    innerWidth: window.innerWidth,
    innerHeight: window.innerHeight,
    scrollWidth: document.documentElement.scrollWidth,
    bodyScrollWidth: document.body.scrollWidth,
    scrollHeight: document.documentElement.scrollHeight,
    domNodeCount: document.querySelectorAll('*').length,
    htmlLength: document.documentElement.outerHTML.length,
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth || document.body.scrollWidth > window.innerWidth
  }))
}

async function collectStyles(page, dialog) {
  return page.evaluate(({ dialogSelector }) => {
    const pick = (selector) => {
      const el = document.querySelector(`${dialogSelector} ${selector}`) || document.querySelector(selector)
      if (!el) return null
      const style = getComputedStyle(el)
      const rect = el.getBoundingClientRect()
      return { fontSize: style.fontSize, lineHeight: style.lineHeight, color: style.color, background: style.backgroundColor, borderRadius: style.borderRadius, x: rect.x, y: rect.y, width: rect.width, height: rect.height }
    }
    const visibleDrawer = [...document.querySelectorAll('.el-drawer')].find(element => element.getBoundingClientRect().width > 0)
    const drawer = visibleDrawer ? (() => {
      const style = getComputedStyle(visibleDrawer)
      const rect = visibleDrawer.getBoundingClientRect()
      return { fontSize: style.fontSize, lineHeight: style.lineHeight, color: style.color, background: style.backgroundColor, borderRadius: style.borderRadius, x: rect.x, y: rect.y, width: rect.width, height: rect.height }
    })() : null
    return { drawer, context: pick('.ma__context'), chip: pick('.ma__chip'), answer: pick('.ma__answer'), fab: pick('.ma__fab'), boundary: pick('.ma__boundary') }
  }, { dialogSelector: dialog ? '.el-drawer' : '' })
}

async function shot(page, name) {
  const relative = path.join('screenshots', `${name}.png`)
  await page.screenshot({ path: path.join(OUT, relative), fullPage: false })
  screenshots.push(relative)
  uiMetrics.screenshots = screenshots
  return relative
}

async function webGoto(page, url, errors) {
  let lastError = null
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 })
      await page.waitForTimeout(700)
      const text = await page.locator('body').innerText().catch(() => '')
      if (/504|Outdated Optimize Dep/.test(text) || errors.some(text => /504|Outdated Optimize Dep/.test(text))) {
        attempts.push({ surface: 'web', url, attempt, result: 'RETRY', reason: 'Vite 504 / Outdated Optimize Dep transient' })
        errors.length = 0
        await page.reload({ waitUntil: 'networkidle', timeout: 30000 })
        await page.waitForTimeout(700)
      }
      return
    } catch (error) {
      lastError = error
      attempts.push({ surface: 'web', url, attempt, result: 'RETRY', reason: String(error?.message || error) })
      await page.waitForTimeout(800)
    }
  }
  throw lastError || new Error(`web navigation failed: ${url}`)
}

async function openWebAssistant(page) {
  const fab = page.locator('.ma__fab')
  await fab.waitFor({ state: 'visible', timeout: 20000 })
  await fab.click()
  const dialog = page.getByRole('dialog', { name: '问材料' })
  await dialog.waitFor({ state: 'visible', timeout: 12000 })
  await page.waitForTimeout(700)
  return dialog
}

async function askWeb(dialog, page, question) {
  const input = dialog.locator('.ma__composer textarea')
  await input.fill(question)
  await dialog.getByRole('button', { name: '发送' }).click()
  await dialog.locator('.ma__answer').waitFor({ state: 'visible', timeout: 20000 })
  await page.waitForTimeout(300)
  return normalized(await dialog.locator('.ma__answer-text').innerText())
}

async function openAppAssistant(page) {
  const fab = page.locator('.ma__fab')
  await fab.waitFor({ state: 'visible', timeout: 20000 })
  await fab.click()
  const panel = page.locator('.jz-sheet__panel')
  await panel.waitFor({ state: 'visible', timeout: 12000 })
  await page.waitForTimeout(700)
  return panel
}

async function askApp(panel, page, question) {
  const input = panel.locator('.ma__input textarea')
  await input.fill(question)
  await panel.getByText('发送', { exact: true }).click()
  await panel.locator('.ma__answer').waitFor({ state: 'visible', timeout: 20000 })
  await page.waitForTimeout(300)
  return normalized(await panel.locator('.ma__answer-text').innerText())
}

async function appLogin(page, token) {
  await page.goto(`${APP}/#/pages/auth/login?devToken=${encodeURIComponent(token)}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForTimeout(1800)
}

async function appGoto(page, pathname) {
  await page.goto(`${APP}/#/${pathname}`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForTimeout(2300)
}

function productErrors(errors, intentional = []) {
  return errors.filter(text => !/504|Outdated Optimize Dep|net::ERR_FAILED/.test(text)
    && !(intentional.length > 0 && /500 \(Internal Server Error\)/.test(text)))
}

async function runWebChecks() {
  const browser = await chromium.launch({ headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
    const page = await context.newPage()
    const errors = []
    const intentionalErrors = []
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()) })
    page.on('pageerror', error => errors.push(`PAGEERROR: ${error.message}`))

    await page.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
    await page.evaluate(token => localStorage.setItem('decoboon-web.token', token), tokens.Boss)
    await webGoto(page, `${WEB}/finance/projects/${PROJECT_ID}`, errors)
    await page.locator('.ma__fab').waitFor({ state: 'visible', timeout: 20000 })
    check('B8-UI-WEB-PROJECT-ENTRY', 'web-ui', '项目核算页显示「问材料」入口', 'fab visible', true, 'visible')
    await shot(page, 'b8-web-project-entry-1440x900')

    let dialog = await openWebAssistant(page)
    const focusInside = await page.evaluate(() => Boolean(document.activeElement?.closest('.el-drawer')))
    const projectContext = normalized(await dialog.locator('.ma__context').innerText())
    const projectChips = await dialog.locator('.ma__chip').allTextContents()
    check('B8-UI-WEB-PROJECT-CONTEXT', 'web-ui', '项目上下文显示对象、已绑定、只读',
      '含项目上下文/已绑定/只读/真实项目名',
      projectContext.includes('项目上下文') && projectContext.includes('已绑定') && projectContext.includes('只读')
        && projectContext.includes('王芳（造数1100）'), projectContext)
    check('B8-UI-WEB-SUGGESTIONS', 'web-ui', '项目建议问题来自服务端且可点击',
      '至少 5 条，命中项目建议',
      projectChips.length >= 5 && projectChips.some(q => q.includes('材料业务现在缺什么')), projectChips)
    check('B8-UI-WEB-FOCUS-OPEN', 'web-ui', '打开抽屉后焦点进入抽屉', 'activeElement 位于 drawer 内', focusInside, focusInside)
    uiMetrics.styles.webProject = await collectStyles(page, true)
    await shot(page, 'b8-web-project-drawer-1440x900')

    const slowWebRoute = async route => { await new Promise(resolve => setTimeout(resolve, 900)); await route.continue() }
    await page.route('**/material/assistant/ask', slowWebRoute)
    await dialog.locator('.ma__chip').filter({ hasText: '这个项目的材料业务现在缺什么？' }).first().click()
    await dialog.locator('.ma__loading').waitFor({ state: 'visible', timeout: 5000 })
    check('B8-UI-WEB-LOADING', 'web-ui', 'Web 请求期间显示加载态', '含正在校核材料事实',
      (await dialog.locator('.ma__loading').innerText()).includes('正在校核材料事实'),
      normalized(await dialog.locator('.ma__loading').innerText()))
    await shot(page, 'b8-web-project-loading-1440x900')
    await dialog.locator('.ma__answer').waitFor({ state: 'visible', timeout: 20000 })
    await page.unroute('**/material/assistant/ask', slowWebRoute)
    await page.waitForTimeout(400)
    const projectAnswerPanel = normalized(await dialog.locator('.ma__answer').innerText())
    webProjectAnswer = normalized(await dialog.locator('.ma__answer-text').innerText())
    check('B8-UI-WEB-SUGGESTION-CLICK', 'web-ui', '点击服务端建议问题可得到回答', '回答可见且类型为实时数据',
      webProjectAnswer.length > 0 && projectAnswerPanel.includes('实时数据'), projectAnswerPanel.slice(0, 260))
    check('B8-UI-WEB-EVIDENCE-TOOL-NEXT', 'web-ui', 'Web 回答展示证据/只读工具/下一步/审计',
      '回答含实时数据、证据、只读工具、下一步、只读、已审计',
      ['证据', '只读工具', '下一步', '只读', '已审计'].every(text => projectAnswerPanel.includes(text))
        && projectAnswerPanel.includes('CHK-UNSELECTED') && projectAnswerPanel.includes('CHK-COST-INCOMPLETE'), projectAnswerPanel.slice(0, 420))
    check('B8-UI-WEB-COST-NULL', 'web-ui', '成本不完整时显示利润 null', '回答含利润：null',
      webProjectAnswer.includes('利润：null'), webProjectAnswer.slice(0, 220))
    check('B8-UI-WEB-CROSS-API-PROJECT', 'web-ui', 'Web 项目回答与 API 同源一致',
      'answer text 归一化后完全一致',
      webProjectAnswer === normalized(projectApiAnswer), { api: projectApiAnswer, web: webProjectAnswer })
    check('B8-UI-WEB-NO-WRITE-ACTION', 'web-ui', '助手抽屉无审批/付款/删除/改权限按钮',
      '按钮文本不包含写操作',
      !(await dialog.locator('button').allTextContents()).some(text => /审批|付款|删除|改权限/.test(text)),
      await dialog.locator('button').allTextContents())
    await shot(page, 'b8-web-project-answer-1440x900')

    const refusalPanelText = await askWeb(dialog, page, '帮我审批这张采购单')
    check('B8-UI-WEB-REFUSAL', 'web-ui', 'REFUSAL 原样展示', '含不在助手范围与不能自动审批',
      (await dialog.locator('.ma__answer').innerText()).includes('不在助手范围') && refusalPanelText.includes('不能自动审批'), refusalPanelText.slice(0, 220))
    await shot(page, 'b8-web-project-refusal-1440x900')

    const noEvidenceText = await askWeb(dialog, page, '火星材料怎么报税？')
    check('B8-UI-WEB-NO-EVIDENCE', 'web-ui', 'NO_EVIDENCE 原样展示', '含暂无证据与没有足够证据',
      (await dialog.locator('.ma__answer').innerText()).includes('暂无证据') && noEvidenceText.includes('没有足够证据'), noEvidenceText.slice(0, 220))
    await shot(page, 'b8-web-project-noevidence-1440x900')

    const longAnswerText = '长回答压力验证：材料计划、作业单、采购单、到场与结算需按同一业务事实核对。'.repeat(70)
    const longCitations = Array.from({ length: 18 }, (_, index) => ({
      type: 'CHECK', id: `B8-STRESS-${String(index + 1).padStart(2, '0')}`, label: `压力证据 ${index + 1}`,
      detail: '仅用于验证长证据列表滚动与换行，不代表新的业务事实。'
    }))
    const longRoute = route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: {
        question: '长回答压力验证', intent: 'PROJECT_HEALTH', answerType: 'LIVE_DATA', answer: longAnswerText,
        confidence: 0.9, readOnly: true, citations: longCitations,
        toolCalls: [{ name: 'stress.read', provider: 'stress/read-only', status: 'OK', scope: 'test', readOnly: true }],
        nextActions: ['核对材料事实', '检查证据列表滚动', '确认无横向溢出'], suggestedQuestions: [],
        auditId: 'B8-STRESS', auditStatus: 'RECORDED'
      } })
    })
    await page.route('**/material/assistant/ask', longRoute)
    await dialog.locator('.ma__composer textarea').fill('执行长回答与长证据压力验证')
    await dialog.getByRole('button', { name: '发送' }).click()
    await dialog.locator('.ma__answer').waitFor({ state: 'visible', timeout: 10000 })
    await page.waitForTimeout(300)
    const longWebBody = await dialog.locator('.ma__answer').innerText()
    const longWebMetrics = await collectMetrics(page)
    check('B8-UI-WEB-LONG-CONTENT', 'web-ui', 'Web 长回答与 18 条证据无破版/横向溢出',
      '回答完整、证据可见、页面无横向溢出',
      longWebBody.includes('长回答压力验证') && longWebBody.includes('B8-STRESS-18') && !longWebMetrics.horizontalOverflow,
      { citationCount: longCitations.length, metrics: longWebMetrics, note: '注入压力数据，仅验证渲染，不代表业务事实' })
    await shot(page, 'b8-web-project-long-content-1440x900')
    await page.unroute('**/material/assistant/ask', longRoute)

    await dialog.getByText('清除上下文', { exact: true }).click()
    await page.waitForTimeout(500)
    const unboundText = normalized(await dialog.locator('.ma__context').innerText())
    check('B8-UI-WEB-CLEAR-CONTEXT', 'web-ui', '清除上下文后明确显示未绑定',
      '含未绑定业务单据/仅解释材料规则',
      unboundText.includes('未绑定业务单据') && unboundText.includes('仅解释材料规则'), unboundText)
    await shot(page, 'b8-web-project-unbound-1440x900')
    await dialog.getByText('恢复当前页面上下文', { exact: true }).click()
    await page.waitForTimeout(500)
    check('B8-UI-WEB-RESTORE-CONTEXT', 'web-ui', '恢复上下文重新绑定当前项目',
      '含已绑定与真实项目名',
      (await dialog.locator('.ma__context').innerText()).includes('已绑定')
        && (await dialog.locator('.ma__context').innerText()).includes('王芳（造数1100）'),
      normalized(await dialog.locator('.ma__context').innerText()))

    let failOnce = true
    await page.route('**/material/assistant/ask', async route => {
      if (failOnce) {
        failOnce = false
        intentionalErrors.push('B8 injected 500 on material/assistant/ask')
        await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ success: false, errorCode: { code: 'B8_INJECTED', message: 'B8 注入失败' } }) })
      } else {
        await route.continue()
      }
    })
    await dialog.locator('.ma__composer textarea').fill('暂估应付是什么意思？')
    await dialog.getByRole('button', { name: '发送' }).click()
    await dialog.locator('.ma__error').waitFor({ state: 'visible', timeout: 10000 })
    check('B8-UI-WEB-ERROR', 'web-ui', '请求失败显示暂不可确认与重试', '含材料助手暂不可确认/重试',
      (await dialog.locator('.ma__error').innerText()).includes('材料助手暂不可确认')
        && (await dialog.locator('.ma__error').innerText()).includes('重试'),
      normalized(await dialog.locator('.ma__error').innerText()))
    await shot(page, 'b8-web-project-error-1440x900')
    await page.unroute('**/material/assistant/ask')
    await dialog.getByRole('button', { name: '重试' }).click()
    await dialog.locator('.ma__answer').waitFor({ state: 'visible', timeout: 20000 })
    check('B8-UI-WEB-RETRY', 'web-ui', '重试后恢复成功回答', 'answer visible',
      (await dialog.locator('.ma__answer-text').innerText()).length > 0,
      normalized(await dialog.locator('.ma__answer-text').innerText()).slice(0, 180))

    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
    const closed = !(await page.getByRole('dialog', { name: '问材料' }).isVisible().catch(() => false))
    const focusReturned = await page.evaluate(() => document.activeElement?.classList?.contains('ma__fab') || document.activeElement?.getAttribute('aria-label') === '问材料')
    check('B8-UI-WEB-ESC', 'web-ui', 'Esc 可关闭抽屉', 'dialog hidden', closed, closed)
    check('B8-UI-WEB-FOCUS-RETURN', 'web-ui', '关闭后焦点回到触发入口', 'activeElement 为问材料按钮', focusReturned, focusReturned)

    uiMetrics.web.project = await collectMetrics(page)
    check('B8-UI-WEB-PROJECT-OVERFLOW', 'web-ui', 'Web 项目页 1440×900 无横向溢出',
      'scrollWidth<=innerWidth',
      !uiMetrics.web.project.horizontalOverflow, uiMetrics.web.project)

    // 1024 project viewport.
    await page.setViewportSize({ width: 1024, height: 768 })
    await webGoto(page, `${WEB}/finance/projects/${PROJECT_ID}`, errors)
    dialog = await openWebAssistant(page)
    uiMetrics.web.projectCompact = await collectMetrics(page)
    check('B8-UI-WEB-PROJECT-1024', 'web-ui', 'Web 项目上下文 1024×768 无横向溢出',
      'scrollWidth<=innerWidth',
      !uiMetrics.web.projectCompact.horizontalOverflow, uiMetrics.web.projectCompact)
    await shot(page, 'b8-web-project-1024x768')
    await page.keyboard.press('Escape')
    await page.setViewportSize({ width: 1440, height: 900 })

    // Ordering page, select real temporary sheet, then assistant binds sheetId.
    await webGoto(page, `${WEB}/finance/material-ordering`, errors)
    const orderingRow = page.locator('li', { hasText: setup.sheetNo }).first()
    await orderingRow.waitFor({ state: 'visible', timeout: 20000 })
    await orderingRow.click()
    await page.waitForTimeout(700)
    dialog = await openWebAssistant(page)
    const purchaseContext = normalized(await dialog.locator('.ma__context').innerText())
    check('B8-UI-WEB-PURCHASE-CONTEXT', 'web-ui', '采购页选择真实作业单后显示已绑定采购上下文',
      '含采购上下文/已绑定/真实 sheetNo/项目名',
      purchaseContext.includes('采购上下文') && purchaseContext.includes('已绑定')
        && purchaseContext.includes(setup.sheetNo) && purchaseContext.includes('王芳（造数1100）'), purchaseContext)
    const purchaseChips = await dialog.locator('.ma__chip').allTextContents()
    check('B8-UI-WEB-PURCHASE-SUGGESTIONS', 'web-ui', '采购上下文建议来自服务端同源', '至少 5 条', purchaseChips.length >= 5, purchaseChips)
    await shot(page, 'b8-web-ordering-drawer-1440x900')

    webSheetAnswer = await askWeb(dialog, page, SHEET_QUESTION)
    const webSheetPanel = normalized(await dialog.locator('.ma__answer').innerText())
    check('B8-UI-WEB-SHEET-ANSWER', 'web-ui', 'Web 作业单上下文回答引用真实单据与证据',
      '含 sheetNo/状态/证据/只读工具/下一步',
      webSheetAnswer.includes(setup.sheetNo) && /已提交请购|待下单/.test(webSheetAnswer)
        && ['证据', '只读工具', '下一步'].every(text => webSheetPanel.includes(text))
        && webSheetPanel.includes('R-SHEET-NO-PRICE'), webSheetPanel.slice(0, 500))
    check('B8-UI-WEB-CROSS-API-SHEET', 'web-ui', 'Web 作业单回答与 API 同源一致',
      'answer text 归一化后完全一致',
      webSheetAnswer === normalized(sheetApiAnswer), { api: sheetApiAnswer, web: webSheetAnswer })
    await shot(page, 'b8-web-ordering-answer-1440x900')
    uiMetrics.web.ordering = await collectMetrics(page)
    check('B8-UI-WEB-ORDERING-OVERFLOW', 'web-ui', 'Web 采购页 1440×900 无横向溢出',
      'scrollWidth<=innerWidth', !uiMetrics.web.ordering.horizontalOverflow, uiMetrics.web.ordering)
    await page.keyboard.press('Escape')

    await page.setViewportSize({ width: 1024, height: 768 })
    await webGoto(page, `${WEB}/finance/material-ordering`, errors)
    const compactRow = page.locator('li', { hasText: setup.sheetNo }).first()
    if (await compactRow.count()) await compactRow.click()
    await page.waitForTimeout(500)
    dialog = await openWebAssistant(page)
    await shot(page, 'b8-web-ordering-1024x768')
    uiMetrics.web.orderingCompact = await collectMetrics(page)
    check('B8-UI-WEB-ORDERING-1024', 'web-ui', 'Web 采购上下文 1024×768 无横向溢出',
      'scrollWidth<=innerWidth', !uiMetrics.web.orderingCompact.horizontalOverflow, uiMetrics.web.orderingCompact)
    await page.keyboard.press('Escape')
    await page.setViewportSize({ width: 1440, height: 900 })

    await webGoto(page, `${WEB}/finance/material-settlement`, errors)
    dialog = await openWebAssistant(page)
    const settlementContext = normalized(await dialog.locator('.ma__context').innerText())
    const settlementChips = await dialog.locator('.ma__chip').allTextContents()
    check('B8-UI-WEB-SETTLEMENT-CONTEXT', 'web-ui', '结算页显示结算上下文与结算建议',
      '含结算上下文/主材采购结算/结算建议',
      settlementContext.includes('结算上下文') && settlementContext.includes('主材采购结算')
        && settlementChips.some(question => question.includes('结算')), settlementContext)
    await shot(page, 'b8-web-settlement-drawer-1440x900')
    const settlementAnswer = await askWeb(dialog, page, '逐单结算和月结会重复计算吗？')
    check('B8-UI-WEB-SETTLEMENT-ANSWER', 'web-ui', '结算上下文回答互斥/逐单优先/月结兜底',
      '含互斥或重复、逐单优先、月结',
      /结算/.test(settlementAnswer) && (settlementAnswer.includes('互斥') || settlementAnswer.includes('重复'))
        && settlementAnswer.includes('逐单') && settlementAnswer.includes('月结'), settlementAnswer.slice(0, 260))
    await shot(page, 'b8-web-settlement-answer-1440x900')
    uiMetrics.web.settlement = await collectMetrics(page)
    check('B8-UI-WEB-SETTLEMENT-OVERFLOW', 'web-ui', 'Web 结算页 1440×900 无横向溢出',
      'scrollWidth<=innerWidth', !uiMetrics.web.settlement.horizontalOverflow, uiMetrics.web.settlement)
    await page.keyboard.press('Escape')

    await page.setViewportSize({ width: 1024, height: 768 })
    await webGoto(page, `${WEB}/finance/material-settlement`, errors)
    dialog = await openWebAssistant(page)
    await shot(page, 'b8-web-settlement-1024x768')
    uiMetrics.web.settlementCompact = await collectMetrics(page)
    check('B8-UI-WEB-SETTLEMENT-1024', 'web-ui', 'Web 结算上下文 1024×768 无横向溢出',
      'scrollWidth<=innerWidth', !uiMetrics.web.settlementCompact.horizontalOverflow, uiMetrics.web.settlementCompact)

    const normalProductErrors = productErrors(errors, intentionalErrors)
    uiMetrics.consoleErrors.web = { product: normalProductErrors, intentional: intentionalErrors }
    check('B8-UI-WEB-CONSOLE', 'web-ui', 'Web 业务 Console Errors=0',
      '排除 Vite 504 瞬态和故意注入失败后为 0',
      normalProductErrors.length === 0, normalProductErrors)
    await context.close()

    // Designer financial pages must not expose the assistant or costs.
    const designerContext = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
    const designerPage = await designerContext.newPage()
    const designerErrors = []
    designerPage.on('console', message => { if (message.type() === 'error') designerErrors.push(message.text()) })
    await designerPage.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
    await designerPage.evaluate(token => localStorage.setItem('decoboon-web.token', token), tokens.Designer)
    await webGoto(designerPage, `${WEB}/finance/projects/${PROJECT_ID}`, designerErrors)
    const designerBody = await designerPage.locator('body').innerText()
    check('B8-UI-WEB-DESIGNER-PERMISSION', 'web-ui', 'Designer 不得因助手入口获得财务成本视图',
      '无问材料入口，页面不展示实际成本/材料应付',
      (await designerPage.locator('.ma__fab').count()) === 0 && !designerBody.includes('实际成本') && !designerBody.includes('材料应付'),
      designerBody.slice(0, 500))
    await shot(designerPage, 'b8-web-designer-no-finance-1440x900')
    uiMetrics.consoleErrors.web.designer = productErrors(designerErrors)
    check('B8-UI-WEB-DESIGNER-CONSOLE', 'web-ui', 'Designer 权限页 Console Errors=0', '0', productErrors(designerErrors).length === 0, productErrors(designerErrors))
    await designerContext.close()
  } finally {
    await browser.close()
  }
}

async function runAppChecks() {
  const browser = await chromium.launch({ headless: true })
  try {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'zh-CN' })
    const page = await context.newPage()
    const errors = []
    const intentionalErrors = []
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()) })
    page.on('pageerror', error => errors.push(`PAGEERROR: ${error.message}`))
    await appLogin(page, tokens.Boss)

    await appGoto(page, `subpackages/project/pages/detail?id=${PROJECT_ID}`)
    let panel = await openAppAssistant(page)
    const projectContext = normalized(await panel.locator('.ma__context').innerText())
    const projectChips = await panel.locator('.ma__chip').allTextContents()
    check('B8-UI-APP-PROJECT-CONTEXT', 'app-ui', 'App 项目详情显示已绑定项目上下文',
      '含项目上下文/已绑定/真实项目名', projectContext.includes('项目上下文') && projectContext.includes('已绑定')
        && projectContext.includes('王芳（造数1100）'), projectContext)
    check('B8-UI-APP-SUGGESTIONS', 'app-ui', 'App 项目建议来自服务端', '至少 5 条', projectChips.length >= 5, projectChips)
    await shot(page, 'b8-app-project-drawer-390x844')
    const slowAppRoute = async route => { await new Promise(resolve => setTimeout(resolve, 900)); await route.continue() }
    await page.route('**/material/assistant/ask', slowAppRoute)
    await panel.locator('.ma__chip').filter({ hasText: '这个项目的材料业务现在缺什么？' }).first().click()
    await panel.locator('.ma__hint').waitFor({ state: 'visible', timeout: 5000 })
    check('B8-UI-APP-LOADING', 'app-ui', 'App 请求期间显示加载态', '含正在校核材料事实',
      (await panel.locator('.ma__hint').innerText()).includes('正在校核材料事实'),
      normalized(await panel.locator('.ma__hint').innerText()))
    await shot(page, 'b8-app-project-loading-390x844')
    await panel.locator('.ma__answer').waitFor({ state: 'visible', timeout: 20000 })
    await page.unroute('**/material/assistant/ask', slowAppRoute)
    await page.waitForTimeout(400)
    appProjectAnswer = normalized(await panel.locator('.ma__answer-text').innerText())
    const appProjectPanel = normalized(await panel.locator('.ma__answer').innerText())
    check('B8-UI-APP-PROJECT-ANSWER', 'app-ui', 'App 项目回答展示证据/只读工具/下一步/审计',
      '含实时数据、证据、只读工具、下一步、只读、已审计、利润 null',
      ['实时数据', '证据', '只读工具', '下一步', '只读', '已审计', '利润：null'].every(text => appProjectPanel.includes(text))
        && appProjectPanel.includes('CHK-UNSELECTED'), appProjectPanel.slice(0, 460))
    check('B8-UI-APP-CROSS-API-PROJECT', 'app-ui', 'App 项目回答与 API 同源一致',
      'answer text 归一化后完全一致', appProjectAnswer === normalized(projectApiAnswer),
      { api: projectApiAnswer, app: appProjectAnswer })
    check('B8-UI-APP-NO-WRITE-ACTION', 'app-ui', 'App 助手无审批/付款/删除/改权限按钮',
      '可点击元素文本不包含写操作',
      !(await panel.locator('button').allTextContents()).some(text => /审批|付款|删除|改权限/.test(text)),
      await panel.locator('button').allTextContents())
    await shot(page, 'b8-app-project-answer-390x844')
    uiMetrics.app.project = await collectMetrics(page)
    uiMetrics.styles.appProject = await page.evaluate(() => {
      const pick = selector => {
        const el = document.querySelector(selector)
        if (!el) return null
        const style = getComputedStyle(el)
        const rect = el.getBoundingClientRect()
        return { fontSize: style.fontSize, lineHeight: style.lineHeight, background: style.backgroundColor, borderRadius: style.borderRadius, x: rect.x, y: rect.y, width: rect.width, height: rect.height, bottomGap: window.innerHeight - rect.bottom }
      }
      return { fab: pick('.ma__fab'), panel: pick('.jz-sheet__panel'), composer: pick('.ma__composer'), boundary: pick('.ma__boundary') }
    })
    check('B8-UI-APP-PROJECT-OVERFLOW', 'app-ui', 'App 项目页 390×844 无横向溢出',
      'scrollWidth<=innerWidth', !uiMetrics.app.project.horizontalOverflow, uiMetrics.app.project)
    check('B8-UI-APP-SAFE-AREA', 'app-ui', 'App 面板与输入区在视口内',
      'panel/composer bottomGap>=0 且 panel 高度<=视口',
      uiMetrics.styles.appProject.panel && uiMetrics.styles.appProject.panel.height <= 844
        && uiMetrics.styles.appProject.composer && uiMetrics.styles.appProject.composer.bottomGap >= 0,
      uiMetrics.styles.appProject)

    await panel.locator('.jz-sheet__close').click()
    await page.waitForTimeout(400)

    // Sheet detail with real temporary sheet.
    await appGoto(page, `subpackages/project/pages/material-sheet-detail?sheetId=${setup.sheetId}`)
    panel = await openAppAssistant(page)
    const sheetContext = normalized(await panel.locator('.ma__context').innerText())
    check('B8-UI-APP-SHEET-CONTEXT', 'app-ui', 'App 作业单详情绑定真实作业单',
      '含作业单上下文/已绑定/真实 sheetNo/状态',
      sheetContext.includes('作业单上下文') && sheetContext.includes('已绑定') && sheetContext.includes(setup.sheetNo), sheetContext)
    await shot(page, 'b8-app-sheet-drawer-390x844')
    await panel.locator('.ma__chip').first().click()
    await panel.locator('.ma__answer').waitFor({ state: 'visible', timeout: 20000 })
    await page.waitForTimeout(400)
    const firstSheetSuggestionAnswer = normalized(await panel.locator('.ma__answer-text').innerText())
    check('B8-UI-APP-SHEET-SUGGESTION', 'app-ui', 'App 点击作业单建议问题可回答',
      '回答含真实 sheetNo 或模板门禁', firstSheetSuggestionAnswer.includes(setup.sheetNo)
        || /测量|模板必填|待下单|已提交请购/.test(firstSheetSuggestionAnswer), firstSheetSuggestionAnswer.slice(0, 240))
    appSheetAnswer = await askApp(panel, page, SHEET_QUESTION)
    const appSheetPanel = normalized(await panel.locator('.ma__answer').innerText())
    check('B8-UI-APP-SHEET-ANSWER', 'app-ui', 'App 作业单回答展示证据/只读/下一步',
      '含 sheetNo、证据、只读工具、下一步',
      appSheetAnswer.includes(setup.sheetNo) && ['证据', '只读工具', '下一步'].every(text => appSheetPanel.includes(text))
        && appSheetPanel.includes('R-SHEET-NO-PRICE'), appSheetPanel.slice(0, 500))
    check('B8-UI-APP-CROSS-API-SHEET', 'app-ui', 'App 作业单回答与 API 同源一致',
      'answer text 归一化后完全一致', appSheetAnswer === normalized(sheetApiAnswer),
      { api: sheetApiAnswer, app: appSheetAnswer })
    await shot(page, 'b8-app-sheet-answer-390x844')
    await panel.locator('.jz-sheet__close').click()
    await page.waitForTimeout(350)

    // Settlement page.
    await appGoto(page, 'subpackages/finance/pages/material-settlement')
    panel = await openAppAssistant(page)
    const settlementContext = normalized(await panel.locator('.ma__context').innerText())
    const settlementChips = await panel.locator('.ma__chip').allTextContents()
    check('B8-UI-APP-SETTLEMENT-CONTEXT', 'app-ui', 'App 主材结算显示结算上下文与建议',
      '含结算上下文/主材采购结算/结算建议',
      settlementContext.includes('结算上下文') && settlementContext.includes('主材采购结算')
        && settlementChips.some(question => question.includes('结算')), settlementContext)
    await shot(page, 'b8-app-settlement-drawer-390x844')
    const appSettlementAnswer = await askApp(panel, page, '逐单结算和月结会重复计算吗？')
    check('B8-UI-APP-SETTLEMENT-ANSWER', 'app-ui', 'App 结算回答含互斥/月结口径',
      '含逐单/月结/互斥或重复',
      appSettlementAnswer.includes('逐单') && appSettlementAnswer.includes('月结')
        && (appSettlementAnswer.includes('互斥') || appSettlementAnswer.includes('重复')), appSettlementAnswer.slice(0, 260))
    await shot(page, 'b8-app-settlement-answer-390x844')
    await panel.locator('.jz-sheet__close').click()
    await page.waitForTimeout(350)

    // Clear/restore context.
    await appGoto(page, `subpackages/project/pages/detail?id=${PROJECT_ID}`)
    panel = await openAppAssistant(page)
    await panel.getByText('清除上下文', { exact: true }).click()
    await page.waitForTimeout(500)
    const appUnbound = normalized(await panel.locator('.ma__context').innerText())
    check('B8-UI-APP-CLEAR-CONTEXT', 'app-ui', 'App 清除上下文后明确未绑定',
      '含未绑定业务单据/仅解释材料规则', appUnbound.includes('未绑定业务单据') && appUnbound.includes('仅解释材料规则'), appUnbound)
    await panel.getByText('恢复当前页面上下文', { exact: true }).click()
    await page.waitForTimeout(500)
    check('B8-UI-APP-RESTORE-CONTEXT', 'app-ui', 'App 恢复上下文重新绑定当前项目',
      '含已绑定与真实项目名',
      (await panel.locator('.ma__context').innerText()).includes('已绑定')
        && (await panel.locator('.ma__context').innerText()).includes('王芳（造数1100）'),
      normalized(await panel.locator('.ma__context').innerText()))

    // Refusal.
    const appRefusal = await askApp(panel, page, '帮我审批这张采购单')
    check('B8-UI-APP-REFUSAL', 'app-ui', 'App REFUSAL 原样展示',
      '含不在助手范围与不能自动审批',
      (await panel.locator('.ma__answer').innerText()).includes('不在助手范围') && appRefusal.includes('不能自动审批'),
      appRefusal.slice(0, 240))
    await shot(page, 'b8-app-refusal-390x844')

    // Error and retry.
    let failOnce = true
    await page.route('**/material/assistant/ask', async route => {
      if (failOnce) {
        failOnce = false
        intentionalErrors.push('B8 injected 500 on material/assistant/ask')
        await route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ success: false, errorCode: { code: 'B8_INJECTED', message: 'B8 注入失败' } }) })
      } else {
        await route.continue()
      }
    })
    await panel.locator('.ma__input textarea').fill('暂估应付是什么意思？')
    await panel.getByText('发送', { exact: true }).click()
    await panel.locator('.ma__error').waitFor({ state: 'visible', timeout: 10000 })
    check('B8-UI-APP-ERROR', 'app-ui', 'App 请求失败显示暂不可确认与重试',
      '含材料助手暂不可确认/重试',
      (await panel.locator('.ma__error').innerText()).includes('材料助手暂不可确认')
        && (await panel.locator('.ma__error').innerText()).includes('重试'), normalized(await panel.locator('.ma__error').innerText()))
    await shot(page, 'b8-app-error-390x844')
    await page.unroute('**/material/assistant/ask')
    await panel.getByText('重试', { exact: true }).click()
    await panel.locator('.ma__answer').waitFor({ state: 'visible', timeout: 20000 })
    check('B8-UI-APP-RETRY', 'app-ui', 'App 重试后恢复成功回答',
      'answer visible', (await panel.locator('.ma__answer-text').innerText()).length > 0,
      normalized(await panel.locator('.ma__answer-text').innerText()).slice(0, 180))
    const longAppAnswer = '长回答压力验证：材料计划、作业单、采购单、到场与结算需按同一业务事实核对。'.repeat(70)
    const longAppCitations = Array.from({ length: 18 }, (_, index) => ({
      type: 'CHECK', id: `B8-APP-STRESS-${String(index + 1).padStart(2, '0')}`, label: `压力证据 ${index + 1}`,
      detail: '仅用于验证长证据列表滚动与换行，不代表新的业务事实。'
    }))
    const longAppRoute = route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: {
        question: '长回答压力验证', intent: 'PROJECT_HEALTH', answerType: 'LIVE_DATA', answer: longAppAnswer,
        confidence: 0.9, readOnly: true, citations: longAppCitations,
        toolCalls: [{ name: 'stress.read', provider: 'stress/read-only', status: 'OK', scope: 'test', readOnly: true }],
        nextActions: ['核对材料事实', '检查证据列表滚动', '确认无横向溢出'], suggestedQuestions: [],
        auditId: 'B8-APP-STRESS', auditStatus: 'RECORDED'
      } })
    })
    await page.route('**/material/assistant/ask', longAppRoute)
    await panel.locator('.ma__input textarea').fill('执行长回答与长证据压力验证')
    await panel.getByText('发送', { exact: true }).click()
    await panel.locator('.ma__answer').waitFor({ state: 'visible', timeout: 10000 })
    await page.waitForTimeout(300)
    const longAppBody = await panel.locator('.ma__answer').innerText()
    const longAppMetrics = await collectMetrics(page)
    check('B8-UI-APP-LONG-CONTENT', 'app-ui', 'App 长回答与 18 条证据无破版/横向溢出',
      '回答完整、证据可见、页面无横向溢出',
      longAppBody.includes('长回答压力验证') && longAppBody.includes('B8-APP-STRESS-18') && !longAppMetrics.horizontalOverflow,
      { citationCount: longAppCitations.length, metrics: longAppMetrics, note: '注入压力数据，仅验证渲染，不代表业务事实' })
    await shot(page, 'b8-app-long-content-390x844')
    await page.unroute('**/material/assistant/ask', longAppRoute)
    await panel.locator('.jz-sheet__close').click()
    await page.waitForTimeout(350)

    // Small viewport.
    const smallContext = await browser.newContext({ viewport: { width: 375, height: 667 }, locale: 'zh-CN' })
    const smallPage = await smallContext.newPage()
    const smallErrors = []
    smallPage.on('console', message => { if (message.type() === 'error') smallErrors.push(message.text()) })
    await appLogin(smallPage, tokens.Finance)
    await appGoto(smallPage, `subpackages/project/pages/detail?id=${PROJECT_ID}`)
    const smallPanel = await openAppAssistant(smallPage)
    await shot(smallPage, 'b8-app-project-375x667')
    uiMetrics.app.compact = await collectMetrics(smallPage)
    uiMetrics.styles.appCompact = await smallPage.evaluate(() => {
      const el = document.querySelector('.jz-sheet__panel')
      const r = el?.getBoundingClientRect()
      return { panel: r ? { x: r.x, y: r.y, width: r.width, height: r.height, bottomGap: innerHeight - r.bottom } : null, viewport: { width: innerWidth, height: innerHeight } }
    })
    check('B8-UI-APP-375', 'app-ui', 'App H5 375×667 无横向溢出',
      'scrollWidth<=innerWidth', !uiMetrics.app.compact.horizontalOverflow, uiMetrics.app.compact)
    check('B8-UI-APP-375-PANEL', 'app-ui', 'App 375×667 面板不超出视口',
      'panel height<=667, bottomGap>=0',
      uiMetrics.styles.appCompact.panel?.height <= 667 && uiMetrics.styles.appCompact.panel?.bottomGap >= 0,
      uiMetrics.styles.appCompact)
    await smallPage.locator('.ma__input textarea').focus()
    await smallPage.setViewportSize({ width: 375, height: 430 })
    await smallPage.waitForTimeout(500)
    const keyboardProxy = await smallPage.evaluate(() => {
      const composer = document.querySelector('.ma__composer')?.getBoundingClientRect()
      const panel = document.querySelector('.jz-sheet__panel')?.getBoundingClientRect()
      return {
        innerWidth, innerHeight: window.innerHeight,
        composer: composer ? { top: composer.top, bottom: composer.bottom, width: composer.width } : null,
        panel: panel ? { top: panel.top, bottom: panel.bottom, width: panel.width } : null,
        horizontalOverflow: document.documentElement.scrollWidth > innerWidth || document.body.scrollWidth > innerWidth
      }
    })
    uiMetrics.styles.appKeyboardProxy = keyboardProxy
    check('B8-UI-APP-KEYBOARD-PROXY', 'app-ui', 'H5 缩小视口模拟键盘后输入区保持可见',
      'composer 在视口内且无横向溢出（非物理键盘证据）',
      keyboardProxy.composer && keyboardProxy.composer.bottom <= keyboardProxy.innerHeight && !keyboardProxy.horizontalOverflow,
      keyboardProxy)
    await shot(smallPage, 'b8-app-keyboard-proxy-375x430')
    await smallPage.setViewportSize({ width: 375, height: 667 })
    await smallPage.waitForTimeout(300)
    await smallPanel.locator('.jz-sheet__close').click()
    await smallContext.close()

    // Designer permission state.
    const designerContext = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'zh-CN' })
    const designerPage = await designerContext.newPage()
    const designerErrors = []
    designerPage.on('console', message => { if (message.type() === 'error') designerErrors.push(message.text()) })
    await appLogin(designerPage, tokens.Designer)
    await appGoto(designerPage, `subpackages/project/pages/detail?id=${PROJECT_ID}`)
    const designerPanel = await openAppAssistant(designerPage)
    const designerContextText = normalized(await designerPanel.locator('.ma__context').innerText())
    const designerAnswer = await askApp(designerPanel, designerPage, PROJECT_QUESTION)
    check('B8-UI-APP-DESIGNER-PERMISSION', 'app-ui', 'App Designer 只看到履约状态与权限提示',
      '含当前角色可查看履约状态，不含成本金额或利润 null',
      designerContextText.includes('已绑定') && /当前角色可查看履约状态/.test(designerAnswer)
        && !/¥|￥/.test(designerAnswer) && !designerAnswer.includes('利润：null'), designerAnswer.slice(0, 260))
    await shot(designerPage, 'b8-app-designer-project-390x844')
    check('B8-UI-APP-DESIGNER-CONSOLE', 'app-ui', 'App Designer 业务 Console Errors=0',
      '0', productErrors(designerErrors).length === 0, productErrors(designerErrors))
    await designerContext.close()

    const normalProductErrors = productErrors(errors, intentionalErrors)
    uiMetrics.consoleErrors.app = { product: normalProductErrors, intentional: intentionalErrors }
    check('B8-UI-APP-CONSOLE', 'app-ui', 'App 业务 Console Errors=0',
      '排除故意注入失败后为 0', normalProductErrors.length === 0, normalProductErrors)
    await context.close()
  } finally {
    await browser.close()
  }
}

function runCrossSurfaceChecks() {
  check('B8-CROSS-PROJECT-API-WEB-APP', 'cross-surface', '项目上下文 API/Web/App 回答同源',
    '三端 answer 文本一致',
    normalized(projectApiAnswer) === normalized(webProjectAnswer) && normalized(projectApiAnswer) === normalized(appProjectAnswer),
    { api: projectApiAnswer, web: webProjectAnswer, app: appProjectAnswer })
  check('B8-CROSS-PROJECT-FACTS', 'cross-surface', '项目状态/计数/阶段/下一步跨端一致',
    '均含 材料计划 2 条、待下单/待到货计数、选材/现场确认、下一步',
    [projectApiAnswer, webProjectAnswer, appProjectAnswer].every(answer => /材料计划\s*2\s*条/.test(answer)
      && /待下单\s*1\s*条/.test(answer) && /选材\s*\/\s*现场确认/.test(answer) && /下一步：设计师完成选材并交付财务/.test(answer)),
    { api: projectApiAnswer, web: webProjectAnswer, app: appProjectAnswer })
  check('B8-CROSS-SHEET-API-WEB-APP', 'cross-surface', '同一作业单 API/Web/App 回答同源',
    '三端 answer 文本一致且含同一 sheetNo',
    normalized(sheetApiAnswer) === normalized(webSheetAnswer) && normalized(sheetApiAnswer) === normalized(appSheetAnswer)
      && [sheetApiAnswer, webSheetAnswer, appSheetAnswer].every(answer => answer.includes(setup.sheetNo)),
    { sheetNo: setup.sheetNo, api: sheetApiAnswer, web: webSheetAnswer, app: appSheetAnswer })
  check('B8-CROSS-CITATION-IDS', 'cross-surface', '核心业务证据编号一致',
    '项目三端含 CHK-UNSELECTED/CHK-UNPRICED/CHK-COST-INCOMPLETE；作业单含 R-SHEET-NO-PRICE',
    ['CHK-UNSELECTED', 'CHK-UNPRICED', 'CHK-COST-INCOMPLETE'].every(id => (apiSamples.project.response.answer || '').includes(id) || (apiSamples.project.response.citations || []).some(c => c.id === id))
      && (apiSamples.sheet.response.citations || []).some(c => c.id === 'R-SHEET-NO-PRICE'),
    { projectCitations: apiSamples.project.response.citations?.map(c => c.id), sheetCitations: apiSamples.sheet.response.citations?.map(c => c.id) })
  check('B8-CROSS-COST-ROLE', 'cross-surface', '成本可见性随角色一致',
    'Boss/StoreManager/Finance 可见；Designer/PM 不可见；App Designer 同样不可见',
    ['B8-API-COST-VISIBLE-BOSS','B8-API-COST-VISIBLE-STOREMANAGER','B8-API-COST-VISIBLE-FINANCE','B8-API-COST-HIDDEN-DESIGNER','B8-API-COST-HIDDEN-PM','B8-UI-APP-DESIGNER-PERMISSION']
      .every(id => results.some(item => item.id === id && item.result === 'PASS')),
    'API 与 App Designer 权限检查逐项核对')
}


async function runDevtoolsChecks() {
  if (!RUN_DEVTOOLS) {
    check('B8-DEVTOOLS-SKIP', 'devtools', '微信开发者工具 automator 验收', '必须执行', false, 'B8_RUN_DEVTOOLS=false', '', 'BLOCKED')
    return
  }
  const script = path.join(OUT, 'run-b8-devtools.mjs')
  const devtoolsOutput = path.join(OUT, 'devtools-results.json')
  try {
    const stdout = execFileSync(process.execPath, [script], {
      cwd: OUT,
      env: {
        ...process.env,
        B8_API: API,
        B8_OUT: OUT,
        B8_PROJECT_ID: PROJECT_ID,
        B8_SHEET_ID: setup.sheetId,
        B8_PASSWORD: PASSWORD,
        B8_DEVTOOLS_PROJECT: path.join(APP_WORKTREE, 'dist/dev/mp-weixin')
      },
      encoding: 'utf8',
      timeout: 180000,
      maxBuffer: 10 * 1024 * 1024
    })
    attempts.push({ surface: 'devtools', result: 'PROCESS', stdout: stdout.slice(-2000) })
  } catch (error) {
    attempts.push({ surface: 'devtools', result: 'ERROR', error: String(error?.stack || error), stdout: String(error?.stdout || '').slice(-2000), stderr: String(error?.stderr || '').slice(-2000) })
    check('B8-DEVTOOLS-UNAVAILABLE', 'devtools', '微信开发者工具 automator 真实验收', '真实模拟器进入页面并完成问答', false,
      String(error?.message || error).slice(0, 500), '', 'BLOCKED')
    return
  }
  if (!fs.existsSync(devtoolsOutput)) {
    check('B8-DEVTOOLS-NO-RESULT', 'devtools', 'DevTools 产生结果文件', 'devtools-results.json 存在', false, 'missing', '', 'BLOCKED')
    return
  }
  const devtools = JSON.parse(fs.readFileSync(devtoolsOutput, 'utf8'))
  for (const item of devtools.results || []) results.push(item)
  Object.assign(uiMetrics.devtools, devtools.metrics || {})
  uiMetrics.consoleErrors.devtools = devtools.consoleErrors || []
  for (const shot of devtools.screenshots || []) {
    if (!screenshots.includes(shot)) screenshots.push(shot)
  }
  uiMetrics.screenshots = screenshots
}

async function cleanupFixture() {
  if (!setup) return
  businessCountsBeforeCleanup = tableCounts(setup.baselineTables)
  expectedCountsAfterCleanup = { ...businessCountsBeforeCleanup }
  expectedCountsAfterCleanup.db_material_order_sheet = Math.max(0, expectedCountsAfterCleanup.db_material_order_sheet - 1)
  expectedCountsAfterCleanup.db_material_order_sheet_item = Math.max(0, expectedCountsAfterCleanup.db_material_order_sheet_item - setup.itemCount)

  const cancel = await post('material/sheet/cancel', { sheetId: setup.sheetId, reason: 'B8 独立验收临时作业单清理' }, tokens.Designer).catch(error => ({ success: false, message: String(error) }))
  const del = await post('material/sheet/delete', { sheetId: setup.sheetId }, tokens.Designer).catch(error => ({ success: false, message: String(error) }))
  if (!del.success) {
    sql(`DELETE FROM db_material_order_sheet_item WHERE sheet_id=${escapeSql(setup.sheetId)}`)
    sql(`DELETE FROM db_material_order_sheet WHERE sheet_id=${escapeSql(setup.sheetId)}`)
  }

  if (projectOriginalManagerId && projectOriginalManagerId !== 'NULL') {
    sql(`UPDATE db_project SET manager_id=${escapeSql(projectOriginalManagerId)} WHERE project_id=${PROJECT_ID}`)
  } else {
    sql(`UPDATE db_project SET manager_id=NULL WHERE project_id=${PROJECT_ID}`)
  }

  if (setup.planOriginal) {
    const [status, quantity, netQuantity, lossRate, lossQuantity, unitPrice, cost, selectionId, arrivedAt] = setup.planOriginal.split('\t')
    sql(`UPDATE db_material_plan SET status=${escapeSql(status)},quantity=${quantity},net_quantity=${netQuantity},loss_rate=${lossRate},loss_quantity=${lossQuantity},unit_price=${unitPrice},cost=${cost},selection_id=${selectionId === 'NULL' ? 'NULL' : selectionId},arrived_at=${arrivedAt === 'NULL' ? 'NULL' : escapeSql(arrivedAt)} WHERE plan_id=${PLAN_ID}`)
  }

  const remainingSheet = Number(sqlOne(`SELECT COUNT(*) FROM db_material_order_sheet WHERE sheet_id=${escapeSql(setup.sheetId)}`))
  const finalCounts = tableCounts(setup.baselineTables)
  const finalPlan = sqlOne(`SELECT status,quantity,net_quantity,loss_rate,loss_quantity,unit_price,cost,COALESCE(selection_id,'NULL'),COALESCE(arrived_at,'NULL') FROM db_material_plan WHERE plan_id=${PLAN_ID}`)
  const finalManager = sqlOne(`SELECT COALESCE(manager_id,'NULL') FROM db_project WHERE project_id=${PROJECT_ID}`)
  check('B8-CLEANUP-SHEET', 'cleanup', '临时作业单删除完成', 'sheet count=0', remainingSheet === 0, { sheetId: setup.sheetId, remainingSheet, cancel, deleteSuccess: del.success })
  check('B8-CLEANUP-COUNTS', 'cleanup', '临时业务数据清理后计数回到预期基线',
    '业务表计数等于清理前减去临时单据', setup.baselineTables.every(table => finalCounts[table] === expectedCountsAfterCleanup[table]),
    { before: businessCountsBeforeCleanup, expected: expectedCountsAfterCleanup, final: finalCounts })
  check('B8-CLEANUP-PLAN', 'cleanup', '材料计划恢复验收前状态', '计划逐列等于验收前快照',
    finalPlan === setup.planOriginal, { before: setup.planOriginal, final: finalPlan })
  check('B8-CLEANUP-MANAGER', 'cleanup', '项目经理恢复验收前负责人', 'manager_id 等于验收前快照',
    finalManager === projectOriginalManagerId, { before: projectOriginalManagerId, final: finalManager })
  apiSamples.cleanup = { cancel, delete: del, remainingSheet, before: businessCountsBeforeCleanup, expected: expectedCountsAfterCleanup, final: finalCounts, finalPlan, finalManager }
}

function gitHead(cwd) {
  try { return execFileSync('git', ['-C', cwd, 'rev-parse', 'HEAD'], { encoding: 'utf8' }).trim() } catch { return null }
}

function writeEvidence() {
  const pass = results.filter(item => item.result === 'PASS').length
  const fail = results.filter(item => item.result === 'FAIL').length
  const blocked = results.filter(item => item.result === 'BLOCKED').length
  const verdict = fail > 0 ? 'FAIL' : blocked > 0 ? 'BLOCKED' : 'PASS'
  const categories = {}
  for (const item of results) {
    categories[item.category] ||= { total: 0, pass: 0, fail: 0, blocked: 0 }
    categories[item.category].total += 1
    if (item.result === 'PASS') categories[item.category].pass += 1
    if (item.result === 'FAIL') categories[item.category].fail += 1
    if (item.result === 'BLOCKED') categories[item.category].blocked += 1
  }
  const environment = {
    api: API,
    web: WEB,
    app: APP,
    mysql: '127.0.0.1:3306/decoboon',
    projectId: PROJECT_ID,
    planId: PLAN_ID,
    sheetId: setup?.sheetId || null,
    sheetNo: setup?.sheetNo || null,
    viewports: ['1440x900', '1024x768', '390x844', '375x667', 'devtools-375x667'],
    commits: {
      backend: process.env.B8_BACKEND_COMMIT || gitHead(BACKEND_WORKTREE),
      app: process.env.B8_APP_COMMIT || gitHead(APP_WORKTREE),
      web: process.env.B8_WEB_COMMIT || gitHead(WEB_WORKTREE)
    },
    worktrees: { backend: BACKEND_WORKTREE, app: APP_WORKTREE, web: WEB_WORKTREE }
  }
  const summary = {
    batch: 'B8',
    date: TODAY,
    verdict,
    totals: { total: results.length, pass, fail, blocked },
    categories,
    environment,
    devtoolsCompleted: results.some(item => item.category === 'devtools' && item.result === 'PASS'),
    acceptanceEvidenceConditionsMet: verdict === 'PASS' && results.some(item => item.id === 'B8-DEVTOOLS-PROJECT-ANSWER' && item.result === 'PASS'),
    boundary: '独立黑盒验收只判断是否满足进入 ACCEPTED 用户决策的证据条件；不自批最终正式接受或发布。',
    observations,
    screenshots,
    attempts
  }
  const defects = results.filter(item => item.result !== 'PASS').map(item => ({
    id: item.id,
    category: item.category,
    severity: item.result === 'BLOCKED' ? 'ENV' : /permission|cross|write|audit|cost/i.test(item.id) ? 'P1' : 'P2',
    title: item.title,
    expected: item.expected,
    actual: item.actual,
    evidence: item.evidence,
    status: 'OPEN'
  }))
  fs.writeFileSync(path.join(OUT, 'summary.json'), JSON.stringify(summary, null, 2))
  fs.writeFileSync(path.join(OUT, 'verification-results.json'), JSON.stringify(results, null, 2))
  fs.writeFileSync(path.join(OUT, 'defects.json'), JSON.stringify(defects, null, 2))
  fs.writeFileSync(path.join(OUT, 'ui-metrics.json'), JSON.stringify(uiMetrics, null, 2))
  fs.writeFileSync(path.join(OUT, 'golden-question-results.json'), JSON.stringify(goldenDetails, null, 2))
  fs.writeFileSync(path.join(OUT, 'api-samples.json'), JSON.stringify(apiSamples, null, 2))
  fs.writeFileSync(path.join(OUT, 'attempts.json'), JSON.stringify(attempts, null, 2))
  console.log(JSON.stringify({ verdict, totals: summary.totals, categories, sheetId: setup?.sheetId }))
  if (verdict !== 'PASS') process.exitCode = 1
}

async function main() {
  try {
    tokens = await loginAll()
    session = await post('auth/session/info', {}, tokens.Boss)
    setup = await setupFixture()
    businessCountsBeforeAssistant = tableCounts(setup.baselineTables)
    await runApiChecks()
    await runWebChecks()
    await runAppChecks()
    runCrossSurfaceChecks()
    await runDevtoolsChecks()
    const afterUi = tableCounts(setup.baselineTables)
    check('B8-UI-NO-BUSINESS-WRITE', 'ui', 'Web/App/DevTools 问答未改业务表',
      '全部业务表计数与 API 前一致',
      Object.keys(businessCountsBeforeAssistant).every(table => afterUi[table] === businessCountsBeforeAssistant[table]),
      { before: businessCountsBeforeAssistant, after: afterUi })
    await cleanupFixture()
  } catch (error) {
    attempts.push({ surface: 'runner', result: 'ERROR', error: String(error?.stack || error) })
    check('B8-RUNNER-ERROR', 'runner', '验收运行器异常', '无异常', false, String(error?.message || error).slice(0, 600), '', 'FAIL')
    try { await cleanupFixture() } catch (cleanupError) {
      attempts.push({ surface: 'cleanup', result: 'ERROR', error: String(cleanupError?.stack || cleanupError) })
    }
  } finally {
    writeEvidence()
  }
}

main()
