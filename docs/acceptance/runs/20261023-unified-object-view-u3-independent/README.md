# 统一对象视图一期 U3 · 报价场景接线独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U3` App/Web/业主/项目/合同/PDF 报价场景接线（U3-A~F） |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 前置 | U1、U2 独立验收 PASS |
| 业务事实 | `ANS-BIZ-20261022-007`、`T12-BIZ-20261022-007`、`DEC-086`、`BR-052` |
| 最终结论 | **FAIL（仅 U3）** |
| 问题状态 | `DEV_IN_PROGRESS`（不得 `CLOSED`） |
| 当前切片状态 | `FAILED`；U3-A~F 仍只能视为 `DEV_VERIFIED` |
| U4 门禁 | **BLOCKED** |

## 一、结论

U3 的分组来源、TYPE 只读、角色字段裁剪、绑定报价版本和 PDF 参数贯通大部分符合冻结口径，但本轮独立反向检查发现 2 个 P1 阻断项和 1 个 P2 异常恢复问题，不能判定 PASS：

1. 金额缺失被补 0 或被前端重新用 `price × quantity` 计算，跨端污染“暂不可确认”事实；
2. Web/业主在服务端分组缺失时把失败伪装成“没有报价分项/清单待完善”的空态；
3. 合同队列在合同读取失败时仍局部显示“待发起合同”，失败态与真实无合同未完全分离。

因此：

```text
U1：PASS
U2：PASS
U3：FAIL / U4 BLOCKED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
U4/U5/U6：不得启动
```

本结论不宣称真实 UI、真机、跨企业/门店负向或 PDF 旧归档回读通过；这些仍属 U6，但 U6 不能越过本次 U3 修复和重验。

## 二、快照与边界

基线提交：

```text
backend: 7cf83fa2b526156640ea8c227eb348ab81ca27d1
app:     aa387a86c198e413e4a340b7c887ba961e0169f0
web:     0cbdc06dd485fc9464622d40eabc495d1d87fea2
```

U3-owned 31 文件清单（含实际展示路径 `QuoteItemRow.vue`）的 SHA-256 清单摘要：

```text
78f6dedcc6bd5bdeeea072e7657b8c8accf7a3676040d476a0992aaf81cb712d
```

### 2.1 共享工作树隔离快照

```text
/tmp/u3-independent.tUtcJf/backend
/tmp/u3-independent.tUtcJf/app
/tmp/u3-independent.tUtcJf/web
```

- 从当前三仓工作树复制，排除 `target/`、`node_modules/`、`dist/` 等构建产物后在快照内独立安装依赖/构建。
- 该快照用于观察当前工作树整体可构建性，发现并行改动基线阻塞：

```text
App type-check: FAIL
- src/subpackages/plan/pages/construction-list.vue: Cannot find name 'PaymentPlanTemplate'
- src/subpackages/plan/pages/payment-list.vue: Cannot find name 'ConstructionPlanTemplate'

Backend testCompile/package: FAIL
- CompanyPlanTemplateContextTest 仍使用旧 6 参数构造器，生产构造器已为 8 参数
```

上述失败不归入 U3-owned 问题，但证明共享工作树当前不能作为 U3 PASS 证据。

### 2.2 U3 scoped 快照

```text
/tmp/u3-scoped.ZSySvB/backend
/tmp/u3-scoped.ZSySvB/app
/tmp/u3-scoped.ZSySvB/web
```

- Backend：从 U2 独立验收快照 `/tmp/uov-v2-independent.82211/repo` 出发，仅覆盖 U3 后端文件及 `QuoteViewContext.applyForPrint` 依赖。
- App/Web：从各自 `HEAD` 建立，仅覆盖 U3-owned 文件和依赖，不引入共享工作树的财务、套餐、计划等并行改动。
- 快照内不使用共享 `target/`；App/Web 构建产物只写入 scoped 快照。

## 三、命令与结果

