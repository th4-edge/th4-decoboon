# CHG-20261101-007 核心 B1 受控写入验收

## 1. 基本信息

- 变更 ID：`CHG-20261101-007`
- 风险级别：`L1`
- 负责人：codex
- 状态：`ACCEPTANCE_PASS`
- 关联运行：
  - `RUN-20261101-CORE-B1-WRITE-001`
  - `RUN-20261101-DQ-ACCEPT-RETEST-001`
- 关联修正：`CHG-20261101-006`

## 2. 范围

- 报价接受权限与操作人留痕。
- 从已接受报价发起合同。
- 合同签约与生效分开执行。
- PendingSign 合同禁止直接生效。
- 合同生效后项目、阶段、材料计划、应收同源生成。

## 3. 结果

### 合同链 PASS

- 新客户：王芳（造数5100）
- 报价：`242541219522019328`
- 合同：`242541468600762368`
- 项目：`242541620656865280`
- 合同金额：`¥2,495.75`
- 合同状态：`Effective`
- 项目派生：8 阶段、1 材料计划、3 应收

### 合同跳步负向 PASS

PendingSign 合同直接执行 Effective 返回 400“合同签约后才能生效”，状态保持 PendingSign，无项目生成。

### 报价接受权限 PASS（复验）

- Marketer Accepted 请求：403。
- Designer 登记 Accepted：成功。
- DB：`acceptedBy=199500000000000105`、`acceptedAt` 已写入。
- 重复 Accepted 被状态机拒绝，原 acceptedBy/acceptedAt 不变。

## 4. 证据

- `docs/acceptance/runs/RUN-20261101-CORE-B1-WRITE-001/reports/final.md`
- `docs/acceptance/runs/RUN-20261101-DQ-ACCEPT-RETEST-001/reports/final.md`
- 两个 RUN 的 evidence 目录。
- 两个 RUN 均通过 `validate_run.py --final`。

## 5. 限制

- App Designer 入口可见性已验证，实际接受动作由 Web 完成；miniprogram-automator 未触发确认弹窗，作为自动化限制记录。
- 本批次不代表预约量房、材料、验收、财务写路径或发布验收。

## 6. 结论

`PASS`：本批次目标用例均通过。报价测试 Oracle 已按 `DEC-071` 修正；产品代码未因本次用例变更而修改。
