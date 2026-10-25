# 2026-10-23 设计师工作队列 200+ 分页与 Dormant 排除复核

> 状态：`UI_REVIEWED / USER_WEB_ACCEPTANCE_PENDING`  
> 性质：研发侧真实 Web 复核，等待用户本人登录验收；不是独立验收。  
> 环境：本地 dev，`http://127.0.0.1:8080`。

## 结论

| 检查项 | 结果 |
| --- | --- |
| 待我谈单使用服务端分页 | PASS：`Page<QuoteWorkbenchItem>`，每页 20 条 |
| 待我谈单 200+ | PASS：总计 214 条，11 页，第 2 页可正常读取 |
| 待我谈单筛选 | PASS：`filter=draft` 回源服务端，75 条且全部为草稿 |
| 待我签约使用服务端分页 | PASS：`Page<SignWorkbenchItem>`，每页 20 条 |
| 待我签约 200+ | PASS：总计 214 条，11 页 |
| 待我签约筛选 | PASS：`filter=toCreate` 返回 200 条，10 页 |
| Dormant 待谈单排除 | PASS：搜索 `TMP-DORMANT-QUEUE` 返回 0，页面不显示 |
| Dormant 待签约排除 | PASS：搜索 `TMP-DORMANT-SIGN` 返回 0，页面不显示 |
| 1440/1024 | PASS：两页均无横向溢出 |
| Console | PASS：0 error，1 条既有 warning |

## 契约变更

- `statistic/workbench/quote/page`：`List<QuoteWorkbenchItem>` → `Page<QuoteWorkbenchItem>`。
- `statistic/workbench/sign/page`：`List<SignWorkbenchItem>` → `Page<SignWorkbenchItem>`。
- 两个 Provider 支持 `current / size / keyword / filter`；签约队列额外支持 `quoteId` 定位深链所在页。
- App 与 Web 同步改为消费 Page；不存在客户端 200 条截断或只取第一页伪装全量。
- Dormant 在组装队列项时排除，排除发生在分页切片之前，因此不会进入总数、筛选数、页码或 DOM。

## 测试

```text
mvn -Dtest=DesignerWorkbenchAggregationTest,DesignerWorkbenchPaginationTest,DesignerWorkbenchRiskTest test
=> 11/11 PASS

Web npm run build + actions/permissions/workbenches/workspaces => PASS
App npm run type-check + build:mp-weixin + action/contract-sign/navigation => PASS
```

## 本地验收数据

本地 dev 库当前保留临时验收数据：

- 待谈单：214 条，11 页；
- 待签约：214 条，11 页；
- Dormant 待谈单标记：`TMP-DORMANT-QUEUE`；
- Dormant 待签约标记：`TMP-DORMANT-SIGN`；
- 临时数据清理脚本：`~/.decoboon-local/cleanup-designer-pagination-acceptance-20261023.sql`。

验收结束后再执行清理，清理前不触碰原始报价和客户数据。

## 截图

- `output/playwright/20261023-designer-web-acceptance/designer-quotes-pagination-214-1440.png`
- `output/playwright/20261023-designer-web-acceptance/designer-quotes-pagination-214-1024.png`
- `output/playwright/20261023-designer-web-acceptance/designer-contract-pagination-214-1440.png`
- `output/playwright/20261023-designer-web-acceptance/designer-contract-pagination-200-1024.png`

## 菜单导航修复复核

- `RoleSideNav` 只为可切换门店的角色附带 `storeId`；Designer 为固定 Self 数据范围，不再写入 `storeId=0`。
- 18 个 Designer 菜单链接均不含 `storeId`。
- 跨页点击 `/designer/quotes` → `/designer/quote-history`：URL 无 `storeId`，SPA marker 保留。
- 当前页重复点击「报价历史」：页面 DOM marker 保留，`performance.getEntriesByType('navigation').length === 1`，无整页刷新。
- console 0 error。

证据：`navigation-checks.json`。
