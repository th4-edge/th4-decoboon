# CHG-20261029-012 · 财务月度考勤应出勤天数支持调整

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261029-012 |
| 类型 | 热修 / 缺陷 |
| 风险级别 | L2 |
| 状态 | 已发布 |
| 负责人 | Codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `USER-DEC-20261029-006`、`BR-038`、`DEC-045` |

## 2. 目标与范围

- 目标：Web 财务月度考勤的应出勤天数仍从当月薪酬方案带出，但允许财务在具体考勤单中调整。
- 范围内：Web 考勤表单、`attendance/summary/save`、`attendance/summary/update`、服务端 1～31 天校验、定向测试。
- 非范围：薪酬方案维护、工资公式、考勤扣款系数、权限矩阵、状态机、DDL、App 交互。
- 受影响端：后端、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及考勤快照和工资计算输入。
- 业务事实来源与裁决编号：`USER-DEC-20261029-006`；沿用 `BR-038`、`DEC-045`。
- 尚存 GAP 或假设置：无；薪酬方案仍是考勤录入前置条件。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 财务新增月度考勤 | 员工当月有生效薪酬方案 | 应出勤自动带出方案值，输入框可编辑 | Web 构建通过；生产页面待验证 |
| 2 | 财务调整应出勤 | 已带出方案值 | 可改为 1～31 内整数并保存，回读为调整值 | `PayrollAttendanceScheduledDaysTest` PASS；生产冒烟待执行 |
| 3 | 非法输入 | 输入 0、负数、超过 31 或非整数 | 前端提示，服务端返回 400，不落库 | 保存/更新负向单元测试 PASS；Web 类型检查 PASS |
| 4 | 工资计算 | 考勤已确认且工资单生成 | 使用考勤单保存的应出勤快照，不反写薪酬方案 | 全量测试回归 PASS |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/payroll-attendance-editable-20261029` | 代码 `9ab0b0f`；制品 `1e34714` | attendance summary save/update、测试、决策记录 |
| th4-decoboon-web | `hotfix/payroll-attendance-editable-20261029` | `49f4b35` | `FinanceAttendanceDrawer.vue` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | `mvn -q -o -Dtest=PayrollAttendanceScheduledDaysTest test` | PASS（5 项） | 本地命令输出 |
| 构建/类型检查 | `npm run type-check && npm run check:client-copy && npm run build` | PASS；权限/门店/工作台门禁 PASS | 本地命令输出 |
| 真实页面/接口冒烟 | `/finance/employee-salaries` 月度考勤 | 待生产执行 | 发布记录 |
| 负向/越权/边界 | 0、32 天校验；现有数据范围测试 | PASS；全量 954/0/0/0 | Surefire 汇总 |

- 未执行项及原因：生产写冒烟需避免污染真实工资数据，优先使用受控测试企业或只读验证。
- 后端全量：954 tests / 0 failures / 0 errors / 0 skipped。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | `20261029-152049` |
| 制品路径/SHA-256 | `th4-decoboon-1.0.0-build.tar.gz` / `747d1fe23c060ecf3c444338e66db580d99fcaedfa807f5668c2b2ff691cd705` |
| 生产 release/版本 | 后端 `/opt/decoboon/releases/20261029-152049`；Web `/opt/decoboon-web/releases/20261029-152049` |
| 上一版本/回滚点 | 后端 `/opt/decoboon/releases/20261027-172000`；Web `/opt/decoboon-web/releases/20261027-162200` |
| 健康检查 | `auth/session/info` HTTP 200；Web `/`、`/login`、`/finance/employee-salaries` HTTP 200；`/v3/` 301 |
| 发布后观察 | 服务 active；线上考勤 chunk 含可编辑输入和 1～31 天校验 |

- 数据库迁移：无；发布前已备份 8 张薪酬/考勤相关表到 `/opt/backups/th4-decoboon/payroll-before-attendance-editable-20261029-152049.sql.gz`（SHA-256 `8279cd0aded7e1d146168872f21cede550a63fa74f0d573cf987489f0b33066c`）
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：考勤保存失败、应出勤被错误覆盖、工资计算输入异常或生产冒烟失败。
- 回滚命令/步骤：后端切回 `/opt/decoboon/releases/20261027-172000` 并重启；Web 执行 `ln -sfn /opt/decoboon-web/releases/20261027-162200 /opt/decoboon-web/current && nginx -t && systemctl reload nginx`。
- 回滚验证：登录与月度考勤页面可用，原薪酬方案与历史考勤不受影响。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是
- 返工原因：无
- 最终结论：已关闭（待财务页面人工确认）

## 10. 复盘与沉淀

- 是否出现重复问题：是，历史热修只改 Web 只读联动，未支持业务调整。
- 是否需要新增门禁：暂不新增；现有定向测试覆盖保存与更新。
- 候选 `EXP/ANT/PATH`：候选 EXP：派生默认值必须支持业务覆盖时，服务端不得再次强制覆盖客户端提交值。
- 后续动作与负责人：已回灌 `prod/test/dev`，三线均指向本热修；请财务刷新生产页面确认输入框可编辑。
