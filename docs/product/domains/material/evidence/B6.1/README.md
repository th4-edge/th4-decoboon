# B6.1 修复独立验收缺陷

日期：2026-10-17
环境：本地 dev 后端 8090、H5 8080、Web 4175、MySQL 127.0.0.1:3306/decoboon。
性质：实现者自测与真实页面证据，不是 B6.2 独立验收，不宣称 `ACCEPTED`。

## 修复结果

| 缺陷 | 结果 | 证据 |
| --- | --- | --- |
| B6-DEF-001 作业单成本可见性 | PASS | `defect-verification.json#B6-DEF-001` |
| B6-DEF-002 采购金额/供应商可见性 | PASS | `defect-verification.json#B6-DEF-002` |
| B6-DEF-003 结算页/详情角色门禁 | PASS | `defect-verification.json#B6-DEF-003` |
| B6-DEF-004 重复下单/应付 | PASS | `defect-verification.json#B6-DEF-004` |
| B6-DEF-005 未到场可结算 | PASS | `defect-verification.json#B6-DEF-005` |
| B6-DEF-006 暂估应付显示 0 | PASS | `defect-verification.json#B6-DEF-006` |

## 关键实测

- 同一作业单行、同一 payload 连续两次 `material/sheet/order`：只有 1 张采购单、1 条采购明细、1 条应付；
- Designer `material/sheet/info` 不再返回报价成本、采购价、结算价；
- Designer `project/purchase/page` 保留数量和履约状态，金额/供应商/单价不返回；Finance/Boss 仍可读；
- Designer/PM `material/settlement/page|info` 返回 403；
- 未到场采购明细 `preview` 为空，`confirm` 返回“采购明细尚未到场，不能结算”；
- `finance/payable/page` 返回 `estimated=true`；App 显示“待定价/结算后安排付款”，Web 显示“待结算”。

## 测试与 UI

- 后端定向测试：5 个测试类 PASS；
- App：type-check、MP 构建、UI、分包检查 PASS；
- Web：type-check、构建 PASS；
- Web 1440×900：`scrollWidth=innerWidth=1440`，Console Errors 0，截图 `screenshots/b61-web-payables-estimated.png`；
- App H5 390×844：`scrollWidth=innerWidth=390`，Console Errors 0，截图 `screenshots/b61-app-payables-estimated.png`。

## 边界

- 未执行 DDL、迁移、生产部署、正式小程序提审/发布；
- 临时作业单/采购/应付/PII 测试数据在验收后清理，测试账号密码哈希恢复；
- B6.2 仍须由独立验收角色复跑受影响矩阵与回归，之后才由用户做最终 `ACCEPTED` 决策。
