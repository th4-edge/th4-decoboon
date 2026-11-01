# CHG-20261101-016 · 财务首页信息层级精简

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-016` |
| 类型 | App/Web UI 优化 |
| 风险级别 | `L1` |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联业务裁决 | `REQ-BIZ-20261101-001`、`ANS-BIZ-20261101-001`、`DEC-018`、`BR-016`、`MET-006` |

## 2. 目标与范围

- 目标：把财务首页从“指标解释页”收敛为“现金判断 + 风险行动 + 月度摘要”的行动工作台。
- 范围内：Web `/finance/workbench`、App 财务首页；删除重复指标区和资金闭环示意，将完整指标口径收纳到按需抽屉/入口。
- 非范围：不修改金额公式、Provider、权限、状态机、路由、菜单、数据结构或后端；不新增业务对象。
- 受影响端：App、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及金额展示位置，不改变口径。
- 业务事实来源与裁决编号：`DEC-018`、`BR-016`、`MET-006`、`DEC-065/DEC-067`、`ANS-BIZ-20261101-001`。
- 尚存 GAP 或假设置：本轮不处理 `MET-005` 历史跨端口径冲突和 `GAP-070` 公司/门店经营利润草案。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 财务打开 Web 首页 | 有汇总数据 | 首屏先显示现金余额与需关注收付，不显示常驻口径百科和资金闭环图 | 1440/1280/1024 截图 |
| 2 | 财务打开 App 财务首页 | 有汇总数据 | Hero 显示现金、待收、待付、经营结果；风险待办紧随其后 | H5/小程序截图或构建证据 |
| 3 | 查看指标口径 | 点击“口径说明” | 完整指标字典在抽屉/独立入口中可查，失败可重试 | 浏览器交互证据 |
| 4 | 成本不完整 | `costComplete=false` | 显示“成本未完整”，不显示 0 利润 | 页面文案/源码核对 |
| 5 | 汇总读取失败 | 接口失败 | 显示暂不可确认与恢复路径，不用 0 或空数组伪装 | 故障态检查 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 未提交 | `docs/operations/change-records/CHG-20261101-016-finance-home-information-hierarchy.md` |
| th4-decoboon-web | dev | 未提交 | `src/views/finance/FinanceWorkbenchView.vue`、`src/components/finance/FinanceMetricGlossary.vue`、`src/styles/finance.scss` |
| th4-decoboon-app | dev | 未提交 | `src/pages/finance/index.vue`、`src/components/JzMetricGlossary.vue`、`src/styles/finance.scss` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| Web 类型检查 | `npm run type-check` | PASS | 本次执行记录 |
| Web 内容门禁 | `check:ai-flavor` / `check:client-copy` / `check:identifier-exposure` | PASS | 本次执行记录 |
| Web 构建 | `npm run build` | PASS | 本次执行记录 |
| App 类型检查 | `npm run type-check` | PASS | 本次执行记录 |
| App 定向门禁 | `check:finance-ia` / `check:client-copy` / `check:ui` | PASS | 本次执行记录 |
| App H5 构建 | `npm run build:h5` | PASS | 本次执行记录 |
| App 小程序构建 | `npm run build:mp-weixin` | PASS | 本次执行记录 |
| Web 真实渲染 | 1440×900 / 1280×800 / 1024×768 | PASS；无横向溢出，内联口径与资金闭环均不存在 | `th4-decoboon-web/output/playwright/finance-home-ia-20261101/` |
| App H5 渲染 | 430×932 / 390×844 / 375×812 | PASS；无横向溢出、无统计卡墙和资金闭环 | `th4-decoboon-app/output/playwright/finance-home-ia-20261101/` |
| 口径故障隔离 | 指标字典 500，财务汇总成功 | PASS；进页时字典请求 0，打开口径后请求 1，仅抽屉显示重试，首页金额不变 | `app-finance-metric-error.png` |
| 机器视觉预检 | 1440 / 1024 / 口径抽屉 / App 390 | 1440、App、抽屉无 P0；1024 的溢出报告经 DOM 全量扫描否定 | 同上目录 `vision-*.md`、`README.md` |

- 未执行项及原因：未导入微信开发者工具做真实小程序真机点击，也未做人工视觉签字；H5 + 小程序构建用于本轮布局回归。
- 已知失败及是否与本次相关：视觉模型在 1024 档误报横向溢出；DOM 证据显示 `scrollWidth=viewport=1024`、越界元素 0，不构成本次缺陷。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 不适用 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：财务人员无法在首屏判断现金或风险，或口径入口不可用、成本完整性表达回退。
- 回滚命令/步骤：恢复三个前端仓库的本次页面改动；后端文档与 CHG 记录保留。
- 回滚验证：原财务首页可加载，风险待办和口径说明入口恢复。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是（类型检查、构建、三档渲染首次执行通过）
- 返工原因：无
- 最终结论：开发自测通过，待用户确认和独立验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，财务首页已有指标说明常驻导致行动区被稀释。
- 是否需要新增门禁：暂不新增硬门禁；先通过页面特定验收验证，避免为单页无限追加检查。
- 候选 `EXP/ANT/PATH`：待完成后沉淀“口径可查不等于首页常驻说明”。
- 后续动作与负责人：完成 App/Web 页面改造与三档渲染验证，再决定是否将同类规则扩展到其他角色首页。
