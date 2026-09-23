const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const targets = [
    ['fix-项目详情', `/subpackages/project/pages/detail?id=${PID}`, '.pd'],
    ['fix-项目旅程', `/subpackages/project/pages/journey?projectId=${PID}`, '.journey'],
    ['fix-验收整改', `/subpackages/project/pages/owner-acceptance?projectId=${PID}`, '.oaccept']
  ]
  const out = []
  for (const [name, url, sel] of targets) {
    await mp.reLaunch(url); await sleep(6000)
    const page = await mp.currentPage()
    const el = await page.$(sel)
    const text = el ? await el.text() : '(no container)'
    await mp.screenshot({ path: `${RUN}/raw/screenshots/${name}.png` })
    out.push({ name, path: page.path, text: String(text).replace(/\s+/g, ' ').slice(0, 900) })
  }
  fs.writeFileSync(`${RUN}/raw/console/verify-fix.json`, JSON.stringify(out, null, 2))
  for (const r of out) console.log('\n===== ' + r.name + '\n' + r.text)
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
