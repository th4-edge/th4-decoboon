# 研发修复交接单 · 业主视角预览 P0

来源：`20261021-owner-preview-p0-independent`
当前状态：D-002/D-003/D-004 已修复；D-001 隔离修复链已跑通，原始 dev/test 仍待修复；禁止发布
原则：先修发布阻断，再做功能修正；不得跳过独立复验

修复验证：`fix-verification/README.md`

## 修复顺序

### 1. P0 · D-001 迁移链结构与账本漂移

先做环境结构审计，不要直接编辑生产账本：

1. 分别核对 dev/test/prod：
   - `db_schema_migration` 中 121/122/123 状态与 checksum
   - `db_business_target` 是否存在 `store_id`
   - 是否存在 `target_scope_key`
   - 是否存在 `uk_business_target_scope_month`、`idx_business_target_store`、`fk_business_target_store`
2. 判定 121 属于：
   - 已完整应用但账本缺失 → 按 DBA 审计流程补账本
   - 结构部分漂移 → 设计受控修复迁移，不要静默改账本
   - 尚未应用且账本完整 → 重新按 SOP 执行
3. 修复后必须从新备份重跑 121→122→123，不得只验证 123。
4. 不得直接删除生产 `FAILED`/`RUNNING` 账本行。

退出条件：

- 121、122、123 连续账本均为 SUCCESS
- 123 重复执行为 SKIP
- rollback 演练通过
- 表、主键、索引、外键与 JPA 结构一致

### 2. P1 · D-002 迁移脚本失败分支

- 修复 `run-migrations.sh.template` 失败分支的未绑定变量问题
- 增加「迁移 SQL 失败 → 账本 FAILED → 脚本稳定退出」测试
- 不得掩盖原始 SQL 错误信息

### 3. P1 · D-003 固定水印

水印完整文案必须是：

```text
业主视角预览 · 仅内部使用 · 员工姓名/工号/时间
```

建议只保留一个真源：

- 后端 `previewMeta.watermark` 生成完整固定前缀；或
- 前端固定前缀 + 后端员工身份/时间片段拼接

不要继续让后端片段覆盖前端组件的默认固定前缀。

### 4. P1 · D-004 OSIV=false 项目详情

涉及接口：

```text
project/project/info
project/stage/page
project/member/list
```

修复要求：

- 不得依靠临时 EAGER、`@JsonIgnore` 或生产 OSIV=true 兜底
- 需要返回的 Store/Quote/Employee 关联应显式预取或放入事务边界
- 增加 `spring.jpa.open-in-view=false` 的项目详情 App/API 回归
- 修复后，App 项目详情必须能正常渲染“业主视角”入口

## 复验门禁

1. `mvn test`：全量通过
2. `npm run verify:mp-weixin`：通过
3. 在 OSIV=false 隔离 API 上重跑：
   - 项目详情
   - 五类允许角色预览
   - 业务员/财务拒绝
   - 跨企业/门店/团队/非成员/离职
   - 无副作用与审计
4. 微信开发者工具重新截图：
   - 五类入口
   - 业务员/财务无入口
   - 首页/全过程/验收
   - 完整固定水印
   - 错误态、刷新、返回
5. 从新备份重跑正式 121→123 迁移链及回滚演练

## 不得使用的结果

- 不得用 OSIV=true 的成功结果关闭 D-004
- 不得用 SQL 冒烟通过关闭 D-001
- 不得在业主 token 禁止边界下声称已完成真实业主 UI 并排验收
- 复验前不得标记“已验收”“可发布”或恢复对外宣传
