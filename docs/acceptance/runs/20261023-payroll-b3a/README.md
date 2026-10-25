# B3A 工资单现有执行链跨端对齐验收执行包

- 状态：`USER_ACCEPTED_NON_INDEPENDENT / BLACK_BOX_PASS`
- 批次：`B3A`
- 变更级别：L3
- 生产发布：未执行
- DDL：无

## 范围

1. Web 接入 `payroll/sheet/info`；
2. 工资项补齐基本工资、提成、奖金、补贴、考勤扣款、社保、个税、其他扣款、应发、扣款、实发、状态与归属；
3. BOSS 删除 Draft 工资单，二次确认；
4. 确认前二次确认，说明生成应付、计成本、工资条发放后可见；
5. 确认按钮仅 BOSS 可见；
6. 历史工资单跳考勤使用工资单月份；
7. 确认后重新读取工资单和工资项。

## 不做

- 不实现 `Draft -> Pending` 复核；
- 不实现作废、退回重算；
- 不修改员工发放权限；
- 不修改提成解耦；
- 不做 DDL。

## 证据

- 黑盒验收：`02-black-box-acceptance.md`
- API：`api-results.json`
- HTTP：`http/`
- SQL：`sql/`
- 截图：`screenshots/`
- 截图哈希：`screenshot-sha256.txt`
- 环境：`environment.txt`
- 可复跑脚本：`run_b3a_acceptance.py`

## 门禁

- 黑盒验证 PASS；
- 隔离 JDK22 全量 `932/0/0/0`；
- 用户已于 2026-10-23 以“下一步”承接；
- 独立评审仍未执行，不得把本包写成独立验收；
- B3B/B4 继续由 `GAP-060`、员工发放权限和复核/作废设计阻塞。
