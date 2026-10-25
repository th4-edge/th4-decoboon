# API 与权限验收证据

## 环境

- 隔离克隆：`decoboon_owner_preview_acceptance`
- 启动：`spring.jpa.open-in-view=true`（用于完成页面走查）；另有一轮 `false` 失败证据见 defects
- 角色/项目夹具：`fixture.sql`（仅隔离库；不创建 CustomerAccess、不生成业主 token）
- 执行：`run-api-acceptance.mjs`，59/59 PASS

## 角色矩阵

| 角色 | 预览接口 | App 入口 |
| --- | --- | --- |
| BOSS | PASS | PASS |
| 店长 | PASS | PASS |
| 团队负责人 | PASS | PASS |
| 设计师 | PASS | PASS |
| 项目经理 | PASS | PASS |
| 业务员 | 403 | 无入口 |
| 财务 | 403 | 无入口 |
| 跨企业/跨店/跨团队/非成员 | 404 | 不适用 |
| 离职/停用 | 403 | 不适用 |

## 响应契约

- 五类允许角色返回字段一致；仅 `previewMeta.watermark` 随员工变化
- `actions=[]`
- 无 `feedback` 分区
- 无成本、成本价、供应商价、利润、内部备注键
- 员工 token 调业主旅程、验收确认、分享、反馈接口均 403
- 业务表校验和、关键行快照前后一致
- `cust_customer_access` 未增加
- `cust_project_share_link` 未增加

## 审计

- 成功/拒绝均有 `db_project_owner_preview_log` 记录
- 拒绝记录 `project_id` 为空、`requested_project_id` 保留被请求项目
- 字段：员工、角色、企业、项目、打开时间、来源 App、结果
- 无点击、停留、路径、正文、访问频率、业主已读列

## 证据

- `04-api-results.json`
- `05-api-summary.txt`
- `08-audit-matrix.txt`
- `backend-runtime-osiv-true.log`
- `../migration/04-api-sandbox-direct-123.txt`
