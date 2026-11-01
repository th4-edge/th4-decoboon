# Web 客户经营统计同源修正

- 日期：2026-10-29
- 状态：`DEV_VERIFIED / USER_EXPERIENCE_PENDING`
- 页面：`/workbench/customers`
- 客户 360：`/workbench/customers/:customerId`
- 边界：仅修正 Web 数据源与请求方式，不修改后端 Provider、权限、状态机、金额口径或 DDL

## 1. 问题

Web 客户页面出现：

- 点击客户 360 调用 `customer/customer/info`；
- 进入客户列表出现 `customer/customer/overview` 报错；
- 非 Boss 角色的顶部统计由当前首屏客户列表计算。

## 2. 同源对账

小程序客户 Tab 的 `usesOwnerOverview` 已明确：

```text
Boss / 店长 / 财务
→ customer/customer/overview
```

小程序的客户详情页：

```text
客户详情 → customer/customer/info
```

因此：

- `info` 是客户 360 的正确详情接口，不是错误调用；
- Web 的问题不是调用 `info`，而是列表统计没有与小程序使用同一 `overview` 口径；
- 非 Boss 角色从当前页客户在前端计数，违反 Web 列表与汇总数据规范。

## 3. 修正

### 3.1 列表与统计

Boss、店长、财务统一调用一次：

```text
customer/customer/overview
```

该响应同时提供：

- `metrics`：顶部统计；
- `stageCounts`：阶段与待量房计数；
- `ownerGroups`：负责人分组；
- `customers`：当前页客户；
- `total` / `hasMore`：分页事实；
- `storeScope`：门店维度与未归属门店标注。

Web 不再：

- 对这三个角色使用 `page + overview(scope)`；
- 从首屏客户数组计算经营统计；
- 从首屏客户数组计算负责人分组和全量待量房。

`?preview=1` 也不再从预览客户卡数组计算指标和分组，而是使用与服务端响应同形状的预览聚合夹具；预览夹具不代表真实业务事实。

### 3.2 客户 360

- 独立客户 360 页面只调用一次 `customer/customer/info`；
- 移除详情加载过程中重复调用 `info`；
- 路由监听增加同一客户加载去重，避免会话恢复时重复请求；
- 房屋、方案、报价、项目、跟进、内容归因继续使用各自领域 Provider。

## 4. `overview` 报错说明

后端日志显示错误为：

```text
customer/customer/overview处理失败:当前账号无权执行该操作
```

这是服务端 403，不是 SQL、参数转换或前端解析错误。

- `overview` 只允许 Boss、店长、财务；
- 其他角色调用会被服务端拒绝；
- 修正后 Web 只在这三个经营角色下调用 `overview`；
- Finance 与 Boss 的 `overview` 权限测试均通过。

如果 Boss/店长/财务账号仍报 403，需要核对当前运行后端是否为最新制品、登录账号实际角色和会话企业/门店归属，而不是继续让前端补算统计。

## 5. 验证

| 场景 | 结果 |
| --- | --- |
| Finance 列表请求 | 仅 `customer/customer/overview` |
| Finance 顶部指标 | 使用 mock Provider 值 991/992/993/994，DOM 原样显示 |
| Finance 负责人“全部客户” | 使用 Provider `total=991` |
| Finance 全部客户待量房 | 使用 Provider `stageCounts.pendingMeasure=993` |
| 预览模式 | 指标和负责人分组从聚合夹具取值，客户卡数组不参与统计 |
| 客户 360 | 仅一次 `customer/customer/info` |
| 重复路由触发 | 去重生效，不重复请求详情 |
| Boss overview 权限测试 | PASS |
| Finance overview 权限测试 | PASS |

## 6. 真实 Finance 验收补充

真实验收发现 `overview` 的客户数组位于 `data.customers`，而 Web 适配层原先只读取 `content/list/records`。修复后：

- Finance 真实登录显示客户总数 1；
- 负责人“张伟”分组显示 1；
- 客户“郑和”正常显示；
- 客户 360 返回两张真实房屋档案；
- 返回列表后客户仍正常显示。

## 6. 规范依据

- `docs/standards/WEB-列表分页与汇总数据规范.md`
  - 汇总必须由后端标准统计接口返回；
  - 前端不得从当前页列表计算业务汇总；
  - 统计失败不得以 0 或当前页数据兜底；
  - 同一筛选下统计与列表应可对账。
- `rules.md §0.4`
- `DEC-036`
- `BR-031`
- `GAP-027`
- `DEC-067`（门店维度与数据范围）
