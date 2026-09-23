const p = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(p)
;(async () => {
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const pid = '228259849996075008'
  await mp.reLaunch(`/subpackages/project/pages/owner-acceptance?projectId=${pid}`)
  await new Promise((r) => setTimeout(r, 2500))
  const page = await mp.currentPage()
  let texts = []
  try {
    const els = await page.$$('text')
    texts = (await Promise.all(els.map((e) => e.text().catch(() => '')))).map((t) => String(t).trim()).filter(Boolean)
  } catch (e) { texts = ['ERR ' + e.message] }
  console.log(JSON.stringify({ path: page.path, query: page.query, count: texts.length, texts: texts.slice(0, 40) }, null, 2))
  try { mp.disconnect() } catch (e) {}
  process.exit(0)
})()
