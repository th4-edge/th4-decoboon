import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'

const APP = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const ROOT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon'
const EVID = resolve(ROOT, 'docs/acceptance/runs/20261022-emg-v2-employee-lifecycle')
const SHOTS = resolve(EVID, 'screenshots')
const API = 'http://127.0.0.1:8090/api/'
const require = createRequire(resolve(APP, 'package.json'))
const automator = require(resolve(APP, 'node_modules/miniprogram-automator/out/index.js'))
const wait = (ms) => new Promise((r) => setTimeout(r, ms))
const result = { at: new Date().toISOString(), steps: [], pass: false, consoleErrors: [] }
let mp
async function post(route, data) {
  const response = await fetch(`${API}${route}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })
  return response.json()
}
async function currentPageSafe(tries = 30) {
  for (let i = 0; i < tries; i++) {
    try { return await mp.currentPage() } catch { await wait(200) }
  }
  throw new Error('无法读取当前页面')
}
async function texts(page) {
  const values = []
  for (const element of await page.$$('*')) {
    try { const t = (await element.text()).trim(); if (t) values.push(t) } catch {}
  }
  return [...new Set(values)]
}
async function launch(url) {
  try { await mp.evaluate((u) => wx.navigateTo({ url: u }), url) } catch {}
  await wait(2200)
  return currentPageSafe()
}
try {
  for (let i = 0; i < 20; i++) {
    try { mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' }); break } catch { await wait(500) }
  }
  if (!mp) throw new Error('无法连接 miniprogram-automator')
  mp.on('console', (event) => { if (event.type === 'error') result.consoleErrors.push(event) })
  mp.on('exception', (event) => result.consoleErrors.push(event))

  const phone = process.env.EMG_ACCEPT_PHONE
  const password = process.env.EMG_ACCEPT_PASSWORD
  if (!phone || !password) throw new Error('缺少 EMG_ACCEPT_PHONE / EMG_ACCEPT_PASSWORD')
  const login = await post('auth/session/password-login', { phone, password })
  if (!login.success || !login.data?.token) throw new Error(`店长登录失败: ${JSON.stringify(login.errorCode)}`)
  try { await mp.evaluate((url) => wx.reLaunch({ url }), `/pages/auth/login?devToken=${encodeURIComponent(login.data.token)}`) } catch {}
  await wait(2200)
  // 首页存在其他域的已知 store report 403；本批次只统计员工生命周期页面自身错误。
  result.consoleErrors.length = 0

  let page = await launch('/subpackages/company/pages/members')
  await wait(1200)
  const cards = await page.$$('.member-card')
  const rows = []
  for (const card of cards) {
    const text = await card.text()
    const actions = []
    for (const action of await card.$$('.member-action')) actions.push((await action.text()).trim())
    rows.push({ text, actions })
  }
  const managerRow = rows.find((r) => r.text.includes('验收店长'))
  const bossRow = rows.find((r) => r.text.includes('验收老板'))
  const marketerRow = rows.find((r) => r.text.includes('验收项目经理'))
  result.steps.push({ step: 'members', path: page.path, rowCount: rows.length, managerActions: managerRow?.actions || [], bossActions: bossRow?.actions || [], marketerActions: marketerRow?.actions || [] })
  await mp.screenshot({ path: resolve(SHOTS, 'app-storemanager-members.png') })

  page = await launch('/subpackages/company/pages/invite')
  await wait(1000)
  const inviteTexts = await texts(page)
  result.steps.push({ step: 'invite', path: page.path, hasStoreManagerRole: inviteTexts.some((t) => t === '店长'), hasTeamLeaderRole: inviteTexts.some((t) => t.includes('团队负责人')) })
  await mp.screenshot({ path: resolve(SHOTS, 'app-invite-storemanager-roles.png') })

  page = await launch('/subpackages/company/pages/member-detail?empId=3004')
  await wait(1200)
  const roleRow = await page.getElementByXpath('//*[contains(@class,"md-row") and contains(.,"岗位")]')
  if (roleRow) await roleRow.tap()
  await wait(700)
  const detailTexts = await texts(page)
  result.steps.push({ step: 'member-detail', path: page.path, hasStoreSelector: detailTexts.some((t) => t === '门店' && detailTexts.some((x) => x.includes('验收二店'))), hasStoreManagerOption: detailTexts.some((t) => t === '店长') })
  await mp.screenshot({ path: resolve(SHOTS, 'app-member-detail-no-store-manager-option.png') })

  const members = result.steps.find((s) => s.step === 'members')
  const invite = result.steps.find((s) => s.step === 'invite')
  const detail = result.steps.find((s) => s.step === 'member-detail')
  result.pass = Boolean(
    members?.managerActions.length === 0 &&
    members?.bossActions.length === 0 &&
    members?.marketerActions.some((t) => t.includes('停用')) &&
    members?.marketerActions.some((t) => t.includes('离职')) &&
    invite && !invite.hasStoreManagerRole && !invite.hasTeamLeaderRole &&
    detail && !detail.hasStoreSelector && !detail.hasStoreManagerOption &&
    result.consoleErrors.length === 0
  )
} catch (error) {
  result.error = error instanceof Error ? error.message : String(error)
} finally {
  fs.writeFileSync(resolve(EVID, 'app-lifecycle-acceptance.json'), JSON.stringify(result, null, 2))
  try { await mp?.disconnect() } catch {}
}
console.log(JSON.stringify(result, null, 2))
