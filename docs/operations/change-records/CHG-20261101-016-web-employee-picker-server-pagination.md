# CHG-20261101-016 · Web 员工选择器统一与服务端分页

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261101-016 |
| 类型 | 缺陷修复/重构 |
| 风险级别 | L2 |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/事故/业务裁决 | `DEC-021` / `BR-019`、`BR-021`、`CHG-20261101-015`、`T15/T16` |

## 2. 目标与范围

- 目标：把 Web 员工选择器改为服务端搜索、筛选和数据库分页，并消除负责人、设计师、项目经理、支持人、任务负责人和设计运营筛选中的 `el-select` 绕过。
- 范围内：
  - `user/employee/page`、`payroll/employee/page` 数据库分页与角色/状态/关键词/门店/员工 ID 过滤。
  - Web `EmployeePicker` 分页、已选回读、全宽卡和紧凑筛选摘要。
  - 五个绕过页面及设计运营筛选接入。
  - 选择器静态门禁语义识别和负向自测。
- 非范围：不改变员工角色矩阵、数据范围、金额、成本、薪酬公式、App 员工选择器和其他业务对象选择器。
- 受影响端：后端、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及员工对象读取范围；不改变既有权限语义。
- 业务事实来源与裁决编号：`DEC-021` / `BR-019`、`DEC-023` / `BR-021`、`DEC-024` / `BR-022`。
- 尚存 GAP 或假设置：独立验收和物理真机未执行；23 个 App 选择器 10× 专项已覆盖员工与薪酬员工 Provider。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | Boss 按多岗位选择 | 打开项目成员或设计运营筛选 | 使用员工专用选择器，服务端按角色分页 | `evidence/api-pagination.json`、网络请求 |
| 2 | 已选员工回显 | 员工不在第一页 | 通过 `empId` 回读并展示姓名/岗位/门店/状态 | `evidence/api-pagination.json`、截图 |
| 3 | 店长门店范围 | 店长选择员工 | 只返回本店范围，跨店请求拒绝 | `evidence/api-pagination.json` |
| 4 | 权责分离 | 设计师访问普通目录/薪酬目录 | 普通目录按既有范围可用；薪酬目录 403 | `evidence/api-pagination.json` |
| 5 | UI 回显 | 表单/筛选选择员工 | 表单全宽卡、筛选紧凑摘要，不只显示“已选择员工” | `evidence/screenshots/` |
| 6 | 门禁负向 | 注入 ownerId/designerId/managerId 下拉 | `check:forms` 必须失败 | `npm run check:forms:self-test` |
| 7 | 响应式与键盘 | 1024/1280/1440 | 无横向溢出，Escape 关闭并归还焦点 | `evidence/web-render.json` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 未提交 | `EmployeeRepository`、`user/employee/page`、`payroll/employee/page`、定向测试、T15、证据 |
| th4-decoboon-web | dev | 未提交 | `EmployeePicker.vue`、身份/薪酬 API、负责人/设计师/任务责任人/受理人与薪酬来源页面、选择器门禁 |
| th4-decoboon-app | dev | 不适用 | 未修改 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `mvn -q -Dtest=EmployeePageAccessTest,PayrollEmployeePickerAccessTest,EmployeePageSerializationContractTest test` | PASS | Maven 输出 |
| 后端上下文/HQL | `mvn -q -Dtest=AuthSessionInfoLazyAcceptanceTest test` | PASS | Maven 输出 |
| 后端编译 | `mvn -q -DskipTests compile` | PASS | Maven 输出 |
| Web 全构建 | `npm run build` | PASS | Web `dist/` |
| Provider 语义显式化 | 普通目录 `user/employee/page`；薪酬目录 `payroll/employee/page` | PASS，真实页面网络确认 | `docs/acceptance/runs/20261101-selector-p1-final/evidence/web-runtime.json` |
| 旧全量入口清理 | `identityApi.employeesByRole`、`identityApi.designers` | PASS，已移除并加入负向门禁 | 命令输出 |
| 员工岗位完整性 | 默认岗位导航 | PASS，含 `TeamLeader` | 薪酬员工选择器页面实测 |
| Web 门禁 | `check:forms:self-test`、`check:forms`、`check:concepts`、`check:permissions`、`check:pagination` | PASS | 命令输出 |
| 既有动作注册 | `npm run check:actions` | BLOCKED；`finance/refund/save` 与员工付款 Provider 未登记，和本批员工选择器改动无关 | 命令输出 |
| Provider 角色烟测 | `action_provider_smoke.py` | 32/32 PASS | `action-provider-smoke.json` |
| 真实页面 | `/workbench/design-reviews`、`/designer/projects` | PASS（开发自测） | `docs/acceptance/runs/20261101-web-employee-picker/` |
| 图像视觉评审 | 截图 | `VISUAL_REVIEW_UNAVAILABLE` | 当前模型无图像输入能力 |

- 未执行项及原因：独立验收和物理真机未执行；10× 员工基准已在 `CHG-20261101-023` 完成。
- 已知失败及是否与本次相关：`check:actions` 被既有退款/员工付款动作注册缺口阻断，与员工选择器改动无关；浏览器存在 1 条 Vue Router 初始路由 no-match warning，最终页面与交互正常。

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

- 回滚条件：员工候选出现越权、分页错误、已选员工无法回读或业务动作失败
- 回滚命令/步骤：回退 Web 与后端该提交，使用上一稳定 release
- 回滚验证：员工候选恢复旧行为，业务动作保存服务端权限仍有效
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否（首轮类型检查与分页基线各出现 1 次可修复问题）
- 返工原因：`FinanceEmployeeRef` 与通用员工投影类型不一致；`employeesByRole` 临时 `size=50` 触发分页门禁
- 最终结论：开发自测与真实 Web 网络/渲染通过；待独立验收。

## 10. 复盘与沉淀

- 是否出现重复问题：是，选择器“已有组件但页面绕过”和“门禁通过但漏检”重复出现
- 是否需要新增门禁：是，已新增员工字段语义识别与负向自测
- 候选 `EXP/ANT/PATH`：待沉淀“选择器分页后必须按 ID 回读已选项”“静态门禁必须有正负样本”
- 后续动作与负责人：执行独立验收与物理真机复核；新页面必须显式声明员工 Provider，禁止恢复全量入口。
