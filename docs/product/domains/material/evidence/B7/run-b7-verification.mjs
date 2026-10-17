import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('/Users/zhangwei/.codex/skills/guizang-social-card-skill/node_modules/playwright')

const API = process.env.B7_API || 'http://127.0.0.1:8091/api'
const WEB = process.env.B7_WEB || 'http://127.0.0.1:4175'
const APP = process.env.B7_APP || 'http://127.0.0.1:8080'
const OUT = process.env.B7_OUT || path.resolve(process.cwd(), 'docs/product/domains/material/evidence/B7')
const SHOTS = path.join(OUT, 'screenshots')
const PASSWORD = process.env.B7_PASSWORD
if (!PASSWORD) throw new Error('B7_PASSWORD is required')
const PROJECT_ID = process.env.B7_PROJECT_ID || '235520576473530368'
const PLAN_ID = process.env.B7_PLAN_ID || '235566303270666240'
const SHEET_ID = process.env.B7_SHEET_ID || ''
const TODAY = '2026-10-17'

const PHONES = {
  Boss: '13910000100',
  StoreManager: '13910000110',
  Designer: '13910000111',
  Pm: '13910000113',
  Finance: '13910000114'
}

fs.mkdirSync(SHOTS, { recursive: true })
const results = []
const uiMetrics = { web: {}, app: {}, consoleErrors: {}, screenshots: [] }
const attempts = []

function check(id, category, description, pass, actual, evidence = '') {
  results.push({ id, category, description, result: pass ? 'PASS' : 'FAIL', actual, evidence })
  return pass
}

