# 材料履约与材料业务助手 · UI 与交互审查

## 审查对象

- Batch：B2
- 路由：
  - `/business-blueprints`
  - `/finance/projects/:projectId`
- 角色：企业负责人（Boss，Company 数据范围）
- 环境：本地 dev，后端 8090，Web 8080
- 设备/分辨率：1440×900、1024×768
- 业务蓝图节点：`material.end-to-end`、`material.health`

## 证据

| 场景 | 截图/路径 | 结果 | 问题 |
| --- | --- | --- | --- |
| 蓝图正常态 | `evidence/B2/b2-blueprint-1440x900.png` | PASS：版本/来源/阶段/对象/字段/规则/例外/未决项可见 | 无阻断 |
| 蓝图交互态 | `evidence/B2/b2-blueprint-1024x768.png` | PASS：阶段可切换，字段搜索生效，1024 下无横向溢出 | 无阻断 |
| 项目材料体检正常态 | `evidence/B2/b2-material-health-1440x900.png` | PASS：真实项目“到场确认”、未定价与成本不完整同屏；利润显示“成本未完整” | 无阻断 |
| 项目材料体检 1024 | `evidence/B2/b2-material-health-1024x768.png` | PASS：指标 3 列、主内容单列、无横向溢出 | 无阻断 |
| 空态 | `evidence/B2/b2-material-health-empty-1440x900.png` | PASS：无材料计划时明确说明“无法确认材料履约是否已开始” | 不伪装完成 |
| 失败态 | `evidence/B2/b2-material-health-error-1440x900.png` | PASS：显示“材料业务体检暂不可确认”并提供重试 | 解除网络拦截后重试恢复 |

## 交互检查

- 阶段按钮切换后标题、触发、系统下一步、账务口径、例外和来源同步更新；
- “字段字典”搜索“金额”后表格收缩为 4 行，未出现空表伪装；
- “去项目账本做材料体检”能进入 `/finance/projects`；
- “刷新体检”与失败态“重试”都能重新请求服务端；
- 1024×768 下侧栏收起，蓝图阶段网格为 3 列，体检指标为 3 列、主区单列；
- 正常页面 Console Errors = 0。

## 缺陷

| ID | 页面 | 现象 | 期望 | 级别 | 状态 |
| --- | --- | --- | --- | --- | --- |
| — | — | 本批未发现阻断级页面缺陷 | — | — | — |

## 跨端一致性

- App 与 Web 字段/状态/下一步：App 侧 B3 尚未做 UI 审查；
- 当前只对 Web 与后端 Provider 同源口径做了对账；
- B5 仍需独立验证 App/Web 跨端一致性。

---

# B3 App 业务体检与页面上下文审查

## 审查对象

- Batch：B3
- App 路由：
  - `/subpackages/project/pages/detail?id=...`
  - `/subpackages/project/pages/material-sheet-detail?sheetId=...`
  - `/subpackages/project/pages/material-sheet-create?projectId=...`
  - `/subpackages/account/pages/help?topic=material-sheet-time`
- 角色：企业负责人（Boss，Company 数据范围）；作业单页面同时覆盖 Designer/Pm 可见的测量上下文
- 环境：微信开发者工具 375×599；H5 390×844；本地 dev 后端真实 Provider
- 业务节点：`material.health`、`material.measure`、`material.order-sheet`

## B3 证据

