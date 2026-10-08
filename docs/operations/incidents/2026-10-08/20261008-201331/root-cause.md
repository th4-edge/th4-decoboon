# INC-20261008-afc65fb470

## 现象

- 2026-10-08 20:09，`customer/customer/dormant` 连续返回 409：`客户存在未完成量房预约，请先处理预约后休眠`。
- 本次生产日志共捕获 12 次同指纹错误，操作人为企业负责人侧页面。

## 根因

`CustomerDormantContext.setDormant` 使用 `existsByCompany_ComIdAndCustomer_CustIdAndStatusIn(..., {Scheduled})`，将任意 `Scheduled` 量房记录都当作阻塞条件。
`BR-036`/`DEC-042` 的实际门禁是“不存在未来已预约量房”，因此已过期但仍保留 `Scheduled` 状态的历史预约被误判。

## 生产事实

两个处于 `PendingMeasure` 的客户均只有过期预约、未来预约数为 0、正式合同数为 0：

| customer_id | customer status | scheduled measures | future measures | contracts |
| --- | --- | ---: | ---: | ---: |
| `228771555587391488` | `PendingMeasure` | 1 | 0 | 0 |
| `233870648483512320` | `PendingMeasure` | 1 | 0 | 0 |

## 修复

- `d2f7c79`：休眠门禁查询改为 `status = Scheduled AND scheduledAt > now`。
- 新增 `CustomerDormantMeasureWindowTest`：真实 H2 持久化验证未来预约阻断、过期预约放行。
- 回归：全量 `mvn -o test` → 573 tests / 0 failures / 0 errors。

## 发布

- release：`/opt/decoboon/releases/20261008-201837`
- 上一版：`/opt/decoboon/releases/20261008-195411`
- 健康检查：HTTP 200 + `tokenIsUndefined`
- 部署后观察：未出现新的 `customer/customer/dormant` 同指纹错误。
