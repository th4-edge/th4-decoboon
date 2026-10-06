# 认证态 BOSS 性能复测（App 首页 / 财务工作台）

```text
AUDIT-ID: AUD-20261007-002
日期: 2026-10-07 04:14–04:17 (+0800)
环境: 生产 https://decoboon.joytsing.com；后端 release 20261007-035423（锁迁移 Batch 7 收口）
租户/角色: 麦鸿装饰 / 企业负责人（BOSS）
数据量: 5 项目、5 合同、47 合同行、47 报价行、550 商品、25 应收、11 收款、76 流水
流量: 用户手机真实 BOSS 会话；BOSS 首页 ⇄ 财务工作台 切换/下拉刷新约 20+20 轮
样本: 每个核心读取 API n=36–40（达标 ≥20）
方法: nginx 临时计时日志 rt/urt/uct/uht（已备份、采完即还原）；MySQL `Questions` 前后差算 SQL
预算依据: DEC-027 / BR-025 / MET-008 / MET-009
```

## 1. 结论：**BLOCK**

三重不达标，且均在生产、真实 BOSS 会话、样本充足（n≈40）下复现：

1. **核心读取 API 耗时**：8 个核心接口里 **6 个 P95 > 500ms**，`material/plan/workbench` P99 = 1.071s **> 1000ms**；
2. **关键路径请求数**：BOSS 首页 **8 个 API/次加载**、财务工作台 **9 个**，超「关键路径请求 ≤5」；
3. **SQL 条数**：窗口内 SQL 14,886 条 / 40 轮加载 ≈ **371 条/轮**（≈44 条/接口），超「核心读取 API SQL ≤30」。

`rt ≈ urt`、`uct ≈ 0` → 耗时全部发生在后端应用，不是 nginx/网络/客户端。

## 2. API 耗时（urt=后端时间，秒）

| 接口 | n | P50 | P95 | P99 | max | avgKB | 预算判定 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| `material/plan/workbench` | 40 | 0.568 | **0.922** | **1.071** | **1.071** | 1.2 | **BLOCK**（P95/P99 双超） |
| `statistic/boss/workbench` | 40 | 0.523 | **0.766** | 0.921 | 0.921 | 3.8 | **BLOCK** |
| `statistic/success/report` | 40 | 0.372 | **0.630** | 0.653 | 0.653 | 1.3 | **BLOCK** |
| `statistic/alert/report` | 40 | 0.312 | **0.613** | 0.656 | 0.656 | 2.4 | **BLOCK** |
| `statistic/onboarding/report` | 40 | 0.325 | **0.586** | 0.614 | 0.614 | 1.1 | **BLOCK** |
| `finance/summary/report` | 36 | 0.272 | **0.538** | 0.901 | 0.901 | 6.0 | **BLOCK**（临界） |
| `collab/notice/count` | 40 | 0.132 | 0.227 | 0.428 | 0.428 | 0.1 | PASS |
| `carryover/batch/info` | 40 | 0.059 | 0.125 | 0.347 | 0.347 | 0.2 | PASS |
| `file/private/download` | 34 | 0.034 | 0.062 | 0.108 | 0.108 | 4.9 | PASS |

- max ≤2000ms：全部通过（最差 1.071s）。
- 原始证据：`raw/nginx/window-20261007-0414-0417.log`，解析见 `raw/nginx/parsed-window.txt`。

## 3. 关键路径与页面（API_PROXY）

- **BOSS 首页 = 8 个 API/次加载**：`statistic/boss/workbench`、`material/plan/workbench`、`statistic/alert/report`、`statistic/onboarding/report`、`statistic/success/report`、`collab/notice/count`、`carryover/batch/info`、`file/private/download`；
- **财务工作台 = 9 个 API**（首页 8 个 + `finance/summary/report`）；
- 轮内串并行后「最长接口」P50 ≈ 0.53s、max ≈ 1.071s，加网络+渲染后首屏可用性估算仍在 2s 预算内（**属 API 代理估算，非真实 trace**）；
- `burst-analysis.txt` 给出 38 个 ≥4 请求的加载突发与接口集合。

