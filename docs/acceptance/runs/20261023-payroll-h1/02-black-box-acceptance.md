# H1 员工薪酬 P0 事故收口 · 黑盒验收记录

## 1. 环境

- 后端：Java 22，本地 `127.0.0.1:8090`
- Web：Vite `127.0.0.1:8085`
- 数据：临时 MySQL `127.0.0.1:33332 / decoboon_h1_schema`
- 数据来源：2026-10-22 验收 schema dump + H1 最小夹具
- 生产：未访问
- 角色：BOSS、总部财务、门店财务、店长、员工

## 2. 结果

- API 黑盒：`14 PASS / 0 FAIL`
- Web DOM/截图：BOSS、Finance、StoreManager 三角色 PASS
- 控制台：0 errors，1 个既有 warning
- 状态：`BLACK_BOX_PASS_PENDING_INDEPENDENT_SIGNOFF`

## 3. H1-01 提成解耦

| 用例 | 结果 |
| --- | --- |
| Finance 最终确认 | 403，PASS |
| 考勤未完整 | 400「考勤未确认」，PASS |
| 无工资项 | 400「工资单没有工资项」，PASS |
| 实发为负 | 400「实发为负」，PASS |
| 提成未结清时 BOSS 确认 | 成功，状态 `Confirmed`，`commissionComplete=false`，PASS |
| 重复确认 | 成功且应付/工资条计数保持 `1/1`，PASS |

证据：`http/H1-01-*.json`、`sql/H1-01-*.txt`、`api-results.json`。

## 4. H1-02 Web 角色矩阵

| 角色 | 薪酬方案 | 月度考勤 | 生成工资单 | 最终确认 | 计提提成 |
| --- | --- | --- | --- | --- | --- |
| BOSS | 可见 | 可见 | 可见 | 选中草稿后可见，受考勤/金额前置约束 | 可见 |
| Finance | 可见 | 可见 | 可见 | 不显示 | 可见 |
| StoreManager | 不显示 | 可见 | 不显示 | 不显示 | 不显示 |

补充：

- StoreManager 页面显示“本店财务只读”；
- StoreManager 员工发放按钮仍可见，属于 B3B 未决发放权限，不计入 H1 PASS；
- 截图：`screenshots/web-boss-employee-salary.png`、`web-finance-employee-salary.png`、`web-manager-employee-salary.png`、`web-manager-employee-commission.png`、`web-finance-employee-commission.png`。

## 5. H1-03 考勤详情门店范围

| 角色 | 本店 | 他店 | 跨企业 |
| --- | --- | --- | --- |
| StoreManager | 成功 | 404 | - |
| 门店财务 | 成功 | 404 | - |
| 总部财务/BOSS | 企业范围成功 | 企业范围成功 | 404 |

证据：`http/H1-03-*.json`。

## 6. 边界

- 本次为同一执行会话完成开发后的黑盒验证，不冒充独立验收签署。
- 未访问生产、未发布、未执行 DDL。
- 真实微信 App 不涉及 H1 Web 变更；不据此声明整个薪酬域闭环。
- B3B 发放/确认/作废边界仍 BLOCKED。
