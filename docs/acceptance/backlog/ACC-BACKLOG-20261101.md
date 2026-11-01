# ACC-BACKLOG-20261101 待验收总账

- 生成时间：`2026-11-01T23:55:51+08:00`
- 验收候选：`ACC-20261101-01`
- 待验收/待确认变更：**62**
- 已排除 `ACCEPTED`、已关闭、已发布和已完成状态
- `PENDING_INVENTORY_MAPPING` 表示尚未映射冻结用例，不代表开发未完成
- 原则：按业务结果分批验收，不按文件或会话验收

## 状态汇总

| 状态 | 数量 |
| --- | ---: |
| `DEV_VERIFIED_PENDING_ACCEPTANCE` | 41 |
| `PENDING_USER_OR_VERIFICATION` | 21 |

## 批次汇总

| 批次 | 名称 | 待验收数 |
| --- | --- | ---: |
| `B1-B2-CORE-CHAIN` | 核心业务链 | 12 |
| `B3-FINANCE` | 财务与成本 | 8 |
| `B4-PAYROLL` | 薪酬与考勤 | 8 |
| `B5-ENTERPRISE` | 企业管理与权限 | 6 |
| `B6-BRAND-SHELL` | 品牌、登录与壳层 | 13 |
| `B7-PLATFORM` | 跨端契约、选择器与性能 | 9 |
| `B8-OTHER` | 其他工程或运维事项 | 6 |

## 明细

