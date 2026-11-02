# CHG-20261102-023 · 手工应收应付支持全部类型

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-023 |
| 类型 | 线上缺陷修复 |
| 风险级别 | L3 |
| 状态 | 生产发布完成，待登录后真实页面保存冒烟 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户 2026-11-02 明确指令；覆盖 `DEC-089` / `BR-055` 中手工类型白名单 |

## 2. 目标与范围

- 目标：手工应收支持 `First/Progress/Final/Retention/Other` 全部类型；手工应付支持 `Material/Salary/EmployeeSalary/EmployeeCommission/Project/Supplier/Expense/Other` 全部类型。
- 范围内：后端 Save 类型门禁、Web 新增表单及入口文案、App 类型契约/选项/提交校验与入口文案。
- 非范围：自动来源生成规则、金额公式、付款核销、权限、门店/租户范围、数据库结构。
- 受影响端：Web、App、后端 Provider。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是（应收/应付业务类型）。
- 业务事实来源与裁决编号：用户 2026-11-02 明确指令；`DEC-089`、`BR-055` 的旧类型限制按本次指令覆盖。
- 尚存 GAP 或假设置：无；自动来源唯一性与门店/租户范围规则保持不变。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 后端应收 | 手工新增五种应收类型 | 全部创建成功且类型回读一致 | 财务闭环定向测试 |
| 2 | 后端应付 | 手工新增八种应付类型 | 全部创建成功且类型回读一致 | 财务闭环定向测试 |
| 3 | Web | 新增应收/应付 | 可选择并提交全部类型 | Web 类型检查/构建；页面冒烟 |
| 4 | App | 新增应收/应付 | 可选择并提交全部类型 | App 类型检查/构建；开发者工具冒烟 |
| 5 | 既有边界 | 自动来源、金额、门店/租户校验 | 原规则不受影响 | 回归测试与静态检查 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/finance-manual-types-20261102` | `aecbdf0a20c79eb0089576ec8ea28c9c0b5e83c0` | 应收/应付 Save、财务闭环测试 |
| th4-decoboon-web | `hotfix/finance-manual-types-20261102` | `9f24d652731556c40cc84ddda61f061347893fe3` | 财务应收/应付 Drawer、入口文案 |
| th4-decoboon-app | `hotfix/finance-manual-types-20261102` | `c0d149d5e703c9c1c401deed4bcc3532c2c74f9e` | 财务模型/API 映射、登记页、入口文案、版本 `1.0.79/179` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `mvn -Dtest=FinanceClosedLoopTest test` | PASS 14/0/0/0；五种应收、八种应付全部创建并回读 | `/tmp/th4-finance-manual-types-mvn.log` |
| 后端全量测试 | `mvn test` | PASS 1214/0/0/0 | `/tmp/th4-finance-manual-types-mvn-full.log` |
| Web 构建 | `npm run build` | PASS；类型检查、表单门禁与生产构建通过 | `/tmp/th4-web-finance-manual-types-build.log` |
| App 完整校验 | `npm run verify:mp-weixin` | PASS；`1.0.79/179`、类型检查、构建、分包检查通过 | `/tmp/th4-app-finance-manual-types-verify-179.log` |
| 分支同步 | `origin/dev = origin/test = origin/prod = HEAD` | PASS（三仓库） | 远端 refs |
| 后端生产健康 | `GET /api/auth/session/info` | HTTP 200；服务 `active` | 生产 release `20261102-104711` |
| Web 生产静态资源 | 线上 `index` assets 与本地最终构建 | PASS：`assets/index-CXE21dLw.js` | Web release `20261102-104836` |
| App 开发版上传 | 微信开发者工具 CLI `upload` | PASS：`1.0.79`，总包 4,933,570 B | `dist/release/upload-info-1.0.79.json` |

- 未执行项及原因：生产浏览器无可复用登录会话，真实页面创建全类型应收/应付的保存冒烟待用户登录后执行。
- 已知失败及是否与本次相关：无；App 首次完整校验因隔离工作区跨仓库相对路径缺失而中断，补齐路径映射后全量通过。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 后端 `20261102-104711`；Web `20261102-104836`；App 开发版 `1.0.79` |
| 制品路径/SHA-256 | 后端 `th4-decoboon-1.0.73-build.tar.gz` / `55c1e4537023f93fc84f0b168940fe3f6f646353c688b43328e060312f0c6019` |
| 生产 release/版本 | 后端 `/opt/decoboon/releases/20261102-104711`；Web `/opt/decoboon-web/releases/20261102-104836`；App `1.0.79/179` 已上传开发版 |
| 上一版本/回滚点 | 后端 `/opt/decoboon/releases/20261102-042648`；Web `/opt/decoboon-web/releases/20261102-042649` |
| 健康检查 | HTTPS 200；`tokenIsUndefined`（未登录预期）；systemd `active`；Web 入口与 `/v3/` 守卫通过 |
| 发布后观察 | 发布层通过；登录后写路径冒烟待执行 |

- 数据库迁移：无 DDL
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：新增类型保存失败、既有自动来源或财务汇总异常、生产登录/健康检查失败。
- 回滚命令/步骤：后端切回 `/opt/decoboon/releases/20261102-042648` 并重启 `decoboon`；Web 执行 `ln -sfn /opt/decoboon-web/releases/20261102-042649 /opt/decoboon-web/current && nginx -t && systemctl reload nginx`。
- 回滚验证：恢复后执行登录会话、应收/应付列表和保存冒烟。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是（功能验证首轮通过；构建环境路径问题不计业务返工）
- 返工原因：无
- 最终结论：三仓库已同步并推送 `dev/test/prod`；后端与 Web 已生产发布，App 1.0.79 开发版已上传。生产写路径需用户登录后完成最终页面冒烟，正式提审与发布由用户本人执行。

## 10. 复盘与沉淀

- 是否出现重复问题：是，旧业务白名单同时落在后端与两端 UI，导致线上行为不一致
- 是否需要新增门禁：后端枚举全量保存测试；App/Web 类型选项与后端枚举对账
- 候选 `EXP/ANT/PATH`：财务类型白名单必须由单一业务决定驱动，三端同步改动
- 后续动作与负责人：发布后回写业务知识库 `DEC-089` / `BR-055`
