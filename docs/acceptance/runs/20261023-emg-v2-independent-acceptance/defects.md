# EMG-V2 独立验收缺陷记录

本记录保留独立验收发现和最终复验结果；缺陷修复由研发侧完成，独立验收责任方未修改实现。所有问题均发生在本地验收库和本地服务。

## EMG-V2-DEF-01 · P1 · 邀请取消接口 NPE

- 状态：`CLOSED`
- 复现：BOSS 取消店长创建的 Pending 财务邀请；店长取消自己的 Pending 邀请。
- 实际：两次均返回 `success=false`、`errorCode.code=undefinedErrorCode`。
- 根因：修复前取消 Provider 未先加载完整登录员工，导致 `Employee.company` 为空；修复后已由 `TenantContext.requireEmployee` 加载完整员工。
- 独立关闭证据：`followup/def01-results.json`、`followup/sql/*.txt`。店长取消与 BOSS 取消均成功，DB `Cancelled`，席位 `3 -> 2`。
- 影响（修复前）：邀请无法取消，Pending 记录保留并占用席位；成员与组织页缺少可恢复路径。
- 证据：
  - `http/02-boss-cancel-invite.json`
  - `http/02-cancel-reprobe.json`
  - `logs/backend-direct-java.log`
  - `sql/final-db-snapshot.txt` 中邀请仍为 `Pending`

## EMG-V2-DEF-02 · P2 · 店长组织页统计权限告警

- 状态：`CLOSED`
- 复现：店长登录 Web，进入“成员与组织”。
- 修复后：店长页面无统计请求、无权限告警；成员列表正常。BOSS 页面 `statistic/organization/report => 200`，组织报告正常。
- 独立关闭证据：`followup/web-def02-results.json`、`followup/screenshots/*`、`followup/logs/*organization*`。
- 影响：成员列表仍可用，但页面存在错误提示，影响可用性和事实可信度。
- 证据：
  - `screenshots/web-manager-members-final.png`
  - `web/manager-members-snapshot.yml`
  - `logs/backend-direct-java.log`
