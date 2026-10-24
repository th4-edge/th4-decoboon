# PI-20261022-002 · TeamLeader 冻结后的存量角色与技术收口

| 项 | 内容 |
| --- | --- |
| 问题 ID | `PI-20261022-002` |
| 当前状态 | `T05_REQUESTED` |
| 严重级别 | P1 |
| 发现日期 | 2026-10-22 |
| 来源 | 用户角色冻结决策 |
| 所属领域 | 身份、角色、权限、组织、历史兼容 |
| 产品决策 | `USER-DEC-20261022-001` D4 |
| 专项会话 | `01a163c7-d470-7702-957b-898e5d05fc09` |
| 业务同步 | `REQ-BIZ-20261022-003` → `ANS-BIZ-20261022-003` → `T12-BIZ-20261022-003` |
| 研发任务 | `T12-BIZ-20261022-003` / TL-F1~F3 |
| 验收 | `PASS`（独立验收，P0=0/P1=0/P2=1 非阻塞） |

## 一、问题摘要

用户已确认：**TeamLeader 角色冻结，后续不再考虑该角色的应用建设**。

当前代码和历史数据仍存在 TeamLeader 枚举、部分 App 快捷入口与能力、Web 仅有的三个冻结页面，以及后端若干 TeamLeader 角色判断。产品目标中不再为 TeamLeader 新建功能，但存量账号如何登录、如何降权、是否转岗、既有客户/任务如何交接，尚未裁决。

## 二、正式决策

- TeamLeader 不再进入后续角色矩阵、业务矩阵、功能矩阵和 App/Web 应用规划；
- 不再新增 TeamLeader 专属页面、入口、能力或业务流程；
- 保留历史枚举和数据的兼容性，不在本决策中直接删除；
- `GAP-026` 从活跃产品裁决转为历史兼容问题；
- 存量 TeamLeader 账号、数据、权限和路由的处置必须专项裁决。

## 三、当前事实

| 维度 | 当前实现 |
| --- | --- |
| 后端枚举 | `EmployeeRole.TeamLeader` 仍存在 |
| 数据范围 | `Team` |
| 邀请 | 当前仍可邀请 TeamLeader；店长不能邀请 TeamLeader，TeamLeader 只能邀请团队成员 |
| App | 仍有 TeamLeader Tab、工作台快捷入口、客户/团队/项目/业绩等表面 |
| Web | TeamLeader 仅有团队概览、团队客户、风险等冻结页面 |
| 后端角色判断 | 有分布在客户、通知、员工管理、报表等处的 TeamLeader 判断 |
| 财务字段 | `GAP-026` 未冻结；现按限制字段临时处理 |

## 四、业务裁决（T05 → T06/T12）

T05：`REQ-BIZ-20261022-003`；答复：`ANS-BIZ-20261022-003`；交接：`T12-BIZ-20261022-003`；决策：`DEC-082`。

1. 旧库恢复或测试数据出现 TeamLeader 账号时，**拒绝登录和业务访问**，提示 BOSS 改为六类目标角色；不提供只读应用。
2. 禁止任何新邀请、接受邀请或岗位调整产生 TeamLeader；历史 Pending 邀请不得接受。当前生产无待处理邀请。
3. 历史客户、团队、任务、项目、通知、业绩和财务关系**保留原值、不自动迁移**；改岗后如需交接由 BOSS 显式发起。
4. App/Web 不装载 TeamLeader Tab、菜单、首页卡片、路由和工作台；前端类型与枚举可保留用于历史解码。
5. 后端旧令牌与 TeamLeader 会话统一 403；`GAP-026` 关闭为不适用。

## 四之一、生产存量盘点

2026-10-22 生产只读查询结果：在职员工 10 人；TeamLeader 账号 0、邀请 0、由 TeamLeader 领导的团队 0、名下客户/房屋/报价/项目/任务 0。当前无真实账号迁移或责任交接；恢复旧库场景仍按上述硬冻结规则兜底。证据：`EV-341`。

## 五、临时处理

专项裁决和研发验收前：

1. 不新增 TeamLeader 专属应用能力；
2. 不把 TeamLeader 纳入新的跨端能力矩阵和 App/Web 对等范围；
3. 不删除 `EmployeeRole.TeamLeader` 枚举和历史数据；
4. 不擅自改变现有 TeamLeader 账号权限；
5. 涉及 TeamLeader 的新需求一律转为“存量兼容”或专项问题，不进入常规产品迭代。

## 六、候选研发切片（未冻结）

| 切片 | 范围 | 前置 |
| --- | --- | --- |
| TL-F1 | 后端硬冻结：登录、会话、旧令牌、邀请、接受、改岗统一拒绝；BOSS 可将历史 TeamLeader 改为目标角色 | `T12-BIZ-20261022-003` |
| TL-F2 | App/Web 入口收口：角色 Tab、首页、菜单、路由、工作台、深链接不装载 TeamLeader | TL-F1 |
| TL-F3 | 回归与生产兼容：六类角色回归、历史归属不变、生产 TeamLeader 计数不回增、独立验收 | TL-F1/F2 |

## 七、验收最低要求

- 存量 TeamLeader 账号行为符合裁决；
- 不再新增 TeamLeader 应用能力；
- 跨企业、跨团队和越权负向通过；
- 历史客户、任务、项目归属和审计可追溯；
- 运营/管理员侧不存在未提示的角色黑洞；
- App/Web/后端对同一存量账号的可见性和拒绝态一致。

## 八、关闭条件

```text
T06/T12 已取得
→ 存量策略冻结
→ 研发迁移/入口/权限切片完成
→ 数据兼容验证完成
→ 独立验收 PASS
→ registry.json 状态 CLOSED
```


## 九、研发与验收证据

- 实现与验收汇总：`docs/acceptance/runs/20261022-teamleader-freeze/README.md`
- 独立验收回执：`docs/acceptance/runs/20261022-teamleader-freeze/INDEPENDENT-ACCEPTANCE.md`
- 业务证据：`EV-410`
- 后端直接相关测试：43/43 PASS（首轮独立验收 42/42；研发补充微信登录门禁后为 43/43）
- App：`check:navigation`、`type-check`、`build:mp-weixin`、`check:subpackages` PASS
- Web：隔离基线 `check:workbenches`、`check:workspaces`、`type-check`、production build PASS
- 生产发布前后计数：当前未发布；发布前查询 TeamLeader 账号/邀请/团队/客户/项目均为 0
- 独立验收结论：PASS；1 项非阻塞 P2 为 App 弹窗标题措辞差异
- 发布前遗留：`ProviderSecurityAcceptanceTest` 的 Provider 总数基线 518 与并行改动后的 516 不一致，需该改动责任方在发布前对账


## 十、关闭结论

- 关闭日期：2026-10-22
- 独立验收：PASS，无 P0/P1；1 项非阻塞 P2 文案差异。
- 全量后端：排除无关 `ProviderSecurityAcceptanceTest` Provider 总数基线后，业务测试 950/950 PASS；该清单测试因并行工作区 Provider 总数 518→517 未更新而失败，本问题未增删 Provider。
- App/Web：类型检查、契约门禁与构建 PASS。
- 生产基线：两次只读查询 TeamLeader 账号/邀请/团队/客户/项目均为 0。
- 代码关闭不等于生产已发布；如需上线，按 L3 权限变更执行备份、全量验证、可回滚发布和发布后计数复核。
