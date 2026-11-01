# CHG-20261031-044 · 项目合同档案服务端汇总

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-044` |
| 类型 | 缺陷修复 / 后端聚合 / 前端消费治理 |
| 风险级别 | L1（单页面 + 既有合同聚合 Provider） |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | `CHG-20261031-042`、`DEC-036`、`DEC-055`、`BR-003`、`BR-041` |

## 2. 目标与范围

- 目标：给现有 `project/contract/workbench` 增加基线合同行数/金额单行聚合，Web 项目合同档案改为消费服务端汇总，删除合同行和变更金额的前端 `reduce`。
- 范围内：`ContractWorkbench` 两个汇总字段、`ContractLineRepository` 聚合查询、`ContractWorkbenchContext`、Web contract model/API/page、合同档案真实页面。
- 非范围：不改变合同、变更、金额和成本公式；不改造 Boss 增减项工作台；不处理合同行完整分页；不发布。
- 受影响端：后端、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本或数据范围：涉及既有合同行金额和已生效变更金额的读取方式，不改变口径。
- 业务事实来源与裁决编号：`DEC-036` 聚合 SQL、`DEC-055` 当前有效剩余、`BR-003` 成本边界、`BR-041` 合同变更规则。
- 尚存 GAP 或假设：无；页面继续使用既有 Designer/Boss/StoreManager/Finance 合同访问权限。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | Boss | 已变更项目合同 | workbench 行数/金额等于合同行聚合；变更金额仅累计 CustomerConfirmed | `reconciliation-change.json` |
| 2 | Designer | 同一项目可见 | 汇总可用且成本字段为空 | `workbench-designer.json` |
| 3 | StoreManager | 本店项目 | 汇总可用，成本按角色可见 | `workbench-store-manager.json` |
| 4 | Web 合同档案 | 已变更项目 | 合同范围/合同变更金额来自 workbench，不执行前端 reduce | `web-contract-archive-final.json` |
| 5 | 页面兼容 | 施工计划枚举 | acceptanceMode 显示中文名，不出现 `[object Object]` | 三档截图 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | `ContractWorkbench`、`ContractLineRepository`、`ContractWorkbenchContext`、workbench whitelist、`ContractFactTraceTest` |
| th4-decoboon-web | `dev` | 未提交工作区 | contract model/API、`DesignerProjectContractView.vue` |
| th4-decoboon-app | `dev` | 无改动 | 不适用 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | `mvn -q -Dtest=ContractFactTraceTest test` | PASS | `backend-test.log` |
| Provider 响应门禁 | `audit-provider-response-contract.py --check` + `--self-test` | PASS，543 Provider / 0 finding | `provider-response-audit.json` |
| API 对账 | workbench vs line list/change list | PASS：6 行 / ¥977.80；1 项变更 / ¥-168.00 | `reconciliation-change.json` |
| 角色范围 | Boss/Designer/StoreManager | PASS；Designer 成本为 null | `workbench-*.json` |
| Web 构建 | `npm run build` | PASS | `web-build.log` |
| 真实页面 | `/designer/projects/241940507872198656/contract` | PASS，console/network error 0 | `web-contract-archive-final.json` |
| 页面兼容 | acceptanceMode | PASS，无 `[object Object]` | 三档截图 |
| 接口时延 | 12 次 workbench 采样 | P50 61.33ms / P95 88.33ms / max 89.21ms | `api-timings.txt` |

- 未执行项及原因：合同行详情仍未改为服务端分页，留作后续独立切片；未独立验收、未发布。
- 已知失败及是否与本次相关：首次页面发现 acceptanceMode 枚举对象显示 `[object Object]`，已在同页修复并复验。

## 7. 发布

- 不发布；仅本地验证。无 DDL、迁移、配置和密钥变化。

## 8. 回滚

- 回滚条件：合同行汇总与合同行列表不一致、变更金额口径错误或页面不可用。
- 回滚命令/步骤：回退 workbench 汇总字段与 Web 页面数据源改动。
- 回滚验证：重新执行 ContractFactTraceTest、Web 构建和真实合同档案页。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否，真实页面发现并修复 acceptanceMode 枚举显示问题
- 返工原因：枚举对象未统一转中文
- 最终结论：开发自测与真实页面验证通过，待独立验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，前端对已返回的金额列表再次求和属于同一类问题
- 是否需要新增门禁：后续补前端业务聚合门禁
- 候选 `EXP/ANT/PATH`：`ANT-合同行和变更金额前端求和`、`PATH-复用 contract/workbench 服务端摘要`
- 后续动作与负责人：合同行详情分页、App 合同页同源检查
