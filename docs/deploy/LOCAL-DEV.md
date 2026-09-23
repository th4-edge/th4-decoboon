# 本地开发运行说明（IDEA / 命令行 / 小程序）

> 适用：本地联调 `th4-decoboon`（后端，8090）+ `th4-decoboon-app`（微信开发者工具）
> 更新：2026-09-16

## 1. 为什么需要这份说明

后端密钥通过**环境变量**注入（微信小程序 AppSecret、OSS、微信支付、文件签名、员工首设 token）。
历史做法是 `./run-dev.sh` 里 `source` `.env.*.local`，但：

- **IDEA 直接运行 `com.th4.edge.Application` 不会加载任何 `.env` 文件**；
- `run-dev.sh` 原先只加载 `.env.oss.local` / `.env.wechat-pay.local`，**不含** `WECHAT_MINI_APP_SECRET`（该变量在 `.env.dev.local`）。

结果：代码没动，但用 IDEA 启动时小程序「微信登录」必然返回
`{"code":"503","message":"微信小程序 appId/appSecret 尚未配置"}`。

## 2. 解决方式：生成外置 dev 配置

Spring Boot 会自动加载工作目录下的 `file:./config/application-dev.yml`，其优先级**高于** `classpath:` 中的同名文件，
因此把本地密钥转换成这个文件即可让 IDEA / Maven / 脚本三种启动方式都能拿到配置。

```bash
# 一次性生成 / 密钥变更后重新生成
bash scripts/gen-local-dev-config.sh
```

脚本行为：

- 从 `.env.dev.local`、`.env.oss.local`、`.env.wechat-pay.local` 读取变量（后者覆盖前者）；
- **只提取白名单**（微信小程序、OSS、微信支付、文件签名、员工首设 token），
  **绝不写入 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `REDIS_*`**，避免本地误连生产库；
- 生成 `config/application-dev.yml`，权限 `600`，且已在 `.gitignore` 中忽略；
- 值不回显到终端。

生成后自检（不打印密钥值）：

| 项目 | 期望 |
| --- | --- |
| 微信小程序 | `enabled=true`、app-id/app-secret 已配置 |
| OSS | `provider=oss`、access-key-id 已配置 |
| 微信支付 | `enabled=true`、mch-id 已配置、api-v3-key 已配置 |

## 3. 启动方式

### 3.1 IDEA（推荐）

1. 先执行一次 `bash scripts/gen-local-dev-config.sh`
2. Run/Debug Configurations → Spring Boot → Main class `com.th4.edge.Application`
3. **Working directory 必须是项目根目录**（默认即是），否则找不到 `./config/application-dev.yml`
4. Active profiles：`dev`（`application.yml` 已默认 `dev`，可不填）
5. 无需配置任何 Environment variables

### 3.2 命令行

```bash
./run-dev.sh          # 生成过 config/application-dev.yml 后即可直接用
# 或
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 4. 本地依赖

| 组件 | 期望 |
| --- | --- |
| MySQL | `127.0.0.1:3306`，库 `decoboon`，dev 配置为 `root/mysqladmin` |
| Redis | `127.0.0.1:6379`，密码 `123456` |
| 微信开发者工具 | 打开 `th4-decoboon-app/dist/dev/mp-weixin`（先 `npm run dev:mp-weixin`） |

## 5. 启动后自检

```bash
# 1) 微信小程序配置（期望：走到真实 code2session，返回 invalid code，而不是 503 未配置）
curl -s -X POST http://127.0.0.1:8090/api/auth/session/login \
  -H 'Content-Type: application/json' -d '{"code":"verify"}'

# 2) 微信支付配置（期望：回调签名缺失/报文不存在，而不是"商户号...尚未配置"）
curl -s -X POST http://127.0.0.1:8090/api/order/callback \
  -H 'Content-Type: application/json' -d '{}'

# 3) OSS：登录小程序后上传一张图片/一个文件，确认文件访问地址为
#    https://decoboon.oss-cn-hangzhou.aliyuncs.com/...（预签名私有读取）
```

## 6. 安全约束

- `config/application-dev.yml`、`.env.*.local`、`config/wechat-pay/` **一律禁止提交**；
- 生成脚本已做白名单限制，**不要**手工把 `DB_*` / `REDIS_*` 加进该文件；
- 生产环境使用 `application-prod.yml` + `/etc/decoboon/th4-decoboon.env`，与本地这套机制互不影响；
- 生产 `spring.jpa.open-in-view` 必须保持 `true`（见 `rules.md` §4.5）。
