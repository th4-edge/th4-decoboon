# CHG-20261104-007 · 员工提成切换对象字段重置

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261104-007` |
| 类型 | 缺陷修复 / 财务表单交互 |
| 风险级别 | L2 |
| 状态 | 待发布 |
| 负责人 | Codex |
| 日期 | 2026-11-04 |
| 关联需求/事故/业务裁决 | `DEF-FIN-COMMISSION-FORM-001`、`BR-022`、`DEC-024`、`BR-055`、`DEC-089` |

## 2. 目标与范围

- 目标：提成表单切换员工或项目后，不得继承上一上下文的签约额、比例、提成金额和备注。
- 范围内：Web 提成计提抽屉、App 提成计提页、Web/App 定向真实页面复验。
- 非范围：不修改提成公式、Provider、工资事实或历史提成。
- 受影响端：Web、App。

## 3. T16 表单评审摘要

- 上下文对象：员工、主合同项目；任一对象更换后依赖金额字段失效。
- 短字段：签约额、手工比例、提成金额。
- 结果区：员工、项目、金额和计算口径必须属于当前上下文。
- 主行动：页面员工与最终提交empId一致，不得把上一员工输入提交给新员工。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | Finance/Web提成 | 谢玲玲填写52000/4%/2000/备注后切董夏鹏 | 金额空、比例2%、提成空、备注空 | `RUN-20261104-FIN-COMMISSION-FORM-FIX-001` |
| 2 | Finance/App提成 | App执行相同切换 | 同上 | 同上 |
| 3 | 项目切换 | 已填写金额后更换主合同项目 | 依赖金额字段重置 | 本轮代码路径已覆盖，待后续真实页面补证 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| `th4-decoboon-web` | `codex/finance-form-lifecycle-hotfix-20261104` | `327222d` | `FinanceCommissionDrawer.vue` |
| `th4-decoboon-app` | `codex/finance-form-lifecycle-hotfix-20261104` | `e88c6a7` | `commission-edit.vue` |
| `th4-decoboon` | `codex/finance-p0-remote-retest-20261104` | 随本记录提交 | 用例、RUN、缺陷与证据 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| Web 门禁与构建 | `npm run build` | PASS | Web worktree |
| App 门禁与构建 | `npm run build:mp-weixin:release` | PASS | App `dist/build/mp-weixin` |
| 生产缺陷复现 | `/finance/employee-commissions` | FAIL：旧字段全部保留 | `RUN-20261104-FIN-COMMISSION-FORM-001` |
| Web/App 修复复验 | 本地生产构建 + 生产只读API | PASS | `RUN-20261104-FIN-COMMISSION-FORM-FIX-001` |

## 7. 发布与回滚

- 数据库迁移：无
- 配置/密钥变化：无
- 当前生产：尚未包含本修复
- 回滚条件：切员工后仍残留金额，或页面员工与提交员工不一致
- 实际是否回滚：否

## 8. 结论

Web/App 修复与本地真实页面定向复验通过，`DEF-FIN-COMMISSION-FORM-001` 状态为 `FIXED_PENDING_RELEASE`。生产部署和全部财务 P0 最终门禁尚未完成。
