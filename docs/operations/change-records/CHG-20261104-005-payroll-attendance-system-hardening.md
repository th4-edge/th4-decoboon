# CHG-20261104-005 · 考勤体系串值、权限与工资单一致性修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261104-005` |
| 类型 | 考勤权限、状态机、校验、审计与跨端交互修复 |
| 风险级别 | L3 |
| 状态 | 后端/Web 已生产发布，App 1.0.86 开发版已上传；待独立验收与真机验证 |
| 日期 | 2026-11-04 |
| 关联审计 | `CHG-20261104-004` / `docs/acceptance/runs/20261104-payroll-attendance-system-audit/README.md` |
| 关联规则 | `BR-015`、`BR-038`、`DEC-047`、`DEC-098` |
| 前置变更 | `CHG-20261104-001` |

## 2. 目标

修复审计确认的考勤数据和权限缺口：

- Web/App 切换月份、员工时清空考勤输入；
- 考勤更新使用 DataScope，阻断门店财务跨店修改；
- 更新接口不再允许任意状态跳转；
- 确认前要求考勤天数完整；
- 已确认数据修改后自动退回 Pending 并清除旧确认章；
- 新增、修改、确认、删除、工资单锁定/恢复写结构化审计日志；
- 工资单草稿重算清理已退出方案范围的旧工资项；
- 考勤列表改为数据库分页，单页上限 50；
- 确认动作补二次确认和恢复提示。

## 3. 非目标

- 不修改工资、考勤扣款、提成、应付、成本和发放公式；
- 不执行生产历史数据修正；
- 不新增数据库表或 DDL；
- 不发布生产；
- 不替代独立验收、真机和客户签字。

## 4. 实现结果

### 后端

- `Update`：
  - 改为 `findOne(byId AND DataScope Specification)`，删除仅按 StoreManager 手工判断门店的路径；
  - 阻断 `InPayroll` 修改；
  - 客户端状态仅允许确认动作；
  - Pending/Draft 可确认，确认前必须满足天数字段完整；
  - Confirmed 数据变更自动回 Pending，清空 `confirmedBy/confirmedAt`；
  - 重复确认幂等。
- `Save`：
  - 月份严格 `YYYY-MM` 校验；
  - 七个天数字段与合计关系校验；
  - `saveAndFlush` 将并发唯一冲突转为业务 409；
  - 备注长度限制 500。
- `Delete`：保留 DataScope 和工资项锁定校验，新增删除审计。
- `Page`：改为数据库分页、排序和 50 条上限，不再全量加载后内存过滤。
- `PayrollBuildContext`：
  - 锁定考勤时记录审计；
  - 重算时识别已退出方案范围的旧 Draft 工资项；
  - 解除旧工资项关联调整，删除旧项，恢复对应考勤为 Confirmed。
- 新增 `PayrollAttendanceValidationContext` 与 `PayrollAttendanceAuditContext`，复用既有“结构化日志审计、不新增表”的薪酬实现方式。

### Web

- `FinanceAttendanceDrawer.vue`：
  - 切月清空天数字段和备注；
  - 切员工清空天数字段和备注；
  - 已确认数据修改后提示需重新确认；
  - 确认考勤增加二次确认。

### App

- `attendance-edit.vue`：
  - 切月、切员工、清空员工时重置天数字段；
  - 已确认数据修改后提示需重新确认。
- `attendance-list.vue`：
  - 确认考勤增加二次确认。

## 5. 验证结果

| 检查 | 结果 |
| --- | --- |
| 定向薪酬考勤测试 | PASS |
| 后端全量测试 | 315 个测试类、1239 个用例、0 失败、0 错误、0 跳过 |
| Provider 响应契约 | PASS，findings=0 |
| Provider 契约自测 | PASS |
| Web `npm run build` | PASS |
| Web `check:permissions` | PASS |
| App `npm run type-check` | PASS |
| App `check:ui` | PASS |
| App `check:finance-ia` / `check:finance-pickers` | PASS |
| App 微信小程序构建 | PASS |
| 真实浏览器复测修复 | PASS（发布记录 `2026-11-04 考勤体系加固三端发布`） |
| 财务 P0 运行时复验 | PASS：状态绕过、确认后编辑、跨店更新 3/3；`RUN-20261104-FIN-P0-REMOTE-RETEST-001` |
| 财务 P0 第二批 | PASS 7/8：4 条页面主链、应付、付款幂等、已发历史通过；跨租户 NOT_RUN；`RUN-20261104-FIN-P0-002` |
| 财务跨域 P0 | PASS 4/4：跨租户、应收退款冲正、材料应付成本、成本完整性；`RUN-20261104-FIN-P0-003` |
| 财务 P0 最终门禁 | PASS 14/14，FAIL/BLOCKED/NOT_RUN=0；`RUN-20261104-FIN-P0-FINAL-001` |
| 微信开发者工具 / 真机复测 | 未执行 |
| 独立验收 / 业务验收 | 未执行 |

## 6. 风险与待办

- 当前审计采用结构化日志，不新增数据库审计表；若业务要求库内持久化审计，需要另立 DDL 变更；
- 2026-08 已发放工资中的“27 正常 + 15 事假”和历史 9/10 月工资单状态冲突仍需用户确认与单独数据修复；
- 生产尚未发布，修复后 Web/App 真实页面和门店财务跨店负向场景尚待复测；
- 工资单生成并发锁未纳入本变更。
