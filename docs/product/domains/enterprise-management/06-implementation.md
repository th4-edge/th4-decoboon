# 企业管理 · EMG-V2 实现记录

## 1. 批次与范围

- 批次：`EMG-V2 员工生命周期`
- 状态：`PASS`（`ACC-PI-20261022-005-EMG-V2` 独立验收通过）
- 级别：L2（权限、组织语义、跨端）
- 业务依据：`DEC-085` / `EV-353`、`DEC-081`、`DEC-082`、`DEC-070`

## 2. 后端改动

- `src/main/java/com/th4/edge/tenant/user/context/MembershipContext.java`
  - 恢复员工时增加企业级锁、Active 门店校验、订阅可写与席位容量校验；
  - 新增目标员工订阅锁键；
  - 店长不能将员工提升为店长；只有 BOSS 可设置 `StoreManager`；
  - 管理员代办离职的成员日志 `operator` 改为实际管理员，目标员工仍为 `employee`；
  - 保持 Team 参数拒绝与历史 Team 只读边界。
- `src/main/java/com/th4/edge/tenant/user/service/invite/invitation/cancel/support/Cancel.java`
  - 取消邀请前统一通过 `TenantContext.requireEmployee` 加载完整登录员工，修复 `@User` 最小投影无企业关联导致的 NPE。
- `src/test/java/com/th4/edge/tenant/user/FinanceOrganizationRulesTest.java`
  - 覆盖财务邀请门店、店长邀请边界、店长提升店长拒绝、恢复席位/门店校验、离职审计操作人和取消邀请完整员工加载。

## 3. App 改动

- `src/subpackages/company/pages/invite.vue`
  - 店长邀请岗位仅设计师、业务员、项目经理、财务；
  - BOSS 邀请财务显式选门店；店长固定本店；
  - 移除 Team 选择。
- `src/subpackages/company/pages/members.vue`
  - 店长不能操作自己、BOSS、其他店长；
  - 离职文案统一为“离职”；普通失败后二次确认强制离职。
- `src/subpackages/company/pages/member-detail.vue`
  - 店长无门店调整入口；
  - 店长岗位选项不含店长；
  - 按范围隐藏无权限操作。
- `src/domains/identity/api.ts`
  - 成员更新支持可选 `storeId`；
  - 邀请支持财务目标门店。

## 4. Web 改动

- `src/domains/identity/api.ts`
  - 新增成员分页、调整、邀请、停用、恢复、离职 API；
  - 统一枚举投影与错误处理所需类型。
- `src/domains/identity/model.ts`
  - 集中员工角色常量，避免页面重复字符串并让冻结角色治理门禁可精确验证。
- `src/views/workbench/BossOrganizationView.vue`
  - 增加在职/停用/离职筛选；
  - 增加调岗/调店、停用、恢复、离职；
  - BOSS 可调岗调店；店长只可本店调岗；
  - 店长不能操作自己、BOSS、其他店长；
  - 邀请岗位不含店长、TeamLeader；
  - 邀请财务按角色固定/选择门店；
  - 店长共用页面时不请求 BOSS 专属组织统计，避免 403 告警；BOSS 仍正常展示组织报告。

## 5. 已执行验证

- 后端定向：`FinanceOrganizationRulesTest`、`MembershipContextTest`、`TenantInternalRoleTest`、`EmployeeRoleContractTest`、`TeamLeaderAuthGateTest`，最终结果 `26 tests / 0 failures / 0 errors`；修复日志见 `docs/acceptance/runs/20261023-emg-v2-def01-fix/backend-targeted-tests.log`。
- App：`npm run type-check` PASS；`npm run check:navigation` PASS；日志见 `app-type-check.log`、`app-navigation.log`。
- App 全量共享工作区构建：最终在独立验收阶段复跑 PASS；此前并行 `expense.scss` 阻断已解除。
- Web：共享工作区 `type-check` PASS；`check:permissions` PASS；`check:workbenches` PASS（修复过一次 TeamLeader 静态门禁误报后重跑）；隔离 `vite build` PASS。
- Web 日志见 `web-type-check-shared.log`、`web-type-check-isolated.log`、`web-build-isolated.log`。
- 真实 HTTP/SQL：BOSS 调岗调店、店长本店调岗、店长跨店 403、店长提升店长 403、停用后登录 403、恢复后登录成功、离职状态/原因落库、管理员离职审计操作人正确。
- 独立黑盒验收：`18/18 P0 PASS`；`EMG-V2-DEF-01` 邀请取消 NPE 与 `EMG-V2-DEF-02` 店长组织页统计告警均修复并独立复验 `CLOSED`。

