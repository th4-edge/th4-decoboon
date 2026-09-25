# th4-decoboon 生产打包与发布

> 分支、合并、RC/正式 tag 和发布记录规则以 `docs/operations/版本管理与发布分支规范.md` 为准。

## 1. 目标环境

- 服务器：`120.55.15.159`
- 系统：Alibaba Cloud Linux 4.0.6
- JDK：`/opt/java/jdk-22.0.2+9`
- 应用目录：`/opt/decoboon/th4-decoboon`
- 环境文件：`/etc/decoboon/th4-decoboon.env`
- 日志目录：`/var/log/th4-decoboon`
- 本地兼容目录：`/opt/decoboon/data/{upload,public}`
- 后端监听：`127.0.0.1:8090`
- MySQL：`127.0.0.1:3306/decoboon`
- Redis：`127.0.0.1:6379`
- Nginx：`0.0.0.0:80/443`

## 2. 解包结构

```text
th4-decoboon-1.0.0-build/
├── bin/
│   ├── cmd.sh
│   └── decoboon.service.example
├── boot/th4-decoboon-1.0.0.jar
├── config/
│   ├── application.yml
│   ├── application-prod.yml
│   └── logback-spring.xml
├── lib/
└── docs/PRODUCTION.md
```

## 3. 服务器准备

以 root 创建运行用户、目录，并部署外部环境文件和支付证书：

```bash
useradd --system --home /opt/decoboon --shell /sbin/nologin decoboon || true
mkdir -p /opt/decoboon/th4-decoboon
mkdir -p /etc/decoboon /var/log/th4-decoboon
mkdir -p /opt/decoboon/data/upload /opt/decoboon/data/public
mkdir -p /opt/decoboon/th4-decoboon/config/wechat-pay
chown -R decoboon:decoboon /opt/decoboon /var/log/th4-decoboon
chmod 750 /etc/decoboon /opt/decoboon/th4-decoboon/config/wechat-pay
chmod 600 /etc/decoboon/th4-decoboon.env
```

微信支付材料复制到：

- `/opt/decoboon/th4-decoboon/config/wechat-pay/apiclient_key.pem`
- `/opt/decoboon/th4-decoboon/config/wechat-pay/pub_key.pem`

私钥权限必须为 `600`，属主为 `decoboon`。

## 4. 生产环境文件

生产环境变量放在 `/etc/decoboon/th4-decoboon.env`，参考仓库 `.env.prod.example`。禁止放入 Git、打包产物或公开目录。

关键项：

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- `REDIS_HOST`、`REDIS_PASSWORD`
- `TH4_FILE_SIGNATURE_SECRET`
- `ALIYUN_OSS_*`
- `WECHAT_MINI_*`
- `WECHAT_PAY_*`

## 5. 启动与检查

```bash
cd /opt/decoboon/th4-decoboon
sudo -u decoboon DECOBOON_ENV_FILE=/etc/decoboon/th4-decoboon.env bin/cmd.sh check
sudo -u decoboon DECOBOON_ENV_FILE=/etc/decoboon/th4-decoboon.env bin/cmd.sh start
sudo -u decoboon DECOBOON_ENV_FILE=/etc/decoboon/th4-decoboon.env bin/cmd.sh status
```

`cmd.sh check` 会校验 JDK、JAR、数据库、Redis、文件目录、OSS 和微信支付证书配置，不启动服务。

## 6. systemd

将 `bin/decoboon.service.example` 安装为 `/etc/systemd/system/decoboon.service`：

```bash
systemctl daemon-reload
systemctl enable --now decoboon
systemctl status decoboon
```

## 7. Nginx

- `decoboon.joytsing.com` 必须解析到服务器。
- `/api/` 反向代理到 `http://127.0.0.1:8090`。
- `client_max_body_size` 必须不低于 500 MB。
- 微信支付回调地址必须为 `https://decoboon.joytsing.com/api/order/callback`。

### 当前生产状态（2026-09-16 复核）

- TLS 证书已安装到 `/etc/nginx/ssl/decoboon.joytsing.com/`，`nginx -t` 通过，HTTP 301 跳转 HTTPS，HTTPS API 可达。
- 当前证书有效期至 `2026-12-13 23:59:59 UTC`（北京时间约 `2026-12-14 07:59:59`），需提前配置续期和部署监控。
- 后端生产版本为 `20260916-0415`；后端监听 `127.0.0.1:8090`，systemd `decoboon.service` 为 active/enabled。
- 生产库 `decoboon` 已完成空库基线，共 88 张表，`01/10/11/12/17/21` 均为 `BASELINE`。
- `/api/auth/session/info` 在未携带 token 时返回 HTTP 200 和业务错误 `tokenIsUndefined`；`/api/order/callback` 空请求已进入微信支付回调 Provider。真实登录业务、首笔支付回调、备份恢复和回滚仍需 acceptance 验证。

## 8. 回滚

- 保留上一版本完整目录和数据库备份。
- 停止新版本前先记录 PID、版本、日志和数据库账本状态。
- 切回上一版本目录或将 `current` 软链接改回上一版本。
- 数据库迁移失败时不得直接在原库覆盖恢复，按 `docs/design/migrations/19-生产发布基线.md` 使用新库恢复后切换。
