# TCR-20261102-001 · 拆分财务收款与冲正角色用例

> 状态：已被 `DEC-099` 取代，不再按“Finance 无收款冲正能力”拆分用例
> 日期：2026-11-02
> 来源：`RUN-20261101-CORE-P0-002`、`RUN-20261102-FIN-FIX-RETEST-001`

> 2026-11-02 后续用户裁决：总部财务可在全企业、门店财务可在本店执行收款冲正；其他角色仍拒绝。测试资产应以 `DEC-099` 和 `CHG-20261102-023` 为准，并保留本文件与历史 RUN 作为旧口径证据，不追改历史结果。

## 1. 问题

冻结用例 `TC-FIN-FINANCE-RECEIPT-002` 同时声明：

- 角色：`Finance`
- 步骤：登记部分收款、重复提交、超余额、执行收款冲正

但正式业务规则为：

- `DEC-075` / `DEC-076`：收款冲正属于高危动作，下放店长仅限本店；财务不自动获得。
- App 能力矩阵：`act:receiptReverse` 仅 Boss、StoreManager 持有。
- 实测：Finance 调用 `finance/receipt/reverse` 返回 403；StoreManager 可正常完成。

因此原用例无法作为单一 Finance 角色的完整预期执行。

## 2. 建议拆分

### 保留：`TC-FIN-FINANCE-RECEIPT-002`

- 角色：`Finance`
- 范围：部分收款、重复提交幂等、超余额拒绝、刷新回读。
- 移除：收款冲正步骤。

### 新增：`TC-FIN-FINANCE-RECEIPT-REVERSAL-003`

- 角色：`Boss`、`StoreManager`
- 前置：存在有效收款，原应收尚未结清。
- 步骤：
  1. 授权角色从 Web/App 收款记录进入冲正。
  2. 填写冲正原因、日期、说明并确认。
  3. 回读原收款 `Reversed`、反向台账唯一、应收余额恢复。
  4. Finance 调用冲正返回 403。
- Provider：`finance/receipt/reverse`
- 规则：`BR-055`、`DEC-075`、`DEC-076`。

## 3. 已知实证

- StoreManager Web 冲正：PASS。
- 原收款保留并转 `Reversed`。
- 反向台账 ¥100。
- 应收已收 ¥200→¥100，待收恢复至 ¥648.73。
- Finance 冲正：403。
- 证据：`docs/acceptance/runs/RUN-20261102-FIN-FIX-RETEST-001/evidence/TC-FIN-FINANCE-RECEIPT-002/`

## 4. 门禁

- 未更新 `core-chain-v0.1.json` 与 Oracle Lock 前，原用例只能标 `BLOCKED`。
- 新版本测试资产发布后，使用新 RUN 执行，不追改已完成 RUN 的历史结果。
