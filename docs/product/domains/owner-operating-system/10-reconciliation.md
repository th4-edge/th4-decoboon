# 装企负责人经营管理体系 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-BOS-001 | A/C | BR-030、SCN-016 | business-progress | 待办/风险/主行动 | 负责人总览 | 原型交互 PASS | 未独立验收 | PENDING |
| REQ-BOS-002 | A | CAP-006、CAP-004 | businessFlow | 阶段/对象/观测点 | 业务推进图 | 原型交互 PASS | 未独立验收 | PENDING |
| REQ-BOS-003 | A/C | SCN-016、CAP-004 | business-progress/risk-queue | 状态动作投影 | 对象处置面板 | 原型交互 PASS | 未独立验收 | PENDING |
| REQ-BOS-004 | A | MET-002/004/005、GAP-070 | enterprise-operation | 金额/成本完整性 | 企业经营页 | 原型状态样例 PASS | 未独立验收 | PENDING |
| REQ-BOS-005 | A | BR-049 | enterprise-operation | 公司/门店目标 | 目标带 | 原型状态样例 PASS | 未独立验收 | PENDING |
| REQ-BOS-006 | B | BR-037、DEC-044/079 | operating-tools | 模板/主数据/权限 | 运营工具页 | 原型矩阵 PASS | 未独立验收 | PENDING |
| REQ-BOS-007 | A/B | BR-028、DEC-075/085 | organization-management | 企业/门店/员工/邀请/审计 | 组织管理页 | 原型矩阵 PASS | 未独立验收 | PENDING |
| REQ-BOS-008 | A/C | 一后端多端同源、BR-030 | surfaces | 同一对象/状态 | 三端切换 | 原型一致性 PASS | 未独立验收 | GAP-GAP-BOS-001 |
| REQ-BOS-009 | B | DEC-032、BR-028 | business-progress | 岗位视角/逐步留痕 | 一人装企场景 | 原型交互 PASS | 未独立验收 | PENDING |
| REQ-BOS-010 | 反向 | AGENTS、MET-002、GAP-070 | rules/exceptions | null/待确认 | 异常态组件 | 原型状态样例 PASS | 未独立验收 | PENDING |

## 未映射

- 真实 Provider 与字段：本批仅定义信息架构，尚未生成 Provider 对账矩阵。
- 微信小程序独立产品边界：`GAP-BOS-001`。
- 负责人风险观测阈值：`GAP-BOS-002`。

## 已过期

- 无。

## 未决业务问题

- `GAP-070`：经营利润、费用分摊、期间和未归属门店费用口径。
- `GAP-BOS-002`：观测阈值是否企业可配置。
- `GAP-BOS-003`：`运营工具`是否作为正式一级产品名称。

## 本批结论

- 已对齐：负责人内部能力、工作台对象锚点、业务推进主链、App 经营工具权限、公司/门店目标分层、项目毛利缺失态。
- 待对齐：三端壳层边界、风险阈值、经营利润口径、真实 Provider 字段。
- 下一批：完成概念原型真实渲染与评审，再选择负责人风险队列作为首个真实数据切片。


## 当前生产能力盘点（BOS-C1）

- 生产基线：后端 `b9b75f5`、Web `d38e553`、移动端 `efe2bae / 1.0.67`。
- 能力组：62 个，按业务推进 23、企业经营 18、运营工具 9、组织管理 9、横向支撑 3。
- 生产可见主能力 41 个，部分覆盖 18 个，后端能力未露出 1 个，未上线 1 个，口径阻塞 1 个。
- 详见 `11-current-capability-inventory.md`、`current-capabilities.json`、`current-capabilities.csv`。
- 本盘点不替代真实账号、真实数据、跨租户和独立验收。


## 负责人经营管理整合版 V2（BOS-P6）

- 原型：`docs/product/prototypes/owner-operating-system/index-v2.html`
- 设计：`13-integrated-v2-review.md`
- 数据：`current-capabilities.json` 62 项能力组。
- 交互模型：经营总览 + 业务对象 + 亲自处理；岗位执行不再作为一级导航。
- 五页 Web、三页移动端、能力详情抽屉、亲自处理抽屉均通过 Chromium 交互检查。
- 1440/1024/390 无页面级横向溢出；控制台 0 error / 0 warning。
- 状态：概念原型可评审；生产实现与独立验收未开始。


## UI Baseline V1（BOS-P7）

