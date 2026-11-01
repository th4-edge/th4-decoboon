# CHG-20261102-002 · 店长客户360路由修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-002 |
| 类型 | 缺陷修复 |
| 风险级别 | L2 |
| 状态 | `BLOCKED_DEPENDENCY / NOT_COMMITTED / APP BLOCKED` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联 | `DEF-20261102-003`、`TC-CRM-MANAGER-HANDOFF-003`、`BR-001`、`DEC-066` |

## 2. 问题与修复

- 问题：店长客户列表“查看客户”跳转 `/workbench/customers/:customerId`，但 `StoreManager` 路由集合未注册 `wbCustomer360`，实际进入 `/forbidden`，阻断转交和设计师指派。
- 修复：StoreManager 路由加入 `wbCustomer360`。服务端仍按登录员工门店范围裁剪，不扩大数据权限。
- 提交阻塞：该路由依赖尚未进入版本的客户360页面实现（`StoreCustomerView.vue` 当前仍承载大量未提交改动）。只提交路由会产生“可进入但页面不具备客户360能力”的假修复，因此未形成 dev 提交。

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
| 最小独立提交复现 | FAIL/BLOCKED：仅路由补丁依赖未提交页面实现，不可独立复现，已从候选提交回退 |

## 4. 发布

未提交、未发布。完整CRM P0仍因 App 未执行保持 BLOCKED；代码侧需先隔离并提交客户360页面实现，再重新提交本路由补丁。
