# 设计方案与设计报价审视 · 实现记录

## 本批范围

- Batch：B1
- 垂直切片：设计师与经营角色看到同一方案的当前价格，并完成企业运营视角的只读查看
- 不包含：审批、核价、退回、通过/拒绝流程；报价过期判断；合同/项目/成本核算改造

## 变更

- 后端：
  - `Design` 增加服务端价格只读投影：方案计算价、区域数、分项数、当前报价摘要；
  - `DesignPriceContext` 批量解析当前报价：`Converted > Accepted > 最新非 Superseded`；
  - `DesignItemRepository`、`SpaceRepository` 使用聚合查询，避免逐方案 N+1；
  - `design/scheme/page` 增加 `ownerId/storeId/keyword` 过滤与价格投影；
  - `design/scheme/info` 增加价格投影和当前报价；
  - `DesignCostContext` 对设计师、业务员、项目经理隐藏 `costPrice/cost`；
  - `statistic/workbench/design/page` 增加最近方案、价格来源、当前报价金额/状态。
- Web：
  - `DesignSchemeRail` 增加正式报价/方案计算价、报价状态；
  - 新增 `DesignReviewView` 与 `DesignReviewDetailView`；
  - Boss/StoreManager 增加 `/workbench/design-reviews`；
  - Finance 增加 `/finance/design-reviews`；
  - 页面只读，不新增任何写动作或状态。
- App：
  - 设计工作台卡片增加当前价格、报价编号和状态；
  - 方案列表无正式报价时回退显示方案计算价；
  - 新增 `design/review` 与 `design/review-detail` 只读页面；
  - Boss/店长/财务增加 `view:design-review` 只读能力，设计师直链由路由和页面双层拦截；
  - 金额展示统一使用 `JzMoney`，修复小于 1 万元时出现 `¥¥82` 的重复货币符号；
  - Boss/店长/财务增加设计方案运营查看入口。
- 迁移：无 DDL、无数据迁移。
- 配置：新增 App 页面路由与 Web 角色静态路由；无新 Provider 路由。

## 代码版本

| 工程 | dev 集成提交 | 隔离实施提交 |
| --- | --- | --- |
| th4-decoboon | `8aeb31a` | `fdbe4fd` |
| th4-decoboon-web | `889c3b4` | `1e0c0a8` |
| th4-decoboon-app | `99d8ab8` | `52626e8` |

## 测试与证据

- 定向测试：`DesignPriceContextTest`、`DesignCostContextTest`、`DesignSchemeInfoFormulaTest`、`DesignerWorkbenchAggregationTest`，13/13 PASS。
- 后端全量：`mvn -o test`，901/901 PASS（本轮修复后复跑）。
- Web：`npm run build`、工作台可达、权限、表单门禁 PASS。
- App：`type-check`、UI、导航、设计分项、动作、分包门禁 PASS；`build:mp-weixin:release` PASS。
- App 构建体积：主包 937062 B，最大分包 664803 B，总包 3700002 B，17 分包/167 页。
- 真实页面：
  - Web BOSS/店长/财务列表与详情、设计师方案执行页均已真实渲染，1440/1280/1024 无横向溢出；
  - App H5 同源页面以 390×844 完成 BOSS/店长/财务/设计师复核，截图见 `docs/acceptance/runs/20261021-design-quote-ui/`；
  - 微信开发者工具切换同一 AppID 的复制项目时出现既有运行时异常 `up.split is not a function`，原生小程序点击冒烟未完成，未标记 UI_REVIEWED。
- 跨范围 HTTP 负向：临时二店店长/门店财务列表为 0、详情 404、跨店筛选 403；其他企业列表为 0、详情 404；本地临时数据已恢复。
- 性能采样：列表 P95 145.4ms、详情 P95 132.6ms（各 10 次，均低于 500ms）；未构造 10 倍真实数据量。
- 凭据恢复：BOSS/店长临时登录哈希已按备份恢复，其他企业凭据与临时二店已回滚。
- 部署/重启：未发布生产。
- 截图/日志：待 UI 审查批次补齐。

## 风险与未闭环

1. `GAP-061` 未关闭：方案变更后正式报价是否过期尚无稳定修订令牌口径；本批不展示“报价已过期”结论。
2. 微信开发者工具原生小程序页面级点击复核未完成；当前 App 证据为同源 H5 渲染 + release 构建，不等于真机通过。
3. 未构造 10 倍真实数据量验证 SQL 条数不增长；当前仅完成 10 次端到端采样。
4. 本批未发布 test/prod；App 正式版仍由用户本人提审发布。
