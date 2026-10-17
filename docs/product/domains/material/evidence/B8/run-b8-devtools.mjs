import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator')

const API = process.env.B8_API || 'http://127.0.0.1:8091/api'
const OUT = process.env.B8_OUT || path.resolve('docs/product/domains/material/evidence/B8')
const PROJECT_ID = process.env.B8_PROJECT_ID || '235520576473530368'
const SHEET_ID = process.env.B8_SHEET_ID
const PASSWORD = process.env.B8_PASSWORD
const DEVTOOLS_PROJECT = process.env.B8_DEVTOOLS_PROJECT || '/tmp/th4-b8-app-67e6da0/dist/dev/mp-weixin'
const CLI = process.env.B8_DEVTOOLS_CLI || '/Applications/wechatwebdevtools.app/Contents/MacOS/cli'
const PORT = Number(process.env.B8_DEVTOOLS_PORT || 9432)
const SHOTS = path.join(OUT, 'screenshots')
const RESULT_PATH = path.join(OUT, 'devtools-results.json')
const SHEET_QUESTION = '这张作业单现在是什么状态？'

if (!PASSWORD) throw new Error('B8_PASSWORD is required')
if (!SHEET_ID) throw new Error('B8_SHEET_ID is required')
fs.mkdirSync(SHOTS, { recursive: true })

const results = []
const consoleEvents = []
const screenshots = []
const metrics = {}
let mp = null

function check(id, title, expected, condition, actual) {
  results.push({ id, category: 'devtools', title, expected, actual, result: condition ? 'PASS' : 'FAIL', evidence: screenshots.join(',') })
}

function normalized(value) {
  return String(value || '').replace(/\s+/g, ' ').trim()
}

async function api(route, body = {}, token) {
  const response = await fetch(`${API}/${route}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(body)
  })
  return { httpStatus: response.status, ...(await response.json().catch(() => ({}))) }
}

async function bossToken() {
  const response = await api('auth/session/password-login', { phone: '13910000100', password: PASSWORD })
  if (!response?.data?.token) throw new Error(`Boss login failed: ${JSON.stringify(response).slice(0, 200)}`)
  return response.data.token
}

async function findAssistant(page) {
  const all = await page.$$('[class]')
  const components = all.filter(element => element.tagName === 'component')
  for (let i = components.length - 1; i >= 0; i -= 1) {
    const wxml = await components[i].outerWxml().catch(() => '')
    if (wxml.includes('components/MaterialAssistant/MaterialAssistant')) return components[i]
  }
  return null
}

async function openAssistant(page) {
  const assistant = await findAssistant(page)
  if (!assistant) throw new Error('MaterialAssistant component host not found')
  const fab = await assistant.$('.ma__fab')
  if (!fab) throw new Error('MaterialAssistant fab not found')
  await fab.tap()
  await page.waitFor(1000)
  const context = await assistant.$('.ma__context')
  if (!context) throw new Error('MaterialAssistant context not found after open')
  return { assistant, context, chips: await assistant.$$('.ma__chip') }
}

async function screenshot(name) {
  const relative = path.join('screenshots', `${name}.png`)
  await mp.screenshot({ path: path.join(OUT, relative) })
  screenshots.push(relative)
  return relative
}

async function loginPage(token) {
  await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(token)}`)
  await new Promise(resolve => setTimeout(resolve, 2600))
}

