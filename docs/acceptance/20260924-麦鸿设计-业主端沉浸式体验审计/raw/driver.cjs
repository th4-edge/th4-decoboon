/**
 * 业主端沉浸式体验审计 · 小程序驱动脚本
 * 连接微信开发者工具自动化端口，输出页面路径/数据并截图。
 * 用法: node driver.cjs <command> [args...]
 */
const path = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/node_modules/miniprogram-automator/out/index.js'
const automator = require(path)
const fs = require('fs')

const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计'

async function main() {
  const cmd = process.argv[2] || 'state'
  const miniProgram = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' })
  const out = { cmd, at: new Date().toISOString() }
  try {
    if (cmd === 'state') {
      const page = await miniProgram.currentPage()
      out.path = page.path
      out.query = page.query
      try { out.data = await page.data() } catch (e) { out.dataError = String(e) }
      out.systemInfo = await miniProgram.systemInfo().catch((e) => String(e))
    } else if (cmd === 'navigate') {
      const url = process.argv[3]
      const page = await miniProgram.navigateTo(url)
      await new Promise((r) => setTimeout(r, 1200))
      out.requested = url
      out.path = (await miniProgram.currentPage()).path
      out.query = (await miniProgram.currentPage()).query
    } else if (cmd === 'relaunch') {
      const url = process.argv[3]
      await miniProgram.reLaunch(url)
      await new Promise((r) => setTimeout(r, 1200))
      out.requested = url
      out.path = (await miniProgram.currentPage()).path
    } else if (cmd === 'eval') {
      const page = await miniProgram.currentPage()
      const script = process.argv[3]
      out.result = await miniProgram.evaluate(script)
    } else if (cmd === 'screenshot') {
      const name = process.argv[3] || 'shot'
      const target = `${RUN}/raw/screenshots/${name}.png`
      await miniProgram.screenshot({ path: target })
      out.screenshot = target
    } else if (cmd === 'callMethod') {
      // 在页面上下文里执行（用于触发页面方法 / 读取 store）
      const script = process.argv[3]
      out.result = await miniProgram.callWxMethod ? await miniProgram.evaluate(script) : null
    }
  } catch (e) {
    out.error = String(e && e.message ? e.message : e)
  }
  console.log(JSON.stringify(out, null, 2))
  await miniProgram.disconnect().catch(() => {})
  fs.writeFileSync(`${RUN}/raw/console/driver-${Date.now()}.json`, JSON.stringify(out, null, 2))
}

main()