| 场景 | 截图/路径 | 结果 | 问题 |
| --- | --- | --- | --- |
| 项目材料体检 | `evidence/B3/b3-app-project-material-health.png` | PASS：真实项目显示选材、待下单、待到货、成本不完整、校核项和聚焦行 | 无阻断 |
| 项目材料体检空态 | `evidence/B3/b3-app-project-material-health-empty.png` | PASS：无材料计划时明确“无法确认材料履约是否已开始” | 不伪装完成 |
| 项目材料体检失败态 | `evidence/B3/b3-app-project-material-health-error-h5.png` | PASS：500 时显示“材料业务暂不可确认”与重试 | 无阻断 |
| 作业单时间/取消上下文 | `evidence/B3/b3-app-material-sheet-context.png` | PASS：当前状态、下一步、时间口径、模板必填、未定价与取消边界可见 | 无阻断 |
| 建单页来源说明 | `evidence/B3/b3-app-material-sheet-create-guide.png` | PASS：材料行只来自计划、合同外走增减项、作业单不承载价格 | 无阻断 |
| 帮助主题 | `evidence/B3/b3-app-help-time-semantics.png` | PASS：测量、配送/安装、要求到场/实际到场的区别可搜索 | 无阻断 |

OCR 文本：`evidence/B3/ocr-text.txt`。

## B3 交互与布局

- 微信开发者工具 automator 成功连接并加载 4 个真实页面；
- 项目材料体检“材料计划”动作实际导航到 `/subpackages/project/pages/material-plan?projectId=...`；
- 作业单详情识别到真实 Tile 模板 `measureAt.required=true`，显示“提交请购前必须先填写”；
- 未定价草稿显示“当前采购侧存在未定价或部分结算，成本不完整”，没有展示 0 成本利润；
- H5 390×844 三个业务页面 `scrollWidth = innerWidth = 390`，无横向溢出；
- 正常页面 Console Errors = 0；
- 失败态来自本地网络拦截，不是业务页面自身异常。

## B3 缺陷

| ID | 页面 | 现象 | 期望 | 级别 | 状态 |
| --- | --- | --- | --- | --- | --- |
| — | — | 未发现阻断级页面缺陷 | — | — | — |

## B3 跨端一致性

- Web 与 App 的 `blueprint/check/report` 使用同一 Provider、同一阶段/计数/成本口径；
- App 作业单时间说明引用 `OBJ-008 / DEC-MP-07 / DEC-072 / DEC-073`，没有在前端新增状态机；
- B5 仍需独立验证 App/Web 真机、越权、空态和边界一致性。

---

# B4 只读材料业务助手审查说明

## 审查范围

- Batch：B4
- 交付形态：Provider/运行时，不新增独立 Web/App 页面；
- 页面影响：B2 蓝图中心、B3 App 业务体检与上下文页面未改动；
- 审查结论：本批不做 UI 通过声明，UI 入口和真实页面验收留 B6 独立验收。

## 运行时证据

- `evidence/B4/api-samples.json`：真实 HTTP 响应、证据引用、工具列表与审计状态；
- `evidence/B4/golden-question-results.json`：50 个黄金问题逐题结果；
- `evidence/B4/audit-summary.json`：审计元数据，不含问题/回答正文；
- 设计—蓝图—实现—验收的一致性由 B6 独立黑盒验证。

---

# B5 App 单端材料下单与结算审查

## 审查状态

- Batch：B5
- 状态：**PASS（真实渲染与交互走查完成）**
- 证据目录：`evidence/B5/`
- 不宣称：独立验收 `ACCEPTED`、正式小程序发布。

## 环境

- 微信开发者工具 2.02.2609231，项目根 `th4-decoboon-app/dist/dev/mp-weixin`；
- DevTools 系统窗口 375×667，页面 `page.size()` 375×599；
- H5 390×844；
- 本地 dev 后端 8090，造数企业 M 真实 Provider 数据；
- 临时作业单 `TILE-20261017-001`，走查后取消并删除。

## 真实页面证据

