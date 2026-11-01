# App 人员选择器 P1 第一批（开发自测）

> 状态：`DEV_SELF_CHECK / CONTRACT_VERIFIED / PHYSICAL_DEVICE_NOT_RUN`，不是独立验收结论。
> 最终复核：`docs/acceptance/runs/20261101-selector-p1-final/`
> 变更：`CHG-20261101-018`
> 业务依据：`DEC-021` / `BR-019`、`DEC-023` / `BR-021`、`DEC-024` / `BR-022`
> 日期：2026-11-01

## 1. 范围

- 离职交接的接收员工改用 `employee-picker` 普通员工目录模式，排除离职员工本人，仅可选在职员工。
- 施工任务负责人改用 `employee-picker`，限定 Boss/StoreManager/Pm/Designer/Marketer。
- 财务工资类付款的发放工人接入新增 `worker-picker` 专用选择页。
- 付款记录“补录工人”不再在页面内一次加载 100 条，改为专用工人选择页回传。
- 工人档案 Provider 改为数据库分页和状态/关键词 SQL 过滤。
- 不修改交接、任务保存、财务付款、工资凭证或成本口径。

## 2. 结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| App 类型检查 | PASS | `npm run type-check` |
| App 分包构建 | PASS | `npm run build:mp-weixin` |
| 选择器契约 | PASS | `check:selectors` |
| 财务选择器契约 | PASS | `check:finance-pickers` |
| 财务工人补录契约 | PASS | `check:finance-payment-worker` |
| UI 规范 | PASS | `check:ui` |
| 动作/导航/客户端文案/分包 | PASS | 命令输出 |
| 离职交接员工选择 | PASS | 截图、网络请求 |
| 任务负责人选择 | PASS | 截图、网络请求 |
| 工人选择页真实空态 | PASS | 截图、API |
| 工人选择页有数据夹具 | PASS（仅 UI） | 截图 |
| 320/390 宽度 | PASS，无横向溢出 | `evidence/web-render.json` |
| 业务 Console 错误 | 0 | H5 仅有一条 uni H5 导航栏开发态 rejection |
| 图像视觉评审 | `VISUAL_REVIEW_UNAVAILABLE` | 当前模型无图像输入能力 |

## 3. 截图

- `handover-receiver-selected-390.png`
- `task-owner-selected-390-visible.png`
- `worker-picker-real-empty-390.png`
- `worker-picker-fixture-390.png`
- `worker-picker-fixture-320.png`

## 4. 未覆盖

- 当前租户没有在职工人，工人有数据状态使用 Playwright 路由夹具验证，不作为业务事实。
- 未执行真实离职交接、任务保存或工人补录提交，避免改变业务数据。
- 未做微信开发者工具真机复验；已做 uni-app H5 390/320 真实渲染和 App 分包构建。
- 未独立验收、未上传体验版、未发布。
