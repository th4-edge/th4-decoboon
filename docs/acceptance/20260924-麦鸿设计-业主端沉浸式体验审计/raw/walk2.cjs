const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function texts(mp) {
  const page = await mp.currentPage()
  try {
    const els = await page.$$('text')
    const arr = await Promise.all(els.map((e) => e.text().catch(() => '')))
    return arr.map((t) => String(t).trim()).filter(Boolean)
  } catch (e) { return ['TEXT_ERR ' + e.message] }
}

;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const session = await mp.evaluate(() => {
    const raw = wx.getStorageSync('jzb_session_v2') || {}
    return { role: raw.role, sessionType: raw.sessionType, company: raw.company && raw.company.name,
      customer: raw.customer && { name: raw.customer.name, projectIds: raw.customer.projectIds, accesses: (raw.customer.accesses || []).map((a) => ({ displayName: a.displayName, projectId: a.projectId, status: a.status })) } }
  })
  const steps = [
    ['01-我的家首页', 'pages/index/index'],
    ['02-项目详情-进度', `/subpackages/project/pages/detail?id=${PID}`],
    ['03-工地相册', `/subpackages/project/pages/album?id=${PID}`],
    ['04-项目旅程', `/subpackages/project/pages/journey?projectId=${PID}`],
    ['05-量房档案', `/subpackages/project/pages/owner-measure?projectId=${PID}`],
    ['06-设计方案', `/subpackages/project/pages/owner-design?projectId=${PID}`],
    ['07-报价清单', `/subpackages/project/pages/owner-quote?projectId=${PID}`],
    ['08-选材记录', `/subpackages/project/pages/owner-selection?projectId=${PID}`],
    ['09-增减项', `/subpackages/project/pages/owner-change?projectId=${PID}`],
    ['10-验收整改', `/subpackages/project/pages/owner-acceptance?projectId=${PID}`],
    ['11-付款计划', `/subpackages/project/pages/owner-payment?projectId=${PID}`],
    ['12-合同档案', `/subpackages/project/pages/owner-contract?projectId=${PID}`],
    ['13-意见与建议', `/subpackages/project/pages/feedback-list?projectId=${PID}`]
  ]
  const results = []
  for (const [name, url] of steps) {
    const r = { name, url }
    try {
      await mp.reLaunch(url)
      await sleep(2400)
      const cur = await mp.currentPage()
      r.path = cur.path
      r.texts = await texts(mp)
      await mp.screenshot({ path: `${RUN}/raw/screenshots/${name}.png` })
    } catch (e) { r.error = String(e.message || e) }
    results.push(r)
  }
  fs.writeFileSync(`${RUN}/raw/console/owner-walk.json`, JSON.stringify({ session, results }, null, 2))
  console.log(JSON.stringify({ session, pages: results.map((r) => ({ name: r.name, path: r.path, n: (r.texts || []).length, texts: (r.texts || []).slice(0, 26), error: r.error })) }, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
