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
  - Finance 增加 `view:design` 只读能力；
  - Boss/店长/财务增加设计方案运营查看入口。
- 迁移：无 DDL、无数据迁移。
- 配置：新增 App 页面路由与 Web 角色静态路由；无新 Provider 路由。

## 代码版本

| 工程 | 分支 | 提交 |
| --- | --- | --- |
| th4-decoboon | `feature/design-quote-review` | `fdbe4fd` |
| th4-decoboon-web | `feature/design-quote-review` | `1e0c0a8` |
| th4-decoboon-app | `feature/design-quote-review` | `52626e8` |

## 测试与证据

- 定向测试：`DesignPriceContextTest`、`DesignCostContextTest`、`DesignSchemeInfoFormulaTest`、`DesignerWorkbenchAggregationTest`，13/13 PASS。
- 后端全量：`mvn -o test`，901/901 PASS。
- Web：`npm run build`、工作台可达、权限、表单门禁 PASS。
- App：`type-check`、UI、导航、设计分项、动作、分包门禁 PASS；`build:mp-weixin:release` PASS。
- App 构建体积：主包 937062 B，最大分包 664803 B，总包 3700002 B，17 分包/167 页。
- 真实页面/真机：未执行，未标记 UI_REVIEWED。
- 部署/重启：未发布生产。
- 截图/日志：待 UI 审查批次补齐。

## 风险与未闭环

1. `GAP-061` 未关闭：方案变更后正式报价是否过期尚无稳定修订令牌口径；本批不展示“报价已过期”结论。
2. 未用真实 BOSS/店长/财务账号做 App/Web 渲染走查。
3. 未用真实跨店、跨企业账号完成 HTTP 负向验收；当前依据既有 DataScope 与静态门禁。
4. 未测量 10 倍数据量下的 P95；实现已使用聚合查询，性能验收仍待执行。
5. 本批未发布 test/prod；App 正式版仍由用户本人提审发布。
