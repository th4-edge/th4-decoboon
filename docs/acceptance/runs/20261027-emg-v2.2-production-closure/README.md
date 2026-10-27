# EMG-V2.2 邀请生产闭环验收记录

- 日期：2026-10-27
- 批次：`EMG-V2.2`
- 状态：`PRODUCTION_DEPLOYED / REAL_USER_ACCEPTANCE_PENDING`
- 业务依据：`DEC-081`、`DEC-082`、`DEC-085`
- 变更记录：`CHG-20261027-003`

## 1. 结论

- 后端邀请取消与邀请分页主体注入缺陷已修复并发布。
- App 1.0.72 已具备待接受邀请列表、重新分享、取消确认和邀请编号移除。
- Web 已具备邀请创建、待接受邀请列表与取消邀请。
- 生产后端 release：`/opt/decoboon/releases/20261027-161846`
- 生产 Web release：`/opt/decoboon-web/releases/20261027-162200`
- 15905600230 的真实邀请仍为 `Pending`、凭证 `emp_id=NULL`；需被邀请人本人打开有效邀请卡片完成接受后，才能确认最终工作台。

## 2. 自动验证

- 后端全量：245 套件 / 949 用例 / 0 失败 / 0 异常。
- 新增邀请 Provider 回归：取消完整员工加载、无员工凭证业务 403、注入类型边界、BOSS 分页。
- App：`check:invite-flow`、`type-check`、`build:mp-weixin:release`、`check:subpackages` PASS。
- Web：`type-check`、`check:permissions`、`check:workbenches`、`build` PASS。

## 3. 生产验证

- 后端服务 active，HTTPS 200。
- 生产 JAR 已确认包含 `User<?> principal + TenantContext` 的邀请分页/取消实现。
- Web production chunk 已确认包含“待接受邀请 / 邀请员工 / 取消邀请 / 请在 App 重新分享”。
- 发布后 10 分钟监控未出现新的取消邀请空公司或邀请页主体注入异常；现存同类错误最近时间均早于发布。
- 当前真实邀请：`1793006971920754377`，角色 `Marketer`，状态 `Pending`，有效期至 `2026-11-02 17:29:31`。

## 4. 待用户完成

1. 在微信公众平台将 App `1.0.72` 设置为体验版（正式提审与发布由用户本人执行）。
2. 邀请人或被邀请人打开原邀请卡片；如原卡片丢失，由 BOSS/店长在 App 1.0.72 的待接受邀请列表重新分享。
3. 被邀请人使用手机号 `15905600230` 授权接受。
4. 接受后验证：邀请 `Accepted`、创建麦鸿业务员员工、凭证绑定 `emp_id`、重新登录进入业务员工作台。

## 5. 回滚

- 后端：`bash scripts/rollback-prod.sh /opt/decoboon/releases/20261027-103227`
- Web：`ssh root@120.55.15.159 "ln -sfn /opt/decoboon-web/releases/20261026-110450 /opt/decoboon-web/current && nginx -t && systemctl reload nginx"`
- 不删除邀请、员工、安全日志或成员日志。