- 原型：`docs/product/prototypes/owner-operating-system/index-v3.html`
- 设计基线：`14-ui-baseline-v1.md`
- 样板页：经营总览、业务推进、项目对象。
- 视觉方向：工程蓝图 + 经营账本。
- 交互模型：经营管理一级导航 + 对象内亲自处理。
- 1440/1024/390 无页面级横向溢出；控制台 0 error / 0 warning；可见文字最小 12px。
- 状态：UI baseline 可评审；其余页面迁移和生产实现未开始。


## Dig UI 审查与重构（BOS-P8）

- 原型：`docs/product/prototypes/owner-operating-system/dig-v1.html`
- 审查：`15-dig-ui-review-and-redesign.md`
- catalog：`dig`
- layout：`dashboard-overview` + `data-table-workspace` + 对象工作区
- 覆盖：Web 三页、App 三页、dark/light、关键控件中/EN、能力详情、亲自处理和 UI 基线。
- 验证：1440/1024/390 无页面级横向溢出，可见文字最小 12px，console 0 error / 0 warning。
- 状态：UI 概念可评审，生产组件和独立验收未开始。

## BOS-R0 负责人角色与 App/Web 现状对账（2026-10-29）

| 项 | 场景 | 业务规则 | 当前事实 | 页面/Provider 证据 | 测试/渲染 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| BOS-R0-01 | BOSS 角色边界 | `BR-028`、`BR-030` | BOSS 为企业内部岗位能力超集，须逐步留痕，不得冒充业主确认 | 角色矩阵、SCN-016 | 文档核对 | MAPPED |
| BOS-R0-02 | BOSS 今日判断 | `BR-030`、`SCN-016` | App 已有负责人聚合首屏；Web 仍是通用卡片首页，经营总览无菜单入口 | App `pages/index/index.vue`；Web `workbenchHome.ts`、`workbenchConfig.ts`、`routes.ts` | 静态代码核对 | UNMAPPED |
| BOS-R0-03 | BOSS 角色认知 | `BR-030` | Web BOSS 首页含“本店经营结果 / 店长查看本店”卡片 | Web `roles/workbenchHome.ts:66-79` | 静态代码核对 | DEFECT |
| BOS-R0-04 | BOSS 跨端事实 | 一后端多端同源 | App 首屏消费 `statistic/boss/workbench`；Web 经营分析消费 `statistic/boss/business/report`，未完成字段对账 | App statistic API；Web boss API | 静态代码核对 | PENDING |
| BOS-R0-05 | BOSS 数据诚实 | `GAP-070`、MET-002/004/005 | 毛利已有成本不完整空值逻辑；App 部分补充数据失败退化为空数组/空对象 | BossDashboardContext；App `pages/index/index.vue:361-364` | 静态代码核对 | PARTIAL |
| BOS-R0-06 | BOSS 首个改造切片 | `BR-030`、`CAP-004`、`CAP-005` | 推荐 `BOS-R1 负责人统一经营总览` | `16-boss-role-product-baseline-v1.md`、`PI-20261029-001` | 待用户决策 | PLAN_READY |

### BOS-R0 未映射/待决

- BOSS Provider 字段对账矩阵：待形成。
- `BOS-D01～D04`：负责人一级空间、岗位视角、首个切片、首屏事实源待用户确认。
- `GAP-BOS-001`：微信小程序是否独立负责人壳层未决。
- `GAP-BOS-002`：负责人风险阈值是否企业可配置未决。
- `GAP-070`：经营利润、费用分摊、期间及未归属门店费用口径未冻结。

### BOS-R0 结论

- 负责人当前能力广，但默认入口仍按岗位能力平铺，负责人首屏未收口为统一经营工作面。
- 本次只完成角色/现状对账和问题登记，未修改后端、Web、App、DDL、权限或金额语义。
- 不宣称负责人工作台已验收或 1.0.73 已发布。

## BOS-R0 高保真交互预览（2026-10-29）

| 项 | 场景 | 业务规则 | 页面能力 | 交互/状态 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| BOS-R0-07 | BOSS 统一经营台直观评审 | `BR-028`、`BR-030`、`SCN-016` | 经营总览、风险表、对象抽屉、业务推进、研发安排 | Web/App、亲自处理、七态、单一主行动 | `docs/product/prototypes/owner-operating-system/unified-v1.html`；`docs/acceptance/runs/20261029-boss-unified-prototype/README.md` | PROTOTYPE_READY_FOR_REVIEW |

- 本行只证明高保真原型可运行、可交互、已真实渲染，不证明生产代码、真实 Provider 或独立验收。
- 用户尚未确认 `BOS-D01～D04`。
