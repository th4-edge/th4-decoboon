# 企业管理 · EMG-V2 发布边界

## 1. 发布结论

- 本批次未发布生产；
- 未创建生产 tag；
- 未上传体验版或提交小程序正式版；
- 无 DDL、迁移、生产数据修改或删除。

## 2. 环境

- 后端：本地 `8090`
- Web：本地 `8085`
- App：微信开发者工具自动化 `9420`
- 数据库：本地隔离 `decoboon_accept`，MySQL `3307`
- MySQL `3307` 为共享进程，禁止停止。

## 3. 发布前仍需

- 独立验收 `PASS`；
- 在稳定工作区重新执行 App/Web 门禁；
- 如涉及权限语义发布，按规范取得用户确认；
- 保留 App/Web/后端回滚点。

## 4. 回滚

- 回退本切片后端、App、Web 改动；
- 不删除历史员工、成员日志或 Team 历史数据；
- 不恢复 TeamLeader；
- 已执行的本地验收数据仅删除 `decoboon_accept`，不动共享 MySQL 其他数据库。

## 5. EMG-V2.2 邀请生产闭环发布（2026-10-27）

- 状态：`PRODUCTION_DEPLOYED / REAL_USER_ACCEPTANCE_PENDING`
- 后端 release：`/opt/decoboon/releases/20261027-161846`
- 后端源码提交：`da01a13acfa66fb303e5bb8b037283af4af830c0`
- 后端包 SHA-256：`17df89d07d71fd54328dc6ea98c94376353d80bdf87beda64231c46e82975831`
- 上一后端 release：`/opt/decoboon/releases/20261027-103227`
- Web release：`/opt/decoboon-web/releases/20261027-162200`
- Web 源码提交：`d38e553`
- 上一 Web release：`/opt/decoboon-web/releases/20261026-110450`
- App 候选：`1.0.72` / commit `006d22f`，已上传开发版，等待用户设置体验版并完成正式提审发布。
- 生产健康：后端 HTTPS 200、服务 active；Web HTTP 200；生产 JAR 与 Web chunk 契约已核对。
- 发布后监控：未出现发布后新增的邀请取消空公司或邀请页主体注入异常。
- 未完成：15905600230 本人接受邀请及正确工作台复验。
