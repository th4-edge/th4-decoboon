# 缺陷台账

| ID | 严重度 | 状态 | 标题 | 角色/端 | 证据 | 业务影响 |
| --- | --- | --- | --- | --- | --- | --- |
| DEF-FIN-001 | P0 | OPEN | App 应收/收款/付款顶部汇总在接口失败时显示 0 | Finance/Boss；App | `raw/notes/app-finance-fin-*.txt`、`raw/api/finance-summary.json` | 有业务数据却显示 0，违反 `BR-055 §4`，会误导回款/付款判断 |
| DEF-FIN-002 | P0 | OPEN_DATA | 台账来源层不守恒：同笔收款双台账、另有付款无台账 | Finance/Boss；Web/App/DB | `raw/sql/accounting-invariants-latest.json` | 台账净现金流多 ¥1,248.73；收付款与台账无法准对账 |
| DEF-FIN-003 | P0 | OPEN_CONTRACT | 手工应收/应付暴露 `BR-055` 禁止类型，保存 Provider 未校验来源/类型 | Finance/Boss；Web/App/Backend | `FinanceReceivableDrawer.vue:42-48`、`FinancePayableDrawer.vue:41-43`、后端 Save 类 | 可形成无来源、错误类型的财务事实 |
| DEF-FIN-004 | P1 | OPEN | 错误码 `undefinedErrorCode` 无法指导恢复 | Boss/Finance/StoreManager；Web/App | summary/project/voucher/expense 响应 | 用户不知道是权限、数据还是系统异常 |
| DEF-FIN-005 | P1 | OPEN | 已收清应收在 Web 仍显示“登记收款”，App 隐藏 | Finance；Web/App | Web 行操作、App `receivables.vue` | 跨端动作不一致；虽前端限超额，但入口误导 |
| DEF-FIN-006 | P1 | OPEN | 店长菜单写“资金台账（只读）”，页面却有手工记账/更正/删除 | StoreManager；Web/App | `store-manager-ledger*.txt`、`app-sm-ledger.txt` | 权限认知错误，误操作风险 |
| DEF-FIN-007 | P1 | OPEN_DATA | 1 笔空门店应收/应付/项目使全公司数与三店合计不一致 | Boss/Finance；Web/App/DB | 应收差 ¥748.73；应付空门店 ¥2,000 | 缺少“未归属门店”核验与迁移处置入口 |
| DEF-FIN-008 | P2 | GAP | Boss `/finance/quotes` 403，Finance 可访问 | Boss/Finance；Web | `boss-finance-quotes.txt` | 需确认 Boss 的报价项目库主入口，不能直接判定缺陷 |

## 复现要点

1. App/Web 使用同一后端和同一数据范围，打开应收、收款、付款页面比较顶部汇总与列表。
2. 打开 `finance/ledger/report` 与收付款源表，执行 `check-accounting-invariants.sh`。
3. Web 打开新增应收/新增应付，展开类型选项并对照 `BR-055`。
4. Web 店长打开台账，比较菜单文案与页面动作。
5. Web 以店长打开应收计划，确认无菜单入口但页面可直达。

## 结论

`DEF-FIN-001~003` 阻断财务闭环验收。开发自测不得以“页面能打开”或“接口有返回”关闭缺陷；必须补负向测试、跨端汇总同源测试、台账来源幂等/唯一性测试和手工类型门禁。
