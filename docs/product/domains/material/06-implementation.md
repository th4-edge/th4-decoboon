# 材料履约与材料业务助手 · 实现记录

## B1 业务事实与蓝图

- Batch：B1
- 产物：`00-charter.md`、`01-scenarios.md`、`02-business-sync.md`、`03-blueprint.json` v1.0.0、字段字典与对账矩阵；
- 后端：核对 `97d5705` 已实现的模板字段投影、提交必填门禁与 `material/sheet/cancel`；
- 测试：`MaterialSheetOrderFlowTest`、`MaterialSheetTemplateFieldConfigTest`、`MaterialSheetEnumsProviderTest`、`MaterialSheetTemplateWhitelistTest`、`MaterialSheetResponseProjectionContractTest` 通过；
- 无 DDL、无迁移、无部署。

## B2 范围

- Batch：B2
- 垂直切片：Web 业务蓝图中心 + 项目材料业务体检；
- 角色：Boss / StoreManager / Finance；
- 不含：App、只读材料业务助手运行时、独立验收、正式发布；
- 不改金额/成本/状态/权限语义，不新增实体表或 DDL。

## B2 后端变更

- `blueprint/runtime/info`
  - 新增 `BlueprintRuntimeQuery`、`BlueprintRuntime`、`BlueprintRuntimeContext` 与 Provider；
  - 只允许 Boss / StoreManager / Finance；
  - 从 `src/main/resources/blueprints/material-v1.json` 读取发布副本，返回版本、状态、最近核对提交、来源数量和规范化蓝图 JSON；
  - 测试校验运行时资源与领域包 `03-blueprint.json` 完全同源。
- `blueprint/check/report`
  - 新增 `MaterialHealthQuery`、`MaterialHealthReport`、`MaterialHealthIssue`、`MaterialHealthRow`、`MaterialHealthContext` 与 Provider；
  - 复用 `MaterialPlanWorkbenchContext` 的计划/选材/下单/到场聚合；
  - 复用 `FinanceReportContext` / `ProjectCostContext` 的成本完整性、材料应付、实际成本与结算状态；
  - 采购单只读取有效状态，补充未定价与缺供应商校核；
  - 成本不完整时 `profitAmount=null`，不使用 0 冒充利润；
  - 通过 `TenantContext.requireEmployee` 先恢复当前员工上下文，再进入财务与材料范围校验，修复 Provider 注入员工懒关联导致的误判 404。
- `@Properties` 白名单覆盖报告、校核项和聚焦材料行；API 不返回 Project/Employee/Company 实体全量。

## B2 Web 变更

- 新增 `/business-blueprints`：
  - 阶段轴、阶段触发/产出/账务/角色/异常/来源；
  - 对象图谱与关键字段；
  - 字段字典搜索与对象筛选；
  - 规则、校核、例外、未决项和来源引用；
  - Boss / StoreManager / Finance 静态路由与菜单入口。
- 新增 `MaterialHealthPanel` 并接入 `/finance/projects/:projectId` 的“总览与成本”：
  - 当前阶段、下一步、结算状态；
  - 材料计划/选材/下单/到场/逾期指标；
  - 未定价、缺供应商、成本不完整校核；
  - 材料应付、预计成本、实际成本、利润；
  - 聚焦材料行与主材下单/结算入口；
  - 空态、失败态和重试恢复。
- 新增领域层 `src/domains/blueprint/{model,api}.ts`，不把业务状态在页面本地拼装。

## 验证

- 后端定向测试：
  `mvn -q -Dtest=MaterialBlueprintRuntimeTest,MaterialHealthContextTest,MaterialSheetOrderFlowTest,MaterialSheetTemplateFieldConfigTest,MaterialSheetEnumsProviderTest,MaterialSheetTemplateWhitelistTest,MaterialSheetResponseProjectionContractTest test`
  → PASS；
- 后端编译：`mvn -q -DskipTests compile` → PASS；
- Web 类型检查：`npm run type-check` → PASS；
- Web 构建：`npm run build` → PASS；
- Web 治理检查：`check:forms`、`check:concepts`、`check:states`、`check:actions`、`check:metrics`、`check:permissions`、`check:page-system`、`check:workbenches`、`check:workspaces` → 全部 exit 0（metrics 保留既有口径提示，与本批无关）。

