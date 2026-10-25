# 独立验收记录

- 问题：`PI-20261022-001`
- 业务裁决：`DEC-081`
- 当前状态：`PASS`
- 执行人：Codex 独立验收责任方（非研发实现方）
- 首次验收：2026-10-22
- 最终复验：2026-10-23（Asia/Shanghai）
- 环境：本地 dev；后端 `8090`；隔离 MySQL `3307/decoboon_accept`；Web `8085`；App H5 `8086`
- 结论：**真实 App/Web 页面证据通过独立复核，PASS；仍未访问生产，不替代生产发布验收。**

## 一、最终结论

| 验收项 | 结论 | 依据 |
| --- | --- | --- |
| Web 总部财务全部门店/单店切换 | PASS | 全部门店 `¥15,000.00`；验收二店 `¥5,000.00`。 |
| Web 硬刷新指定门店 | PASS | `?storeId=2002` 证据显示验收二店且 `¥5,000.00`；`FinanceLayout` 在 `scope.resolved` 前不挂载业务页。 |
| Web 门店财务锁定本店 | PASS | 门店财务页面显示“验收二店（本店）”，金额 `¥5,000.00`，无全部门店选项。 |
| 公司级工资单单店可见性 | PASS | 总部财务、二店财务均显示 2026-10 工资单草稿 1 张；DB 快照为表头总店 `2001`、工资项二店 `2002`。 |
| Web 主材结算失败态 | PASS | 500 模拟失败显示“验收模拟结算列表失败”和“重新加载”。 |
| App 总部财务全部门店/单店切换 | PASS | 全部门店 `1.5万`、两条流水；验收二店 `¥5,000`、仅二店流水。 |
| App 门店财务锁定本店 | PASS | 显示验收二店、`¥5,000`，无全部门店切换。 |
| App 薪酬单店可见性 | PASS | 二店视图显示 2026-10 工资单草稿 1 张。 |
| App 主材结算失败态 | PASS | 显示“可结算明细暂不可确认”和“重新加载”。 |
| App/Web 同门店同口径 | PASS | 全部门店均为 `15,000`；验收二店均为 `5,000`。 |
| 跨店/跨企业负向、邀请与调店边界 | PASS | 前轮隔离后端测试通过，本轮未发现回退。 |

## 二、真实页面证据复核

证据包：

`docs/acceptance/runs/20261022-finance-organization-real/`

### 2.1 文件完整性

- README、DB 快照、SHA256 文件均存在；
- 共 12 张截图，Web 6 张、App 6 张；
- `shasum -a 256 -c screenshots.sha256`：**12/12 OK**；
- 截图尺寸有效：Web 为 1440px 宽，App 为 390px 宽；所有图片文件非空。

### 2.2 截图内容

逐图 OCR/像素区域检查与 README 一致：

- `web-hq-all.png`：门店维度“全部门店”，现金余额 `¥15,000.00`。
- `web-hq-store2-reload-fixed.png`：门店维度“验收二店”，余额 `¥5,000.00`，流水为 `STORE_FINANCE`。
- `web-store-finance-locked.png`：门店财务“验收二店（本店）”，余额 `¥5,000.00`。
- `web-payroll-store2-sheet-visible.png`：总部财务按验收二店查看，工资单草稿计数 `1`。
- `web-store-finance-payroll-sheet.png`：二店财务锁定本店，工资单草稿计数 `1`。
- `web-material-settlement-error.png`：显示模拟失败文案和“重新加载”。
- `app-hq-all.png`：全部门店 `1.5万`，可见 `+¥5,000` 与 `+1万` 两条流水。
- `app-hq-store2.png`：验收二店 `¥5,000`，仅二店流水。
- `app-store-finance-locked.png`：门店财务本店视图 `¥5,000`。
- `app-payroll-store2-sheet-visible.png`：工资单草稿计数 `1`。
- `app-store-finance-payroll-sheet.png`：二店财务工资单草稿计数 `1`。
- `app-material-settlement-error.png`：显示“可结算明细暂不可确认”和“重新加载”。

### 2.3 DB 夹具一致性

`db-fixture-snapshot.tsv` 与 README 一致：

- 临时财务：`3005 → 2001`、`3006 → 2002`；
- 临时流水：总店 `10,000.00`、二店 `5,000.00`，合计 `15,000.00`；
- 工资单 `9001`：周期 `2026-10`、表头总店 `2001`、Draft；
- 工资项 `9101`：所属工资单 `9001`、门店二店 `2002`、Draft。

只读查询隔离库 `decoboon_accept` 复核清理结果：

```text
employee_3005_3006      0
ledger_...314176        0
ledger_...192640        0
payroll_sheet_9001      0
payroll_item_9101       0
```

临时账号、流水、工资单和工资项均已清理。

## 三、首屏竞态修复复核

`FinanceLayout.vue` 已改为：

- `scope.resolved=false` 时不挂载财务业务页；
- 对齐完成后按 `activePath-storeId` 重挂当前页；
- 避免了“先以全部门店请求、后切单店覆盖”的首屏错位。

目标 Web 隔离副本合入该布局修复后：

- `npm run type-check`：PASS
- `npm run build`：PASS
- `check:client-copy`：PASS

## 四、此前技术验收结论

在真实页面证据补齐前，独立复验已完成：

- 财务邀请目标门店与角色边界：PASS；
- BOSS 跨店调财务、非 BOSS 拒绝、历史不回溯：PASS；
- 默认总店创建、不可停用、无切换入口：PASS；
- 公司级工资单 `visibleSheets` 修复：聚焦后端测试 **6/6 PASS**；
- Web 四个财务页门店变化重载：目标隔离构建 PASS；
- App 无新增业务改动，隔离 type-check/build PASS。

本轮真实页面采集未发现上述结论回退。

## 五、剩余风险

1. **非生产验证**：本证据为本地 dev 环境，不证明生产部署后的网络、缓存、网关和真实存量数据表现。
2. **默认总店存量约束**：代码路径未发现可复现绕过，但数据库仍缺“每企业恰好一个默认总店”的唯一约束；生产存量审计应作为独立运维事项保留。
3. **证据可审计性**：硬刷新截图未包含浏览器地址栏，`?storeId=2002` 由 README 采集说明、截图页面状态和 `FinanceLayout` 源码共同支持；若需要更高审计强度，可补带地址栏的截图或 HAR。
4. 本轮未执行生产发布和上线后冒烟。

## 六、发布建议

- 本问题验收结论：**PASS**。
- 允许主会话根据其治理流程更新 issue 状态。
- 本记录不直接修改 registry，不标记 `CLOSED`；生产发布仍需按发布规范执行并完成后置冒烟。
