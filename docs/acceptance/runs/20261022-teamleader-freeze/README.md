# PI-20261022-002 · TeamLeader 冻结存量收口验收

- 日期：2026-10-22
- 业务裁决：`DEC-082` / `ANS-BIZ-20261022-003` / `T12-BIZ-20261022-003`
- 结论：独立验收 PASS；无 P0/P1，尚有 1 项非阻塞 P2 文案差异
- 变更级别：L3（角色权限、登录、存量兼容）
- 数据库：无 DDL、无迁移、无历史数据改写

## 1. 生产存量基线

只读盘点（`EV-341`）：

| 指标 | 数量 |
| --- | ---: |
| 在职员工 | 10 |
| TeamLeader 账号 | 0 |
| TeamLeader 邀请 | 0 |
| TeamLeader 领导团队 | 0 |
| TeamLeader 名下客户/房屋/报价/项目/任务 | 0 |

结论：当前生产无真实 TeamLeader 账号需要迁移或交接；恢复旧库仍按硬冻结兜底。

## 2. 已实现收口

- 服务端 `EmployeeRolePolicy` 只允许六类目标角色进入应用；TeamLeader 统一 403。
- 密码登录、微信登录、首次设密、旧令牌、session/info、邀请接受、岗位调整均拒绝 TeamLeader。
- 所有 Security Provider 在订阅/续费/文件授权豁免前先过 `TenantContext` 角色门禁。
- 邀请创建、接受、改岗不得产生 TeamLeader；BOSS 可将历史 TeamLeader 改为六类目标角色。
- App 主动清理旧缓存 TeamLeader 会话；session/info 403 清会话并提示联系 BOSS 改岗。
- App/Web 不装载 TeamLeader Tab、快捷动作、首页、菜单或业务路由。
- Web 邀请与员工筛选不再提供 TeamLeader；历史员工仍解码显示“团队负责人”。
- 历史客户、项目、任务、团队领导指针不自动改写。

## 3. 验证记录

| 层级 | 命令 | 结果 |
| --- | --- | --- |
| 后端定向 | `mvn -q -Dtest=EmployeeRoleContractTest,TenantInternalRoleTest,FinanceOrganizationRulesTest,PasswordSessionAcceptanceTest,DataScopeRequestFilterIntegrationTest,SubscriptionAccessFilterAcceptanceTest,OwnerPreviewInfoAcceptanceTest test` | PASS，43/43 |
| App 契约 | `npm run check:navigation` | PASS |
| App 类型/构建 | `npm run type-check` + `npm run build:mp-weixin` + `npm run check:subpackages` | PASS |
| Web 治理 | `npm run check:workbenches` + `npm run check:workspaces` | PASS |
| Web 构建 | `npm run build`（隔离干净基线 + 本问题补丁） | PASS |
| 后端全量 | `mvn -q -Dtest='!ProviderSecurityAcceptanceTest' test` | 业务测试 950/950 PASS；首轮 5 个用例仅因隔离快照未带 `docs/` 缺少文件，补回后 5/5 单独复跑 PASS |
| Provider 安全清单 | `mvn -q -Dtest=ProviderSecurityAcceptanceTest test` | 已知无关失败：Provider 总数基线 518、当前 517；本问题未增删 Provider |
| 生产只读复核 | TeamLeader 账号/邀请/团队/客户/项目计数查询 | PASS，均为 0（2026-10-22 两次查询） |

### 构建隔离说明

当前共享工作区存在其他专项会话的未提交财务改造。Web 在隔离干净基线上叠加本问题文件后验证；后端全量在独立快照中运行，避免并行 `target` 写入污染。`ProviderSecurityAcceptanceTest` 的 Provider 总数基线仍为 518，而并行工作区实际为 516，属于无关未提交改动，不计入本问题失败，但发布前必须由该改动责任方更新并复验。

## 4. 独立验收

- 首轮：FAIL，发现首次设密旁路、邀请接受切换旁路、App 缓存会话漏网、Web 邀请选项和历史标签问题。
- 修复后复审：**PASS**。无 P0/P1；唯一 P2 为 App 弹窗标题“账号状态已变更”与说明中的“岗位状态”措辞不完全一致，正文已明确给出冻结原因和改岗恢复路径，不影响权限与安全结论。
- 独立验收实际覆盖：后端直接相关 42/42；App `check:navigation` + `type-check`；Web `check:workbenches` + `check:workspaces` + `type-check`。
- 验收方未修改任何源码、配置、迁移或问题档案。

## 5. 剩余风险

- `file/private/download` 是带时效签名公开路由；冻结前已签发的短时下载链接在有效期内仍可用。该路由不承载 TeamLeader 应用入口，但如需立即撤销历史链接需额外机制。
- 生产 TeamLeader 为 0，真实生产账号切换、改岗和历史归属保持只能通过恢复性测试数据验证。
- 发布后仍需复核 TeamLeader 计数未回增，并保留发布前后员工角色与核心归属计数证据。
