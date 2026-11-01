# 业主合同履约档案身份域拆分交接

- ISSUE-ID：`DEF-OWNER-CONTRACT-001`
- 时间：2026-09-23
- lane：development
- 状态：`implementation_and_self_test_done_pending_independent_acceptance`

## 现象与根因

业主微信会话调用 `project/contract/workbench` 时，员工 Provider 尝试把 `Credentials`
注入 `@User Employee employee`，生产返回 500：

`Can not set Employee ... Workbench.employee to Credentials`

根因不是登录失效，而是业主端错误复用了员工身份域 Provider。合同履约档案又包含项目
成本、动态毛利、合同成本快照等内部经营字段，不能通过放宽主体类型兼容业主调用。

## 实现

后端新增两个业主域受保护 Provider：

- `customer/contract/workbench`：以 `@User User<?> principal` 接收业主会话，经
  `CustomerAccessContext` 和 `CustomerContractContext` 校验 Active 项目授权。
- `customer/contract-line/page`：仅返回业主合同范围摘要，不返回 `cost`、
  `materialPrice`、`manualPrice`、`formula` 等内部字段。

业主工作台通过 `@Properties` 显式排除 `cost`、`signedCost`、`quote`、
`createdBy`、`remark` 等内部经营字段；业务代码同时执行 `view.setCost(null)` 作为
防御性保护。

小程序：

- `tradeApi.contractWorkbench` 按 `session.role === 'customer'` 分流到业主 Provider。
- `tradeApi.contractLines` 按身份分流到业主合同行 Provider。
- 业主合同页隐藏员工专用的收款登记、合同打印、原始报价、材料计划入口、选材工作台
  跳转和增减项明细入口，只保留合同摘要、付款节点、进度、变更汇总与选材摘要。

## Changed Files

后端：

- `src/main/java/com/th4/edge/decoboon/customer/context/CustomerContractContext.java`
- `src/main/java/com/th4/edge/decoboon/customer/service/contract/workbench/**`
- `src/main/java/com/th4/edge/decoboon/customer/service/contractlines/page/**`

小程序：

- `../../前端代码/th4-decoboon-app/src/domains/trade/api.ts`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/contract.vue`

回归测试：

- `src/test/java/com/th4/edge/decoboon/owner/OwnerClosureAcceptanceTest.java`
- `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java`

## Verification

- 后端 `mvn -o -q -DskipTests compile` PASS。
- `OwnerClosureAcceptanceTest` PASS：覆盖业主工作台、成本裁剪、`contractId` 越权
  404、员工主体调用业主 Provider 403、合同行业主授权。
- `ProviderSecurityAcceptanceTest` PASS：路由总数 304，新增两条路由未登录均拒绝。
- `DetailResponseWhitelistContractTest,DataScopeRequestFilterIntegrationTest` PASS。
- 后端全量回归（排除已知在飞 `BossMeasureWorkspace2AcceptanceTest`）PASS：92 suites / 301 tests / 0 failures / 0 errors。
- 小程序 `npm run type-check` PASS。
- 小程序 `npm run build:mp-weixin` PASS。
- 小程序 `npm run check:subpackages` PASS：主包 634901 B，最大分包 415579 B，总包 2202242 B。

## Residual Risks / Acceptance Needed

- 未执行生产部署；生产问题状态不能宣称已修复。
- 未用真实业主微信在生产 HTTPS 环境完成端到端验证。
- 业主端仍需 acceptance 验证：已授权项目正常、未授权合同 404、响应无成本字段、
  页面无员工动作、员工端合同履约档案行为不变。
- 业主合同变更明细、选材工作台和合同打印若后续要开放，应分别新增业主域
  Provider，不得回退为复用 `project/*` 员工接口。
