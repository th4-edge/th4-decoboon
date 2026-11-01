# 体验日志

| step-id | 时间 | 角色 | 端面/尺寸 | 页面路径 | 操作与观察 | 控制台/网络异常 | 证据 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S10-finance-001 | 22:19 | Finance | Web 963px | `/finance/workbench` | 首轮财务列表请求失败、无数据；定位为关联白名单截断 | 请求失败，无合法 JSON | EV-002/012 |
| S10-finance-002 | 22:23 | Finance | Web 963px | `/finance/workbench`、应收/收款/应付/付款/台账 | 修复后真实金额与行列恢复；已收清应收不显示登记收款 | 无请求失败 | EV-002/013 |
| S12-boss-001 | 22:24 | Boss | Web 963px | `/finance/workbench` | 合同 ¥2,495.75、应收 ¥3,495.75、待收 ¥2,747.02、待付 ¥1,500 | 无 | EV-002 |
| S13-store-001 | 22:23 | StoreManager | Web 963px | `/finance/ledger` | 4 条本店台账，净现金流 -¥851.27；菜单为“资金台账”，不再标“只读” | 无 | EV-009 |
| S13-store-002 | 22:33 | StoreManager | Web 963px | `/workbench/customers` | 顾客 1、负责人分组、面积 100㎡、量房/设计/合同/项目链可见 | 无 | EV-008 |
| S13-store-003 | 22:25 | StoreManager | 微信开发者工具 | `pages/index/index` | 修复前“待开工 0”；修复后待开工 1，项目卡真实 | 修复前非法 JSON | EV-007/014 |
| S11-finance-app-001 | 22:32 | Finance | 微信开发者工具 | `pages/finance/index` | 净现金流 -¥851，待收 ¥2,747，待付 ¥1,500，台账 4 条 | 无 | EV-003 |
| S10-project-001 | 22:32 | Finance | 微信开发者工具 | `subpackages/finance/pages/project` | 项目成本 ¥800、成本未完整、利润不展示；付款进度 25% | 无 | EV-004 |
| S12-boss-app-001 | 22:31 | Boss | 微信开发者工具 | `pages/index/index` | 签约 ¥2,496、回款 ¥749、在建 1；项目事件 6 条 | 无 | EV-005/015 |
| S20-attack-001 | 22:35 | StoreManager | API envelope | `statistic/store/report`、`finance/summary/report` | 传滨湖店 `storeId` 均返回 `403 门店不存在或无权访问` | 预期拒绝 | EV-016 |

## 体验结论

- 财务与店长页面在修复后不再出现请求失败或伪 0；
- App 金额按端侧格式取整，底层 API/DB 与 Web 一致；
- 店长台账的高危操作按钮仍需独立业务裁决；
- 未进行材料、验收、真机和弱网沉浸体验。
