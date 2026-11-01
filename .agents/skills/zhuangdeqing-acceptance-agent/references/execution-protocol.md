# 逐例执行协议

## 1. 执行前

对每条用例确认：

```text
case.status == READY
oracleId 与当前 test suite 一致
providerRoutes 在当前后端源码可定位
pageTargets 在当前前端路由可定位
fixture / tenant / account / data scope 可用
证据要求已明确
```

不满足时不得开始写操作，结果记 `BLOCKED` 或 `STALE`。

## 2. 页面优先

- 业务状态只能通过当前角色可见的菜单、按钮、表单、选择器和确认动作推进；
- API、SQL、日志用于核验，不用于补页面未实现的终态；
- 隐藏调试路由、修改 localStorage、注入脚本、伪造身份和替角色操作均不合法；
- 正常刷新、返回、深链接和重新进入属于恢复验证，不属于绕过。

## 3. 每步记录

```yaml
step_id:
actor:
role:
surface:
page_path:
control:
control_state:
business_state_before:
business_state_after:
feedback:
console_or_network:
evidence_ids: []
```

## 4. 页面与事实对账

按用例要求至少组合：

- 页面显示事实；
- Provider 响应事实；
- 数据库或独立汇总事实；
- 同角色刷新后的回读；
- 跨角色或跨端回读。

页面/接口/数据库冲突时，结果记 `FAIL`，不得选择对自己结论有利的一方。

## 5. 状态判定

| 状态 | 条件 |
| --- | --- |
| `PASS` | 所有步骤、预期页面、预期状态、不变量和证据均满足 |
| `FAIL` | 可复现地违反业务规则、数据不变量或页面契约 |
| `BLOCKED` | 环境、身份、授权、夹具、业务裁决或依赖不可用 |
| `NOT_RUN` | 尚未执行 |

`PARTIAL` 不用于单条用例结果；未完成的完整用例只能是 `BLOCKED` 或 `NOT_RUN`。
