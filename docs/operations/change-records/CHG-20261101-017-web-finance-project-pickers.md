# CHG-20261101-017 · Web 员工待发应付、原收款与项目筛选选择器收口

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261101-017 |
| 类型 | 缺陷修复/重构 |
| 风险级别 | L2 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/事故/业务裁决 | `DEC-021` / `BR-019`、`BR-021`、`BR-022`、`CHG-20261101-015` |

## 2. 目标与范围

- 目标：清除 Web 剩余 P1 业务对象下拉，收口员工薪酬应付、客户退款原收款和项目员工筛选。
- 范围内：
  - `EmployeePayablePicker` 与员工待发应付 Provider 分页。
  - `ReceiptPicker` 与收款 Provider Active/Reversed 状态过滤。
  - `ProjectPicker` 设计师/项目经理筛选改用 `EmployeePicker`。
  - 选择器负向门禁、业务概念注册和清单更新。
- 非范围：不改变退款或薪酬金额公式、审批、权限、台账、提成调整和业务状态机；不修改 App。
- 受影响端：后端、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及员工应付、原收款读取和退款金额带入；不改变既有业务口径。
- 业务事实来源与裁决编号：`DEC-021` / `BR-019`、`DEC-023` / `BR-021`、`DEC-024` / `BR-022`。
- 尚存 GAP 或假设置：员工待发应付无真实数据；`ReceiptRefundContext.create` 未显式校验已冲正收款，需业务裁决后再决定是否补服务端门禁。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 客户退款选择原收款 | 存在有效收款 | 打开即展示有效收款，不再用 `el-select` | 截图、网络请求 |
| 2 | 原收款状态过滤 | 请求 Active/Reversed | Active 返回真实数据，Reversed 不混入 | `evidence/api-pickers.json` |
| 3 | 原收款选择回填 | 选择 399.32 收款 | 全宽卡展示项目/客户/金额/日期/节点，金额回填 | 截图、页面快照 |
| 4 | 员工待发应付真实空态 | 当前租户无应付 | 显示“当前没有待发员工应付”及恢复说明 | 页面快照 |
| 5 | 员工应付有数据展示 | Playwright 路由夹具 | 员工、类型、期间、应付/已发/待发正确展示 | 截图，明确标注夹具 |
| 6 | 项目按设计师筛选 | 选择审计设计 | 项目从 4 条收窄为 3 条，请求带 designerId | 网络请求、API 证据 |
| 7 | 静态门禁负向 | 注入 receipt/payable/selectedId 下拉 | 检查失败 | `check:forms:self-test` |
| 8 | 响应式 | 1024/1280/1440 | 无横向溢出 | `evidence/web-render.json` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 未提交 | 员工待发应付 Provider、收款状态分页、T15、证据 |
| th4-decoboon-web | dev | 未提交 | EmployeePayablePicker、ReceiptPicker、ProjectPicker、财务抽屉、业务概念注册、门禁 |
| th4-decoboon-app | dev | 不适用 | 未修改 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `mvn -q -Dtest=PayrollEmployeePaymentPageScopeTest,PayrollEmployeePickerAccessTest,EmployeePageAccessTest test` | PASS | Maven 输出 |
| 后端编译 | `mvn -q -DskipTests compile` | PASS | Maven 输出 |
| Web 构建 | `npm run build` | PASS | Web `dist/` |
| Web 门禁 | `check:forms:self-test`、`check:forms`、`check:concepts`、`check:pagination`、`type-check` | PASS | 命令输出 |
| 真实收款页面 | `/finance/refunds` | PASS（开发自测） | `docs/acceptance/runs/20261101-web-finance-project-pickers/` |
| 员工应付空态 | `/finance/employee-commissions` | PASS（真实空态） | 页面快照 |
| 员工应付有数据态 | `/finance/employee-commissions` | PASS（路由夹具，仅 UI） | 截图 |
| 项目筛选 | `/finance/expenses` 内 ProjectPicker | PASS | 网络请求 |
| 图像视觉评审 | 截图 | `VISUAL_REVIEW_UNAVAILABLE` | 当前模型无图像输入能力 |

- 未执行项及原因：未执行退款提交，避免生成真实资金台账与提成负向调整；未构造员工应付真实数据。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 不适用 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：财务选择器越权、来源应付混入、已冲正收款可选、项目筛选错误或页面布局阻断
- 回滚命令/步骤：回退后端与 Web 本批提交，使用上一稳定 release
- 回滚验证：原有收款/应付页面可读，服务端财务权限仍生效
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否（首轮分页基线需收缩）
- 返工原因：新选择器替代旧大页调用后，需要更新分页基线
- 最终结论：待观察

## 10. 复盘与沉淀

- 是否出现重复问题：是，财务业务对象仍沿用普通下拉
- 是否需要新增门禁：是，已将 receipt/payable/selectedId 纳入负向样本
- 候选 `EXP/ANT/PATH`：业务对象选择器替换必须同时处理已选项按 ID 回读和来源上下文
- 后续动作与负责人：业务裁决已冲正收款是否必须在退款服务端拒绝；继续 App P1
