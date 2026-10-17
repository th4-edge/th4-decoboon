# 材料域 B6 独立验收与对账

- 批次：B6（独立验收，不是 B1–B5 实现者自批）
- 日期：2026-10-17
- 结论：**FAIL**（仅作为独立验收建议；不是 `ACCEPTED`）
- 结论范围：可以据此进入用户最终决策，但当前证据不支持 `ACCEPTED`
- 环境：本地 dev；后端 `/tmp/th4-b6-backend-af12b9f` commit `af12b9f`（8090）；App `/tmp/th4-b6-app-225b963` commit `225b963`（H5 8080）；MySQL `127.0.0.1:3306/decoboon`；Redis 已运行
- 边界：未触碰生产；未执行 DDL/迁移；未修改 App/后端实现；未执行正式小程序提审/发布；只在本目录 `evidence/B6/` 写证据

## 1. 验收方法

1. 黑盒登录 6 个账号（Boss M、StoreManager A、Designer A、PM A、Finance A、Boss S），直接调用 `8090/api/*` Provider；
2. 用真实 Provider 完成一条临时材料链：作业单草稿 → PM 测量 → 提交请购 → Finance 拆单（有价/无价、两个供应商）→ 到场 → 逐单/月结结算 → 撤销；临时数据验收后清理，种子行逐列恢复；
3. 对旧直采路线、App 旧入口、跨企业/跨门店、角色越权、重复请求、成本边界、助手 50 黄金问题做负向/边界验证；
4. 用 H5 390×844 真实浏览器渲染主材下单工作台、下单页、结算页、空态、到场态、项目材料体检、材料计划/供应商页；截图见 `screenshots/`；
5. Web 不在 B6 新改；按 run-envelope 允许复用 B2 真实浏览器证据，并对同一 Provider 重新取数做跨端对账。

## 2. 逐项矩阵

| 类别 | 通过 | 失败 | 合计 | 结论 |
| --- | ---: | ---: | ---: | --- |
| 正向 | 17 | 0 | 17 | 主链可跑通：建单、测量、提交、拆单、到场、逐单/月结、撤销、成本收敛/回退 |
| 反向 | 7 | 0 | 7 | 未选材、超量、缺测量、已下单取消、越权建单、空明细均被拦截 |
| 越权/权限 | 15 | 3 | 18 | 跨企业/跨门店/角色负向通过；但 Designer/PM 成本与结算读权限失败 |
| 幂等/重复 | 5 | 1 | 6 | 重复结算/到场/提交/审计稳定；重复订单生成重复采购/应付 |
| 边界 | 6 | 2 | 8 | 成本 null/收敛/回退正确；未到场可结算、暂估应付读模型显示 0 |
| 旧路线 | 5 | 0 | 5 | 旧 Provider 与旧 App 入口不可达；历史采购/到场保留 |
| 材料助手 | 55 | 0 | 55 | 50/50 黄金问题 + 权限/拒答/审计 5 项 |
| 跨端 | 4 | 0 | 4 | App 与 Provider/B2 Web 证据的阶段、计数、成本、下一步一致 |
| UI | 7 | 0 | 7 | 390×844 无横向溢出；业务 Console 错误 0；空态/旧入口不可达 |
| **合计** | **121** | **6** | **127** | **FAIL** |

明细逐条见 `blackbox-results.json`；缺陷台账见 `defects.json`；关键请求/响应见 `api-samples.json`。

## 3. 阻断结论的缺陷

| ID | 级别 | 问题 | 证据 |
| --- | --- | --- | --- |
| B6-DEF-001 | P1 | Designer 调 `material/sheet/info` 返回 `quoteCostPrice=120.0`；页面却称作业单不承载价格/成本 | `X12`、`screenshots/b6-h5-sheet-detail-arrived.png` |
| B6-DEF-002 | P1 | Designer/PM 调 `project/purchase/page` 可读 `amount=300`、供应商名称/联系人 | `X11` |
| B6-DEF-003 | P1 | Designer/PM 调 `material/settlement/page|info` 可读结算金额 500/300/330 与供应商；preview/confirm 已正确 403 | `X10` |
| B6-DEF-004 | P1 | 两次完全相同 `material/sheet/order` 均成功，生成两张采购单和两条 300 元应付 | `I01`、`api-samples.json#duplicate_order` |
| B6-DEF-005 | P1 | `receivedQuantity=0` 的采购明细仍进入结算候选，`settlement/confirm` 仍成功生成 330 元结算 | `B04`、`api-samples.json#settlement_before_receipt` |
| B6-DEF-006 | P2 | 无价采购 DB `estimated=true`，但 `finance/payable/page` 返回 `amount=0.0` 且不暴露 `estimated` | `B07`、`api-samples.json#unpriced_payable_projection` |

## 4. 已独立验证通过的关键事实

