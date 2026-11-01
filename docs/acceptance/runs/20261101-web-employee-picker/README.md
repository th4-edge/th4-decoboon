# Web 员工选择器统一与分页升级（开发自测）

> 状态：`DEV_SELF_CHECK / WEB_RUNTIME_VERIFIED / PHYSICAL_DEVICE_NOT_RUN`，不是独立验收结论。
> 最终复核：`docs/acceptance/runs/20261101-selector-p1-final/`
> 变更：`CHG-20261101-016`
> 业务依据：`DEC-021` / `BR-019`、`DEC-023` / `BR-021`、`T15` 薪酬员工选择器既有评审
> 日期：2026-11-01

## 1. 范围

- `EmployeePicker` 改为服务端搜索、筛选和数据库分页，默认 20 条，接口硬上限 50 条。
- 已选员工通过 `empId` 从当前权限范围回读，不依赖列表第一页。
- Web 负责人、设计师、项目经理、支持人、任务负责人和设计运营筛选全部接入专用员工选择器。
- 员工选择器门禁增加语义识别和负向自测。
- 不修改 App 员工选择器、不修改员工权限角色矩阵、不修改薪酬金额口径。

## 2. 结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| Web 构建 | PASS | `npm run build` |
| Web 类型检查 | PASS | `npm run type-check` |
| 选择器静态门禁 | PASS | `npm run check:forms` |
| 选择器负向自测 | PASS | `npm run check:forms:self-test` |
| 业务概念/权限门禁 | PASS | `check:concepts`、`check:permissions` |
| 动作注册门禁 | BLOCKED（既有退款/员工付款动作未登记，与员工选择器无关） | `check:actions` 输出 |
| 角色动作 Provider 烟测 | 32/32 PASS | `action-provider-smoke.json` |
| 服务端多角色分页 | PASS | `evidence/api-pagination.json` |
| 已选员工按 ID 回读 | PASS | `evidence/api-pagination.json` |
| 门店/角色权限负向 | PASS | `evidence/api-pagination.json` |
| 真实 Web 选择、搜索、空态、清除与焦点归还 | PASS | `evidence/web-render.json` |
| 1440/1280/1024 横向溢出 | PASS | `evidence/web-render.json` + screenshots |
| 图像视觉评审 | `VISUAL_REVIEW_UNAVAILABLE` | 截图已留存，但当前模型无图像输入能力 |

## 3. 截图

- `evidence/screenshots/employee-picker-compact-dialog-1440.png`
- `evidence/screenshots/employee-picker-compact-selected-1440.png`
- `evidence/screenshots/employee-picker-full-dialog-1440.png`
- `evidence/screenshots/employee-picker-full-dialog-1280.png`
- `evidence/screenshots/employee-picker-full-dialog-1024.png`
- `evidence/screenshots/employee-picker-full-selected-1024.png`

## 4. 未覆盖

- 未构造 200+ 员工大数据租户；已通过 9 人真实数据、`size=2` 服务端翻页和角色动作烟测验证分页契约。10× 数据量专项仍待补。
- 未做独立验收；开发自测不能替代独立复验。
- 未发布、未部署。
