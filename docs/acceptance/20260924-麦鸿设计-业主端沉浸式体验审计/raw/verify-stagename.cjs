const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  await mp.reLaunch(`/subpackages/project/pages/detail?id=${PID}`); await sleep(6500)
  const page = await mp.currentPage()
  const el = await page.$('.pd')
  const text = el ? String(await el.text()).replace(/\s+/g, ' ') : '(未匹配)'
  await mp.screenshot({ path: `${RUN}/raw/screenshots/fix2-项目详情-当前阶段.png` })
  const pick = text.match(/当前：[^·]*· 阶段 \d+\/\d+/)
  console.log(JSON.stringify({ path: page.path, matched: pick ? pick[0] : null, head: text.slice(0, 220) }, null, 2))
  fs.writeFileSync(`${RUN}/raw/console/verify-stagename.json`, JSON.stringify({ text }, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
