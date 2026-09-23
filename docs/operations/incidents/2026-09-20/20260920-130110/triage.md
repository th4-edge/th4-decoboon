# 生产监控处置记录｜2026-09-20 13:01:11 +08:00

## 结论

- 采集命令：`bash scripts/prod-monitor.sh collect "35 minutes ago"`
- 退出码：`20`
- 处置策略：仅归档证据并给出人工处理建议；不修改源码、不创建源码快照、不重启服务、不构建、不发版。
- 服务状态：`decoboon.service` enabled/active/running，PID `47233`，启动于 `2026-09-18 13:10:08 CST`。
- 当前版本：`/opt/decoboon/releases/20260918-130915`。
- 健康检查：HTTPS `/api/auth/session/info` 返回 HTTP 200；未携带 token 时返回 `tokenIsUndefined`，符合未登录语义。
- 资源：根分区使用率 17%，内存 available 约 4.9 GiB，未见磁盘、内存、端口或服务状态异常。
- 采集说明：首次本地沙箱执行因 SSH 网络权限失败，报告目录 `20260920-130049`；批准网络访问后重跑成功，后续结论以 `20260920-130110` 为准。

## 时间窗口复核

- `analysis.json` 汇总为 unknown 62 条、expected 28 条；应用错误日志采集使用 `tail -n 1500`，未按 35 分钟窗口过滤，因此退出码仍会受历史日志影响。
- `journalctl -u decoboon --since "35 minutes ago"` 无条目。
- 当前窗口内唯一应用日志是 `2026-09-20 13:01:11.627` 的健康检查 `tokenIsUndefined`，属于 expected 噪音。
- 本次提取到的非预期异常最近发生于 `2026-09-20 04:20:50.738`，早于窗口开始约 8 小时；窗口内没有新的异常堆栈、数据库、Redis、磁盘或内存故障。

## 本次重点事件与建议

### 扫描探测与未知路由

- 指纹包括 `.env`、`dev/.env`、`staging/.env`、`v1/v2/v3/.env`、`graphql`、`config`、`mcp`、`kconfig`、`host.describe`、`getServices`、`chunkplayer/index.html` 等。
- 判断：路径形态与公网漏洞扫描探测一致；应用仅返回“找不到对应的处理服务”，没有源码调用方或数据影响证据。
- 建议：在 Nginx/WAF/云安全组侧统一拦截常见探测路径并限制异常路径；不要为探测请求新增业务 Provider，也不要放宽鉴权。
- 状态：保持 `open`，按安全日志而非代码缺陷跟踪。

### INC-20260920-0cd74dd730：`auth/signin` 未知路由

- 现象：`auth/signin处理失败:找不到对应的处理服务:auth/signin`，共 11 条，集中在 `2026-09-18 14:05:25` 至 `14:05:32`。
- 判断：不在本次窗口内，也没有异常堆栈或源码调用证据，无法确认是前端旧入口、攻击探测还是路由缺失。
- 建议：人工核对生产 App 是否有调用 `/api/auth/signin`；若没有，按扫描或陈旧客户端处理；若有，再由开发 lane 单独定位调用链。
- 状态：保持 `open`。

### INC-20260920-a1233597b0：历史懒加载 fixed 基线再次被改写

- 现象：`registry.json` 中历史已修复的 `INC-20260916-a1233597b0` 被改写为 `INC-20260920-a1233597b0`，状态变为 `open`，累计计数继续增加。
- 对照证据：`docs/operations/incidents/2026-09-16/20260916-130232/triage.md` 记录该事故已于 `/opt/decoboon/releases/20260916-053143` 修复；本次窗口无该指纹新增日志。
- 根因线索：`scripts/prod-monitor-analyze.py` 的 `canonicalize_existing()` 使用当前日期重建 incident ID，并把历史记录状态强制改成 `open`；`update_registry()` 又按历史 `count` 累加。
- 判断：这是监控工具的数据完整性问题，不是生产代码回归。由于本次 `collect` 为退出码 20，按安全门禁未修改脚本。
- 建议：人工修脚本，保留 stable incident ID、`fixedAt` 和 `fixedRelease`；对“已修复时间晚于日志时间”的历史记录不得重新计为本次可处理异常，并补回归测试。
- 状态：保持人工待处理。

### 存量人工事项

- `Invite.telNo` 非空失败本次未出现在快照中，仍保持 `open`，等待邀请手机号字段绑定修复。
- 离职/停用员工 token、授权过期分类、成员自操作和退出企业 30 天冷却规则均在更早快照中出现，本次窗口无新增；继续等待人工确认或产品处理。
- 不建议把明确业务拒绝、缺 token、授权过期或扫描探测直接判为代码缺陷。

## 变更清单

- 写入本 triage、development worklog、shared 交接和 automation memory。
- `collect` 自动更新了 `registry.json` 的计数、最近时间、状态和 `reportPaths`。
- 未修改业务源码、测试、脚本、配置或数据库。
- 未创建源码快照、未构建、未部署、未重启、未标记任何事故为 fixed。