## 6. 最终缺陷关闭

- `EMG-V2-DEF-01`：店长取消本人邀请、BOSS 取消店长邀请均成功，DB `Cancelled`，席位 `3 -> 2`、Pending `1 -> 0`。
- `EMG-V2-DEF-02`：店长组织页无 `statistic/organization/report` 请求和权限告警；BOSS 同页统计 200。
- 独立证据：`docs/acceptance/runs/20261023-emg-v2-independent-acceptance/followup/README.md`。

## 7. 未改事实

- 未恢复 Team 或 TeamLeader；
- 未新增或修改 DDL / 迁移；
- 未改变金额、成本、利润、支付或订阅计价公式；
- 未修改生产数据，未发布生产或小程序正式版；
- 未把开发自测标记为独立验收 PASS。


## 8. EMG-V2.1 邀请幂等与重发闭环（2026-10-26）

- 批次：`EMG-V2.1`，状态 `DEV_VERIFIED`，等待独立验收。
- 后端：同手机号未过期 Pending 邀请改为幂等复用；过期邀请先关闭再新建；店长跨门店复用拒绝；增加邀请结构化事件日志。
- App：邀请页增加 Pending 邀请列表、重新分享、取消、复用提示；移除邀请编号和手工编号入口；BOSS 邀请财务补门店选择；无 Employee 身份不调用门店接口。
- Web：成员组织页增加 Pending 邀请列表与取消能力，明确重新分享需进入 App。
- 回归：后端 `FinanceOrganizationRulesTest` 20/0；App type-check、invite-flow、onboarding、registration、mp-weixin build、subpackages PASS；Web type-check、Vite build PASS。
- 证据：`docs/acceptance/runs/20261026-emg-v21-invite-idempotency/README.md`。

## 9. EMG-V2.2 邀请生产闭环（2026-10-27）

- 后端：
  - `invite/invitation/cancel` 改为 `User<?>` 主体注入；先拒绝无员工 Credentials，再通过 `TenantContext.requireEmployee` 加载完整员工后取消邀请。
  - `invite/invitation/page` 同样改为 `User<?>` 主体注入，只允许 BOSS/店长；TeamLeader 不再进入邀请管理。
  - 新增 `InvitationProviderPrincipalTest`，覆盖完整员工加载、无员工凭证业务 403、框架注入类型可容纳 Credentials、BOSS 分页。
  - 发布契约增加邀请取消与邀请分页关键类/方法标记。
- App：
  - 复用 `codex/invite-flow-release@006d22f`（1.0.72）：待接受邀请列表、重新分享、取消确认、复用提示、移除邀请编号手输。
  - `check:invite-flow`、`type-check`、release 构建、分包检查通过。
- Web：
  - 在生产基线补齐邀请创建、待接受邀请列表、取消邀请；重新分享明确引导到 App。
  - `type-check`、权限门禁、工作台门禁、production build 通过。
- 已知无关失败：
  - App 全量 `verify:mp-weixin` 在既有“设计师首页快捷入口顺序”契约处失败；本次邀请变更未修改该契约涉及的 `src/roles/catalog.ts`、`src/pages/index/index.vue` 或检查脚本，判定为生产基线既有问题，另行处理。