## 真实 Provider 冒烟

- `blueprint/runtime/info`：PASS，`material.end-to-end` v1.0.0，9 阶段 / 12 对象 / 26 字段；
- `blueprint/check/report`：PASS，真实项目 `P1791842095079` 返回 1 条部分到场材料、1 张未定价采购、成本不完整 1 项，阶段为“到场确认”；
- 非法蓝图 ID：404；
- Designer 访问蓝图：单元测试断言 403。

## 风险与未闭环

- 独立验收未执行，B5 才能宣称 `ACCEPTED`；
- 测量模板门禁只证明 Tile 基线，不能宣称全品类；
- `GAP-MAT-TIME-SEMANTICS` 仍未裁决；
- 蓝图运行时资源是领域包的发布副本，后续修改蓝图必须同步更新并通过同源测试；
- 本地验收使用造数账号，临时重置密码哈希后已恢复原哈希；不涉及生产、权限、金额或业务数据。

## B3 范围

- Batch：B3
- 垂直切片：App 业务体检 + 页面上下文说明；
- 角色：Boss / StoreManager / Finance（材料体检）；Designer / Pm（作业单时间与测量上下文）；
- 不含：新 Provider、新业务规则、DDL、金额/成本语义变化、B4 助手、B5 独立验收、正式小程序发布。

## B3 App 变更

- `src/domains/material/model.ts`、`src/domains/material/api.ts`
  - 新增 `MaterialHealthReport` / `MaterialHealthIssue` / `MaterialHealthRow` 只读模型；
  - 新增 `materialHealthApi.report(projectId)`，消费 B2 的 `blueprint/check/report`。
- `src/components/MaterialHealthCard/MaterialHealthCard.vue`
  - 项目详情展示阶段、指标、材料应付/实际成本、成本不完整、利润为空、校核项和聚焦行；
  - 提供刷新、材料计划、主材作业单入口；
  - 加载、空态、失败态与重试。
- `src/components/MaterialSheetContextGuide/MaterialSheetContextGuide.vue`
  - 解释作业单当前状态与下一步；
  - 解释测量、配送/安装、要求到场/实际到场的时间口径；
  - 标出模板必填测量字段、未定价/部分结算、已进入采购锁定与取消边界。
- `src/subpackages/project/pages/material-sheet-detail.vue`
  - 接入上下文说明卡。
- `src/subpackages/project/pages/material-sheet-create.vue`
  - 增加材料行来源、合同外走增减项、模板字段、作业单不承载价格说明。
- `src/subpackages/project/pages/detail.vue`
  - Boss/Store/Finance 项目详情接入材料体检；与财务报告并行读取，体检失败不阻断项目主体。
- `src/domains/material/meta.ts`
  - 新增纯解释型 `materialSheetGuidance` 与时间口径常量。
- `src/core/errorHelp.ts`、`src/content/helpCenter.ts`
  - 新增“作业单时间语义”和“作业单取消边界”帮助主题及错误匹配。

## B3 验证

- `npm run type-check` → PASS；
- `npm run build:mp-weixin` → PASS；
- `npm run check:ui` → PASS；
- `npm run check:subpackages` → PASS（主包 894877 B / 最大分包 643571 B / 总包 3583639 B）；
- 微信开发者工具 automator：
  - 项目详情材料体检真实数据 PASS；
  - 作业单详情上下文说明 PASS；
  - 建单页来源说明 PASS；
  - 帮助主题 PASS；
- H5 390×844：
  - 项目详情、作业单详情、建单页无横向溢出；
  - 作业单详情显示模板必填提示；
  - 建单页显示行来源与价格边界；
  - 正常页面 Console Errors = 0；
- 失败态：H5 网络拦截 `blueprint/check/report` 返回 500，页面显示“材料业务暂不可确认”与重试。

## B3 证据与清理

- 证据目录：`docs/product/domains/material/evidence/B3/`；
- OCR 文本：`evidence/B3/ocr-text.txt`；
- 临时 Tile 草稿已删除；
- 本地造数账号密码哈希临时重置后已恢复；
- 无 DDL、无迁移、无生产发布。
