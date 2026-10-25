# 企业运营标准资产 · 设计—蓝图—实现—页面—验收对账

状态：`DEV_VERIFIED（EOP-V4，待独立验收）`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-EOP-001 | 五类资产生命周期统一读取 | `DEC-083` | `StandardAssetLifecycleContract` | `standard-asset/lifecycle/list` | 待 EOP-V6 | `StandardAssetLifecycleContractTest` | PASS | ACCEPTED |
| REQ-EOP-002 | 按语义区分状态集合 | `BR-051` §1 | statuses | 只读投影 | 待 EOP-V6 | `StandardAssetLifecycleContractTest` | PASS | ACCEPTED |
| REQ-EOP-003 | 修订型与版本型区分 | `DEC-083` §2 | versionMode | 只读投影 | 待 EOP-V6 | `StandardAssetLifecycleContractTest` | PASS | ACCEPTED |
| REQ-EOP-004 | 企业自建与基线复制区分 | `BR-051` §4 | sourceMode | 只读投影 | 待 EOP-V6 | `StandardAssetLifecycleContractTest` | PASS | ACCEPTED |
| REQ-EOP-005 | 引用/快照字段冻结 | `BR-051` §3 | referenceFields/snapshotFields | 只读投影 | 待 EOP-V6 | `StandardAssetLifecycleContractTest` | PASS | ACCEPTED |
| REQ-EOP-006 | 不承载金额 | `T12-BIZ-20261022-004` | 无金额节点 | 结构测试 | 不涉及 | 结构测试 | PASS | ACCEPTED |
| REQ-EOP-007 | 内部登录保护 | `AGENTS.md` | Provider Security | `@User Employee` | 待 EOP-V6 | `StandardAssetLifecycleContractTest`、`ProviderSecurityAcceptanceTest` | PASS | ACCEPTED |
| REQ-EOP-010 | 创建套餐草稿 | `DEC-083` §3 | PackageTemplateVersionRow | `pack/template/save` | App 套餐管理 | `PackageLifecycleContextTest` | PASS | ACCEPTED |
| REQ-EOP-011 | 发布不可变版本与重复发布幂等 | `DEC-083` §3 | PackageTemplateVersionRow | `pack/template/publish` | App 发布按钮 | `PackageLifecycleContextTest` 9 例 | PASS | ACCEPTED |
| REQ-EOP-012 | 并发创建下一版 | `DEC-083` §3、`BR-051` §2 | PackageTemplateVersionRow | `pack/template/build` | App 下一版 | 单测 + MySQL 双会话锁前后事务验证 | 锁后新事务读取 v2；冲突映射 409 | ACCEPTED |
| REQ-EOP-013 | 停用当前版本 | `DEC-083` §3 | PackageTemplateVersionRow | `pack/template/disable` | App 停用 | `PackageLifecycleContextTest` | PASS | ACCEPTED |
| REQ-EOP-014 | 报价来源版本与重报校验 | `BR-051` §3 | QuotePackageSourceSnapshot | `quote/save` 字段 | App/Web 报价 | `QuoteCopyContextTest` 负向用例 | 停用/历史套餐重报被拒绝 | ACCEPTED |
| REQ-EOP-015 | 存量迁移/哈希/唯一性/回滚 | `T12-BIZ-20261022-004` | migration 124 | SQL | 不涉及 | 隔离 MySQL 8.4 全矩阵 | 有行 Published；无行 Draft/non-current；hash、唯一、回滚均 PASS | ACCEPTED |
| REQ-EOP-016 | 套餐快照字段进入报价行 | `BR-051` §3 | QuoteLinePackageSnapshot | `packageSectionNameSnapshot` | App/Web 报价 | `HybridQuoteBuilderTest` | type/category/section/craft/cost PASS | ACCEPTED |
| REQ-EOP-017 | App 仅选择发布版 | 用户双端裁决 | provider.package-available | `pack/template/list` | App picker/确认 | App type-check/build、静态审计 | PASS；真实 UI 留 EOP-V6 | ACCEPTED_FOR_EOP_V2 |
| REQ-EOP-020 | 付款/施工模板版本行 | `DEC-083` §4 | CompanyPlanVersionRow | `plan/*/build|publish|disable` | App 计划管理 | CompanyPlanTemplateContextTest 10/10、全量测试 | 同 code 多版本/旧版本不可原地改 PASS | ACCEPTED |
| REQ-EOP-021 | 显式平台升级 | `BR-051` §4 | CompanyPlanVersionRow | `plan/*/build` | App 升级入口 | 重复升级测试、App 静态审计 | 来源 code/version 返回，重复升级返回同一草稿 PASS | ACCEPTED |
| REQ-EOP-022 | 无默认不静默 | `DEC-083` §4.4 | PlanExplicitDefaultPolicy | `resolve*Plan` | 合同选择 | 负向测试 | 无 code 且无 current+default 返回 400 PASS | ACCEPTED |
| REQ-EOP-023 | 全企业默认唯一 | `DEC-083` §4 | PlanExplicitDefaultPolicy | `publish*` | App 默认 | 顺序测试、双线程默认锁诊断 | 顺序与并发发布均默认数 1 PASS | ACCEPTED |
| REQ-EOP-024 | 施工总工期快照 | `DEC-048` | ContractPlanSourceSnapshot | `sourceTotalDays` | 合同/项目详情 | ContractPlanContextTest、显式 120/权重 100、空值 40/60 | 显式 120、空值兜底 100、合同快照 PASS | ACCEPTED |
| REQ-EOP-025 | 多版本回滚归档 | `T12-BIZ-20261022-004` | migration 127 | rollback 127 | 不涉及 | 隔离 MySQL 回滚矩阵 | 计划/明细及合同 source_total_days=120 归档 PASS | ACCEPTED |
| REQ-EOP-026 | 平台升级入口可达 | 用户双端裁决 | CompanyPlanVersionRow | sourceBaselineCode/version | App 计划列表 | type-check/build、静态审计 | 来源字段进入 Provider 和 App，入口可达 PASS | ACCEPTED |
| PLAN-EOP-V3-07 | Build/Publish/Disable/Default 锁先于事务且唯一冲突 409 | `AGENTS.md`、`DEC-029` | Provider/Context | Provider/Context | 不涉及 | 静态审计、定向测试 | Provider 无外层事务；锁→TransactionTemplate→unlock；冲突映射存在 | PASS |

