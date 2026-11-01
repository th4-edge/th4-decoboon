# CHG-20261101-013 决策覆盖与角色动作依赖门禁

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-013` |
| 类型 | 测试资产与质量门禁 |
| 风险级别 | `L1` |
| 状态 | `DEV_VERIFIED_PENDING_APP_MANUAL` |
| 负责人 | codex |
| 日期 | 2026-11-01 |
| 关联问题 | `PI-20261101-003` |

## 2. 目标与范围

- 目标：防止“主Provider允许、动作依赖Provider拒绝”的权限接缝缺陷再次漏测。
- 范围内：Oracle规则、测试用例、角色动作依赖矩阵、资产校验和自测。
- 非范围：一次性扫描全部543个Provider；本轮先建立机制和CRM首个高价值动作。
- 受影响端：测试资产、Web、App、后端验收。

## 3. 业务与决策

- `USER-DEC-20261029-009` 已加入 Oracle Lock。
- 财务可转交客户负责人；总部财务企业范围、门店财务本店范围。
- 财务不得指派或改派设计师。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 决策未映射用例 | 资产校验可发现 | `test_assets.py self-test` |
| 2 | 动作依赖Provider不存在或未声明 | 资产校验失败 | `action_dependency_detected=true` |
| 3 | Finance打开客户负责人弹层 | 候选非空、无403 | `RUN-20261101-CRM-FIN-OWNER-001` |
| 4 | Marketer加载候选 | 仍403 | `05-role-scope-api.json` |

## 5. 变更文件

- `docs/testing/traceability/oracle-lock.json`
- `docs/testing/cases/core-chain-v0.1.json`
- `docs/testing/traceability/coverage-map.json`
- `docs/testing/traceability/role-action-provider-matrix.json`
- `docs/testing/scripts/test_assets.py`
- `docs/testing/03-traceability-model.md`
- `docs/testing/02-test-case-standard.md`

## 6. 验证

- `test_assets.py validate`：43 cases、1 action、72 rules，PASS；
- `test_assets.py self-test`：五类负向全部检出，PASS；
- `test_assets.py trace`：矩阵和覆盖摘要重建，PASS；
- `test_assets.py catalog`：用例目录重建，PASS；
- `RUN-20261101-CRM-FIN-OWNER-001`：Web/API/数据范围通过，App UI因automator异常BLOCKED。

## 7. 未完成

- App客户详情负责人弹层手工/真机点击；
- 将首层点击烟测扩展到其他高价值页面动作；
- 自动从前端动作清单生成依赖矩阵，目前首个动作采用人工登记+脚本校验。

## 8. 结论

机制已落地并完成负向自测，可以检测“动作依赖Provider权限错误”这一类问题。App UI仍需手工复验后才能关闭本批。