| 变更记录 | 当前状态 | 工作流状态 | 建议批次 | 冻结用例 | 验收 RUN |
| --- | --- | --- | --- | --- | --- |
| `CHG-20261029-001-boss-product-baseline.md` | 草稿 / 待用户确认 | `PENDING_USER_OR_VERIFICATION` | `B8-OTHER` | 待映射 | 待建立 |
| `CHG-20261029-007-local-dev-db-reset.md` | 待用户验证 | `PENDING_USER_OR_VERIFICATION` | `B8-OTHER` | 待映射 | 待建立 |
| `CHG-20261029-008-app-owner-scenario-invite-gate.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B5-ENTERPRISE` | 待映射 | 待建立 |
| `CHG-20261029-009-enterprise-quick-onboarding.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B5-ENTERPRISE` | 待映射 | 待建立 |
| `CHG-20261029-010-owner-entry-public-guide.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B8-OTHER` | 待映射 | 待建立 |
| `CHG-20261029-011-login-phone-authorization-once.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-012-company-brand-profile-ui-and-logo-projection.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-013-company-brand-color-application-theme.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-014-brand-theme-runtime-regression.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-015-app-brand-visual-coverage.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-016-web-company-brand-memory-and-official-logo.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-016-zcool-brand-palette-refresh.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-017-web-login-copy-visual-refinement.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-018-web-login-owned-system-copy-gate.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-019-web-standard-shell-navigation.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-020-brand-theme-and-login-refinement.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-021-web-authenticated-interaction-system.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261029-021-web-pending-invite-enum-filter.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B5-ENTERPRISE` | 待映射 | 待建立 |
| `CHG-20261029-022-invite-auto-discovery-and-session-recovery.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B5-ENTERPRISE` | 待映射 | 待建立 |
| `CHG-20261029-022-web-identifier-exposure-cleanup.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B8-OTHER` | 待映射 | 待建立 |
| `CHG-20261029-023-local-finance-reinvite-two-route-test.md` | READY_FOR_TWO_ROUTE_USER_TEST | `PENDING_USER_OR_VERIFICATION` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261029-024-miniapp-app-invite-context-runtime-fix.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B5-ENTERPRISE` | 待映射 | 待建立 |
| `CHG-20261029-027-pending-design-workbench-ui-upgrade.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261029-028-customer-web-card-page-upgrade.md` | 开发自测通过，待用户体验 | `PENDING_USER_OR_VERIFICATION` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261029-029-app-customer-form-alignment-brand-tabs.md` | 待用户验证 | `PENDING_USER_OR_VERIFICATION` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261029-031-unified-customer-workspace-efficient-role-scopes.md` | 待用户验证 | `PENDING_USER_OR_VERIFICATION` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261030-002-app-payroll-attendance-scheduled-days.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B4-PAYROLL` | 待映射 | 待建立 |
| `CHG-20261030-002-frontend-provider-contract-audit.md` | 开发自测通过，待用户确认 | `PENDING_USER_OR_VERIFICATION` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261030-003-payroll-employee-payment-context.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B4-PAYROLL` | 待映射 | 待建立 |
| `CHG-20261030-003-provider-route-convergence.md` | 开发自测通过，待用户验收 | `PENDING_USER_OR_VERIFICATION` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261030-004-app-logout-and-web-first-registration.md` | 开发自测通过，待用户验收 | `PENDING_USER_OR_VERIFICATION` | `B6-BRAND-SHELL` | 待映射 | 待建立 |
| `CHG-20261030-004-payroll-permission-store-scope.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B4-PAYROLL` | 待映射 | 待建立 |
| `CHG-20261030-005-payroll-self-attendance.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B4-PAYROLL` | 待映射 | 待建立 |
| `CHG-20261030-007-manager-payroll-field-visibility.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B4-PAYROLL` | 待映射 | 待建立 |
| `CHG-20261030-008-manager-payroll-actions.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B4-PAYROLL` | 待映射 | 待建立 |
| `CHG-20261030-034-finance-p0-cross-surface-accounting-guard.md` | 代码与真实页面验证通过；历史数据修复待确认；未发布 | `PENDING_USER_OR_VERIFICATION` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261031-039-有效减项成本完整性分母修复.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261031-041-provider-response-parity-and-web-app-e2e.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261031-043-quote-finance-server-summary.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261031-044-contract-archive-server-summary.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261031-045-contract-line-database-pagination.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261031-046-owner-app-contract-line-pagination.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261031-047-contract-enrich-cost-aggregate.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261031-051-payroll-workbench-aggregate.md` | DEV_VERIFIED / RENDER_AND_10X_PERF_PASS | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B4-PAYROLL` | 待映射 | 待建立 |
| `CHG-20261101-012-finance-customer-owner-directory-fix.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261101-013-decision-action-coverage-gate.md` | DEV_VERIFIED_PENDING_APP_MANUAL | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261101-014-action-provider-matrix-v02.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261101-015-business-object-picker-standard.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261101-016-finance-home-information-hierarchy.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261101-016-web-employee-picker-server-pagination.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B5-ENTERPRISE` | 待映射 | 待建立 |
| `CHG-20261101-017-boss-home-analysis-hierarchy.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B8-OTHER` | 待映射 | 待建立 |
| `CHG-20261101-017-web-finance-project-pickers.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261101-018-app-personnel-pickers.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261101-018-pm-workbench-project-first.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261101-019-designer-quote-queue-noise-reduction.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261101-020-designer-queue-family-noise-reduction.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261101-021-app-quote-supplier-pickers.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B1-B2-CORE-CHAIN` | 待映射 | 待建立 |
| `CHG-20261101-022-app-selector-semantic-audit.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261101-023-app-selector-10x-p95-devtools.md` | DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B7-PLATFORM` | 待映射 | 待建立 |
| `CHG-20261101-028-supplier-payment-code-images.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B8-OTHER` | 待映射 | 待建立 |
| `CHG-20261101-029-finance-month-count-semantics.md` | DEV_VERIFIED_PENDING_USER | `DEV_VERIFIED_PENDING_ACCEPTANCE` | `B3-FINANCE` | 待映射 | 待建立 |
| `CHG-20261101-030-finance-payroll-draft-delete-by-scope.md` | 待验证 | `PENDING_USER_OR_VERIFICATION` | `B4-PAYROLL` | 待映射 | 待建立 |

## 当前候选限制

- 当前为三端未提交工作区，`allReposClean=false`。
- 该候选只能用于现状盘点和内部预验，不能作为最终发布制品。
- 每个批次必须先形成干净候选提交，再创建正式独立验收 RUN。
- 现有冻结 44 条用例只覆盖核心主链；薪酬、企业管理、品牌等需另行建立或扩展测试资产。