## EOP-V4 对账

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-EOP-030 | 有效变更递增 revision | `DEC-083` §2、`BR-051` §2 | CatalogItem.revision | `catalog/item/update` | App/Web 维护 | `CatalogRevisionWriteTest` | 价格变化 R3→R4 | DEV_PASS |
| REQ-EOP-031 | 不可变审计与快照 hash | `BR-051` §2–3 | CatalogItemRevision | `catalog_item_revision` | App/Web 历史 | `CatalogRevisionContextTest` | Create/Update/Delete 审计字段、sha256=64 | DEV_PASS |
| REQ-EOP-032 | 新报价保存来源 revision | `DEC-083` §2.6 | QuoteCatalogSourceSnapshot | `catalogSourceId/Code/Revision` | App/Web 报价 | `CatalogRevisionContextTest`、既有 Quote Context 回归 | sourceId=20、code=ZC-1、revision=7 | DEV_PASS |
| REQ-EOP-033 | 无变化保存不产生虚假修订 | `DEC-083` §2.5 | effective_change_only | `catalog/item/update` | App/Web 保存 | `CatalogRevisionContextTest`、`CatalogRevisionWriteTest` | scale/sort/sourceType 不算变化；无审计 | DEV_PASS |
| REQ-EOP-034 | 并发旧 revision 409 | `BR-051` §2、`AGENTS.md` | expected_revision_conflict | `CatalogMutationContext` | App/Web 冲突提示 | `CatalogMutationContextTest`、`CatalogRevisionWriteTest` | 锁→独立事务→旧 revision 409，无裸 1062 | DEV_PASS |
| REQ-EOP-035 | 已引用项目删除保护 | `DEC-083` §2.4、`DEC-079` | CatalogItem | `catalog/item/delete` | App 删除 | `CatalogCostMaintenanceTest`、既有引用删除测试 | 有引用拒绝；删除前保留 Delete 审计 | DEV_PASS |
| REQ-EOP-036 | 非成本角色历史裁剪 | `DEC-079`、`BR-051` §6 | cost_masked_revision_history | `catalog/item/revision/page` | App/Web 历史 | `CatalogRevisionPageTest` | Designer 无 snapshotJson，Finance 可见成本快照 | DEV_PASS |
| REQ-EOP-037 | 修订历史企业隔离 | `AGENTS.md`、`DEC-083` §6 | revision_history_company_scope | `catalog/item/revision/page` | App/Web 历史 | Provider 企业参数静态审计 | 只查询登录员工 comId | DEV_PASS |
| REQ-EOP-038 | 迁移/回滚/hash/唯一键 | `T12-BIZ-20261022-004` | migration 128 | `catalog_item_revision` | 不涉及 | 隔离 MySQL 8.4.0 | revision=1、hash 匹配、来源回填、回滚归档 | DEV_PASS |
| REQ-EOP-039 | App/Web revision 与异常语义 | 用户双端裁决 | provider.catalog-revision-history | R{n} + history | App/Web | App/Web type-check、build、静态审计 | 两端携带 revision 并展示历史；真实页面留 EOP-V6 | DEV_VERIFIED_PENDING_REAL_UI |

