# 统一对象视图一期 U3 · 修复后第二轮独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U3-A~F` 报价场景跨端接线（修复后复验） |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 前置 | U1、U2 独立验收 PASS |
| 业务事实 | `DEC-086`、`BR-052`、`T12-BIZ-20261022-007` |
| 最终结论 | **PASS / U3 ACCEPTED** |
| 首轮历史 | `docs/acceptance/runs/20261023-unified-object-view-u3-independent/README.md` 保持 FAIL，不覆盖 |
| 问题状态 | `DEV_IN_PROGRESS`；U4 仅允许重新对账后准备 |
| U5 门禁 | **BLOCKED** |

## 一、结论

U3 首轮三个阻断项已全部关闭：

1. 缺失金额在 App 员工行、业主汇总/journey、Web 报价表格中保持缺失，不再补 0 或按 `price × quantity` 重算。
2. 有原始行但服务端当前分组为空时，Web 与业主端均显示“报价分组暂不可确认”并提供重新读取路径，不再渲染正常空态。
3. 合同读取失败/读取中不再进入“待发起合同”，失败态、读取态与真实无合同已分离。

TYPE 只读、REGION 编辑回原始 `itemId`、PDF `quoteId + version + groupMode` 参数和客户版字段边界均未回归。因此本轮独立验收 **PASS**，仅代表 U3；不等于整个一期完成，也不代表 U4/U5/U6 已验收。

## 二、隔离快照与边界

主复验快照：

```text
/tmp/u3-reaccept.VBA8s3/backend
/tmp/u3-reaccept.VBA8s3/app
/tmp/u3-reaccept.VBA8s3/web
```

- 从研发修复快照 `/tmp/u3-fix.tegh6A` 重新复制，排除 `target/dist` 后在快照内重新构建。
- App/Web 依赖使用只读 `node_modules` 软链接；业务源码和构建输出都在复验快照内。
- U3 remediation 17 文件清单 SHA-256 摘要：`eaf5f3cd9b1258e9a171d6d66c49e2b49dfcb4caeaf0ea2babd1abb7a68737da`。

### 2.1 U3-only 边界复核

研发修复快照中的 `DesignerContractHandoffView.vue` 同时带有并行 U4 `contractFactTrace` 代码。为避免把 U4 依赖算作 U3 证据，另建立：

```text
/tmp/u3-reaccept-clean.N8DqcI/{backend,app,web}
```

- Backend/App 使用首轮 U3 scoped 快照 + remediation 文件。
- Web 对合同页只重放 P2-01 的 7 个 hunk，不含 U4 事实链导入和模板。
- P2-only 合同页 SHA-256：`46e186ff0ed5d1b521270ac0b433a79784630dc3aca3a7226d270ac7e6d22240`。
- U3-only 复核：后端 18/18 PASS、package PASS；App type-check/check:document/build PASS；Web type-check/client-copy/actions/build PASS。
- 结论：P2-01 修复本身不依赖 U4 代码；完整修复快照构建通过，但 U4 内容不进入 U3 验收结论。

## 三、命令与结果

