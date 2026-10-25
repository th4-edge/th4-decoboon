# 企业运营标准资产 · 实现记录

## EOP-V1 · 生命周期契约与只读投影

- 批次状态：`DEV_VERIFIED`
- 变更级别：L1（新增只读契约与 Provider，无 DDL、无金额、无权限放宽）
- 业务依据：`DEC-083`、`BR-051`、`T12-BIZ-20261022-004`

### Changed files

- `src/main/java/com/th4/edge/decoboon/standardasset/configure/StandardAssetType.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/configure/StandardAssetVersionMode.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/configure/StandardAssetSourceMode.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/configure/StandardAssetDefaultPolicy.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/model/StandardAssetStatusContract.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/model/StandardAssetLifecycleContract.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/context/StandardAssetLifecycleContext.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/service/lifecycle/list/List.java`
- `src/main/java/com/th4/edge/decoboon/standardasset/service/lifecycle/list/support/List.java`
- `src/test/java/com/th4/edge/decoboon/standardasset/StandardAssetLifecycleContractTest.java`

### API contract

- Route：`standard-asset/lifecycle/list`
- 入参：`Employee`（`@User`）
- 响应：`List<StandardAssetLifecycleContract>`
- 序列化：具体 Provider 使用 `@Properties` 白名单。
- 认证：Provider 接口继承 `Security`；实现调用 `TenantContext.requireEmployee`。
- 五类资产：报价项目库、套餐模板、付款计划模板、施工计划模板、单据/凭证模板。
- 状态集合按资产语义区分，不返回统一状态机。
- 默认策略：报价/凭证不适用；套餐 `Deferred`（`GAP-063`）；付款/施工为可选且最多一个。
- 来源模式：报价/套餐企业自建；付款/施工/凭证为平台基线复制且不自动升级。
- 每类资产返回引用字段、快照字段和历史策略。

### Tests / build

```text
mvn -q -Dtest=StandardAssetLifecycleContractTest test
PASS

mvn -q -DskipTests compile
PASS

git diff --check
PASS
```

定向测试共 10 项，覆盖：

1. 恰好五类标准资产；
2. 不同资产使用不同状态集合；
3. 发布/启用状态允许新业务，草稿不允许；
4. 报价项目库为 revision，其余为不可变版本；
5. 平台基线仅适用于付款/施工/凭证；
6. 套餐默认延后、付款/施工默认最多一个；
7. 每类资产均有引用和快照字段；
8. 契约类不定义金额/成本计算字段；
9. Provider 路由、Security 和 `@Properties` 白名单存在；
10. 报价项目库停用后仍可编辑修订，但不得用于新业务。

### 明确未改

- 无 DDL、迁移、数据库写入。
- 无权限、角色、数据范围变化。
- 无金额、成本、利润、合同、应收应付或凭证金额公式变化。
- 无 App/Web 页面变更。
- 未实现 EOP-V2～V6。

### 回滚

删除 `standardasset` 新增包和对应测试即可；无数据回滚。

## EOP-V2 · 套餐不可变版本与报价来源版本

- 批次状态：`DEV_VERIFIED`
- 变更级别：L2/L3（版本模型、迁移、报价来源与跨企业边界）
- 业务依据：`DEC-083` §3、`BR-051`、`T12-BIZ-20261022-004`

### 核心实现

- `pack_template` 每条记录代表一个版本；同一 `(com_id, code)` 下 `version` 递增。
- `status` 使用 `Draft/Published/Disabled`；`currentPublished` 标识唯一可承接新报价的发布版本。
- `pack/template/build` 从最新版本创建下一版草稿；`publish` 生成不可变快照 JSON + SHA-256；`disable` 停止新报价。
- `pack/template/list` 只返回当前发布套餐，供新报价选择。
- `quote` 记录 `package_template_code/version/snapshot_hash`；新报价仅允许当前发布版本。
- `pack_line` 固化分区名、项目类型、分类、工艺和标准成本，报价生成使用版本快照值。
- 未实现套餐默认和平台套餐基线（`GAP-063`）。

### DDL / 兼容

- 迁移：`docs/design/migrations/124-套餐模板不可变版本.sql`
- 回滚：`docs/design/migrations/rollback/124-套餐模板不可变版本-rollback.sql`
- 存量套餐：`code=PACK-<templateId>`、`version=1`、`status=Published`、`currentPublished=1`。
- 存量行快照和存量报价来源版本回填；未伪造更早版本。
- `scripts/build-release.sh` 的 `MIGRATION_END` 已上调至 124。

### 迁移验证

在隔离 MySQL 8.4 临时实例上完成：

