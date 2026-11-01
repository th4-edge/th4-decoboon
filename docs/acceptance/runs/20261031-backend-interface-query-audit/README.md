# 后端接口查询与业务编排问题盘点

```text
AUDIT-ID: AUD-20261031-001
日期: 2026-10-31
范围: th4-decoboon src/main/java
状态: AUDIT_ONLY_NOT_ACCEPTANCE
规范: docs/design/34-后端接口查询与业务编排强制规范.md
```

## 1. 目的

梳理后端对外 Provider 中反复出现的 N+1 SQL、JPA 全量加载后内存统计、固定大页冒充全量、循环查库和复杂接口编排问题，为存量整改排序和新增接口审查提供基线。

本盘点不修改生产代码、不改变业务事实、不宣布性能验收通过。

## 2. 静态盘点结果

| 检查 | 结果 |
| --- | ---: |
| `@Service` Provider 实现 | 543 |
| `/page` 能力 | 117 |
| `/report` 能力 | 44 |
| `/list` 能力 | 21 |
| `findAll(` | 64 处 / 53 文件 |
| `PageRequest.of(` | 96 处 / 86 文件 |
| `subList(` | 40 处 / 38 文件 |
| `/page` 或 `/range` 中使用 `subList` | 33 文件 |
| `size` 硬上限大于 50 | 68 文件 |
| 固定 `PageRequest.of(0, >=100)` | 5 处 / 4 文件 |
| 循环体或 stream lambda 内仓储调用候选 | 7 文件 |

说明：

- `findAll`、`PageRequest.of`、`subList` 的命中必须结合数据增长和业务上限分类，不能直接等同于违规；
- 固定枚举、单张单据有限配置、已限量的批量动作可能合理；
- `size > 50` 中部分可能是存量设置，当前 33 号规范已要求普通列表和选择器不超过 50；
- 循环仓储候选包含写前逐行校验，不只代表查询接口。

## 3. 已确认高风险路由

| 路由 | 证据位置 | 问题 | 影响 |
| --- | --- | --- | --- |
| `customer/report/page` | `customer/service/report/page/support/Page.java` | 遍历可访问项目逐项目查 200 条动态，再全局排序和 `subList` | N+1、截断、分页总数失真 |
| `project/report/page` | `project/service/reports/page/support/Page.java` | 先查可见项目，再固定取 500 条动态后 Java 过滤、`subList` | 超过 500 条漏数、筛选和分页不正确 |
| `project/report/list` | `project/service/reports/list/support/List.java` | 固定取 500 条动态返回 | 数据增长后静默截断 |
| `project/acceptance/page` | `project/service/acceptances/page/support/Page.java` | 固定取前 100 条，未形成真实 `current/size` 分页 | 验收历史增长后不可追溯 |
| `project/purchase/page` | `project/service/purchases/page/support/Page.java` | 全量项目后逐项目查采购单，再全量装配明细，无真实分页 | `1 + N` SQL，响应随企业项目量增长 |
| `project/material-plan/page` | `project/service/materialplans/page/support/Page.java` | 全量项目后逐项目查材料计划，返回全量 List Page | `1 + N` SQL，无真实分页 |
| `payroll/workbench/report` | `payroll/context/PayrollWorkbenchContext.java` | 原为 5 类实体全量加载后 Java 计数/求和；`PAY-OPS-01` 已改为 6 条聚合/投影查询 | FIXED / DEV_VERIFIED：实体加载 22→2，10× SQL/实体加载不增长；真实页面和生产 P95 待验收 |
| `finance/summary/report` 等财务汇总 | `finance/context/FinanceReportContext.java` | 多类财务实体 `findAll` 后流式汇总，已在静态门禁 A1 基线 | 财务数据量增长后接口不可控 |

## 4. 循环/流式仓储调用候选

