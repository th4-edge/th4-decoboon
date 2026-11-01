# CHG-20261030-005 · 员工本人月度考勤跨端查看

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-005 |
| 类型 | 需求 / 权限 |
| 风险级别 | L3 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | `BR-038`、`DEC-017`、`PI-20261023-001` |

## 2. 目标与范围

- 目标：补齐员工本人在 App/Web 查看本人月度考勤的能力，并保持同一 Provider、同一权限和同一数据范围。
- 范围内：`attendance/summary/page|info` 的本人范围、`selfOnly` 本人视角、App 本人考勤页与入口、Web 本人考勤页与账户入口、定向测试和构建门禁。
- 非范围：员工编辑/确认考勤、本人提成、店长敏感字段裁剪、工资单/应付/发放/作废、DDL、迁移、生产发布。
- 受影响端：后端、Web、App。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及考勤读取权限和 Self 数据范围。
- 业务事实来源与裁决编号：`BR-038`（员工本人可查看本人考勤）、`DEC-017`（员工本人仅本人工资条、提成、考勤）。
- 尚存 GAP 或假设：无；本批不裁决“个税明细是否包含汇总税额”。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 普通员工打开本人月度考勤 | 已登录且存在本人考勤 | 只返回本人记录；无需提交员工 ID；接口失败显示暂不可确认 | 后端定向测试、App/Web 构建 |
| 2 | 管理角色打开本人考勤 | BOSS/Finance/StoreManager 有管理范围 | `selfOnly=true` 时仍只返回登录人本人记录 | 后端定向测试 |
| 3 | 普通员工尝试读取他人考勤详情 | 已知他人 summaryId | 返回“不存在或无权访问” | 后端定向测试 |
| 4 | 管理角色读取本店/企业考勤 | 原权限不变 | 原有管理视角不退化 | 回归测试 |
| 5 | App/Web 入口 | 员工账号 | 均可进入本人考勤，不出现编辑、确认、删除或他人数据 | 两端构建；真实页面待验收 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 待提交 | `PayrollAccessContext`、`attendance/summary/page|info`、定向测试 |
| th4-decoboon-app | `dev` | 待提交 | `domains/payroll/api.ts`、`my-attendance.vue`、个人中心入口、权限/路由/契约 |
| th4-decoboon-web | `dev` | 待提交 | `domains/finance/api.ts`、`MyAttendanceView.vue`、账户路由和菜单 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `PayrollAttendanceInfoScopeTest,PayrollAttendanceSelfViewTest,PayrollAttendanceAccessContextTest` | PASS，8/8 | `docs/acceptance/runs/20261030-payroll-self-attendance/backend-verification.txt` |
| 后端全量回归 | Java 22 `mvn -q test` | PASS，1136 tests / 0 failures / 0 errors / 0 skipped | 同上 |
| App 契约/类型/构建 | payroll contract、type-check、client-copy/UI/导航、`build:mp-weixin`、分包检查 | PASS | `docs/acceptance/runs/20261030-payroll-self-attendance/frontend-verification.txt` |
| Web 类型/构建 | type-check、production build、权限/门店/工作台门禁 | PASS | 同上 |
| 真实页面/越权 | 受控员工与管理角色账号 | 待执行 | 待补 |

- 未执行项及原因：真实账号跨端回读待受控企业环境；不以构建替代真实页面验收。
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

- 回滚条件：普通员工可读取他人考勤、管理角色原范围退化、App/Web 任一端无法读取本人考勤。
- 回滚命令/步骤：回退本 CHG 对应后端/App/Web 文件；无数据库回滚。
- 回滚验证：恢复原管理视角；员工本人接口恢复为拒绝。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否
- 返工原因：Web 固定大页门禁拦截初稿 `size: 200`，改为 `current/size` 分页后通过；另修正一处 Sass Token 名称。
- 最终结论：开发验证通过，真实页面与独立验收待执行

## 10. 复盘与沉淀

- 是否出现重复问题：员工本人薪酬能力此前仅有工资条，考勤和提成未落地。
- 是否需要新增门禁：是，后端必须固定覆盖“非管理员工始终 Self”和“管理角色 selfOnly 仅本人”。
- 候选 `EXP/ANT/PATH`：员工自助页面不能按客户端角色决定数据范围，必须由同一 Provider 固定投影。
- 后续动作与负责人：完成开发验证后补真实页面证据，再决定是否进入本人提成或店长敏感字段批次。
