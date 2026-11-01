import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'

const require = createRequire(import.meta.url)
const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const API = 'http://127.0.0.1:8090/api'
const OUT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261031-dq-x9-owner-contract-line-pagination'
const wait = (ms) => new Promise((r) => setTimeout(r, ms))

async function token() {
  const phone = process.env.TH4_TEST_PHONE
  const password = process.env.TH4_TEST_PASSWORD
  if (!phone || !password) throw new Error('缺少 TH4_TEST_PHONE / TH4_TEST_PASSWORD')
  const response = await fetch(`${API}/auth/session/password-login`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ phone, password })
  })
  const body = await response.json()
  if (!body.success || !body.data?.token) throw new Error('Boss 登录失败')
  return body.data.token
}

async function currentPath(mp) {
  try { return (await mp.currentPage())?.path || '' } catch { return '' }
}

async function waitPath(mp, expected, tries = 80) {
  for (let index = 0; index < tries; index += 1) {
    await wait(250)
    const path = await currentPath(mp)
    if (path === expected) return await mp.currentPage()
  }
  throw new Error(`等待页面超时: ${expected}; 当前=${await currentPath(mp)}`)
}

const result = { outcome: 'FAIL', checks: [] }
let mp
try {
  for (let index = 0; index < 30; index += 1) {
    try { mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9428' }); break } catch { await wait(500) }
  }
  if (!mp) throw new Error('无法连接微信开发者工具自动化端口')
  const devToken = await token()
  await mp.evaluate((url) => wx.reLaunch({ url }), `/pages/auth/login?devToken=${encodeURIComponent(devToken)}`)
  await waitPath(mp, 'pages/index/index')
  await mp.evaluate((url) => wx.navigateTo({ url }), '/subpackages/project/pages/contract?projectId=241940507872198656&contractId=241939522005237760')
  const page = await waitPath(mp, 'subpackages/project/pages/contract')
  await wait(1000)
  const root = await page.$('.cw')
  const text = root ? (await root.text()).trim() : ''
  const checks = [
    ['workbench-contract', text.includes('C1793372477050') && text.includes('已变更')],
    ['server-line-summary', text.includes('签约时冻结的合同明细，共 6 项') && text.includes('合计 ¥978')],
    ['all-lines-loaded', text.includes('合同明细已全部加载')],
    ['contract-scope-visible', text.includes('合同范围') && text.includes('未分区域') && text.includes('6 项')]
  ].map(([id, pass]) => ({ id, pass: Boolean(pass) }))
  result.checks = checks
  result.text = text.slice(0, 5000)
  result.outcome = checks.every((check) => check.pass) ? 'PASS' : 'FAIL'
  await mp.screenshot({ path: resolve(OUT, 'app-contract-native.png') })
} catch (error) {
  result.error = error instanceof Error ? error.message : String(error)
} finally {
  fs.writeFileSync(resolve(OUT, 'app-contract-check.json'), JSON.stringify(result, null, 2) + '\n')
  if (mp) await mp.disconnect()
}
if (result.outcome !== 'PASS') process.exitCode = 1
