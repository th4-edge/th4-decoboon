const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
async function grab(mp, sel) {
  const page = await mp.currentPage()
  const el = await page.$(sel)
  return { path: page.path, query: page.query, text: el ? String(await el.text()).replace(/\s+/g, ' ').trim() : '(容器未匹配:' + sel + ')' }
}
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const out = []
  // ① 我的家首页（登录后就在这一页）
  await sleep(2500)
  out.push({ step: '我的家首页', ...(await grab(mp, '.home__body').catch(async () => await grab(mp, 'page'))) })
  await mp.screenshot({ path: `${RUN}/raw/screenshots/journey-01-我的家首页.png` })

  const steps = [
    ['项目旅程（全链路）', `/subpackages/project/pages/journey?projectId=${PID}`, '.journey'],
    ['量房档案（交房原样）', `/subpackages/project/pages/owner-measure?projectId=${PID}`, '.measure'],
    ['设计方案（做成什么样）', `/subpackages/project/pages/owner-design?projectId=${PID}`, '.design'],
    ['报价清单（要花多少钱）', `/subpackages/project/pages/owner-quote?projectId=${PID}`, '.quote'],
    ['合同档案（签了什么）', `/subpackages/project/pages/owner-contract?projectId=${PID}`, '.ocontract'],
    ['选材记录（用什么材料）', `/subpackages/project/pages/owner-selection?projectId=${PID}`, '.oselect'],
    ['增减项（有没有加钱）', `/subpackages/project/pages/owner-change?projectId=${PID}`, '.ochange'],
    ['付款计划（什么时候交钱）', `/subpackages/project/pages/owner-payment?projectId=${PID}`, '.opayment'],
    ['验收整改（要我确认什么）', `/subpackages/project/pages/owner-acceptance?projectId=${PID}`, '.oaccept'],
    ['工地相册（每天干了什么）', `/subpackages/project/pages/album?id=${PID}`, '.album'],
    ['项目详情（工地首页）', `/subpackages/project/pages/detail?id=${PID}`, '.pd'],
    ['意见与建议', `/subpackages/project/pages/feedback-list?projectId=${PID}`, '.fl']
  ]
  for (const [name, url, sel] of steps) {
    try {
      await mp.reLaunch(url); await sleep(5200)
      out.push({ step: name, ...(await grab(mp, sel)) })
      await mp.screenshot({ path: `${RUN}/raw/screenshots/journey-${name.slice(0, 2)}.png` })
    } catch (e) { out.push({ step: name, error: String(e.message || e) }) }
  }
  fs.writeFileSync(`${RUN}/raw/console/owner-journey-20260924.json`, JSON.stringify(out, null, 2))
  for (const r of out) console.log('\n===== ' + r.step + ' | ' + (r.path || '') + (r.error ? ' ERR:' + r.error : '') + '\n' + String(r.text || '').slice(0, 1100))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
