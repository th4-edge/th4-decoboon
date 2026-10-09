# 主材选材（DEC-056）本地 dev 接口冒烟

- 日期：2026-10-09
- 环境：本地 dev 后端 `http://127.0.0.1:8090/api`（Java 23 + 本地 MySQL `decoboon`），麦鸿设计测试企业
- 账号：财务 19900000005 / 设计师 19900000003 / 项目经理 19900000006（密码 123456）
- 脚本：`smoke.py`；原始证据：`smoke.json`

## 结果

| 步骤 | 结果 |
| --- | --- |
| `auth/session/password-login`（finance/designer/pm） | PASS |
| `project/project/page` 发现项目 | PASS（11） |
| `material/selection/item/page` | PASS |
| `material/selection/save`（不交付，designer） | PASS → status=Selected「已选材」，orderStatus=none |
| `material/selection/item/info` 回读 | PASS（selectionText 一致） |
| `file/private/grant` 上传（businessType=MaterialSelection，多图） | PASS（path 落 projects/.../materialselection/...） |
| `material/selection/save`（带图 + deliver=true） | PASS → orderStatus=ready「待财务下单」，attachments 回读 1 张 |
| `file/private/grant` 图片回读授权 | PASS（urlPresent=true） |
| `project/material-plan/page` | PASS |
| 材料计划落库 | PASS：`plan_id=233710289614592`(实际 233710289621614592) status=`ReadyToOrder`，selection_id=新选材 |
| 权限反向：Pm 保存 | PASS（被拒 404「房屋不存在或无权访问」） |
| `material/selection/confirm` 路由 | PASS（已移除：canNotFoundHandler） |

## 发现的缺陷与修复

1. **P0（已修）** `MaterialSelectionRepository` 的 `@EntityGraph` 仍引用已删除属性 `confirmedBy` → `material/selection/item/page` 报 `Unable to locate Attribute [confirmedBy]`。已移除该属性引用（repository + page/info 白名单），重新打包后修复。
2. **P0（已处理）** 本地库 `db_material_selection.status` 实际是 `enum('Confirmed','Draft','PendingConfirm')`（与迁移 46 的 `varchar(24)` 漂移），写入 `Selected` 报 `Data truncated`；且存在 13 条旧状态存量记录（Draft 3 / PendingConfirm 1 / Confirmed 9）。
   - 按用户裁决「目前不存在，如果存在就删除重做」处理：备份 → `alter table ... modify status varchar(24) not null` → 解除 9 条材料计划的外键引用 → 删除旧选材与附件。
   - 备份：`backup-db_material_selection-before.sql`（13 行）、`backup-material-plan-legacy-links.txt`（9 条引用）。
   - 迁移脚本：`docs/design/migrations/97-主材选材现场代录状态.sql`。

## 环境说明

- 冒烟期间有**另一个 Codex 线程**并发在本仓库执行 `mvn test` 并重启 8090 后端，导致后端多次被替换/中断；主链步骤已在后端稳定窗口内跑通并落库。
- 未做：真机 / 微信开发者工具走查、生产发布。
