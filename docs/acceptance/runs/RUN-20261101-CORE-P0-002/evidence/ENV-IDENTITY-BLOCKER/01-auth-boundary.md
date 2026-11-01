# App / 业主端身份阻断说明

- 采集时间：2026-11-02T00:53:32+08:00
- 后端：`http://127.0.0.1:8092/api`
- Web：`http://127.0.0.1:5175`
- App 构建：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev/mp-weixin-accept-8092`
- 隔离数据库：`decoboon_accept_20261101`

## 事实

1. 用户已明确说明当前无法提供与昭青测试环境匹配、且经授权的微信小程序身份。
2. 业主端同样没有可验证的真实业主身份、登录会话或真人见证。
3. 根据验收智能体硬边界，不得用员工账号、数据库造登录态、devToken 或直接改数据冒充 App/业主端页面通过。
4. 现有 App 登录尝试停在微信登录入口，截图：`evidence/TC-FIN-FINANCE-RECEIPT-002/12-app-login.png`。

## 影响

- 依赖 App 页面、真机交互、业主本人确认或业主真实身份的步骤只能记 `BLOCKED_IDENTITY`。
- Web、Provider、数据库可继续做事实核验与受控夹具，但不得据此将整条跨端用例标为 `PASS`。
