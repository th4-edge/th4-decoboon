const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const page = await mp.currentPage()
  const d = await page.data()
  const out = { path: page.path, keys: Object.keys(d || {}).length, sample: {} }
  for (const k of Object.keys(d || {})) {
    const v = d[k]
    if (typeof v === 'string' && v.length < 80) out.sample[k] = v
    else if (typeof v === 'number' || typeof v === 'boolean') out.sample[k] = v
    else if (Array.isArray(v)) out.sample[k] = `array(${v.length})`
  }
  // 页面级错误（如果有）
  out.error = await mp.evaluate(() => (getApp().globalData && getApp().globalData.__lastError) || null).catch(() => null)
  console.log(JSON.stringify(out, null, 2).slice(0, 1500))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
