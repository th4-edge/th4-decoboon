const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const out = {}
  // 报价清单：金额组件是否存在、内部文本是什么
  await mp.reLaunch(`/subpackages/project/pages/owner-quote?projectId=${PID}`); await sleep(6000)
  let page = await mp.currentPage()
  out.quote = { path: page.path }
  for (const sel of ['.jz-money', '.quote__summary-row', '.quote__summary-total', '.quote__summary-note']) {
    try {
      const els = await page.$$(sel)
      out.quote[sel] = { count: els.length, texts: (await Promise.all(els.map((e) => e.text().catch(() => '<err>')))).map((t) => String(t).replace(/\s+/g, ' ').trim()).slice(0, 6) }
    } catch (e) { out.quote[sel] = { error: String(e.message) } }
  }
  // 验收整改：明细卡片组件是否存在
  await mp.reLaunch(`/subpackages/project/pages/owner-acceptance?projectId=${PID}`); await sleep(6000)
  page = await mp.currentPage()
  out.acceptance = { path: page.path }
  for (const sel of ['.ofl__card', '.ofl__empty', '.factstrip__item', '.oaccept__empty']) {
    try {
      const els = await page.$$(sel)
      out.acceptance[sel] = { count: els.length, texts: (await Promise.all(els.map((e) => e.text().catch(() => '<err>')))).map((t) => String(t).replace(/\s+/g, ' ').trim()).slice(0, 4) }
    } catch (e) { out.acceptance[sel] = { error: String(e.message) } }
  }
  fs.writeFileSync(`${RUN}/raw/console/domprobe.json`, JSON.stringify(out, null, 2))
  console.log(JSON.stringify(out, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
