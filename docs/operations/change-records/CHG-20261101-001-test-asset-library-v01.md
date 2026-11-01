# CHG-20261101-001 测试资产库与追溯矩阵 V0.1

## 1. 基本信息

- 变更 ID：`CHG-20261101-001`
- 风险级别：`L1`
- 负责人：codex
- 状态：`DEV_VERIFIED`
- 关联：`TP-20261101-V0.1`

## 2. 目标与范围

- 目标：先建立测试事实与追溯底座，再建设执行智能体，避免智能体自行发明预期结果。
- 范围：测试策略、风险模型、用例规范、首版计划、核心主链用例、oracle lock、追溯矩阵、覆盖摘要和未映射台账。
- 非范围：生产代码、Provider 契约、业务规则、测试执行、执行智能体、小程序发布。

## 3. 影响面

- 受影响仓库/服务：测试文档与本地校验脚本，无运行时服务影响。
- 数据库/DDL：无。
- 权限：无。
- 金额/成本/利润：无业务口径变更，只引用正式规则。
- 配置/密钥：无。

## 4. 产物

- `docs/testing/00-test-strategy.md`
- `docs/testing/01-risk-model.md`
- `docs/testing/02-test-case-standard.md`
- `docs/testing/03-traceability-model.md`
- `docs/testing/plans/TP-20261101-V0.1.md`
- `docs/testing/cases/core-chain-v0.1.json`
- `docs/testing/cases/core-chain-v0.1.md`
- `docs/testing/schemas/test-case.schema.json`
- `docs/testing/traceability/oracle-lock.json`
- `docs/testing/traceability/matrix.csv`
- `docs/testing/traceability/coverage-summary.json`
- `docs/testing/traceability/unmapped.json`
- `docs/testing/scripts/test_assets.py`

## 5. 验收条件与结果

- 用例库为机器可读且每条用例绑定正式业务规则：PASS，42 条；
- 所有用例 Provider 在当前后端源码可定位：PASS；
- 所有 Web/App/Owner 页面目标在当前前端路由可定位：PASS；
- 每条用例有前置、步骤、预期页面、预期状态、不变量、禁止结果和证据：PASS；
- oracle lock 固定业务知识库提交与关键文件哈希：PASS；
- 追溯矩阵和覆盖摘要可重建：PASS；
- 负向自测能识别未知规则、未知 Provider 和缺失证据：PASS。

命令：

```bash
python3 docs/testing/scripts/test_assets.py validate
python3 docs/testing/scripts/test_assets.py trace
python3 docs/testing/scripts/test_assets.py catalog
python3 docs/testing/scripts/test_assets.py self-test
```

结果：

```text
cases=42 providers=543 webRoutes=64 appRoutes=195 rules=71
validate PASS
trace PASS
catalog PASS
self-test PASS
```

## 6. 当前覆盖

- P0：23 条；
- P1：19 条；
- 业务区域：10 个；
- 角色：Boss、StoreManager、Marketer、Designer、Pm、Finance、Owner；
- 端面：Web、App、Owner；
- 未映射：0 条用例；
- 已知缺口：7 项，明确标记为 `BLOCKED/PARTIAL/NOT_TESTED`。

## 7. 回滚

- 回滚条件：测试资产与业务事实源不一致，或未来执行智能体无法消费。
- 回滚步骤：删除本变更新增的 `docs/testing/**` 与变更记录；不影响运行系统。
- 实际是否回滚：否。

## 8. 结论

`ASSET_READY`：测试资产库和追溯矩阵已建立，可进入执行智能体设计；当前没有执行任何业务验收用例，不能据此宣称产品验收通过。
