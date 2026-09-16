# 生产事故处理手册

## 证据优先级

1. 应用错误日志：`/var/log/th4-decoboon/log_error.log`
2. 应用标准输出：`/opt/decoboon/th4-decoboon/logs/stdout.log`
3. systemd：`journalctl -u decoboon`
4. 服务状态、端口和 HTTPS 健康接口
5. 资源：磁盘、内存、数据库和 Redis 连通性

## 分类

| 分类 | 含义 | 自动动作 |
| --- | --- | --- |
| `expected` | 缺 token、密码错误、支付空回调、重复注册等预期业务错误 | 只记录统计，不修代码 |
| `code_bug` | 懒加载、空指针、类型转换、事务或确定性业务逻辑异常 | 满足门禁后可自动修复和发版 |
| `infrastructure` | 数据库、Redis、磁盘、内存、网络、超时 | 记录并告警，不改业务代码 |
| `external_dependency` | 微信、OSS、支付等第三方接口失败 | 记录并核对第三方状态，不改业务代码 |
| `unknown` | 证据不足或无法归类 | 保留开放事故并补充证据 |

## 代码缺陷判定

至少满足：

- 异常堆栈包含仓库源码类或明确指向仓库查询/序列化链路。
- 可以通过本地测试或最小复现稳定触发。
- 修复前后行为可观察，且不是仅靠增加日志或吞异常。
- 不把生产数据脏值、配置错误、网络抖动误判为代码缺陷。

## 发版检查

```bash
bash scripts/prod-monitor.sh snapshot-source
mvn -o -q test
bash scripts/build-release.sh
bash scripts/deploy-prod.sh --release-dir dist/releases/<timestamp>
```

发版记录必须包含：

- 事故 ID、异常指纹、根因和影响范围。
- 修改文件、回归测试、测试结果。
- release 目录、压缩包 SHA-256、部署版本目录。
- 前一版本、回滚命令、HTTPS 冒烟结果。
- 残留风险和后续观察窗口。

## 回滚

```bash
bash scripts/rollback-prod.sh
bash scripts/rollback-prod.sh /opt/decoboon/releases/<version>
```

回滚后仍需重新采集日志，确认旧版本恢复且事故影响停止扩大。

修复验证完成后标记事故：

```bash
bash scripts/prod-monitor.sh mark-fixed <incident-id> \
  --release /opt/decoboon/releases/<version> \
  --note "根因、回归测试、发布和冒烟结果"
```
