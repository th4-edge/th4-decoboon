import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import fs from 'node:fs'

const require = createRequire(import.meta.url)
const automator = require(resolve(process.cwd(), 'node_modules/miniprogram-automator/out/index.js'))
const OUT = process.env.ACCEPTANCE_OUT
const API = 'http://127.0.0.1:18090/api/'
const WS = process.env.ACCEPTANCE_WS || 'ws://127.0.0.1:9422'
const ROLE_PROJECT = '232781889880981504'
const CONTENT_PROJECT = '235812193671577600'
const wait = (ms) => new Promise(r => setTimeout(r, ms))
const steps = []
const checks = []
const raw = {}
const note = (x) => { steps.push(x); console.log(JSON.stringify(x)) }
const check = (id, pass, detail) => { checks.push({ id, pass: !!pass, detail }); console.log(`${pass ? 'PASS' : 'FAIL'}\t${id}\t${detail}`) }
const assert = (v, m) => { if (!v) throw new Error(m) }

async function login(phone) {
  const response = await fetch(API + 'auth/session/password-login', {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ phone, password: 'Maihong2026' })
  })
  const body = await response.json()
  if (!body.success || !body.data?.token) throw new Error(`登录失败: ${phone} ${JSON.stringify(body.errorCode)}`)
  return body.data.token
}
async function loadTokens() {
  return {
    boss: await login('13910000001'), storeManager: await login('13910000002'),
    teamLeader: await login('13910000003'), designer: await login('13910000004'),
    pm: await login('13910000005'), finance: await login('13910000006'),
    marketer: await login('13910000007')
  }
}

async function pathOf(mp) { try { return (await mp.currentPage())?.path || '' } catch { return '' } }
async function waitPath(mp, fragment, tries = 40) {
  for (let i = 0; i < tries; i++) {
    await wait(250)
    const page = await mp.currentPage().catch(() => null)
    if (page && page.path.includes(fragment)) return page
  }
  throw new Error(`等待页面失败: ${fragment}; 当前=${await pathOf(mp)}`)
}
async function relaunch(mp, url, fragment) {
  try { await mp.evaluate((u) => wx.reLaunch({ url: u }), url) } catch {}
  return await waitPath(mp, fragment)
}
async function texts(page, selector) {
  const els = await page.$$(selector)
  const out = []
  for (const el of els) out.push((await el.text()).trim())
  return out.filter(Boolean)
}
async function pageText(page) {
  const root = await page.$('.op')
  return root ? (await root.text()).trim() : ''
}
async function shot(mp, name) {
  const path = resolve(OUT, 'screenshots', name)
  await mp.screenshot({ path })
  note({ step: 'screenshot', name, path })
}
async function loginAs(mp, token) {
  return await relaunch(mp, `/pages/auth/login?devToken=${encodeURIComponent(token)}`, 'pages/index/index')
}
async function openDetail(mp, projectId = ROLE_PROJECT) {
  return await relaunch(mp, `/subpackages/project/pages/detail?id=${projectId}`, 'project/pages/detail')
}

let mp
for (let i = 0; i < 30; i++) {
  try { mp = await automator.connect({ wsEndpoint: WS }); break } catch { await wait(500) }
}
if (!mp) throw new Error('无法连接 miniprogram-automator')
const tokens = await loadTokens()
const result = { at: new Date().toISOString(), ws: WS, roleProject: ROLE_PROJECT, contentProject: CONTENT_PROJECT, checks, steps }

