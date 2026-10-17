# 材料履约与材料业务助手 · 独立验收

## 范围

- Batch：B2/B3/B4/B5 已具备独立验收输入；B6 为独立验收批次；
- 角色：Boss / StoreManager / Finance / Designer / PM；
- 环境：本地 dev / 真实浏览器 / 微信开发者工具 / H5 / Provider 黑盒；
- 数据/租户：造数企业 M，Company / Store / 项目范围；跨企业负向数据另行隔离。

## 用例

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 | 蓝图中心按角色和版本展示 | 阶段/对象/字段/来源同源 | 待独立验收 | `evidence/B2/` | PENDING |
| 正向 | 项目材料体检显示真实阶段与成本完整性 | 计数/下一步/利润 null 正确 | 待独立验收 | `evidence/B2/` | PENDING |
| 反向 | 无材料计划不伪装完成 | 显示空态和恢复路径 | 待独立验收 | `evidence/B2/` | PENDING |
| 越权 | 非授权角色/跨租户项目 | 403/404 | B4 助手跨项目/跨企业 404 PASS；页面黑盒待验 | `evidence/B4/api-samples.json` | PENDING |
| 幂等 | 只读接口重复刷新 | 结果稳定、无写入 | 本地刷新 PASS；黑盒待验 | `evidence/B2/` | PENDING |
| 边界 | 未定价、部分到场、成本不完整 | 不显示伪利润 | B2/B4 真实数据 PASS；黑盒待验 | `evidence/B2/api-health-summary.json`、`evidence/B4/api-samples.json` | PENDING |
| 跨端 | App/Web 同口径 | 字段/状态/下一步一致 | App 页面已渲染；独立黑盒待验 | `evidence/B3/` | PENDING |
| 正向 | App 从作业单提交请购后完成下单 | 采购单/明细、应付、状态正确 | B5 实现/UI PASS；独立黑盒待验 | `evidence/B5/` | PENDING |
| 正向 | App 主材结算与撤销 | 逐单/月结、结算价、成本收敛正确 | B5 实现/UI PASS；独立黑盒待验 | `evidence/B5/` | PENDING |
| 反向 | 旧材料计划直采入口 | `project/purchase/save`、`project/purchase/batch` 与 App 入口不可达 | B5 静态/真实页面 PASS；独立黑盒待验 | `evidence/B5/` | PENDING |

## 结论

- 状态：PENDING
- 未覆盖：独立黑盒、跨租户、跨端一致性；
- B3 已提供 App 项目材料体检、作业单时间/取消上下文、建单来源说明与帮助主题证据；
- B4 已提供 50 个黄金问题、只读助手运行时、权限负向和审计证据；
- B5 实现已完成 App 单端下单/结算与真实 UI 走查，证据见 `evidence/B5/`；开发自测和 UI 走查不构成独立验收；
- 遗留风险：App 真机全链、GAP-MAT-TIME-SEMANTICS；
- 下一批：允许进入 B6 独立验收；本实现线程不得自批 `ACCEPTED`。