### 3.1 后端

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q -Dtest=CustomerJourneyChangeSectionTest,CustomerJourneyEnrichmentTest,CustomerJourneyOwnerPreviewModeTest,DocumentArchiveContextTest,QuotePdfRendererTest,QuoteViewContextTest,QuoteUnifiedViewPropertiesTest test

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q -DskipTests package
```

结果：

- 7 个测试类：**18/18 PASS**，0 failures / 0 errors / 0 skipped。
- JDK 22 package：PASS，生成 `target/th4-decoboon-1.0.0.jar`（约 7.5 MB）。
- `CustomerJourneyChangeSectionTest` 2/2；`CustomerJourneyEnrichmentTest` 1/1；`CustomerJourneyOwnerPreviewModeTest` 1/1。
- `DocumentArchiveContextTest` 2/2；`QuotePdfRendererTest` 3/3；`QuoteViewContextTest` 7/7；`QuoteUnifiedViewPropertiesTest` 2/2。

### 3.2 App

```text
npm run type-check          PASS
npm run check:document      PASS
npm run build:mp-weixin     PASS
```

### 3.3 Web

```text
npm run type-check          PASS
npm run check:client-copy   PASS
npm run check:actions       PASS
npm run build               PASS
```

Sass deprecation warning为既有告警，不是 U3 失败。

### 3.4 静态探针

```text
JOURNEY_MONEY_NULL=null
JOURNEY_MONEY_TEXT_NULL=暂不可确认
REGION_PROJECTION_MISSING=true
TYPE_PROJECTION_MISSING=true
TRUE_EMPTY_RETURNS_ARRAY=true
QUEUE_STATUS_ORDER=PASS
NO_TO_CREATE_ERROR_COUNT=PASS
HERO_AMOUNT_ERROR_GUARD=PASS
HERO_SIGN_PROGRESS_ERROR_GUARD=PASS
STAGE_ERROR_GUARD=PASS
TYPE_READONLY_REGION_ITEMID=PASS
PDF_PARAMS_AND_CUSTOMER_BOUNDARY=PASS
```

## 四、上轮问题逐项复验

| 上轮问题 | 本轮结果 | 证据 |
| --- | --- | --- |
| `P1-01` 缺失金额补 0/重算 | **CLOSED** | App 行、业主汇总/journey、Web 表格均保持缺失；后端 `money(null)=null`，回归 2/2 覆盖 |
| `P1-02` 分组缺失伪空态 | **CLOSED** | Web `toSheetGroups` 返回 null；Web/业主显示暂不可确认并可重试 |
| `P2-01` 合同读取失败伪待发起 | **CLOSED** | 队列、Hero、阶段、金额、签约进度、主行动统一先判错误/读取态；`toCreate` 排除失败项 |

### 4.1 缺失金额

- App `QuoteItemRow.vue`：`effectiveAmount == null` 显示“暂不可确认”，无 `price × quantity`。
- App `owner-quote.vue`：总额、清单小计、税金缺失保持 null 并显示“暂不可确认”。
- App `OwnerPriceGroups.vue`：组小计和行金额缺失均显示“暂不可确认”。
- 后端 `CustomerJourneyContext`：`money(null)=null`，文本场景使用“暂不可确认”；回归测试断言报价金额缺失时 `item.amount=null`。
- Web `QuoteSheetGrid.vue` 通过 `MoneyText` 对 null/undefined 显示“暂不可确认”；行金额来源为 `item.amount ?? null`。

### 4.2 分组缺失

- Web `toSheetGroups`：分组字段 undefined 且原始行存在，或分组为空但原始行存在，均返回 `null`。
- Web `QuoteSheetGrid`：`projectionUnavailable` 分支优先于空表，显示“报价分组暂不可确认 / 重新读取”。
- App 业主 journey：后端返回 `hasSourceLines`；业主页据此区分正常空清单与投影缺失。
- App `OwnerPriceGroups`：`unavailable` 分支优先于普通空态，显示“报价分组暂不可确认，请稍后刷新重试”。

### 4.3 合同读取失败

- `contractError` 在 `itemReason` 中先于“无合同/待发起合同”判断。
- 队列筛选与计数排除读取失败项；真实无合同才进入 `toCreate`。
- 队列、Hero、阶段轨道、合同金额、签约进度和主行动均显示“合同状态暂不可确认/读取中”。
- 错误态不会创建“发起合同”动作。

### 4.4 U3-A~F 回归边界

- TYPE 模式只读；REGION 保存使用原始 `itemId`。
- App/Web PDF 均携带 `quoteId + version + groupMode`；后端 `QuotePrintContract.requireParameters` 强制校验。
- PDF 使用 `QuoteViewContext.applyForPrint` 客户版投影；渲染器未读取成本、供应商价或 `acceptedBy`。
- App `domain/trade` 与 Web `domain/trade` 的 U3 投影契约未受 U4 影响。

## 五、未覆盖与剩余风险

1. 未执行真实登录后的 App/Web/业主页面、真机截图和交互验收；真实 UI 仍归 U6。
2. 未执行真实 Provider HTTP、跨企业/跨门店/非参与项目负向；U3 本轮不冒充 U6。
3. `GAP-053` 的真实非空区域数据仍未覆盖。
4. `QuotePdfRenderer.amount(null)` 仍将空金额格式化为 `0.00`，属于 U6 真实 PDF 金额口径风险，不得据此宣称客户版 PDF 缺失金额已验收。
5. U4 的合同事实链与成本裁剪未进入本轮快照；U4 仍需在 U3 ACCEPTED 后重新对账，不能直接沿用旧 U4 复验结论。

## 六、门禁结论

```text
U1：PASS
U2：PASS
U3：ACCEPTED
PI-20261022-010：DEV_IN_PROGRESS
U4：仅允许重新对账后准备；当前无 ACCEPTED 结论
U5：BLOCKED
U6：未启动
GAP-065：OPEN
```

旧 U3 FAIL 报告保留为历史；本报告是 U3 的最新独立验收事实源。
