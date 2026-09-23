const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const page = await mp.currentPage()
  const out = { path: page.path }
  // 按钮文案 + 是否处于手机号授权态
  const sub = await page.$('.login__submit')
  out.submitText = sub ? (await sub.text().catch(() => '?')) : null
  out.submitAttr = sub ? await sub.attribute('open-type').catch(() => null) : null
  // 页面已渲染的关键状态
  out.panelTitle = await mp.evaluate(() => {
    const raw = wx.getStorageSync('jzb_session_v2')
    return { hasToken: !!wx.getStorageSync('jzb_token'), sessionRole: raw && raw.role }
  }).catch((e) => String(e.message))
  const code = await mp.evaluate(() => new Promise((r) => wx.login({ success: (x) => r(x.code), fail: () => r('') })))
  out.code = code ? code.slice(0, 6) + '...' : ''
  process.stdout.write(JSON.stringify(out, null, 2))
  process.stdout.write('\nCODE=' + code)
  process.exit(0)
})()
