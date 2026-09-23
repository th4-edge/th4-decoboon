const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
// 关键：element.text() 会聚合子孙节点（含自定义组件），page.$$('text') 不会
async function containerText(mp, selector) {
  const page = await mp.currentPage()
  const el = await page.$(selector)
  if (!el) return { selector, missing: true }
  const t = await el.text().catch((e) => 'ERR ' + e.message)
  return { selector, text: String(t).replace(/[ \t]+/g, ' ').split('\n').map((x) => x.trim()).filter(Boolean) }
}
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const steps = [
    ['01-项目详情', `/subpackages/project/pages/detail?id=${PID}`, '.pd'],
    ['02-工地相册', `/subpackages/project/pages/album?id=${PID}`, '.album'],
    ['03-项目旅程', `/subpackages/project/pages/journey?projectId=${PID}`, '.journey'],
    ['04-量房档案', `/subpackages/project/pages/owner-measure?projectId=${PID}`, '.measure'],
    ['05-设计方案', `/subpackages/project/pages/owner-design?projectId=${PID}`, '.design'],
    ['06-报价清单', `/subpackages/project/pages/owner-quote?projectId=${PID}`, '.quote'],
    ['07-选材记录', `/subpackages/project/pages/owner-selection?projectId=${PID}`, '.oselect'],
    ['08-增减项', `/subpackages/project/pages/owner-change?projectId=${PID}`, '.ochange'],
    ['09-验收整改', `/subpackages/project/pages/owner-acceptance?projectId=${PID}`, '.oaccept'],
    ['10-付款计划', `/subpackages/project/pages/owner-payment?projectId=${PID}`, '.opayment'],
    ['11-合同档案', `/subpackages/project/pages/owner-contract?projectId=${PID}`, '.ocontract'],
    ['12-意见与建议', `/subpackages/project/pages/feedback-list?projectId=${PID}`, '.fl']
  ]
  const results = []
  for (const [name, url, sel] of steps) {
    const r = { name, url, sel }
    try {
      await mp.reLaunch(url); await sleep(5500)
      const ct = await containerText(mp, sel)
      r.path = (await mp.currentPage()).path
      r.content = ct.text || []
      r.missing = ct.missing || false
      await mp.screenshot({ path: `${RUN}/raw/screenshots/v2-${name}.png` })
    } catch (e) { r.error = String(e.message || e) }
    results.push(r)
  }
  fs.writeFileSync(`${RUN}/raw/console/owner-walk-v2.json`, JSON.stringify(results, null, 2))
  for (const r of results) {
    console.log(`\n===== ${r.name} | ${r.path} | ${r.sel} | ${(r.content || []).length} 行 ${r.error ? 'ERR:' + r.error : ''}`)
    for (const line of (r.content || []).slice(0, 60)) console.log('  ', line.slice(0, 120))
  }
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
