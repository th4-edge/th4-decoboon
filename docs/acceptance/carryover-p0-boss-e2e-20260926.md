# 新装企业在跑项目承接 P0 · BOSS 端到端自测报告

> 状态：development self-test PASS  
> 日期：2026-09-26  
> 环境：本地 dev 后端 + 本地 MySQL `decoboon`  
> 账号：麦鸿装饰 BOSS `19900000001`  
> 证据目录：`docs/acceptance/evidence/carryover-p0-20260926/`  
> 说明：本报告是开发自测证据，不替代 acceptance lane 的独立验收结论。

## 1. 验证范围

```text
真实 BOSS 登录
-> 建立承接任务
-> 录入在跑项目
-> 系统校验
-> BOSS 代 BOSS/财务/项目经理确认
-> 落地报价、合同、项目、期初应收应付、接管前成本
-> 项目源标记和财务回读
-> 首笔真实收款
-> 单项目锁定
-> 锁定后撤回拒绝
-> 成本调整与调整历史
-> 附件上传、列表和锁定后删除拒绝
```

## 2. 环境结果

- MySQL 迁移 61 已在本地 `decoboon` 执行；
- 新表 6 张全部存在；
- Quote、ProjectContract、Project、Receivable、Payable 均存在 `business_origin`，存量默认 `New`；
- 本地承接批次和项目初始为 0，迁移无历史业务数据污染；
- 受影响表结构快照保存于 `/private/tmp/decoboon-migration-61-backup/schema-before.sql`。

## 3. 验收结果

| 项目 | 结果 |
| --- | --- |
| 真实 BOSS 登录 | PASS |
| 建立承接任务 | PASS |
| 项目录入和系统校验 | PASS |
| BOSS 代三类角色确认 | PASS |
| 正式承接和项目生成 | PASS |
| 项目来源标记 | PASS，`business_origin=Carryover` |
| 期初应收应付生成 | PASS |
| 接管前成本完整性传播 | PASS，`costComplete=false` |
| 首笔收款触发锁定 | PASS，`status=Locked`、`firstBusinessType=Receipt` |
| 锁定后撤回拒绝 | PASS |
| 锁定后成本调整 | PASS |
| 调整历史回读 | PASS |
| 附件上传和列表 | PASS |
| 锁定后附件删除拒绝 | PASS |
| 财务回读 | PASS，已收 5000，未收 115000，利润为空 |

## 4. 关键业务结果

```text
承接项目：229343917324107776
正式项目：229343918263631872
项目来源：Carryover
项目状态：Ongoing
首笔收款：5000.00
期初应收：120000.00
未收余额：115000.00
接管前成本：TotalOnly
成本完整：false
利润：null
锁定状态：Locked
锁定业务：Receipt
```

## 5. 证据文件

- `01-login.json`：真实 BOSS 登录；
- `02-batch.json`：承接任务；
- `03-project-saved.json`：项目草稿和校验；
- `04-project-ready.json`：承接就绪；
- `05-confirmations.json`：分角色确认；
- `06-built.json`：正式承接结果；
- `07-project-projection.json`：项目来源投影；
- `08-finance-before-receipt.json`：收款前财务；
- `09-receipt.json`：首笔收款；
- `10-locked.json`：项目锁定；
- `11-cancel-after-lock.json`：撤回拒绝；
- `12-adjustment.json`：成本调整；
- `13-finance-after-lock.json`：锁定后财务；
- `14-summary.json`：端到端摘要；
- `15-attachment-upload.json`：附件上传；
- `16-attachment.json`：附件列表和锁定后删除拒绝。

## 6. 结论边界

- 本轮证明指定本地环境、真实 BOSS 账号、真实 Provider 和真实 MySQL 下的承接主链可用；
- 不能证明生产环境、真实微信会话、全部角色真机 UI、迁移 61 在生产执行或独立验收通过；
- 仍需 acceptance lane 复核。
