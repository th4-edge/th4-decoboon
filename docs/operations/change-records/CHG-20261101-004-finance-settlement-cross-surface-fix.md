# CHG-20261101-004 App 财务结算门禁与金额展示跨端修复

## 1. 基本信息

- 变更 ID：`CHG-20261101-004`
- 风险级别：`L2`
- 负责人：codex
- 状态：`DEV_VERIFIED / RETEST_PASS`
- 关联：`PI-20261101-001`、`DEF-20261101-001`、`DEF-20261101-002`

## 2. 目标与范围

- 目标：修复 App 财务项目页在成本不完整时确认结算按钮仍可用，以及金额展示取整到元的问题。
- 范围：App 财务项目页、App 金额格式化工具、金额格式测试。
- 非范围：结算后端状态机、Web 财务页面、应收应付业务规则、正式发布。

## 3. 影响面

- 仓库：`th4-decoboon-app`
- 文件：
  - `src/subpackages/finance/pages/project.vue`
  - `src/core/format.ts`
  - `scripts/verify-money-format.mjs`
- 数据库/DDL：无。
- 权限：无权限模型变化；前端门禁与既有后端门禁对齐。
- 金额/成本/利润：无公式变化；财务项目页展示改为精确到分，利润为空规则不变。

## 4. 实现

- 新增 `moneyExact()`：固定两位小数和千分位，缺值返回 `-`。
- 财务项目页全部财务金额改用 `moneyExact()`。
- 新增 `settlementConfirmBlockedReason`：成本不完整时阻止确认结算并展示原因。
- `changeSettlement('confirmed')` 增加运行时门禁，防止绕过按钮状态。

## 5. 验证

```text
node scripts/verify-money-format.mjs    PASS 11/11
npm run type-check                      PASS
npm run build:mp-weixin                 PASS
npm run check:ui                        PASS
npm run check:finance-ia                PASS
```

独立复验：

```text
RUN-20261101-FIN-P0-RETEST-001
TC-FIN-FINANCE-COST-004 PASS
TC-FIN-CROSSSURFACE-PARITY-006 PASS
2/2 PASS, 0 FAIL, 0 BLOCKED
```

复验同时覆盖 Boss、店长、财务 Web 与 Finance App；合同、回款、待收、应付、已付、总成本均一致到分。

## 6. 回滚

- 回滚条件：App 财务项目页金额布局不可用、门禁误阻断合法确认或构建失败。
- 回滚方式：回退上述 App 文件修改。
- 实际是否回滚：否。

## 7. 结论

`RETEST_PASS`：原 P1 缺陷和 P2 展示差异已修复并通过独立页面复验。尚未提交、发布或进行全量财务验收。
