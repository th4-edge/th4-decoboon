import fs from 'node:fs'
import { createServer } from 'vite'
import { createSSRApp, h } from 'vue'
import { renderToString } from '@vue/server-renderer'

const root = process.cwd()
const read = (path) => fs.readFileSync(path, 'utf8')
const model = read('src/domains/project/model.ts')
const api = read('src/domains/project/api.ts')
const panel = read('src/components/project/ProjectFactPhasePanel.vue')
const view = read('src/views/workbench/ProjectExecutionView.vue')
const routes = read('src/roles/routes.ts')
const checks = []
const failures = []
const check = (name, ok, evidence = '') => {
  checks.push({ name, ok: Boolean(ok), evidence })
  if (!ok) failures.push(name)
}
const has = (source, text) => source.includes(text)
const count = (source, text) => source.split(text).length - 1
const before = (source, left, right) => source.indexOf(left) >= 0 && source.indexOf(right) >= 0 && source.indexOf(left) < source.indexOf(right)

check('factTrace 契约映射', has(model, 'factTrace?: ProjectFactTrace'))
check('事实归一化严格存在', has(api, 'normalizeProjectFactTrace') && has(api, "FACT_STAGE[enumValue(raw.stage)]") && has(api, "typeof raw.current !== 'boolean'") && has(api, 'Number.isInteger(version)'))
check('事实项目绑定校验', has(api, 'nodes.some((node) => node.projectId !== projectId)'))
check('禁止前端 stage/current/version 兜底', !has(api, "FACT_STAGE[enumValue(raw.stage)] || 'source'") && !has(api, 'raw.current !== false') && !has(api, 'raw.version ?? 1'))
check('服务端节点顺序直连', has(panel, 'const serverNodes = computed(() => props.trace?.nodes ?? [])') && !has(panel, '[...(props.trace?.nodes ?? [])].sort'))
check('按 nodeKey 分组且当前为主卡', has(panel, 'grouped.find((node) => node.current) ?? grouped[0]'))
check('历史节点 DOM 锚点', has(panel, ':id="anchorId(history.nodeId)"'))
check('历史节点滚动边距', has(panel, '.pfact-history__item {') && has(panel, 'scroll-margin-top: 96px'))
check('返回历史时自动展开对应历史分组', has(panel, 'if (!focused.current)') && has(panel, 'expandedHistories.value = Array.from(new Set([...expandedHistories.value, focused.nodeKey]))'))
check('返回锚点滚动恢复', has(view, 'document.getElementById(anchor)') && has(view, 'scrollIntoView({ behavior: \'smooth\', block: \'center\' })'))
check('返回路径携带 facts 与原锚点', has(view, 'const returnQuery = { projectId: selectedProjectId.value, view: \'facts\', anchor }') && has(view, 'returnPath') && has(view, 'returnAnchor'))
check('采购来源接入主材下单', has(view, "case 'Purchase':") && has(view, "routeIfAvailable('finance-material-ordering', undefined, { projectId })"))
check('材料计划与到货不伪造精确路由', !/case\s+'MaterialPlan'/.test(view) && !/case\s+'GoodsReceipt'/.test(view))
check('材料三类摘要-only 口径', has(panel, "const SUMMARY_ONLY_FACT_TYPES = new Set(['MaterialPlan', 'Purchase', 'GoodsReceipt'])") && has(panel, '暂无 Web 来源入口，请按摘要核对'))
check('来源路由按当前角色路由集合判断', has(view, 'function routeIfAvailable') && has(view, 'router.hasRoute(name)'))
check('施工执行原能力保留', ['openSchedule', 'openTask', 'openReport', 'task-table', 'report-block'].every((text) => has(view, text)))
check('U5-D 角色路由与菜单接线', count(routes, 'L.wbProjectExecution') === 4 && count(read('src/roles/workbenchConfig.ts'), "item('pm.execution', '施工排期与现场留证', '/workbench/project-execution')") === 4)

const stageLabels = ['来源', '售前', '签约', '履约', '变更', '验收', '财务', '售后']
const stageBlock = panel.slice(panel.indexOf('const STAGES'), panel.indexOf('const FACT_LABELS'))
let cursor = -1
let stageOrder = true
for (const stage of ['source', 'preSale', 'signing', 'fulfillment', 'change', 'acceptance', 'finance', 'afterSales']) {
  const position = stageBlock.indexOf(`key: '${stage}'`)
  if (position <= cursor) stageOrder = false
  cursor = position
}
check('固定 8 阶段顺序', stageOrder && stageLabels.every((label) => has(panel, `label: '${label}'`)))

