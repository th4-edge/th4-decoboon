# 生产监控处置记录｜2026-09-23 13:01:04 +08:00

## 结论

- 采集命令：`bash scripts/prod-monitor.sh collect "35 minutes ago"`。
- 首次执行受本地 macOS `date -Is` 兼容性和沙箱 SSH 限制影响，退出码为 `2`，失败目录为 `20260923-130037`；使用临时日期兼容 shim 并授权只读 SSH 后重跑成功，最终退出码 `20`。
- 处置策略：按安全门禁仅归档证据并给出人工处理建议；未修改业务源码或测试、未重启服务、未创建发布包、未部署、未标记任何事故为 fixed。
- 服务状态：`decoboon.service` enabled/active/running，PID `63746`，启动于 `2026-09-21 05:06:10 CST`。
- 当前版本：`/opt/decoboon/releases/20260921-050519`。
- 健康检查：HTTPS `/api/auth/session/info` 返回 HTTP 200；未携带 token 时返回 `tokenIsUndefined`，符合未登录语义。
- 资源：根分区使用率 18%，内存 available 约 4.3 GiB，未见磁盘、内存、端口或服务状态异常。

## 时间窗口复核

- 窗口约为 `2026-09-23 12:26:04` 至 `13:01:04`。
- `journalctl -u decoboon --since "35 minutes ago"` 无条目。
- 窗口内应用日志只有三类记录：`12:42:05.453` 的 `.env` 扫描探测（本地分类器记 expected）、`12:42:45.454` 的 `.git/config` 扫描探测（当前记 unknown/open）、`13:01:04.579` 本次健康检查产生的 expected `tokenIsUndefined`。
- 未发现窗口内新增的异常堆栈、数据库/Redis 故障、磁盘或内存故障，也没有 `code_bug` 分类项。
- `analysis.json` 汇总 unknown `66` 条、expected `60` 条；日志文件采集仍使用 `tail` 而非按时间过滤，因此计数包含历史记录，`20` 不代表本窗口发生 66 次生产异常。

## 本次重点事件与建议

### 公网扫描路径

- 窗口内命中：`.env`、`.git/config`；历史仍有 `.env.local`、`dev/.env`、`staging/.env`、`v1/v2/v3/.env`、`mcp`、`vendor/phpunit/.../eval-stdin.php` 等。
- 判断：请求路径与公网漏洞扫描特征一致；没有异常堆栈、源码调用链、数据访问或权限绕过证据。
- 建议：在 Nginx/WAF/云安全组侧统一拦截常见探测路径并限制异常路径；不要为探测请求新增业务 Provider，也不要放宽鉴权。
- 状态：`.git/config` 对应 `INC-20260923-8f4d3f4876` 保持 `open`；其余扫描类历史事项按安全日志跟踪。

### 历史未知与业务拒绝

- `不能操作自己的员工记录`、`企业唯一负责人不能退出，请先转让负责人`、`接受邀请后 30 天内不能再次加入企业`、`该手机号已有待接受邀请` 等最近发生在 `2026-09-22 16:xx`，均早于本窗口。
- 判断：现有证据更像业务规则拒绝或旧测试操作，不满足代码缺陷判定与自动修复门禁。
- 建议：人工复核相应前端调用与权限文案；明确为业务规则的指纹应补入 expected/业务噪音分类，避免继续污染可处理异常计数。
- 状态：保持 `open`。

### 授权过期与缺 token 分类

- `访问受限，授权已过期，请重新登录后再访问。` 最近一次为 `2026-09-22 16:11:42`；缺 token 健康检查在本次窗口产生 expected 噪音。
- 判断：均属于会话/授权状态语义，没有代码缺陷证据。
- 建议：将明确的授权过期纳入 expected；保留缺 token 的预期分类。
- 状态：保持 `open`，等待人工调整分类规则。

### registry fixed 基线与 ID 改写

- 现象：`registry.json` 当前包含 52 条事故，其中历史已修复的懒加载事故仍保留 `fixedAt=2026-09-16 06:53:34` 和 `fixedRelease=/opt/decoboon/releases/20260916-065204`，但 incident ID 已改写为 `INC-20260923-a1233597b0`，状态为 `open`。
- 对照证据：本次窗口和当前日志快照均无该指纹新增；历史 triage 已记录该事故修复发布。
- 根因线索：`scripts/prod-monitor-analyze.py::canonicalize_existing()` 使用当前日期重建 incident ID，并把历史记录状态强制改为 `open`；`update_registry()` 还会重复累加历史快照计数。
- 判断：这是监控工具的数据完整性问题，不是生产代码回归。由于本次 `collect` 为退出码 `20`，按安全门禁未修改脚本。
- 建议：人工修复 analyzer，保留 stable ID、`fixedAt` 和 `fixedRelease`；增加“已修复时间晚于日志时间的历史指纹不得重新打开/重复计数”的回归测试；并人工校正当前 registry。
- 状态：保持人工待处理。

### 本地采集失败路径的日期兼容性

- 现象：macOS 的 `date -Is` 失败，失败摘要无法写入采集时间；真实 SSH/网络失败码也在该路径中被覆盖为退出码 `2`。
- 判断：仅影响本地监控归档和退出码可观测性，不影响生产服务。
- 建议：使用 GNU/BSD 均可用的时间格式或统一日期函数，并确保失败摘要显式保留真实 SSH 退出码；补一条兼容性脚本测试。
- 状态：保持人工待处理；本次仅使用 `/tmp` 临时 shim 完成采集，未修改仓库脚本。

## 变更清单

- 写入本 triage、development worklog、shared 交接和 automation memory。
- `collect` 自动更新了 `docs/operations/incidents/registry.json` 的计数、最近时间、状态和 `reportPaths`。
- 未修改业务源码、测试、脚本、配置或数据库；未创建源码快照、未构建、未部署、未重启、未标记任何事故为 fixed。
