const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'
const withTimeout = (promise, ms, label) => Promise.race([
  promise,
  new Promise((_, rej) => setTimeout(() => rej(new Error(`TIMEOUT ${label} ${ms}ms`)), ms))
])
const out = []
;(async () => {
  const t0 = Date.now()
  const mp = await withTimeout(automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' }), 10000, 'connect')
  out.push({ step: 'connect', ms: Date.now() - t0 })
  try {
    const page = await withTimeout(mp.currentPage(), 8000, 'currentPage')
    out.push({ step: 'currentPage', path: page.path, query: page.query })
    const agree = await withTimeout(page.$$('.login__agree'), 8000, 'find agree')
    out.push({ step: 'findAgree', count: agree.length })
    const submit = await withTimeout(page.$$('.login__submit'), 8000, 'find submit')
    out.push({ step: 'findSubmit', count: submit.length })
    const text = await withTimeout(page.text(), 8000, 'text').catch((e) => String(e.message))
    out.push({ step: 'pageText', text: String(text).slice(0, 160) })
  } catch (e) {
    out.push({ step: 'error', error: String(e.message || e) })
  }
  console.log(JSON.stringify(out, null, 2))
  try { mp.disconnect() } catch (e) {}
  require('fs').writeFileSync(`${RUN}/raw/console/probe.json`, JSON.stringify(out, null, 2))
  process.exit(0)
})()
