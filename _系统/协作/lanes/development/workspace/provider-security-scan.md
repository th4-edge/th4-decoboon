# Provider 安全静态扫描

## 复现命令

```bash
python3 _系统/协作/lanes/development/workspace/provider-security-scan.py . \
  _系统/协作/lanes/development/workspace/provider-security-scan.csv
```

## 结果

```text
total=198 protected=187 public=11 violations=0
```

完整逐路由清单见 `provider-security-scan.csv`，包含路由、是否继承
`Security`、是否公开白名单、实现类和接口契约路径。

## 公开路由白名单

| 路由 | 原因 |
| --- | --- |
| `auth/session/login` | 微信 code 换取登录凭证 |
| `auth/session/password-login` | 员工手机号密码登录，公开入口 |
| `auth/session/password-setup` | 受 bootstrap token、手机号和未设密条件保护的首次设置入口 |
| `auth/session/register` | 已取得微信身份后的企业注册入口 |
| `identity/customer-invite/accept` | 业主邀请接受，使用登录 token 和邀请 token 双重校验 |
| `identity/customer-invite/info` | 业主邀请公开查询，只返回脱敏信息 |
| `invite/invitation/accept` | 员工邀请接受，使用登录 token 和邀请校验 |
| `invite/invitation/info` | 员工邀请信息，需邀请 ID 和手机号 |
| `invite/invitation/reject` | 邀请拒绝，需邀请 ID 和手机号 |
| `order/callback` | 微信支付回调，使用微信平台签名、报文解密和二次查询验真 |
| `file/private/download` | 短期 HMAC 文件 URL，签名和有效期由服务端校验 |

## 本轮修复

以下路由原缺少 `Security`，已补齐：

- `auth/session/info`
- `project/acceptance/page`
- `project/task/update`
- `user/user/info`
- `user/user/update`

Web 密码会话路由已拆分为明确 Provider：

- 公开：`auth/session/password-login`、`auth/session/password-setup`
- 受保护：`auth/session/password-change`、`auth/session/logout`

新增 `ProviderSecurityValidator` 在 Spring Boot 启动时再次扫描项目内 Provider：
除上述 11 条显式白名单外，任何未继承 `Security` 的具体业务路由都会阻止应用启动；`auth/session/password-change`、`auth/session/logout` 均为显式 `Security` 路由。

订阅权益由 `SubscriptionAccessRequestFilter` 自动作用于所有继承 `Security` 的 Provider：
查询能力继续放行，写操作在订阅到期、冻结或关闭时拒绝，订阅、套餐和订单续费路由豁免。
`file/private/grant` 属于文件读取授权，不受企业写闸门限制；文件本身仍需通过业务权限和短期签名校验。
