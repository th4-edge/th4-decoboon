const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  await sleep(2500)
  const page = await mp.currentPage()
  const el = await page.$('.home__body')
  const text = el ? String(await el.text()).replace(/\s+/g, ' ').trim() : '(未匹配)'
  await mp.screenshot({ path: `${RUN}/raw/screenshots/fix3-我的家首页-状态一致.png` })
  const out = {
    path: page.path,
    项目状态断言: /施工中/.test(text) ? '施工中 ✅' : (/待验收/.test(text) ? '仍为待验收 ❌' : '未识别'),
    当前阶段断言: /当前阶段\s*开工交底/.test(text.replace(/\s+/g, '')) ? '开工交底 ✅' : (text.includes('当前阶段待验收') ? '仍为待验收 ❌' : '未识别'),
    head: text.slice(0, 300)
  }
  console.log(JSON.stringify(out, null, 2))
  fs.writeFileSync(`${RUN}/raw/console/verify-home.json`, JSON.stringify({ text }, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