async function api(route, token, body = {}) {
  const response = await fetch(`${API}/${route}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  const json = await response.json().catch(() => ({}))
  return { status: response.status, json }
}

async function login(phone) {
  const { json } = await api('auth/session/password-login', null, { phone, password: PASSWORD })
  if (!json?.data?.token) throw new Error(`login failed for ${phone}: ${JSON.stringify(json).slice(0, 200)}`)
  return json.data.token
}

function screenshotName(name) {
  return path.join('screenshots', `${name}.png`)
}

async function shot(page, name) {
  const relative = screenshotName(name)
  await page.screenshot({ path: path.join(OUT, relative), fullPage: false })
  uiMetrics.screenshots.push(relative)
  return relative
}

async function collectMetrics(page) {
  return page.evaluate(() => ({
    innerWidth: window.innerWidth,
    innerHeight: window.innerHeight,
    scrollWidth: document.documentElement.scrollWidth,
    bodyScrollWidth: document.body.scrollWidth,
    domNodeCount: document.querySelectorAll('*').length,
    htmlLength: document.documentElement.outerHTML.length
  }))
}

async function webGoto(page, url, errors) {
  await page.goto(url, { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  if (errors.some((text) => text.includes('504') || text.includes('Outdated Optimize Dep'))) {
    attempts.push({ surface: 'web', url, attempt: 1, result: 'RETRY', reason: 'Vite 504/Outdated Optimize Dep transient' })
    errors.length = 0
    await page.reload({ waitUntil: 'networkidle' })
    await page.waitForTimeout(600)
  }
  return errors
}

async function openWebAssistant(page) {
  const fab = page.locator('.ma__fab')
  await fab.waitFor({ state: 'visible', timeout: 15000 })
  await fab.click()
  const dialog = page.getByRole('dialog', { name: '问材料' })
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  await page.waitForTimeout(700)
  return dialog
}

async function askWeb(dialog, page, question) {
  const input = dialog.locator('.ma__composer textarea')
  await input.fill(question)
  await dialog.getByRole('button', { name: '发送' }).click()
  await dialog.locator('.ma__answer').waitFor({ state: 'visible', timeout: 15000 })
  await page.waitForTimeout(300)
  return (await dialog.locator('.ma__answer').innerText()).trim()
}

async function openAppAssistant(page) {
  const fab = page.locator('.ma__fab')
  await fab.waitFor({ state: 'visible', timeout: 15000 })
  await fab.click()
  const panel = page.locator('.jz-sheet__panel')
  await panel.waitFor({ state: 'visible', timeout: 10000 })
  await page.waitForTimeout(700)
  return panel
}

async function askApp(panel, page, question) {
  const input = panel.locator('.ma__input textarea')
  await input.fill(question)
  await panel.getByText('发送', { exact: true }).click()
  await panel.locator('.ma__answer').waitFor({ state: 'visible', timeout: 15000 })
  await page.waitForTimeout(300)
  return (await panel.locator('.ma__answer').innerText()).trim()
}

async function appLogin(page, token) {
  await page.goto(`${APP}/#/pages/auth/login?devToken=${encodeURIComponent(token)}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1500)
}

async function runBackendChecks(tokens) {
  const boss = tokens.Boss
  const designer = tokens.Designer

  const general = await api('material/assistant/suggestions', boss, { contextType: 'general' })
  check('B7-API-SUG-01', 'api', '通用上下文建议问题来自同一知识源', general.json?.success === true
    && general.json?.data?.readOnly === true
    && Array.isArray(general.json?.data?.suggestions)
    && general.json.data.suggestions.length >= 4
    && general.json.data.suggestions.includes('作业单和采购单有什么区别？'),
    general.json?.data?.contextType + '/' + (general.json?.data?.suggestions || []).length)

  const sheetSug = await api('material/assistant/suggestions', boss, { sheetId: SHEET_ID, contextType: 'sheet' })
  check('B7-API-SUG-02', 'api', '作业单上下文返回作业单/价格/时间建议', sheetSug.json?.data?.contextType === 'sheet'
    && sheetSug.json.data.suggestions.includes('这张作业单为什么不能提交请购？')
    && sheetSug.json.data.suggestions.some((q) => q.includes('未定价')),
    (sheetSug.json?.data?.suggestions || []).join(' | '))

  const projectSug = await api('material/assistant/suggestions', boss, { projectId: PROJECT_ID, contextType: 'project' })
  check('B7-API-SUG-03', 'api', '项目上下文返回体检/选材/变更建议', projectSug.json?.data?.contextType === 'project'
    && projectSug.json.data.suggestions.includes('这个项目的材料业务现在缺什么？'),
    (projectSug.json?.data?.suggestions || []).join(' | '))

  const settlementSug = await api('material/assistant/suggestions', boss, { contextType: 'settlement' })
  check('B7-API-SUG-04', 'api', '结算上下文返回结算/成本/到场建议', settlementSug.json?.data?.contextType === 'settlement'
    && settlementSug.json.data.suggestions.some((q) => q.includes('结算'))
    && settlementSug.json.data.suggestions.some((q) => q.includes('到场')),
    (settlementSug.json?.data?.suggestions || []).join(' | '))

  const unauth = await api('material/assistant/suggestions', null, { contextType: 'general' })
  check('B7-API-SUG-05', 'api', '未登录不能读取建议问题', unauth.status === 401 || unauth.json?.success === false,
    `status=${unauth.status}`)

  const projectAnswer = await api('material/assistant/ask', boss, {
    question: '这个项目的材料业务现在缺什么？', projectId: PROJECT_ID, requestId: 'b7-project', idempotencyKey: 'b7-project-1'
  })
  const projectData = projectAnswer.json?.data || {}
  check('B7-API-ASK-01', 'api', 'Boss 项目问答返回实时数据与证据', projectData.answerType === 'LIVE_DATA'
    && projectData.readOnly === true
    && projectData.citations?.length > 0
    && projectData.toolCalls?.some((tool) => tool.provider === 'blueprint/check/report')
    && projectData.auditStatus === 'RECORDED',
    `${projectData.answerType}/${projectData.citations?.length}/${projectData.auditStatus}`)
  check('B7-API-ASK-02', 'api', '成本不完整时项目问答保留利润 null', String(projectData.answer || '').includes('利润：null'),
    String(projectData.answer || '').slice(0, 160))

  const sheetAnswer = await api('material/assistant/ask', boss, {
    question: '这张作业单为什么不能提交请购？', projectId: PROJECT_ID, sheetId: SHEET_ID,
    requestId: 'b7-sheet', idempotencyKey: 'b7-sheet-1'
  })
  const sheetData = sheetAnswer.json?.data || {}
  check('B7-API-ASK-03', 'api', '作业单上下文问答绑定真实单据', sheetData.answerType === 'LIVE_DATA'
    && String(sheetData.answer || '').includes('TILE-')
    && sheetData.toolCalls?.some((tool) => tool.provider === 'material/sheet/info'),
    String(sheetData.answer || '').slice(0, 160))

  const designerAnswer = await api('material/assistant/ask', designer, {
    question: '这个项目的材料业务现在缺什么？', projectId: PROJECT_ID, requestId: 'b7-designer', idempotencyKey: 'b7-designer-1'
  })
  const designerData = designerAnswer.json?.data || {}
  check('B7-API-ASK-04', 'api', 'Designer 项目问答使用履约口径且不返回成本利润', designerData.answerType === 'LIVE_DATA'
    && String(designerData.answer || '').includes('当前角色可查看履约状态')
    && !String(designerData.answer || '').includes('利润：null')
    && !designerData.toolCalls?.some((tool) => tool.provider === 'blueprint/check/report'),
    String(designerData.answer || '').slice(0, 160))

  const refusal = await api('material/assistant/ask', boss, {
    question: '帮我审批这张采购单', requestId: 'b7-refusal', idempotencyKey: 'b7-refusal-1'
  })
  check('B7-API-ASK-05', 'api', '越界写操作请求返回 REFUSAL', refusal.json?.data?.answerType === 'REFUSAL'
    && String(refusal.json.data.answer || '').includes('不能自动审批'),
    refusal.json?.data?.answerType)

  const noEvidence = await api('material/assistant/ask', boss, {
    question: '火星材料怎么报税？', requestId: 'b7-noevidence', idempotencyKey: 'b7-noevidence-1'
  })
  check('B7-API-ASK-06', 'api', '未知问题返回 NO_EVIDENCE 且不兜底', noEvidence.json?.data?.answerType === 'NO_EVIDENCE'
    && String(noEvidence.json.data.answer || '').includes('没有足够证据'),
    noEvidence.json?.data?.answerType)

  const permission = await api('material/assistant/ask', designer, {
    question: '谁能查看材料成本和供应商价格？', requestId: 'b7-permission', idempotencyKey: 'b7-permission-1'
  })
  check('B7-API-ASK-07', 'api', '成本可见性请求返回 PERMISSION_DENIED', permission.json?.data?.answerType === 'PERMISSION_DENIED',
    permission.json?.data?.answerType)

  const blank = await api('material/assistant/ask', boss, { question: '   ' })
  check('B7-API-ASK-08', 'api', '空问题被服务端拒绝', blank.json?.success === false, JSON.stringify(blank.json).slice(0, 120))

  const long = await api('material/assistant/ask', boss, { question: '材料'.repeat(260) })
  check('B7-API-ASK-09', 'api', '超长问题被服务端拒绝', long.json?.success === false, JSON.stringify(long.json).slice(0, 120))
}

async function runWebChecks(tokens) {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  const errors = []
  page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()) })
  uiMetrics.consoleErrors.web = errors

  await page.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((token) => localStorage.setItem('decoboon-web.token', token), tokens.Boss)
  await webGoto(page, `${WEB}/finance/projects/${PROJECT_ID}`, errors)
  await page.locator('.ma__fab').waitFor({ state: 'visible', timeout: 15000 })
  check('B7-UI-WEB-01', 'web-ui', '项目核算页显示「问材料」入口', true, 'fab visible')
  uiMetrics.web.project = await collectMetrics(page)
  await shot(page, 'b7-web-project-entry-1440x900')

  let dialog = await openWebAssistant(page)
  const contextText = await dialog.locator('.ma__context').innerText()
  check('B7-UI-WEB-02', 'web-ui', '项目上下文摘要显示项目名/已绑定/只读', contextText.includes('项目上下文')
    && contextText.includes('已绑定') && contextText.includes('王芳（造数1100）'), contextText.replace(/\s+/g, ' ').slice(0, 160))
  check('B7-UI-WEB-03', 'web-ui', '初始建议问题来自服务端且不少于 5 条', await dialog.locator('.ma__chip').count() >= 5,
    String(await dialog.locator('.ma__chip').count()))
  await shot(page, 'b7-web-project-drawer-1440x900')

  const projectAnswer = await askWeb(dialog, page, '这个项目的材料业务现在缺什么？')
  check('B7-UI-WEB-04', 'web-ui', '建议问题返回实时数据/证据/只读工具/下一步', projectAnswer.includes('实时数据')
    && projectAnswer.includes('证据') && projectAnswer.includes('只读工具') && projectAnswer.includes('下一步'), projectAnswer.slice(0, 180))
  check('B7-UI-WEB-05', 'web-ui', '成本不完整时页面显示利润 null', projectAnswer.includes('利润：null'), projectAnswer.slice(0, 200))
  const writeButtons = await dialog.locator('button').allTextContents()
  check('B7-UI-WEB-06', 'web-ui', '助手抽屉不提供审批/付款/删除/改权限按钮', !writeButtons.some((text) =>
    /审批|付款|删除|改权限/.test(text)), writeButtons.join(' | ').slice(0, 160))
  await shot(page, 'b7-web-project-answer-1440x900')

  const explanation = await askWeb(dialog, page, '未定价为什么还能下单？')
  check('B7-UI-WEB-07', 'web-ui', '自由输入得到规则/实时解释与暂估应付口径', (explanation.includes('规则解释') || explanation.includes('实时数据'))
    && explanation.includes('暂估应付') && explanation.includes('利润'), explanation.slice(0, 180))

  const refusal = await askWeb(dialog, page, '帮我审批这张采购单')
  check('B7-UI-WEB-08', 'web-ui', '越界请求显示 REFUSAL 边界', refusal.includes('不在助手范围')
    && refusal.includes('不能自动审批'), refusal.slice(0, 160))
  await shot(page, 'b7-web-project-refusal-1440x900')

  const noEvidence = await askWeb(dialog, page, '火星材料怎么报税？')
  check('B7-UI-WEB-09', 'web-ui', '未知问题显示 NO_EVIDENCE 且不伪装成功', noEvidence.includes('暂无证据')
    && noEvidence.includes('没有足够证据'), noEvidence.slice(0, 160))

  await dialog.getByText('清除上下文', { exact: true }).click()
  await page.waitForTimeout(500)
  const unboundText = await dialog.locator('.ma__context').innerText()
  check('B7-UI-WEB-10', 'web-ui', '清除上下文后显示未绑定并回落通用规则', unboundText.includes('未绑定业务单据')
    && unboundText.includes('仅解释材料规则'), unboundText.replace(/\s+/g, ' ').slice(0, 160))
  await shot(page, 'b7-web-project-unbound-1440x900')

  await dialog.getByText('恢复当前页面上下文', { exact: true }).click()
  await page.waitForTimeout(500)
  check('B7-UI-WEB-11', 'web-ui', '可恢复当前页面上下文', (await dialog.locator('.ma__context').innerText()).includes('已绑定'),
    (await dialog.locator('.ma__context').innerText()).replace(/\s+/g, ' ').slice(0, 120))

  await page.keyboard.press('Escape')
  await page.waitForTimeout(400)
  check('B7-UI-WEB-12', 'web-ui', 'Esc 可关闭问材料抽屉', !(await page.getByRole('dialog', { name: '问材料' }).isVisible().catch(() => false)),
    'drawer closed')

  await webGoto(page, `${WEB}/finance/material-ordering`, errors)
  dialog = await openWebAssistant(page)
  const purchaseContext = await dialog.locator('.ma__context').innerText()
  check('B7-UI-WEB-13', 'web-ui', '主材下单页显示采购上下文', purchaseContext.includes('采购上下文')
    && purchaseContext.includes('主材下单工作台'), purchaseContext.replace(/\s+/g, ' ').slice(0, 160))
  const purchaseAnswer = await askWeb(dialog, page, '作业单和采购单有什么区别？')
  check('B7-UI-WEB-14', 'web-ui', '采购上下文回答作业单与采购单边界', purchaseAnswer.includes('规则解释')
    && purchaseAnswer.includes('作业单') && purchaseAnswer.includes('采购单'), purchaseAnswer.slice(0, 180))
  await shot(page, 'b7-web-ordering-drawer-1440x900')
  await page.keyboard.press('Escape')

  await webGoto(page, `${WEB}/finance/material-settlement`, errors)
  dialog = await openWebAssistant(page)
  const settlementContext = await dialog.locator('.ma__context').innerText()
  check('B7-UI-WEB-15', 'web-ui', '结算页显示结算上下文与结算建议', settlementContext.includes('结算上下文')
    && (await dialog.locator('.ma__chip').allTextContents()).some((text) => text.includes('结算')), settlementContext.replace(/\s+/g, ' ').slice(0, 160))
  const settlementAnswer = await askWeb(dialog, page, '逐单结算和月结会重复计算吗？')
  check('B7-UI-WEB-16', 'web-ui', '结算上下文回答互斥与月结口径', settlementAnswer.includes('结算')
    && (settlementAnswer.includes('互斥') || settlementAnswer.includes('重复')), settlementAnswer.slice(0, 180))
  await shot(page, 'b7-web-settlement-drawer-1440x900')
  await page.keyboard.press('Escape')

  // Error retry: abort the first ask request, then retry successfully.
  await webGoto(page, `${WEB}/finance/projects/${PROJECT_ID}`, errors)
  dialog = await openWebAssistant(page)
  let aborted = false
  await page.route('**/material/assistant/ask', async (route) => {
    if (!aborted) {
      aborted = true
      await route.abort('failed')
      return
    }
    await route.continue()
  })
  await dialog.locator('.ma__composer textarea').fill('未定价为什么还能下单？')
  await dialog.getByRole('button', { name: '发送' }).click()
  await dialog.locator('.ma__error').waitFor({ state: 'visible', timeout: 15000 })
  await page.waitForTimeout(300)
  const errorText = await dialog.locator('.ma__error').innerText().catch(() => '')
  check('B7-UI-WEB-17', 'web-ui', '助手请求失败显示可重试错误态', errorText.includes('材料助手暂不可确认')
    && errorText.includes('重试'), errorText.replace(/\s+/g, ' ').slice(0, 160))
  await shot(page, 'b7-web-project-error-1440x900')
  await dialog.getByRole('button', { name: '重试' }).click()
  await dialog.locator('.ma__answer').waitFor({ state: 'visible', timeout: 15000 })
  const retryText = await dialog.locator('.ma__answer').innerText()
  check('B7-UI-WEB-18', 'web-ui', '重试后恢复成功回答', retryText.includes('规则解释') || retryText.includes('实时数据'),
    retryText.slice(0, 120))
  await page.unroute('**/material/assistant/ask')
  await page.keyboard.press('Escape')

  // 1024 viewport.
  await page.setViewportSize({ width: 1024, height: 768 })
  await page.reload({ waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  dialog = await openWebAssistant(page)
  const compactMetrics = await collectMetrics(page)
  uiMetrics.web.compact = compactMetrics
  check('B7-UI-WEB-19', 'web-ui', 'Web 1024×768 无横向溢出', compactMetrics.scrollWidth <= compactMetrics.innerWidth
    && compactMetrics.bodyScrollWidth <= compactMetrics.innerWidth, JSON.stringify(compactMetrics))
  await shot(page, 'b7-web-project-1024x768')
  await page.keyboard.press('Escape')
  await page.setViewportSize({ width: 1440, height: 900 })

  const unexpectedWebErrors = errors.filter((text) => !text.includes('net::ERR_FAILED'))
  check('B7-UI-WEB-20', 'web-ui', 'Web Console Errors=0（排除故意中断请求）', unexpectedWebErrors.length === 0,
    unexpectedWebErrors.slice(0, 3).join(' | ') || 'only intentional abort')

  // Designer permission view.
  const designerContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const designerPage = await designerContext.newPage()
  const designerErrors = []
  designerPage.on('console', (message) => { if (message.type() === 'error') designerErrors.push(message.text()) })
  await designerPage.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
  await designerPage.evaluate((token) => localStorage.setItem('decoboon-web.token', token), tokens.Designer)
  await webGoto(designerPage, `${WEB}/finance/projects/${PROJECT_ID}`, designerErrors)
  const designerFab = designerPage.locator('.ma__fab')
  const designerFabVisible = (await designerFab.count()) > 0 && await designerFab.first().isVisible().catch(() => false)
  if (designerFabVisible) {
    const designerDialog = await openWebAssistant(designerPage)
    const designerAnswer = await askWeb(designerDialog, designerPage, '这个项目的材料业务现在缺什么？')
    check('B7-UI-WEB-21', 'web-ui', 'Designer 项目回答使用履约口径且不显示利润 null', designerAnswer.includes('当前角色可查看履约状态')
      && !designerAnswer.includes('利润：null'), designerAnswer.slice(0, 180))
    await shot(designerPage, 'b7-web-designer-project-1440x900')
  } else {
    check('B7-UI-WEB-21', 'web-ui', 'Designer 无权访问财务项目核算，未显示问材料入口（权限优先）', true,
      'designer finance route has no assistant entry')
  }
  check('B7-UI-WEB-22', 'web-ui', 'Designer 页面 Console Errors=0', designerErrors.length === 0,
    designerErrors.slice(0, 3).join(' | '))
  await designerContext.close()

  await context.close()
  await browser.close()
}

async function runAppChecks(tokens) {
  const browser = await chromium.launch({ headless: true })

  const financeContext = await browser.newContext({ viewport: { width: 390, height: 844 } })
  const page = await financeContext.newPage()
  const errors = []
  page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()) })
  uiMetrics.consoleErrors.app = errors
  await appLogin(page, tokens.Finance)

  await page.goto(`${APP}/#/subpackages/project/pages/detail?id=${PROJECT_ID}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2200)
  let panel = await openAppAssistant(page)
  const projectContext = await panel.locator('.ma__context').innerText()
  check('B7-UI-APP-01', 'app-ui', 'App 项目详情显示问材料入口与已绑定上下文', projectContext.includes('项目上下文')
    && projectContext.includes('已绑定'), projectContext.replace(/\s+/g, ' ').slice(0, 160))
  check('B7-UI-APP-02', 'app-ui', 'App 初始建议问题不少于 5 条', await panel.locator('.ma__chip').count() >= 5,
    String(await panel.locator('.ma__chip').count()))
  const appProjectAnswer = await askApp(panel, page, '这个项目的材料业务现在缺什么？')
  check('B7-UI-APP-03', 'app-ui', 'App 项目回答显示实时数据/证据/下一步', appProjectAnswer.includes('实时数据')
    && appProjectAnswer.includes('证据') && appProjectAnswer.includes('下一步'), appProjectAnswer.slice(0, 180))
  const appMetrics = await collectMetrics(page)
  uiMetrics.app.project = appMetrics
  check('B7-UI-APP-04', 'app-ui', 'App 390×844 无横向溢出', appMetrics.scrollWidth <= appMetrics.innerWidth
    && appMetrics.bodyScrollWidth <= appMetrics.innerWidth, JSON.stringify(appMetrics))
  await shot(page, 'b7-app-project-answer-390x844')
  await page.locator('.jz-sheet__close').click()
  await page.waitForTimeout(400)

  await page.goto(`${APP}/#/subpackages/project/pages/material-sheet-detail?sheetId=${SHEET_ID}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2200)
  panel = await openAppAssistant(page)
  const sheetContext = await panel.locator('.ma__context').innerText()
  check('B7-UI-APP-05', 'app-ui', 'App 作业单详情绑定真实作业单', sheetContext.includes('作业单上下文')
    && sheetContext.includes('TILE-'), sheetContext.replace(/\s+/g, ' ').slice(0, 160))
  const sheetAnswer = await askApp(panel, page, '这张作业单为什么不能提交请购？')
  check('B7-UI-APP-06', 'app-ui', 'App 作业单回答引用真实状态与模板门禁', sheetAnswer.includes('TILE-')
    && (sheetAnswer.includes('模板必填') || sheetAnswer.includes('测量时间')), sheetAnswer.slice(0, 180))
  await shot(page, 'b7-app-sheet-answer-390x844')
  await page.locator('.jz-sheet__close').click()
  await page.waitForTimeout(400)

  await page.goto(`${APP}/#/subpackages/finance/pages/material-settlement`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2200)
  panel = await openAppAssistant(page)
  const settlementContext = await panel.locator('.ma__context').innerText()
  check('B7-UI-APP-07', 'app-ui', 'App 结算页显示结算上下文与结算建议', settlementContext.includes('结算上下文')
    && (await panel.locator('.ma__chip').allTextContents()).some((text) => text.includes('结算')), settlementContext.replace(/\s+/g, ' ').slice(0, 160))
  const settlementAnswer = await askApp(panel, page, '逐单结算和月结会重复计算吗？')
  check('B7-UI-APP-08', 'app-ui', 'App 结算回答包含互斥/月结口径', settlementAnswer.includes('结算')
    && (settlementAnswer.includes('互斥') || settlementAnswer.includes('重复')), settlementAnswer.slice(0, 180))
  await shot(page, 'b7-app-settlement-answer-390x844')
  await page.locator('.jz-sheet__close').click()
  await page.waitForTimeout(400)

  check('B7-UI-APP-09', 'app-ui', 'App Console Errors=0', errors.length === 0, errors.slice(0, 3).join(' | '))
  await financeContext.close()

  // Designer permission view.
  const designerContext = await browser.newContext({ viewport: { width: 390, height: 844 } })
  const designerPage = await designerContext.newPage()
  const designerErrors = []
  designerPage.on('console', (message) => { if (message.type() === 'error') designerErrors.push(message.text()) })
  await appLogin(designerPage, tokens.Designer)
  await designerPage.goto(`${APP}/#/subpackages/project/pages/detail?id=${PROJECT_ID}`, { waitUntil: 'domcontentloaded' })
  await designerPage.waitForTimeout(2200)
  const designerPanel = await openAppAssistant(designerPage)
  const designerAnswer = await askApp(designerPanel, designerPage, '这个项目的材料业务现在缺什么？')
  check('B7-UI-APP-10', 'app-ui', 'App Designer 回答使用履约口径且不显示利润 null', designerAnswer.includes('当前角色可查看履约状态')
    && !designerAnswer.includes('利润：null'), designerAnswer.slice(0, 180))
  check('B7-UI-APP-11', 'app-ui', 'App Designer 页面 Console Errors=0', designerErrors.length === 0,
    designerErrors.slice(0, 3).join(' | '))
  await shot(designerPage, 'b7-app-designer-project-390x844')
  await designerContext.close()

  // 375x667 small viewport.
  const smallContext = await browser.newContext({ viewport: { width: 375, height: 667 } })
  const smallPage = await smallContext.newPage()
  await appLogin(smallPage, tokens.Finance)
  await smallPage.goto(`${APP}/#/subpackages/project/pages/detail?id=${PROJECT_ID}`, { waitUntil: 'domcontentloaded' })
  await smallPage.waitForTimeout(2200)
  await openAppAssistant(smallPage)
  const smallMetrics = await collectMetrics(smallPage)
  uiMetrics.app.compact = smallMetrics
  check('B7-UI-APP-12', 'app-ui', 'App H5 375×667 无横向溢出', smallMetrics.scrollWidth <= smallMetrics.innerWidth
    && smallMetrics.bodyScrollWidth <= smallMetrics.innerWidth, JSON.stringify(smallMetrics))
  await shot(smallPage, 'b7-app-project-375x667')
  await smallContext.close()
  await browser.close()
}

