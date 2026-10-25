# 2026-10-24 DQ-X1 待我谈单查询整改 · 独立验收

> 验收对象：`design-quote / DQ-X1`  
> 结论：**ACCEPTED**  
> 性质：独立只读黑盒验收；未修改产品实现。  
> 环境：本地 dev 后端、MySQL 8.0.24、Web 1440/1280。

## 验收结果

| 验收项 | 结果 |
| --- | --- |
| 独立黑盒检查 | **18/18 PASS** |
| Boss 当前任务 | 22，第一页 20，第三页 0 |
| 店长本店任务 | 22 |
| 设计师本人任务 | 14 |
| 无令牌 | 拒绝 |
| Finance / Marketer / Pm | 403 |
| 其他企业 Boss / 其他企业店长 | 0，无跨企业泄漏 |
| 店长当前页 ID 与数据库门店归属 | 20/20 一致 |
| 设计师当前页 ID 与数据库创建人 | 14/14 一致 |
| `size=500` | 服务端限制为 100 |
| SQL 条数 | 固定 **3 条 SELECT**：count、ID 页、当前页实体批量预取 |
| 逐行客户/房屋查询 | 0 |
| 数据副作用 | quote/customer/property 行数前后均为 `85/73/67` |
| 10 次请求 P95 | **47.4ms** |
| 缺陷 | P0=0，P1=0 |

SQL 证据见 `acceptance-results.json` 中的 `raw.quoteStatements`：

1. `select count(q.quote_id) ...`；
2. `select q.quote_id ... limit 20`；
3. `select q1_0... from quote q1_0 where quote_id in (...)`，客户和房屋通过 EntityGraph 一次带回。

不存在按当前页 20 条逐行查询 Customer/Property 的 N+1 模式。

## 页面证据

独立验收复核了开发侧真实页面证据，并归档副本：

- `screenshots/designer-quotes-1440.png`：设计师 14 条；
- `screenshots/boss-quotes-1440.png`：Boss 22 条、2 页；
- `screenshots/boss-quotes-page2-1440.png`、`boss-quotes-page2-1280.png`：翻页后总数不变；
- `screenshots/store-manager-quotes-1440.png`：店长本店 22 条；
- `screenshots/finance-forbidden-1440.png`：Finance 直链进入 `/forbidden`。

## 构建与回归

- 后端干净全量：**1098/1098 PASS**，0 failure / 0 error / 0 skipped。
- Web build、分页门禁、类型检查、客户端文案门禁：PASS。
- DQ-X1 无 DDL、无数据迁移。

## 结论

DQ-X1 的数据库分页、固定 SQL 结构、无 N+1、四角色数据范围、跨企业隔离、无令牌拒绝、页大小上限、只读副作用和页面证据均通过独立验收。状态可标记 `ACCEPTED`。

不包含小程序上传、正式提审或生产发布；生产发布需用户明确确认。
