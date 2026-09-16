/**
 * Boss 角色小程序全路由巡检（微信开发者工具 + miniprogram-automator）
 * 用法: NODE_PATH=/private/tmp/mp-automator/node_modules node route-sweep.cjs [outFile]
 */
const automator = require('miniprogram-automator')
const fs = require('fs')
const path = require('path')

const sleep = ms => new Promise(r => setTimeout(r, ms))
const CUST = '225289174440214528'
const CUST2 = '225676558629601280'
const PROJECT = '225673146043203584'
const DESIGN = '225672867549806592'
const QUOTE = '225672894754062336'
const STAGE = '225673146114506752'
const TASK = '225673146131283968'
const EMP = '1789402550555064838'

const ROUTES = [
  ['工作台', '/pages/index/index'],
  ['客户列表', '/pages/customer/list'],
  ['项目列表', '/pages/project/list'],
  ['动态', '/pages/dynamic/index'],
  ['我的', '/pages/mine/index'],
  ['财务总览', '/pages/finance/index'],
  ['我的团队', '/subpackages/team/pages/list'],
  ['团队工作台', '/subpackages/team/pages/workbench'],
  ['设计师列表', '/subpackages/team/pages/designers'],
  ['成员管理', '/subpackages/company/pages/members'],
  ['成员详情', `/subpackages/company/pages/member-detail?empId=${EMP}`],
  ['企业信息', '/subpackages/company/pages/info'],
  ['邀请成员', '/subpackages/company/pages/invite'],
  ['成员交接', `/subpackages/company/pages/handover?empId=${EMP}`],
  ['成员离职', '/subpackages/company/pages/resign'],
  ['账号密码', '/subpackages/account/pages/password'],
  ['客户详情', `/subpackages/customer/pages/detail?id=${CUST}`],
  ['客户编辑', `/subpackages/customer/pages/edit?id=${CUST}`],
  ['客户跟进', `/subpackages/customer/pages/follow?id=${CUST}`],
  ['客户量房', `/subpackages/customer/pages/measure?id=${CUST}`],
  ['量房记录', `/subpackages/customer/pages/measure-record?customerId=${CUST}`],
  ['设计列表', `/subpackages/design/pages/list?customerId=${CUST}`],
  ['设计需求列表', '/subpackages/design/pages/requirements'],
  ['设计需求', `/subpackages/design/pages/requirement?customerId=${CUST}`],
  ['设计方案', `/subpackages/design/pages/scheme?designId=${DESIGN}`],
  ['设计洽谈', `/subpackages/design/pages/talk?designId=${DESIGN}`],
  ['目录管理', '/subpackages/quote/pages/catalog/manage'],
  ['报价列表', '/subpackages/quote/pages/list'],
  ['报价详情', `/subpackages/quote/pages/detail?id=${QUOTE}`],
  ['报价编辑', `/subpackages/quote/pages/edit?customerId=${CUST}`],
  ['合同签约', '/subpackages/project/pages/create'],
  ['项目详情', `/subpackages/project/pages/detail?id=${PROJECT}`],
  ['项目计划', `/subpackages/project/pages/plan?id=${PROJECT}`],
  ['计划编辑', `/subpackages/project/pages/plan-edit?id=${STAGE}`],
  ['项目成员', `/subpackages/project/pages/members?id=${PROJECT}`],
  ['项目邀请', `/subpackages/project/pages/invite?id=${PROJECT}`],
  ['项目相册', `/subpackages/project/pages/album?id=${PROJECT}`],
  ['阶段详情', `/subpackages/project/pages/stage/detail?id=${STAGE}`],
  ['任务详情', `/subpackages/project/pages/task/detail?id=${TASK}`],
  ['任务编辑', `/subpackages/project/pages/task/edit?id=${TASK}`],
  ['日报编辑', `/subpackages/project/pages/report/edit?projectId=${PROJECT}&stageId=${STAGE}&taskId=${TASK}`],
  ['日报列表', `/subpackages/project/pages/report/daily?id=${PROJECT}`],
  ['验收编辑', `/subpackages/project/pages/accept/edit?stageId=${STAGE}`],
  ['财务台账', `/subpackages/finance/pages/ledger?projectId=${PROJECT}`],
  ['应收列表', `/subpackages/finance/pages/receivables?projectId=${PROJECT}`],
  ['收款列表', `/subpackages/finance/pages/receipts?projectId=${PROJECT}`],
  ['应付列表', `/subpackages/finance/pages/payables?projectId=${PROJECT}`],
  ['付款列表', `/subpackages/finance/pages/payments?projectId=${PROJECT}`],
  ['财务项目列表', '/subpackages/finance/pages/projects'],
  ['财务项目详情', `/subpackages/finance/pages/project?projectId=${PROJECT}`],
  ['财务记账', `/subpackages/finance/pages/entry?projectId=${PROJECT}`],
  ['经营分析', '/subpackages/statistic/pages/analysis'],
  ['人员绩效', '/subpackages/statistic/pages/performance'],
  ['绩效考核', '/subpackages/statistic/pages/assessment'],
  ['消息通知', '/subpackages/message/pages/list'],
  ['订阅套餐', '/subpackages/subscription/pages/index'],
  ['供应链', '/subpackages/supply/pages/index']
]

async function main () {
  const out = process.argv[2] || path.join(__dirname, 'route-sweep-result.json')
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const events = []
  mp.on('exception', e => events.push({ kind: 'exception', at: Date.now(), detail: JSON.stringify(e).slice(0, 400) }))
  mp.on('console', m => {
    const text = (m.args || []).map(a => typeof a === 'string' ? a : JSON.stringify(a)).join(' ')
    if (/error|失败|异常|undefined|null/i.test(text) && !/^\{/.test(text)) {
      events.push({ kind: 'console', at: Date.now(), detail: `${m.type}: ${text}`.slice(0, 400) })
    }
  })

  const results = []
  for (const [name, url] of ROUTES) {
    const target = url.split('?')[0]
    const startedAt = Date.now()
    const row = { name, url, target, actual: null, ok: false, text: [], error: null, ms: 0 }
    try {
      await mp.reLaunch(url)
      await sleep(2600)
      const page = await mp.currentPage()
      row.actual = page.path
      row.ok = page.path === target.replace(/^\//, '')
      const data = await page.data()
      row.dataSize = JSON.stringify(data || {}).length
      const els = await page.$$('text')
      const texts = []
      for (const el of els.slice(0, 22)) {
        try {
          const t = ((await el.text()) || '').replace(/\s+/g, ' ').trim()
          if (t) texts.push(t.slice(0, 30))
        } catch (_) {}
      }
      row.text = texts
      if (!row.ok) row.error = `redirected:${page.path}`
    } catch (e) {
      row.error = String(e && e.message || e).slice(0, 200)
    }
    row.ms = Date.now() - startedAt
    results.push(row)
    console.log(`${row.ok ? 'OK ' : 'FAIL'} ${name} -> ${row.actual || '-'} (${row.ms}ms) ${row.text.slice(0, 6).join(' | ')}${row.error ? ' ERR=' + row.error : ''}`)
  }
  fs.writeFileSync(out, JSON.stringify({ generatedAt: new Date().toISOString(), results, events }, null, 1))
  console.log('--- saved:', out)
  console.log('events:', events.length)
  events.slice(0, 20).forEach(e => console.log('  ', e.kind, e.detail.slice(0, 160)))
  await mp.disconnect()
}
main().catch(e => { console.error('ERR', e && e.message); process.exit(1) })