const node = (overrides) => ({
  nodeId: 'node', nodeKey: 'node', stage: 'fulfillment', stageName: '履约',
  factType: 'Purchase', factTypeName: '采购', objectType: 'PurchaseOrder', objectId: 'po1',
  projectId: 'project-1', version: 1, current: true, status: 'Active', statusName: '进行中',
  businessTime: '2026-10-23T10:00:00+08:00', recordedAt: '2026-10-23T10:01:00+08:00',
  actorName: '项目经理', title: '采购事实', summary: '摘要事实', sourceLabel: '采购',
  amount: null, quantity: null, progress: null, costComplete: null, profitAmount: null, grossMargin: null,
  uncertainty: null, relations: [], visibility: 'internal', ...overrides
})
const trace = {
  projectId: 'project-1', projectNo: 'P-001', projectName: '验收项目', defaultViewMode: 'PHASE', asOf: '2026-10-23T10:05:00+08:00',
  nodes: [
    node({ nodeId: 'current-design', nodeKey: 'design-v1', stage: 'signing', stageName: '签约', factType: 'Design', factTypeName: '设计', objectType: 'Design', objectId: 'design1', title: '当前设计', summary: '当前设计摘要', uncertainty: null }),
    node({ nodeId: 'history-design', nodeKey: 'design-v1', stage: 'signing', stageName: '签约', factType: 'Design', factTypeName: '设计', objectType: 'Design', objectId: 'design0', version: 1, current: false, title: '历史设计', summary: '历史设计摘要', uncertainty: '已被替代' }),
    node({ nodeId: 'current-quote', nodeKey: 'quote-v1', stage: 'signing', stageName: '签约', factType: 'Quote', factTypeName: '报价', objectType: 'Quote', objectId: 'quote1', title: '当前报价' }),
    node({ nodeId: 'material-plan', nodeKey: 'material-plan', stage: 'fulfillment', factType: 'MaterialPlan', factTypeName: '材料计划', objectType: 'MaterialPlan', objectId: 'mp1', title: '材料计划' }),
    node({ nodeId: 'purchase', nodeKey: 'purchase', stage: 'fulfillment', factType: 'Purchase', factTypeName: '采购', objectType: 'PurchaseOrder', objectId: 'po1', title: '采购单' }),
    node({ nodeId: 'goods-receipt', nodeKey: 'goods-receipt', stage: 'fulfillment', factType: 'GoodsReceipt', factTypeName: '到货', objectType: 'GoodsReceipt', objectId: 'gr1', title: '到货单' })
  ],
  sourceGaps: [{ nodeKey: 'receipt-gap', stage: 'fulfillment', stageName: '履约', factType: 'GoodsReceipt', factTypeName: '到货', label: '到货记录', reason: '供应商尚未回传', required: true }]
}

const server = await createServer({ root, logLevel: 'error', server: { middlewareMode: true }, appType: 'custom' })
try {
  const Panel = (await server.ssrLoadModule('/src/components/project/ProjectFactPhasePanel.vue')).default
  const renderPanel = async (props) => renderToString(createSSRApp({ render: () => h(Panel, props) }))
  const historyHtml = await renderPanel({ trace, focusAnchor: 'fact-anchor-history-design', canOpen: () => true })
  check('当前主卡 DOM 锚点实际渲染', has(historyHtml, 'id="fact-anchor-current-design"'))
  check('历史节点 DOM 锚点实际渲染', has(historyHtml, 'id="fact-anchor-history-design"'))
  check('返回历史锚点时历史列表实际展开', has(historyHtml, '查看历史来源') && has(historyHtml, '历史设计摘要'))
  check('同阶段服务端顺序未被重排', before(historyHtml, 'id="fact-anchor-current-design"', 'id="fact-anchor-current-quote"'))
  check('8 阶段实际按固定顺序渲染', stageLabels.every((label) => has(historyHtml, `>${label}</strong>`)) && before(historyHtml, '>来源</strong>', '>售前</strong>') && before(historyHtml, '>财务</strong>', '>售后</strong>'))
  check('必须来源缺口实际渲染', has(historyHtml, '供应商尚未回传') && has(historyHtml, '必须补齐'))

  const noRouteHtml = await renderPanel({ trace, canOpen: () => false })
  check('无精确路由时三类材料事实均显示摘要-only', count(noRouteHtml, '暂无 Web 来源入口，请按摘要核对') === 3)
  const purchaseRouteHtml = await renderPanel({ trace, canOpen: (item) => item.factType === 'Purchase' })
  check('采购有合法路由时显示来源按钮', has(purchaseRouteHtml, '查看来源') && count(purchaseRouteHtml, '暂无 Web 来源入口，请按摘要核对') === 2)

  const { routesByRole } = await server.ssrLoadModule('/src/roles/routes.ts')
  const flatten = (records, names = new Set()) => {
    for (const record of records ?? []) {
      if (typeof record.name === 'string') names.add(record.name)
      flatten(record.children, names)
    }
    return names
  }
  const namesByRole = Object.fromEntries(Object.entries(routesByRole).map(([role, records]) => [role, flatten(records)]))
  for (const role of ['Boss', 'StoreManager', 'Designer', 'Pm']) check(`${role} 可达项目执行页`, namesByRole[role].has('workbench-project-execution'))
  for (const role of ['Boss', 'StoreManager']) check(`${role} 采购来源路由合法`, namesByRole[role].has('finance-material-ordering'))
  for (const role of ['Designer', 'Pm']) check(`${role} 无采购页时保持摘要-only`, !namesByRole[role].has('finance-material-ordering'))
  check('Finance 无项目执行入口时不生成采购伪路由', !namesByRole.Finance.has('workbench-project-execution'))
} finally {
  await server.close()
}

for (const item of checks) console.log(`${item.ok ? 'PASS' : 'FAIL'} ${item.name}${item.evidence ? `: ${item.evidence}` : ''}`)
console.log(`INDEPENDENT_U5D=${checks.length - failures.length}/${checks.length}`)
if (failures.length) {
  console.error(`FAILURES=${failures.join(' | ')}`)
  process.exit(1)
}
