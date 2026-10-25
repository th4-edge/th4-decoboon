# ACC-PI-20261022-005-EMG-V2 · 员工生命周期独立验收

| 项 | 内容 |
| --- | --- |
| 验收批次 | `ACC-PI-20261022-005-EMG-V2` |
| 关联问题 | `PI-20261022-005` |
| 切片 | `EMG-V2 员工生命周期` |
| 业务裁决 | `DEC-085` / `EV-353`、`DEC-081`、`DEC-082`、`DEC-070` |
| 验收类型 | 独立黑盒验收 |
| 当前状态 | `PASS` |
| P0 用例 | `18 PASS / 0 FAIL / 0 BLOCKED` |
| Follow-up 缺陷 | `2 CLOSED / 0 OPEN` |
| 日期 | 2026-10-23 |

## 1. 边界

- 未修改后端、App、Web 源码或测试；
- 未恢复 Team/TeamLeader；
- 未操作生产、未发布正式版；
- 只使用本地 `decoboon_accept` 合成数据。

## 2. 独立结论

18 个 P0 用例全部 PASS，两个 follow-up 缺陷也已完成独立复验关闭。`EMG-V2` 切片结论为 `PASS`。问题 `PI-20261022-005` 整体仍为 `DEV_IN_PROGRESS`，不得关闭。

## 3. 用例结果

| ID | 用例 | 预期 | 结果 |
| --- | --- | --- | --- |
| `ACC-EMG-V2-01` | BOSS 邀请财务不选门店 | 400 | PASS |
| `ACC-EMG-V2-02` | 店长邀请本店财务 | 成功，目标门店固定本店 | PASS |
| `ACC-EMG-V2-03` | 店长邀请其他门店财务 | 403 | PASS |
| `ACC-EMG-V2-04` | 店长邀请岗位 | 无店长/TeamLeader | PASS |
| `ACC-EMG-V2-05` | BOSS 跨店调岗调店 | 成功，日志前后值正确 | PASS |
| `ACC-EMG-V2-06` | 店长本店调岗 | 成功 | PASS |
| `ACC-EMG-V2-07` | 店长跨店调整 | 403 | PASS |
| `ACC-EMG-V2-08` | 店长提升他人为店长 | 403 | PASS |
| `ACC-EMG-V2-09` | 店长操作自己/BOSS/其他店长 | 页面无操作入口，接口拒绝 | PASS |
| `ACC-EMG-V2-10` | 停用员工访问 | 403 | PASS |
| `ACC-EMG-V2-11` | 恢复员工 | 成功；停用门店恢复拒绝 | PASS |
| `ACC-EMG-V2-12` | 有在手责任员工普通离职 | 409，要求交接或强制确认 | PASS |
| `ACC-EMG-V2-13` | 确认强制离职 | `Resigned`，历史归属保留 | PASS |
| `ACC-EMG-V2-14` | 管理员代办离职 | 操作人为管理员 | PASS |
| `ACC-EMG-V2-15` | 跨企业成员/门店 | 404/拒绝，不泄露存在性 | PASS |
| `ACC-EMG-V2-16` | Team/TeamLeader 回归 | 无写入口；历史只读；角色 403 | PASS |
| `ACC-EMG-V2-17` | 重复停用/恢复/离职 | 明确状态错误 | PASS |
| `ACC-EMG-V2-18` | App/Web 双端同源 | 状态、门店、岗位与接口一致 | PASS |

## 4. 关键事实

- 店长跨店、提升店长、操作 BOSS/自己/其他店长均拒绝；
- 停用后登录 403，恢复后登录成功；
- 停用门店上的恢复返回 400，门店恢复启用后无残留；
- 普通离职检测到 1 条在手客户返回 409，强制离职后该客户仍归属原员工；
- 管理员代办离职日志 `operator_id=3001`、目标 `emp_id=3003`；
- Team 列表为空，无 Team 写路由，TeamLeader 邀请和登录均 403；
- Web 店长邀请岗位仅设计师、业务员、项目经理、财务；App 页面目标门店固定为验收总店。

## 5. 缺陷关闭

| ID | 级别 | 独立复验 | 状态 |
| --- | --- | --- | --- |
| `EMG-V2-DEF-01` | P1 | 店长取消本人邀请、BOSS 取消店长邀请均成功；DB `Cancelled`；席位 `3 -> 2`；Pending `1 -> 0` | CLOSED |
| `EMG-V2-DEF-02` | P2 | 店长页面无统计请求/权限告警；BOSS 统计请求 200 且组织报告正常 | CLOSED |

## 6. 证据

目录：`docs/acceptance/runs/20261023-emg-v2-independent-acceptance/`

- 结论与逐用例：`README.md`、`http/case-results.md`、`http/case-results-full.json`
- 真实 HTTP：`http/*.json`
- 只读 SQL 与数据库事实：`sql/*.txt`
- Web/App 页面：`screenshots/*`、`web/*`、`app/*`
- 命令日志：`logs/*`
- 缺陷：`defects.md`
- Follow-up 复验：`followup/README.md`、`followup/def01-results.json`、`followup/web-def02-results.json`
- 完整性：`SHA256SUMS.txt`

## 7. 下一步

1. `EMG-V2` 18/18 P0 与 2/2 follow-up defects 已关闭，可进入 `EMG-V3` 准备；
2. `PI-20261022-005` 整体保持 `DEV_IN_PROGRESS`，不得关闭；
3. 启动 `EMG-V3` 前重新核对领域对账与状态账本；
4. 未完成 EMG-V3~V6 前不得发布生产或宣称问题闭环。
