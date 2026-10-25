# B2 · 财务月度考勤跨端完整对齐研发任务

## 状态

- 批次：`B2`
- 当前状态：`RECONCILED`（黑盒 PASS，待用户验收）
- 变更级别：L3（权限、员工薪酬事实）
- 前置：H1 完成
- 编码授权：已由用户“下一步”指令承接进入开发
- DDL/迁移：预计无

## 目标

让 Web 月度考勤与 App 使用同一薪酬方案、同一考勤事实和同一权限矩阵。

## 范围

### Web API 与模型

新增并使用：

```text
payroll/compensation/info { empId, month }
attendance/summary/info { summaryId }
attendance/summary/delete { summaryId }
```

函数命名与 App 对齐：`compensationForMonth`、`attendanceInfo`、`deleteAttendance`。

### 应出勤派生

- 员工或工资月份变化时读取当月有效薪酬方案；
- 请求增加竞态保护，防止快速切换串值；
- 有方案时只读展示 `scheduledDays`；
- 无方案时提示“未找到当月生效方案”，不得回退 22；
- 保存时后端继续作为唯一快照来源。

### 已有考勤

- 点击历史记录进入编辑；
- 回填保存时快照和全部异常字段；
- 支持删除未进工资单的考勤；
- 已进工资单时禁止编辑/删除并给出恢复路径；
- 确认后提示需重新生成同月工资单。

### 字段

Web 补齐正常出勤、事假、病假、旷工、外勤、年假、调休和备注。

### 月份上下文

工资单确认阻断项“去确认考勤”必须携带工资单 `period`；历史月份编辑、确认和删除不得误操作当前月。

### 后端权限收口

`BR-038` 允许 BOSS、Finance、StoreManager 本店录入、编辑、删除和确认考勤。

- `attendance/summary/save` 允许 StoreManager 本店；
- `attendance/summary/delete` 允许 StoreManager 本店；
- 不得扩大到跨店；
- 保持 Finance 门店范围。

## 验证

- 麦鸿 27 天方案选择员工后显示 27；
- 无方案、跨月份、快速切换员工不串值；
- 新增、编辑、删除、确认均回读服务端；
- 店长本店成功，跨店 404/403；
- Finance 本店成功；
- 历史工资单跳转月份正确；
- App 同场景回归不退化。

## 不做

不改薪酬方案维护；不改工资单确认与发放；不修改历史考勤 `normalDays`；不做 DDL。