| 页面 | DevTools 截图 | H5 截图 | 结果 |
| --- | --- | --- | --- |
| 主材下单工作台 | `evidence/B5/screenshots/b5-workbench-submitted.png` | `evidence/B5/screenshots/b5-h5-workbench-submitted.png` | PASS：待财务下单、去下单、结算入口 |
| 作业单下单 | `evidence/B5/screenshots/b5-material-sheet-order.png` | `evidence/B5/screenshots/b5-h5-material-sheet-order.png` | PASS：供应商/数量/可空单价/未定价 |
| 作业单详情 | `evidence/B5/screenshots/b5-material-sheet-detail-submitted.png` | `evidence/B5/screenshots/b5-h5-material-sheet-detail.png` | PASS：待下单、去下单、时间与模板说明 |
| 主材结算 | `evidence/B5/screenshots/b5-material-settlement.png` | `evidence/B5/screenshots/b5-h5-material-settlement.png` | PASS：逐单/月结、结算价、待结算明细 |
| 结算单空态 | `evidence/B5/screenshots/b5-material-settlement-settlements-tab.png` | — | PASS：明确“还没有结算单” |
| 材料计划工作台 | `evidence/B5/screenshots/b5-material-plan-workbench.png` | `evidence/B5/screenshots/b5-h5-material-plan-workbench.png` | PASS：旧直采入口移除 |
| 供应商协作 | `evidence/B5/screenshots/b5-supply-index.png` | `evidence/B5/screenshots/b5-h5-supply-index.png` | PASS：旧直采入口移除，保留历史/到场 |

## 交互

- 主材工作台“去下单”实际 tap 后进入 `subpackages/project/pages/material-sheet-order`；
- 主材结算“结算单”tab 实际 tap 后进入空态；
- 作业单下单页显示“未定价”“采购单价 可留空=未定价”“1 行未定价 · 成本待结算”；
- 材料计划工作台显示“下单统一从主材作业单发起”；
- 供应商协作页显示“主材下单工作台 / 材料计划工作台 / 主材结算”，无旧材料计划直采入口。

## 布局与 Console

- DevTools 各页 `page.size()` 宽度均为 375，无横向溢出；材料计划工作台高度 667，其余 599；
- H5 六页 `scrollWidth = innerWidth = 390`；
- H5 业务页面 Console Errors = 0；仅 `favicon.ico` 404；
- DevTools 编译成功、automator 可连接。

## 缺陷与修复

| ID | 页面 | 现象 | 修复 | 状态 |
| --- | --- | --- | --- | --- |
| B5-UI-001 | 材料计划工作台 | 模板存在孤立 `"`，DevTools 编译报 `unexpected character`，automator 反复 timeout | 删除 `mpw__item` 节点多余引号，重新构建后编译通过 | FIXED |

## 结论

- UI：`UI_REVIEWED`；
- 仍需 B6 独立验收，不能由本实现自批 `ACCEPTED`；
- 未执行正式小程序提审或发布。


---

# B6 独立验收 UI 记录

- 独立验收任务使用 App 干净提交 `225b963` 的 H5 390×844 真实浏览器；
- 采集 11 张截图：主材工作台、下单、结算、空态、到场后、项目体检、材料计划、供应商、旧入口不可达；
- 11 项 UI 检查全部 PASS；`scrollWidth=innerWidth=390`，业务 Console Errors=0；
- 证据：`evidence/B6/README.md`、`evidence/B6/screenshots/`、`evidence/B6/browser-metrics.json`；
- UI 通过不代表业务验收通过；B6 因 6 个缺陷仍判定 FAIL。


---

# B6.1 暂估应付 UI 验证

- Web `/finance/payables` 1440×900：暂估应付显示“待结算”，安排付款按钮禁用；Console Errors 0；无横向溢出；
- App H5 `/subpackages/finance/pages/payables` 390×844：显示“待定价 / 暂估/待定价 / 结算后安排付款”；Console Errors 0，`scrollWidth=innerWidth=390`；
- 截图：`evidence/B6.1/screenshots/b61-web-payables-estimated.png`、`b61-app-payables-estimated.png`；
- 本记录是实现者 UI 自测，B6.2 仍需独立验收。


---

# B6.2 独立复验 UI 记录

- Web `/finance/payables` 1440×900：暂估应付“待结算”，安排付款禁用；`scrollWidth=innerWidth=1440`，Console Errors 0；
- App H5 390×844：应付页“待定价/暂估/待结算”、空态、Designer 权限态、主材下单工作台、旧路由；
- App `scrollWidth=innerWidth=390`，Console Errors 0；
- 截图：`evidence/B6.2/screenshots/`；
- UI 通过，但 B62-DEF-001 仍阻断 B6.2 整体通过。