### 3.1 Scoped 后端

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q -Dtest=QuotePdfRendererTest,DocumentArchiveContextTest,CustomerJourneyChangeSectionTest,CustomerJourneyOwnerPreviewModeTest,QuoteViewContextTest,QuoteUnifiedViewPropertiesTest test
```

结果：**16/16 PASS，0 failures / 0 errors / 0 skipped**。

| 测试类 | 结果 |
| --- | --- |
| `QuotePdfRendererTest` | 3/3 |
| `DocumentArchiveContextTest` | 2/2 |
| `CustomerJourneyChangeSectionTest` | 1/1 |
| `CustomerJourneyOwnerPreviewModeTest` | 1/1 |
| `QuoteViewContextTest` | 7/7 |
| `QuoteUnifiedViewPropertiesTest` | 2/2 |

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q -Dmaven.test.skip=true package
```

结果：**PASS**，生成 `target/th4-decoboon-1.0.0.jar`，7.6 MB。

### 3.2 Scoped App

```bash
npm run type-check
npm run build:mp-weixin
```

结果：**均 PASS**。构建仅有既有 Sass `@import/legacy-js-api` deprecation warning。

### 3.3 Scoped Web

```bash
npm run type-check
npm run build
```

结果：**均 PASS**；`build` 内含 type-check、`check:client-copy` 和 Vite 生产构建。

### 3.4 Diff / JSON / 领域账本

```text
U3 backend git diff --check：PASS
U3 App git diff --check：PASS
U3 Web git diff --check：PASS
registry.json / state.json / blueprint JSON parse：PASS
registry issue ID duplicate：NONE
PI-20261022-010 status=DEV_IN_PROGRESS：PASS
domain_state.py validate --domain product-system-baseline：PASS
```

## 四、独立反向探针

### 4.1 金额缺失

Java 反射探针：

```text
OWNER_MONEY_NULL=0.00
PDF_AMOUNT_NULL=0.00
```

证据：

- `CustomerJourneyContext.money(null)` 返回 `0.00`，且 `quoteItem` 无条件写入 `amount` fact；
- `QuotePdfRenderer.amount(null)` 返回 `0.00`；
- App `QuoteItemRow.vue:53`：

```ts
props.displayAmount ?? (
  Number(props.item.amount || 0)
  || Number(props.item.price || 0) * Number(props.item.quantity || 0)
)
```

- App `owner-quote.vue:67/97/105/109/125` 多处使用 `Number(... || 0)`。

结论：与 `DEC-086 §1.3/§2.4`、`BR-052 §1.6` 的“金额缺失保持暂不可确认，不得补 0 或按单价重算”冲突。

### 4.2 分组缺失空态

Web 编译后直接调用 `toSheetGroups`：

```text
MISSING_GROUPS_WITH_RAW_ITEMS=[]
MERGED_ROW_AMOUNT_NULL=true
MERGED_ROW_KEY=type-1
```

- 原始 `items` 存在但 `regionGroups=[]` 时，`toSheetGroups` 返回 `[]`；
- `QuoteSheetGrid.vue:280-285` 随后显示“还没有报价分项”，不是“暂不可确认”；
- 业主 `owner-quote.vue:28-33` 在选中分组缺失时同样得到空数组，`OwnerPriceGroups.vue:116` 显示“清单待完善”。

结论：与 `DEC-086 §1.3`、`T12-BIZ-20261022-007`“读取失败不得伪装无数据”冲突。

### 4.3 角色字段裁剪

```text
CUSTOMER_QUOTE_LINE_FORBIDDEN=[]
OWNER_GROUP_COST_FIELD=false
PM_LINE_INTERNAL_FIELDS=[]
PM_GROUP_COST_FIELD=false
```

- Owner `@Properties` 的 `QuoteLine` 不含 `cost/formula/quantityFormula/supplierPrice/purchasePrice/acceptedBy`；
- Owner `QuoteGroup` 不含 `cost`；
- PM `QuoteLine/QuoteGroup` 不含内部成本与公式字段。

