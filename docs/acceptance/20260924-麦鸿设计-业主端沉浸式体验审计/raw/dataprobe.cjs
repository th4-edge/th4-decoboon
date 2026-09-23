const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const fs = require('fs')
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const PID = '228259849996075008'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const out = {}
  for (const [name, url] of [['quote', `/subpackages/project/pages/owner-quote?projectId=${PID}`], ['acceptance', `/subpackages/project/pages/owner-acceptance?projectId=${PID}`]]) {
    await mp.reLaunch(url); await sleep(6000)
    const page = await mp.currentPage()
    const data = await page.data()
    // 只保留可读的业务字段，避免 dump 全量
    const pick = {}
    for (const k of Object.keys(data || {})) {
      const v = data[k]
      if (Array.isArray(v)) pick[k] = `array(${v.length})`
      else if (v && typeof v === 'object') pick[k] = `object:${Object.keys(v).slice(0, 8).join(',')}`
      else if (typeof v === 'string' && v.length < 60) pick[k] = v
      else if (typeof v === 'number' || typeof v === 'boolean') pick[k] = v
    }
    out[name] = { path: page.path, keys: Object.keys(data || {}).length, pick }
  }
  fs.writeFileSync(`${RUN}/raw/console/dataprobe.json`, JSON.stringify(out, null, 2))
  console.log(JSON.stringify(out, null, 2).slice(0, 4000))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
