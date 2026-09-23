const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const steps = [
    ['P0-选择页', '/pages/company/join', '.entry-body'],
    ['P0-业主入口', '/pages/owner/enter', '.owner-enter__body'],
    ['P0-业主邀请兼容跳转', '/pages/invite/accept?type=customer', '.owner-enter__body']
  ]
  const out = []
  for (const [name, url, sel] of steps) {
    try {
      await mp.reLaunch(url); await sleep(3000)
      const page = await mp.currentPage()
      const el = await page.$(sel)
      out.push({ name, path: page.path, container: !!el, text: el ? String(await el.text()).replace(/\s+/g, ' ').slice(0, 420) : '(容器未匹配)' })
      await mp.screenshot({ path: `${RUN}/raw/screenshots/${name}.png` })
    } catch (e) { out.push({ name, error: String(e.message || e) }) }
  }
  fs.writeFileSync(`${RUN}/raw/console/verify-p0.json`, JSON.stringify(out, null, 2))
  console.log(JSON.stringify(out, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
