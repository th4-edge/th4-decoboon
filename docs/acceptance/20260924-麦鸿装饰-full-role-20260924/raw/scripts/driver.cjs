const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const fs = require('fs')
const path = require('path')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿装饰-full-role-20260924'
const PROJECT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin'
const sleep = ms => new Promise(r => setTimeout(r, ms))
async function pageText(page) { try { return String(await page.text()).slice(0, 20000) } catch { return '' } }
;(async () => {
  const cmd = process.argv[2] || 'state'
  const mp = await automator.launch({ cliPath: '/Applications/wechatwebdevtools.app/Contents/MacOS/cli', projectPath: PROJECT, timeout: 60000 })
  try {
    let page = await mp.currentPage()
    const out = { cmd, at: new Date().toISOString(), path: page.path, query: page.query }
    if (cmd === 'state') {
      out.pageData = await page.data().catch(e => ({ error: String(e) }))
      out.text = await pageText(page)
    } else if (cmd === 'text') {
      out.text = await pageText(page)
    } else if (cmd === 'shot') {
      const name = process.argv[3] || `state-${Date.now()}`
      const target = path.join(RUN, 'raw', 'screenshots', `${name}.png`)
      fs.mkdirSync(path.dirname(target), { recursive: true })
      await mp.screenshot({ path: target })
      out.screenshot = target
    } else if (cmd === 'login') {
      const token = process.argv[3]
      await mp.callWxMethod('clearStorageSync')
      page = await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(token)}`)
      await sleep(3500)
      page = await mp.currentPage()
      out.path = page.path
      out.query = page.query
      out.session = await mp.evaluate(() => {
        const s = wx.getStorageSync('jzb_session') || {}
        return { role: s.role, emp: s.emp, company: s.company, hasToken: !!s.token }
      })
      out.text = await pageText(page)
    } else if (cmd === 'relaunch') {
      page = await mp.reLaunch(process.argv[3])
      await sleep(1000)
      out.path = page.path; out.query = page.query; out.text = await pageText(page)
    } else if (cmd === 'navigate') {
      page = await mp.navigateTo(process.argv[3])
      await sleep(1000)
      out.path = page.path; out.query = page.query; out.text = await pageText(page)
    } else if (cmd === 'eval') {
      out.result = await mp.evaluate(process.argv[3])
    }
    console.log(JSON.stringify(out, null, 2))
  } finally {
    try { mp.disconnect() } catch {}
  }
})().catch(err => { console.error(err && err.stack || err); process.exit(1) })
