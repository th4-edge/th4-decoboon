# 生产发版配置与产物交接

## Server Facts

- 服务器：`120.55.15.159`
- 域名：`decoboon.joytsing.com`，DNS 已解析到服务器
- JDK：`/opt/java/jdk-22.0.2+9`
- MySQL：`127.0.0.1:3306`
- Redis：`127.0.0.1:6379`
- Nginx：`0.0.0.0:80`
- 应用目录规划：`/opt/decoboon/th4-decoboon`
- Web 目录规划：`/opt/decoboon-web/dist`
- 生产环境文件：`/etc/decoboon/th4-decoboon.env`

## Release Artifacts

- 后端：`dist/releases/20260916-040241/th4-decoboon-1.0.0-build.tar.gz`
- 后端 SHA-256：`84c488eb20381793ca4c1ce1dad25f83730796cc8a09e09e509e215927d5abce`
- Web：`../../前端代码/th4-decoboon-web/release/20260916-040220/th4-decoboon-web-2.0.0.tar.gz`
- Web SHA-256：`be2cbd027b0c6b87339de147034837ffa5b5f7a4bc678c3834a477a597214432`

## Configuration

- `.env.prod.local`：真实数据库、Redis、OSS、微信小程序和微信支付参数，权限 `600`，已忽略提交。
- `.env.prod.example`：可提交的生产变量模板。
- `src/bin/cmd.sh`：生产启动、停止、重启、状态和配置校验，默认读取 `/etc/decoboon/th4-decoboon.env`。
- `src/bin/decoboon.service.example`：systemd `Type=forking` 示例。
- `docs/deploy/decoboon.joytsing.com.conf`：Nginx HTTPS/API/Web SPA 模板。
- `docs/deploy/PRODUCTION.md`：服务器部署、权限、启动和回滚说明。

## Verification

- 后端 release 包：Maven assembly 成功，包内仅包含生产配置，无 `application-dev.yml`、`application-test.yml`、微信支付证书或真实环境文件。
- 后端生产配置校验：临时解包后执行 `cmd.sh check` 通过。
- Web 生产构建：`npm run build` 通过，API 地址为 `https://decoboon.joytsing.com/api/`。
- Web release 包：静态资源、index 和 SHA-256 校验生成。
- 生产部署版本：`/opt/decoboon/releases/20260916-0415`、`/opt/decoboon-web/releases/20260916-0415`。
- 生产数据库：空库基线完成，88 张表，`01/10/11/12/17/21` 均为 `BASELINE`。
- systemd：`decoboon.service` enabled + active，后端监听 `127.0.0.1:8090`。
- Nginx：HTTPS 证书有效，`nginx -t` 通过，80 重定向 HTTPS，443 监听。
- 生产 HTTPS：首页 `200`，静态 JS `200`，`/api/auth/session/info` 返回 `tokenIsUndefined`，支付回调空请求返回“微信支付回调签名缺失”。
- 浏览器验证：`https://decoboon.joytsing.com/` 正常渲染“登录企业工作台”。

## Final Production Recheck

- 时间：2026-09-16 04:18 CST（服务器本地时间）。
- 发布包 SHA-256 与本地制品一致：后端 `84c488eb...d5abce`，Web `be2cbd02...214432`。
- `decoboon.service`：enabled + active，主进程运行；`decoboon.service` journal 无启动异常。
- `cmd.sh check`：`Production configuration validation passed`。
- 进程运行参数：`SPRING_PROFILES_ACTIVE=prod`、`SERVER_PORT=8090`、`TH4_STORAGE_PROVIDER=oss`、`ALIYUN_OSS_BUCKET=decoboon`、支付回调为正式 HTTPS 地址。
- 监听：Nginx `0.0.0.0:80/443`，Java 仅监听 `127.0.0.1:8090`。
- Nginx `nginx -t`：通过。外部页面标题为“装得清 · 企业工作台”，首页引用 JS 资源返回 HTTP 200。
- HTTPS：HTTP 301 到 HTTPS；首页和 API 会话信息接口均返回 HTTP 200；支付回调空请求已进入业务 Provider。
- 数据库：`decoboon` 88 张表，迁移账本 `01/10/11/12/17/21` 全部为 `BASELINE`。
- 证书：`decoboon.joytsing.com` SAN 正确，有效期至 2026-12-13 23:59:59 UTC。
- 权限：环境文件 `640 root:decoboon`，支付私钥 `600 decoboon:decoboon`，公开支付公钥 `644 decoboon:decoboon`。

## Remaining Acceptance

- 尚无真实企业账号业务数据，登录后完整业务页面仍待 acceptance 使用生产账号验证。
- 微信支付 API 已通过正式商户配置，但真实回调解密仍需首笔支付或测试通知完成端到端确认。
- 后端全量测试仍有并行开发带来的 Provider 路由固定总数断言失败（测试期望 196，当前实际 198），发布包使用 `-DskipTests` 构建。
- 旧 `application-dev.yml` 中开发凭据仍存在；生产包已排除该文件，但建议后续治理开发默认密钥。

## Acceptance Needed

- 使用真实企业账号完成 Web 登录后全业务页面联调。
- 微信支付首笔下单、支付查询、回调验签解密和幂等履约。
- 生产密钥轮换、证书轮换、备份恢复和回滚演练。
