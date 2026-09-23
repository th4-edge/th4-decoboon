const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
async function texts(mp) {
  const page = await mp.currentPage()
  const els = await page.$$('text')
  const arr = await Promise.all(els.map((e) => e.text().catch(() => '')))
  return arr.map((t) => String(t).trim()).filter(Boolean)
}
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const targets = [
    ['recheck-报价清单', `/subpackages/project/pages/owner-quote?projectId=${PID}`],
    ['recheck-验收整改', `/subpackages/project/pages/owner-acceptance?projectId=${PID}`],
    ['recheck-我的家首页', `pages/index/index`]
  ]
  const out = []
  for (const [name, url] of targets) {
    const r = { name, url }
    try {
      if (url.startsWith('pages/')) { await mp.switchTab(url).catch(async () => { await mp.reLaunch(url) }) }
      else { await mp.reLaunch(url) }
      await sleep(7000)
      r.path = (await mp.currentPage()).path
      r.texts = await texts(mp)
      await mp.screenshot({ path: `${RUN}/raw/screenshots/${name}.png` })
    } catch (e) { r.error = String(e.message || e) }
    out.push(r)
  }
  fs.writeFileSync(`${RUN}/raw/console/recheck.json`, JSON.stringify(out, null, 2))
  console.log(JSON.stringify(out, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