1. 构造存量 `pack_template/pack_line/quote` 数据；
2. 执行迁移 124，验证编码、版本、发布状态、行快照和报价来源版本回填；
3. 执行回滚 124，验证新增列全部移除且原表数据保留；
4. 未触碰本机 `decoboon` 开发库或生产库。

### 测试与构建

```text
mvn -q -Dtest=PackageLifecycleContextTest,PackageCostContextTest,PackageProviderInjectionContractTest,PackageTemplateMaintainRoleTest,QuoteContextBusinessRulesTest,HybridQuoteBuilderTest,QuoteCopyContextTest,ProviderSecurityAcceptanceTest,DecoboonClosedLoopTest test
PASS（25 个用例，0 失败）

App: npm run type-check
PASS

App: npm run build:mp-weixin
PASS

App: npm run check:ui
BLOCKED_BY_UNRELATED：src/App.vue:66 既有 uni.showModal/showActionSheet 规范问题，不在 EOP-V2 文件范围
```

全量后端测试已尝试运行 974 个用例；失败项均落在当前工作区并行会话的在途改动（QuoteLineRepository `@EntityGraph`、财务/收款/薪酬等），不在 EOP-V2 新增/修改文件范围。EOP-V2 定向回归全通过。

### App

- 套餐管理页显示状态/版本，支持草稿编辑、发布、创建下一版、停用。
- 套餐选择器、报价确认页改用 `pack/template/list`，不再展示草稿/历史/停用版本。
- 已发布版本深链接进入编辑页会提示并返回。
- 真实小程序页面/真机验收和 Web 套餐管理页仍属 EOP-V6，不在本批宣称 UI 通过。

### 回滚

- 代码回退到上一稳定提交；
- 数据库仅在业务确认可丢弃版本数据后执行回滚脚本；
- 回滚前必须备份 `pack_template/pack_line/quote`。


### EOP-V2 独立验收补充证据（2026-10-23）

- 独立验收：`PASS`，30 个后端定向用例 0 失败、0 错误。
- 并发事务：`Build/Publish/Disable` Provider 无外层事务；先拿 Redisson 锁，再通过 `TransactionTemplate` 开独立事务。
- MySQL 8.4 `REPEATABLE-READ` 双会话：
  - 旧模式事务先开再拿锁：`before_lock=1`、`after_lock=1`；
  - 新模式先拿锁再开事务：A 提交 v2 释放锁后，B `tx_after_wait=2`。
- 唯一版本冲突稳定映射 409；数据库 `current_published_code` 唯一索引兜底。
- 存量迁移生成 `snapshot_json` 与 `snapshot_hash`；无行存量降为 Draft/non-current；`quote_line.package_section_name_snapshot` 回填。
- App 最新 `type-check` 与微信构建通过。
- 真实 App/Web 页面和真机仍留 EOP-V6。

## EOP-V3 · 付款/施工计划模板不可变版本与显式平台升级

- 批次状态：`DEV_VERIFIED`
- 业务依据：`DEC-083` §4、`BR-051`、`DEC-048`、`T12-BIZ-20261022-004`

### 核心实现

- 付款计划、施工计划改为版本行：同一 `(com_id, code)` 下 `version` 递增。
- `is_current_enabled` 标识唯一可用于新合同的企业版本；数据库生成列唯一索引兜底。
- 已启用/已停用版本不可原地修改；`build` 创建下一版草稿。
- `build` 可携带 `sourceBaselineCode`，显式从平台基线创建升级草稿；平台升级不自动同步。
- 合同解析付款/施工模板时仅使用 `Enabled + currentEnabled` 版本；未指定 code 时只从当前启用默认中选择。
- 合同继续保存 `sourceCode/sourceVersion` 和完整节点/阶段快照，历史合同不回溯。
- 发布新版本时清理旧当前版本的 current/default 标记；停用清默认，不退回旧版本。
- 并发写采用“先 Redisson 锁，再 TransactionTemplate 独立事务”；唯一键冲突映射 409。
- App 付款/施工管理页支持最新草稿编辑、启用、创建下一版、显式升级平台基线、默认和停用；历史版本只读。

### DDL / 迁移

- 迁移：`docs/design/migrations/127-付款施工计划模板不可变版本.sql`
- 回滚：`docs/design/migrations/rollback/127-付款施工计划模板不可变版本-rollback.sql`
- 有节点/阶段的启用存量模板回填为 `currentEnabled=1`；无明细启用模板降为 Draft/non-current。
- 唯一键从 `(com_id, code)` 改为 `(com_id, code, version)`，并增加 `(com_id, current_enabled_code)`。

### 验证

```text
公司计划模板/合同/基线/Provider 安全定向测试：15/15 PASS
App type-check：PASS
App build:mp-weixin：PASS
```

隔离 MySQL 8.4 已验证：

