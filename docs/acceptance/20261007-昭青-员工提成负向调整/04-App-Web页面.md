# App 与 Web 页面交付

## App 小程序

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 客户退款列表 | `/subpackages/finance/pages/refunds` | 展示退款项目、原收款、退款金额、账户和原因 |
| 退款登记 | `/subpackages/finance/pages/refund-create` | 从原收款进入，校验退款金额不超过原收款，提交 `finance/refund/save` |
| 提成负向调整 | `/subpackages/payroll/pages/adjustment-list` | 展示员工、项目、类型、状态、应抵扣和已抵扣 |
| 员工工资快捷入口 | 财务工作台 → 员工工资 | 进入工资单；可新增薪酬方案、录入考勤、生成工资单、确认和发放 |
| 员工提成快捷入口 | 财务工作台 → 员工提成 | 进入提成列表；可计提提成、查看释放/发放并执行提成发放 |

入口：
- 财务工作台「客户退款」；
- 收款记录 `?refund=1` 选择原收款；
- 员工薪酬工作台「负向调整」。

验证：`npm run verify:mp-weixin` PASS，主包约 810,936 B，总包约 3,049,034 B，131 页 / 16 分包，分包规范通过。

## Web 财务工作台

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 客户退款 | `/finance/refunds` | 退款列表、当前页合计、登记退款抽屉 |
| 提成负向调整 | `/finance/commission-adjustments` | 调整类型、状态、应抵扣、已抵扣、发生日期和说明 |
| 员工工资 | `/finance/employee-salaries` | 薪酬方案、月度考勤、工资单生成/确认、工资项、员工发放 |
| 员工提成 | `/finance/employee-commissions` | 提成计提、签约额/比例/金额、释放/发放、员工提成发放 |

验证：`npm run type-check` PASS，`npm run build` PASS，Vite 已生成 `FinanceRefundsView` 和 `FinanceCommissionAdjustmentsView` 页面资源。

员工工资/提成补页验证：Web `npm run build` PASS，生成 `FinanceEmployeeSalariesView` 和 `FinanceEmployeeCommissionsView`；员工工资、员工提成在 1024/1280/1440 下无页面横向溢出。使用说明见 `docs/product/2026-10-07-员工工资与提成使用说明书.md`。

员工选择权限修复：新增后端 `payroll/employee/page`，仅允许 BOSS/财务在薪酬编辑权限内选择本企业员工，避免财务误用通用 `user/employee/page` 被 403 阻断。

## 未覆盖

- 未进行微信开发者工具/真机截图与多机型 UI 目视验收。
- Web 未进行真实浏览器多分辨率视觉门禁。
- 微信支付/第三方退款回调：当前不属于本需求范围。现有微信支付只服务平台 SaaS 续费订单；装企客户收款/退款没有外部支付单关联，退款由财务登记 `finance/refund/save` 形成事实。
- 离职/跨期金额规则仍由财务手工确认。
