import fs from 'node:fs'
import path from 'node:path'

const app = path.resolve(process.cwd(), '../../前端代码/th4-decoboon-app')
const read = (relativePath) => fs.readFileSync(path.join(app, relativePath), 'utf8')
const has = (text, pattern) => pattern.test(text)

const workbench = read('src/subpackages/customer/pages/measure-workbench.vue')
const assessment = read('src/subpackages/statistic/pages/assessment.vue')
const permission = read('src/roles/permission.ts')
const catalog = read('src/roles/catalog.ts')

const results = [
  {
    id: 'only-marketer-grouping',
    status: has(workbench, /marketerGroups/) && !/sourceGroups|designerGroups/.test(workbench) ? 'PASS' : 'FAIL',
    detail: '预约量房页只读取 marketerGroups，不读取来源/设计师分组'
  },
  {
    id: 'buttons-and-routes',
    status: ['约定量房', '立即派单', '改约', '查看预约', '查看量房记录', '联系客户', 'designer=change', 'customerMeasureRecord'].every((s) => workbench.includes(s)) ? 'PASS' : 'FAIL',
    detail: '主按钮、联系客户、立即派单和量房记录路径存在'
  },
  {
    id: 'overdue-threshold',
    status: has(workbench, /84\s*\*\s*3600\s*\*\s*1000/) ? 'FAIL' : 'PASS',
    detail: 'App 逾期筛选必须使用 4 小时阈值，不得保留 84 小时'
  },
  {
    id: 'performance-filter-propagation',
    status: has(assessment, /workbenchApi\.bossMeasure\(\)/) ? 'FAIL' : 'PASS',
    detail: '人员绩效页月份/门店筛选必须传入量房业绩数据源'
  },
  {
    id: 'marketer-workbench-visible',
    status: /marketer:\s*\[[\s\S]*?view:measure-workbench[\s\S]*?\]/.test(permission) ? 'PASS' : 'FAIL',
    detail: '业务员角色应能看到我的量房入口'
  },
  {
    id: 'forbidden-facts',
    status: /已接单|上门中|准时率|接单率|派单时长|改期原因/.test(workbench) ? 'FAIL' : 'PASS',
    detail: 'P0 页面不得出现无事实支撑的状态或指标'
  },
  {
    id: 'return-overview-focus-guard',
    status: workbench.includes('function exitWorkspaceFocus') && workbench.includes('suppressAutoFocus') && /workspaceFocused\.value\s*=\s*false/.test(workbench) && workbench.includes('setTimeout(() => { suppressAutoFocus = false }, 120)') ? 'PASS' : 'FAIL',
    detail: '返回经营概览应恢复顶部统计并抑制内部滚动自动重新聚焦'
  },
  {
    id: 'periods-two-by-two-grid',
    status: /<view class="mw__period-row">/.test(workbench) && /\.mw__period-row\s*\{[^}]*grid-template-columns:\s*repeat\(2/.test(workbench) ? 'PASS' : 'FAIL',
    detail: '日期维度使用 2×2 自适应宫格'
  },
  {
    id: 'exceptions-two-by-two-grid',
    status: /<view class="mw__exception-row">/.test(workbench) && /\.mw__exception-row\s*\{[^}]*grid-template-columns:\s*repeat\(2/.test(workbench) ? 'PASS' : 'FAIL',
    detail: '异常待办使用 2×2 自适应宫格'
  },
  {
    id: 'dashboard-metrics',
    status: ['今日预约量房', '待派单', '近7日预约', '已完成', '完成率', '交设计率'].every((s) => workbench.includes(s)) ? 'PASS' : 'FAIL',
    detail: '经营看板保留指定六项主指标'
  },
  {
    id: 'card-visual-hierarchy',
    status: [
      '<JzAvatar :name="item.customerName"',
      '<JzTag :text="item.derivedStateName"',
      'item.telNo',
      'item.sourceName',
      'item.marketerName',
      'mw__property-card',
      'mw__property-address',
      'mw__property-meta',
      'mw__schedule-chip',
      'mw__photo-count',
      '`设计 ${item.designerName',
      '`执行 ${item.executorName}',
      'mw__next-label',
      'mw__action mw__action--primary',
      'mw__action--ghost',
      'border-top: 1rpx solid $jzb-border-light',
      'linear-gradient(180deg',
      'mw__item--danger::before',
      'mw__item--current',
      'mw__item--danger'
    ].every((s) => workbench.includes(s)) ? 'PASS' : 'FAIL',
    detail: '客户卡片头像/身份、房屋卡、时间 chip、标签、下一步、操作层级、当前态和风险态均已接入'
  },
  {
    id: 'boss-entry',
    status: has(catalog, /label: '预约量房'[\s\S]*?icon: 'ruler'[\s\S]*?customerMeasureWorkbench/) ? 'PASS' : 'FAIL',
    detail: 'BOSS 首页入口为预约量房并使用 ruler 图标'
  }
]

console.log(JSON.stringify({ request: 'REQ-20260922-BOSS-MEASURE-WORKSPACE-2', results }, null, 2))
if (results.some((item) => item.status === 'FAIL')) process.exitCode = 1
