# 第一批 P0 运行证据

## 1. 环境

- 后端：本地 dev，`http://127.0.0.1:8090`
- 数据库：本地 `decoboon`，仅使用合成数据
- 基线：当前工作区，未修改产品代码

## 3. 同企业跨数据范围越权

### 业务员读取老板项目

请求：业务员 token 调用 `project/project/save`，传入老板负责的已生效合同对应报价。

结果：

```json
{"success":true,"projectId":"990000000000001004","projectNo":"YS-PRJ-001"}
```

### 业务员读取老板合同

临时将同一报价状态置为 `Accepted` 后，业务员调用 `project/contract/save`；调用完成后立即恢复 `Converted`。

结果：

```json
{"success":true,"contractId":"990000000000001005","contractNo":"YS-CT-001"}
```

对照：业务员调用 `project/contract/update` 返回：

```json
{"success":false,"code":"404","message":"合同不存在或无权访问"}
```

结论：更新链路有范围校验，创建/读取链路存在 P0 绕过。

## 4. 跨租户负向

临时插入并立即清理了一个 B 租户项目，Boss A 查询该项目： 

```json
{"success":false,"code":"404","message":"项目不存在或无权访问"}
```

结论：项目详情的跨租户负向通过；但同企业跨门店/团队/非参与范围仍被 `DEF-P0-PERM-001` 绕过。

## 5. 自动创建房屋

使用一个只填写客户地址、未创建房屋的合成客户调用 `customer/customer/measure`。

结果：

```text
measure_success=true
property_count=1
measure_count=1
```

验证后已清理合成客户、自动房屋、量房记录和相关通知。

结论：服务端由客户地址自动创建房屋，违反 `BR-001`。

## 6. App 客户来源枚举

用 App 实际发送值调用本地后端：

| 发送值 | 接口结果 | 实际落库 |
| --- | --- | --- |
| `Walkin` | success=true | `Other` |
| `Old` | success=true | `Other` |

后端正式枚举为 `WalkIn`、`OldCustomer`。验证后两条合成客户已删除。

## 7. 财务伪利润


```text
材料应付：¥1,600
估算成本：¥0
实际成本：¥0
累计利润：¥48,400
毛利率：97%
```

与 `GAP-003` 一致。该结果在成本完整性修复前不得作为经营或营销证据。

## 8. 基线验证

- `mvn -o -q -Dtest=DataScopeRequestFilterIntegrationTest,MeasureStateGuardTest,PropertyHandoffOwnershipTest,QuoteCalcContextTest,QuoteViewContextTest,ProjectCostContextTest,ProjectContractContextTest,DecoboonClosedLoopTest test`：PASS
- App `npm run verify:mp-weixin`：PASS；主包 480,494 B，最大分包 164,914 B，总包 1,312,811 B
- 上述测试均未覆盖已复现的同企业跨范围创建/读取绕过。
