import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const AUTOMATOR_ROOT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app'
const require = createRequire(import.meta.url)
const automator = require(`${AUTOMATOR_ROOT}/node_modules/miniprogram-automator/out/index.js`)
const RUN = path.dirname(path.dirname(path.dirname(fileURLToPath(import.meta.url))))
const API = 'http://127.0.0.1:8090/api/'

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))

async function pageText(page) {
  const values = []
  for (const element of await page.$$('*')) {
    try {
      const text = String(await element.text()).trim()
      if (text) values.push(text)
    } catch {}
  }
  return [...new Set(values)].filter(text => text.length <= 1000).slice(0, 300).join('\n')
}

async function tapByTextAndClass(page, text, className) {
  for (const element of await page.$$('*')) {
    try {
      const content = String(await element.text()).trim()
      const classes = String(await element.attribute('class') || '')
      if (content === text && classes.includes(className)) {
        await element.tap()
        return
      }
    } catch {}
  }
  throw new Error(`未找到可点击元素：${text} / ${className}`)
}

async function capture(mp, page, id) {
  await mp.screenshot({ path: path.join(RUN, 'raw/screenshots', `${id}.png`) })
  const text = await pageText(page)
  fs.writeFileSync(path.join(RUN, 'raw/accessibility', `${id}.txt`), text)
  return { id, path: page.path, text }
}

async function measure(page, selectors) {
  const result = {}
  for (const selector of selectors) {
    const element = await page.$(selector)
    if (element) result[selector] = await element.size()
  }
  return result
}

const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
const logs = []
mp.on('console', value => logs.push({ kind: 'console', ...value }))
mp.on('exception', value => logs.push({ kind: 'exception', ...value }))

await mp.reLaunch('/pages/index/index')
await sleep(1800)
let page = await mp.currentPage()
const steps = []
steps.push(await capture(mp, page, 'S20-home-boss'))

await tapByTextAndClass(page, '财务工作台', 'home__quick')
await sleep(1800)
page = await mp.currentPage()
steps.push(await capture(mp, page, 'S21-finance-home'))

await tapByTextAndClass(page, '员工工资', 'fin-quick__item')
await sleep(1800)
page = await mp.currentPage()
steps.push(await capture(mp, page, 'S22-salary-list'))

let card = null
for (const element of await page.$$('*')) {
  try {
    const classes = String(await element.attribute('class') || '')
    const content = String(await element.text()).trim()
    if (classes.includes('sheet-card') && content.includes('2027-01')) { card = element; break }
  } catch {}
}
if (!card) throw new Error('未找到 2027-01 工资单卡')
await card.tap()
await sleep(1600)
page = await mp.currentPage()
steps.push(await capture(mp, page, 'S23-salary-detail'))

await mp.navigateBack(1)
await sleep(1000)
await mp.navigateBack(1)
await sleep(1200)
page = await mp.currentPage()
await tapByTextAndClass(page, '员工提成', 'fin-quick__item')
await sleep(1800)
page = await mp.currentPage()
steps.push(await capture(mp, page, 'S24-commission-list'))

const sizes = await measure(page, ['.pay-page', '.pay-body', '.commission-card', '.commission-card__amounts'])
const systemInfo = await mp.systemInfo()
const evidence = {
  generatedAt: new Date().toISOString(),
  appId: 'wxe5cc5fec9e50a9a9',
  project: `${AUTOMATOR_ROOT}/dist/dev/mp-weixin`,
  systemInfo,
  steps,
  sizes,
  console: logs
}
fs.writeFileSync(path.join(RUN, 'raw/api/single-boss-app-visual-touch.json'), JSON.stringify(evidence, null, 2))
fs.writeFileSync(path.join(RUN, 'raw/console/single-boss-app-visual-touch-console.json'), JSON.stringify(logs, null, 2))
console.log(JSON.stringify({ path: page.path, systemInfo, sizes, consoleCount: logs.length }, null, 2))
await mp.disconnect()
