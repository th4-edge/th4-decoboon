# EMG-V2 · 员工生命周期研发任务

| 项 | 内容 |
| --- | --- |
| 研发任务 | `DEV-PI-20261022-005 / EMG-V2` |
| 关联问题 | `PI-20261022-005` |
| 冻结契约 | `EMG-V0-企业管理组织跨端契约.md` |
| 业务裁决 | `DEC-085` / `EV-353`；`DEC-081` / `DEC-082` / `DEC-070` |
| 状态 | `PASS`（独立验收 18/18 P0 + 2/2 follow-up defects CLOSED） |
| 日期 | 2026-10-22 |
| 变更级别 | L2（权限、组织语义、跨端） |

## 1. 目标

在不恢复 Team/TeamLeader 的前提下，完成 App/Web/后端员工邀请、调岗、调店、停用、恢复和离职闭环，并确保离职审计记录实际管理员。

## 2. 范围

### 后端

- 财务邀请目标门店；
- 店长邀请角色限制；
- 恢复员工席位、Active 门店、订阅可写校验；
- 仅 BOSS 可设置店长；
- 管理员离职审计操作人修复；
- Team 冻结回归。

### App

- 成员列表操作范围；
- 邀请岗位与财务门店；
- 员工详情调岗/调店边界；
- 停用、恢复、离职和强制离职确认；
- “注销”统一改为“离职”。

### Web

- 成员状态筛选；
- 调岗/调店、停用、恢复、离职；
- BOSS/店长范围与目标门店；
- 管理员确认和错误反馈。

## 3. 不变量

- BOSS 全企业；店长本店；普通员工本人；
- 店长不能操作自己、BOSS、其他店长；
- 店长不能邀请或设置店长；不能邀请 TeamLeader；
- 跨店调整仅 BOSS；
- 停用/离职立即撤销访问；
- 恢复校验席位与门店；
- 历史业务归属和门店快照不回溯；
- Team/TeamLeader 继续冻结；
- 管理员代办离职 `operator=管理员`。

## 4. 验证

- 后端定向测试：26 tests / 0 failures / 0 errors；
- App `type-check`、`check:navigation`、隔离 `build:mp-weixin`；
- Web `check:permissions`、`check:workbenches`、隔离 `type-check` / `vite build`；
- 真实 HTTP/SQL：BOSS 调岗调店、店长跨店 403、店长提升店长 403、店长本店调岗、停用/恢复登录、离职原因落库、审计操作人正确；
- App/Web 真实运行态截图与脚本证据。

## 5. 证据路径

- 领域对账：`10-reconciliation.md`
- 实现记录：`06-implementation.md`
- UI 复核：`07-ui-review.md`
- 验收交接：`08-acceptance.md`
- 证据包：`docs/acceptance/runs/20261022-emg-v2-employee-lifecycle/`

## 6. 回滚

- 回退本切片后端、App、Web 改动；
- 不恢复 TeamLeader；
- 不删除历史员工、成员日志或 Team 数据；
- 无 DDL、无生产数据、无生产发布。

## 7. 下一门禁

`ACC-PI-20261022-005-EMG-V2` 已独立验收 PASS；下一批可重新对账后准备 `EMG-V3 组织影响与交接`。问题整体仍为 `DEV_IN_PROGRESS`，不得 `CLOSED`。