async function main() {
  const token = await bossToken()
  mp = await automator.launch({
    cliPath: CLI,
    projectPath: DEVTOOLS_PROJECT,
    port: PORT,
    timeout: 90000,
    trustProject: true,
    projectConfig: { setting: { urlCheck: false } }
  })
  mp.on('console', message => consoleEvents.push({ type: message.type, args: message.args }))
  mp.on('exception', error => consoleEvents.push({ type: 'exception', error }))

  const system = await mp.systemInfo()
  metrics.system = system
  check('B8-DEVTOOLS-LAUNCH', '微信开发者工具 automator 启动并连接', 'miniprogram-automator 连接成功',
    true, { port: PORT, project: DEVTOOLS_PROJECT, system })
  check('B8-DEVTOOLS-VIEWPORT', '真实模拟器尺寸为 375×667', 'windowWidth=375, windowHeight=667',
    system.windowWidth === 375 && system.windowHeight === 667,
    { windowWidth: system.windowWidth, windowHeight: system.windowHeight, SDKVersion: system.SDKVersion })

  await loginPage(token)
  const projectPage = await mp.reLaunch(`/subpackages/project/pages/detail?id=${PROJECT_ID}`)
  await projectPage.waitFor(3200)
  check('B8-DEVTOOLS-PROJECT-ENTRY', '真实进入项目详情', '页面路由为 subpackages/project/pages/detail',
    projectPage.path === 'subpackages/project/pages/detail', { path: projectPage.path, query: projectPage.query })

  const projectAssistant = await openAssistant(projectPage)
  const projectContext = normalized(await projectAssistant.context.text())
  const projectChips = (await projectAssistant.chips).map(element => element.text())
  const chipTexts = await Promise.all(projectChips)
  check('B8-DEVTOOLS-PROJECT-CONTEXT', '微信小程序项目上下文已绑定', '含项目上下文/已绑定/真实项目名',
    projectContext.includes('项目上下文') && projectContext.includes('已绑定') && projectContext.includes('王芳（造数1100）'), projectContext)
  check('B8-DEVTOOLS-PROJECT-SUGGESTIONS', '微信小程序建议问题来自服务端', '至少 5 条且含项目体检问题',
    chipTexts.length >= 5 && chipTexts.some(question => question.includes('材料业务现在缺什么')), chipTexts)
  await screenshot('b8-devtools-project-drawer-375x667')

  await projectAssistant.chips[0].tap()
  await projectPage.waitFor(2200)
  const projectAnswer = await projectAssistant.assistant.$('.ma__answer')
  const projectAnswerPanel = normalized(await projectAnswer.text())
  check('B8-DEVTOOLS-PROJECT-ANSWER', '微信小程序项目问答完整展示', '含实时数据/证据/只读工具/下一步/审计/利润 null',
    ['实时数据', '证据', '只读工具', '下一步', '只读', '已审计', '利润：null'].every(text => projectAnswerPanel.includes(text))
      && projectAnswerPanel.includes('CHK-UNSELECTED') && projectAnswerPanel.includes('CHK-COST-INCOMPLETE'),
    projectAnswerPanel.slice(0, 520))
  await screenshot('b8-devtools-project-answer-375x667')

  const sheetPage = await mp.reLaunch(`/subpackages/project/pages/material-sheet-detail?sheetId=${SHEET_ID}`)
  await sheetPage.waitFor(3200)
  check('B8-DEVTOOLS-SHEET-ENTRY', '真实进入作业单详情', '页面路由为 material-sheet-detail',
    sheetPage.path === 'subpackages/project/pages/material-sheet-detail', { path: sheetPage.path, query: sheetPage.query })
  const sheetAssistant = await openAssistant(sheetPage)
  const sheetContext = normalized(await sheetAssistant.context.text())
  check('B8-DEVTOOLS-SHEET-CONTEXT', '微信小程序作业单上下文绑定真实 sheetId', '含作业单上下文/已绑定/真实 sheetNo',
    sheetContext.includes('作业单上下文') && sheetContext.includes('已绑定') && /TILE-\d{8}-\d{3}/.test(sheetContext), sheetContext)
  await sheetAssistant.chips[0].tap()
  await sheetPage.waitFor(2200)
  const sheetAnswer = await sheetAssistant.assistant.$('.ma__answer')
  const sheetAnswerPanel = normalized(await sheetAnswer.text())
  check('B8-DEVTOOLS-SHEET-ANSWER', '微信小程序作业单回答显示证据/只读/下一步', '含真实 sheetNo、证据、只读工具、下一步',
    /TILE-\d{8}-\d{3}/.test(sheetAnswerPanel) && ['证据', '只读工具', '下一步', '只读'].every(text => sheetAnswerPanel.includes(text)),
    sheetAnswerPanel.slice(0, 520))
  await screenshot('b8-devtools-sheet-answer-375x667')

  // Send a free-form question in the same real sheet context, proving the service binding is active.
  const textarea = await sheetAssistant.assistant.$('.ma__input')
  if (textarea) {
    await textarea.input(SHEET_QUESTION)
    const send = await sheetAssistant.assistant.$('.ma__composer button')
    if (send) {
      await send.tap()
      await sheetPage.waitFor(2200)
      const typedAnswer = normalized(await (await sheetAssistant.assistant.$('.ma__answer')).text())
      check('B8-DEVTOOLS-FREEFORM', '微信小程序单轮自由输入可发送并返回上下文回答', '回答含真实 sheetNo 与只读提示',
        typedAnswer.includes('TILE-') && typedAnswer.includes('只读'), typedAnswer.slice(0, 420))
      await screenshot('b8-devtools-sheet-freeform-375x667')
    } else {
      check('B8-DEVTOOLS-FREEFORM', '微信小程序单轮自由输入可发送并返回上下文回答', '发送按钮可用', false, 'send button missing')
    }
  } else {
    check('B8-DEVTOOLS-FREEFORM', '微信小程序单轮自由输入可发送并返回上下文回答', '输入框可用', false, 'textarea missing')
  }

  const errors = consoleEvents.filter(event => event.type === 'error' || event.type === 'exception')
  metrics.consoleEvents = consoleEvents
  check('B8-DEVTOOLS-CONSOLE', '微信开发者工具业务 Console Errors=0', 'error/exception=0',
    errors.length === 0, consoleEvents)

  fs.writeFileSync(RESULT_PATH, JSON.stringify({
    results,
    metrics,
    consoleErrors: errors,
    consoleWarnings: consoleEvents.filter(event => event.type === 'warn'),
    screenshots,
    system
  }, null, 2))
  console.log(JSON.stringify({ total: results.length, pass: results.filter(item => item.result === 'PASS').length, fail: results.filter(item => item.result === 'FAIL').length, screenshots }))
}

try {
  await main()
} catch (error) {
  check('B8-DEVTOOLS-RUNNER', '微信开发者工具验收执行', '无运行异常', false, String(error?.stack || error).slice(0, 1200))
  fs.writeFileSync(RESULT_PATH, JSON.stringify({
    results,
    metrics,
    consoleErrors: consoleEvents.filter(event => event.type === 'error' || event.type === 'exception'),
    consoleWarnings: consoleEvents.filter(event => event.type === 'warn'),
    screenshots,
    system: metrics.system || null,
    runnerError: String(error?.stack || error)
  }, null, 2))
  process.exitCode = 1
} finally {
  try { await mp?.close() } catch { /* IDE may already be closed */ }
}