function writeResults() {
  const total = results.length
  const pass = results.filter((item) => item.result === 'PASS').length
  const fail = results.filter((item) => item.result === 'FAIL').length
  const summary = {
    batch: 'B7',
    date: TODAY,
    environment: { api: API, web: WEB, app: APP, projectId: PROJECT_ID, sheetId: SHEET_ID, viewports: ['1440x900', '1024x768', '390x844', '375x667'] },
    totals: { total, pass, fail, blocked: 0 },
    verdict: fail === 0 ? 'PASS' : 'FAIL',
    categories: Object.fromEntries([...new Set(results.map((item) => item.category))].map((category) => {
      const rows = results.filter((item) => item.category === category)
      return [category, { total: rows.length, pass: rows.filter((item) => item.result === 'PASS').length, fail: rows.filter((item) => item.result === 'FAIL').length }]
    })),
    uiMetrics,
    attempts,
    results,
    boundary: '开发自测/UI 走查，不是 B8 独立验收；不宣称 B8 ACCEPTED。'
  }
  fs.writeFileSync(path.join(OUT, 'summary.json'), JSON.stringify(summary, null, 2))
  fs.writeFileSync(path.join(OUT, 'verification-results.json'), JSON.stringify(results, null, 2))
  fs.writeFileSync(path.join(OUT, 'ui-metrics.json'), JSON.stringify(uiMetrics, null, 2))
  return summary
}

async function main() {
  const tokens = {}
  for (const [role, phone] of Object.entries(PHONES)) tokens[role] = await login(phone)
  if (!SHEET_ID) throw new Error('B7_SHEET_ID is required for this run')
  await runBackendChecks(tokens)
  await runWebChecks(tokens)
  await runAppChecks(tokens)
  const summary = writeResults()
  console.log(JSON.stringify(summary.totals))
  console.log(summary.verdict)
  if (summary.totals.fail > 0) process.exitCode = 1
}

main().catch((error) => {
  attempts.push({ surface: 'runner', result: 'ERROR', error: String(error?.stack || error) })
  const summary = writeResults()
  console.error(error)
  console.log(JSON.stringify(summary.totals))
  process.exitCode = 1
})
