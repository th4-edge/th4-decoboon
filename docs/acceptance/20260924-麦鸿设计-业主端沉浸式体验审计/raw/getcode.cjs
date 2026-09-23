const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const code = await mp.evaluate(() => new Promise((resolve) => {
    wx.login({ success: (r) => resolve(r.code), fail: () => resolve('') })
  }))
  process.stdout.write(code || '')
  process.exit(0)
})()
