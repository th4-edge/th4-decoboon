# CHG-20261030-008 · 店长本店薪酬动作权限扩展

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-008 |
| 类型 | 权限 / 需求 |
| 风险级别 | L3 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | `DEC-097`、`ANS-BIZ-20261030-002`、`EV-429`、`DEC-093` |

## 2. 目标与范围

- 目标：在本店范围内将工资单确认、退回、Draft 删除和员工工资/提成发放下放给店长。
- 范围内：PayrollAccessContext 动作角色、工资单结算/删除、员工发放 page/save、App/Web 能力和入口、定向与构建验证。
- 非范围：店长薪酬方案/考勤/提成计提/工资单生成；混合门店整单操作；B8B 作废和 DDL；生产发布。
- 受影响端：后端、App、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及薪酬动作、应付、成本和本店范围。
- 业务事实来源与裁决编号：`DEC-097`、`ANS-BIZ-20261030-002`、`EV-429`。
- 尚存 GAP 或假设：作废反向事实、版本/唯一约束和迁移方案仍待 B8B 技术设计；本批不实现作废。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | StoreManager 确认本店 Draft/Pending 工资单 | 本店工资项、考勤完整、实发非负 | 生成应付/成本，状态 Confirmed | 定向测试、真实页面 |
| 2 | StoreManager 退回 Pending | 本店工资单、原因必填 | 状态 Draft，原因回读 | 定向测试、真实页面 |
| 3 | StoreManager 删除本店 Draft | 无财务事实 | 工资单和工资项删除 | 定向测试 |
| 4 | StoreManager 发放本店员工应付 | 应付已确认、本店范围 | 核销、台账、幂等 | 定向测试、真实页面 |
| 5 | StoreManager 操作混合门店整单 | 工资单含他店工资项 | 403，提示由 BOSS/财务处理 | `requireStoreItemsInStore` 测试 |
| 6 | Finance 最终确认/退回/删除 | Finance 有复核/发放权限 | 仍 403 | 角色回归 |
| 7 | BOSS 原动作 | 企业范围 | 原能力不退化 | 回归 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 待提交 | `PayrollAccessContext`、`PayrollSettlementContext`、sheet delete、employee payment |
| th4-decoboon-app | `dev` | 待提交 | `roles/permission.ts`、`sheet-detail.vue`、契约脚本 |
| th4-decoboon-web | `dev` | 待提交 | `payrollAccess.ts` 及店长动作入口 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向 | confirm/return/delete/payment/manager scope | PASS，16/16 | `docs/acceptance/runs/20261030-payroll-manager-actions/backend-verification.txt` |
| App 契约/类型/构建 | payroll contract、type-check、UI、导航、文案、mp build、分包 | PASS | `docs/acceptance/runs/20261030-payroll-manager-actions/frontend-verification.txt` |
| Web 类型/构建 | type-check、build、权限/门店/工作台门禁 | PASS | 同上 |
| 真实店长本店/跨店页面 | 受控企业 | 待执行 | 待补 |

## 7. 发布

- Release ID：未发布
- 数据库迁移：无
- 回滚：回退本 CHG 的 Provider/前端能力；无数据库回滚
- 发布后观察：不执行，待真实页面验收与用户确认

## 8. 结论

- 当前：`DEV_VERIFIED / PENDING_REAL_STORE_MANAGER_UI`
- 作废：B8B `BLOCKED`，不得以动作权限裁决替代反向事实和 DDL 设计
