# 角色动作依赖矩阵 V0.2 与动态烟测

## 本轮范围

在首个客户负责人动作之外，扩展5个高风险动作：

1. 客户负责设计师指派；
2. 项目成员调整；
3. 财务安排付款；
4. 财务主材下单；
5. 企业费用代填员工选择。

同时新增运行时依赖探测脚本：

```bash
TH4_TEST_PASSWORD="..." python3 docs/testing/scripts/action_provider_smoke.py
```

脚本遍历矩阵中每个 allowed=true 的角色，调用其依赖 Provider，要求 HTTP 200、success=true 且候选非空。

## 首次运行捕获的新问题

- 总计18个角色 × 依赖Provider检查；
- 首轮17 PASS / 1 FAIL；
- 失败项：`ACT-FIN-EXPENSE-PROXY / StoreManager / payroll/employee/page` 返回403。

业务规则 `BR-057 / DEC-091` 明确：

- 店长可代本店员工提交费用；
- 财务只能提交本人费用，不得代他人提交。

根因：`payroll/employee/page` 使用薪酬编辑权限 `requireEditor`（Boss、Finance），而费用代填允许 Boss、StoreManager，导致：

- 店长有代填入口，但员工候选403；
- 财务能加载员工候选，但费用保存拒绝代理。

## 修复

- `payroll/employee/page` 改用薪酬读取权限 `requireManager`（Boss、Finance、StoreManager）；
- StoreFilterContext继续按登录员工数据范围裁剪：
  - 店长只能看到本店；
  - 总部财务企业范围；
  - 门店财务本店范围；
- 财务代他人提交仍由 `ExpenseAccessContext.resolveOwner` 返回403，没有扩大费用代理权限。

## 复测结果

- 动态烟测：18/18 PASS；
- StoreManager `payroll/employee/page`：200，8名本店员工；
- Boss：200，9名企业员工；
- 定向测试、后端构建、Provider响应契约、资产校验、self-test、trace、catalog全部通过。

## 证据

- 修复前：`raw/smoke-before-fix.json`
- 修复后：`raw/smoke-after-fix.json`
- 定向测试：`raw/targeted-tests.log`
- 构建：`raw/build.log`
- Provider契约：`raw/provider-contract.log`
- 资产校验：`raw/test-assets-validate.log`
- 负向自测：`raw/test-assets-self-test.log`
