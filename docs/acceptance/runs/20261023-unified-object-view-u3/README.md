# 统一对象视图一期 U3 · 场景接线开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 批次 | `U3` 报价场景接线 |
| 日期 | 2026-10-23 |
| 结论 | `DEV_VERIFIED`，等待独立验收 |
| 前置 | U1、U2 独立验收 PASS |
| 业务裁决 | `DEC-086`、`BR-052`、`T12-BIZ-20261022-007` |
| 数据变更 | 无 DDL、无迁移 |
| 验收边界 | 未执行真实登录页面/真机截图；真实 UI 属 U6 |

## 一、U3 实现结果

### App

- 报价详情删除客户端 TYPE 合并、数量/金额累计和区域回退。
- 区域/类型直接消费服务端 `regionGroups/typeGroups`。
- TYPE 行展示 `sourceCount/sourceRegionNames/typeRowId`。
- 分组缺失且原始行存在时显示“报价分组暂不可确认”。
- PDF 生成前通过统一 ActionSheet 选择 REGION/TYPE，并携带 `quoteId + version + groupMode`。
- 业主报价页支持 REGION/TYPE 切换，消费服务端 `QuoteGroup`。
- 业主报价不展示 `acceptedBy`。

### Web

- `quote-sheet.ts` 删除 regionGroups 缺失时的前端区域回退。
- 设计师报价工作区增加“按区域 / 按类型”切换。
- TYPE 模式只读，编辑、批量改价、删除继续只作用于原始 REGION 行。
- TYPE 行展示来源区域和合并行数。
- PDF 使用当前 REGION/TYPE 和当前报价版本生成。
- 合同交接、报价列表 PDF 默认 REGION，并显式携带报价版本。

### 后端

- `CustomerJourneyContext` 报价项改用 `QuoteViewContext.applyForPrint`，统一生成区域/类型投影。
- 业主旅程 Provider 白名单增加 `regionGroups/typeGroups/QuoteGroup/QuoteLine`，仅含销售字段。
- `document/quote/build` 强制接收并校验 `quoteId + version + groupMode`。
- `QuotePdfRenderer` 直接消费统一 `QuoteGroup`，支持 REGION/TYPE，页眉展示分组方式。
- PDF 文件名包含版本和分组，例如 `..._V2_按区域.pdf`。
- 客户版打印投影使用 `applyForPrint`，不读取成本字段；旧归档不覆盖。
- 合同/项目宿主仍跳统一报价视图，不嵌入第二套报价明细。

## 二、验证命令与结果

### 后端

隔离快照：

```text
/tmp/unified-object-u1-recheck.nGf3ac
```

```text
mvn -o -q -Dtest=QuotePdfRendererTest,DocumentArchiveContextTest,CustomerJourneyChangeSectionTest,CustomerJourneyOwnerPreviewModeTest,QuoteViewContextTest,QuoteUnifiedViewPropertiesTest test
mvn -o -q -DskipTests compile
```

| 测试 | 结果 |
| --- | --- |
| QuotePdfRendererTest | 3/3 PASS |
| DocumentArchiveContextTest | 2/2 PASS |
| CustomerJourneyChangeSectionTest | 1/1 PASS |
| CustomerJourneyOwnerPreviewModeTest | 1/1 PASS |
| QuoteViewContextTest | 7/7 PASS |
| QuoteUnifiedViewPropertiesTest | 2/2 PASS |
| 合计 | **16/16 PASS** |
| 编译 | PASS |
| `git diff --check` | PASS |

### App

```text
npm run type-check                         PASS
npm run check:document                     PASS
npm run build:mp-weixin                    PASS
npm run check:ui                           FAIL：仅 src/App.vue 既有 uni.showModal，非 U3 文件
npm run check:owner-preview                FAIL：并行 TeamLeader 冻结与旧脚本要求冲突
```

### Web

```text
npm run type-check                         PASS
npm run check:client-copy                  PASS
npm run check:actions                      PASS
npm run build                              PASS
```

## 三、覆盖场景

- 同一报价版本在 App/Web/业主端消费同一服务端分组。
- App/Web/业主报价支持 REGION/TYPE。
- TYPE 合并行不参与编辑，来源行/区域可追溯。
- 前端不再按名称、价格或空间名兜底分组。
- 金额、成本缺失仍以“暂不可确认”表达。
- PDF REGION/TYPE 均可生成，版本和分组进入页眉与文件名。
- 客户版 PDF 不读取成本字段。
- 合同/项目报价入口只做摘要和统一视图跳转。

## 四、残留风险

- 未执行真实账号、真实报价和生产 API 的三端页面截图；U3 不冒充 U6 验收。
- `GAP-053` 的区域真实非空数据覆盖仍未完成。
- App `check:ui` 被并行 `App.vue` 既有违规阻断；U3 详情页自身已改为统一 ActionSheet。
- App `check:owner-preview` 被 TeamLeader 冻结与旧脚本冲突阻断，需由角色专项收口。
- Web TYPE 视图为只读检查视图；类型合并行的正式编辑和引用仍回到原始 `itemId`。
- 共享工作区存在大量并行改动，U3 结论只对应本文件列出的 U3-owned 变更。
