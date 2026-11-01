# RUN-20261101-CORE-B1-001 核心 B1 独立验收报告

## 运行信息

- Plan：`TP-20261101-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1`
- 环境：本地 `local-fixture`
- 端面：Web + App
- 角色：Boss、StoreManager、Finance、Pm

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| ---: | ---: | ---: | ---: | ---: |
| 3 | 3 | 0 | 0 | 0 |

## 用例结果

### TC-CRM-PERMISSION-STORE-005 PASS

- 总店店长：客户总数 20。
- 滨湖店长：本店客户 0。
- 滨湖店长直接访问总店客户：Web 进入无权访问页，App 显示“客户不存在或无权访问”。
- API：`customer/customer/info` 与 `update` 均返回 404。
- DB：总店客户 20，滨湖店客户 0。

### TC-CT-BOSS-PROJECT-002 PASS

- 合同状态：`Effective`。
- 项目状态：`Preparing`。
- 合同与项目 customerId、propertyId 一致。
- 项目派生阶段 8 个、材料计划 1 条、应收 4 条。
- Finance Web、Pm App 项目详情均可回读合同生效与项目事实。

### TC-WB-PM-GUIDANCE-002 PASS

- Web 和 App 均显示 PM 在管项目 3 个。
- 包含施工中、待验收、待开工项目。
- 未默认选择第一条项目；Web 明确要求选择项目上下文。
- `statistic/pm/report` 返回 `projectCount=3`。

## 结论

`PASS`

本结论覆盖本 RUN 的三条用例，不代表报价状态机、合同新建/生效写入、全部角色或全部业务域已验收。未执行新增报价接受、合同变更生效或收付款写入。
