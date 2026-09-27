# AI 调用与企业门店影子用量底账 Handoff

## 状态

`implementation_and_self_test_done_pending_independent_acceptance_and_target_migration`

## 输入依据

- `docs/product/2026-09-26-AI资源用量基础统计P0产品需求规格.md`
- `docs/product/2026-09-26-AI资源用量与企业门店统计设计.md`
- `REQ-BIZ-20260926-004`
- `rules.md` 五条铁律、Provider 命名与安全边界
- `AGENTS.md` 版本管理与权限先行

## 实现范围

1. 新增 AI 调用底账 `AiUsageCall`，不保存提示词、客户正文和模型原始输出。
2. 记录企业、业务门店、执行门店、分摊门店、员工、能力、模型、资源单位、影子点数和状态。
3. 门店归属按 `业务门店 -> 执行门店 -> 企业公共` 规则。
4. 同一企业同一幂等键只形成一条有效调用。
5. 影子点数 P0 默认按 `resourceUnits` 一比一占位，费率版本固定为 `shadow-direct-v1`，不代表正式收费费率。
6. 新增受保护只读 Provider：
   - `ai/usage/report`
   - `ai/usage/page`
7. 企业报表按企业、门店、能力、日期聚合，企业公共单独显示。
8. 调用明细不序列化 `company`、`idempotencyKey`、`requestId` 和 `platformCost`。

## Changed Files

- `src/main/java/com/th4/edge/decoboon/ai/configure/AiUsageStatus.java`
- `src/main/java/com/th4/edge/decoboon/ai/configure/AiBillingStatus.java`
- `src/main/java/com/th4/edge/decoboon/ai/configure/AiStoreAllocation.java`
- `src/main/java/com/th4/edge/decoboon/ai/model/AiUsageCall.java`
- `src/main/java/com/th4/edge/decoboon/ai/model/AiUsageQuery.java`
- `src/main/java/com/th4/edge/decoboon/ai/model/AiUsageReport.java`
- `src/main/java/com/th4/edge/decoboon/ai/repository/AiUsageCallRepository.java`
- `src/main/java/com/th4/edge/decoboon/ai/context/AiUsageContext.java`
- `src/main/java/com/th4/edge/decoboon/ai/context/AiUsageReportContext.java`
- `src/main/java/com/th4/edge/decoboon/ai/service/usage/report/**`
- `src/main/java/com/th4/edge/decoboon/ai/service/usage/page/**`
- `src/test/java/com/th4/edge/decoboon/ai/**`
- `docs/design/migrations/64-AI调用与影子用量底账.sql`
- `scripts/build-release.sh`
- `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java`

## Verification

- `mvn -o -q -DskipTests compile`：PASS
- `mvn -o -q -Dtest=AiUsageContextTest,AiUsageReportContextTest test`：7 tests PASS
- `mvn -o -q -Dtest=AiUsageSerializationContractTest test`：PASS
- `mvn -o -q -Dtest=AiUsageContextTest,AiUsageReportContextTest,ProviderSecurityAcceptanceTest test`：PASS
- `mvn -o -q test`：全量后端测试 PASS
- Provider 安全清单：AI 用量新增 2 条，路由总数 343，public 12 + protected 331。

## Risks

- T06 尚未书面冻结正式收费主体、费率、失败返还和资源包规则；当前只可内部统计，禁止收费。
- 迁移 63 尚未在目标 MySQL 执行。
- 当前没有真实 AI Provider 调用 `AiUsageContext`，底账已可写入但尚未接入首个 AI 能力。
- 影子点数默认一比一，仅用于验证链路，不可用于定价。
- 当前统计 Provider 仅开放 BOSS；店长、财务和平台运营视图尚未实现。
- `platformCost` 已保留字段但未向租户序列化，平台成本对账接口尚未开发。

## Web Follow-up

- Web 已建立独立 Git 仓库，基线提交 `970049c`。
- 功能分支：`feature/ai-usage-statistics`。
- 从「组织效能」进入 `/boss/statistic/ai-usage`，未增加第七个一级菜单。
- 页面覆盖筛选、企业指标、调用趋势、能力分布、门店与企业公共、调用明细分页和影子计量提示。
- Web `npm run type-check`、`npm run build` PASS。
- Web 交接：`th4-decoboon-web/docs/ai-usage-statistics-handoff.md`。

## Next Action

1. 在目标 MySQL 预发布环境演练迁移 63。
2. 启动后端并完成 Web 真实接口联调，验证企业公共、门店筛选和明细分页。
3. 将首个 AI 能力接入 `AiUsageContext`，验证真实调用写入。
4. 由 acceptance lane 验证企业公共、业务门店优先、幂等和统计对账。
5. 取得 T06 后再开发正式扣点、AI 资源包和账单。

## Acceptance Needed

- 企业总调用量、成功/失败、影子点数与调用明细一致。
- 门店分摊加企业公共等于企业总量。
- 业务门店优先、执行门店兜底、企业公共兜底。
- 重复、失败、取消和作废不造成重复计费。
- BOSS 可看企业总量；非 BOSS 不越权。
- 关闭 OSIV 场景下报表读取门店名称不报懒加载异常。
