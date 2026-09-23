# 生产监控处置记录｜2026-09-16 13:02:32 +08:00

## 结论

- 采集命令：`bash scripts/prod-monitor.sh collect "35 minutes ago"`
- 退出码：`20`
- 处置策略：只归档证据并给出处理建议；不修改源码、不重启服务、不构建、不发版。
- 服务状态：`decoboon.service` enabled/active/running，PID `33085`，启动于 `2026-09-16 06:59:33 CST`。
- 健康检查：HTTPS `/api/auth/session/info` 返回 HTTP 200，未携带 token 时返回 `tokenIsUndefined`，符合未登录语义。
- 资源：根分区使用率 15%，内存 available 约 5.3 GiB，未见基础设施异常。
- 采集说明：首次本地沙箱执行因 SSH 网络权限失败（报告目录 `20260916-130209`）；批准网络访问后重跑成功，后续结论以 `20260916-130232` 为准。

## 待处理事项

### INC-20260916-cd863ee0f9 / INC-20260916-a8b87027fc：员工邀请手机号未绑定

- 原始时间：`2026-09-16 12:30:13.247`
- 入口：`invite/invitation/save`
- 现象：事务提交时发生 `DataIntegrityViolationException`，根因为
  `PropertyValueException: not-null property references a null or transient value: com.th4.edge.tenant.user.model.Invite.telNo`。
- 证据链：
  - 小程序调用方发送 `telNo`：`../../前端代码/th4-decoboon-app/src/domains/identity/api.ts:327`
  - Provider 注入的字段名却是 `invitePhone`：`src/main/java/com/th4/edge/tenant/user/service/invite/invitation/save/support/Save.java`
  - `MembershipContext.createInvite` 将 `invitePhone` 写入 `Invite.telNo`：`src/main/java/com/th4/edge/tenant/user/context/MembershipContext.java`
  - 因此请求中的 `telNo` 未被读取，最终以 null 写入非空列。
- 影响：企业员工邀请创建失败；本次未发现脏数据写入。
- 自动处置：禁止。本次 `collect` 退出码为 `20`，虽然源码证据已指向明确的请求字段绑定缺陷，但不满足本轮自动修复门禁。
- 建议人工修复：
  1. 先补稳定失败的 Provider/集成回归测试，请求体使用前端真实契约 `telNo`。
  2. 以最小改动统一绑定名，例如将 `invitePhone` 改为 `telNo`，或使用框架支持的 `@AutoInject("telNo")`。
  4. 定向测试通过后按标准流程运行全量测试、构建、部署和发版后监控。
- 状态：保持 `open`，等待人工确认或下一次符合自动修复门禁的运行。

### INC-20260916-060771017b：授权过期错误被计为未知异常

- 原始时间：`2026-09-16 12:29:27.868`
- 入口：`statistic/boss/dashboard`
- 现象：`访问受限，授权已过期，请重新登录后再访问。`
- 判断：这是典型的会话过期业务结果，不是服务端代码缺陷；当前仅 2 条，同一秒出现，未持续增长。
- 建议：人工确认后把该文案纳入监控的 expected 业务噪音；若产品要求降低日志级别，应单独走日志规范变更，不与本次邀请缺陷混修。
- 状态：保持 `open`，不自动修复。

### INC-20260916-cddd8368fa：未知 config 路由探测

- 首见：`2026-09-16 06:33:46.274`；本次窗口无新增时间。
- 现象：`config处理失败:找不到对应的处理服务:config`
- 判断：历史单点请求，倾向外部扫描器噪音；当前仓库未发现 `config` 业务 Provider 调用方。
- 建议：继续观察；若确认无业务调用方，可将其与 `.env` 探测一起纳入 expected 规则。不要为此增加业务路由或放宽鉴权。
- 状态：保持 `open`。

## 历史事故复核

- `INC-20260916-a1233597b0` 保持 `fixed`。
- 本次分析仍显示其历史累计 22 条，但最近发生时间为 `2026-09-16 06:04:02.211`，早于修复时间 `2026-09-16 06:53:34`；本次 35 分钟窗口无新增指纹。

## 变更清单

- 未创建源码快照。
- 未修改源码、测试、配置、数据库或生产文件。
- 未执行构建、部署或重启。
