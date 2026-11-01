import { createRequire } from 'node:module'
import fs from 'node:fs'
const require = createRequire(import.meta.url)
const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const API = 'http://127.0.0.1:8091/api'
const OUT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261031-pay-ops-01/raw'
const wait = ms => new Promise(r => setTimeout(r, ms))
const result = { outcome: 'FAIL', checks: [] }
let mp
try {
  for (let i = 0; i < 30; i++) {
    try { mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' }); break } catch { await wait(500) }
  }
  if (!mp) throw new Error('无法连接微信开发者工具')
  const login = await fetch(`${API}/auth/session/password-login`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ phone: process.env.TH4_TEST_PHONE, password: process.env.TH4_TEST_PASSWORD })
  }).then(r => r.json())
  if (!login.success) throw new Error('自动化登录失败')
  const token = login.data.token
  await mp.evaluate(url => wx.reLaunch({ url }), `/pages/auth/login?devToken=${encodeURIComponent(token)}`)
  await wait(1800)
  await mp.evaluate(url => wx.navigateTo({ url }), '/subpackages/payroll/pages/workbench')
  await wait(1800)
  const page = await mp.currentPage()
  const root = await page.$('.pay-page')
  const text = root ? await root.text() : ''
  const hero = await page.$('.pay-hero__value')
  const statValues = await page.$$('.pay-stat__value')
  const values = []
  for (const item of statValues) values.push(await item.text())
  const checks = [
    { id: 'page-path', pass: page.path === 'subpackages/payroll/pages/workbench', detail: page.path },
    { id: 'current-period', pass: text.includes('2026-10') },
    { id: 'pending-payable', pass: text.includes('¥1,600') || text.includes('1600.00'), detail: hero ? await hero.text() : '' },
    { id: 'pending-attendance', pass: values.includes('2') },
    { id: 'draft-sheet', pass: values.includes('1') }
  ]
  result.checks = checks
  result.pageText = text.slice(0, 3000)
  result.statValues = values
  result.outcome = checks.every(x => x.pass) ? 'PASS' : 'FAIL'
  await mp.screenshot({ path: `${OUT}/app-payroll-workbench.png` })
} catch (e) {
  result.error = e instanceof Error ? e.message : String(e)
} finally {
  fs.writeFileSync(`${OUT}/app-payops-check.json`, JSON.stringify(result, null, 2) + '\n')
  if (mp) await mp.disconnect()
}
if (result.outcome !== 'PASS') process.exitCode = 1
