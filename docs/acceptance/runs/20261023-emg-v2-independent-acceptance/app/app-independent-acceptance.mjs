import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'

const APP = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const ROOT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon'
const EVID = resolve(ROOT, 'docs/acceptance/runs/20261023-emg-v2-independent-acceptance')
const SHOTS = resolve(EVID, 'screenshots')
const API = 'http://127.0.0.1:8090/api/'
const require = createRequire(resolve(APP, 'package.json'))
const automator = require(resolve(APP, 'node_modules/miniprogram-automator/out/index.js'))
const wait = (ms) => new Promise((r) => setTimeout(r, ms))
const result = { at: new Date().toISOString(), automationEndpoint: 'ws://127.0.0.1:9421', steps: [], pass: false, consoleErrors: [], consoleScope: 'EMG-V2 lifecycle pages only; known other-domain redirect errors are cleared after persisted-session injection', authInjection: '真实员工令牌经接口签发后写入 App 既有会话存储；devToken 快速登录入口在本次自动化环境未生效，未作为验收依据' }
let mp

async function post(route, data, token) {
  const response = await fetch(`${API}${route}`, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}) }, body: JSON.stringify(data) })
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
  for (let i = 0; i < 30; i++) {
    try { mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9421' }); break } catch { await wait(500) }
  }
  if (!mp) throw new Error('无法连接 miniprogram-automator 9421')
  mp.on('console', (event) => { if (event.type === 'error') result.consoleErrors.push(event) })
  mp.on('exception', (event) => result.consoleErrors.push(event))

  const phone = process.env.EMG_ACCEPT_PHONE
  const password = process.env.EMG_ACCEPT_PASSWORD
  if (!phone || !password) throw new Error('缺少 EMG_ACCEPT_PHONE / EMG_ACCEPT_PASSWORD')
  const login = await post('auth/session/password-login', { phone, password })
  if (!login.success || !login.data?.token) throw new Error(`店长登录失败: ${JSON.stringify(login.errorCode)}`)
  const token = login.data.token
  const profile = await post('auth/session/info', {}, token)
  const raw = profile.data?.employee
  if (!profile.success || !raw) throw new Error('会话信息读取失败')
  const company = { ...raw.company, id: String(raw.company.comId), shortName: raw.company.name, storeName: raw.store.name }
  const store = { ...raw.store, id: String(raw.store.storeId) }
  const emp = { ...raw, id: String(raw.empId), phone: raw.telNo, role: 'storeManager', title: '店长', companyId: String(raw.company.comId), storeId: String(raw.store.storeId), company, store, status: 'active', joinDate: raw.joinedAt }
  const agreement = { agreementVersion: '1.0.0', privacyVersion: '1.0.0', agreedAt: new Date().toISOString(), subject: '安徽昭青网络科技有限公司', contact: 'decoboon@joytsing.com', appVersion: '1.0.66' }
  const user = { id: String(profile.data.creId), phone, nickname: raw.name, status: 'active', sessionType: 'Employee', currentEmployee: emp, passwordSet: true, customerAccesses: [] }
  const session = { token, company, role: 'storeManager', emp, user, customer: null, sessionType: 'employee', agreement, agreed: true }
  await mp.evaluate(({ token, session }) => { wx.setStorageSync('jzb_token', token); wx.setStorageSync('jzb_consent_v1', JSON.stringify(session.agreement)); wx.setStorageSync('jzb_session_v2', JSON.stringify(session)); wx.reLaunch({ url: '/pages/index/index' }) }, { token, session })
  await wait(2500)
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
  result.steps.push({ step: 'members', path: page.path, rowCount: rows.length, rows, managerActions: managerRow?.actions || [], bossActions: bossRow?.actions || [] })
  await mp.screenshot({ path: resolve(SHOTS, 'app-manager-members-final.png') })

  page = await launch('/subpackages/company/pages/invite')
  await wait(1000)
  const inviteTexts = await texts(page)
  result.steps.push({ step: 'invite', path: page.path, hasStoreManagerRole: inviteTexts.some((t) => t === '店长'), hasTeamLeaderRole: inviteTexts.some((t) => t.includes('团队负责人')), hasFinanceRole: inviteTexts.some((t) => t.includes('财务')), texts: inviteTexts.filter((t) => ['店长','设计师','业务员','项目经理','财务'].includes(t) || t.includes('团队负责人') || t.includes('验收总店') || t.includes('目标门店')) })
  await mp.screenshot({ path: resolve(SHOTS, 'app-manager-invite-roles.png') })

  result.pass = Boolean(
    rows.length >= 2 &&
    (managerRow?.actions.length || 0) === 0 &&
    (bossRow?.actions.length || 0) === 0 &&
    !result.steps.find((s) => s.step === 'invite')?.hasStoreManagerRole &&
    !result.steps.find((s) => s.step === 'invite')?.hasTeamLeaderRole &&
    result.steps.find((s) => s.step === 'invite')?.hasFinanceRole &&
    result.consoleErrors.length === 0
  )
} catch (error) {
  result.error = error instanceof Error ? error.message : String(error)
} finally {
  fs.writeFileSync(resolve(EVID, 'app/app-independent-acceptance.json'), JSON.stringify(result, null, 2) + '\n')
  try { await mp?.disconnect() } catch {}
}
console.log(JSON.stringify(result, null, 2))
