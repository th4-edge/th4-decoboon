# CHG-20261102-026 · 套餐草稿删除热修

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-026 |
| 类型 | 线上缺陷修复 / 数据删除能力 |
| 风险级别 | L3（新增物理删除能力，涉及套餐主表、项目行及业务引用保护） |
| 状态 | 开发与全量测试通过，待发布 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户 2026-11-02 明确指令；`BR-051`、`DEC-083`、`T12-BIZ-20261022-004` |

## 2. 目标与范围

- 目标：仅允许 BOSS/店长删除企业内最新、无业务引用的套餐草稿。
- 范围内：`pack/template/delete`、套餐生命周期删除守卫、App 套餐管理删除入口、引用检查与级联删除测试。
- 非范围：删除已发布/停用版本、删除历史版本、修改金额/成本公式、调整角色数据范围、数据库结构变更。

## 3. 业务规则

1. 只有 `Draft` 状态套餐可删除。
2. 只能删除同企业、同套餐编码下的最新版本。
3. 套餐已被报价、报价行、材料计划或项目任务引用时，返回 409，禁止删除。
4. 草稿删除时级联清理项目行、项目行说明、套餐说明和套餐差项。
5. 已发布/停用版本永久保留，不能物理删除；停用只阻止新报价。
6. 非 BOSS/店长不可调用删除接口；跨企业 ID 返回 404。
7. 删除为不可恢复动作，App 必须二次危险确认。

## 4. 实现

| 仓库 | 分支 | 内容 |
| --- | --- | --- |
| th4-decoboon | `hotfix/package-draft-delete-20261102` | 新增 `pack/template/delete`；生命周期锁、最新草稿校验、引用检查、级联删除 |
| th4-decoboon-app | `hotfix/package-draft-delete-20261102` | 最新草稿显示“删除草稿”；危险确认；删除后重读列表；版本 `1.0.82/182` |

## 5. 引用保护

删除前统一检查以下生产外键引用：

- `quote.package_template_id`
- `quote_line.package_line_id`
- `db_material_plan.package_line_id`
- `db_project_task.package_line_id`

只要存在任一引用即拒绝删除。

## 6. 验证

| 检查 | 命令/证据 | 结果 |
| --- | --- | --- |
| 生命周期定向测试 | `PackageLifecycleContextTest` | PASS：未引用草稿删除、已发布拒绝、有引用拒绝、跨企业拒绝 |
| 级联删除集成测试 | `PackageDraftDeleteIntegrationTest` | PASS：草稿和项目行、行说明同时删除，无孤儿数据 |
| 路由鉴权 | `ProviderSecurityAcceptanceTest` | PASS：`pack/template/delete` 未登录必须鉴权 |
| Provider 响应审计 | `audit-provider-response-contract.py --check` | PASS，findings=0 |
| 后端全量测试 | `mvn -o -q clean test` | PASS，1222/0/0/0 |
| App 套餐契约 | `check:package-config -- --self-test` | PASS，10 个负向夹具被拒绝 |
| App 完整门禁 | `npm run verify:mp-weixin` | PASS |
| 真实页面 | 麦鸿账号登录后删除测试草稿 | 待发布后执行 |

## 7. 生产数据保护

- 发布前备份目录：`/opt/backups/th4-decoboon/pre-package-draft-delete-20261102-124700`
- 备份文件：`db.sql.gz`
- 备份 SHA-256：`44d6aa4e8a8f6e5223ef926753fa156a192820816905edc554e5198df7665184`
- 备份完整性：`gzip -t` PASS
- 本次发布不自动删除任何现有草稿；删除只由用户在 App 明确确认后触发。

## 8. 发布与回滚

- 发布方式：后端标准 release + App 开发版上传。
- DDL/数据迁移：无。
- 回滚条件：已发布套餐可删除、有引用草稿可删除、跨企业可删除、删除后残留孤儿行、非维护角色可删除。
- 回滚方式：后端切回上一 release；App 体验版切回 `1.0.81/181`。
- 回滚后验证：套餐管理可读、已发布套餐与历史报价不变、数据库备份可恢复。

## 9. 后续真实验证

使用麦鸿账号在 `1.0.82/182` 体验版中：

1. 对一个无引用草稿执行删除并确认列表消失。
2. 对已有报价/项目引用的套餐确认后端拒绝。
3. 确认已发布和历史版本仍只读保留。
