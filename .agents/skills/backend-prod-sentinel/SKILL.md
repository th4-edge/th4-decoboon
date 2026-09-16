---
name: backend-prod-sentinel
description: Monitor the th4-decoboon production backend, archive actionable log incidents, diagnose root causes, and implement tested code fixes with guarded deployment and rollback when a confirmed safe code bug is found.
---

# 后端生产哨兵

用于生产后端巡检、报错归档、定位代码缺陷、修复测试和受控发版。默认先只读采集；只有确认是仓库内代码缺陷、修复范围小且不涉及数据库迁移或高风险权限/资金语义时，才允许进入自动修复和发版。

## 必读

1. `AGENTS.md`
2. `rules.md`
3. `docs/deploy/PRODUCTION.md`
4. `_系统/协作/lanes/development/workspace/production-release-config-handoff.md`
5. `references/incident-playbook.md`
6. `docs/operations/prod-monitor/README.md`

## 命令

```bash
bash scripts/prod-monitor.sh status
bash scripts/prod-monitor.sh collect "30 minutes ago"
bash scripts/prod-monitor.sh latest
bash scripts/prod-monitor.sh snapshot-source
bash scripts/prod-monitor.sh mark-fixed <incident-id> --release <release-path> --note <summary>

bash scripts/build-release.sh
bash scripts/deploy-prod.sh --release-dir dist/releases/<timestamp>
bash scripts/rollback-prod.sh
```

`collect` 退出码：

- `0`：没有可处理异常。
- `10`：发现高置信代码缺陷。
- `20`：发现基础设施、外部依赖或未知异常。
- `2`：采集失败。

## 工作流

1. 执行 `collect`，读取生成的 `summary.md`、`analysis.json` 和 `docs/operations/incidents/registry.json`。
2. 如果只有 `expected` 噪音，输出简短健康结论，不修改代码、不重启服务。
3. 对新的或重新打开的异常，结合原始日志和代码定位根因。
4. 分类为 `infrastructure`、`external_dependency`、`expected` 或 `unknown` 时，只记录证据和处理建议，不自动改代码。
5. 分类为 `code_bug` 且满足下面的自动修复门禁时，修复、测试并发布。
6. 每次结束后更新事故状态、开发 worklog 和共享交接记录。

## 自动修复与发版门禁

必须同时满足：

- 日志能稳定复现，且根因能定位到当前仓库源码。
- 一个根因，一个修复方向，修改文件不超过 5 个。
- 不涉及数据库 DDL、数据迁移、接口破坏性变更、密钥、支付金额、权限放宽或敏感数据外泄。
- 先创建源码快照，再修改代码。
- 新增或更新回归测试；定向测试和全量测试通过。
- release 包经 SHA-256 校验，生产部署脚本先做配置检查。
- 发版后 HTTPS 冒烟通过；失败必须自动切回上一版本。
- 单次运行最多发布 1 次，不允许循环重试。

任一条件不满足时，将事故保持为 `open`，记录缺失条件，等待人工处理。

## 修复步骤

1. `bash scripts/prod-monitor.sh snapshot-source`
2. 阅读相关 Provider、Context、Repository、实体和现有测试。
3. 编写能稳定失败的回归测试，确认它捕获生产根因。
4. 做最小修复，不顺手重构无关代码。
5. 先运行定向测试，再运行 `mvn -o -q test`。
6. 如果存在已知且与本次改动无关的历史测试失败，必须单独记录并证明不是新引入回归；无法证明时停止发版。
7. `bash scripts/build-release.sh`
8. 使用输出目录执行 `bash scripts/deploy-prod.sh --release-dir <dir>`。
9. 发版成功后再次执行 `bash scripts/prod-monitor.sh collect "10 minutes ago"`，确认同一指纹不再增长。
10. 更新事故文件状态为 `fixed`，写入提交、测试、release、部署、冒烟、回滚和残留风险。

## 禁止

- 不把用户输入、鉴权失败、支付回调空请求等预期业务错误当成代码 bug。
- 不因为日志有 ERROR 就直接重启、改配置或改代码。
- 不绕过发布门禁，不直接覆盖生产目录，不使用 `rm -rf` 清理生产版本和日志。
- 不在无法回滚、无法验证或存在数据迁移时自动发版。
- 不自动修改数据库结构或生产密钥。
