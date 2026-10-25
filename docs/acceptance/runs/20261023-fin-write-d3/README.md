# FIN-WRITE-D3 独立验收任务书

## 验收对象

`PI-20261022-016` 的 `FIN-WRITE-D3`：店长本店凭证生成、重打与跨端动作对齐。

## 业务依据

- `ANS-BIZ-20261022-010`
- `T12-BIZ-20261022-010`
- `DEC-089`
- `BR-055`

## 验收边界

- 不验收来源字段、期初资金 DDL、收款冲正/账龄页面。
- 不改变金额、模板版本或凭证 PDF 公式。
- 独立验收不得修改实现代码。

## 必须覆盖

1. 店长本店生成凭证成功；
2. 店长本店查看、重打凭证成功且同号同文件；
3. 店长跨店生成/重打拒绝；
4. BOSS、总部财务、门店财务权限不回归；
5. 模板配置仍为独立能力，店长可维护但影响全企业并留痕；
6. Web 与 App 同角色入口与后端一致；
7. Web 构建、动作契约检查；
8. 开发自测不得作为独立验收结论。

## 证据入口

- 开发证据：`01-dev-evidence.md`
- 后端定向：`FinanceStoreScopeHttpAcceptanceTest#storeManagerCanIssueAndReprintOwnStoreVoucherButNotOtherStore`
- 后端回归：`VoucherBuildAcceptanceTest,DocumentTemplateContextTest`
- Web：`check:actions`、`build`

## 输出

写入 `02-independent-acceptance.md`，结论仅允许 `PASS / FAIL / BLOCKED`。