- **主链可跑通**：Designer 建单/提交，PM 测量/到场，Finance 下单/结算/撤销；作业单状态按 `Submitted → Ordered → Arrived` 推进；同作业单可拆给两个供应商，支持无价行。
- **结算互斥/月结兜底**：逐单结算后，月结候选只保留未结算明细；重复结算被 400 拒绝；撤销后明细释放、应付回到采购价口径。
- **成本边界**：未结算时 `costComplete=false`、利润/毛利率 `null`；全部结算后 `costComplete=true`、`profitAmount=-1554.25`、`grossMargin=-62.3`；撤销后回到 `false/null`。
- **旧路线**：`project/purchase/save`、`project/purchase/batch` 返回 `canNotFoundHandler`；App 三个旧页面路由空白且 Vue Router no match；材料计划/供应商页只保留“从主材作业单下单”和“历史采购/到场”。
- **助手**：50/50 黄金问题意图匹配、引用非空、`readOnly=true`、`auditStatus=RECORDED`；Designer 本人项目不返回金额；跨项目/跨企业 404；越界动作 REFUSAL；无证据 NO_EVIDENCE；同一 `requestId+idempotencyKey` 返回同一 auditId；审计表不存问题/回答正文。
- **跨端**：App 项目详情显示“1 条已选材 / 待下单 0 / 待到货 0 / 材料应付 ¥3,100 / 实际成本 ¥2,560 / 利润成本未完整 / 下一步完成采购结算或补齐成本”，与 `blueprint/check/report` 同源一致；作业单 Provider `Arrived` 与 App“已到场/成本待结算”一致。
- **UI**：H5 390×844 各页 `scrollWidth=innerWidth=390`；业务 Console Errors=0，仅 `favicon.ico` 404；空态、失败态、旧入口不可达均有真实浏览器证据。

## 5. 证据文件

- `blackbox-results.json`：127 条逐项结果（含 50 条黄金问题）
- `defects.json`：6 个开放缺陷，含复现、影响、根因假设
- `summary.json`：结论、矩阵计数、需求状态、清理与限制
- `api-samples.json`：重复订单、未到场结算、成本收敛/回退、权限泄漏、跨租户、旧路线、助手审计样本
- `browser-metrics.json`：390×844 宽度/溢出/Console 摘要
- `ui-text.txt`：关键页面真实文本
- `screenshots/`：
  - `b6-h5-workbench-submitted.png`
  - `b6-h5-sheet-order.png`
  - `b6-h5-settlement-pending.png`
  - `b6-h5-settlement-empty.png`
  - `b6-h5-workbench-after-arrival.png`
  - `b6-h5-sheet-detail-arrived.png`
  - `b6-h5-settlement-after-seed.png`
  - `b6-h5-project-detail.png`
  - `b6-h5-subpackages-project-pages-material-plan-workbench.png`
  - `b6-h5-subpackages-supply-pages-index.png`
  - `b6-h5-old-entry-unreachable.png`

## 6. 临时数据与恢复

临时数据仅用于本地 dev 验收，已清理并校验：

- 临时供应商、临时作业单、3 张临时采购单/明细、3 条到场、5 条临时结算（含 1 条未到场探测）已删除；
- 临时使用的既有采购单/计划/报价行/应付逐列恢复到验收前快照，`diff=0`；
- 临时把 PM 设为测试项目负责人以验证 PM 测量/到场，验收后已恢复 `manager_id=NULL`；
- 清理后：作业单 0、临时供应商 0、结算 0、采购单恢复 18；测试项目材料体检恢复为 `1 计划 / 1 已选材 / 1 部分到场 / costComplete=false / profit=null`；
- 未执行 DDL/迁移、生产部署、正式小程序提审/发布。

## 7. 未闭环项与风险

1. **P1 权限泄漏**：Designer/PM 可通过 `material/sheet/info|page`、`project/purchase/page`、`material/settlement/page|info` 读取成本/采购金额/结算金额；这是进入 `ACCEPTED` 前必须修复的硬阻断。
2. **P1 幂等**：`material/sheet/order` 无业务幂等键，重复点击/重试会重复生成采购与应付。
3. **P1 结算门禁**：结算候选/确认未校验 `receivedQuantity>0`，未到场可结算。
4. **P2 暂估应付读模型**：`finance/payable/page` 对无价应付显示 0 且不暴露 `estimated`。
5. **种子数据限制**：PM 账号初始没有可见项目（项目 `manager_id` 为空）；PM 正向流程是在临时赋权后验证的，已恢复。该限制属于本地造数数据，不计为代码缺陷，但会影响后续验收复现。
6. **未执行真机**：B6 使用 H5 390×844 真实浏览器作为 App 证据，未跑微信开发者工具/真机；正式体验版发布仍留在 B7。
7. **Web 未重渲染**：按 run-envelope 复用 B2 真实浏览器证据 + 实时 Provider 对账，未新改 Web。

## 8. 是否满足进入用户最终 ACCEPTED 决策的独立证据条件

- **用于判定 FAIL 的独立证据条件：满足。** 已独立覆盖正向、反向、越权、幂等、边界、旧路线、助手、跨端、UI，并有真实请求/响应、真实 H5 截图和清理恢复证据。
- **用于判定 ACCEPTED 的独立证据条件：不满足。** 6 个缺陷（尤其 5 个 P1）未闭环；B6 不建议自批 `ACCEPTED`。需由实现者修复后重新执行受影响用例，再由用户做最终 `ACCEPTED` 决策。

## 9. 实际执行摘要

- 登录 6 个账号，直接调用 Provider；未改实现代码；
- 创建临时供应商/作业单/拆单/到场/结算/撤销，完成主链；清理并逐列恢复；
- 复跑 50 个黄金问题（50/50）及权限/拒答/审计负向；
- 验证 `project/purchase/save|batch` 旧路线、3 个旧 App 路由、材料计划/供应商页；
- H5 Playwright 390×844 渲染 11 张截图，检查 `scrollWidth`、Console、空态、旧入口；
- 对比 `blueprint/check/report`、`finance/project/report`、App 项目详情/作业单文本；
- 未执行 DDL/迁移、生产、正式小程序提审/发布。
