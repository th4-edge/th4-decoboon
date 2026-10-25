# EMG-V2 follow-up 缺陷修复证据

- 批次：`ACC-PI-20261022-005-EMG-V2`
- 日期：2026-10-23
- 结论：`DEF-01 / DEF-02 CLOSED`
- 独立复验：[followup/README.md](../20261023-emg-v2-independent-acceptance/followup/README.md)

## 1. EMG-V2-DEF-01 邀请取消 NPE

修复：`invite/invitation/cancel` Provider 先执行 `TenantContext.requireEmployee`，再进入 `MembershipContext.cancel`，不再把缺少企业关联的 `@User` 最小投影传进业务权限判断。

验证：

- 隔离后端 5 个相关测试类：`26 tests / 0 failures / 0 errors`；
- 店长取消本人 Pending 邀请：成功，DB `Cancelled`；
- BOSS 取消店长创建的 Pending 邀请：成功，DB `Cancelled`；
- 独立验收遗留 Pending 邀请已取消；
- 三条记录的 `cancelled_at` 已写入。

文件：

- `backend-targeted-tests.log`
- `recheck_cancel.py`、`recheck-cancel.log`
- `def01-*.json`、`def01-cancelled-db.txt`
- `def01-results.json`

## 2. EMG-V2-DEF-02 店长组织页统计告警

修复：`BossOrganizationView.load()` 对非 BOSS 不请求 `statistic/organization/report`；店长仅加载成员事实，BOSS 继续加载组织报告。

验证：

- Web `type-check`、`check:permissions`、`check:workbenches`、`vite build` 均 PASS；
- 独立复验：店长页面无统计请求、无 403、无权限告警；BOSS 统计 200 且组织报告正常。
- 独立证据：`../20261023-emg-v2-independent-acceptance/followup/web-def02-results.json`。

## 3. 边界

- 修复仅在本地工作树和验收库验证；
- 无 DDL、无生产数据、无生产发布；
- 共享 MySQL `3307` 未停止；
- 验收账号密码不落盘，token 不写入证据。