---

# B6.3 多供应商价格状态 UI 验证

- App H5 390×844 打开真实作业单详情：显示“部分已结算 / 部分到场”，与后端 Mixed 投影一致；
- `scrollWidth=innerWidth=390`，Console Errors 0；
- 截图：`evidence/B6.3/screenshots/b63-app-sheet-mixed-390x844.png`；
- 无前端源码变更；B6.4 仍需独立复验。


---

# B6.4 独立复验 UI 记录

- Web `/finance/payables` 1440×900 PASS：暂估“待结算”且付款禁用；无横向溢出、Console Errors 0；
- App H5 作业单 390×844 PASS：Mixed 显示“部分已结算”；
- Web `/finance/material-ordering` FAIL：目标 Mixed 作业单仅显示“已到场”，未显示价格状态/成本待结算；
- 截图：`evidence/B6.4/screenshots/b64-web-material-ordering-mixed-1440x900.png`、`b64-app-sheet-mixed-390x844.png`；
- 开放缺陷：`B64-UI-WEB-SHEET-01`。


---

# B6.5 Web Mixed 状态展示审查

- `/finance/material-ordering` 1440×900：Mixed 行显示“部分已结算 · 成本待结算”；
- 其他状态文案：Priced/Unpriced/Settled 均有对应 pill；
- `innerWidth=scrollWidth=bodyScrollWidth=1440`，Console Errors 0；
- 截图：`evidence/B6.5/screenshots/b64-web-material-ordering-mixed-1440x900.png`；
- UI 自测 PASS；B6.6 独立复验 157/157 PASS，混合状态展示与回归均通过。

---

# B6.6 独立 UI 复验

- Web `/finance/material-ordering` 1440×900：Priced→已定价、Unpriced→未定价 · 成本待结算、Mixed→部分已结算 · 成本待结算、Settled→已结算；
- Mixed 行同时保留履约状态（部分到场/已到场），不是只显示“已到场”；文案、pill class、computed color/background、API 与页面 XHR `priceState` 一致；
- 前端审计确认 `priceState` 仅消费服务端字段，不由 `purchasePrice`/`settlementPrice` 推导；
- App H5 390×844 同源状态显示 PASS；Web 1440×900 与 App H5 均无横向溢出，Console Errors 0；
- 截图：`evidence/B6.6/screenshots/`（9 张）；
- 结论：独立 UI PASS，`B64-UI-WEB-SHEET-01` CLOSED。


---

# B7 问材料上下文入口 UI 审查

- Web 项目核算 1440×900：浮动「问材料」入口、右侧 520px 抽屉、项目上下文/已绑定/只读、6 条服务端建议问题；回答展示实时数据、证据、只读工具、下一步；
- Web 主材下单/结算页：采购/结算上下文标签、未绑定业务单据提示、对应建议问题；清除/恢复上下文可用；
- Web 拒答/无证据：`不在助手范围`、`暂无证据` 原样展示；抽屉内无审批/付款/删除/改权限按钮；
- Web 错误重试：故意中断一次请求后显示“材料助手暂不可确认/重试”，重试成功；
- Web 1024×768/1440×900 与 App H5 390×844/375×667 均无横向溢出，Console Errors 0；
- App 项目/作业单/结算三页真实渲染：项目上下文、真实 `TILE-` 作业单上下文、结算建议均正确；底部安全区与长内容滚动正常；
- App Designer 项目回答使用“当前角色可查看履约状态”，不返回利润 null 或成本工具；
- 截图：`evidence/B7/screenshots/`（14 张）；样式指标：`ui-style-metrics-web.json`、`ui-style-metrics-app.json`；
- 限制：微信开发者工具 automator 本轮未执行；MP 构建通过，DevTools 逐页走查留 B8 独立验收。
