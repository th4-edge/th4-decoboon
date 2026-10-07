# 设计师业务工作台联合索引迁移

> 批次：`DESIGNER-WORKBENCH-INDEX-20261008`
> 类型：`LOCAL_DEV_DDL`
> 状态：本地 dev 已应用；生产未发布
> 变更前备份：`output/migration-backups/20261008-designer-workbench-indexes-before.sql`
> 迁移脚本：`docs/design/migrations/90-设计师业务工作台聚合索引.sql`

## 1. 新增索引

| 表 | 索引 | 列顺序 |
| --- | --- | --- |
| `cust_customer` | `idx_cust_customer_company_designer` | `com_id, designer_id, cust_id` |
| `db_project` | `idx_project_company_designer_status` | `com_id, designer_id, status, project_id` |
| `db_design` | `idx_design_company_property_status` | `com_id, property_id, status, design_id` |
| `db_material_plan` | `idx_material_plan_company_project_status` | `com_id, project_id, status, plan_id` |
| `db_contract_change` | `idx_contract_change_company_project_status` | `com_id, project_id, status, change_id` |
| `db_acceptance` | `idx_acceptance_company_project_status` | `com_id, project_id, status, acceptance_id` |
| `db_project_stage` | `idx_project_stage_company_project_status` | `com_id, project_id, status, stage_id` |
| `db_project_contract` | `idx_project_contract_company_customer_status` | `com_id, customer_id, status, contract_id` |

## 2. 本地执行证据

- 执行前已导出变更前表结构备份。
- 已对 8 张表执行 `ANALYZE TABLE`。
- `information_schema.statistics` 校验 8 个索引及列顺序全部存在。
- `EXPLAIN ANALYZE` 显示：
  - `db_project` 使用 `idx_project_company_designer_status`。
  - `db_contract_change` 使用 `idx_contract_change_company_project_status`。
  - 设计派生聚合和客户范围索引已在实体元数据和数据库中登记。

## 3. 本地时间样本

以下为本地小数据 `EXPLAIN ANALYZE` 样本，仅验证执行计划无异常，不代表生产规模性能：

```text
客户 + 房屋 + 设计    约 2.7ms
项目状态             约 1.0ms
材料选材             约 0.23ms
增减项               约 0.12ms
验收/整改            约 1.0ms
```

## 4. 回滚

按 `90-设计师业务工作台聚合索引.sql` 文件末尾的注释逐条执行 `DROP INDEX`。索引变更不修改业务数据。

## 5. 未完成

- 生产备份、迁移评审和发布。
- 10 倍数据量或生产等价数据量下的执行计划与 P95 验证。
- 下一次空库基线生成时确认 8 个索引已包含。
