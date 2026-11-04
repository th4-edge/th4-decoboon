# CHG-20261104-009 · 考勤应出勤天数整数契约修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261104-009` |
| 类型 | 热修 / Provider 入参契约 |
| 风险级别 | L3 |
| 状态 | 修复已完成并验证；18条P0为17 PASS / 1 FAIL，因独立门店范围P0暂不发布 |
| 负责人 | Codex |
| 日期 | 2026-11-04 |
| 关联缺陷 | `DEF-FIN-ATTENDANCE-SCHEDULED-DECIMAL-001` |
| 关联门禁 | `CHG-20261104-002`、`TC-FIN-ATTENDANCE-DECIMAL-027` |
| 业务来源 | `BR-015`、`BR-038`；应出勤必须为 1 至 31 的整数 |

## 2. 目标与范围

- 目标：阻止 Provider 将小数应出勤静默截断为整数后落库。
- 范围内：`attendance/summary/save`、`attendance/summary/update`、统一考勤校验、对应单元测试。
- 非范围：实际考勤天数小数能力、应出勤金额公式、DDL、历史工资重算、已发放工资更正、Web/App 控件改造。
- 受影响端：后端；Web 与 App 继续复用同一 Provider 契约。

## 3. 根因

- Save/Update 的 `scheduledDays` 直接声明为 `Integer`；
- JSON 小数在进入业务校验前已被反序列化截断；
- 后续“是否 1 至 31 的整数”校验只能看到截断后的整数，无法识别原始 `26.5`。

## 4. 修复原则

- Provider 先以 `BigDecimal` 接收原始精度；
- 使用 `intValueExact()` 同时验证整数性和范围；
- `26`、`26.0`、`26.00` 视为合法整数；
- `26.5`、`0.5`、`32.5` 等返回 400；
- 非法输入不得创建、修改考勤，不得产生工资项；
- Save 和 Update 必须共用同一校验方法。

## 5. 验收条件

| 序号 | 场景 | 预期结果 |
| --- | --- | --- |
| 1 | Save 传 `scheduledDays=26` | 成功并落库 26 |
| 2 | Save 传 `scheduledDays=26.00` | 成功并落库 26 |
| 3 | Save 传 `scheduledDays=26.5` | 400，未创建考勤 |
| 4 | Update 传 `scheduledDays=26.5` | 400，原考勤不变 |
| 5 | 实际考勤 `21.5/0.5` | 仍保存、确认并计算扣款 |
| 6 | 财务18条P0 | 全部 PASS，最终门禁退出码 0 |

## 6. 验证记录

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 定向测试 | `mvn -q -Dtest=PayrollAttendanceScheduledDaysTest test` | PASS |
| 后端全量 | `mvn -o -q test` | PASS：316类 / 1252用例 / 0失败 |
| Provider 契约 | `audit-provider-response-contract.py --check` + `--self-test` | PASS：findings=0 |
| 修复后运行态定向 | Save/Update传26.5 | PASS：均400且无副作用 |
| 修复后合法整数 | Save传26.0、Update传27.0 | PASS：落库26/27 |
| 修复后实际小数 | 26.5正常、0.5事假 | PASS：26.50/0.50保持 |
| 财务18条P0 | `RUN-20261104-FIN-P0-SCHEDULED-INTEGER-RETEST-001` | 应出勤缺陷PASS；新发现独立P0 `DEF-FIN-STORE-PAYROLL-MIXED-SCOPE-001`，最终门禁FAIL |

## 7. 发布与回滚

- 本修复不包含 DDL 和数据迁移；
- 发布前锁定不可变制品及回滚点；
- 应用回滚使用上一后端 release；
- 已存在合法整数应出勤数据不受影响。
