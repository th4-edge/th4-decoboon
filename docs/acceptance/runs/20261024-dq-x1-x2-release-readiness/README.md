# DQ-X1/DQ-X2 发布前复核

- 日期：2026-10-24
- 状态：**SUPERSEDED / 已发布**
- 后续：DQ-X1/DQ-X2 已由发布提交 `9633395` 记录；生产迁移 131 已补执行。原阻断清单仅保留为发布前历史证据。
- 性质：发布前只读复核；未提交、未推送、未部署、未上传小程序。
- 批次：`DQ-X1 + DQ-X2`，两者均已 `ACCEPTED`。

## 1. 已通过

- DQ-X1 独立验收：18/18 PASS，固定 3 条 SELECT，无 N+1，P95 47.4ms，无跨租户泄漏，无数据副作用。
- DQ-X2 独立验收：API 103/103、App 24/24、Web 6/6。
- 后端干净全量：1098/1098 PASS。
- App `build:mp-weixin:release`、分包门禁 PASS。
- Web build、permissions、workbenches、workspaces PASS。
- `build-release.sh`、`deploy-prod.sh`、`rollback-prod.sh` 均存在且可执行。
- release/deploy 脚本具备健康检查和失败自动回滚能力。

## 2. 发布阻断项

| 编号 | 阻断项 | 事实 | 影响 |
| --- | --- | --- | --- |
| REL-B01 | 工作区不干净 | 后端 271 M / 3 D / 308 ??；Web 71 M / 21 ??；App 84 M / 21 ?? | 无法确定发布对应的唯一提交，无法保证发布内容等于已验收内容 |
| REL-B02 | Web `dev` 与 origin 分叉 | 本地 `dev` ahead 1 / behind 5 `origin/dev` | 直接发布会遗漏远端 5 个提交或制造非快进历史 |
| REL-B03 | 混入未裁决 DDL | 当前工作区含迁移 124–131 及对应 rollback，但 DQ-X1/DQ-X2 本身无 DDL | 发布候选实际升级为 L3 DDL 批次，必须备份、迁移账本、回滚演练和用户确认 |
| REL-B04 | 无远端 test 环境入口 | 历史发布规范记录当前没有独立远端 test 部署入口；test/prod 分支明显落后 dev | 无法完成规定的 test 环境发布冒烟 |
| REL-B05 | App 版本未 bump | `manifest.json` 仍为 `1.0.66 / 166`，但包含 DQ-X2 新改动 | 新体验版/正式版上传会与已发布版本号冲突 |
| REL-B06 | 未取得生产发布授权 | 用户只要求继续“下一步”，未明确授权生产发布 | 不得执行 `deploy-prod.sh`，不得正式发布小程序 |
| REL-B07 | 生产缺少 DQ-X1 依赖表 | 生产只读核对确认 `cust_customer_loss` 不存在，但 DQ-X1 SQL 已引用该表 | 直接发布会导致待谈单接口 SQL 报错；至少需执行迁移 131 并完成 L3 备份/回滚确认 |

## 3. 当前构建产物（仅技术快照，不是发布候选）

| 产物 | SHA-256 | 时间 |
| --- | --- | --- |
| 后端 `target/th4-decoboon-1.0.0.jar` | `f1f5ff7b424ccb9e1a22d2aa184e7e1acdd4cc41cefc7fd4f4bcab6cfbd53661` | 2026-10-24 02:13:55 |
| Web `dist/index.html` | `3e92bcf305df0da01076ac8af685aa89b07d4b43a4bf7684789b4df2cdcfcd67` | 2026-10-24 02:23:39 |
| App `dist/build/mp-weixin/app.json` | `52a2fbcd15b3d21f65351eb022dfafb1941bf5d921c3e9f6e3dece0e3f8a98df` | 2026-10-24 02:23:49 |

这些产物由当前脏工作区生成，不能作为正式发布依据。

## 4. 解锁条件

1. 明确本次发布范围：只发 DQ-X1/DQ-X2，还是连同当前工作区其他改动一起发布。
2. 若只发 DQ-X1/DQ-X2：
   - 将 DQ 相关代码、测试、领域文档和验收证据拆成独立提交；
   - 排除 124–131 DDL、费用/合同/材料/客户流失等无关改动；
   - Web 先处理 `origin/dev` 的 5 个提交，保证快进或完成明确合并。
3. 若包含 DDL 124–131：
   - 按 L3 执行生产库备份、迁移账本登记、正/回滚脚本演练和用户确认；
   - 明确迁移执行窗口与责任人。
4. 从干净提交重新构建三端，并重新记录 SHA-256。
5. 完成 test 环境或等价隔离环境冒烟；同步 test/prod 分支策略。
6. App 若随本批发布：先 bump 版本号，上传体验版；正式提审与发布由用户本人完成。
7. 用户明确授权生产发布后，才可执行 `scripts/deploy-prod.sh`。

8. DQ-X1 依赖迁移 131；生产核对结果：`cust_customer_loss` 不存在。必须先取得用户对迁移 131 的明确 DDL 发布确认。

## 5. 结论

当前不能发布。不是业务代码验收失败，而是**发布候选不可唯一识别且混入未裁决 DDL**。在发布范围、分支同步和 DDL 决策完成前，禁止推送 test/prod、禁止生产部署、禁止小程序正式发布。
