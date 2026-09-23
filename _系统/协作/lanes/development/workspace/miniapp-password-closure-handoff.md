# 小程序密码设置闭环开发交接

## 输入依据


## Changed Files

### 后端

- `src/main/java/com/th4/edge/tenant/user/service/auth/session/passwordchange/support/PasswordChange.java`
- `src/main/java/com/th4/edge/tenant/user/service/auth/session/passwordchange/PasswordChange.java`
- `src/main/java/com/th4/edge/tenant/user/service/auth/session/info/support/Info.java`
- `src/main/java/com/th4/edge/tenant/user/model/Credentials.java`
- `src/main/java/com/th4/edge/tenant/user/context/PasswordAuthContext.java`

### 小程序

- `../../前端代码/th4-decoboon-app/src/subpackages/account/pages/password.vue`
- `../../前端代码/th4-decoboon-app/src/pages.json`
- `../../前端代码/th4-decoboon-app/src/config/index.ts`
- `../../前端代码/th4-decoboon-app/src/pages/mine/index.vue`
- `../../前端代码/th4-decoboon-app/src/domains/identity/api.ts`
- `../../前端代码/th4-decoboon-app/src/domains/identity/model.ts`
- `../../前端代码/th4-decoboon-app/src/domains/identity/session.ts`
- `../../前端代码/th4-decoboon-app/src/App.vue`
- `../../前端代码/th4-decoboon-app/src/pages/auth/login.vue`
- `../../前端代码/th4-decoboon-app/src/pages/auth/enterprise.vue`
- `../../前端代码/th4-decoboon-app/src/pages/invite/accept.vue`

## Verification

- `mvn -o -q -DskipTests compile`：通过。
- `mvn -o -q test`：79 tests，0 failures，0 errors。
- `npm run verify:mp-weixin`：type-check、微信小程序构建、分包检查全部通过。

## Contract

- `auth/session/password-change` 继续要求登录 token，不新增公开入口。
- 当前凭证未设置密码时：提交 `newPassword + confirmPassword`，首次设置并要求符合 8-64 位、同时包含字母和数字的策略。
- 当前凭证已设置密码时：必须提交并校验 `oldPassword`，成功后使用 `PasswordChange` 审计。
- 首次设置使用 `PasswordSet` 审计；BCrypt 哈希落库；响应只返回 `passwordSet` 布尔值，不返回密码哈希。
- 小程序“我的 -> 账号密码”根据 `auth/session/info.passwordSet` 自动展示“设置密码”或“修改密码”。

## Risks

- 开发阶段已用临时探针跑通“微信员工 token 首次设密 -> 密码登录”，但尚未固化为 `src/test` 的独立 acceptance 用例。
- 未执行真实微信开发者工具/真机交互和真实后端联调。

## Next Action

- acceptance lane 增加并执行受保护首次设密用例：匿名拒绝、微信员工 token 首次设密、重复设密、改密旧密码校验、密码登录、审计与敏感字段排除。
- 前端验收“我的 -> 账号密码”在未设置/已设置两种状态下的表单、错误提示和成功回写。

## Acceptance Needed

是。开发自测不等于最终验收结论。
