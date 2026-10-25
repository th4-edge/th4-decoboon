import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'

const APP = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const ROOT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon'
const EVID = resolve(ROOT, 'docs/acceptance/runs/20261022-emg-v1-team-freeze')
const SHOTS = resolve(EVID, 'screenshots')
const API = 'http://127.0.0.1:8090/api/'
const require = createRequire(resolve(APP, 'package.json'))
const automator = require(resolve(APP, 'node_modules/miniprogram-automator/out/index.js'))
const wait = (ms) => new Promise((r) => setTimeout(r, ms))
const result = { at: new Date().toISOString(), steps: [], pass: false, consoleErrors: [] }
let mp
async function post(route, data) {
  const response = await fetch(`${API}${route}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
  })
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

  const login = await post('auth/session/password-login', { phone: '13990000003', password: 'Accept123' })
  if (!login.success || !login.data?.token) throw new Error(`店长登录失败: ${JSON.stringify(login.errorCode)}`)
  try { await mp.evaluate((url) => wx.reLaunch({ url }), `/pages/auth/login?devToken=${encodeURIComponent(login.data.token)}`) } catch {}
  await wait(1800)
  try { await mp.evaluate(() => wx.reLaunch({ url: '/pages/index/index' })) } catch {}
  await wait(1800)
  let page = await currentPageSafe()
  let pageTexts = await texts(page)
  result.steps.push({ step: 'home', path: page.path, hasTeamEntry: pageTexts.some((t) => t.includes('门店团队') || t.includes('企业团队') || t.includes('团队成员')) })
  await mp.screenshot({ path: resolve(SHOTS, 'app-store-manager-home.png') })

  page = await launch('/subpackages/team/pages/designers')
  await wait(1200)
  pageTexts = await texts(page)
  result.steps.push({ step: 'designers', path: page.path, hasMemberManagement: pageTexts.some((t) => t.includes('成员管理')), hasTeamManagement: pageTexts.some((t) => t.includes('门店团队') || t.includes('企业团队') || t.includes('团队成员')) })
  await mp.screenshot({ path: resolve(SHOTS, 'app-designers-member-entry.png') })

  page = await launch('/subpackages/company/pages/invite')
  await wait(1200)
  pageTexts = await texts(page)
  result.steps.push({ step: 'invite', path: page.path, hasTeamSelection: pageTexts.some((t) => t === '团队' || t.includes('新团队')), hasTeamLeaderRole: pageTexts.some((t) => t.includes('团队负责人')) })
  await mp.screenshot({ path: resolve(SHOTS, 'app-invite-no-team.png') })

  page = await launch('/subpackages/team/pages/list')
  await wait(1200)
  page = await currentPageSafe()
  result.steps.push({ step: 'frozen-team-deeplink', path: page.path, blocked: !page.path.includes('subpackages/team/pages/list') })
  await mp.screenshot({ path: resolve(SHOTS, 'app-team-deeplink-blocked.png') })

  const home = result.steps.find((s) => s.step === 'home')
  const designers = result.steps.find((s) => s.step === 'designers')
  const invite = result.steps.find((s) => s.step === 'invite')
  const blocked = result.steps.find((s) => s.step === 'frozen-team-deeplink')
  result.pass = Boolean(!home?.hasTeamEntry && designers?.hasMemberManagement && !designers?.hasTeamManagement && !invite?.hasTeamSelection && !invite?.hasTeamLeaderRole && blocked?.blocked && result.consoleErrors.length === 0)
} catch (error) {
  result.error = error instanceof Error ? error.message : String(error)
} finally {
  fs.writeFileSync(resolve(EVID, 'app-team-freeze-acceptance.json'), JSON.stringify(result, null, 2))
  try { await mp?.disconnect() } catch {}
}
console.log(JSON.stringify(result, null, 2))
