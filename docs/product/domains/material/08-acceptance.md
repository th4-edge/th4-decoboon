# 材料履约与材料业务助手 · 独立验收

## 范围

- Batch：B2 已具备独立验收输入；
- 角色：Boss / StoreManager / Finance；
- 环境：本地 dev / 真实浏览器；
- 数据/租户：造数企业 M，Company / Store 范围。

## 用例

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 | 蓝图中心按角色和版本展示 | 阶段/对象/字段/来源同源 | 待独立验收 | `evidence/B2/` | PENDING |
| 正向 | 项目材料体检显示真实阶段与成本完整性 | 计数/下一步/利润 null 正确 | 待独立验收 | `evidence/B2/` | PENDING |
| 反向 | 无材料计划不伪装完成 | 显示空态和恢复路径 | 待独立验收 | `evidence/B2/` | PENDING |
| 越权 | 非授权角色/跨租户项目 | 403/404 | Provider 单元测试 PASS；黑盒待验 | `MaterialBlueprintRuntimeTest` | PENDING |
| 幂等 | 只读接口重复刷新 | 结果稳定、无写入 | 本地刷新 PASS；黑盒待验 | `evidence/B2/` | PENDING |
| 边界 | 未定价、部分到场、成本不完整 | 不显示伪利润 | 本地真实数据 PASS；黑盒待验 | `evidence/B2/api-health-summary.json` | PENDING |
| 跨端 | App/Web 同口径 | 字段/状态/下一步一致 | App B3 未完成 | — | PENDING |

## 结论

- 状态：PENDING
- 未覆盖：独立黑盒、跨租户、跨端一致性；
- 遗留风险：App 页面未审、助手指南未验、GAP-MAT-TIME-SEMANTICS；
- 是否允许进入下一批：允许进入 B3 开发；不允许进入 `ACCEPTED`。
