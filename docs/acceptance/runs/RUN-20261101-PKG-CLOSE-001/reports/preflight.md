# RUN-20261101-PKG-CLOSE-001 预检

## 资产与版本

- Plan：`TP-20261101-V0.1`
- Suite：`SUITE-CORE-CHAIN-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- 测试资产校验：`python3 docs/testing/scripts/test_assets.py validate` PASS，44 cases / 543 providers。
- App：`1.0.74 / 174`，hotfix `e3c95d4`，本地 `8090` 构建树 SHA-256 `59a02a8af72c3e3d7c8e450c202b7eabdee61e242e8aabfb5f7eac19ad215a28`。
- Backend：`7b7dc5f`，工作区存在未提交变更，运行实例为本地 dev `8090`。
- 生产对照：App 正式版发布记录 `1.0.72`；生产套餐接口仅用于 READ_ONLY 数据对照。
- 租户：昭青科技 · 业务闭环审计，`com_id=199500000000000001`。
- 数据前缀：`AUDIT-20261101-PKGCLOSE-001-`。

## 关键资产缺口

- V0.1 用例库没有 `pack/*` 套餐配置、发布、下一版、停用、项目行和套餐报价来源专项用例。
- 现有 `TC-DQ-DESIGNER-QUOTE-003` 是设计报价用例，不能替代套餐配置闭环。
- 因此正式套餐验收结论必须是 `BLOCKED`；套餐专项执行只能作为探索性证据。

## 写前安全

- 写前全库备份：`raw/backup/local-decoboon-before-RUN-20261101-PKG-CLOSE-001.sql.gz`。
- SHA-256：`3f4d380bf815f42d2e322ee8f85d27ae2ef1e8888be378bb49504f696ea0c56f`。
- 所有套餐测试数据使用唯一 AUDIT 前缀；未修改生产数据。

## 环境限制

- 使用生产 `origin/prod` 旧套餐后端连接当前新 schema 的本地库时，`pack_template.code` 无默认值导致创建失败；该路径不纳入产品结论。
- 实际页面写操作使用当前本地 dev 后端；发布动作因 App 1.0.72 无发布入口，使用受控 API 夹具将测试草稿发布，未把发布夹具当作用户页面通过。
- 线上生产套餐仅做只读对照；没有使用生产 Boss 会话执行写操作。
