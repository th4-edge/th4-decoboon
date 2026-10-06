# 装得清 App + 后端性能审核报告（首份）

```text
AUDIT-ID: AUD-20261007-001
日期: 2026-10-07
环境/版本: 生产 https://decoboon.joytsing.com；后端 release 20261007-021158（P1）
租户/角色: 麦鸿装饰 / BOSS（App 真实流量）
数据量: 5 项目、5 合同、47 合同行、47 报价行、550 商品、25 应收、11 收款、76 流水
并发: 单租户真实 App 流量；多租户 10× 数据量暂未验证
审核人: zhuangdeqing-performance-audit（独立只读）
预算依据: DEC-027 / BR-025 / MET-008 / MET-009；锁与 SQL: DEC-029 / BR-027
```

## 1. 结论

- **结论：BLOCK（基线）→ CONDITIONAL（P1 代码已上线，待复测）**
- 阻断项：
  1. P0/P1 前实测 `material/plan/workbench` P95 0.804s、`statistic/boss/workbench` P95 0.684s，超过核心读取 API P95 ≤500ms；
  2. ~~仓库历史上 12 个含 `@Lock(PESSIMISTIC_*)` 的仓储文件、17 处数据库锁用法~~ —— **Batch 7 已收口解除**：`src/main` 与生产 JAR 均无 `@Lock(PESSIMISTIC_*)`（扫描命中 0 个 class），符合 `DEC-029/BR-027`；
  3. P1 后认证态 App/Web 页面级复测尚未执行。
- 已确认改善：P0/P1 已上线 `default_batch_fetch_size=50`、主查询 `@EntityGraph`、材料计划工作台 `IN` 批量查询、App 1.0.24 缓存优先 + onboarding/success 分阶段加载；是否 PASS 必须复测后判定。
- 审核方不得自我豁免：上述阻断项必须由用户书面豁免或完成整改后复测。

## 2. API 耗时（P0/P1 前真实 App 流量，88 次）

| 接口 | 采样 | 平均 | P95 | max | 预算判定 |
| --- | ---: | ---: | ---: | ---: | --- |
| `material/plan/workbench` | 8 | 0.564s | 0.804s | 0.804s | **BLOCK** |
| `statistic/boss/workbench` | 9 | 0.477s | 0.684s | 0.684s | **BLOCK** |
| `statistic/onboarding/report` | 9 | 0.355s | 0.545s | 0.545s | BLOCK（>0.5s max） |
| `statistic/success/report` | 9 | 0.352s | 0.530s | 0.530s | BLOCK（>0.5s max） |
| `statistic/alert/report` | 9 | 0.274s | 0.393s | 0.393s | PASS |
| `collab/notice/count` | 9 | 0.146s | 0.343s | 0.343s | PASS |
| `finance/summary/report` | 8 | 0.280s | 0.305s | 0.305s | PASS |
| `carryover/batch/info` | 9 | 0.096s | 0.142s | 0.142s | PASS |
| `statistic/metric/dictionary` | 8 | 0.028s | 0.032s | 0.032s | PASS |
| `auth/session/info` | 2 | 0.028s | 0.048s | 0.048s | PASS |

- `rt ≈ urt`、`uct ≈ 0`：耗时在后端应用，不是 nginx/网络。
- 财务工作台接口本身达标；BOSS 首页被 material plan、boss workbench、onboarding、success 拖慢。

## 3. 页面加载

| 页面 | TTFB | 总耗时 | HTTP | 判定 |
| --- | ---: | ---: | ---: | --- |
| Web `/` | 0.102s | 0.102s | 200 | 公开入口 PASS |
| Web `/login` | 0.060s | 0.060s | 200 | 公开入口 PASS |
| Web `/v3/` | 0.066s | 0.067s | 200 | 公开入口 PASS |
| App BOSS 首页 | — | — | — | **未测（需真实 BOSS 会话 trace）** |
| App 财务工作台 | — | — | — | **未测（需真实 BOSS 会话 trace）** |

## 4. 锁与 SQL 审核（DEC-029 / BR-027）

