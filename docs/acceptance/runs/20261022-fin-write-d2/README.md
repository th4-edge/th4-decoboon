# FIN-WRITE-D2 独立验收执行包

- 问题：`PI-20261022-016`
- 业务裁决：`ANS/T12-BIZ-20261022-010`
- 决策/规则：`DEC-089` / `BR-055`
- 切片：`FIN-WRITE-D2` 按 ID 写入目标门店范围统一
- 当前状态：`PASS（FIN-WRITE-D2）`；独立全量 Java22 1056/0/0/0
- 独立复验记录：`02-independent-acceptance.md`（最终结论：PASS）
- 本次复验重点：F1 员工工资/提成来源门店继承、F2 空门店历史保持、F3 退款单测门禁
- 开发证据：`01-dev-evidence.md`
- 复验证据摘要：`03-reacceptance-evidence.txt`
- 最终全量证据：`05-final-independent-evidence.md`
- 独立验收：`02-independent-acceptance.md`
- 历史失败尝试：`03-full-test-attempt.md`
- 独立全量最终证据：`05-final-independent-evidence.md`
- 全量测试绿色复跑：`04-full-test-green.md`

## 验收环境

- 独立工作副本，不使用共享工作区 `target`
- H2 MySQL 模式
- 两企业：企业 A（默认总店 + 普通门店）、企业 B
- 企业 A 角色：BOSS、总部财务、门店财务、店长、设计师
- 企业 B：跨企业门店

## 必验用例

| # | 场景 | 期望 |
| --- | --- | --- |
| 1 | 门店财务/店长修改他店应收 | 403/404，无写入 |
| 2 | 门店财务/店长修改他店应付 | 403/404，无写入 |
| 3 | 门店财务/店长向他店应付付款 | 403/404，无付款、无核销、无流水 |
| 4 | 门店财务/店长向他店原收款退款 | 403/404，无退款、无流水 |
| 5 | 门店财务/店长修改他店结算 | 403/404，无状态变化 |
| 6 | 门店财务/店长释放他店质保金 | 403/404，无状态变化 |
| 7 | 门店财务/店长更正他店手工流水 | 403/404，无修订 |
| 8 | 门店财务生成他店流水凭证 | 403/404，无凭证文件 |
| 9 | 总部财务混合两家门店流水生成一张凭证 | 400，提示同一门店 |
| 10 | 门店财务向他店员工工资/提成应付发放 | 403/404，无付款、无核销、无流水 |
| 11 | 本店正向写操作 | 成功，门店/团队/负责人继承来源事实 |
| 12 | 多应付付款混店 | 400，整笔拒绝 |
| 13 | 空门店历史数据 | BOSS/总部财务可处理；门店角色不可见 |
| 14 | 失败后重试 | 不产生部分写入或重复副作用 |

## 必查代码路径

- `finance/receivable/update`
- `finance/payable/update`
- `finance/payment/save`
- `finance/payment/employee/save`
- `finance/refund/save`
- `finance/settlement/update`
- `finance/settlement/retention-release`
- `finance/ledger/update`
- `document/voucher/build`

## 通过条件

- 所有正向、跨店、跨企业、多目标混店和恢复用例 PASS；
- 不存在他店对象被更新、付款、退款、核销、出凭证或写流水；
- 失败时不出现部分写入；
- 独立验收人不能是实现人；
- 通过后回写 `PI-20261022-016`、`registry.json`、`10-reconciliation.md`。
