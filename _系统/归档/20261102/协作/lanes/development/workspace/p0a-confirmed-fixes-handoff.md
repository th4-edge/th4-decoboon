# P0-A 已确认修复包开发交接

## 输入依据

- `输出/草稿/P0应用开发第一批-20260918/06-开发冻结包-已确认修复.md`
- `输出/草稿/P0应用开发第一批-20260918/02-R2-权限状态账务与反例.md`
- `输出/草稿/P0应用开发第一批-20260918/evidence/runtime-evidence.md`
- 业务事实源：`BR-001`、`BR-002`、`DEC-001`、`GAP-003`、`SCN-001`、`SCN-002`、`SOL-001`、`SOL-002`、`MET-002`

## changed_files / output_paths

后端：

- `src/main/java/com/th4/edge/decoboon/project/context/ProjectAccessContext.java`
- `src/main/java/com/th4/edge/decoboon/project/context/ProjectContext.java`
- `src/main/java/com/th4/edge/decoboon/project/context/ProjectContractContext.java`
- `src/main/java/com/th4/edge/decoboon/project/context/ProjectCostContext.java`
- `src/main/java/com/th4/edge/decoboon/project/service/contract/save/support/Save.java`
- `src/main/java/com/th4/edge/decoboon/project/service/contract/update/support/Update.java`
- `src/main/java/com/th4/edge/decoboon/project/service/projects/save/support/Save.java`
- `src/main/java/com/th4/edge/decoboon/property/context/PropertyContext.java`
- `src/main/java/com/th4/edge/decoboon/customer/service/cust/measure/support/Measure.java`
- `src/main/java/com/th4/edge/decoboon/customer/service/cust/handoff/support/Handoff.java`
- `src/main/java/com/th4/edge/decoboon/quote/context/QuoteContext.java`



App：

- `../../前端代码/th4-decoboon-app/src/domains/crm/api.ts`
- `../../前端代码/th4-decoboon-app/src/subpackages/customer/pages/detail.vue`
- `../../前端代码/th4-decoboon-app/src/subpackages/customer/pages/measure.vue`
- `../../前端代码/th4-decoboon-app/src/subpackages/team/pages/designers.vue`
- `../../前端代码/th4-decoboon-app/scripts/verify-customer-source-contract.mjs`
- `../../前端代码/th4-decoboon-app/package.json`

测试：

- `src/test/java/com/th4/edge/decoboon/DecoboonClosedLoopTest.java`
- `src/test/java/com/th4/edge/decoboon/project/context/ProjectContractContextTest.java`
- `src/test/java/com/th4/edge/decoboon/project/context/ProjectCostContextTest.java`
- `src/test/java/com/th4/edge/decoboon/property/PropertyHandoffOwnershipTest.java`
- `src/test/java/com/th4/edge/decoboon/customer/CustomerMeasureStateGateTest.java`
- `src/test/java/com/th4/edge/decoboon/project/ProjectCreationScopeGuardTest.java`

## verification

- `mvn -o -q test`：PASS，42 suites / 120 tests / 0 failures / 0 errors / 0 skipped。
- `npm run verify:mp-weixin`：PASS；主包 480520 B，最大分包 164914 B，总包 1313312 B。
- 定向测试覆盖合同/项目角色与报价范围、无房屋量房、New/已完成量房门禁、合同 Accepted/Signed/Effective 三阶段、空报价分项成本不完整。

## migration_required

- 无新增/调整表结构，本批不要求数据库迁移。历史异常数据需单独审计，不在本批自动修复。

## risks

- 历史数据库中若已有由客户地址自动生成的重复房屋，本次不会自动清理；需由 DBA 按客户/房屋审计后处理。
- 历史报价可能缺少 store/team 归属快照，旧数据是否可由店长/团队负责人读取需由 acceptance 按真实库验证。
- App 来源枚举已通过静态契约检查，但仍需在真实小程序页面保存后核对列表与详情显示。

## next_action

- acceptance lane 独立复验六项 P0 修复及负向用例。
- 使用隔离测试库检查历史报价 store/team 归属数据，必要时另立数据修复任务。

## acceptance_needed

- 四角色、跨企业、跨门店、跨团队、非参与员工对合同和项目创建的负向验证。
- propertyId=null、伪造他客户房屋、New 客户、已量房客户量房负向验证。
- Quote Accepted -> Customer Quoted、Contract Signed -> Customer Signed、Contract Effective -> Customer Serving 三阶段验证。
