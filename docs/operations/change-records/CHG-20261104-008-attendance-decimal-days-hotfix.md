# CHG-20261104-008 · 月度考勤天数支持小数热修

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261104-008` |
| 类型 | 热修 / DDL / 跨端契约 |
| 风险级别 | L3 |
| 状态 | 已发布，待体验版业务验收 |
| 负责人 | Codex / 用户批准生产热修 |
| 日期 | 2026-11-04 |
| 关联需求/事故/业务裁决 | `INC-20261104-ATTENDANCE-DECIMAL`；用户明确反馈“实际存在请假 0.5 天” |

## 2. 目标与范围

- 目标：月度考勤七类实际天数支持最多两位小数，使 `0.5` 天请假可保存、确认并按实际天数计算工资扣款。
- 范围内：后端实体、Provider 入参、校验、工资扣款、MySQL 迁移与回滚保护；Web 财务考勤输入；App 财务考勤输入。
- 非范围：应出勤天数小数化、历史工资重算、已发放工资更正、考勤审批流或权限变更。
- 受影响端：后端 / Web / App；Admin 不涉及。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是（考勤天数与工资扣款）。
- 业务事实来源与裁决编号：用户 2026-11-04 明确反馈；`DEC-017 / EMP-P0-03` 的扣款规则保持不变。
- 尚存 GAP 或假设：无；只修复半天/小数录入与计算精度，不改变扣款系数。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 店长/财务录入半天事假 | 应出勤 22，正常 21.5，事假 0.5 | 保存成功，回读仍为 `21.50 / 0.50`，可确认 | 后端专项测试 |
| 2 | 工资扣款 | 日薪 200，事假 0.5 | 事假扣款 100.00，不截断为 0 | `PayrollCalculationContextTest` |
| 3 | 非法精度 | 任一天数 `0.555` | 返回 400，提示最多两位小数 | `PayrollAttendanceScheduledDaysTest` |
| 4 | MySQL 8 迁移 | INT 表含整数数据 | 七列转为 `DECIMAL(5,2)`，可写入 `0.5` | 临时表演练 |
| 5 | 回滚保护 | 已存在小数考勤 | 回滚脚本报错停止，不截断数据 | MySQL 8 临时表演练 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/attendance-decimal-days-20261104` | `fe1bb1c4` | `AttendanceSummary`、考勤保存/更新、校验与工资计算、迁移 135 |
| th4-decoboon-web | `hotfix/attendance-decimal-days-20261104` | `7695afd` | `FinanceAttendanceDrawer.vue` |
| th4-decoboon-app | `hotfix/attendance-decimal-days-20261104` | `8765af2` | `attendance-edit.vue`、契约校验、1.0.88 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端专项 | `mvn -q -Dtest=... test` | PASS | 本机 surefire 报告 |
| 后端全量 | `mvn -o -q test` | PASS，315 类 / 1243 用例 / 0 失败 | `/tmp/decoboon-leave-decimal-full-test.log` |
| Provider 响应契约 | `audit-provider-response-contract.py --check/--self-test` | PASS，findings=0 | 本机输出 |
| Web 构建 | `npm run type-check && npm run build` | PASS | `dist/` |
| App 构建 | `npm run build:mp-weixin:release` | PASS | `dist/build/mp-weixin` |
| App 开发版上传 | 微信开发者工具 CLI 1.0.88 | PASS | `upload-info-1.0.88.json` SHA-256 `30e2e2fd…` |
| MySQL 8 迁移/回滚 | 隔离临时表演练 | PASS | 迁移写入 `21.50 / 0.50`；有小数时回滚 exit=1 |
| 生产写精度 | 生产表事务内 `15.00 -> 0.50 -> ROLLBACK` | PASS | 回滚后仍为 `15.00`，无持久化修改 |

- 未执行项及原因：真实体验版页面、真机与客户业务验收待生产迁移和客户端体验版完成后执行。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 后端 `20261104-142823`；Web `20261104-142959` |
| 制品路径/SHA-256 | 后端 `dist/releases/20261104-142742/th4-decoboon-1.0.73-build.tar.gz` / `67cff5b4…`；Web release aggregate `16689b30…` |
| 生产 release/版本 | 后端 `20261104-142823` / 1.0.73；Web `20261104-142959` |
| 上一版本/回滚点 | 后端 `/opt/decoboon/releases/20261104-123233`；Web `/opt/decoboon-web/releases/20261104-134154` |
| 健康检查 | PASS：API HTTP 200 + `tokenIsUndefined`；Web 入口与懒加载资产 HTTP 200 |
| 发布后观察 | 已执行即时日志扫描；仅出现预期缺 token 健康检查，无新增考勤业务异常 |

- 数据库迁移：`135-考勤天数小数.sql`，checksum `2d98d054…`，账本 `SUCCESS`；执行前备份 `db_payroll_attendance_summary`。
- 配置/密钥变化：无。

## 8. 回滚

- 回滚条件：应用启动失败、考勤接口错误率上升、半天数据无法回读、工资扣款异常。
- 回滚命令/步骤：先评估是否已有小数考勤；有小数时禁止执行 135 回滚，恢复迁移前备份或保留 DECIMAL 并回退兼容版本。应用回滚使用后端 `rollback-prod.sh` 和 Web 上一 release 链接。
- 回滚验证：健康接口、考勤读取、工资单只读与日志错误率。
- 实际是否回滚：否。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是（开发与本地验证）
- 返工原因：无
- 最终结论：已发布，系统冒烟通过；体验版真实账号业务验收待执行

## 10. 复盘与沉淀

- 是否出现重复问题：否
- 是否需要新增门禁：是，已补迁移小数类型测试与 App 薪酬闭环契约
- 候选 `EXP/ANT/PATH`：待发布后复盘
- 后续动作与负责人：管理员设置 App 1.0.88 体验版；用户完成半天请假录入与工资扣款业务验收
