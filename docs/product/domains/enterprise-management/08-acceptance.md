# 企业管理 · EMG-V2 独立验收

## 1. 当前结论

- 批次：`ACC-PI-20261022-005-EMG-V2`
- 状态：`PASS`
- 独立验收门禁：`PASS`
- P0：`18 PASS / 0 FAIL / 0 BLOCKED`
- Follow-up defects：`2 CLOSED / 0 OPEN`
- 问题总状态：`DEV_IN_PROGRESS`，不得关闭 `PI-20261022-005`
- 下一门禁：准备 `EMG-V3 组织影响与交接`

## 2. 已验证范围

- BOSS/店长财务邀请与岗位、门店边界；
- BOSS 跨店调岗调店、店长本店调岗、跨店和提升店长拒绝；
- 店长不可操作自己/BOSS/其他店长；
- 停用、恢复、停用门店恢复拒绝、重复动作；
- 在手责任普通离职 409 与确认强制离职；
- 离职原因、终态、历史归属和成员日志；
- 管理员代办离职操作人；
- 跨企业对象；
- Team/TeamLeader 冻结；
- App/Web 页面事实与接口同源。

## 3. Follow-up 缺陷关闭

| ID | 级别 | 独立复验 | 状态 |
| --- | --- | --- | --- |
| `EMG-V2-DEF-01` | P1 | 店长本人取消、BOSS 代取消均成功；DB `Cancelled`；席位 `3 -> 2`；Pending `1 -> 0` | CLOSED |
| `EMG-V2-DEF-02` | P2 | 店长无统计请求/权限告警；BOSS 统计请求 200 且组织报告正常 | CLOSED |

## 4. 证据

`docs/acceptance/runs/20261023-emg-v2-independent-acceptance/`

- 结论：`README.md`
- 逐用例：`http/case-results.md`、`http/case-results-full.json`
- HTTP：`http/*.json`
- SQL：`sql/*.txt`
- 页面：`screenshots/*`、`web/*`、`app/*`
- 缺陷：`defects.md`
- Follow-up：`followup/README.md`、`followup/def01-results.json`、`followup/web-def02-results.json`
- 完整性：`SHA256SUMS.txt`

## 5. 边界

- 未修改任何实现代码或测试；
- 未恢复 Team/TeamLeader；
- 未操作生产或发布正式版；
- 未停止共享 MySQL，未删除验收库。

## 6. EMG-V2.2 生产邀请闭环补充（2026-10-27）

- 后端全量：`245` 套件 / `949` 用例 / `0` 失败 / `0` 异常。
- 新增邀请 Provider 主体注入和取消完整员工加载回归：PASS。
- App 1.0.72：邀请契约、类型检查、release 构建、分包检查 PASS。
- Web 生产基线：类型检查、业务权限、工作台门禁、生产构建 PASS。
- 既有 18/18 独立验收仍作为邀请角色、门店、幂等和取消业务边界的依据。
- 待真实被邀请人完成生产接受后，才可将“接受后正确工作台”标记为生产 PASS。
- App 全量 `verify:mp-weixin` 仍被与本批无关的首页快捷入口历史契约失败阻断；涉及文件与 `origin/prod` 完全一致。
