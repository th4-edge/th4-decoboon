# 2026-09-27 生产热修 · Admin 订阅调整“订阅不存在”

## 根因

Admin 订阅与续费页使用企业列表 `CompanyTenant`。原实现提交：

```text
subId = selected.comId
```

但后端 `platform/subscription/update` 按订阅主键 `subId` 查询，因此把企业 ID 当成订阅 ID，稳定返回“订阅不存在”。

企业分页快照 `PlatformTenant` 同时缺少真实 `subId`，导致前端没有可用的正确订阅主键。

## 修复

- 后端 `PlatformTenant` 新增真实 `subId`。
- `TenantDataAdapter.tenant(company)` 从当前订阅写入 `subId`。
- Admin 类型 `CompanyTenant` 增加 `subId`。
- Admin 订阅调整提交真实 `subId`。
- 无订阅企业禁用“调整”按钮并给出明确提示。
- 增加平台验收断言：企业分页必须返回随后可成功调整的真实 `subId`。

## 版本与制品

| 仓库 | 分支 | commit | 生产目录 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/admin-subscription-subid` | `6b89fd6` | `/opt/decoboon/releases/20260927-161525` |
| th4-decoboon-admin | `hotfix/admin-subscription-subid` | `bf27f86` | `/opt/decoboon-admin/releases/20260927-161700` |

后端制品 SHA-256：

```text
beac77e5ccf803ab86eda675e4ae713cd32f8a78927eff145de3cdf395c81a1d
```

Admin 制品 SHA-256：

```text
27c1e4acd9698eb8f060417325af2ac9baaa40ec6f6f1db29571208cc07d69be
```

## 验证

- `PlatformAdminAcceptanceTest` PASS。
- 后端全量测试 PASS。
- Admin `npm run build` PASS。
- 后端生产热修部署成功，上一版本 `/opt/decoboon/releases/20260927-155033` 保留。
- Admin 热修部署成功，上一版本 `/opt/decoboon-admin/releases/20260927-155500` 保留。
- 热修 release 日志中未再出现“订阅不存在”。
- 事故 `INC-20260927-9e5cc3140b` 已标记 fixed。

## 回滚

- 后端：`/opt/decoboon/releases/20260927-155033`
- Admin：`/opt/decoboon-admin/releases/20260927-155500`
- 不使用任何数据库迁移，回滚仅涉及代码和静态资源。

## 后续

- 将热修合并到 `prod`，再回灌 `test` 和 `dev`。
- 后端与 Admin 分别创建补丁 tag `v2026.09.27.2`。