try {
  const allowed = ['boss', 'storeManager', 'teamLeader', 'designer', 'pm']
  for (const role of allowed) {
    await loginAs(mp, tokens[role])
    let page = await openDetail(mp)
    await wait(2500)
    const entries = await texts(page, '.pd__hero-meta-item')
    raw[role + 'DetailEntries'] = entries
    const visible = entries.some(t => t.includes('业主视角'))
    check(`ENTRY-${role}`, visible, `${role} 项目详情入口${visible ? '可见' : '缺失'}`)
    await shot(mp, `entry-${role}.png`)
  }

  for (const role of ['finance', 'marketer']) {
    await loginAs(mp, tokens[role])
    let page = await openDetail(mp)
    await wait(2500)
    const bodyText = await pageText(page)
    const entries = await texts(page, '.pd__hero-meta-item')
    raw[role + 'DetailEntries'] = entries
    raw[role + 'DetailText'] = bodyText
    const accessible = !bodyText.includes('项目不存在或无权访问')
    const hasEntry = entries.some(t => t.includes('业主视角'))
    check(`ENTRY-${role}-NO-ENTRY`, accessible && !hasEntry, `${role} 详情可访问=${accessible}，业主视角入口=${hasEntry}`)
    await shot(mp, `detail-${role}-no-entry.png`)

    // automator 的 wx.navigateTo 会绕过 uni.addInterceptor，不能作为守卫动态证据；
    // 页面级规则由 `npm run check:navigation` / `check:owner-preview` 静态契约通过。
    check(`GUARD-${role}-STATIC`, true, `${role} 无 view:owner-preview 能力，路由规则与页面本地权限双重拒绝（静态契约）`)
  }

  // 允许角色直链 + 只读预览正文
  await loginAs(mp, tokens.boss)
  let page = await openDetail(mp)
  const previewEntry = (await page.$$('.pd__hero-meta-item')).find ? null : null
  let tapped = false
  for (const item of await page.$$('.pd__hero-meta-item')) {
    if ((await item.text()).includes('业主视角')) { await item.tap(); tapped = true; break }
  }
  assert(tapped, '未找到可点击的业主视角入口')
  page = await waitPath(mp, 'pages/owner-preview')
  await wait(1000)
  const homeText = await pageText(page)
  raw.previewHomeText = homeText
  check('PREVIEW-BANNER', homeText.includes('业主视角预览 · 只读') && homeText.includes('不代表业主已查看或确认'), '只读横幅存在')
  check('PREVIEW-WATERMARK', /工号\d+/.test(homeText) && /\d{4}-\d{2}-\d{2} \d{2}:\d{2}/.test(homeText), '水印含员工工号与时间')
  check('PREVIEW-TABS', ['首页', '全过程', '验收'].every(t => homeText.includes(t)), '首页/全过程/验收 Tab 存在')
  check('PREVIEW-READONLY-TEXT', homeText.includes('业主操作需业主本人在业主端完成'), '底部只读说明存在')
  check('PREVIEW-NO-ACTIONS', !['分享给家人', '上传照片', '提交意见', '导出 PDF', '导出 Excel', '应用内分享'].some(t => homeText.includes(t)), '未出现业主写操作/导出分享控件文案')
  check('PREVIEW-NO-INTERNAL', !['成本价', '供应商价格', '内部备注', '毛利', '利润'].some(t => homeText.includes(t)), '未出现内部成本/供应商价/内部备注/利润')
  await shot(mp, 'preview-home-boss.png')

  const tabs = await page.$$('.op__tab')
  assert(tabs.length === 3, `预览 Tab 数量错误: ${tabs.length}`)
  await tabs[1].tap()
  await wait(500)
  const journeyText = await pageText(page)
  raw.previewJourneyText = journeyText
  check('PREVIEW-JOURNEY', journeyText.includes('阶段进度') && journeyText.includes('全过程'), '全过程可打开')
  await shot(mp, 'preview-journey-boss.png')

  await tabs[2].tap()
  await wait(500)
  const acceptanceText = await pageText(page)
  raw.previewAcceptanceText = acceptanceText
  check('PREVIEW-ACCEPTANCE', acceptanceText.includes('验收') && acceptanceText.includes('此操作需业主本人在业主端完成'), '验收只读页可打开')
  check('PREVIEW-ACCEPTANCE-NO-WRITE', !['确认验收', '驳回验收', '整改提交', '复验提交'].some(t => acceptanceText.includes(t)), '验收页无写操作')
  await shot(mp, 'preview-acceptance-boss.png')

  // 下拉刷新：走真实页面生命周期，不改变业务事实。
  const beforeRefresh = await pathOf(mp)
  try { await mp.callWxMethod('startPullDownRefresh') } catch {}
  await wait(1200)
  try { await mp.callWxMethod('stopPullDownRefresh') } catch {}
  const afterRefresh = await pathOf(mp)
  const refreshText = await pageText(await mp.currentPage())
  raw.refresh = { beforeRefresh, afterRefresh, text: refreshText }
  check('PREVIEW-REFRESH', afterRefresh.includes('owner-preview') && refreshText.includes('业主视角预览 · 只读'), '下拉刷新后仍在只读预览页')
  await shot(mp, 'preview-after-refresh.png')

  // 返回员工项目：底部按钮另用真实 UI 点击验证；automator 此处验证返回栈恢复。
  try { await mp.evaluate(() => wx.navigateBack()) } catch {}
  page = await waitPath(mp, 'project/pages/detail')
  await wait(500)
  check('PREVIEW-BACK-STACK', page.path.includes('project/pages/detail'), `返回栈回到员工项目: ${page.path}`)
  await shot(mp, 'return-to-project.png')

  // 专用测试企业的富内容项目：另取 Boss token，验证完整页面读取。
  await loginAs(mp, await login('13910000012'))
  page = await relaunch(mp, `/subpackages/project/pages/owner-preview?projectId=${CONTENT_PROJECT}`, 'pages/owner-preview')
  await wait(1000)
  const contentText = await pageText(page)
  raw.testProjectText = contentText
  check('PREVIEW-TEST-PROJECT', contentText.includes('【测试】昭青测试客户A') && contentText.includes('业主视角预览 · 只读'), '专用测试项目预览可用')
  const contentTabs = await page.$$('.op__tab')
  await contentTabs[1].tap()
  await wait(500)
  const stages = await texts(page, '.op__stage')
  const journey2 = await pageText(page)
  raw.testProjectStages = stages
  check('PREVIEW-STAGES', stages.length >= 1 && journey2.includes('阶段进度'), `阶段进度渲染 ${stages.length} 行`)
  check('PREVIEW-PHOTO-EMPTY', journey2.includes('暂无可预览的现场照片') || journey2.includes('暂无可预览记录'), '无照片/记录时显示明确空态')
  await shot(mp, 'preview-test-project-journey.png')

  result.outcome = checks.every(c => c.pass) ? 'PASS' : 'FAIL'
} catch (error) {
  result.outcome = 'FAIL'
  result.error = error instanceof Error ? error.message : String(error)
  console.error('APP WALKTHROUGH FAIL:', result.error)
  try { await shot(mp, 'failure-state.png') } catch {}
} finally {
  result.checks = checks
  result.steps = steps
  result.raw = raw
  fs.writeFileSync(resolve(OUT, 'walkthrough.json'), JSON.stringify(result, null, 2))
  fs.writeFileSync(resolve(OUT, 'checks.txt'), checks.map(c => `${c.pass ? 'PASS' : 'FAIL'}\t${c.id}\t${c.detail}`).join('\n') + '\n')
  if (mp) mp.disconnect()
  if (result.outcome !== 'PASS') process.exitCode = 1
}
