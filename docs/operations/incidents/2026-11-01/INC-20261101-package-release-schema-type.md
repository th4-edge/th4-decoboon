# INC-20261101 · 套餐发布首次部署被数据库类型门禁拦截

## 摘要

- 时间：2026-11-01 19:43（Asia/Shanghai）
- 环境：生产
- 影响：部署期间出现约数分钟 502；`deploy-prod.sh` 自动回滚至 `20261029-152049`，当前服务和 HTTPS 健康检查恢复 200。
- 数据：迁移 124 已成功执行并登记；未发现业务写入损坏。

## 根因

迁移 124 将 `pack_line.craft_snapshot` 定义为 `LONGTEXT`，发布代码实体只使用 `@Lob`，Hibernate 在 `ddl-auto=validate` 下期望更短文本类型，导致启动失败。

## 修复

- 在 `PackageLine.craftSnapshot` 显式声明 `columnDefinition = "longtext"`，与迁移和数据库实际类型一致。
- 修复提交：`6dc3a95`。
- 使用生产数据库备份恢复的隔离库执行迁移 124，并以 `ddl-auto=validate` 启动修复后的后端，验证通过。

## 回滚

- 自动回滚点：`/opt/decoboon/releases/20261029-152049`
- 回滚后 `decoboon` 服务 active，`/api/auth/session/info` 返回 HTTP 200 和预期 `tokenIsUndefined`。

## 证据

- 首次部署日志：`/tmp/package-release-build.log`、部署终端记录
- 生产备份：`/opt/backups/th4-decoboon/pre-package-release-20261101-193609/db.sql.gz`
- 隔离库类型验证：`ddl-auto=validate` 启动成功
