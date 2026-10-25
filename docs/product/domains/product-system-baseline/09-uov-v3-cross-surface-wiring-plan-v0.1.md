# UOV-V3 报价场景跨端接线准备单 v0.1

> 日期：2026-10-23
>
> 当前状态：`PASS / ACCEPTED`；U3-A~F 第二轮独立复验通过；P1-01/P1-02/P2-01 已关闭
>
> 前置：U1 契约独立验收 PASS；U2 服务端 REGION/TYPE 双投影独立验收 PASS
>
> 依据：`ANS-BIZ-20261022-007`、`T12-BIZ-20261022-007`、`DEC-086`、`BR-052`、`GAP-053`、`GAP-065`

## 一、本切片目标

让同一 `quoteId + version` 的报价在员工 App、Web、业主端、项目入口、合同摘要和 PDF 入口消费服务端统一投影，不在前端重新合并、重算或失败回退。

本切片的主闭环：

```text
服务端 regionGroups/typeGroups
→ App 员工报价详情按服务端分组展示
→ Web 报价工作区按服务端分组展示
→ 业主报价按服务端分组展示
→ 项目来源报价/合同摘要跳统一报价视图
→ PDF 入口传递 quoteId + version + groupMode
```

## 二、不做范围

- 不在 U3 重构合同上游事实链（U4）。
- 不在 U3 实现项目 PHASE 事实索引、历史折叠和返回锚点（U5）。
- 不在 U3 完成 PDF REGION/TYPE 渲染、旧归档回读和最终全链验收（U6）。
- 不新增 DTO/VO；不改变金额、成本、利润、权限或状态机。
- 不新增 DDL/迁移；不把前端 fallback 作为正常业务路径。

## 三、现状对账

| 消费面 | 当前实现 | U3 缺口 | 目标 |
| --- | --- | --- | --- |
| App 报价模型/API | 仅归一化 `regionGroups`；`QuoteGroup` 无 `groupMode`，报价行无 `source*` | 未消费 U2 `typeGroups/groupMode/source fields` | 补齐服务端投影类型和归一化 |
| App 员工报价详情 | `detail.vue` 从 `regionGroups` 回退到本地空间分组；`categoryView` 本地合并、累加金额和来源计数 | 前端业务合并/回退仍存在 | 使用服务端 `regionGroups/typeGroups`；成功响应不本地重新分组；失败显示暂不可确认 |
| App 项目报价入口 | 通过报价详情/项目经理来源报价展示；U2 已提供 region/type groups | 未明确消费 `groupMode`、来源和 PROJECT 场景 | 按 PROJECT 场景展示分组事实，不返回原始 items |
| App 业主报价 | `owner-quote.vue` 消费 journey `priceGroups`；后端 `CustomerJourneyContext.quoteGroups` 另建 PriceGroup，并读取实时设计/目录 `criterion` | 与 U2/U1 不同源；无 type/groupMode/source 契约 | 业主 journey 报价段绑定 `quoteId + version` 并由统一投影生成；只给销售字段白名单 |
| Web 报价模型/API | 仅归一化 `regionGroups`，无 `typeGroups/groupMode/source*` | 未消费 U2 投影 | 补齐类型、分组模式和来源字段 |
| Web 报价工作表 | `quote-sheet.ts` 仅按区域映射；`regionGroups` 缺失时本地按 `space.name` 回退 | 前端回退和本地分组仍存在 | 服务端投影直出；失败显示暂不可确认，不重算 |
| Web 设计师报价工作区 | 使用 `toSheetGroups`，无 REGION/TYPE 选择；PDF 既有入口只传 `quoteId` | 无分组切换、无版本参数 | 增加 REGION/TYPE 视图状态和 PDF 参数 |
| 合同摘要 | 合同工作台只暴露报价摘要 `quoteId/no/title/amount/status/version` | 报价节点未跳统一 REGION/TYPE 视图 | 保留摘要，跳转统一报价视图；完整上游链留 U4 |
| PDF 入口/后端 | `document/api.ts` 和 `quote/build` 只传 `quoteId`；`QuotePdfRenderer` 固定区域 | `quoteId + version + groupMode` 未贯通 | U3 贯通参数和入口；真实渲染/归档验收留 U6 |

