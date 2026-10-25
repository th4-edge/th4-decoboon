# 统一对象视图一期 U3 · 修复后开发验证证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 切片 | `U3` 报价跨端接线修复 |
| 日期 | 2026-10-23 |
| 状态 | `DEV_VERIFIED / REMEDIATED_PENDING_REACCEPTANCE` |
| 独立验收前结论 | **不是 PASS**；U4 仍 BLOCKED |
| 基线独立验收 | `docs/acceptance/runs/20261023-unified-object-view-u3-independent/README.md` |
| 隔离验证目录 | `/tmp/u3-fix.tegh6A` |

## 一、修复问题

### P1-01 · 金额缺失不得补 0 或重算

- App `QuoteItemRow.vue`：`amount` 缺失时不再使用 `price × quantity`，显示“暂不可确认”。
- App 业主报价：总额、小计、税金缺失时显示“暂不可确认”，不再 `Number(... || 0)`。
- 后端 `CustomerJourneyContext.money(null)` 返回 `null`；字符串场景使用 `moneyText(null) = 暂不可确认`。
- 不改变报价头、合同金额或底层金额公式。

### P1-02 · 分组缺失不得伪装为空数据

- App `JourneyItem.hasSourceLines` 标记服务端是否读到原始报价行。
- App 业主报价在“有原始行但当前分组为空”时显示“报价分组暂不可确认”，不再显示“清单待完善”。
- Web `toSheetGroups` 在存在原始 `items` 但服务端未返回 REGION/TYPE 分组时返回 `null`。
- Web `QuoteSheetGrid` 显示“报价分组暂不可确认 / 重新读取”，不再显示“还没有报价分项”。

### P2-01 · 合同读取失败不得伪装为待发起

- Web `DesignerContractHandoffView.vue`：合同读取失败/读取中时，队列行、Hero、阶段轨道、合同金额和签约进度统一显示“暂不可确认/读取中”。
- `toCreate` 筛选不再把读取失败项计入“待发起合同”。

## 二、变更文件

### 后端

- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerJourney.java`
- `src/main/java/com/th4/edge/decoboon/customer/context/CustomerJourneyContext.java`
- `src/main/java/com/th4/edge/decoboon/customer/service/project/journey/info/support/Info.java`
- `src/test/java/com/th4/edge/decoboon/customer/CustomerJourneyChangeSectionTest.java`
- `src/test/java/com/th4/edge/decoboon/customer/CustomerJourneyEnrichmentTest.java`

### App

- `src/subpackages/quote/components/QuoteItemRow/QuoteItemRow.vue`
- `src/subpackages/project/pages/owner-quote.vue`
- `src/components/owner/OwnerPriceGroups.vue`
- `src/components/JzMoney.vue`
- `src/domains/journey/model.ts`
- `src/domains/journey/api.ts`

### Web

- `src/domains/trade/model.ts`
- `src/domains/trade/api.ts`
- `src/domains/trade/quote-sheet.ts`
- `src/views/designer/DesignerQuoteWorkspaceView.vue`
- `src/components/QuoteSheetGrid.vue`
- `src/views/designer/DesignerContractHandoffView.vue`

## 三、验证结果

| 检查 | 结果 |
| --- | --- |
| 后端 7 个定向测试类 | **18/18 PASS**，0 failures / 0 errors / 0 skipped |
| JDK 22 后端 package | PASS |
| App `npm run type-check` | PASS |
| App `npm run build:mp-weixin` | PASS |
| Web `npm run type-check` | PASS |
| Web `npm run build` | PASS（含 client-copy 和 Vite build） |
| 三端 `git diff --check` | PASS |

后端覆盖包含：

- 报价金额缺失保持 `null` 和“暂不可确认”；
- `hasSourceLines` 正确表达原始行是否存在；
- 业主 journey 报价段返回统一 REGION/TYPE 投影；
- 既有变更、OwnerPreview、PDF、QuoteView 契约回归。

## 四、未覆盖

- 未执行真实 App/Web/业主页面和真机截图；
- 未执行真实 Provider/HTTP 与跨企业/门店/非参与项目负向；
- 未执行 PDF 真实归档回读；
- 未执行独立复验。

## 五、门禁

```text
U1：PASS
U2：PASS
U3：DEV_VERIFIED / REMEDIATED_PENDING_REACCEPTANCE
U4/U5/U6：BLOCKED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
```

只有独立复验 PASS 后才能将 U3 标记 ACCEPTED，并解除 U4 阻塞。
