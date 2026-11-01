import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'

const require = createRequire(import.meta.url)
const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const API = 'http://127.0.0.1:8091/api'
const OUT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261031-dq-x11-contract-change-range-picker'
const wait = (ms) => new Promise((r) => setTimeout(r, ms))

async function login() {
  const response = await fetch(`${API}/auth/session/password-login`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ phone: process.env.TH4_TEST_PHONE, password: process.env.TH4_TEST_PASSWORD })
  })
  const body = await response.json()
  if (!body.success || !body.data?.token) throw new Error('登录失败')
  return body.data.token
}
async function currentPath(mp) { try { return (await mp.currentPage())?.path || '' } catch { return '' } }
async function waitPath(mp, expected, tries = 80) {
  for (let i = 0; i < tries; i++) { await wait(250); if ((await currentPath(mp)) === expected) return await mp.currentPage() }
  throw new Error(`等待页面超时: ${expected}; 当前=${await currentPath(mp)}`)
}

const result = { outcome: 'FAIL', checks: [] }
let mp
try {
  for (let i = 0; i < 30; i++) { try { mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9429' }); break } catch { await wait(500) } }
  if (!mp) throw new Error('无法连接微信开发者工具')
  const token = await login()
  await mp.evaluate((url) => wx.reLaunch({ url }), `/pages/auth/login?devToken=${encodeURIComponent(token)}`)
  await waitPath(mp, 'pages/index/index')
  await mp.evaluate((url) => wx.navigateTo({ url }), '/subpackages/project/pages/change-create?projectId=241940507872198656&contractId=241939522005237760')
  const page = await waitPath(mp, 'subpackages/project/pages/change-create')
  await wait(700)
  const actionItems = await page.$$('.cc__seg--action .cc__seg-item')
  if (actionItems.length < 2) throw new Error('未找到减项动作')
  await actionItems[1].tap()
  await wait(300)
  const sourceCard = await page.$('.cc__src')
  if (!sourceCard) throw new Error('未找到来源选择入口')
  await sourceCard.tap()
  await wait(900)
  const panel = await page.$('.sp')
  let text = panel ? await panel.text() : ''
  const initialOk = text.includes('共 7 项') && text.includes('来源已全部加载')
  const tabs = await page.$$('.sp__tab')
  let materialTab = null
  for (const tab of tabs) { if ((await tab.text()).includes('主材')) materialTab = tab }
  if (!materialTab) throw new Error('未找到主材筛选')
  await materialTab.tap()
  await wait(900)
  const filteredPanel = await page.$('.sp')
  text = filteredPanel ? await filteredPanel.text() : ''
  const filteredOk = text.includes('共 3 项') && (await page.$$('.sp__item')).length === 3
  result.checks = [
    { id: 'initial-total-and-loaded', pass: initialOk },
    { id: 'material-server-filter', pass: filteredOk, detail: text.slice(0, 800) }
  ]
  result.outcome = initialOk && filteredOk ? 'PASS' : 'FAIL'
  await mp.screenshot({ path: resolve(OUT, 'app-range-native-material.png') })
} catch (error) {
  result.error = error instanceof Error ? error.message : String(error)
} finally {
  fs.writeFileSync(resolve(OUT, 'app-native-check.json'), JSON.stringify(result, null, 2) + '\n')
  if (mp) await mp.disconnect()
}
if (result.outcome !== 'PASS') process.exitCode = 1
