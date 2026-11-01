# 独立验收执行流程

执行资产：`docs/testing/**`

## 定位

它是测试资产执行流程，不是业务事实源，也不是产品开发或缺陷修复者。原专用验收 Skill 已归档，运行能力保留在 `docs/testing/scripts/`。

```text
测试计划 + 冻结用例 + Oracle Lock
→ 创建 RUN
→ 页面级执行
→ 证据与结果
→ DEF 与复验
→ 关门建议
```

## 使用方式

```text
按 `docs/testing/plans/` 中当前计划和冻结用例执行指定 RUN。
```

创建运行：

```bash
python3 docs/testing/scripts/create_run.py \
  --run-id RUN-20261101-001 \
  --plan TP-20261101-V0.1 \
  --case-id TC-CRM-BOSS-CUSTOMER-001
```

最终校验：

```bash
python3 docs/testing/scripts/validate_run.py \
  docs/acceptance/runs/RUN-20261101-001 --final
```

## 权限边界

允许：

- 读取测试资产、业务事实、页面和只读对账数据；
- 在明确授权环境中通过页面执行受控测试动作；
- 创建 RUN、结果、证据、缺陷和报告；
- 对已修复缺陷执行独立复验。

禁止：

- 修改业务规则、测试用例、oracle lock、matrix 或产品代码；
- 直接造页面终态、代替业主确认、真实支付或正式发布；
- 以开发自测或接口通过冒充页面验收；
- 在没有证据或存在未处理 P0/P1 时给 `PASS`。

## 当前状态

专用 Skill 已按用户要求归档；测试资产、RUN 创建/校验脚本和模板继续有效。是否执行独立验收仍由用户和高风险门禁决定。
