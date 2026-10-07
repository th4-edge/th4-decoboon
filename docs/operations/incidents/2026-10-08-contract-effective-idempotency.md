# 合同重复确认生效幂等修复

## 现象

合同已生效并生成唯一项目后，再次提交 `project/contract/update` 的 `Effective`，返回：

```text
400 合同生效后不能更换付款或施工计划
```

数据库未产生重复项目，但接口幂等语义错误。

## 根因

`ProjectContractContext.markEffective` 每次都会调用 `ContractPlanContext.ensure`。`ensure` 对已有付款和施工计划快照再次调用 `select`，而 `select` 禁止已生效合同更换计划，导致重复生效被误判。

## 修复

`ContractPlanContext.ensure` 在付款计划和施工计划快照都已存在时直接返回，不再重复走选择/替换逻辑。

## 回归

```bash
mvn -q -Dtest=ContractPlanContextTest,ContractSignRoleTest,ProjectCreationScopeGuardTest test
```

本地 dev 验证：重复 `Effective` 返回成功，`projectId=233710289445453824` 保持不变，`db_project` 仅 1 条。