该项 **PASS**。

## 五、P0 / P1 / P2

### P1-01 · 金额缺失被补 0 或重新计算

**证据**

- `App/src/subpackages/quote/components/QuoteItemRow/QuoteItemRow.vue:53`
- `App/src/subpackages/project/pages/owner-quote.vue:67,97,105,109,125`
- `backend/src/main/java/com/th4/edge/decoboon/customer/context/CustomerJourneyContext.java:887-888`
- 独立探针：`OWNER_MONEY_NULL=0.00`

**影响**

- 员工报价行可能把缺失金额展示成单价乘数量；
- 业主报价汇总/总额可能显示 `0`；
- 服务端 U2 已保证的 `amount=null` 在 U3 被重新解释为已知金额。

**退回条件**

- 展示层仅在服务端金额存在时渲染数值；否则显示“暂不可确认”；
- 删除员工报价行、业主页、journey facts 的 `|| 0` 和 `price × quantity` 金额兜底；
- 增加缺失金额的 App 员工详情、业主 journey/页面回归；
- 后端不把 null 金额格式化为 `0.00`。

### P1-02 · 分组缺失伪装为空数据

**证据**

- `Web/src/domains/trade/quote-sheet.ts:136-145`
- `Web/src/components/QuoteSheetGrid.vue:280-285`
- `App/src/subpackages/project/pages/owner-quote.vue:28-33`
- `App/src/components/owner/OwnerPriceGroups.vue:116`
- 独立探针：`MISSING_GROUPS_WITH_RAW_ITEMS=[]`

**影响**

- 服务端成功响应但投影字段缺失时，Web 显示“还没有报价分项”；
- 业主端显示“清单待完善”，掩盖读取/投影失败，用户无法判断需要重试。

**退回条件**

- 当原始报价存在而当前 REGION/TYPE 投影为空时，显示“报价分组暂不可确认”并提供恢复路径；
- Web 和业主页均增加该判定，不得把缺失投影降级为正常空态；
- 增加有原始行、无 regionGroups/typeGroups 的回归测试。

### P2-01 · 合同读取失败仍局部显示“待发起合同”

**证据**

- `Web/src/views/designer/DesignerContractHandoffView.vue:709-710`
- 同文件 `:733,739`，以及 `mainAction` 中虽已禁用动作，但展示标签仍按“无合同”渲染。

**影响**

- 合同查询失败时，页脚显示“合同状态暂不可确认”，但队列和 Hero 仍显示“待发起合同/合同待发起”，失败态与真实无合同并存。

**退回条件**

- `contractError=true` 时行状态、Hero 状态、摘要统一显示“合同状态暂不可确认”；
- 不得展示“待发起合同/未发起合同”；
- 增加合同读取失败恢复回归。

## 六、U3 范围逐项判定

| 范围 | 结论 | 说明 |
| --- | --- | --- |
| 1. App 员工报价详情消费 `regionGroups/typeGroups/groupMode/source*` | **FAIL** | 分组消费、无本地 merge/space fallback 为 PASS；金额缺失重算为 P1 |
| 2. Web REGION/TYPE、失败不伪空、编辑落原始 `itemId` | **FAIL** | TYPE 只读和 REGION 编辑 `itemId` PASS；分组缺失伪空为 P1 |
| 3. 业主 `CustomerJourneyContext.applyForPrint`、只销售字段 | **FAIL** | `applyForPrint` 与白名单 PASS；金额 null→0、分组缺失伪空失败 |
| 4. 项目/合同绑定摘要、不取最新报价 | **PASS（静态）** | App 合同入口使用 `contract.quoteId`；Web 合同交接使用绑定 `item.quote.id/version`；无“取最新报价”替换代码。真实权限证据留 U6 |
| 5. PDF 参数贯通 `quoteId + version + groupMode`、默认 REGION | **PASS（入口）** | App/Web 调用与后端 `Build/DocumentArchiveContext` 校验贯通；真实渲染/归档回读未在 U3 冒充 |
| 6. App/Web 同事实/分组/金额/版本/裁剪/异常恢复 | **FAIL** | 同源分组、版本和字段裁剪大体一致；金额缺失与异常空态不满足 |

