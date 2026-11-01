# CHG-20261101-006 报价接受测试 Oracle 修正

## 1. 基本信息

- 变更 ID：`CHG-20261101-006`
- 风险级别：`L1`
- 负责人：codex
- 状态：`DEV_VERIFIED`
- 关联：`DEC-071`、`RUN-20261101-CORE-B1-WRITE-001`
- 用例：`TC-DQ-PERMISSION-QUOTE-005`

## 2. 问题

旧用例要求“Designer 不得接受报价”，但受控验收实测：

- Marketer `quote/update Accepted` → 403；
- Designer `quote/update Accepted` → 200；
- DB 写入 `accepted_by` 和 `accepted_at`。

该行为与 `DEC-071` 一致：报价接受由设计师正常登记，系统记录操作人；Marketer 不可置位。旧用例预期与正式业务事实冲突。

## 3. 修正

- 用例标题改为“报价接受登记权限与操作人留痕”。
- 预期改为 Marketer 拒绝、Designer 可登记、acceptedBy/acceptedAt 留痕；重复 Accepted 按现有状态机拒绝，原登记事实不被覆盖。
- 绑定规则改为 `BR-002`、`DEC-071`。
- 页面目标保留 Web 与 App 报价详情；Provider 路由不变。

## 4. 验证

```text
test_assets.py validate PASS
test_assets.py trace PASS
test_assets.py catalog PASS
```

## 5. 结论

`DEV_VERIFIED`：测试资产已与 `DEC-071` 对齐。`RUN-20261101-CORE-B1-WRITE-001` 中对应用例保持历史 `BLOCKED`，需在新 RUN 复验，不追改旧运行结果。
