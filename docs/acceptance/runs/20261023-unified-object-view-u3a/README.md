# 统一对象视图一期 U3-A · App/Web 报价契约适配开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 批次 | `U3-A` 报价契约适配（U3 子切片） |
| 日期 | 2026-10-23 |
| 结论 | `DEV_VERIFIED`（仅 U3-A；U3 整体未完成、未验收） |
| 前置 | U1/U2 独立验收 PASS |
| 数据变更 | 无 DDL、无迁移、无权限/金额/状态机变更 |

## 一、范围

只改 App/Web 报价模型与归一化层，使前端能够消费 U2 已验收的服务端字段：

- `typeGroups`
- `groupMode`
- `typeRowId`
- `sourceLineIds/sourceRegionIds/sourceRegionNames/sourceCount`
- 金额缺失保持未知，不归一化为 0
- TYPE 合并行展示键使用稳定 `typeRowId`，原始编辑/删除仍保留 `itemId`/`sourceLineIds`

本切片不切换页面业务视图、不删除 App 详情本地分类、不接业主 journey、不改项目/合同事实链、不改 PDF 渲染。

## 二、变更文件

### App

- `src/domains/trade/model.ts`
- `src/domains/trade/api.ts`

### Web

- `src/domains/trade/model.ts`
- `src/domains/trade/api.ts`
- `src/domains/trade/quote-sheet.ts`
- `src/views/designer/DesignerQuoteWorkspaceView.vue`

## 三、实现结果

1. App/Web `Quote` 均增加 `typeGroups`，`QuoteGroup` 增加 `groupMode/type/category/unit`。
2. `QuoteItem` 增加 `typeRowId/sourceLineIds/sourceRegionIds/sourceRegionNames/sourceCount`。
3. App/Web 报价行增加原始 `itemId`，展示键优先使用 `typeRowId`；TYPE 合并行不把稳定展示 ID 冒充原始 itemId。
4. Web `quote-sheet` 的 `toSheetGroups(quote, mode)` 读取服务端 `regionGroups/typeGroups`，无数据时返回空数组，不再按 `space.name` 前端回退。
5. Web 金额映射使用 `item.amount ?? null`；App/Web 金额缺失均不补 0。
6. Web 版本差异比较对缺失金额显示不可确认，不进行 `undefined/null` 算术。
7. `groupMode` 归一化支持枚举对象/字符串两种服务端序列化形态。

## 四、验证命令与结果

App：

```text
npm run type-check
结果：PASS

npm run build:mp-weixin
结果：PASS，生成 dist/build/mp-weixin
```

Web：

```text
npm run type-check
结果：PASS

npm run build
结果：PASS（含 type-check、check:client-copy、vite build）
```

Diff：

```text
App U3-A git diff --check
PASS

Web U3-A git diff --check
PASS
```

构建中仅有既有 Sass `@import/legacy-js-api` deprecation warning，不属于 U3-A 失败。

## 五、未完成与风险

- App `subpackages/quote/pages/detail.vue` 仍使用本地空间 fallback 和本地 `categoryView` 合并；属于 U3-B。
- 业主报价仍消费 `CustomerJourney.PriceGroup`，属于 U3-D。
- 项目/合同入口和 PDF 参数接线未完成，属于 U3-E/U3-F。
- 未执行真实 App/Web 页面、Provider JSON 和跨企业权限负向；U3-A 不能作为 UI 或独立验收证据。
- 共享工作区有财务/费用/套餐并行改动，U3-A 证据只对应上述文件。

## 六、门禁

```text
U3-A：DEV_VERIFIED
U3 整体：DEV_IN_PROGRESS
U3 独立验收：未执行
U4/U5/U6：未启动
```