### EOP-V4 阻断与修复

| 编号 | 事实 | 影响 | 证据 | 状态 |
| --- | --- | --- | --- | --- |
| EOP-V4-B1 | 更新无条件递增 revision | 无变化保存产生虚假历史 | 增加业务值 fingerprint，只有有效变化才递增/写审计 | RESOLVED |
| EOP-V4-B2 | 更新/删除先开事务后加锁或未加锁 | 并发覆盖、审计唯一键 1062 | `CatalogMutationContext`：锁先于 TransactionTemplate；唯一冲突映射 409 | RESOLVED |
| EOP-V4-B3 | 前端未提交期望 revision | 旧页面无法识别并发覆盖 | App/Web 保存、启停、删除均带 revision | RESOLVED |
| EOP-V4-B4 | 审计历史无查询接口 | 用户无法追溯修订 | 新增 `catalog/item/revision/page` | RESOLVED |
| EOP-V4-B5 | 历史快照可能向非成本角色泄漏成本 | 越权查看标准成本 | 非 BOSS/店长/财务将 snapshotJson 置空后再响应 | RESOLVED |
| EOP-V4-B6 | 迁移 128 未验证 | DDL/回填/回滚风险未知 | 隔离 MySQL 8.4.0 全矩阵 PASS | RESOLVED |

## 阻断对账

