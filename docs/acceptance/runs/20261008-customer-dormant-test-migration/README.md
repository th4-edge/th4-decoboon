# 客户休眠状态 test 环境迁移与回滚演练

- 日期：2026-10-08
- 环境：本机 MySQL 8.0.24 `decoboon-test`，152 张表，迁移前 `cust_customer` 为 0 行
- 结论：测试库迁移 92 成功；临时库恢复备份后执行“迁移 92 → 回滚 92”成功；test profile 后端启动与未登录路由烟测通过

## 迁移前状态

- `cust_customer.status` 不含 `Dormant`
- 不存在 `cust_customer_status_log`
- 不存在休眠字段
- 无迁移账本表 `db_schema_migration`

## 备份

- 文件：`docs/design/migrations/backups/20261008-191830-92-test-decoboon-full.sql.gz`
- SHA-256：`85b6c7b9a3aedaf4cb51260bd573b560ad823e64020444d3a73d837f293be953`

## test 库迁移

执行：

```text
mysql decoboon-test < docs/design/migrations/92-客户休眠状态与风险跟进排除.sql
```

验证结果：

- `status` 枚举包含 `Dormant`
- `status_before_dormant` 存在
- `cust_customer_status_log` 存在
- 状态日志表 3 个外键存在

## 回滚演练

步骤：

1. 新建临时数据库。
2. 从 test 迁移前备份恢复。
3. 执行迁移 92。
4. 执行 `docs/design/migrations/rollback/92-客户休眠状态与风险跟进排除-rollback.sql`。
5. 验证状态枚举恢复为原 8 值、休眠字段归零、日志表不存在。
6. 删除临时数据库。

结果：PASS。

## test profile 启动烟测

- 命令使用 `spring.profiles.active=test`、`server.port=8091`。
- 首次启动因本机 Redis 需要密码而失败；补充 `REDIS_PASSWORD=123456` 后启动成功。
- `/api/auth/session/info` 未登录访问返回 HTTP 200 业务错误 `tokenIsUndefined`，符合受保护 Provider 预期。
- 验证后已优雅停止 test profile 进程。

## 边界

- 本演练仅覆盖本地 `decoboon-test`，不是独立测试环境或生产发布。
- `decoboon-test` 没有迁移账本，因而没有登记版本行；生产仍需按正式账本和发布流程执行。
- Redis 密码依赖属于本地环境差异，不作为代码缺陷。
