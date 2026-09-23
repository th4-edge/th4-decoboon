const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const log = []

;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const step = async (name, fn) => {
    try { const r = await fn(); log.push({ step: name, ok: true, result: r }) }
    catch (e) { log.push({ step: name, ok: false, error: String(e.message || e) }) }
  }
  const shot = async (name) => { await mp.screenshot({ path: `${RUN}/raw/screenshots/${name}.png` }).catch(() => {}) }

  await step('初始化-当前页', async () => (await mp.currentPage()).path)
  await shot('01-登录页-初始')

  await step('勾选协议', async () => {
    const page = await mp.currentPage()
    const el = await page.$('.login__agree')
    if (!el) throw new Error('未找到协议勾选框 .login__agree')
    await el.tap()
    await sleep(500)
    const agree = await page.$('.login__checkbox--on')
    return { checked: !!agree }
  })
  await shot('02-登录页-已勾选协议')

  await step('点击微信登录', async () => {
    const page = await mp.currentPage()
    const el = await page.$('.login__submit')
    if (!el) throw new Error('未找到登录按钮 .login__submit')
    await el.tap()
    await sleep(3000)
    return 'tapped'
  })

  await step('登录后页面', async () => {
    const page = await mp.currentPage()
    return { path: page.path, query: page.query }
  })
  await shot('03-登录后首页')

  // 读取本地会话（只取角色与会话类型，不落 token）
  await step('会话快照', async () => {
    const r = await mp.evaluate(() => {
      const raw = wx.getStorageSync('jzb_session') || wx.getStorageSync('session') || null
      if (!raw) {
        const keys = wx.getStorageInfoSync().keys
        return { keys }
      }
      return { role: raw.role, sessionType: raw.sessionType, hasToken: !!raw.token, customerProjects: (raw.customer && raw.customer.projectIds) || [], company: raw.company && raw.company.name }
    })
    return r
  })

  await step('首页正文', async () => {
    const page = await mp.currentPage()
    try { return String(await page.text()).slice(0, 300) } catch (e) { return 'text() 不可用' }
  })

  console.log(JSON.stringify(log, null, 2))
  require('fs').writeFileSync(`${RUN}/raw/console/login-sequence.json`, JSON.stringify(log, null, 2))
})()