- 有明细 Enabled → `currentEnabled=1`；无明细 Enabled → Draft/non-current。
- 同 code 不同 version 可共存。
- 第二个 currentEnabled 版本插入返回 1062。
- 迁移回滚后新增列与索引移除，原数据行保留。


### EOP-V3 独立验收结论（2026-10-23）

- 独立验收：`PASS`。
- 定向测试：20/20 PASS。
- 后端全量测试：1032 tests，0 failures / 0 errors / 0 skipped。
- App type-check 与微信构建 PASS。
- 全企业默认锁并发验证：不同 code 同时发布后企业默认数为 1。
- 自定义施工模板：未传 totalDays 时按阶段权重合计兜底；显式声明值优先，不被阶段合计覆盖。
- 127 多版本回滚：计划、节点、阶段和合同 sourceTotalDays 全部归档；旧唯一键恢复。
- V3 需求/蓝图/对账已完整映射。

## EOP-V4 · 报价项目库 revision、不可变审计与下游来源修订

- 批次状态：`DEV_VERIFIED`
- 业务依据：`DEC-083` §2、`BR-051`、`DEC-079`、`T12-BIZ-20261022-004`

### 核心实现

- `CatalogItem` 增加 `revision`，保持 `Active/Inactive` 生命周期，不改成发布型模板。
- 新增不可变 `CatalogItemRevision`：保存项目、revision、变更类型、操作人、时间、业务快照 JSON 和 SHA-256。
- 新增 `catalog/item/revision/page` 只读历史接口；全部内部员工可读修订元数据，成本快照仅 BOSS、店长、财务可见。
- 更新路径使用 item 级 Redisson 锁，锁内通过 `TransactionTemplate` 开启独立事务，再读取当前项目并校验客户端期望 revision。
- 客户端携带旧 revision 时稳定返回 409；无业务变化不递增 revision、不新增审计。
- 报价生成、混合报价、调整、复制和视图复制均保存 `catalogSourceId/catalogSourceCode/catalogSourceRevision`。
- 删除保护继续由 `CatalogImportReferenceContext` 阻断已引用项目；未引用项目删除前写 Delete 审计。
- App/Web 列表和详情展示 `R{n}`；App 维护页与 Web 详情提供变更历史，异常状态可重新加载。
- App 保存、启停、删除与 Web 保存请求均携带期望 revision；新报价继续读取报价行当时业务快照。


### DDL / 迁移

- 迁移：`docs/design/migrations/128-报价项目库修订与审计.sql`
- 回滚：`docs/design/migrations/rollback/128-报价项目库修订与审计-rollback.sql`
- 存量 `catalog_item` 回填 `revision=1`，生成 Create 审计与快照 hash。
- 存量 `quote_line.catalog_item_id` 回填来源 ID/code/revision。
- 回滚先归档审计表和报价来源修订，再删除新增字段；不逆向删除已生成历史事实。


### 定向验证

```text
mvn -q -Dtest=CatalogRevisionContextTest,CatalogRevisionWriteTest,CatalogRevisionPageTest,CatalogMutationContextTest,CatalogCostMaintenanceTest,CatalogImportCostBoundaryTest test
PASS（16 个用例，0 失败）
```

隔离 MySQL 8.4.0 已验证迁移 128：

- 存量项目 `revision=1`，Create 审计 1 条，hash 长度 64 且与 `SHA2(snapshot_json,256)` 一致。
- 存量报价行回填 `catalog_source_id=20`、`catalog_source_code=ZC-1`、`catalog_source_revision=1`。
- 唯一键 `uk_catalog_revision_item_revision` 为非唯一值 0 的联合唯一索引。
- 回滚后新增列/索引移除；审计与报价来源修订归档表保留数据。

### 收口验证（2026-10-23）

```text
后端 catalog/pack/plan/quote/standardasset 定向回归：102/102 PASS（29 个测试类，0 failure / 0 error）
后端 compile：PASS
App type-check：PASS
App build:mp-weixin：PASS
App check:client-copy：PASS
Web type-check：PASS
Web check:client-copy：PASS
Web production build：PASS
隔离 MySQL 8.4.0 迁移 128 + rollback：PASS
```

- 全量后端测试已执行，1070 tests 中 258 errors；根因是并行会话未收口的 `GoodsReceiptRepository.findByPurchaseOrder_Project_ProjectIdOrderByReceiptIdDesc` 派生查询把 `PurchaseOrder` 解析成不存在的 `order` 属性，44 个 Spring 上下文测试报同一启动错误。
- 该失败不涉及 EOP-V4 文件；未越界修改采购仓储。全量门禁仍需并行专项修复后重跑。
- 真实 App/Web 页面、多分辨率/真机与独立验收仍留 `EOP-V6`；当前不得写成 `ACCEPTED`。
