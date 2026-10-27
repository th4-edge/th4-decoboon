# EMG-V2 缺陷聚焦独立复验

- 批次：`ACC-PI-20261022-005-EMG-V2` follow-up
- 日期：2026-10-23
- 结论：`PASS`
- `EMG-V2-DEF-01`：CLOSED
- `EMG-V2-DEF-02`：CLOSED
- 组合结果：原 18/18 P0 PASS + 2/2 follow-up defects CLOSED

## DEF-01 邀请取消

真实店长创建 Pending 邀请并由店长本人取消：

- 创建成功：`1792689454791342807`
- 取消成功，无错误码
- DB：`status=Cancelled`，`cancelled_at` 已写入
- 席位：`used_seats 2 -> 3 -> 2`，`pending_invites 1 -> 0`

店长创建 Pending 邀请并由 BOSS 取消：

- 创建成功：`1792689455694925518`
- 取消成功，无错误码
- DB：`status=Cancelled`，`cancelled_at` 已写入
- 席位：`used_seats 2 -> 3 -> 2`，`pending_invites 1 -> 0`

证据：`def01-results.json`、`http/*.json`、`sql/*.txt`、`logs/def01-followup.log`。

## DEF-02 店长组织页

- 店长 `/workbench/organization`：
  - 成员列表正常加载 2 人；
  - 页面无“当前账号无权执行该操作”；
  - 网络记录无 `statistic/organization/report` 请求；
  - 无 403/失败请求，console error 0。
- BOSS `/workbench/organization`：
  - `statistic/organization/report => 200 OK`；
  - 组织报告显示在职人数、签约额、人均产出、人均订单、角色产能；
  - console error 0。

证据：`web-def02-results.json`、`screenshots/*`、`logs/*organization*`。

## 边界

- 未修改实现源码或测试；
- 未恢复 Team/TeamLeader；
- 未操作生产或正式发布；
- 保留共享 MySQL `3307 / decoboon_accept`。
