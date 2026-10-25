# 产品问题独立验收交接 · EMG-V1 Team 冻结收口

| 项 | 内容 |
| --- | --- |
| 关联问题 | `PI-20261022-005` |
| 研发任务 | `DEV-PI-20261022-005 / EMG-V1` |
| 验收批次 | `ACC-PI-20261022-005-EMG-V1` |
| 验收状态 | `PASS` |
| 业务裁决 | `DEC-085` / `EV-353` / `T12-BIZ-20261022-006` |
| 冻结点 | Team 与 TeamLeader 冻结；Team 仅历史只读 |
| 执行方式 | 独立验收会话；真实 Web 浏览器 + 微信开发者工具运行态 + 本地隔离 MySQL + HTTP/SQL 证据 |

## 1. 验收边界

- App/Web/后端范围：Team 入口清理、邀请/成员更新 Team 写入拒绝、历史 Team 只读。
- 明确不验收：Team 创建/维护/归档、Team 经营报表、Team 数据权限、历史 Team 数据清理。
- 不涉及：DDL、迁移、订阅价格、金额/成本/利润。

## 2. 环境与账号

| 项 | 值 |
| --- | --- |
| 后端 | 本地 `8090`；隔离 MySQL `3307/decoboon_accept`；Redis DB 15 |
| Web | Vite `8080`；真实 BOSS 会话 |
| App | 微信开发者工具 `wcc` 自动化端口 `9420`；StoreManager 会话 |
| 企业 A | `验收装企有限公司`；默认总店 + 二店 |
| 企业 B | `他企有限公司`；用于跨租户负向 |
| 账号 | BOSS `13990000001`、店长 `13990000003`、历史 TeamLeader `13990000006`、设计师 `13990000002` |

## 3. 用例结果

| ID | 场景 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- | --- |
| `ACC-EMG-V1-01` | BOSS Web 查看菜单/首页 | 无团队经营/团队客户入口 | 菜单和首页均无 Team 入口 | PASS |
| `ACC-EMG-V1-02` | BOSS Web 打开邀请员工 | 岗位无 TeamLeader | 选项为店长、设计师、业务员、项目经理、财务 | PASS |
| `ACC-EMG-V1-03` | 邀请携带 `teamId` | 400，无邀请落库 | `团队已冻结，不能创建或分配团队` | PASS |
| `ACC-EMG-V1-04` | 成员更新携带 `teamId` | 400，Team 不变 | `团队已冻结，不能调整团队`；员工 Team 仍为 NULL | PASS |
| `ACC-EMG-V1-05` | 不带 Team 创建邀请 | 成功且 `team_id=NULL` | 创建成功；DB `team_id=NULL` | PASS |
| `ACC-EMG-V1-06` | 历史 Team 读取 | 仅历史只读可见 | `user/team/list` 返回 `历史冻结团队`；无 Team 写 Provider | PASS |
| `ACC-EMG-V1-07` | App StoreManager 首页 | 无“门店团队/企业团队/团队成员”入口 | 页面文本无 Team 入口 | PASS |
| `ACC-EMG-V1-08` | App 设计调度 | 原 Team 入口改为“成员管理” | 页面显示“成员管理”，无 Team 管理入口 | PASS |
| `ACC-EMG-V1-09` | App 邀请页 | 无 Team 选择、无 TeamLeader | 两项均不存在 | PASS |
| `ACC-EMG-V1-10` | App Team 列表深链接 | 被权限拦截，不进入 Team 页 | 深链接后仍停留在成员管理/设计调度上下文 | PASS |
| `ACC-EMG-V1-11` | Web Team 概览/Team 客户深链接 | 页面不存在或无权 | 两条深链接均落“页面不存在” | PASS |
| `ACC-EMG-V1-12` | 跨企业员工操作 | 404，不泄露存在性 | `员工不存在` | PASS |
| `ACC-EMG-V1-13` | 跨企业 teamId 参数 | 拒绝，不写入 | 400 `团队已冻结，不能创建或分配团队` | PASS |
| `ACC-EMG-V1-14` | 历史 TeamLeader 登录 | 403 | `团队负责人角色已冻结，请联系企业负责人改为其他岗位` | PASS |
| `ACC-EMG-V1-15` | Team 写路由 | 不存在 | `user/team/save|update|status` 均为 `canNotFoundHandler` | PASS |

## 4. 验收中发现并修复的问题

- **缺陷**：`BossOrganizationView.vue` 的“邀请员工”按钮错误使用 `#actions`，而 `PanelCard` 只暴露 `#extra`，导致按钮未渲染，阻断邀请角色验收。
- **修复**：改为 `#extra`。
- **复测**：真实 Web 页面重新打开邀请弹窗，岗位选项无 TeamLeader，验收继续执行并通过。

## 5. 自动化与构建证据

| 项 | 结果 |
| --- | --- |
| 后端编译 | PASS |
| 后端定向测试 | 32/0/0 PASS |
| App 导航契约 | PASS |
| App type-check | PASS |
| App mp-weixin build | PASS |
| App 运行时验收 | PASS；console errors 0 |
| Web type-check | PASS |
| Web 权限/工作台检查 | PASS |
| Web runtime | PASS；console errors 0 |
| Web production build | PASS |
| 后端隔离全量测试 | BLOCKED：隔离副本编译被并行会话引入的 `QuoteViewContext.regionName` 类型错误阻断，非本切片代码 |

## 6. 证据清单

- `web-boss-home-no-team-entry.png` / `web-boss-home.yml`
- `web-boss-organization.yml`
- `web-boss-invite-roles.png` / `web-boss-invite-roles.yml`
- `web-team-overview-deeplink.yml`
- `web-team-customers-deeplink.yml`
- `app-store-manager-home.png`
- `app-designers-member-entry.png`
- `app-invite-no-team.png`
- `app-team-deeplink-blocked.png`
- `app-team-freeze-acceptance.json`
- `api-team-list.json`
- `api-invite-team-reject.json`
- `api-invite-no-team.json`
- `api-member-team-reject.json`
- `api-teamleader-login-reject.json`
- `api-cross-tenant-member.json`
- `api-cross-tenant-team-param.json`
- `db-after-api.jsonl`
- `backend-targeted-tests.log`
- `backend-isolated-full.log`

## 7. 结论

- `EMG-V1` 功能与安全门禁：**PASS**
- 问题总状态：仍为 `DEV_IN_PROGRESS`，不得因单切片通过标记 `CLOSED`
- 下一片：`EMG-V2 员工生命周期`
- 发布边界：本切片无 DDL；未执行生产发布或生产数据变更
