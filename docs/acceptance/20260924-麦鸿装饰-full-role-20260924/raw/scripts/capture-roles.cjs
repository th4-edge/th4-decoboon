const automator = require('/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js')
const fs = require('fs')
const path = require('path')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿装饰-full-role-20260924'
const PROJECT = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin'
const sleep = ms => new Promise(r => setTimeout(r, ms))
const tokens = JSON.parse(fs.readFileSync('/private/tmp/maihong-full-role-tokens.json', 'utf8'))
const roles = {
  boss: { name: '钱小明', token: tokens['钱小明'], routes: ['pages/index/index','pages/customer/list','subpackages/design/pages/list','subpackages/quote/pages/list','pages/project/list','pages/finance/index','subpackages/finance/pages/projects','subpackages/team/pages/workbench','pages/dynamic/index','pages/mine/index'] },
  storeManager: { name: '王洪文', token: tokens['王洪文'], routes: ['pages/index/index','pages/customer/list','subpackages/customer/pages/measure-workbench','subpackages/design/pages/workbench','subpackages/design/pages/list','subpackages/team/pages/workbench','pages/project/list','subpackages/project/pages/selection-board','subpackages/company/pages/members','pages/mine/index'] },
  marketer: { name: '黄贤杰', token: tokens['黄贤杰'], routes: ['pages/index/index','pages/customer/list','subpackages/customer/pages/flow','subpackages/customer/pages/measure-workbench','subpackages/customer/pages/measure-record','pages/project/list','pages/mine/index'] },
  designerDong: { name: '董夏鹏', token: tokens['董夏鹏'], routes: ['pages/index/index','subpackages/design/pages/list','subpackages/design/pages/workbench','subpackages/design/pages/requirements','subpackages/quote/pages/list','subpackages/quote/pages/workbench','subpackages/project/pages/change-workbench','subpackages/project/pages/selection-board','pages/project/list','pages/mine/index'] },
  designerXie: { name: '谢玲玲', token: tokens['谢玲玲'], routes: ['pages/index/index','subpackages/design/pages/list','subpackages/design/pages/workbench','subpackages/design/pages/requirements','subpackages/quote/pages/list','subpackages/quote/pages/workbench','subpackages/project/pages/change-workbench','subpackages/project/pages/selection-board','pages/project/list','pages/mine/index'] },
  finance: { name: '吴濛濛', token: tokens['吴濛濛'], routes: ['pages/index/index','pages/finance/index','subpackages/finance/pages/ledger','subpackages/finance/pages/receivables','subpackages/finance/pages/receipts','subpackages/finance/pages/payables','subpackages/finance/pages/payments','subpackages/finance/pages/projects','subpackages/finance/pages/entry','pages/mine/index'] },
  pmXu: { name: '徐勇', token: tokens['徐勇'], routes: ['pages/index/index','subpackages/team/pages/workbench','pages/project/list','subpackages/project/pages/plan','subpackages/project/pages/material-plan-workbench','subpackages/project/pages/report/daily','subpackages/project/pages/accept/edit','pages/mine/index'] },
  pmWang: { name: '王昌富', token: tokens['王昌富'], routes: ['pages/index/index','subpackages/team/pages/workbench','pages/project/list','subpackages/project/pages/plan','subpackages/project/pages/material-plan-workbench','subpackages/project/pages/report/daily','subpackages/project/pages/accept/edit','pages/mine/index'] }
}
async function visibleText(page) {
  const els = await page.$$('*')
  const values = []
  for (const el of els) {
    try { const t = String(await el.text()).trim(); if (t) values.push(t) } catch {}
  }
  return [...new Set(values)].filter(t => t.length <= 500)
}
async function main() {
  const mp = await automator.launch({ cliPath: '/Applications/wechatwebdevtools.app/Contents/MacOS/cli', projectPath: PROJECT, timeout: 60000 })
  const logs = []
  for (const [role, cfg] of Object.entries(roles)) {
    const rec = { role, name: cfg.name, startedAt: new Date().toISOString(), pages: [], errors: [] }
    try {
      await mp.callWxMethod('clearStorageSync')
      await mp.reLaunch(`/pages/auth/login?devToken=${encodeURIComponent(cfg.token)}`)
      await sleep(3500)
      let page = await mp.currentPage()
      await mp.screenshot({ path: path.join(RUN, 'raw', 'screenshots', `${role}-00-login-home.png`) })
      rec.loginPath = page.path
      for (let i = 0; i < cfg.routes.length; i++) {
        const route = cfg.routes[i]
        const item = { route, path: null, query: {}, texts: [], pageData: null, screenshot: null, textError: null, dataError: null }
        try {
          await mp.reLaunch(route.startsWith('/') ? route : `/${route}`)
          await sleep(1300)
          page = await mp.currentPage()
          item.path = page.path; item.query = page.query
          try { item.texts = await visibleText(page) } catch (e) { item.textError = String(e) }
          try { item.pageData = await page.data() } catch (e) { item.dataError = String(e) }
          const outName = `${role}-${String(i + 1).padStart(2, '0')}-${route.replace(/[\\/?=&]/g, '-')}.png`
          await mp.screenshot({ path: path.join(RUN, 'raw', 'screenshots', outName) })
          item.screenshot = outName
          rec.pages.push(item)
        } catch (e) {
          item.dataError = String(e); rec.errors.push({ route, error: item.dataError }); rec.pages.push(item)
        }
      }
    } catch (err) { rec.errors.push({ phase: 'login', error: String(err && err.stack || err) }) }
    logs.push(rec)
    fs.writeFileSync(path.join(RUN, 'raw', 'accessibility', `role-${role}.json`), JSON.stringify(rec, null, 2))
    console.log(JSON.stringify({ role, name: cfg.name, pages: rec.pages.length, visitErrors: rec.errors.length, textErrors: rec.pages.filter(x=>x.textError).length }))
  }
  fs.writeFileSync(path.join(RUN, 'raw', 'accessibility', 'roles-summary.json'), JSON.stringify(logs, null, 2))
}
main().catch(err => { console.error(err && err.stack || err); process.exit(1) })
