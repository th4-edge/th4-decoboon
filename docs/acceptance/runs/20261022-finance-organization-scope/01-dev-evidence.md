# 开发自测证据

## 后端

- 定向回归：`FinanceOrganizationRulesTest`、`StoreScopeCandidateTest`、`StoreSaveDefaultStoreTest`、`PayrollWorkbenchContextTest`、`MaterialSettlementFlowTest`、`FinanceStoreScopeTest`、`FinanceStoreScopeHttpAcceptanceTest`、`StoreStatusContextTest`
- 结果：**64 tests / 0 failures / 0 errors**。
- 覆盖：总部财务门店候选、邀请目标门店、店长/团队负责人/财务邀请边界、BOSS 调店、非 BOSS 跨店拒绝、默认总店不可停用、新增门店非默认、薪酬/主材门店过滤、既有跨店负向。
- 全量：939 项中 1 项失败，失败为并行角色改造导致的 `ProviderSecurityAcceptanceTest` Provider 清单计数（518 vs 516），与本问题无关；排除该用例后 **936/936 PASS**。

## Web

- `npm run type-check`：PASS
- `npm run build`：PASS
- `check:permissions`、`check:workbenches`、`check:actions`、`check:forms`、`check:states`、`check:page-system`、`check:store-scope`：PASS

## App

- `npm run type-check`：PASS
- `npm run build:mp-weixin`：PASS
- 未通过项：`check:ui` 被并行会话新增的 `App.vue` 冻结角色 `uni.showModal` 拦截，非本问题改动。

## 证据边界

以上是开发自测与构建证据，不是独立验收结论；不得据此标记 `CLOSED` 或宣称正式上线。

## 复验修复证据（2026-10-22）

- FAIL-01：Web 薪酬工作台、提成负向调整、主材下单、主材结算均已监听 `scope.requestStoreId` 并在切换后重载。
- FAIL-02：工资单可见性改为“工资单头门店或工资单内工资项门店”判定；`payroll/sheet/page` 与 `PayrollWorkbenchContext` 共用 `PayrollAccessContext.visibleSheets`。
- FAIL-03：Web 主材结算列表异常进入“暂不可确认”错误态并提供重新加载。
- 定向反例：默认总店制单、普通门店工资项，BOSS 下钻普通门店可见该工资单且工作台 latestSheetId 命中；PASS。
- Web：type-check、build、actions、states、store-scope PASS。
