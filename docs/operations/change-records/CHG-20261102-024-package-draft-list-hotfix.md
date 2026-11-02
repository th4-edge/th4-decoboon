# CHG-20261102-024 · 套餐草稿保存后管理列表不显示热修

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-024 |
| 类型 | 线上缺陷修复 / 跨端契约修复 |
| 风险级别 | L2（套餐生命周期与跨端读取语义；无 DDL、金额公式和权限变更） |
| 状态 | 生产已发布，待麦鸿真实页面登录后冒烟 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户线上反馈；`BR-051`、`DEC-083`、`T12-BIZ-20261022-004`、`CHG-20261101-021` |

## 2. 目标与范围

- 目标：修复麦鸿装饰套餐保存后管理列表不显示的问题，并保持报价选择器只能选择当前发布套餐。
- 范围内：`pack/template/page` 读取语义、App 套餐管理/报价选择 API 契约、定向和全量测试。
- 非范围：套餐业务规则、金额/成本公式、角色权限、数据库结构、生产数据修改。

## 3. 业务场景与边界

1. 企业负责人新建套餐草稿并配置分区、项目行；返回套餐管理后必须立即看到草稿，并可继续编辑或发布。
2. 设计师创建套餐报价时只能选择当前已发布版本；草稿、历史版本和停用版本不得进入报价选择器。
3. 成本继续按现有角色裁剪；本次不放宽权限或数据范围。

## 4. 根因

`pack/template/page` 被报价选择器改为固定过滤 `Published + currentPublished`；App 套餐管理页又复用了同一个分页方法。新建套餐必然先保存为 `Draft`，所以保存成功但管理列表查询结果为 0。

线上麦鸿装饰修复前数据核对：6 条套餐均为 Draft，项目行合计 49，分区 6；保存数据未丢失。

## 5. 实现

| 仓库 | 分支 | 内容 |
| --- | --- | --- |
| th4-decoboon | `hotfix/package-draft-list-20261102` | `pack/template/page` 增加 `publishedOnly`；仅在报价选择模式过滤当前发布版本 |
| th4-decoboon-app | `hotfix/package-draft-list-20261102` | 套餐管理调用 `templateManagePage`（全状态）；报价选择调用 `templatePickerPage`（当前发布） |

## 6. 验收条件

| 序号 | 场景 | 预期结果 |
| --- | --- | --- |
| 1 | 新建草稿后进入套餐管理 | 草稿立即显示，可继续编辑/发布 |
| 2 | 报价选择套餐 | 只显示当前发布版本，不显示草稿/历史/停用版本 |
| 3 | 发布草稿 | 发布后管理页状态更新，报价选择器可见 |
| 4 | 数据范围 | 只读取登录员工所属企业套餐，跨租户隔离不变 |
| 5 | 成本权限 | 非 BOSS/店长/财务仍不可见成本 |

## 7. 验证

| 检查 | 命令/证据 | 结果 |
| --- | --- | --- |
| 后端定向测试 | `mvn -o -q -Dtest=PackageTemplatePageScopeTest,PackageLifecycleContextTest,PackageProviderInjectionContractTest,PackageTemplateMaintainRoleTest test` | PASS |
| Provider 响应审计 | `python3 scripts/audit-provider-response-contract.py --check` | PASS，findings=0 |
| 后端全量测试 | `mvn -o -q test` | PASS，1217/0/0/0 |
| App 套餐契约 | `npm run check:package-config -- --self-test` | PASS，9 个负向夹具被拒绝 |
| App 完整门禁 | `npm run verify:mp-weixin` | PASS（含类型、契约、构建和分包检查） |
| 生产健康 | 服务 active；`/api/auth/session/info` HTTP 200（未登录预期） | PASS |
| 生产数据回读 | 管理列表口径 6 条；报价选择器口径 0 条 | PASS |
| 真实页面复验 | 麦鸿账号登录后打开套餐管理 | 待执行（开发者工具无登录会话） |

## 8. 发布与回滚

- 发布方式：后端标准 release + App 开发版上传。
- 后端构建目录：`dist/releases/20261102-123226`
- 后端生产 release：`/opt/decoboon/releases/20261102-123251`
- 后端制品 SHA-256：`7e7d68756d3c8e3a28e535d319d98ff63abcb8d57acf56713548c15d23df3ee1`
- 上一后端 release：`/opt/decoboon/releases/20261102-113430`
- App：`1.0.81 / 181` 开发版上传成功，`upload-info-1.0.81.json` SHA-256 `1feebad09fb451c268698b111b11618dee7e22b11d9359d52cb62b81a050d160`
- DDL/数据迁移：无。
- 回滚条件：套餐管理仍看不到草稿、草稿进入报价选择器、跨租户读取、成本越权。
- 回滚方式：后端切回上一 release；App 体验版切回 `1.0.80/180`。
- 回滚验证：套餐管理空列表恢复为已知问题版本，不扩大数据影响。

## 9. 数据恢复

麦鸿现有 6 条草稿无需迁移或重建。后端与 App 同版本上线后，套餐管理页应直接显示这 6 条记录及已有项目行。
