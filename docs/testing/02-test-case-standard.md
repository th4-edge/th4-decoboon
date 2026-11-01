# 测试用例规范

## 1. 用例 ID

```text
TC-<AREA>-<ROLE-GROUP>-<FLOW>-<NNN>
```

示例：`TC-FIN-FINANCE-RECEIPT-001`。

## 2. 必填字段

| 字段 | 要求 |
| --- | --- |
| `id` | 全局唯一、稳定、不复用 |
| `title` | 以用户目标命名，不以页面功能命名 |
| `priority` | `P0/P1/P2/P3` |
| `area` | 与覆盖模型一致的业务区域 |
| `roles` | 实际执行角色和数据范围 |
| `surfaces` | `Web/App/Owner/API` |
| `scenarioTypes` | happy/negative/permission/order/concurrency/recovery/boundary/parity |
| `businessRuleIds` | 预期结果的事实来源，不得为空；支持 BR/DEC/SCN/OBJ/MET 与 USER-DEC |
| `fixture` | 使用的阶段夹具或独立数据集 |
| `preconditions` | 租户、账号、对象状态、金额/成本前置 |
| `steps` | 页面动作或验证步骤，动作写清角色和端面 |
| `expectedPage` | 页面应显示的结论、下一步和异常 |
| `expectedState` | 对象、状态、金额、权限、责任人的结果 |
| `expectedInvariants` | 可被 API/DB/对账验证的不变量 |
| `forbiddenOutcomes` | 明确禁止的错误结果 |
| `providerRoutes` | 必须存在的同源 Provider，至少一条 |
| `pageTargets` | 页面入口，格式 `web:/path`、`app:/path`、`owner:/path` |
| `evidenceRequired` | 截图、网络、接口、SQL、视频、人工见证 |
| `executionMode` | `manual/e2e/contract/probe/hybrid` |
| `status` | `READY/BLOCKED/STALE/UNMAPPED` |

## 3. 预期结果纪律

- 预期结果必须来自 `BR/DEC/SCN/OBJ/MET/USER-DEC`；
- 当前页面行为、现有接口返回和数据库现状不能反过来证明业务正确；
- 业务规则存在冲突时，用例标 `BLOCKED` 并关联 `GAP-*`；
- 页面、接口和数据库只允许作为同一条业务事实的不同证据面，不允许互相替代。

## 4. 反向用例最低要求

每个业务区域至少覆盖一种：

- 越权或跨范围；
- 非法状态跳转；
- 重复提交；
- 缺失前置或错误字段；
- 失败后的恢复；
- 金额/数量超过边界；
- 历史快照被错误回写。

## 5. 证据最低要求

| 用例类型 | 最低证据 |
| --- | --- |
| 页面正向 | 操作前后截图 + 页面路径 + 角色 + 状态回读 |
| 页面负向 | 拒绝提示截图 + 网络响应/日志摘要 + 状态未变化 |
| 金额/成本 | 页面 + Provider + DB 对账 |
| 权限 | 允许账号正向 + 拒绝账号负向 + 数据范围 |
| 跨端 | App + Web/业主端同事实截图与接口摘要 |
| 并发/幂等 | 两次请求或双击证据 + 唯一事实计数 |
