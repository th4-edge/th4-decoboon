const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const out = {}
  try {
    out.callWxLogin = await mp.callWxMethod('login')
  } catch (e) { out.loginError = String(e.message || e) }
  try {
    out.codeViaPage = await mp.evaluate(() => new Promise((resolve) => {
      wx.login({ success: (r) => resolve({ code: r.code ? 'HAS_CODE' : 'NO_CODE', len: (r.code || '').length }), fail: (e) => resolve({ fail: e.errMsg }) })
    }))
    // 真实 code 单独取一次并存到 raw（避免与日志混淆）
    out.realCode = await mp.evaluate(() => new Promise((resolve) => {
      wx.login({ success: (r) => resolve(r.code), fail: () => resolve(null) })
    }))
  } catch (e) { out.evalError = String(e.message || e) }
  console.log(JSON.stringify(out, null, 2))
  await mp.disconnect().catch(() => {})
})()
