# 微信支付 API v3 配置交接

## Merchant Facts

- 商户号：`1750623009`
- 商户主体：安徽昭青网络科技有限公司
- AppID：`wxe5cc5fec9e50a9a9`
- 小程序：装得清
- AppID 绑定状态：已关联
- 商户 API 证书序列号：`24A00BDC638A9B5BE07C8E6BF396096BA379AE07`
- 证书有效期：2026-09-15 至 2031-09-14
- 微信支付公钥 ID：`PUB_KEY_ID_0117506230092026091600211597001000`
- 回调地址：`https://decoboon.joytsing.com/api/order/callback`

## Secure Files

- 商户私钥：`config/wechat-pay/apiclient_key.pem`，权限 `600`
- 商户证书：`config/wechat-pay/apiclient_cert.pem`
- PKCS#12：`config/wechat-pay/apiclient_cert.p12`，权限 `600`
- 微信支付公钥：`config/wechat-pay/pub_key.pem`
- 本地环境变量：`.env.wechat-pay.local`，权限 `600`

`.env.wechat-pay.local` 和 `config/wechat-pay/` 均已加入 `.gitignore`。API v3 密钥不在本交接文档中出现。

## Changed Files

- `src/main/java/com/th4/edge/components/wechat/sdk/WechatPaySdk.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `run-dev.sh`
- `.gitignore`
- `.env.wechat-pay.local.example`
- `docs/design/migrations/23-微信支付配置.md`

## Verification

- 商户证书与 PKCS#8 私钥公钥指纹一致。
- 证书序列号和证书 CN 商户号一致。
- 商户平台 AppID 绑定核对通过。
- 微信支付公钥 PEM 加载通过，公钥 ID 与商户平台一致。
- 正式交易查询签名请求被微信支付接受。
- 不存在订单返回 `ORDER_NOT_EXIST`，响应验签通过，响应公钥 ID 匹配。
- Spring 完整上下文加载环境变量后，`WechatPayClient` 正式接口调用 1/1 通过。
- `WechatPaySdkTest` 通过。

## Risks

- API v3 密钥的真实回调解密需要首笔真实支付或微信支付测试通知才能端到端确认。
- 商户 API 证书 2031-09-14 到期；微信支付公钥可能轮换，需建立到期和公钥 ID 变更监控。
- 回调和 HTTPS 域名必须在生产网络可访问，且微信支付商户平台配置必须与 `WECHAT_PAY_NOTIFY_URL` 完全一致。
- 当前证书仅在本机配置；生产服务器仍需通过外部 `config/wechat-pay/` 或环境变量部署同一套材料。

## Acceptance Needed

- 首笔 JSAPI 支付预下单、支付结果查询和真实回调验签/解密。
- 商户订单号、金额、AppID、OpenID 和商户号绑定校验。
- 回调重复通知、异常金额、错误 AppID/商户号和过期签名负向测试。
