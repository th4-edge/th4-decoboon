import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'

const APP = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const ROOT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon'
const EVID = resolve(ROOT, 'docs/acceptance/runs/20261021-payroll-b1-independent')
const SHOTS = resolve(ROOT, 'output/playwright/payroll-b1-independent')
const API = 'http://127.0.0.1:8090/api/'
const XU = '1790224826490830054'
const require = createRequire(resolve(APP, 'package.json'))
const automator = require(resolve(APP, 'node_modules/miniprogram-automator/out/index.js'))
const wait = (ms) => new Promise((r) => setTimeout(r, ms))
const result = { at: new Date().toISOString(), apiBase: API, employeeId: XU, steps: [], pass: false }
const logs = []
let mp
let token = ''
let schemeId = ''

async function post(route, data, authToken) {
  const response = await fetch(`${API}${route}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(authToken ? { token: authToken } : {}) },
    body: JSON.stringify(data)
  })
  return response.json()
}
async function currentPath() { try { return (await mp.currentPage())?.path || '' } catch { return '' } }
async function waitPath(fragment, tries = 40) {
  for (let i = 0; i < tries; i++) {
    if ((await currentPath()).includes(fragment)) return await mp.currentPage()
    await wait(250)
  }
  throw new Error(`等待页面失败: ${fragment}, current=${await currentPath()}`)
}
async function byXPath(page, expression) { return await page.getElementByXpath(expression) }
async function tapXPath(page, expression, label) {
  const element = await byXPath(page, expression)
  if (!element) throw new Error(`未找到: ${label}`)
  await element.tap()
}
async function xuSchemes() {
  const response = await post('payroll/compensation/page', { current: 1, size: 200, keyword: '徐勇' }, token)
  if (!response.success) throw new Error(`读取薪酬方案失败: ${JSON.stringify(response.errorCode)}`)
  return (response.data?.content || []).filter((item) => String(item.employee?.empId) === XU)
}

try {
  for (let i = 0; i < 20; i++) {
    try { mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' }); break } catch { await wait(500) }
  }
  if (!mp) throw new Error('无法连接 miniprogram-automator')
  mp.on('console', (event) => logs.push({ kind: 'console', ...event }))
  mp.on('exception', (event) => logs.push({ kind: 'exception', ...event }))

  const login = await post('auth/session/password-login', { phone: process.env.PAYROLL_FINANCE_PHONE, password: process.env.PAYROLL_TEST_PASSWORD })
  if (!login.success || !login.data?.token) throw new Error(`Finance 登录失败: ${JSON.stringify(login.errorCode)}`)
  token = login.data.token
  const before = await xuSchemes()
  result.steps.push({ step: 'before', xuSchemeCount: before.length })
  if (before.length !== 0) throw new Error(`徐勇起始方案数应为 0，实际 ${before.length}`)

  try { await mp.evaluate((url) => wx.reLaunch({ url }), `/pages/auth/login?devToken=${encodeURIComponent(token)}`) } catch {}
  await wait(1200)
  try { await mp.evaluate(() => wx.reLaunch({ url: '/subpackages/payroll/pages/compensation-list' })) } catch {}
  let page = await waitPath('compensation-list')
  await wait(800)
  const initialCards = await page.$$('.comp-card')
  if (initialCards.length !== 5) throw new Error(`初始列表应为 5 条，实际 ${initialCards.length}`)
  result.steps.push({ step: 'list-before', cardCount: initialCards.length })

  await tapXPath(page, '//*[contains(@class,"jz-header__action")]', '新增入口')
  page = await waitPath('compensation-edit')
  await wait(700)
  await tapXPath(page, '//*[contains(@class,"jz-employee-field")]', '发薪员工选择')
  page = await waitPath('employee-picker')
  await wait(800)
  let picked = false
  for (const item of await page.$$('.ep-item')) {
    if ((await item.text()).includes('徐勇')) { await item.tap(); picked = true; break }
  }
  if (!picked) throw new Error('员工选择器未找到徐勇')
  page = await waitPath('compensation-edit')
  await wait(700)

  const moneyCreate = await byXPath(page, '//input[@placeholder="请输入月固定工资"]')
  const daysCreate = await byXPath(page, '//input[@placeholder="按企业实际考勤制度填写"]')
  if (!moneyCreate || !daysCreate) throw new Error('新增页未找到金额/应出勤输入框')
  await moneyCreate.input('5432.12')
  await daysCreate.input('23')
  await wait(300)
  result.steps.push({ step: 'create-form', values: { money: await moneyCreate.value(), days: await daysCreate.value() } })
  await mp.screenshot({ path: resolve(SHOTS, 'acc003-app-01-create-filled.png') })
  await tapXPath(page, '//button[contains(.,"保存薪酬方案")]', '保存薪酬方案')
  page = await waitPath('compensation-list')
  await wait(1100)

  let cards = await page.$$('.comp-card')
  let xuCard = null
  let createdText = ''
  for (const card of cards) {
    const text = await card.text()
    if (text.includes('徐勇')) { xuCard = card; createdText = text; break }
  }
  const apiCreated = await xuSchemes()
  const createdExact = createdText.includes('¥5,432.12') && createdText.includes('23 天') && !createdText.includes('已锁定')
  result.steps.push({ step: 'created', cardCount: cards.length, cardText: createdText, api: apiCreated[0] ? { schemeId: apiCreated[0].schemeId, baseSalary: apiCreated[0].baseSalary, scheduledDays: apiCreated[0].scheduledDays, locked: apiCreated[0].locked } : null, displayExact: createdText.includes('¥5,432.12') })
  if (!xuCard || !createdExact || apiCreated.length !== 1 || Number(apiCreated[0].baseSalary) !== 5432.12 || Number(apiCreated[0].scheduledDays) !== 23 || apiCreated[0].locked !== false) {
    throw new Error(`新增金额精度回读失败: card=${createdText}; api=${JSON.stringify(apiCreated[0])}`)
  }
  schemeId = String(apiCreated[0].schemeId)
  await mp.screenshot({ path: resolve(SHOTS, 'acc003-app-02-created-exact.png') })

  await xuCard.tap()
  page = await waitPath('compensation-edit')
  await wait(800)
  const moneyEdit = await byXPath(page, '//input[@placeholder="请输入月固定工资"]')
  const daysEdit = await byXPath(page, '//input[@placeholder="按企业实际考勤制度填写"]')
  if (!moneyEdit || !daysEdit) throw new Error('编辑页未找到金额/应出勤输入框')
  const editInitial = { money: await moneyEdit.value(), days: await daysEdit.value() }
  await moneyEdit.input('5432.12')
  await daysEdit.input('24')
  await wait(300)
  result.steps.push({ step: 'edit-form', initialValues: editInitial, updatedValues: { money: await moneyEdit.value(), days: await daysEdit.value() } })
  await mp.screenshot({ path: resolve(SHOTS, 'acc003-app-03-edit-filled.png') })
  await tapXPath(page, '//button[contains(.,"保存薪酬方案")]', '保存修改')
  page = await waitPath('compensation-list')
  await wait(1100)

  cards = await page.$$('.comp-card')
  xuCard = null
  let editedText = ''
  for (const card of cards) {
    const text = await card.text()
    if (text.includes('徐勇')) { xuCard = card; editedText = text; break }
  }
  const apiEdited = await xuSchemes()
  const editedExact = editedText.includes('¥5,432.12') && editedText.includes('24 天') && !editedText.includes('已锁定')
  result.steps.push({ step: 'edited', cardCount: cards.length, cardText: editedText, api: apiEdited[0] ? { schemeId: apiEdited[0].schemeId, baseSalary: apiEdited[0].baseSalary, scheduledDays: apiEdited[0].scheduledDays, locked: apiEdited[0].locked } : null, displayExact: editedText.includes('¥5,432.12') })
  if (!xuCard || !editedExact || apiEdited.length !== 1 || Number(apiEdited[0].baseSalary) !== 5432.12 || Number(apiEdited[0].scheduledDays) !== 24 || apiEdited[0].locked !== false) {
    throw new Error(`编辑金额精度回读失败: card=${editedText}; api=${JSON.stringify(apiEdited[0])}`)
  }
  await mp.screenshot({ path: resolve(SHOTS, 'acc003-app-04-edited-exact.png') })

  const deleteAction = await xuCard.$('.comp-card__delete')
  if (!deleteAction) throw new Error('未找到删除方案入口')
  await deleteAction.tap()
  await wait(500)
  const confirm = await byXPath(page, '//*[contains(@class,"jz-confirm__card")]')
  if (!confirm) throw new Error('删除确认弹窗未出现')
  result.steps.push({ step: 'delete-confirm', visible: true })
  await mp.screenshot({ path: resolve(SHOTS, 'acc003-app-05-delete-confirm.png') })
  await tapXPath(page, '//button[normalize-space(.)="删除"]', '确认删除')
  await wait(1400)

  cards = await page.$$('.comp-card')
  const finalTexts = []
  for (const element of await page.$$('*')) { try { const t = await element.text(); if (t) finalTexts.push(t) } catch {} }
  const xuVisible = finalTexts.some((text) => text.includes('徐勇') && text.includes('5,432.12'))
  const apiDeleted = await xuSchemes()
  result.steps.push({ step: 'deleted', cardCount: cards.length, xuVisible, apiCount: apiDeleted.length })
  if (cards.length !== 5 || xuVisible || apiDeleted.length !== 0) throw new Error(`删除清理失败: cards=${cards.length}, visible=${xuVisible}, apiCount=${apiDeleted.length}`)
  await mp.screenshot({ path: resolve(SHOTS, 'acc003-app-06-deleted-list.png') })

  result.pass = true
} catch (error) {
  result.error = error instanceof Error ? error.message : String(error)
} finally {
  if (token) {
    try {
      for (const item of await xuSchemes()) {
        if (String(item.schemeId) === schemeId || item.employee?.empId == XU) {
          await post('payroll/compensation/delete', { schemeId: item.schemeId }, token)
        }
      }
    } catch {}
  }
  result.logs = logs
  result.consoleErrors = logs.filter((item) => item.kind === 'exception' || item.type === 'error')
  fs.writeFileSync(resolve(EVID, 'acc003-app-precision-e2e.json'), JSON.stringify(result, null, 2))
  try { await mp?.disconnect() } catch {}
}
console.log(JSON.stringify(result, null, 2))
