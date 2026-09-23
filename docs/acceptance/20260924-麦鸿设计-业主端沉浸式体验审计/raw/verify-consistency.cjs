const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const targets = [
    ['我的家首页', null, '.home__body'],
    ['项目详情', `/subpackages/project/pages/detail?id=${PID}`, '.pd'],
    ['项目旅程', `/subpackages/project/pages/journey?projectId=${PID}`, '.journey']
  ]
  const out = []
  for (const [name, url, sel] of targets) {
    if (url) { await mp.reLaunch(url); await sleep(6000) }
    else await sleep(2000)
    const page = await mp.currentPage()
    const el = await page.$(sel)
    const text = el ? String(await el.text()).replace(/\s+/g, '').trim() : ''
    out.push({
      page: name,
      path: page.path,
      状态: text.includes('施工中') ? '施工中' : (text.includes('待验收') ? '待验收' : '未识别'),
      当前阶段: text.includes('开工交底') ? '开工交底' : '未识别'
    })
    await mp.screenshot({ path: `${RUN}/raw/screenshots/consistency-${name}.png` })
  }
  fs.writeFileSync(`${RUN}/raw/console/verify-consistency.json`, JSON.stringify(out, null, 2))
  console.log(JSON.stringify(out, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
