# 生产后端哨兵开发交接

## 目标

监控 `th4-decoboon` 生产后端，定期采集错误、归档事故、识别代码缺陷，并对符合门禁的缺陷执行测试、修复、发布和失败回滚。

## 命令

```bash
bash scripts/prod-monitor.sh status
bash scripts/prod-monitor.sh collect "30 minutes ago"
bash scripts/prod-monitor.sh latest
bash scripts/prod-monitor.sh snapshot-source
bash scripts/prod-monitor.sh mark-fixed <incident-id> --release <path> --note <summary>

bash scripts/build-release.sh
bash scripts/deploy-prod.sh --release-dir dist/releases/<timestamp>
bash scripts/rollback-prod.sh
```

## Skill

- `backend-prod-sentinel`：`.agents/skills/backend-prod-sentinel/SKILL.md`
- 事故手册：`.agents/skills/backend-prod-sentinel/references/incident-playbook.md`
- 事故目录：`docs/operations/incidents/`

## 安全边界

- 默认只读采集，不因 ERROR 自动重启或改代码。
- 只有单个根因、单修复方向、最多 5 个文件，且无数据库迁移、密钥、权限放宽和资金语义变化时，才允许自动修复发版。
- 发版前创建源码快照、跑定向测试和全量验证。
- 发版后等待 HTTPS 就绪；失败自动切回上一版本。
- 单次运行最多发布 1 次，不做循环重试。

## 首个生产事故

- 事件：`INC-20260916-a1233597b0`
- 指纹：`LazyInitializationException: Could not initialize proxy - no session`
- 现象：登录后 `auth/session/info` 序列化员工归属时触发 Hibernate 懒加载。
- 修复：`CredentialsRepository` 对 `findByCreId` 和 `findFirstByEmployee_EmpIdOrderByCreIdDesc` 增加 `@EntityGraph`。
- 回归测试：`src/test/java/com/th4/edge/tenant/user/AuthSessionInfoLazyAcceptanceTest.java`。
- 发布目录：`/opt/decoboon/releases/20260916-053143`。
- 回滚点：`/opt/decoboon/releases/20260916-0415`。
- 部署验证：HTTPS `/api/auth/session/info` 返回 HTTP 200 和 `tokenIsUndefined`。
- 残留测试债务：`ProviderSecurityAcceptanceTest` 路由总数断言、`ProductionReleaseBaselineAcceptanceTest` 的旧回环地址断言仍需 acceptance 更新。
