# 2026-10-24 登录 token 类加载器热修

## 结论

- 状态：已发布并完成生产登录冒烟。
- 发布时间：2026-10-24 15:11 CST。
- 生产 release：`/opt/decoboon/releases/20261024-150858`。
- 上一版本：`/opt/decoboon/releases/20261024-030000`。
- 源码提交：`18099ed fix(release): enforce thin boot jar for production`。
- 运行时代码：沿用 `20261024-030000` 同一批 class，仅修复 boot JAR 打包形态，不夹带业务功能改动。

## 根因

`20261024-030000` 将 Spring Boot repackage JAR 直接用于生产，应用类位于 `BOOT-INF/classes`，由 `LaunchedClassLoader` 加载；外部 `lib/th4-framework.jar` 由 `AppClassLoader` 加载。

JWT 校验时 `JwtSecurityManager.authenticate()` 执行 `Class.forName(audience)`，试图加载 `com.th4.edge.tenant.user.model.Employee`。由于调用方类加载器看不到 `BOOT-INF/classes`，抛出 `ClassNotFoundException`，框架捕获后统一返回 `tokenIsUndefined`。因此登录接口已签发 token，但会话接口一定失败。

标准 `scripts/build-release.sh` 的 assembly 交付物本应是 thin JAR；本次生产包偏离了该交付路径。

## 修复

- `spring-boot-maven-plugin` 的 repackage 产物改用独立 classifier `boot`，主制品保持 thin JAR。
- 新增 `scripts/verify-release-boot-jar.py`。
- `scripts/build-release.sh` 与 `scripts/deploy-prod.sh` 均增加制品门禁：
  - `Main-Class` 必须为 `com.th4.edge.Application`；
  - 必须存在根路径 `com/th4/edge/Application.class`；
  - 禁止 `BOOT-INF/classes/`；
  - 禁止 Spring Boot loader。
- 发版规范新增“后端发版制品与类加载器门禁”，并要求发布后验证登录后的真实会话，不得只验证未登录时的 `tokenIsUndefined`。

## 验证

- 全量后端测试：239 个测试套件，928 个用例，0 失败、0 异常、0 跳过。
- 标准构建：主制品 `th4-decoboon-1.0.0.jar` 为 thin JAR；重打包产物单独输出为 `th4-decoboon-1.0.0-boot.jar`。
- 制品门禁：对正确包 PASS；对本次故障的 Spring Boot loader JAR 正确 FAIL。
- 同数据 A/B：
  - Spring Boot loader JAR：`password-login` 成功，`auth/session/info` 返回 `tokenIsUndefined`。
  - thin JAR：登录成功，`auth/session/info` 成功返回员工会话。
- 生产发布包 SHA-256：`f6ddfbc5900d829a41d1649696960d4c31da29b78c5e5cb370eb05ae97543e3d`。
- 生产微信开发者工具真机链路：
  - `auth/session/login` HTTP 200，签发 168 字符 token；
  - `auth/session/info` HTTP 200，成功返回企业员工会话；
  - 页面实际从登录页进入 `pages/index/index`，本地会话角色为 `boss`；
  - Nginx 会话成功响应体由故障时 162/178 B 恢复为 892 B。
- 生产健康检查：HTTP 200；服务 active；boot JAR 为 thin JAR，`BOOT-INF=false`。
- Web 登录页：HTTP 200。

## 备份与回滚

- 备份目录：`/opt/backups/th4-decoboon/20261024-150510/`。
- 数据库备份：`decoboon.sql.gz`，已通过 `gzip -t`。
- 当前版本完整备份：`release-20261024-030000.tar.gz`。
- 环境文件备份：`th4-decoboon.env.backup`，权限 600。
- 回滚命令：`bash scripts/rollback-prod.sh /opt/decoboon/releases/20261024-030000`。

## 观察

- 发布后 `auth/session/login -> auth/session/info` 真实链路已恢复。
- 监控剩余 `tokenIsUndefined` 均为未携带 token 的健康探测或正常未登录请求，不是登录后会话失败。
