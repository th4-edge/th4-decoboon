# 交接：REQ-BIZ-20260926-001 报价项目库批量导入 P0

- request_id: `REQ-BIZ-20260926-001`（产品补充 PRD-CATALOG-IMPORT-20260926-001，frozen_for_development）
- lane: development
- 结论：后端 + 小程序 P0 最小闭环已实现并自测；未申报验收通过
- 证据：`_系统/协作/lanes/development/workspace/catalog-import-verify/probe-output.txt`（真实 550 条 + 正反例探针）

## 1. changed_files

后端新增：`catalog/configure/{CatalogImportStatus,CatalogImportRowStatus,CatalogSourceType}.java`、`catalog/model/{CatalogImportBatch,CatalogImportRow}.java`、`catalog/repository/{CatalogImportBatchRepository,CatalogImportRowRepository}.java`、`catalog/context/{CatalogImportParser,CatalogImportContext,CatalogImportFileContext,CatalogImportReferenceContext}.java`、`catalog/service/importbatch/{build,info,page,save,update}/**`、`catalog/service/importrow/page/**`、`catalog/service/importtemplate/build/**`、`src/main/resources/catalog/报价项目库批量导入模板_v1.0.xlsx`、`docs/design/migrations/60-报价项目库批量导入.sql`。
后端修改：`pom.xml`（xlsx 资源纳入构建）、`catalog/model/CatalogItem.java`、`catalog/context/CatalogDetailContext.java`、`catalog/repository/CatalogItemRepository.java`、`catalog/service/items/{save,update,page}/support/*.java`、`design/model/DesignItem.java`、`design/context/DesignCopyContext.java`、`design/service/items/save/support/Save.java`、`quote/model/QuoteLine.java`、`quote/context/{QuoteCalcContext,QuoteContext,QuoteAdjustmentContext,HybridQuoteBuilder,QuoteCopyContext}.java`、`src/test/java/.../ProviderSecurityAcceptanceTest.java`（仅路由计数机械同步）。
小程序：`domains/catalog/{model,api}.ts`、`domains/file/api.ts`、`domains/base/model.ts`、`domains/design/{model,api}.ts`、`config/index.ts`、`pages.json`、`components/CatalogPicker.vue`、`subpackages/quote/pages/catalog/manage.vue`、`subpackages/quote/pages/catalog/import.vue`（新）、`subpackages/quote/pages/catalog/import-result.vue`（新）、`subpackages/design/pages/scheme.vue`。

## 2. implementation_summary / provider_api_contract

| 路由 | 入参 | 返回 | 说明 |
| --- | --- | --- | --- |
| `catalog/import-batch/build` | `@AutoFile StreamFile file` | `CatalogImportBatch` | 上传+解析+暂存预览；同企业同 SHA-256 命中有效批次直接返回原批次；预览不写 `CatalogItem` |
| `catalog/import-batch/info` | `batchId` | `CatalogImportBatch` | 结果页刷新，超过 24h 预览自动置 `Expired` |
| `catalog/import-batch/page` | `current/size/status` | `Page<CatalogImportBatch>` | 最近导入记录（默认 10 条） |
| `catalog/import-batch/save` | `batchId/partialImport/duplicatePolicy` | `CatalogImportBatch` | 单事务确认导入；有错误行时默认拒绝，`partialImport=true` 仅导入合格行；重复确认幂等 |
| `catalog/import-batch/update` | `batchId/status=Revoked` | `CatalogImportBatch` | 24h 内且无下游引用整批撤销；重复撤销幂等 |
| `catalog/import-row/page` | `batchId/status/current/size` | `Page<CatalogImportRow>` | 可导入/重复/疑似重复/错误清单（行号+字段+错误码+原因+原值） |
| `catalog/import-template/build` | 空 | `FileGrant` | V1.0 模板下载（文件名含 V1.0，服务端校验 SHA-256） |

权限：6 条批次/行路由仅 BOSS（`CatalogImportContext.requireBoss`）；模板为已登录员工的只读公共资源；企业归属只取登录员工。
状态机：`Uploaded/Parsed/PendingConfirm/Importing/Imported/PartiallyImported/Failed/Expired/Revoked`（同步实现中前两态为瞬时态）。

## 3. migration_or_model_changes

