const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const out = {}
  // 清掉本地存储，让登录页回到初始态（needPhone 复位）
  await mp.evaluate(() => wx.clearStorageSync())
  await mp.reLaunch('/pages/auth/login'); await sleep(2500)
  let page = await mp.currentPage()
  out.afterClear = { path: page.path }
  const submit0 = await page.$('.login__submit')
  out.submitTextBefore = submit0 ? await submit0.text().catch(() => '?') : null
  // 勾协议
  const cb = await page.$('.login__checkbox--on')
  if (!cb) { const a = await page.$('.login__agree'); if (a) { await a.tap().catch(() => {}); await sleep(600) } }
  out.agreeChecked = !!(await page.$('.login__checkbox--on'))
  // 点登录（不等待 tap，避免导航打断）
  const submit = await page.$('.login__submit')
  submit.tap().catch(() => {})
  await sleep(6000)
  page = await mp.currentPage()
  out.afterTap = { path: page.path }
  out.storage = await mp.evaluate(() => ({ hasToken: !!wx.getStorageSync('jzb_token'), keys: wx.getStorageInfoSync().keys }))
  const s2 = await page.$('.login__submit')
  out.submitTextAfter = s2 ? await s2.text().catch(() => '?') : null
  await mp.screenshot({ path: '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计/raw/screenshots/relogin-after.png' })
  console.log(JSON.stringify(out, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
