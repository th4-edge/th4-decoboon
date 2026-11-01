# 业主整改责任人默认项目经理交接

- ISSUE-ID：`DEF-OWNER-RECTIFY-OWNER-001`
- 时间：2026-09-23
- lane：development
- 状态：`implementation_and_self_test_done_pending_independent_acceptance`

## 用户口径

业主端提交整改要求时，整改责任人默认当前项目项目经理，不再让业主选择人员。

## 实现

- `customer/acceptance/update` 删除 `rectificationOwnerId` 入参和候选校验。
- 业主驳回时，服务端固定取 `Acceptance.project.manager`。
- 项目经理为空时阻断驳回，返回 `400 当前项目未指定项目经理，无法提交整改要求`。
- 整改截止日期仍由业主选择并必填。
- App 业主验收表单将整改责任人改为只读展示，不再渲染人员 picker，也不再发送责任人 ID。

## Changed Files

- `src/main/java/com/th4/edge/decoboon/customer/service/acceptance/update/support/Update.java`
- `../../前端代码/th4-decoboon-app/src/domains/acceptance/model.ts`
- `../../前端代码/th4-decoboon-app/src/domains/acceptance/api.ts`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/accept/edit.vue`
- `src/test/java/com/th4/edge/decoboon/owner/OwnerClosureAcceptanceTest.java`

## Verification

- 后端 `mvn -o -q -DskipTests compile` PASS。
- `OwnerClosureAcceptanceTest` PASS：新增“提交人与项目经理不同，驳回仍归属项目经理”和“项目经理为空阻断”回归。
- `ProviderSecurityAcceptanceTest` PASS。
- App `npm run type-check` PASS。
- App `npm run build:mp-weixin` PASS。
- App `npm run check:subpackages` PASS：主包 634718 B，最大分包 415147 B，总包 2201627 B。

## 业务知识缺口

用户决策与现有 `BR-008`、`SCN-010` 中“项目侧指定整改责任人”口径冲突。
已登记咨询单：

`docs/product/consultations/REQ-BIZ-20260923-003-业主整改责任人默认项目经理.md`

## Acceptance Needed

- 真实业主微信提交驳回：责任人只读显示当前项目经理。
- 项目经理为空：页面提示且接口拒绝，不产生 Rejected 状态。
- 项目经理与验收提交人不同：整改轮次仍归属项目经理。
- 历史轮次中的责任人、截止日期和问题清单不被覆盖。

## 2026-09-23 增量：整改类型、多图与服务端默认截止日期

- 新增整改类型 `Construction`（施工整改，3 天）和 `Design`（设计整改，暂定 5 天）。
- 业主整改表单支持选择整改类型，不再选择截止日期；系统按类型自动计算。
- 业主可上传最多 9 张整改问题图片，保存到验收单和当前轮次。
- 业务提交验收 Provider 已移除 `rectificationOwnerId/rectificationDueDate` 入参，避免绕过业主域规则。
- 项目经理整改看板展示整改类型、问题图片数量和系统截止日期。
- 数据库迁移：`docs/design/migrations/56-验收整改类型与问题图片.sql`，并同步空库基线。
- 业务确认缺口：设计整改 5 天为暂定值，咨询单 `docs/product/consultations/REQ-BIZ-20260923-004-整改类型与默认截止天数.md`。

## 2026-09-23 增量：业主验收轮次时间线身份域拆分

- 原日志 `project/acceptance/round/page处理失败: Can not set Employee ... to Credentials` 已定位为业主页调用员工域轮次 Provider。
- 新增 `customer/acceptance/round/page`，只接收 `Credentials` 业主身份，并通过 `CustomerAccess` Active 授权读取轮次。
- App `acceptanceRoundApi.rounds` 按 `session.role === 'customer'` 分流到业主路由。
- 回归：`OwnerClosureAcceptanceTest` 覆盖业主读取本人轮次、设计/施工类型与截止日期、跨项目 404、员工主体 403；`ProviderSecurityAcceptanceTest` 路由总数更新为 305，新增路由未登录拒绝。

## 2026-09-23 增量：业主验收整改图片上传授权

- 现场错误：业主上传整改图片时报“仅企业员工可以上传文件”。
- 修复：`FileStorageContext` 保留员工通用上传，额外允许 `Credentials` 业主在 `FileScope.Acceptance` 且 `projectId` 有 Active `CustomerAccess` 时上传；其他业务范围仍拒绝。
- 读取授权：`FileAuthorizationContext` 的 Acceptance 白名单加入 `rectificationIssueImages`，业主上传后可重新签名预览。
- App：业主驳回表单恢复最多 9 张整改问题图片上传，并提交 `rectificationIssueImages`。
- 回归：`OwnerFileExposureAcceptanceTest` 覆盖业主上传本人项目验收图片成功、跨项目 404、上传非验收范围 403；`OwnerClosureAcceptanceTest` 覆盖业主整改图片落库和轮次留痕。
- 全量回归：92 suites / 305 tests / 0 failures / 0 errors PASS。
