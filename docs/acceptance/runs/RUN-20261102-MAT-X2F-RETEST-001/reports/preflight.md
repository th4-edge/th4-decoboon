# 预检

- RUN：`RUN-20261102-MAT-X2F-RETEST-001`
- 缺陷：`DEF-20261102-005`
- Plan：`TP-20261101-V0.1`
- Suite：`SUITE-CORE-CHAIN-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- 基础提交：`256cc587dfea26d63fc4ec918caf66d671fe592b`
- 修复补丁：`501163359a5e26e171af87e7ca36e297252aee247a4e428de9413b613b1370f4`
- Web：`ec3da3bc8c7db34ddc2996e345e5c8cd495e09ad`
- App：`f2fb91e0785bf953940257ce97e3ea84fc2ae808`
- 环境：干净 worktree + 隔离库 `decoboon_accept_20261101`，后端 `http://127.0.0.1:8093/api`，Web `http://127.0.0.1:5180`
- 租户：`199500000000000001`
- 账号：Designer `13910002009`、Finance `13910002003`、Pm `13910002006`
- 数据前缀：`AUDIT-20261102-MATX2F001-`
- 迁移备份：`raw/db-before-134.sql.gz`，SHA-256 `523a898ac35ba48e1233709004d1c84ad07facf2c70b2e42e4a70203c10a6545`
- 迁移 134：仅在隔离库执行；独立克隆库完成迁移→回滚演练
- App/Owner：`BLOCKED_IDENTITY`，禁止伪造
- 状态：预检通过，进入修复复验
