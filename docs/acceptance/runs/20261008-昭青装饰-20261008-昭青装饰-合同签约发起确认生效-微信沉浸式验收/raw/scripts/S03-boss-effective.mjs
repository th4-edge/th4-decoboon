import automator from 'file:///Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
import path from 'node:path'

const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261008-昭青装饰-20261008-昭青装饰-合同签约发起确认生效-微信沉浸式验收'
const API = 'http://127.0.0.1:8090/api'
const QUOTE = '228668971543429120'
const CUSTOMER = '228668969219784704'
const PROPERTY = '228668969349808128'

async function login(phone) {
  const res = await fetch(`${API}/auth/session/password-login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ phone, password: '123456' })
  })
  const data = await res.json()
  if (!data.success || !data.data?.token) throw new Error(`login failed: ${phone}`)
  return data.data.token
}

const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
try {
  const token = await login('19900000001')
  await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(token)}`)
  await new Promise((r) => setTimeout(r, 2200))
  await mp.reLaunch(`/subpackages/project/pages/create?quoteId=${QUOTE}&customerId=${CUSTOMER}&propertyId=${PROPERTY}`)
  await new Promise((r) => setTimeout(r, 2200))
  const page = await mp.currentPage()
  await mp.screenshot({ path: path.join(RUN, 'raw/screenshots/S03-boss-before-effective.png') })
  const buttons = await page.$$('button')
  let confirm = null
  for (const button of buttons) {
    if ((await button.text())?.trim() === '确认生效并生成项目') { confirm = button; break }
  }
  if (!confirm) throw new Error('effective button missing')
  await confirm.tap()
  await new Promise((r) => setTimeout(r, 500))
  await mp.screenshot({ path: path.join(RUN, 'raw/screenshots/S03-boss-effective-modal.png') })
  console.log(JSON.stringify({ path: page.path, modalOpened: true }, null, 2))
} finally {
  try { await mp.disconnect() } catch {}
}
