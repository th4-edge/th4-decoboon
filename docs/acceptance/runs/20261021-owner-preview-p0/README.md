# 业主视角只读预览 P0 · 开发验证证据

日期：2026-10-21
状态：`DEV_VERIFIED`（开发自测通过；不是独立验收，不代表已发布）
业务来源：`USER-DECISION-20261021-001/002`、`ANS-BIZ-20261021-001`、`T12-BIZ-20261021-001/002`

## 1. 已实现范围

- 后端新增 `project/owner-preview/info`，员工身份鉴权，复用 `CustomerJourneyContext`.
- 预览模式 `EmployeeOwnerPreview`：`actions=[]`，P0 不返回 feedback 分区，`previewMeta.readOnly=true`。
- 允许角色：BOSS、店长、团队负责人、设计师、项目经理；业务员、财务拒绝。
- 项目范围继续由 `ProjectAccessContext.requireProject` 校验企业、门店/团队/本人范围与参与关系。
- 新增 append-only 审计 `db_project_owner_preview_log`，记录成功/拒绝/未找到/异常，不写业主行为或业务状态。
- App 新增 `view:owner-preview`、`owner-preview` 路由、项目详情入口、独立 `useOwnerPreviewJourney` 和只读预览页。
- 页面显示固定只读横幅、员工姓名/工号/时间水印、只读说明与返回员工项目，不提供业主确认/驳回、反馈、分享或导出。

## 2. 后端验证

命令：

```bash
mvn test
```

结果：

```text
Tests run: 893, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖本次关键契约：

- `CustomerJourneyOwnerPreviewModeTest`：同源字段一致、actions 清空、feedback 跳过、预览元数据。
- `OwnerPreviewInfoAcceptanceTest`：5 类允许角色；业务员/财务 403；跨企业/非成员/跨店/跨团队 404；审计写入；项目/阶段/验收/付款/CustomerAccess 无业务副作用。
- `MigrationNumberingTest`、`MigrationSchemaParityTest`：迁移 123 编号唯一且 JPA/DDL 列对齐。
- `ProviderSecurityAcceptanceTest`：新 Provider 纳入保护路由基线。

证据摘要：`backend-summary.txt`、`backend-focused-tests.txt`。

## 3. App 验证

命令：

```bash
npm run verify:mp-weixin
```

结果：PASS。

包含 `type-check`、UI 规范、导航/权限守卫、本功能契约、小程序构建与分包体积检查。

```text
构建体积: 主包 946713 B, 最大分包 686311 B, 总包 3707470 B
分包清单: 主包 14 页, 分包 17 个 / 166 页
```

本功能专用契约命令：

```bash
npm run check:owner-preview
```

校验路由、5 角色白名单、业务员/财务排除、独立 Hook、只读交互、水印与项目详情入口。

证据摘要：`app-summary.txt`。

## 4. 启动与路由证据

后端使用 H2 内存库、隔离端口 `18090` 冷启动，避免触碰本地 MySQL 业务数据：

```text
Started Application in 12.864 seconds
```

启动后请求：

```bash
curl -X POST http://127.0.0.1:18090/api/project/owner-preview/info \
  -H 'content-type: application/json' -d '{"projectId":1}'
```

返回框架保护路由的未登录拒绝：

```json
{"success":false,"errorCode":{"code":"tokenIsUndefined","message":"访问受限，缺少授权标识，请先登录后再访问。","system":false}}
```

随后优雅停机。说明新 Provider 已注册、应用可启动且未放宽登录保护；该启动验证不替代携带真实员工 token 的端到端冒烟。

## 5. 未覆盖 / 待验收

- 迁移 123 尚未在 MySQL dev/test/prod 执行，未做真实库备份、执行账本与回滚演练。
- 未使用真实微信员工会话与真实项目完成 App 页面渲染、五角色按钮级走查和截图证据。
- 未做真机/微信开发者工具上的字段视觉比对、照片预览与错误态交互验收。
- 未发布后端/App，未上传开发版或体验版。
- 独立验收、发布和对外宣传仍需后续执行。