- `docs/design/migrations/60-报价项目库批量导入.sql`：`catalog_item.cost_price` 可空、新增 `fixed_total_price/import_batch_id/source_type`；`db_design_item.cost_price` 可空 + `fixed_total_price`；`quote_line.fixed_total_price`；新增 `catalog_import_batch`、`catalog_import_row`（含 `(com_id,batch_id,row_no)` 唯一键与批次/状态索引）。
- 本地 dev 库：Hibernate `ddl-auto=update` 建表建列，`cost_price` 可空由迁移语句手工执行（已验证）。
- 正式发布基线 `01-空库基线.sql` 本轮未同步（PRD 要求按 `19-生产发布基线.md` 审计后执行），列为待办。

## 4. tests_and_results

- `mvn -o -q test`：**103 suites / 344 tests / 0 failures / 0 errors / 0 skipped**。
- 真实 550 条端到端探针（`catalog-import-verify/probe-output.txt`，真实麦鸿 550 条数据 + 真实 V1.0 模板生成文件）：
  - P1 模板下载：名称含 V1.0，下载 SHA-256 = `0791273625f65cf6f021089d58284d17aefaab654ddadea50a67766e0f3638a9`（与冻结值一致）。
  - P2 非法表头（改列名）→ `400 表头与 V1.0 模板不一致，缺少列：项目名称`，未创建批次。
  - P3 非 BOSS（Finance）上传 → `403 只有企业负责人可以批量导入报价项目库`。
  - P4–P8：550 条 `ready=550/dup=0/error=0` → 确认导入 `imported=550`（DB `import_batch_id` 计数 550）→ 重复确认幂等不再新增 → 24h 内无引用撤销删除 550 条（计数 0）。
  - P5 同一文件重复上传：指纹命中返回原批次，不重复建批次/项目。
  - P9 撤销后重新上传同一文件：创建新批次（已撤销指纹不再命中）。
  - P10–P14 混合文件：`total=5/ready=2/dup=1/error=2/costPending=2`；整批导入被拒绝；`partialImport=true` 后 `imported=2/skipped=2`；DB 中成本空白项目 `cost_price=NULL`、固定总价项目 `fixed_total_price=1280.00` 且 `material_price=manual_price=0`；错误清单返回行号+字段+错误码+原值（R-04 单位错误、C-03 编码冲突）；撤销后删除 2 条。
  - P15 最近导入记录分页正常。
- 微信临时文件名无扩展名的兼容：`catalog/import-batch/build` 支持可选 `fileName`（小程序上传时传原始名），探针用「无扩展名临时文件 + fileName=mixed.xlsx」导入成功（total=5/ready=2/error=2）。
- 未覆盖：跨企业 HTTP 负例（需第二企业 BOSS 登录夹具，dev 库昭青科技 BOSS 无密码）、真实微信聊天选文件上传（真机）、真实 MySQL 8 迁移演练。

## 5. app_verification

- `npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 719741 B、最大分包 539173 B、总包 2493128 B，符合分包规范）。
- 新增页面：`subpackages/quote/pages/catalog/import.vue`、`import-result.vue`；维护页新增 BOSS 专属模板下载/批量导入/最近导入记录；`CatalogPicker` 与维护页固定总价按独立字段展示，成本区分“待维护 / ¥0.00（明确零成本）”。

## 6. risks / blocked

- `fixed_total_price` 已贯穿设计分项与报价行；`ContractLine` 仍以最终金额快照承载固定总价（未加独立列），合同/项目成本链路按金额与成本快照计算，未改动财务主链语义。
- 成本完整性：`null` 已在报价项目/设计分项传播为待维护；但 `ProjectCostContext` 仍沿用“成本≤0 视为不完整”的历史判定，**明确 0 成本视为完整事实**尚未落地（需给报价/合同/项目行增加完整性标记），列为 GAP 待产品/验收确认。
- 正式基线 SQL、生产迁移演练、第二企业跨企业负例、真机微信选文件与真机预览/撤销未执行。
- `ProviderSecurityAcceptanceTest` 路由计数已由 development lane 机械同步 315→322（protected 303→310），待 acceptance 复核。

## 7. acceptance_needed

- 按 PRD §14 覆盖：模板完整性、550 条导入耗时与结果、重复文件/重复项目/编码冲突、错误行与部分导入二次确认、成本待维护与明确 0 成本、固定总价报价不乘工程量、跨企业拒绝、24h 撤销与下游引用阻断、导入后立即可在设计/套餐/报价读取。
- 复核路由计数同步与 `catalog/item/import` 旧路由保留策略（PRD §12.3）。
