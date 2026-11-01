# App 合同报价与供应商选择器 P1（开发自测）

> 状态：`DEV_SELF_CHECK / DEVTOOLS_VERIFIED / PHYSICAL_DEVICE_NOT_RUN`，不是独立验收结论。
> 最终复核：`docs/acceptance/runs/20261101-selector-p1-final/`
> 变更：`CHG-20261101-021`
> 日期：2026-11-01
> 业务依据：`DEC-021` / `BR-019`、`BR-059` / `OBJ-010`、`DEC-095` / `BR-060`

## 1. 范围

- 新增客户已确认报价专用选择页 `quote-picker`。
- 合同创建页移除报价全量数组、`JzSelectField` 和显示文本反查，改为全宽报价业务卡。
- 供应商选择页改为服务端关键词、合作类型、状态和数据库分页。
- 供应商价格页接入 `supplier-picker` 和 `CatalogPicker`，移除供应商数组和 200 条报价项目数组。
- 采购价编辑按 ID 精确读取；供应商价格 Provider 改为数据库分页。
- 报价 Provider 改为数据库分页；供应商应付摘要按当前页批量聚合。
- 不修改合同、供应商价格、成本、权限或状态规则。

## 2. 结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| App 类型检查 | PASS | `npm run type-check` |
| App 分包构建 | PASS | `dist/build/mp-weixin` |
| 选择器契约 | PASS | `npm run check:selectors` |
| 客户端文案/UI/分包 | PASS | 对应 npm 脚本 |
| 列表分页 | PASS | `check:pagination`，基线收缩 1 类 |
| 后端定向测试 | PASS | 3 个 Provider 测试 |
| 后端编译 | PASS | Maven 输出 |
| Provider 响应契约 | PASS，0 findings | 审计命令 |
| 合同报价真实 H5 | PASS（开发自测） | `project-create-selected-quote-*.png` |
| 报价更换回传 | PASS（开发自测） | H5 路径；返回后报价卡仍为选中值 |
| 供应商选择回传 | PASS（开发自测） | `partner-price-supplier-selected-390.png` |
| 报价库项目选择 | PASS（开发自测） | `catalog-picker-open-390.png`、`partner-price-both-selected-*.png` |
| 320/390 布局 | PASS，无横向溢出 | `web-render.json`、8 张截图 |
| 图像视觉评审 | `VISUAL_REVIEW_UNAVAILABLE` | 当前模型无图像输入能力 |

## 3. 接口证据

- `evidence/api-contract.json`
  - 报价请求 `size=200` 实际上限 50。
  - 当前客户/房屋 + 关键词返回 1 份已确认报价。
  - 供应商关键词和服务端分页返回 1 家，含合作状态、账期和批量应付摘要。
  - 报价库项目按 ID 精确返回 1 条。
  - 采购价 Provider 请求 `size=200` 实际上限 50。
- 本批 API 只读，无业务数据写入。

## 4. 截图

- `quote-picker-confirmed-320.png` / `quote-picker-confirmed-390.png`
- `supplier-picker-320.png` / `supplier-picker-390.png`
- `partner-price-supplier-selected-390.png`
- `catalog-picker-open-390.png`
- `partner-price-both-selected-320.png` / `partner-price-both-selected-390.png`
- `project-create-selected-quote-320.png` / `project-create-selected-quote-390.png`

## 5. 未覆盖

- 未提交真实合同或采购价保存，避免改变本地业务数据。
- 当前租户采购价记录为 0，未完成有数据编辑态的真实页面保存。
- 未做 0/21/200+ 全矩阵、10× 数据和微信开发者工具真机复验。
- 未独立验收、未上传体验版、未发布。