> 注：线上请求集里 **`statistic/alert/report` 仍是独立请求**，与 P0「首页预警合并进 `boss/workbench`」的预期不一致，需确认手机端 App 版本（≥1.0.23 才含合并）。

## 4. SQL 条数（MySQL `Questions` 前后差）

| 项 | 值 |
| --- | ---: |
| 窗口 SQL 总数（含后台） | 14,886 |
| 后台基线（0.23 条/秒 × 178s） | ≈ 41 |
| 归因 App 流量 | ≈ 14,845 |
| 加载轮次 | 40 |
| **每轮加载 SQL** | **≈ 371 条** |
| 每接口均值（8–9 接口） | **≈ 44 条** |

- 生产未开启 Hibernate SQL 计数，故本轮为**整轮聚合**；逐接口精确值已于 2026-10-07 04:24 用一次性 BOSS token 补齐，见 **`02-SQL归因与并发验证.md`**：`finance/summary/report` 106 条、`material/plan/workbench` 69、`statistic/boss/workbench` 67、`success/report` 57、`alert/report` 46 → **5 个核心接口超 ≤30**；并发放大验证复现 P95 0.49–0.63s，根因为「查询条数超标 × 页面级 8–9 并发」。

## 5. 与上一轮（P0/P1 前基线）对比

| 接口 | 基线 P95 | 本次 P95 | 变化 |
| --- | ---: | ---: | --- |
| `material/plan/workbench` | 0.804s | 0.922s | 变慢 |
| `statistic/boss/workbench` | 0.684s | 0.766s | 变慢 |
| `finance/summary/report` | 0.305s | 0.538s | 变慢 |
| `statistic/alert/report` | 0.393s | 0.613s | 变慢 |

- 两次窗口不同（基线为混合真实流量、本次为定向 40 轮），不能直接归因；但**没有任何接口变好**，说明 P0/P1 的四项改动未在本场景体现收益，真实瓶颈仍在读路径的查询条数。

## 6. 建议（按优先级）

1. **P0｜`material/plan/workbench`**：P95 0.92s / P99 1.07s 全场最慢，先做该接口的 SQL 条数与慢查询归因（材料计划/供应商/到货链路）；
2. **P0｜`statistic/boss/workbench` + `onboarding/success report`**：三个报表接口 P95 0.58–0.77s，疑似逐项查询，落到读模型/单条聚合 SQL；
3. **P0｜关键路径请求 8→≤5**：确认 App 版本是否含 P0 合并；若已含则合并未生效，需回查；
4. **P1｜`finance/summary/report`** P99 0.901s 逼近 P99 预算，账本聚合需索引/汇总表；
5. **P1｜SQL 条数治理**：371 条/轮是量级问题，建议加「每接口 SQL 条数」常量断言与 CI 门禁。

## 7. 方法与合规

- nginx 临时计时配置：备份 → 启用 → 采集 → **已还原**（备份 `/root/nginx-perf-stamp.txt`，stamp `perf-20261007-040059`），还原后 `nginx -t` 通过、`systemctl reload nginx`、就绪探针 200；
- 未修改任何业务代码、数据库、权限、金额或业务口径；
- 未记录 token、Cookie、手机号或完整业务数据。

## 8. 续篇

- **`02-SQL归因与并发验证.md`**：逐接口 SQL 条数、串行 vs 并发对比、根因链与整改建议（P0/P1 可验收项）。

## 9. 未覆盖 / 待补

- App 真实首屏 trace（认证态需要用户侧开发者工具）；本轮以 API 代理估算；
- Web 财务页认证态采样；
- 多租户 10× 数据量与并发（`GAP-025` 必测项）。
