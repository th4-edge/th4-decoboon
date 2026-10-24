# 小程序/Web 登录 token 校验生产回归

- 首次定位：2026-10-24 14:43 CST
- 影响：微信小程序及 Web 登录后 `auth/session/info` 返回 `tokenIsUndefined`，用户无法进入工作台。
- 对照：2026-10-21/22 同接口成功响应为 700-892 B；2026-10-24 02:55 发布后降为 162/178 B 错误响应。
- 复现：微信开发者工具 `dist/build/mp-weixin` 请求生产环境，`auth/session/login` 成功返回 168 字符 JWT；同一 token 立即请求 `/api/auth/session/info` 返回 `tokenIsUndefined`。
- 排除：服务 active、HTTP 200、数据库可用；token 签名与框架固定密钥一致；tcpdump 确认 `token` header 已到达 127.0.0.1:8090。
- 版本差异：框架 JAR、application.yml、application-prod.yml 与上一版一致；当前 boot JAR 从上一版普通 thin JAR（classes 在根目录）变为 Spring Boot loader JAR（classes 在 BOOT-INF/classes）。
- 结论：今天 02:55 发布引入的认证请求头/令牌校验回归，非微信 code2session、账号、密码或用户网络问题。
- 建议：立即回滚 `/opt/decoboon/releases/20261022-131500`；修复需在隔离环境验证 header 解码和 boot 打包方式后再发版。

## 根因证实（A/B 隔离复现）

- 使用同一份源码、同一依赖、同一 H2 数据、同一端口配置做两次启动：
  - Spring Boot loader 包（`BOOT-INF/classes`）：`password-login` 成功，`auth/session/info` 返回 `tokenIsUndefined`。
  - thin JAR（`Main-Class: com.th4.edge.Application`）：同一账号、同一 token，`auth/session/info` 成功。
- 类加载器证据：
  - `JwtSecurityManager` 由 `AppClassLoader` 加载（外部 `lib/th4-framework-1.0.0.jar`）。
  - `Employee` 由 `LaunchedClassLoader` 加载（`BOOT-INF/classes`）。
  - `JwtSecurityManager.authenticate()` 内部执行 `Class.forName(audience)` 时，使用自身 `AppClassLoader`，无法加载 `com.th4.edge.tenant.user.model.Employee`，抛出 `ClassNotFoundException`。
  - 框架捕获该异常并统一转换成 `tokenIsUndefined`，因此表面像“缺少 token”，实际是 JWT audience 类加载失败。
- 标准构建验证：`mvn -o -q -DskipTests package` 生成的 assembly 中 boot JAR 仍是 thin JAR（`Main-Class=com.th4.edge.Application`，无 `BOOT-INF/classes`），连续两次构建结果一致。
- 发布根因：`20261024-030000` 实际部署的是 Spring Boot repackage 后的 `target/th4-decoboon-1.0.0.jar`，不是 assembly 应交付的 thin JAR；这是本次发版制品选择/组装路径偏离标准 `build-release.sh` 的结果。
