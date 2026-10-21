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
let createdSchemeId = ''

async function post(route, data, authToken) {
  const response = await fetch(`${API}${route}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(authToken ? { token: authToken } : {}) },
    body: JSON.stringify(data)
  })
  return response.json()
}

async function currentPath() {
  try { return (await mp.currentPage())?.path || '' } catch { return '' }
}

async function waitPath(fragment, tries = 40) {
  for (let i = 0; i < tries; i++) {
    if ((await currentPath()).includes(fragment)) return await mp.currentPage()
    await wait(250)
  }
  throw new Error(`等待页面失败: ${fragment}, current=${await currentPath()}`)
}

async function byXPath(page, expression) {
  return await page.getElementByXpath(expression)
}

async function tapXPath(page, expression, label) {
  const element = await byXPath(page, expression)
  if (!element) throw new Error(`未找到: ${label}`)
  await element.tap()
}

async function texts(page) {
  const out = []
  for (const element of await page.$$('*')) {
    try {
      const value = await element.text()
      if (value && value.trim()) out.push(value.trim())
    } catch {}
  }
  return [...new Set(out)]
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

  const login = await post('auth/session/password-login', {
    phone: process.env.PAYROLL_FINANCE_PHONE,
    password: process.env.PAYROLL_TEST_PASSWORD
  })
  if (!login.success || !login.data?.token) throw new Error(`Finance 登录失败: ${JSON.stringify(login.errorCode)}`)
  token = login.data.token

  const before = await xuSchemes()
  result.steps.push({ step: 'before', xuSchemeCount: before.length })
  if (before.length !== 0) throw new Error(`徐勇起始方案数应为 0，实际 ${before.length}`)

  try {
    await mp.evaluate((url) => wx.reLaunch({ url }), `/pages/auth/login?devToken=${encodeURIComponent(token)}`)
  } catch {}
  await wait(1200)
  try {
    await mp.evaluate(() => wx.reLaunch({ url: '/subpackages/payroll/pages/compensation-list' }))
  } catch {}
  let page = await waitPath('compensation-list')
  await wait(800)
  const initialCards = await page.$$('.comp-card')
  result.steps.push({ step: 'list-before', path: page.path, cardCount: initialCards.length })
  if (initialCards.length !== 5) throw new Error(`初始列表应为 5 条，实际 ${initialCards.length}`)

  // Real UI: open create form.
  await tapXPath(page, '//*[contains(@class,"jz-header__action")]', '新增入口')
  page = await waitPath('compensation-edit')
  await wait(800)

  // Real UI: employee picker.
  await tapXPath(page, '//*[contains(@class,"jz-employee-field")]', '发薪员工选择')
  page = await waitPath('employee-picker')
  await wait(900)
  let picked = false
  for (const item of await page.$$('.ep-item')) {
    const text = await item.text()
    if (text.includes('徐勇')) { await item.tap(); picked = true; break }
  }
  if (!picked) throw new Error('员工选择器未找到徐勇')
  page = await waitPath('compensation-edit')
  await wait(700)

  const money = await byXPath(page, '//input[@placeholder="请输入月固定工资"]')
  const days = await byXPath(page, '//input[@placeholder="按企业实际考勤制度填写"]')
  if (!money || !days) throw new Error('未找到工资/应出勤输入框')
  await money.input('4321.00')
  await days.input('23')
  await wait(300)
  const createInputValues = { money: await money.value(), days: await days.value() }
  result.steps.push({ step: 'create-form', path: page.path, inputValues: createInputValues, texts: (await texts(page)).filter((t) => t.includes('徐勇') || t.includes('新增薪酬方案')).slice(0, 6) })
  await mp.screenshot({ path: resolve(SHOTS, 'p2-app-01-create-filled.png') })
  await tapXPath(page, '//button[contains(.,"保存薪酬方案")]', '保存薪酬方案')
  page = await waitPath('compensation-list')
  await wait(1100)

  let cards = await page.$$('.comp-card')
  let xuCard = null
  let createdCardText = ''
  for (const card of cards) {
    const text = await card.text()
    if (text.includes('徐勇')) { xuCard = card; createdCardText = text; break }
  }
  let apiCreated = await xuSchemes()
  result.steps.push({
    step: 'created', path: page.path, cardCount: cards.length, cardText: createdCardText,
    apiCount: apiCreated.length, apiScheme: apiCreated[0] ? {
      schemeId: apiCreated[0].schemeId, baseSalary: apiCreated[0].baseSalary,
      scheduledDays: apiCreated[0].scheduledDays, locked: apiCreated[0].locked
    } : null
  })
  if (!xuCard || !createdCardText.includes('¥4,321') || !createdCardText.includes('23 天') || createdCardText.includes('已锁定') || apiCreated.length !== 1) {
    throw new Error(`新增回读不符合预期: card=${createdCardText}; apiCount=${apiCreated.length}`)
  }
  createdSchemeId = String(apiCreated[0].schemeId)
  await mp.screenshot({ path: resolve(SHOTS, 'p2-app-02-created-list.png') })

  // Real UI: edit unlocked scheme.
  await xuCard.tap()
  page = await waitPath('compensation-edit')
  await wait(800)
  const editMoney = await byXPath(page, '//input[@placeholder="请输入月固定工资"]')
  const editDays = await byXPath(page, '//input[@placeholder="按企业实际考勤制度填写"]')
  if (!editMoney || !editDays) throw new Error('编辑页未找到输入框')
  const editInitialValues = { money: await editMoney.value(), days: await editDays.value() }
  await editMoney.input('5432.12')
  await editDays.input('24')
  await wait(300)
  result.steps.push({ step: 'edit-form', path: page.path, initialValues: editInitialValues, updatedValues: { money: await editMoney.value(), days: await editDays.value() } })
  await mp.screenshot({ path: resolve(SHOTS, 'p2-app-03-edit-filled.png') })
  await tapXPath(page, '//button[contains(.,"保存薪酬方案")]', '保存修改')
  page = await waitPath('compensation-list')
  await wait(1100)

  cards = await page.$$('.comp-card')
  xuCard = null
  let editedCardText = ''
  for (const card of cards) {
    const text = await card.text()
    if (text.includes('徐勇')) { xuCard = card; editedCardText = text; break }
  }
  const apiEdited = await xuSchemes()
  result.steps.push({
    step: 'edited', path: page.path, cardCount: cards.length, cardText: editedCardText,
    apiCount: apiEdited.length, apiScheme: apiEdited[0] ? {
      schemeId: apiEdited[0].schemeId, baseSalary: apiEdited[0].baseSalary,
      scheduledDays: apiEdited[0].scheduledDays, locked: apiEdited[0].locked
    } : null
  })
  const displayAmount = (editedCardText.match(/¥[0-9,]+(?:\.[0-9]+)?/) || [])[0] || ''
  const amountDisplayMatchesApi = displayAmount === '¥5,432.12'
  result.steps.push({ step: 'amount-display-check', displayAmount, apiAmount: apiEdited[0]?.baseSalary, amountDisplayMatchesApi })
  if (!xuCard || !editedCardText.includes('¥5,432') || !editedCardText.includes('24 天') || editedCardText.includes('已锁定') || apiEdited.length !== 1 || Number(apiEdited[0].baseSalary) !== 5432.12 || Number(apiEdited[0].scheduledDays) !== 24 || apiEdited[0].locked !== false) {
    throw new Error(`编辑回读不符合预期: card=${editedCardText}; api=${JSON.stringify(apiEdited[0])}`)
  }
  await mp.screenshot({ path: resolve(SHOTS, 'p2-app-04-edited-list.png') })

  // Real UI: delete unlocked scheme with confirmation.
  const deleteAction = await xuCard.$('.comp-card__delete')
  if (!deleteAction) throw new Error('未找到删除方案入口')
  await deleteAction.tap()
  await wait(500)
  const confirm = await byXPath(page, '//*[contains(@class,"jz-confirm__card")]')
  if (!confirm) throw new Error('删除确认弹窗未出现')
  const confirmTexts = await texts(page)
  result.steps.push({ step: 'delete-confirm', path: page.path, confirmTexts: confirmTexts.filter((t) => t.includes('删除薪酬方案') || t.includes('确认删除')).slice(0, 6) })
  await mp.screenshot({ path: resolve(SHOTS, 'p2-app-05-delete-confirm.png') })
  await tapXPath(page, '//button[normalize-space(.)="删除"]', '确认删除')
  await wait(1400)

  cards = await page.$$('.comp-card')
  const finalTexts = await texts(page)
  const stillVisible = finalTexts.some((t) => t.includes('徐勇') && (t.includes('5,432') || t.includes('5432')))
  const apiAfterDelete = await xuSchemes()
  result.steps.push({ step: 'deleted', path: page.path, cardCount: cards.length, xuVisible: stillVisible, apiCount: apiAfterDelete.length })
  if (cards.length !== 5 || stillVisible || apiAfterDelete.length !== 0) {
    throw new Error(`删除回读不符合预期: cards=${cards.length}, visible=${stillVisible}, apiCount=${apiAfterDelete.length}`)
  }
  await mp.screenshot({ path: resolve(SHOTS, 'p2-app-06-deleted-list.png') })

  result.pass = true
  result.amountDisplayMatchesApi = (result.steps.find((item) => item.step === 'amount-display-check') || {}).amountDisplayMatchesApi === true
  if (!result.amountDisplayMatchesApi) result.finding = 'App 列表金额显示与 API 两位小数金额不一致：¥5,432.12 被显示为 ¥5,432。'
} catch (error) {
  result.error = error instanceof Error ? error.message : String(error)
} finally {
  // Only XuYong temporary rows are eligible for this safety cleanup.
  if (token) {
    try {
      for (const item of await xuSchemes()) {
        if (String(item.schemeId) === createdSchemeId || item.employee?.empId == XU) {
          await post('payroll/compensation/delete', { schemeId: item.schemeId }, token)
        }
      }
    } catch {}
  }
  result.logs = logs
  result.consoleErrors = logs.filter((item) => item.kind === 'exception' || item.type === 'error')
  fs.writeFileSync(resolve(EVID, 'p2-app-crud-e2e.json'), JSON.stringify(result, null, 2))
  try { await mp?.disconnect() } catch {}
}
console.log(JSON.stringify(result, null, 2))