| 编号 | 事实 | 影响 | 证据 | 状态 |
| --- | --- | --- | --- | --- |
| EOP-V2-B3 | 旧实现事务先于锁开启，锁内普通查询受 REPEATABLE-READ 旧快照影响 | 并发第二次构建可能落到 1062 | 已改为锁后 TransactionTemplate 独立事务；MySQL `lock_a=1`、`tx_after_wait=2`；唯一冲突映射 409 | RESOLVED |
| EOP-V3-B1 | 无默认时选择首个启用模板或平台基线 | 新合同可能使用非用户确认模板 | `resolve*Plan` 仅接受 current+default，空 code 无默认返回 400 | RESOLVED |
| EOP-V3-B2 | 发布不同 code 的模板产生多个企业默认 | 顺序路径已修复；并发已由 B8 验证 | 全企业默认锁与跨 code 测试通过 | RESOLVED |
| EOP-V3-B3 | 后端视图/API 未暴露平台来源，App 无平台升级入口 | 显式平台升级不可用 | 视图/Provider 返回 sourceBaselineCode/version；App model 读取 | RESOLVED |
| EOP-V3-B4 | 127 回滚在已有多版本时失败 | 回滚路径已修复 | 计划/明细归档，旧唯一键恢复 | RESOLVED |
| EOP-V3-B5 | 施工合同快照缺声明总工期，App 用阶段合计冒充标准总工期 | 合同快照和 UI 输入已闭环 | 显式 120/权重 100 PASS；空 totalDays 40/60 保存 100 PASS | RESOLVED |
| EOP-V3-B6 | EOP-V3 需求/蓝图未建立 | 无 REQ→蓝图→API→页面→验收链 | 03/04 已补 REQ-EOP-020~026、对象、字段和 API/页面映射 | RESOLVED |
| EOP-V3-B7 | 重复平台升级不幂等 | 超时重试/重复点击返回 409，而非同一有效草稿 | 相同 sourceBaselineCode 重复 build 返回同一草稿 | RESOLVED |
| EOP-V3-B8 | 并发发布不同 code 产生两个默认 | 违反企业内最多一个已启用默认 | 全企业 default 锁双线程验证默认数 1 | RESOLVED |
| EOP-V3-B9 | 请求 totalDays 为空时保存 1 而非阶段合计 | 空字段客户端声明工期错误，违反 DEC-048 | `saveConstruction` 改为按 request.totalDays 判断；空值 40/60 保存 100 PASS | RESOLVED |
| EOP-V3-B10 | 127 回滚丢失合同 source_total_days | 历史合同声明总工期无法恢复 | 回滚前建合同归档表，MySQL 归档值 120 | RESOLVED |


## 已解决

| 编号 | 原问题 | 当前证据 | 状态 |
| --- | --- | --- | --- |
| EOP-V2-B1 | 停用/历史套餐经 sourceQuoteId 重报 | `QuoteCopyContext` 调用 `requireQuoteSource`；负向用例 PASS | RESOLVED |
| EOP-V2-B2 | 存量 current Published 无 snapshot | 迁移生成 JSON/hash；ensureSnapshot 补冻结；MySQL 实测 hash 非空且匹配 | RESOLVED |
| EOP-V2-B3 | 真实并发事务快照导致 1062 | 锁后独立事务；MySQL 双会话验证；反射测试禁止外层事务 | RESOLVED |
| EOP-V2-B4 | pack_line 快照未完整映射报价行 | QuoteLine 增加分区快照；Hybrid 测试断言 category/section/craft/cost | RESOLVED |

## 未映射

- EOP-V3 设计/需求映射已补齐，REQ-EOP-020~026 与本批实现/API/页面/测试均有映射。
- Web 套餐管理页和 App/Web 真实页面、真机验证仍未完成，按计划归 EOP-V6。
- 套餐默认、平台套餐基线、平台报价项目基线仍由 `GAP-063` 管理，本批未实现。

## 已过期

- 上一轮真实并发 BLOCK、迁移快照缺失、重报绕过和空行问题均已失效。

## 本批结论

- EOP-V1、EOP-V2 均保持 `ACCEPTED`。
- EOP-V3 独立验收 `PASS`：20/20 定向用例、全量测试、App type-check/build、迁移/回滚、并发默认、空/显式总工期和合同归档均通过。
- EOP-V4 实现自测完成：29 个相关测试类 102/102、App/Web 类型检查与构建、迁移 128/回滚均 PASS；状态仅到 `DEV_VERIFIED`。
- 全量后端测试当前被并行会话的 `GoodsReceiptRepository` 派生查询启动错误阻断，修复后必须重跑；该问题不属于 EOP-V4 文件范围。
- EOP-V4 必须交独立验收，重点补真实并发、跨企业、非成本角色和 App/Web 同错误语义证据。
- `PI-20261022-003` 整体仍未完成，不得标记 `CLOSED`。
- 真实 App/Web 页面和双端一致性仍由 EOP-V6 独立验收。
