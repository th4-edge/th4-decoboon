# INC-20261104-ATTENDANCE-DECIMAL 月度考勤请假天数不支持小数

## 1. 分类与影响

- 分类：`code_bug`
- 发现时间：2026-11-04
- 影响端：Web/App 财务月度考勤；后端工资扣款
- 影响：实际存在 0.5 天请假，但页面、接口和数据库只接受整数，半天无法录入，工资扣款可能按 0 天计算。
- 数据影响：修复前已保存的整数考勤不会被本次迁移改写；历史半天诉求需要用户重新录入。
- 紧急处置：从 `prod` 建独立热修分支；DDL 前备份考勤表；先迁移后发布兼容代码。

## 2. 根因

- App/Web 输入限制与前端校验固定为整数。
- `AttendanceSummary`、Save/Update Provider 使用 `Integer`。
- MySQL 七列均为 `INT`。
- 确认合计校验和工资扣款均按整数处理。

## 3. 关联

- 变更：`CHG-20261104-008`
- 后端修复：`fe1bb1c4`
- Web 修复：`7695afd`
- App 修复：`8765af2`
- 热修分支：`hotfix/attendance-decimal-days-20261104`

## 4. 发布与回滚

- 发布状态：后端/Web 已上线；App 1.0.88 开发版已上传
- 生产 release：后端 `/opt/decoboon/releases/20261104-142823`；Web `/opt/decoboon-web/releases/20261104-142959`
- 回滚点：后端 `/opt/decoboon/releases/20261104-123233`；Web `/opt/decoboon-web/releases/20261104-134154`
- 回滚条件：应用启动失败、考勤接口错误率上升、半天数据无法回读、工资扣款异常
- 回滚保护：已有小数数据时禁止执行迁移 135 回滚，避免截断

## 5. 闭环结果

- 生产迁移：`135` 已登记 `SUCCESS`；考勤表 14 行迁移前后不变；七列均为 `DECIMAL(5,2)`。
- 备份：`/opt/backups/th4-decoboon/attendance-decimal-20261104-142809/db_payroll_attendance_summary.sql.gz`，SHA-256 `1cb2057c…`。
- 发布：后端 `/opt/decoboon/releases/20261104-142823`；Web `/opt/decoboon-web/releases/20261104-142959`；App 1.0.88 开发版已上传。
- 系统冒烟：API 健康 HTTP 200；Web 入口、主包资产与含考勤小数文案的懒加载资产均 HTTP 200；生产表事务内验证 `15.00 → 0.50 → ROLLBACK` 后恢复 `15.00`；发布后日志仅出现预期缺 token 检查，无新增考勤异常。
- 事故状态：RELEASED / SYSTEM_SMOKE_PASSED；真实登录态半天录入和工资扣款业务验收待用户完成。