## 七、逐项对照 `DEC-086` / `BR-052`

| 规则 | 结论 | 证据 |
| --- | --- | --- |
| 同一 `quoteId + version` 使用同一原始行和 REGION/TYPE 投影 | PASS（结构） | U2 与 U3 代码路径均调用统一 `QuoteViewContext`；U3 定向测试 16/16 |
| 前端不得自行分组、合并、补造区域 | PASS（目标路径） | `mergeKey/categoryView` 在 App 详情/Web 工作区目标路径缺失；`quote-sheet` 仅映射服务端组 |
| 前端不得重算金额或缺省补 0 | **FAIL** | P1-01；`QuoteItemRow` 与业主汇总存在 0/乘积兜底 |
| TYPE 行只展示，编辑/合同/项目引用回原始身份 | PASS（静态） | Web `gridEditable = editable && viewMode === REGION`；REGION 保存使用原始 `itemId`；无 TYPE 行写入路径 |
| 业主不可见成本/内部公式/供应商价/`acceptedBy` | PASS（静态/探针） | Owner 白名单独立探针为空；`acceptedBy` fact 已移除 |
| 失败、缺数据显示暂不可确认，不得空数组伪装 | **FAIL** | P1-02；Web/业主空态证据 |
| 合同/项目绑定实际版本，不取最新 | PASS（静态） | 合同交接绑定 `item.quote.id/version`；App 合同入口使用 `contract.quoteId` |
| PDF 绑定 `quoteId + version + groupMode`，默认 REGION | PASS（入口） | App/Web 调用和后端参数校验通过；旧归档/真实渲染仍未覆盖 |
| 客户版 PDF 不读取成本/内部字段 | PASS（静态） | `QuotePdfRenderer` 调用 `applyForPrint`；客户白名单不含禁用字段 |

## 八、未覆盖项

1. 未执行真实登录后的 App/Web 页面与真机截图；本轮没有把构建成功冒充 UI PASS。
2. 未执行 U3 业主 journey 的真实 Provider HTTP 与跨企业/跨门店/非参与项目负向；U2 的 HTTP 2/2 不能代替 U3。
3. 未执行 PDF 真实渲染、旧归档回读和重新生成不覆盖验证；这些仍属 U6。
4. `GAP-053` 的真实非空区域数据仍未覆盖。
5. 共享工作树当前 App type-check / 后端 testCompile 受并行改动阻塞；scoped pass 不能消除该发布基线风险。

## 九、U6 进入前已知风险

虽然不是 U3 的 PDF 渲染验收项，但静态观察与独立探针发现：

```text
PDF_AMOUNT_NULL=0.00
```

`QuotePdfRenderer` 的行金额和报价总额仍会把 null 格式化 0。U6 做真实 PDF 验收前必须先修复并补缺失金额 PDF 证据；本轮不把该渲染问题计入 U3 FAIL，但不得在 U6 宣称客户版金额口径通过。

## 十、退回与重验门禁

U3 退回研发修复 `P1-01`、`P1-02` 与 `P2-01` 后重新提交独立验收。重验至少包含：

1. 缺失金额在 App 员工行、业主汇总/journey、Web 表格均显示“暂不可确认”；
2. 有原始行但投影缺失时 Web/业主均显示失败态并可重试；
3. 合同查询失败不出现“待发起合同”；
4. scoped 后端 16/16、App/Web type-check 与目标构建、diff/JSON/账本检查；
5. 仍不得启动 U4。

## 十一、最终门禁

```text
U1：PASS
U2：PASS
U3：FAIL / U4 BLOCKED
PI-20261022-010：DEV_IN_PROGRESS（不得 CLOSED）
GAP-065：OPEN
U4/U5/U6：未启动
```
