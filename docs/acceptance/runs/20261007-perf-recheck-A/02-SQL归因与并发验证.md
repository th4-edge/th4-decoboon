# 附篇：逐接口 SQL 条数归因与并发放大验证

```text
AUDIT-ID: AUD-20261007-002-B
日期: 2026-10-07 04:24–04:35 (+0800)
环境: 生产 https://decoboon.joytsing.com；后端 release 20261007-035423
身份: 麦鸿装饰 / 企业负责人（BOSS，emp 1789938943545297099）
方法: 一次性 BOSS token（仅内存使用，测完即删；未写入仓库/报告/日志）
     逐接口 SQL = 单请求前后 MySQL `Questions` 差值 - 1（自身 SHOW）
     并发验证 = 用 8–9 个接口同时发起，复刻 App 页面加载模式
```

## 1. 逐接口 SQL 条数（每接口 3 次，取中位数）

| 接口 | SQL(中位) | 波动 | 串行耗时(中位) | 响应大小 | 预算 ≤30 |
| --- | ---: | ---: | ---: | ---: | --- |
| `finance/summary/report` | **106** | 106–107 | 234ms | 47.0 KB | **BLOCK** |
| `material/plan/workbench` | **69** | 68–69 | 201ms | 9.7 KB | **BLOCK** |
| `statistic/boss/workbench` | **67** | 67–73 | 221ms | 14.8 KB | **BLOCK** |
| `statistic/success/report` | **57** | 57 | 145ms | 3.3 KB | **BLOCK** |
| `statistic/alert/report` | **46** | 46 | 136ms | 10.0 KB | **BLOCK** |
| `statistic/onboarding/report` | 27 | 27 | 110ms | 2.3 KB | PASS |
| `collab/notice/count` | 15 | 14–15 | 50ms | 27 B | PASS |
| `carryover/batch/info` | 11 | 11 | 35ms | 202 B | PASS |
| `statistic/metric/dictionary` | 7 | 7 | 48ms | 14.0 KB | PASS |
| `auth/session/info`（对照） | 7 | 7 | 43ms | 927 B | 对照 |

- **5 个核心读取接口超出 SQL ≤30**，最高 `finance/summary/report` **106 条**（超标 3.5 倍）。
- 单次页面加载 SQL：**BOSS 首页 ≈ 293 条**（8 接口求和）；**财务工作台 ≈ 399 条**（+106）。
  与 04:14–04:17 真实流量实测「≈371 条/轮」一致（该窗口为首页/财务交替）。

## 2. 并发放大验证（复刻 App 一次加载同时打 8–9 个接口）

| 接口 | 串行(单发) | 并发 P50 | 并发 P95 | 并发 max | 放大 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `material/plan/workbench` | 201ms | 501ms | **633ms** | 633ms | **2.5×** |
| `statistic/success/report` | 145ms | 380ms | **549ms** | 549ms | 2.6× |
| `statistic/onboarding/report` | 110ms | 330ms | **528ms** | 528ms | 3.0× |
| `finance/summary/report` | 234ms | 477ms | **528ms** | 528ms | 2.0× |
| `statistic/boss/workbench` | 221ms | 395ms | **506ms** | 506ms | 1.8× |
| `statistic/alert/report` | 136ms | 332ms | 493ms | 493ms | 2.5× |
| `statistic/metric/dictionary` | 48ms | 97ms | 347ms | 347ms | — |
| `collab/notice/count` | 50ms | 158ms | 328ms | 328ms | — |
| `carryover/batch/info` | 35ms | 104ms | 315ms | 315ms | — |

- 10 轮并发加载整轮墙钟：**352–633ms**；
- 真实手机端 20+20 轮连续刷新的 P95 更高（`material/plan` 922ms、`boss/workbench` 766ms），因为连续下拉会让**相邻两轮突发互相重叠**。

## 3. 根因链（证据闭环）

1. **查询量级过大**：核心读接口单次 46–106 条 SQL（预算 ≤30）；
2. **App 一次加载并发打 8–9 个接口** → 单次加载 ~293–399 条 SQL；
3. **Hikari 连接池 `maximum-pool-size=10`**（`application.yml`），400 条查询挤在 10 条连接上 + 应用线程争用；
4. → 串行 200ms 的接口在并发下变成 500–900ms，**P95 冲破 500ms 预算**；
5. `rt ≈ urt`、`uct ≈ 0`（前篇）→ 全部发生在后端应用，与 nginx/网络无关。

**结论**：本轮 BLOCK 的根因不是「某个接口算法慢」，而是**单接口查询条数超标 × 页面级并发**共同作用；只优化单个接口无法达标，必须同时降查询条数与降低首屏并发请求数。

## 4. 整改建议（可验收）

| 优先级 | 动作 | 目标 | 验收 |
| --- | --- | --- | --- |
| P0 | `finance/summary/report` 从 106 → ≤30：账本/应收/应付/流水改一次聚合 SQL 或汇总表 | SQL ≤30 | 单请求 Questions 差值 ≤30 |
| P0 | `material/plan/workbench`（69）与 `statistic/boss/workbench`（67）合并逐项查询为批量/投影查询 | SQL ≤30，P95 串行 ≤150ms | 并发 P95 ≤500ms |
| P0 | `statistic/success/report`（57）、`alert/report`（46）同样批量化 | SQL ≤30 | 同上 |
| P0 | 首屏关键路径 8→≤5：确认 App 版本；把 `alert`、`onboarding`、`success` 合并进 `boss/workbench` 一次返回 | 请求数 ≤5 | 抓包/日志确认单次加载接口数 |
| P1 | 复核 Hikari `maximum-pool-size=10` 是否匹配 8–9 并发聚合读 | 无连接等待 | 池等待时长指标 |
| P1 | CI 增加「每接口 SQL 条数」断言 + 页面级关键路径请求数断言 | 防回归 | 门禁拦截 |

## 5. 合规

- token 仅在本机内存与一次性临时文件（600 权限）中使用，报告/仓库/日志中**不含 token**；测完已删除本地副本；
- 全部为只读查询接口，未做任何写操作；
- 未重启后端、未改生产配置；nginx 计时配置此前已还原。
