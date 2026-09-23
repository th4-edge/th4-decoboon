const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const page = await mp.currentPage()
  const out = { path: page.path, query: page.query }
  out.storage = await mp.evaluate(() => ({ hasToken: !!wx.getStorageSync('jzb_token'), role: (wx.getStorageSync('jzb_session_v2') || {}).role }))
  for (const sel of ['.pd', '.pd__hero', 'page-state', '.measure']) {
    const el = await page.$(sel).catch(() => null)
    out[sel] = el ? String(await el.text().catch(() => '?')).replace(/\s+/g, ' ').slice(0, 200) : null
  }
  console.log(JSON.stringify(out, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