## 四、实现顺序

### U3-A · 契约适配

- App：`src/domains/trade/model.ts`、`src/domains/trade/api.ts`
- Web：`src/domains/trade/model.ts`、`src/domains/trade/api.ts`
- 增加 `typeGroups`、`groupMode`、`typeRowId/sourceLineIds/sourceRegionIds/sourceRegionNames/sourceCount`。
- 不改服务端结构；只消费 U2 已验收字段。

### U3-B · App 员工报价详情

- `src/subpackages/quote/pages/detail.vue`
- 成功响应优先使用服务端 `regionGroups/typeGroups`。
- 删除/停用本地 `mergeKey`、`categoryView` 合并和 `sourceCount` 自造逻辑。
- 增加 REGION/TYPE 视图选择；失败态显示“暂不可确认”，不 fallback 到本地分组。
- 保留服务端权威金额、数量、成本字段；不做前端求和。

### U3-C · Web 报价工作区

- `src/domains/trade/quote-sheet.ts`
- `src/views/designer/DesignerQuoteWorkspaceView.vue`
- 使用服务端 `regionGroups/typeGroups` 映射表格。
- 移除 `toSheetGroups` 的本地空间回退和金额循环累加。
- 增加 REGION/TYPE 视图切换；可编辑动作仍回到原始 `itemId`。
- 服务端读取失败不显示伪造空报价。

### U3-D · 业主报价 journey

- App `src/subpackages/project/pages/owner-quote.vue`、`src/domains/journey/*`
- 后端 `CustomerJourneyContext` 的报价段必须复用统一投影口径，绑定明确 `quoteId + version`。
- 不实时追加 `criterion`；如展示验收标准，只能标注“按来源核对”。
- 业主只接收销售价、数量、单位、折扣、税额、总额、版本、接受时间和区域/类型来源摘要；不返回成本、公式、供应商价、`acceptedBy`。

### U3-E · 项目/合同入口

- 项目来源报价使用 U2 PROJECT 投影；合同摘要保留绑定报价摘要并跳统一报价视图。
- 不在 U3 生成合同完整上游事实链；U4 负责量房/设计/报价/合同事实节点。
- 不在 U3 生成项目 PHASE 全链；U5 负责事实索引。

### U3-F · PDF 参数入口

- App/Web `document/api` 请求增加 `version + groupMode`。
- 后端 `document/quote/build` 接收并校验参数；`QuotePdfRenderer` 的分组渲染和归档属于 U6，不得在 U3 自测中冒充 PDF 验收通过。
- 默认 REGION；客户版字段继续执行 U1 白名单。

## 五、退出条件

1. App/Web/业主/项目/合同入口均从服务端投影读取 `regionGroups/typeGroups/groupMode/source*`。
2. 成功响应不执行前端业务合并、金额重算或本地空间 fallback。
3. 同一 `quoteId + version` 的销售行、金额、版本、状态一致。
4. 业主字段白名单通过；PM/设计师成本边界不扩大。
5. PDF 入口参数贯通；真实 PDF 渲染和归档由 U6 验收。
6. App type-check/build、Web type-check/build、定向契约/API 测试通过。
7. 真实页面和越权负向证据齐全；没有真实证据不得声称 UI 通过。

## 六、回滚与风险

- 回滚：前端恢复旧读取路径；后端 U2 投影保留，不删除兼容分类组。
- 共享工作区已有财务/费用/套餐等并行改动，U3 必须在独立快照或明确文件边界内编码，禁止把并行改动混入 U3。
- `GAP-053` 的非空区域真实数据仍未覆盖；U3 不能把“综合项目”的兼容展示当成区域数据已验收。
- `GAP-065` 继续保持打开，直到 U3-U6 的真实页面、权限、金额和归档证据完整。

## 七、当前门禁

```text
U1：PASS
U2：PASS
U3：ACCEPTED
U4：ACCEPTED（后续最终独立复验）
U5：DEV_IN_PROGRESS（U5-A/U5-B1/U5-B2/U5-C DEV_VERIFIED；U5-D~F 未启动）
U6：未启动
GAP-065：OPEN
```
