# 生产监控处置记录｜2026-09-22 13:01:46 +08:00

## 结论

- 采集命令：`bash scripts/prod-monitor.sh collect "35 minutes ago"`
- 退出码：`20`
- 处置策略：仅归档证据并给出人工处理建议；不修改源码、不创建源码快照、不重启服务、不构建、不发版。
- 服务状态：`decoboon.service` enabled/active/running，PID `63746`，启动于 `2026-09-21 05:06:10 CST`。
- 当前版本：`/opt/decoboon/releases/20260921-050519`。
- 健康检查：HTTPS `/api/auth/session/info` 返回 HTTP 200；未携带 token 时返回 `tokenIsUndefined`，符合未登录语义。
- 资源：根分区使用率 18%，内存 available 约 4.3 GiB，未见磁盘、内存、端口或服务状态异常。
- 采集说明：首次本地沙箱执行因 SSH 网络权限失败，误生成报告目录 `20260922-130133`；批准网络访问后重跑成功，后续结论以 `20260922-130146` 为准。

## 时间窗口复核

- 窗口约为 `2026-09-22 12:26:46` 至 `13:01:47`。
- `analysis.json` 汇总为 unknown 45 条、expected 44 条。应用错误日志采集沿用 `tail -n 1500`，并未按 35 分钟窗口过滤，因此退出码仍会受历史日志影响。
- `journalctl -u decoboon --since "35 minutes ago"` 无条目。
- 窗口内应用日志只有本次 HTTPS 健康检查在 `13:01:47.234` 产生的 expected `tokenIsUndefined`。
- 本次提取到的非预期异常最近发生于 `2026-09-22 10:27:23.371`，早于窗口开始约 2 小时；窗口内没有新的异常堆栈、数据库、Redis、磁盘或内存故障。

## 本次重点事件与建议

### 公网扫描与未知路径

- 指纹包括 `.env`、`.env.local`、`dev/.env`、`staging/.env`、`v1/v2/v3/.env`、`.git/config`、`mcp`、`vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` 等。
- 判断：路径形态与公网漏洞扫描探测一致；应用仅返回“找不到对应的处理服务”，没有源码调用方、数据访问或权限绕过证据。
- 建议：在 Nginx/WAF/云安全组侧统一拦截常见探测路径并限制异常路径；不要为探测请求新增业务 Provider，也不要放宽鉴权。
- 状态：保持 `open`，按安全日志而非代码缺陷跟踪。

### `auth/signin` 未知入口

- 现象：`auth/signin处理失败:找不到对应的处理服务:auth/signin` 共 12 条，集中在 `2026-09-21 14:04:41.584` 至 `14:04:48.401`，不在本次窗口内。
- 判断：没有异常堆栈或源码调用证据，无法确认是前端旧入口、攻击探测还是路由缺失。
- 建议：人工核对生产 App 是否仍调用 `/api/auth/signin`；若没有，按扫描或陈旧客户端处理；若有，再由开发 lane 单独定位调用链。
- 状态：保持 `open`。

### 授权过期业务结果

- 现象：`auth/session/info处理失败:访问受限，授权已过期，请重新登录后再访问。` 共 3 条，最近一次为 `2026-09-22 10:27:23.371`。
- 判断：属于会话/授权状态语义，当前分类器将其记为 unknown，但没有代码缺陷证据。
- 建议：将明确的授权过期、缺 token 业务拒绝纳入 expected/业务噪音分类，避免长期污染可处理异常计数。
- 状态：保持 `open`，等待人工确认分类规则。

### registry fixed 基线与 ID 改写

- 现象：`registry.json` 当前包含 49 条事故，其中历史已修复的懒加载事故被改写为 `INC-20260922-a1233597b0`，状态为 `open`，但仍保留 `fixedAt=2026-09-16 06:53:34` 和 `fixedRelease=/opt/decoboon/releases/20260916-065204`。
- 对照证据：`docs/operations/incidents/2026-09-16/20260916-130232/triage.md` 记录该事故已修复；本次窗口及当前日志快照均无该指纹新增。
- 根因线索：`scripts/prod-monitor-analyze.py` 的 `canonicalize_existing()` 使用当前日期重建 incident ID，并把历史记录状态强制改成 `open`；`update_registry()` 又按每次快照重复累加计数。
- 判断：这是监控工具的数据完整性问题，不是生产代码回归。由于本次 `collect` 为退出码 20，按安全门禁未修改脚本。
- 建议：人工修脚本，保留 stable incident ID、`fixedAt` 和 `fixedRelease`；对“已修复时间晚于日志时间”的历史记录不得重新计为可处理异常并补回归测试。
- 状态：保持人工待处理。

### 本地采集失败路径的日期兼容性

- 现象：首次沙箱内 SSH 失败时，失败摘要调用 macOS 不兼容的 `date -Is`，导致采集时间留空并覆盖原始退出码为 `2`。
- 判断：仅影响本地失败归档，不影响生产服务。
- 建议：将监控脚本日期格式改为 GNU/macOS 兼容写法，并在失败摘要中保留真实 SSH 退出码。
- 状态：保持人工待处理；本次不修改脚本。

## 变更清单

- 写入本 triage、development worklog、shared 交接和 automation memory。
- `collect` 自动更新了 `registry.json` 的计数、最近时间、状态和 `reportPaths`。
- 未修改业务源码、测试、脚本、配置或数据库。
- 未创建源码快照、未构建、未部署、未重启、未标记任何事故为 fixed。