| 路由 | 位置 | 判断 |
| --- | --- | --- |
| `customer/customer/handoff` | `customer/service/cust/handoff/support/Handoff.java:139` | 逐房屋查询量房并逐条保存，写前应批量加载 |
| `customer/customer-follow/save` | `customer/service/follows/save/support/Save.java:204` | 逐图片 `exists`，应批量查重 |
| `design/template/items/batchsave` | `designtemplate/service/templates/items/batchsave/support/BatchSave.java:168` | 逐行查报价项目库，属批量保存 N+1 |
| `payroll/sheet/delete` | `payroll/service/sheets/delete/support/Delete.java:72` | 逐工资项查应付，应一次批量查重 |
| `project/purchase/page` | `project/service/purchases/page/support/Page.java:48` | 项目流中逐项目查采购单，已确认高风险 |
| `project/material-plan/page` | `project/service/materialplans/page/support/Page.java:66` | 项目流中逐项目查材料计划，已确认高风险 |
| `design/items/save` | `design/service/items/batchsave/support/BatchSave.java:136` | 逐分项查归属和报价快照引用，应批量校验 |

## 5. `size` 上限大于 50 的典型路由

扫描得到 68 个文件命中，包括：

- `customer/customer/page`
- `project/project/page`
- `project/report/page`
- `project/purchase/page`
- `payroll/item/page`
- `payroll/commission/page`
- `finance/receivable/page`
- `finance/payable/page`
- `finance/payment/page`
- `finance/ledger/page`

这些接口必须逐项分类为 `PAGE_LIST`、`PICKER_PAGE` 或 `BOUNDED_LIST`。不能因为前端历史调用传入大值就把服务端上限继续放大。

## 6. 复杂业务编排问题

当前复杂接口主要表现为：

1. 一个 Provider 同时处理角色分支、项目范围、筛选、排序、分页和多类关联装配；
2. 工作台在一次请求中全量读取多个实体集合并计算多个指标；
3. 写接口中逐行加载和校验，导致批量动作实际是 N 次查询/N 次写入；
4. 汇总接口没有复用聚合层，或顺序调用多个重型汇总接口；
5. 详情页承载持续增长的历史明细、附件或日志。

这些问题不只是“慢”，还会导致：

- 新增一个筛选条件需要同时修改 Java 过滤、计数、排序和权限；
- 同一个金额/状态在列表、汇总和详情中口径不一致；
- 某个子模块失败时整页失败或伪 0；
- 无法用单一查询链和单一验收用例证明正确性。

## 7. 根因

| 根因 | 表现 |
| --- | --- |
| 实体先行而非场景先行 | 从表和关联直接生成接口，默认返回完整对象 |
| 接口边界按页面而非用户目标 | 一个接口同时服务浏览、统计、详情、导出 |
| 数据增长未建模 | 默认几十条，忽略 10× 和企业级增长 |
| JPA 便利性被当作性能方案 | `findAll`、LAZY 关联、Java stream 汇总 |
| 缺少查询链评审 | 编码后才发现 N+1 和固定大页 |
| 只看 SQL 条数 | SQL 下降但 P95 和响应体未改善 |

## 8. 整改顺序

1. 财务、薪酬、金额、成本、利润、应收应付和经营报表；
2. 已确认的 8 个高风险路由；
3. 33 个 `subList` 分页/范围路由和 68 个 `size > 50` 上限；
4. 7 个循环/流式仓储调用候选；
5. 其余详情、选择器、日志和历史明细。

每一批必须完成：

- 场景卡和接口分类；
- 数据库分页/聚合查询；
- 同筛选 `totalElements == summary.count` 对账；
- 权限负向用例；
- 10× 数据量和 P95/响应体证据；
- App/Web 真实页面回读；
- 变更记录和回滚说明。

## 9. 关联规范

- `rules.md` §0.1、§0.3、§0.4、§0.5、§0.6
- `docs/design/31-一后端多端同源架构规范.md`
- `docs/design/32-Provider接口响应序列化与懒加载安全规范.md`
- `docs/design/33-后端分页与聚合接口强制规范.md`
- `docs/design/34-后端接口查询与业务编排强制规范.md`
- `docs/acceptance/runs/20261007-perf-retrospective/性能定位复盘.md`
