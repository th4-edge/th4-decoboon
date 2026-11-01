# CHG-20261030-006 · 员工本人提成跨端查看

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-006 |
| 类型 | 需求 / 权限 |
| 风险级别 | L3 |
| 状态 | 待发布 |
| 负责人 | Codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | `BR-038`、`DEC-017`、`PI-20261023-001` |

## 2. 目标与范围

- 目标：补齐员工本人在 App/Web 查看本人提成，复用同一 Provider、同一 Self 数据范围和同一金额字段。
- 范围内：`payroll/commission/page|info` 本人范围、`selfOnly`、App/Web 本人提成只读页面和账户入口、定向测试与构建门禁。
- 非范围：计提、释放、调整、发放、工资单、应付、店长敏感字段、DDL、迁移、生产发布。
- 受影响端：后端、App、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及提成读取权限和金额字段。
- 业务事实来源与裁决编号：`BR-038`（员工本人可查看本人提成）、`DEC-017`（员工本人仅本人工资条、提成、考勤）。
- 尚存 GAP 或假设：无；不裁决店长“个税明细”范围。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 普通员工查看本人提成 | 已登录且存在本人提成 | 只返回本人记录；金额字段与服务端一致 | 后端定向测试、App/Web 构建 |
| 2 | 管理角色打开本人提成 | BOSS/Finance/StoreManager | `selfOnly=true` 时只返回登录员工本人 | 后端定向测试 |
| 3 | 普通员工读取他人提成详情 | 已知他人 commissionId | 返回“不存在或无权访问” | 后端定向测试 |
| 4 | 管理角色读取管理列表 | 原企业/门店范围 | 原范围不退化 | 回归测试 |
| 5 | 两端本人页面 | 员工账号 | 只读，不出现计提/释放/调整/发放动作 | 两端构建；真实页面待验收 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 待提交 | `PayrollAccessContext`、`payroll/commission/page|info`、定向测试 |
| th4-decoboon-app | `dev` | 待提交 | `domains/payroll/api.ts`、`my-commission.vue`、个人中心入口、路由/契约 |
| th4-decoboon-web | `dev` | 待提交 | `domains/finance/api.ts`、`MyCommissionsView.vue`、账户路由和菜单 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `PayrollCommissionAccessContextTest,PayrollCommissionSelfViewTest,PayrollCommissionInfoScopeTest` | PASS，8/8 | `docs/acceptance/runs/20261030-payroll-self-commission/backend-verification.txt` |
| 后端全量回归 | Java 22 `mvn -q test` | PASS，1144 tests / 0 failures / 0 errors / 0 skipped | 同上 |
| App 契约/类型/构建 | payroll contract、type-check、UI/导航、`build:mp-weixin`、分包检查 | PASS；`check:client-copy` 被无关设计模板页阻断 | `docs/acceptance/runs/20261030-payroll-self-commission/frontend-verification.txt` |
| Web 类型/构建 | type-check、production build、权限/门店/工作台门禁 | PASS | 同上 |
| 真实页面/越权 | 受控员工与管理角色账号 | 待执行 | 待补 |

- 未执行项及原因：独立验收未执行；真实 App/Web 页面回读已通过。
- 已知失败及是否与本次相关：待验证。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 未发布 |
| 制品路径/SHA-256 | 未生成 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 发布前按 `scripts/deploy-prod.sh` 自动保留 |
| 健康检查 | 待发布 |
| 发布后观察 | 待发布 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：普通员工可读取他人提成、管理范围退化、本人金额字段错配或两端无法读取本人提成。
- 回滚命令/步骤：回退本 CHG 对应后端/App/Web 文件；无数据库回滚。
- 回滚验证：恢复原管理视角；员工本人提成恢复为拒绝。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否
- 返工原因：首轮定向测试缺少 `StoreFilterContext` 注入和分页 Stub；修正测试装配后通过。Web 首轮文案门禁命中“服务端结果”，已改为业务表达。
- 最终结论：真实 App/Web 页面 PASS，用户 2026-10-30 已确认；记 `USER_ACCEPTED_NON_INDEPENDENT`，独立验收与生产发布未执行

## 10. 复盘与沉淀

- 是否出现重复问题：员工本人薪酬自助此前只完成工资条和考勤，提成仍未落地。
- 是否需要新增门禁：是，固定覆盖“非管理员工始终 Self”和“管理角色 selfOnly 仅本人”。
- 候选 `EXP/ANT/PATH`：员工自助页面只能展示服务端金额，不能用前端公式重算提成。
- 后续动作与负责人：开发验证后补真实页面证据，再处理店长敏感字段边界。
