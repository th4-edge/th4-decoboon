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

## 历史输入结论（B2–B5）

- B6 已执行并判定 **FAIL**，实际结果见下方“B6 独立验收执行结果”；
- B3/B4/B5 的运行时、权限负向、UI 走查证据只作为独立验收输入，不构成 `ACCEPTED`；
- 遗留限制：App 真机全链、GAP-MAT-TIME-SEMANTICS；
- 下一批：B6.1 修复后由 B6.2 独立复验；本实现线程不得自批 `ACCEPTED`。


---

## B6 独立验收执行结果（2026-10-17）

| 类别 | PASS | FAIL | 合计 |
| --- | ---: | ---: | ---: |
| 正向 | 17 | 0 | 17 |
| 反向 | 7 | 0 | 7 |
| 越权/权限 | 15 | 3 | 18 |
| 幂等/重复 | 5 | 1 | 6 |
| 边界 | 6 | 2 | 8 |
| 旧路线 | 5 | 0 | 5 |
| 材料助手 | 55 | 0 | 55 |
| 跨端 | 4 | 0 | 4 |
| UI | 7 | 0 | 7 |
| **合计** | **121** | **6** | **127** |

### B6 缺陷

| ID | 级别 | 问题 | 状态 |
| --- | --- | --- | --- |
| B6-DEF-001 | P1 | Designer/PM 可读取作业单成本字段 | OPEN |
| B6-DEF-002 | P1 | Designer/PM 可读取采购金额与供应商 | OPEN |
| B6-DEF-003 | P1 | Designer/PM 可读取结算金额与供应商 | OPEN |
| B6-DEF-004 | P1 | 重复下单生成重复采购单/应付 | OPEN |
| B6-DEF-005 | P1 | 未到场采购明细可结算 | OPEN |
| B6-DEF-006 | P2 | 暂估应付显示 0 且不暴露 estimated | OPEN |

### B6 结论

- 独立验收结论：**FAIL**，不是 `ACCEPTED`；
- 正向主链、旧路线、助手和 UI 已通过，但 5 个 P1 阻断权限/金额/幂等/门禁；
- 证据：`evidence/B6/README.md`、`defects.json`、`blackbox-results.json`、`summary.json`；
- 下一批：`B6.1` 修复缺陷；修复后由 `B6.2` 独立复验，仍不得由实现者自批。


---

## B6.1 修复自测输入（不是独立验收）

| 缺陷 | 自测结果 | 证据 |
| --- | --- | --- |
| B6-DEF-001 作业单成本可见性 | PASS | `evidence/B6.1/defect-verification.json` |
| B6-DEF-002 采购金额/供应商可见性 | PASS | 同上 |
| B6-DEF-003 结算页/详情角色门禁 | PASS | 同上 |
| B6-DEF-004 重复下单/应付 | PASS | 同上 |
| B6-DEF-005 未到场可结算 | PASS | 同上 |
| B6-DEF-006 暂估应付显示 0 | PASS | 同上 |

- 后端全量 871/0/0/0 PASS；App/Web 构建与真实页面自测 PASS；
- 该结果仅用于进入 B6.2 独立复验，不能作为 `ACCEPTED` 依据；
- 临时数据、项目负责人和测试密码哈希已恢复；证据 `evidence/B6.1/`。


---

## B6.2 独立复验执行结果（2026-10-17）

- 环境：后端 `8cc1d11`、App `46a5be4`、Web `04ba3b4`；
- 结果：150 项 **149 PASS / 1 FAIL**；原 B6 六项缺陷 6/6 关闭；
- 新缺陷：`B62-DEF-001` P2 OPEN，多供应商拆单后作业单价格投影仍为 `Unpriced`；
- 采购/应付金额事实、权限、幂等、到场门禁、助手、旧路线、跨端和 UI 通过；
- 证据：`evidence/B6.2/README.md`、`blackbox-results.json`、`defects.json`、`summary.json`、`screenshots/`；
- 结论：不满足 `ACCEPTED`；下一批 B6.3 修复，B6.4 独立复验。


---

## B6.3 修复自测输入（不是独立验收）

- `B62-DEF-001` 自测：Priced 3@100 + Unpriced 2 → Mixed；2 到场结算 120 后仍 Mixed；撤销后仍 Mixed；
- 后端全量 876/0/0/0 PASS；App H5 真实页面显示“部分已结算”；
- 证据：`evidence/B6.3/defect-verification.json`、`screenshots/`；
- 仅用于进入 B6.4 独立复验，不能作为 `ACCEPTED` 依据。
