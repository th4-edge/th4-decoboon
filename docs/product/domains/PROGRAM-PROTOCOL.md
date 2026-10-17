# 装得清全域业务域交付协议

## 1. 目的

本协议规定多个业务域、多个 Codex 会话和长期监督任务如何共享同一台电脑、同一套仓库与同一批测试环境，避免：

- 两个监督同时修改同一 Git 工作区；
- 两个域同时提交、推送或重启服务；
- 测试数据库和服务端口互相污染；
- 同一业务口径被不同会话各自解释；
- 把“代码存在”误当成“业务已验收”。

## 2. 角色

| 角色 | 职责 | 禁止 |
| --- | --- | --- |
| 全域交付总控 | 读程序看板、排下一域、管理工作区租约、检查门禁、通知用户 | 不直接修改业务代码，不绕过域 Run Envelope |
| 领域监督 | 读取自身 domain packet，执行一个垂直切片，更新证据与状态 | 不得在无租约时修改代码，不得自批独立验收 |
| 独立验收 | 黑盒验证、权限/金额/边界/幂等/跨端对账 | 不修改实现，不宣布 ACCEPTED |
| 用户 | 业务裁决、高风险授权、正式小程序发布 | 不依赖聊天记忆作为唯一状态 |

## 3. 外部状态

程序级状态：

```text
docs/product/domains/PROGRAM-BOARD.json
docs/product/domains/DOMAIN-REGISTRY.md
```

领域级状态：

```text
docs/product/domains/<domain>/state.json
docs/product/domains/<domain>/run-envelope.md
docs/product/domains/<domain>/00-charter.md
docs/product/domains/<domain>/03-blueprint.json
docs/product/domains/<domain>/10-reconciliation.md
docs/product/domains/<domain>/11-next-batch-prompt.md
```

任何会话恢复必须先读外部状态，不依赖聊天历史。

## 4. 工作区租约

同一时间只允许一个域获得代码写租约：

```text
activeLease.domain
activeLease.ownerThreadId
activeLease.status
activeLease.codeWriteAllowed
activeLease.releaseCondition
```

规则：

1. 总控只能在 `activeLease` 为空或已释放时授予下一域；
2. 领域监督修改代码前必须确认租约属于自己；
3. 新域启动前，必须确认后端 `src/main`、`src/test`、App/Web 代码没有其它域的未提交改动；
4. 切换域前必须记录交接：完成批次、未闭环项、测试命令、回滚路径、下一提示词；
5. 若租约超过 3 小时无所有者心跳，总控标记 `LEASE_STALE` 并通知用户，不得静默抢占。

## 5. 领域队列

当前顺序以 `PROGRAM-BOARD.json` 为准：

```text
material
→ contract-change
→ project-delivery
→ acceptance-rectify
→ finance-profit
→ lead-content
→ customer-property
→ measure
→ design
→ quote
→ payroll-labor
→ owner-delivery-after-sales
→ analytics
→ saas-platform
```

顺序不是永久业务优先级，而是当前依赖和风险下的交付顺序；调整必须更新程序看板并写原因。

## 6. 每域门禁

每个域必须按以下阶段推进：

```text
SCENARIOS_READY
→ BIZ_ALIGNED
→ BLUEPRINT_READY
→ PLAN_READY
→ DEV_IN_PROGRESS
→ DEV_VERIFIED
→ UI_REVIEWED
→ RECONCILED
→ ACCEPTED
→ RELEASED
→ LEARNED
```

不得跳过：

- 业务事实核对；
- 真实页面/真机证据；
- 独立验收；
- 发布边界确认。

## 7. 总控每轮做什么

1. 读取 `PROGRAM-BOARD.json`、`DOMAIN-REGISTRY.md` 和各域 `state.json`；
2. 检查当前租约所有者是否仍在推进；
3. 检查当前域是否到达门禁、是否有阻塞或需要用户决策；
4. 如果当前域未释放：只记录状态，不启动下一域；
5. 如果当前域已释放：检查代码工作区是否干净，再更新租约和下一域提示词；
6. 更新 `PROGRAM-BOARD.json` 的观测时间、阻塞和下一动作；
7. 只在门禁变化、失败、需要决策或全域收口时通知。

## 8. 通知条件

允许通知：

- 某域到达 `BLUEPRINT_READY`、`RECONCILED`、`ACCEPTED`、`RELEASED`；
- 独立验收失败并产生阻断缺陷；
- 工作区租约变化或 stale；
- 需要用户做业务/高风险/发布决策；
- 连续两次无进展；
- 全域完成或全部阻塞。

普通无变化心跳保持静默。

## 9. 发版边界

- 后端/Web 变更必须满足门禁并可回滚；
- 小程序只允许构建、上传开发版/体验版和验证；
- 正式提审与正式发布永远由用户本人完成；
- DDL、迁移、权限放宽、金额/成本/支付语义变化必须人工确认。
