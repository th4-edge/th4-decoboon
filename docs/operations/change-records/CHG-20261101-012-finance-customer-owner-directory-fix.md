# CHG-20261101-012 财务客户负责人候选目录权限修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-012` |
| 类型 | 缺陷 |
| 风险级别 | `L1` |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-11-01 |
| 关联决策 | `USER-DEC-20261029-009`、`BR-001`、`DEC-065`、`DEC-070` |

## 2. 目标与范围

- 目标：修复财务点击客户负责人时 `user/employee/page` 返回403，导致候选负责人为空。
- 范围内：后端员工候选目录权限和财务数据范围；App负责人候选不再按当前门店二次截断。
- 非范围：设计师指派/改派权限、实际跨负责人业务迁移。
- 受影响端：后端、Web、App（共用同一 Provider）。

## 3. 业务与决策

- 是否涉及状态、权限、金额、成本或数据范围：是。
- 业务事实：`USER-DEC-20261029-009` 明确财务可转交客户负责人，但不得指派设计师。
- 数据范围：总部财务企业范围，门店财务本门店范围；不得跨企业。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 财务/Web | 客户详情 | 点击客户负责人不报错，候选非空 | `04-finance-owner-options-after-fix.json` |
| 2 | 总部财务/API | 默认总店财务 | 返回企业范围候选 | `05-finance-employee-candidates-after-fix.json` |
| 3 | 门店财务/单测 | 财务数据范围为Store | 只查询本门店员工 | `EmployeePageAccessTest` |
| 4 | 业务员/API | 无候选目录权限 | 仍返回403 | `05-finance-employee-candidates-after-fix.json` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | 当前工作树 | 待提交 | `user/employee/page`、`EmployeePageAccessTest` |
| th4-decoboon-web | 当前工作树 | 无代码变更 | 复用客户详情负责人弹层 |
| th4-decoboon-app | 当前工作树 | 待提交 | `src/subpackages/customer/pages/detail.vue` 负责人候选范围 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | `EmployeePageAccessTest,CustomerOwnerAssignmentTest,CustomerOwnerChangeReasonTest` | PASS | `raw/targeted-tests-final.log` |
| 构建 | `mvn -q -DskipTests package` | PASS | `raw/build.log` |
| Provider契约 | `audit-provider-response-contract.py --check` | PASS, findings=0 | `raw/provider-contract.log` |
| 财务真实页面 | 客户详情点击负责人 | PASS，候选9人，无403 | `evidence/04-finance-owner-options-after-fix.*` |
| 同负责人提交 | Web负责人弹层 | PASS，提示已转交，DB负责人未变 | `evidence/07-finance-owner-transfer-same-owner-after-fix.*` |
| App共享Provider | 财务token `user/employee/page` | PASS，9名候选 | `evidence/05-finance-employee-candidates-after-fix.json` |
| App构建 | `type-check + build:mp-weixin + check:customer-admin` | PASS | `raw/app-typecheck-build.log` |

- 未执行：实际更换到其他负责人的写入，避免改变现有项目客户归属。
- App UI 点击自动化未触发，保留真机/手工复验。

## 7. 发布

- 不适用，本轮仅本地 dev 验证。

## 8. 回滚

- 回滚条件：财务候选目录导致越权或错误范围。
- 回滚步骤：回退 `user/employee/page` 本次改动并重启 dev。
- 实际是否回滚：否。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集。
- 人工耗时：未采集。
- 一次通过：否，用户实际点击发现。
- 返工原因：Provider角色白名单漏掉 Finance。
- 最终结论：开发验证通过，待用户确认 App/Web 实际操作。

## 10. 复盘与沉淀

- 是否出现重复问题：否。
- 是否需要新增门禁：已有定向测试覆盖财务企业/门店范围，无需新增独立脚本门禁。
- 后续动作：用户在 App 客户详情手工点击负责人；后续真实换人使用独立测试客户。
