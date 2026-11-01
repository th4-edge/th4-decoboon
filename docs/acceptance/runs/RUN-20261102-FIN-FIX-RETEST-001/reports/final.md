# RUN-20261102-FIN-FIX-RETEST-001 复验报告

## 结果

- 用例：3
- PASS：0
- FAIL：0
- BLOCKED：3
- NOT_RUN：0

## 缺陷复验

### DEF-20261102-001

`VERIFIED`：Web已为店长显示冲正入口，填写原因和确认项后可完成冲正；原收款保留并转Reversed，生成¥100反向台账，应收已收从¥200降至¥100、待收恢复至¥648.73。Finance调用冲正仍返回403，符合DEC-075/DEC-076。

整条冻结用例仍为`BLOCKED`，因为用例把Finance声明为冲正执行角色，与业务规则冲突；且App授权微信身份不可用。

### DEF-20261102-002

`VERIFIED`：`finance/project/report`与`finance/settlement/page`对同一项目的totalCost、estimatedCost、actualCost、costComplete、costIncompleteCount、profitAmount、grossMargin七项完全一致；Web结算页继续显示成本未完整、利润为空、确认结算禁用。

整条跨端用例仍为`BLOCKED`，因为App授权微信身份不可用。

## 结论

- 缺陷修复：PASS（缺陷级复验）
- 原P0用例：BLOCKED（角色契约冲突 + App身份缺失）
- 不允许把缺陷级VERIFIED写成整条P0 PASS
