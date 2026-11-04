# 2026-11-04 工资项存在考勤状态回填草案

## 目的

修正历史数据中“同企业、同员工、同工资月份已存在工资项，但考勤仍为 `Confirmed`”的状态不一致，使其反映真实锁定状态 `InPayroll`。

## 前置门禁

1. 用户确认执行窗口和范围。
2. 全量备份 `db_payroll_attendance_summary`、`db_payroll_sheet`、`db_payroll_item`。
3. 先执行只读核对，确认差异数量、工资单状态和工资项状态。
4. 不在 `Pending/Confirmed/PartialPaid/Paid/Closed` 工资单上修改金额或业务事实，仅同步考勤状态。
5. 回填后由 Web/App 真实页面复核，不以 SQL 成功代替业务验收。

## 1. 只读核对

```sql
SELECT
    s.period,
    s.sheet_id,
    s.status AS sheet_status,
    i.item_id,
    i.status AS item_status,
    a.summary_id,
    a.emp_id,
    a.status AS attendance_status
FROM db_payroll_attendance_summary a
JOIN db_payroll_sheet s
  ON s.com_id = a.com_id
 AND s.period = a.payroll_month
JOIN db_payroll_item i
  ON i.com_id = a.com_id
 AND i.sheet_id = s.sheet_id
 AND i.emp_id = a.emp_id
WHERE a.status = 'Confirmed'
ORDER BY s.period, a.emp_id, i.item_id;
```

预期：新代码发布前，历史现金额可能存在差异；发布后新生成数据不应再出现此差异。

## 2. 差异计数

```sql
SELECT
    s.period,
    s.status AS sheet_status,
    COUNT(DISTINCT a.summary_id) AS affected_attendances
FROM db_payroll_attendance_summary a
JOIN db_payroll_sheet s
  ON s.com_id = a.com_id
 AND s.period = a.payroll_month
WHERE a.status = 'Confirmed'
  AND EXISTS (
      SELECT 1
      FROM db_payroll_item i
      WHERE i.com_id = a.com_id
        AND i.sheet_id = s.sheet_id
        AND i.emp_id = a.emp_id
  )
GROUP BY s.period, s.status
ORDER BY s.period, s.status;
```

## 3. 回填前备份

```sql
CREATE TABLE bak_payroll_attendance_inpayroll_20261104 AS
SELECT DISTINCT
    a.summary_id,
    a.com_id,
    a.emp_id,
    a.payroll_month,
    a.status AS before_status
FROM db_payroll_attendance_summary a
JOIN db_payroll_sheet s
  ON s.com_id = a.com_id
 AND s.period = a.payroll_month
WHERE a.status = 'Confirmed'
  AND EXISTS (
      SELECT 1
      FROM db_payroll_item i
      WHERE i.com_id = a.com_id
        AND i.sheet_id = s.sheet_id
        AND i.emp_id = a.emp_id
  );
```

## 4. 状态回填

```sql
UPDATE db_payroll_attendance_summary a
JOIN bak_payroll_attendance_inpayroll_20261104 b
  ON b.summary_id = a.summary_id
SET a.status = 'InPayroll'
WHERE a.status = 'Confirmed';
```

## 5. 回填后核对

```sql
SELECT
    a.summary_id,
    a.emp_id,
    a.payroll_month,
    a.status
FROM db_payroll_attendance_summary a
JOIN bak_payroll_attendance_inpayroll_20261104 b
  ON b.summary_id = a.summary_id
WHERE a.status <> 'InPayroll';
```

预期返回 0 行。

## 6. 回滚

```sql
UPDATE db_payroll_attendance_summary a
JOIN bak_payroll_attendance_inpayroll_20261104 b
  ON b.summary_id = a.summary_id
SET a.status = b.before_status
WHERE a.status = 'InPayroll';
```

回滚仅在回填后未发生新的正常考勤状态流转时执行；否则按 `summary_id` 和审计时间人工确认范围。

## 执行状态

- 生产执行：未执行。
- 需要用户确认：是。
- 备份要求：是。
