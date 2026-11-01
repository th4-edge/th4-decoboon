# Web 财务选择器与项目筛选收口（开发自测）

> 状态：`DEV_SELF_CHECK`，不是独立验收结论。
> 变更：`CHG-20261101-017`
> 业务依据：`DEC-021` / `BR-019`、`DEC-023` / `BR-021`、`DEC-024` / `BR-022`
> 日期：2026-11-01

## 1. 范围

- 员工薪酬发放的“待发应付”从 `el-select` 升级为 `EmployeePayablePicker`。
- 客户退款“原收款”从 `el-select` 升级为 `ReceiptPicker`。
- `ProjectPicker` 的设计师/项目经理筛选改为员工专用选择器，不再全量预取员工或通过 200 条项目兜底。
- 选择器静态门禁扩展到 `receiptId/payableId/selectedId` 和两个财务抽屉。
- 不修改薪酬/退款金额公式、审批、权限规则和业务状态机。

## 2. 结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 有效收款服务端分页 | PASS，3 条真实数据 | `evidence/api-pickers.json` |
| 已冲正收款筛选 | PASS，0 条 | `evidence/api-pickers.json` |
| 员工待发应付真实空态 | PASS | `evidence/api-pickers.json` |
| 项目按设计师筛选 | PASS，4→3 条 | `evidence/api-pickers.json`、网络请求 |
| 原收款选择与金额回填 | PASS | 截图、`evidence/web-render.json` |
| 员工应付有数据时组件展示 | PASS（路由夹具，仅 UI） | 截图，`syntheticFixtures` |
| ProjectPicker 嵌套员工筛选 | PASS | 截图、网络请求 |
| 1440/1280/1024 布局 | PASS，无横向溢出 | `evidence/web-render.json` |
| Console | 0 error | `evidence/web-render.json` |
| 图像视觉评审 | `VISUAL_REVIEW_UNAVAILABLE` | 当前模型无图像输入能力 |

## 3. 截图

- `receipt-picker-selected-1440.png`
- `receipt-picker-selected-1024.png`
- `employee-payable-selected-1440.png`
- `employee-payable-selected-1024.png`
- `project-picker-employee-filter-1440.png`
- `project-picker-employee-filter-1280.png`
- `project-picker-employee-filter-1024-final.png`

## 4. 边界与未覆盖

- 员工待发应付当前租户无真实数据；有数据卡片使用 Playwright 路由夹具验证，不作为业务事实。
- 未执行退款提交，避免生成真实资金台账和提成负向调整。
- 发现一个既有高风险待裁决点：`ReceiptRefundContext.create` 未显式校验原收款是否已冲正；本次选择器只允许有效收款，未擅自修改退款业务规则，建议业务裁决后补服务端门禁。
- 未独立验收、未发布。
