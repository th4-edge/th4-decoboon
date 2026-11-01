# CHG-20261102-004 · 店长 Web 独立客户360页面

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-004 |
| 类型 | 功能补强 / 缺陷修复 |
| 风险级别 | L2 |
| 状态 | `COMMITTED / STORE_MANAGER_WEB_PASS / APP_BLOCKED` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联 | `DEF-20261102-003`、`CHG-20261102-002`、`TC-CRM-MANAGER-HANDOFF-003`、`BR-001`、`DEC-066`、`DEC-067` |
| 设计依据 | `docs/product/domains/finance/19-customer-web-standalone-360-page-v4.md`、`20-customer-web-overview-same-source-fix.md` |

## 2. 目标与范围

- 目标：店长从本店客户列表进入独立客户360页面，恢复客户上下文、房屋、业务链和转交/指派动作。
- 范围内：Web 客户列表与独立客户360页面、CRM overview 同源投影、内容归因 Provider、店长路由。
- 非范围：未修改后端 Provider、DDL、权限、金额、成本、状态机；未向 Finance/Pm/Marketer 等其他角色扩大路由。
- `?preview=1` 预览夹具继续仅用于设计预览；真实客户路由不读取预览夹具。

## 3. 实现

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-web | dev | `3b2e220` | `StoreCustomerView.vue`、`crm/api.ts`、`crm/model.ts`、`content/api.ts`、`content/model.ts`、`roles/routes.ts` |
| th4-decoboon | dev | 无代码改动 | 设计文档、CHG 和验收证据 |

## 4. 关键约束

- 客户列表的 Boss/店长/财务经营统计只消费 `customer/customer/overview`，不从当前首屏客户数组计算指标。
- 客户360详情只调用一次 `customer/customer/info`，房屋、方案、报价、合同、项目、量房、收款和内容来源继续使用各领域既有 Provider。
- overview 默认页大小收敛为 20，服务端参数上限仍为 50；Web 不固定请求 200/500。
- 店长仍只能看到本店客户；路由可达不等于扩大后端数据范围。
- 页面不把预览聚合夹具当成真实事实；无 Provider 数据时显示不可确认或空态。

## 5. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 干净提交 TypeScript / Vite build | PASS | `3b2e220` 独立 worktree `npm run build` |
| 分页门禁 | PASS | 无新增大页固定值 |
| 店长客户列表 | PASS | `/workbench/customers` 显示本店真实客户 |
| 列表进入客户360 | PASS | `/workbench/customers/242297007991947264?from=/workbench/customers` |
| production 无预览夹具 | PASS | 页面无 `预览模式`、无 `PREVIEW-*` 数据 |
| 详情请求 | PASS | `customer/customer/info=1` 次，`customer/customer/overview=0` 次 |
| 指派设计师入口 | PASS | 真实客户360可打开指派对话框，不直接写数据 |
| 返回列表 | PASS | 返回 `/workbench/customers` |
| App 真机回读 | BLOCKED | 无授权微信身份 |

## 6. 发布与回滚

- 已提交到本地 Web `dev`；未推送、未部署。
- 回滚：回退 `3b2e220`；不涉及数据库回滚。
- 完整 CRM P0 仍因 App 身份不可用保持 `BLOCKED`，不得宣称三端全部通过。
