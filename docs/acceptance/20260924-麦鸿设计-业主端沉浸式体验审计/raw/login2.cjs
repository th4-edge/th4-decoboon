const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const race = (pr, ms, label) => Promise.race([pr, new Promise((_, rej) => setTimeout(() => rej(new Error('TIMEOUT ' + label)), ms))])
const log = []
const rec = async (step, fn, ms = 8000) => {
  try { const r = await race(Promise.resolve().then(fn), ms, step); log.push({ step, ok: true, result: r ?? null }) ; return r }
  catch (e) { log.push({ step, ok: false, error: String(e.message || e) }); return undefined }
}
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const page = await mp.currentPage()
  await rec('起始页', () => `${page.path}`)

  const agreeOn0 = await rec('协议初始态', async () => (await page.$$('.login__checkbox--on')).length)
  if (!agreeOn0) {
    await rec('点击协议勾选', async () => { const el = await page.$('.login__agree'); await race(el.tap(), 5000, 'tapAgree'); return 'tapped' })
    await sleep(600)
    await rec('协议勾选后', async () => (await page.$$('.login__checkbox--on')).length)
  }
  await rec('截图-登录页已勾选', () => mp.screenshot({ path: `${RUN}/raw/screenshots/03-登录前-已勾选协议.png` }), 12000)

  // 点击登录：导航会打断 tap 的 Promise，因此不等待 tap 自身完成
  await rec('点击微信登录(不等待)', async () => {
    const el = await page.$('.login__submit')
    race(el.tap(), 5000, 'tapSubmit').catch(() => {})
    return 'tap-fired'
  })
  await sleep(5000)

  await rec('登录后页面', async () => {
    const cur = await mp.currentPage()
    return { path: cur.path, query: cur.query }
  })
  await rec('截图-登录后', () => mp.screenshot({ path: `${RUN}/raw/screenshots/04-登录后首页.png` }), 12000)
  await rec('会话快照', async () => mp.evaluate(() => {
    const keys = wx.getStorageInfoSync().keys
    const out = { keys }
    for (const k of keys) {
      if (/session|token|jzb/i.test(k)) {
        const v = wx.getStorageSync(k)
        if (v && typeof v === 'object') out[k] = { role: v.role, sessionType: v.sessionType, hasToken: !!v.token, company: v.company && v.company.name }
        else out[k] = typeof v
      }
    }
    return out
  }))
  console.log(JSON.stringify(log, null, 2))
  fs.writeFileSync(`${RUN}/raw/console/login-sequence.json`, JSON.stringify(log, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
