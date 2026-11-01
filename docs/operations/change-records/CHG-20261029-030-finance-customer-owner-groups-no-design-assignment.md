# CHG-20261029-030 · 财务客户页负责人分组与非设计派单边界

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261029-030 |
| 类型 | 缺陷 / 权限边界 / 客户页面优化 |
| 风险级别 | L2 |
| 状态 | 已由 CHG-20261029-031 补充统一工作面 |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `USER-DEC-20261029-009`；部分修订 `DEC-080` 第 4 条 |

## 2. 目标与范围

- 目标：财务客户页支持按客户负责人分组；财务不再承担设计师分配、指派和改派。
- 范围内：
  - 小程序财务客户页负责人分组、筛选和客户列表；
  - 财务能力矩阵移除 `act:assignDesigner`；
  - 客户详情不再向财务展示设计师指派入口；
  - 后端 `customer/customer/handoff` 拒绝财务；
  - 客户经营概览 Provider 支持财务按登录员工数据范围返回负责人分组；
  - Web 财务客户页隐藏指派设计师动作；
  - Boss、店长、团队负责人既有派单能力保持不变。
- 非范围：财务其他客户经营能力、金额口径、企业治理、生产发布。
- 受影响端：后端 / Web / App。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是；仅调整设计师指派权限，不改变客户数据范围与金额口径。
- 业务事实来源与裁决编号：`USER-DEC-20261029-009`；继续遵循 `DEC-080` 的财务客户入口与数据范围，推翻其“财务可指派/改派设计承接人员”部分。
- 尚存 GAP 或假设置：无；用户本次明确要求作为最新高优先级决策。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 财务查看客户 | 进入小程序客户 Tab | 显示按负责人分组、负责人客户数和风险数；无待指派设计筛选或派单卡 | `finance-customer-page.png` |
| 2 | 财务选择负责人 | 点击负责人分组 | 客户列表、阶段统计按该负责人刷新，分组仍完整可见 | 真实 DevTools 夹具走查 |
| 3 | 财务客户详情 | 打开客户详情 | 无指定/改派设计师入口；仍可转交负责人 | 能力矩阵与页面核验 |
| 4 | 财务直接调用 Provider | 调用 `customer/customer/handoff` | 返回 403：仅团队负责人、店长或企业负责人可以指定设计师 | `HandoffDesignerChangeTest`、HTTP 验收 |
| 5 | 店长客户页 | 进入客户 Tab | 保留待指派设计筛选、派单卡与指派入口 | `storeManager-customer-page.png` |
| 6 | Boss/Web/数据范围 | 财务调用客户经营概览 | 负责人分组按企业或门店数据范围返回，不跨范围、不前端全量拼组 | `CustomerOverviewFinanceScopeTest`、HTTP 验收 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | `CustomerOverviewContext`、`Overview`、`Handoff`、财务/客户/Provider 安全测试 |
| th4-decoboon-app | `dev` | 未提交 | `customer/list.vue`、`roles/permission.ts`、客户管理/首页契约 |
| th4-decoboon-web | `dev` | 未提交 | `StoreCustomerView.vue` 财务指派动作门禁 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端全量测试 | `mvn -q test` | PASS；1128 / 0 failures / 0 errors / 0 skipped | `target/surefire-reports` |
| 角色权限定向测试 | `HandoffDesignerChangeTest`、`CustomerOverviewFinanceScopeTest`、财务 HTTP 验收、Provider 安全面板 | PASS | Surefire |
| 小程序发布前校验 | `npm run verify:mp-weixin` | PASS；含 type-check、mp-weixin build、分包与客户/权限契约 | `/tmp/th4-app-verify.log` |
| Web 构建 | `npm run build` | PASS；含 TypeScript、分页、壳层、品牌、编号与构建 | Web `dist` |
| Web 权限审计 | `npm run check:permissions` | PASS | 本地命令输出 |
| 角色页面截图 | 微信开发者工具客户页 | PASS；Boss/财务/店长/业务员/设计师/项目经理六种投影 | `docs/acceptance/runs/20261029-app-role-customer-pages/` |

- 未执行项及原因：未上传体验版、未提审、未发布。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 当前 `dev` 工作区 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：用户验收发现财务客户经营能力被过度收窄，或负责人分组影响既有客户范围。
- 回滚命令/步骤：回退三仓库本变更文件；恢复 `DEC-080` 财务派单能力并重跑后端/App/Web 门禁。
- 回滚验证：财务可重新调用 handoff，客户页恢复旧布局，其他角色不受影响。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；首次全量测试发现旧财务派单断言与既有 Provider 面板计数基线过期，更新后全量通过。
- 返工原因：`DEC-080` 与用户新决策冲突；集成分支新增 `invite/invitation/pending` 未同步安全面板数量。
- 最终结论：待用户验证

## 10. 复盘与沉淀

- 是否出现重复问题：否；本次为业务裁决变更，不是同一缺陷复发。
- 是否需要新增门禁：否；已更新现有客户管理、首页权限、财务 HTTP 与 Provider 安全测试。
- 候选 `EXP/ANT/PATH`：负责人分组必须复用服务端数据范围聚合；角色动作隐藏必须同时修改能力矩阵与服务端 Provider。
- 后续动作与负责人：用户验收财务页负责人分组与无派单边界；确认后再决定是否上传体验版。
