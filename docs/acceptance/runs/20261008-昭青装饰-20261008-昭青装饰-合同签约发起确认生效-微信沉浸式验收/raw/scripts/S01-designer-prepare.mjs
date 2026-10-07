import automator from 'file:///Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
import fs from 'node:fs'
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

async function texts(page) {
  const body = await page.$('body')
  return body ? (await body.text()) || '' : ''
}

const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
try {
  const token = await login('19900000005')
  await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(token)}`)
  await new Promise((r) => setTimeout(r, 2200))
  await mp.reLaunch(`/subpackages/project/pages/create?quoteId=${QUOTE}&customerId=${CUSTOMER}&propertyId=${PROPERTY}`)
  await new Promise((r) => setTimeout(r, 1800))
  let page = await mp.currentPage()
  await mp.screenshot({ path: path.join(RUN, 'raw/screenshots/S01-designer-before-prepare.png') })
  const before = await texts(page)

  let blocks = await page.$$('.pc__plan-block')
  if (blocks.length < 2) throw new Error('plan blocks missing')
  await blocks[0].tap()
  await new Promise((r) => setTimeout(r, 800))
  page = await mp.currentPage()
  let cards = await page.$$('.bp-card')
  if (!cards.length) throw new Error('payment plans missing')
  await cards[0].tap()
  await new Promise((r) => setTimeout(r, 1000))

  page = await mp.currentPage()
  blocks = await page.$$('.pc__plan-block')
  if (blocks.length < 2) throw new Error('construction plan block missing')
  await blocks[1].tap()
  await new Promise((r) => setTimeout(r, 800))
  page = await mp.currentPage()
  cards = await page.$$('.bp-card')
  if (!cards.length) throw new Error('construction plans missing')
  await cards[0].tap()
  await new Promise((r) => setTimeout(r, 1000))

  page = await mp.currentPage()
  const buttons = await page.$$('button')
  let submit = null
  for (const button of buttons) {
    if ((await button.text())?.trim() === '提交合同签约') { submit = button; break }
  }
  if (!submit) throw new Error('submit button missing')
  await submit.tap()
  await new Promise((r) => setTimeout(r, 2200))
  page = await mp.currentPage()
  const after = await texts(page)
  await mp.screenshot({ path: path.join(RUN, 'raw/screenshots/S01-designer-after-prepare.png') })
  const result = { path: page.path, before, after }
  fs.writeFileSync(path.join(RUN, 'raw/accessibility/S01-designer-prepare.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify({ path: page.path, pending: after.includes('等待负责人确认'), submitted: after.includes('已提交') }, null, 2))
} finally {
  try { await mp.disconnect() } catch {}
}
