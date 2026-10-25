# PI-20261022-004 · EOP-D7a 开发自测证据

| 项 | 内容 |
| --- | --- |
| 关联问题 | `PI-20261022-004` |
| 业务裁决 | `DEC-084`、`ANS-BIZ-20261022-005` v2、`EV-354` |
| 研发任务 | `T12-BIZ-20261022-005` / `EOP-D7a` |
| 状态 | 定向/HTTP 自测 37/37 PASS；独立验收 CONDITIONAL；不得 CLOSED |
| 日期 | 2026-10-22 |

## 1. 变更边界

本切片只处理供应商旧价格链路和人工成本价边界：

- `CatalogItem.costPrice` 保持人工维护，不由 `PartnerPrice`、`MaterialSupplier` 或采购价自动刷新；
- `PartnerPriceContext` 已删除；
- `PartnerPrice.purchasePrice` 不再新增或用于采购执行价来源判断；
- `partner-price/save` 只维护 `laborPrice`，更新时保留历史 `purchasePrice`；
- `MaterialSupplier.purchasePrice` 保存时不再覆盖，品牌兜底不再读取旧合作方价格；
- 采购执行价来源统一为“手工录入”；
- 本切片无 DDL、无价格迁移、无供应关系价格历史表。

## 2. 验证

执行命令：

```text
mvn -q -Dtest=SupplierLegacyPriceBoundaryTest,MaterialPurchaseContextTest,MaterialSheetOrderFlowTest,MaterialGoodsReceiptFlowTest test
```

测试结果：

- `CatalogCostMaintenanceTest`：3/3
- `CatalogImportCostBoundaryTest`：2/2
- `SupplierLegacyPriceBoundaryTest`：4/4
- `MaterialPurchaseContextTest`：4/4
- `MaterialSheetOrderFlowTest`：15/15
- `MaterialGoodsReceiptFlowTest`：6/6
- `SupplierLegacyPriceHttpAcceptanceTest`：3/3
- 合计：37/37，0 failures，0 errors

编译：

```text
mvn -q -DskipTests compile
```

退出码 0。`00-compile.log` 为该命令输出；`-q` 模式无输出即成功。

## 3. 证据文件

- `01-goods-receipt.txt`
- `02-material-order.txt`
- `03-purchase-context.txt`
- `04-legacy-price-boundary.txt`
- `05-catalog-cost-maintenance.txt`
- `06-catalog-import-cost.txt`
- `07-supplier-http-acceptance.txt`
- `00-compile.log`
- `90-full-test-summary.log`

## 4. 验收待覆盖

- BOSS/店长/财务真实页面：维护报价项目库成本价后，采购下单不改变成本价；
- 旧 App 继续提交 `purchasePrice` 时服务端不得覆盖；
- 旧合作方价格仅历史只读可查，不参与新业务价格判断；
- 无价采购仍形成暂估应付，成本价不变化；
- 跨企业、跨门店、越权请求拒绝；
- 多角色真实接口/页面证据。

## 5. 明确未完成

- 未做 Web/App 供应商主数据与选择器改造（后续 `EOP-D8`）；
- 未做设计模版 App 双端闭环（后续 `EOP-D10`）；
- 未修改实际成本、结算和利润公式；
- 本目录是开发自测证据，不代表独立验收通过。


## 6. 全量测试结果

执行命令：

```text
mvn -q test
```

结果：973 tests，7 failures，5 errors，0 skipped。完整日志见 `05-full-test.log`。

失败项与本切片无直接调用关系，主要来自共享工作区其它并行变更或隔离副本未携带的非源码文档：

- `DecoboonClosedLoopTest`：`PackageTemplate.code` 非空约束失败；
- `QuoteViewContextTest`：报价视图合并金额 2000.00 vs 200；
- `SummaryImplementationRulesTest`：既有实体图规则失败；
- `ProviderSecurityAcceptanceTest`：既有 Provider 安全清单失败；
- `PayrollWorkbenchContextTest`：工资工作台既有断言失败；
- `StoreDimensionBindingHttpAcceptanceTest`：门店绑定既有断言失败；
- `FinanceStoreScopeHttpAcceptanceTest`：财务门店既有断言失败；
- 迁移/蓝图/发布基线测试因隔离副本未包含 `docs/`、`site/` 等非源码资料而缺文件。

结论：不能声明全量测试通过。定向测试 27/27 和编译通过可证明本切片核心行为，但全量套件必须由独立验收环境在干净的统一基线上重跑。


## 7. P1 修复复验

独立验收发现 JSON 导入把空成本写成 0 的 P1 缺陷，已修复：

- `catalog/service/items/importer/support/Import.java` 不再对 `source.getCostPrice()` 调用 `zero(...)`；
- `null` 保持为成本待维护；
- 明确 `0` 仍保持为零成本；
- 新增 `CatalogImportCostBoundaryTest` 覆盖两种值；
- 复验后定向测试总计 34/34 PASS，编译 PASS。


## 8. 独立验收结论

独立验收角色 `01a16445-08c6-7562-b686-c494f8a094f8` 复核结论为 `CONDITIONAL`：

- P1 JSON 导入 null→0 已修复并关闭；
- 34/34 定向测试独立复验通过；
- 跨企业 PartnerPrice/MaterialSupplier 拒绝单测通过；
- 未发现新的代码级阻断；
- 全量统一基线、真实 HTTP/数据库、跨门店和真实角色链路仍未覆盖，禁止标记 CLOSED。


## 9. HTTP 级权限补证

新增 `SupplierLegacyPriceHttpAcceptanceTest`，使用独立 H2 数据库、真实 Provider 路由、真实密码登录 token 和两个企业夹具：

- 企业 1 财务跨企业保存 `partner-price` → HTTP 404；
- 企业 1 财务保存本企业历史 PartnerPrice 时 `purchasePrice` 不可覆盖，`laborPrice` 正常更新；
- CatalogItem 人工成本价保持 88.00；
- 企业 1 财务跨企业保存材料供应关系 → HTTP 404；
- 材料供应关系分页只返回本企业数据；
- 企业 1 设计师保存材料供应关系 → HTTP 403。

测试结果 3/3 PASS。该证据为 HTTP 层，不是生产/本地 MySQL 真实账号证据。
