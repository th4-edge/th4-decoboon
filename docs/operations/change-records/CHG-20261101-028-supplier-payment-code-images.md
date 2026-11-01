# CHG-20261101-028 · 供应商付款码多图与 Web 对象详情工作台

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-028` |
| 类型 | 功能补齐 / UI 重构 / DDL |
| 风险级别 | L3 |
| 状态 | DEV_VERIFIED_PENDING_USER |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/业务裁决 | 用户明确要求供应商支持多图上传与预览；图片唯一用途是记录供应商付款码；Web 详情需按 UI Craft 重做。`INFO-MATERIAL-009`、`BR-019`、`BR-045` |

## 2. 目标与范围

- 目标：供应商实体支持最多 9 张私有付款码图片；App/Web 支持多图上传、删除、保存和预览；Web 改为独立供应商对象详情工作台。
- 范围内：Partner 实体、迁移 134、私有文件 Scope `Partner`、Provider 响应字段、App 供应商编辑、Web 供应商详情、权限负向和 UI Craft 渲染门禁。
- 非范围：不把付款码当作支付凭证或资金流水；不自动识别二维码内容；不发起付款；不新增 Web 专用 API；不生产发布。
- 受影响端：后端、App、Web。

## 3. D1 / D1.5 页面意图

- 页面类型：对象详情工作台。
- 目标角色：BOSS、店长、财务；PM 只能查看供应商基础档案，不能读取付款码。
- 触发事件：付款前核对该供应商收款主体，或新供应商需要补录付款码。
- 三秒结论：供应商名称、合作状态、联系人、未付余额和现有付款码。
- 唯一主行动：上传/维护付款码并保存；编辑基础资料为次要动作。
- 三层信息：对象身份与余额 → 付款码证据/结算与往来 → 合作备注和审计延伸。
- Web 提升点：多图并发维护、大图预览、桌面密度、固定侧栏事实对照。
- 七类状态：加载骨架、真实空态、失败重试、无权限提示、长备注换行、0 余额、1–9 张图片。
- 2.5D/3D：不适用，付款码核验不需要空间关系。

## 4. 业务与数据

- `INFO-MATERIAL-009` 已登记为 Evidence，主源为供应商档案，付款码不是付款凭证。
- 图片只存私有对象键；展示必须经 `file/private/grant` 签发短期 URL。
- 图片访问角色：BOSS、店长、财务；PM 不返回短期 URL，直接授权请求返回 403。
- 图片类型：JPG/PNG/WEBP；最多 9 张、10MB/张；删除只解除档案引用。
- 公开 API 仍为 `partner/page|save|update`，无 Web 专用供应商接口。

## 5. 验证

| 检查 | 命令/页面 | 结果 | 证据 |
| --- | --- | --- | --- |
| 后端编译 | `mvn -DskipTests compile` | PASS | 本次执行 |
| 定向测试 | `mvn -Dtest=PartnerPaymentCodeImageTest,MigrationNumberingTest test` | PASS | `target/surefire-reports` |
| 全量测试 | `mvn clean test` | 1,201 tests / 5 failures / 1 error；全部集中在既有统计、薪酬、报价分页和 Provider 数量基线，独立复跑同源失败；本次供应商/迁移测试通过 | `target/surefire-reports/` |
| Provider 契约 | `audit-provider-response-contract.py --check/--self-test` | PASS：547 providers，0 findings | 本次执行 |
| Web 构建 | `npm run build` | PASS | `dist/` |
| App 构建 | `npm run build:mp-weixin -- --mode development` | PASS | `dist/build/mp-weixin` |
| App 真实加载 | 供应商编辑页 | PASS：付款码区、1/9 计数、上传入口；实际已有付款码可加载 | 页面 DOM（付款码图片按敏感数据不落公开证据） |
| Web 上传预览 | 测试供应商详情 | PASS：上传后保存、刷新回读、点击放大预览 | `logs/web-supplier-detail-final-1440.png`、`-1280.png`、`-1024.png` |
| 权限负向 | PM token 请求 Partner 图片授权 | PASS：403，随后恢复 Finance 角色 | HTTP 响应 |
| 三档渲染 | 1440/1280/1024 | PASS：无横向溢出 | 三档截图 |

## 6. 数据库与回滚

- 迁移：`docs/design/migrations/134-供应商付款码图片.sql`
- 回滚：`docs/design/migrations/rollback/134-供应商付款码图片-rollback.sql`
- 本地应用前备份：`data/backups/prod-import/20261101-213046/local-account-mapping/decoboon_prod_20261101-before-payment-codes.sql.gz`
- SHA-256：`71bfacee91eaa1a14422a85242469cca82b17947a85d02c1a99fe85a396b2316`
- 回滚演练：隔离库执行 133 后付款码行数 1；执行 rollback 后付款码表数量 0；临时库已删除。
- 发布包序号：`MIGRATION_END=133`。
- 生产状态：未应用、未发布。

## 7. UI Craft 视觉批判

- 轮次 1：视觉桥认可整体工作台层级，指出上传按钮不够突出、空态区域偏紧。
- 修正：增强付款码上传按钮对比度，增加空态容器留白；保留状态标签和金额表达，拒绝泛化建议。
- 轮次 2：页面层级、排版、可信度和无模板感均通过；剩余意见属于测试数据完整度或通用建议，不作为阻断。
- 最终证据：`logs/web-supplier-detail-final-1440.png`、`-1280.png`、`-1024.png`；视觉桥输出 `logs/web-supplier-detail-final-1440-vision-review.md`。
- `VISUAL_REVIEW_UNAVAILABLE`：仍未进行人工视觉签字或更强视觉模型复核。

## 8. 数据清理

- 联调测试供应商已删除。
- 测试付款码图片已从对象存储删除并 HEAD 验证 404。
- 未删除用户已有供应商付款码。

## 9. 结论

- 最终结论：开发自测通过，状态 `DEV_VERIFIED_PENDING_USER`。付款码多图、私有授权、App/Web 上传预览和 Web 对象详情工作台已完成；因存在 6 个与本功能无关的既有测试失败，本批不得宣称全量回归通过或生产可发布。
- 全量测试已知失败：`StatisticStoreAggregateContextTest` 3、`PayrollCompensationScopeTest` 1、`DesignerQuoteStoreManagerScopeTest` 1、`ProviderSecurityAcceptanceTest` 1；隔离复跑均可复现，不由供应商付款码改动引入。
- 未执行项：生产 DDL、生产发布、真机微信选图、人工视觉签字、更强视觉模型复核。
