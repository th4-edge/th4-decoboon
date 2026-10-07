import automator from 'file:///Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
import fs from 'node:fs'
import path from 'node:path'

const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/runs/20261008-昭青装饰-20261008-昭青装饰-合同签约发起确认生效-微信沉浸式验收'
const API = 'http://127.0.0.1:8090/api'
const QUOTE = '228668971543429120'
const CONTRACT = '233710002739609600'
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
  const token = await login('19900000003')
  await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(token)}`)
  await new Promise((r) => setTimeout(r, 2200))
  await mp.reLaunch(`/subpackages/project/pages/create?quoteId=${QUOTE}&customerId=${CUSTOMER}&propertyId=${PROPERTY}`)
  await new Promise((r) => setTimeout(r, 2200))
  const page = await mp.currentPage()
  const amountInputs = await page.$$('input')
  let amountInput = null
  for (const input of amountInputs) {
    const value = await input.value?.()
    if (String(value) === '58903.17') { amountInput = input; break }
  }
  if (!amountInput) throw new Error('final amount input missing or not editable')
  await mp.screenshot({ path: path.join(RUN, 'raw/screenshots/S02-storemanager-before-review.png') })
  const buttons = await page.$$('button')
  let confirm = null
  for (const button of buttons) {
    if ((await button.text())?.trim() === '调整并确认签约') { confirm = button; break }
  }
  if (!confirm) throw new Error('confirm sign button missing')
  await confirm.tap()
  await new Promise((r) => setTimeout(r, 500))
  await mp.screenshot({ path: path.join(RUN, 'raw/screenshots/S02-storemanager-confirm-modal.png') })
  console.log(JSON.stringify({ path: page.path, contract: CONTRACT, modalOpened: true }, null, 2))
} finally {
  try { await mp.disconnect() } catch {}
}
