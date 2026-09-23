const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function pageText(mp) {
  return await mp.evaluate(() => {
    const out = []
    const walk = (nodes) => {
      for (const n of nodes || []) {
        const t = (n.text || '').trim()
        if (t && t.length < 120) out.push(t)
        if (n.children) walk(n.children)
      }
    }
    // 通过 SelectorQuery 拿到渲染树文本
    return new Promise((resolve) => {
      wx.createSelectorQuery().selectAll('view,text').fields({ text: true }, (res) => {
        resolve((res || []).map((r) => (r && r.text ? String(r.text).trim() : '')).filter(Boolean).slice(0, 80))
      }).exec()
    })
  })
}

;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const session = await mp.evaluate(() => {
    const raw = wx.getStorageSync('jzb_session_v2')
    return { role: raw && raw.role, sessionType: raw && raw.sessionType, company: raw && raw.company && raw.company.name, projectIds: (raw && raw.customer && raw.customer.projectIds) || [], customerName: raw && raw.customer && raw.customer.name }
  })
  const pid = session.projectIds[0]
  const steps = [
    ['01-我的家首页', 'pages/index/index'],
    ['02-项目详情', `/subpackages/project/pages/detail?id=${pid}`],
    ['03-工地相册', `/subpackages/project/pages/album?id=${pid}`],
    ['04-项目旅程', `/subpackages/project/pages/journey?projectId=${pid}`],
    ['05-量房档案', `/subpackages/project/pages/owner-measure?projectId=${pid}`],
    ['06-设计方案', `/subpackages/project/pages/owner-design?projectId=${pid}`],
    ['07-报价清单', `/subpackages/project/pages/owner-quote?projectId=${pid}`],
    ['08-选材记录', `/subpackages/project/pages/owner-selection?projectId=${pid}`],
    ['09-增减项', `/subpackages/project/pages/owner-change?projectId=${pid}`],
    ['10-验收整改', `/subpackages/project/pages/owner-acceptance?projectId=${pid}`],
    ['11-付款计划', `/subpackages/project/pages/owner-payment?projectId=${pid}`],
    ['12-合同档案', `/subpackages/project/pages/owner-contract?projectId=${pid}`],
    ['13-意见与建议', `/subpackages/project/pages/feedback-list?projectId=${pid}`]
  ]
  const results = []
  for (const [name, url] of steps) {
    const r = { name, url }
    try {
      await mp.reLaunch(url)
      await sleep(2200)
      const cur = await mp.currentPage()
      r.path = cur.path
      r.query = cur.query
      await mp.screenshot({ path: `${RUN}/raw/screenshots/${name}.png` })
      const texts = await Promise.race([pageText(mp), sleep(6000).then(() => ['(text超时)'])])
      r.texts = texts
    } catch (e) { r.error = String(e.message || e) }
    results.push(r)
  }
  fs.writeFileSync(`${RUN}/raw/console/owner-walk.json`, JSON.stringify({ session, results }, null, 2))
  console.log(JSON.stringify({ session, summary: results.map((r) => ({ name: r.name, path: r.path, texts: (r.texts || []).slice(0, 12), error: r.error })) }, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
