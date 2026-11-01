# 测试追溯模型

## 1. 追溯链

```text
业务规则/决策/场景
→ 业务对象与状态
→ Provider
→ App / Web / 业主端页面
→ 测试用例
→ 执行证据
→ 缺陷/复验
→ 覆盖结论
```

## 2. 机器事实

- `traceability/oracle-lock.json`：冻结业务知识库提交和源文件哈希；
- `cases/core-chain-v0.1.json`：机器可读测试用例；
- `traceability/matrix.csv`：由用例和 oracle lock 生成；
- `traceability/role-action-provider-matrix.json`：角色、页面动作、主 Provider 和依赖 Provider 的权限矩阵；
- `traceability/coverage-summary.json`：按区域、角色、端面、风险和动作统计；
- `traceability/unmapped.json`：没有冻结规则、没有 Provider/页面映射或已知 GAP 的能力。

## 3. 状态

| 状态 | 含义 |
| --- | --- |
| `MAPPED` | 规则、Provider、页面和用例均已定位 |
| `UNMAPPED` | 缺少规则、Provider、页面或用例中的至少一项 |
| `STALE` | oracle lock 或页面/Provider 契约已变化，需要复核 |
| `BLOCKED` | 业务口径未裁决或环境/身份不可用 |

## 4. 强制规则

1. 没有业务规则 ID 的用例不能进入 `READY`；
2. 没有 Provider 的业务用例不能标 `MAPPED`；
3. 声明页面端的用例必须至少有一个对应页面目标；
4. 已批准业务决策必须至少映射一条可执行用例，或在 `unmapped.json` 明确登记；
5. 页面可见动作必须映射角色允许性、主 Provider 和全部依赖 Provider；
6. 对有权限角色，主 Provider 与依赖 Provider 必须同时允许；禁止“主接口允许、弹窗依赖接口403”；
6.1 `action_provider_smoke.py` 必须按矩阵对所有允许角色实测依赖 Provider，要求200且候选非空；
7. 矩阵由脚本重建，不手工维护重复事实；
8. 每次业务知识库或用例库变化后重新校验并更新 oracle lock；
9. 追溯矩阵存在 `UNMAPPED/STALE` 时，相关区域不得标 `COVERED`。
