# CHG-20261102-002 · 店长客户360路由修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-002 |
| 类型 | 缺陷修复 |
| 风险级别 | L2 |
| 状态 | `COMMITTED_VIA_CHG-20261102-004 / APP BLOCKED` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联 | `DEF-20261102-003`、`TC-CRM-MANAGER-HANDOFF-003`、`BR-001`、`DEC-066` |

## 2. 问题与修复

- 问题：店长客户列表“查看客户”跳转 `/workbench/customers/:customerId`，但 `StoreManager` 路由集合未注册 `wbCustomer360`，实际进入 `/forbidden`，阻断转交和设计师指派。
- 修复：StoreManager 路由加入 `wbCustomer360`。服务端仍按登录员工门店范围裁剪，不扩大数据权限。
- 提交结果：未单独提交路由；已将路由与客户360页面实现作为最小依赖闭包提交到 `CHG-20261102-004`，Web commit `3b2e220`。

## 3. 验证

| 检查 | 结果 |
| --- | --- |
| Web type-check | PASS |
| 店长客户列表 → 客户360 | PASS |
| 选择业务房屋并指派审计设计 | PASS |
| 客户设计ID `199500000000000105` | PASS |
| 房屋设计ID同源 | PASS |
| 设计师客户列表回读 | PASS |
| Boss/店长回读 | PASS |
| App端 | BLOCKED，无授权微信身份 |
| 最小独立提交复现 | PASS：客户360页面 + CRM overview + 内容归因 + 店长路由作为独立闭包提交，干净 worktree build 通过 |

## 4. 发布

已随 `CHG-20261102-004` 提交到本地 Web `dev`（`3b2e220`）；未推送、未发布。完整 CRM P0 仍因 App 未执行保持 BLOCKED。