- 迁移前全仓扫描：`@Lock(PESSIMISTIC_*)` / `FOR UPDATE` / `LOCK IN SHARE MODE` 共 **17 处，12 个文件**；截至 **Batch 7 收口**后实测 `grep -rn "@Lock" src/main` 为 **0 处**、生产 JAR 扫描 `LockModeType`/`PESSIMISTIC` 命中 **0 个 class**；
- **Batch 1 已完成**：`CatalogImportBatchRepository` 数据库锁移除，确认导入/撤销改用 `DistributedLock`（生产 `RedissonLock`，测试禁用时 no-op）；release `20261007-022508`，commit `60fabbe`；
- **Batch 2 已完成**：`CarryoverProjectRepository` 两个数据库锁方法移除，`CarryoverApplyContext`、`CarryoverProjectContext`、`CarryoverAdjustmentContext`、`CarryoverLockContext` 改用 `DistributedLock`；release `20261007-023421`，commit `d952c40`；
- **Batch 3 已完成**：`ProjectStageRepository`、`AcceptanceRepository` 数据库锁移除，两个 Provider 与 `ProjectContext.submitAcceptance` 改用分布式锁；release `20261007-025828`，commit `95f380d`；
- **Batch 4 已完成**：`CustomerRepository`、`PropertyRepository` 的公共 finder 移除 `@Lock(PESSIMISTIC_WRITE)`；报价确认/签约建合同/立项改用统一房屋锁 `property:{comId}:{propertyId}`，客户删除改用 `customer:{comId}:{custId}`；release `20261007-032237`，commit `7fb7a42`；
- **Batch 5 已完成**：`CustomerInviteRepository`、`CustomerAccessRepository` 数据库锁移除；`CustomerInviteContext.accept/cancel` 改为「邀请 + 微信身份×项目」双锁（`@DistributedLocked.extraKeys`）；release `20261007-033446`，commit `b6dda62`；
- **Batch 6 已完成**：`AiResourceAccountRepository` 数据库锁移除；`AiBillingContext` 的 account/grant/reserve/settle/release/refund/adjust 统一按 `ai-resource-account:{comId}` 分布式锁串行；release `20261007-034157`，commit `1511b0b`；
- **Batch 7 已完成（用户确认后发布）**：`CredentialsRepository`、`SubscriptionRepository`、`OrderRepository` 数据库锁移除；认证凭证按 `credential:{creId}` / `credential-phone:{phone}`、支付履约按 `order:{orderNo}`+`subscription:{comId}`、订阅额度与平台调整订阅按 `subscription:{comId|subId}` 串行；release `20261007-035423`，commit `0c49eaa`；
- 结论：按 `DEC-029/BR-027`，**数据库锁迁移 1–7 全部完成，该项 BLOCK 解除**；业务并发一律走 `RedissonLock`；
- 自定义 SQL 例外已批准：框架 API 低效时允许 `@Query`/nativeQuery/JDBC/批量 `IN`/投影/`@EntityGraph`，但必须保留租户/数据权限、参数绑定与金额口径。

## 5. 慢点与根因

- `ProjectCostContext` 逐行懒加载 `sourceQuoteLine`/`CatalogItem`（P0 已加批量抓取，P1 已加主查询 EntityGraph）；
- `MaterialPlanWorkbenchContext` 原逐项目查询材料计划（P1 已改 `findByProject_ProjectIdIn` 批量）；
- `statistic/boss/workbench` 同时做 dashboard + alerts + events，App 首页原先又单独请求 alerts（P0 已合并 alertReport）；
- `onboarding/report`、`success/report` 各自多 Repository 逐项查询（P1 已改为分阶段加载，未合并）；
- App 首页关键路径请求由 5 个降为 4 个，并加入缓存优先（P1）。

## 6. 建议与复测

1. 用真实 BOSS 会话复测 App 首页、财务工作台、Web 财务页：≥20 次采样，输出 P50/P95/P99、SQL 条数、页面瀑布；
2. ~~继续迁移剩余数据库锁~~ —— Batch 1–7 已全部完成；后续新增并发写路径必须默认用 `RedissonLock` / `@DistributedLocked`，并提供 key/wait/lease/幂等/解锁证据，禁止回退到数据库锁；
3. 若复测仍超预算，P2：`findAll(Specification)` EntityGraph/投影、onboarding/success 合并读模型、请求耗时 + SQL 条数常驻监控、报表分页/异步；
4. 多租户 10× 数据量与并发验证未执行，属于必测项。

## 7. 证据

- nginx 计时日志：`/var/log/nginx/api-timed.log`（99+ 条，01:36–01:38）
- 生产 release：`20261007-021158`（P1）、`20261007-015056`（P0）
- App：`1.0.24`（cache-first/staged loading）、`1.0.23`（首页预警合并）
- 数据库锁扫描：见 `rules.md` §0.3 与 `GAP-025`
- 规范：`DEC-027`、`BR-025`、`MET-008`、`MET-009`、`DEC-029`、`BR-027`
