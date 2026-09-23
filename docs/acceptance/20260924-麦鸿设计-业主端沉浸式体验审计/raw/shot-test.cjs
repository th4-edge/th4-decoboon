const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const res = {}
  const t = Date.now()
  try {
    const r = await Promise.race([
      mp.screenshot({ path: `${RUN}/raw/screenshots/probe-shot.png` }),
      new Promise((_, rej) => setTimeout(() => rej(new Error('SCREENSHOT_TIMEOUT')), 15000))
    ])
    res.screenshot = String(r || 'ok')
  } catch (e) { res.screenshotError = String(e.message || e) }
  res.ms = Date.now() - t
  console.log(JSON.stringify(res, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
