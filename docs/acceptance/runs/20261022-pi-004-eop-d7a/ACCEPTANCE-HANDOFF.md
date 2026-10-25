# EOP-D7a 独立验收交接

| 项 | 内容 |
| --- | --- |
| 关联问题 | `PI-20261022-004` |
| 研发任务 | `T12-BIZ-20261022-005` / `EOP-D7a` |
| 验收状态 | `CONDITIONAL` |
| 开发自测 | 37/37 PASS（含 HTTP 级企业/门店权限），编译 PASS；P1 导入缺陷已修复复验 |
| 变更级别 | L2/L3 边界（成本语义、旧价格退场；本切片无 DDL） |

## 1. 验收边界

- 验证 `CatalogItem.costPrice` 只由报价项目库人工维护；
- 验证采购下单/应付形成不覆盖成本价；
- 验证 `PartnerPrice.purchasePrice` 只读兼容、不再参与新业务价格判断；
- 验证 `MaterialSupplier.purchasePrice` 不由保存接口覆盖、不用于品牌兜底价格；
- 验证劳务价格仍可维护和读取；
- 不验收 Web/App 主数据页面改造（`EOP-D8`）；
- 不验收设计模版 App 闭环（`EOP-D10`）；
- 不改变实际成本、结算和利润公式。

## 2. 用例

| ID | 场景 | 前置 | 动作 | 预期 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- | --- |
| D7A-01 | 成本价人工维护 | BOSS/店长/财务；JSON 导入 | 修改或导入空成本/明确 0 | null 保持待维护；0 保持明确零成本 | 定向测试已通过，真实接口待验 | CONDITIONAL |
| D7A-02 | 有价下单不改成本 | 已选材计划、有效供应商 | 下单并形成应付 | 应付形成，CatalogItem.costPrice 不变 | 待填 | PENDING |
| D7A-03 | 无价下单不改成本 | 已选材计划 | 无价下单 | 暂估应付形成，成本价不变 | 待填 | PENDING |
| D7A-04 | PartnerPrice 历史兼容 | 存在历史 PartnerPrice | 调用保存只改 laborPrice | 原 purchasePrice 保留，成本价不变 | 待填 | PENDING |
| D7A-05 | MaterialSupplier 保存 | 存在历史 purchasePrice | 修改 MOQ/交期/账期 | purchasePrice 不覆盖，成本价不变 | 待填 | PENDING |
| D7A-06 | 品牌兜底 | 无材料供应关系、供应商品牌匹配 | 查询供应商候选 | 返回供应商，不使用 PartnerPrice 价格 | 待填 | PENDING |
| D7A-07 | 跨企业/跨门店 | 两企业/两门店 | 伪造 partnerId/supplierId/itemId | 403/404，不泄露数据 | HTTP 级跨企业拒绝、角色 403、同企业双门店共享已通过 | PASS（HTTP） |
| D7A-08 | 劳务价格兼容 | 现有劳务结算数据 | 劳务结算读取 laborPrice | 行为不变 | 待填 | PENDING |

## 3. 必须回填

- 真实账号、企业、门店和项目；
- 请求/响应原文或接口日志；
- 页面/接口截图；
- 数据库只读核对结果；
- 失败项、风险、是否需要研发返工；
- 独立验收结论（PASS/CONDITIONAL/BLOCK）。

## 4. 当前结论

开发自测通过，不代表独立验收通过；在 `D7A-01~08` 完成前，本切片和 `PI-20261022-004` 均不得标记 `CLOSED`。


## 5. 独立验收回执

- 验收角色：独立验收 Agent `01a16445-08c6-7562-b686-c494f8a094f8`
- 结论：`CONDITIONAL`
- 代码级阻断：无
- 已关闭：P1 JSON 导入 null→0
- 独立复验：34/34 定向测试 PASS
- 剩余阻断：全量统一基线、真实 HTTP/数据库/角色、跨门店、历史价格只读接口语义
- CLOSED：否


## 6. 双门店复核回执

独立验收确认：D7A-07 的跨企业、跨门店和角色 HTTP 级证据齐全，更新为 PASS（HTTP 级）。整体仍为 CONDITIONAL，剩余为干净全量基线、MySQL 等价环境、真实运行服务冒烟和页面可见性。
