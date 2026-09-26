# A0.2 AI资源点数账本 Handoff

## Status

`implementation_and_self_test_done_pending_acceptance_and_target_migration`

## Business Freeze

- `ANS-BIZ-20260926-004`
- `DEC-012`
- `BR-011`
- `BR-012`
- `MET-003`

T06 已允许开发正式点数账本机制，但真实票价、模型倍率、套餐点数、资源包规格价格、货币扣款和退款开票继续暂缓到 A0.4。

## Scope

- 企业资源账户；
- append-only 点数账本；
- 不可变费率版本；
- 预占、结算、释放、返还和调整；
- 成功、失败、超时、取消、重复和缓存规则；
- 企业与门店归属；
- 平台成本不进入租户接口；
- `ai.billing.formal-enabled` 默认关闭。

## Changed Files

- `src/main/java/com/th4/edge/decoboon/ai/billing/**`
- `src/test/java/com/th4/edge/decoboon/ai/AiBillingContextTest.java`
- `docs/design/migrations/65-AI资源点数账本.sql`
- `scripts/build-release.sh`
- `docs/product/2026-09-26-AI资源点数账本P0产品需求规格.md`
- `docs/product/2026-09-26-AI谈单官P0产品需求规格.md`

## Verification

- `mvn -o -q -DskipTests compile` PASS
- `AiBillingContextTest` 4 tests PASS
- 正式扣点默认关闭
- 账户、预占、释放、消耗、返还和调整流水可审计
- 费率计算按向上取整且成功调用最低1点

## Next

1. 新增平台运营成本与费率管理 Provider。
2. 将正式账本接入 AI 调用网关，但仍保持功能开关关闭。
3. 在隔离 MySQL 执行迁移 65。
4. 由 acceptance 独立验证并发、余额不足、超预占和返还边界。
5. A0.4 成本验证后再确定真实费率、套餐点数和资源包价格。
