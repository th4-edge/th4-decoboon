# 预检

- RUN：`RUN-20261102-MAT-X2-001`
- Plan：`TP-20261101-V0.1`
- Suite：`SUITE-CORE-CHAIN-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- 后端：`256cc587dfea26d63fc4ec918caf66d671fe592b` / `http://127.0.0.1:8093/api`
- Web：`ec3da3bc8c7db34ddc2996e345e5c8cd495e09ad` / `http://127.0.0.1:5180`
- App：`f2fb91e0785bf953940257ce97e3ea84fc2ae808`（无可授权微信身份）
- 数据库：隔离库 `decoboon_accept_20261101`；租户 `199500000000000001`
- 账号：Designer `13910002009`、Finance `13910002003`、Pm `13910002006`
- 数据前缀：`AUDIT-20261102-MATX2001-`
- 用例数：4；P0/P1：3/1
- 资产校验：PASS
- App/Owner 身份：`BLOCKED_IDENTITY`，禁止使用员工账号、devToken 或数据库登录态冒充
- 状态：预检通过，进入逐例执行
