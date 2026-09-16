# saas 数据同步设计说明

## 1. 目标

把 `saas` 数据库中与 `th4-bim` 对应的业务数据同步到本系统 `decoboon` 数据库。

本次同步范围是 BIM 主数据和套餐数据，不包含空的 ERP、OA 业务表。

## 2. 同步方向

```text
saas.bim_*  →  decoboon.bim_*
```

原则：

- 源库只读。
- 表名保持 `bim_*`，保证与 `th4-bim` 实体和 JPA 映射一致。
- 主键保持不变，便于关联表和历史数据稳定迁移。
- 批次同步使用 `REPLACE INTO` 或先导入空库的方式，最终以目标库数据校验为准。
- 同步脚本不删除 `db_tenant_*` 及其他非 BIM 表。

## 3. 同步表清单

### 施工

- `bim_construct_modular`
- `bim_construct_modular_criterions`
- `bim_construct_modular_tips`
- `bim_construct_construction`
- `bim_construct_construction_criterions`
- `bim_construct_construction_images`
- `bim_construct_construction_space_types`
- `bim_construct_construction_tips`
- `bim_construct_construction_upgrades`

### 材料

- `bim_material_category`
- `bim_material_category_criterions`
- `bim_material_category_tips`
- `bim_material_brand`
- `bim_material_brand_criterions`
- `bim_material_brand_tips`
- `bim_material_series`
- `bim_material_series_criterions`
- `bim_material_series_spec`
- `bim_material_series_tips`
- `bim_material_material`
- `bim_material_material_criterions`
- `bim_material_material_images`
- `bim_material_material_space_types`
- `bim_material_material_styles`
- `bim_material_material_tips`
- `bim_material_material_upgrades`

### 套餐

- `bim_pack_gift_group`
- `bim_pack_gift_group_space_types`
- `bim_pack_gift_item`
- `bim_pack_gift_item_constructions`
- `bim_pack_gift_item_criterions`
- `bim_pack_gift_item_materials`
- `bim_pack_gift_pack`
- `bim_pack_gift_pack_criterions`
- `bim_pack_gift_pack_item`
- `bim_pack_gift_pack_items`
- `bim_pack_gift_pack_measures`
- `bim_pack_gift_pck_item_space_types`

## 4. 首次同步

```bash
bash scripts/sync-saas-bim.sh
```

默认参数可通过环境变量覆盖：

```bash
SOURCE_HOST=127.0.0.1
SOURCE_PORT=3306
SOURCE_DB=saas
TARGET_HOST=127.0.0.1
TARGET_PORT=3306
TARGET_DB=decoboon
MYSQL_USER=root
MYSQL_PASSWORD=mysqladmin
```

## 5. 同步后校验

1. 主表行数。
2. 外键关联表行数。
3. `construction.mod_id` 是否都指向有效 Modular。
4. `material.cat_id/brand_id/ser_id` 是否都有效。
5. `gift_pack_items.gift_id/item_id` 是否有效。
6. `gift_item_constructions.const_id` 和 `gift_item_materials.mat_id` 是否有效。
7. 对两个套餐重新执行成本计算，结果应可重复且不产生未解释差异。

## 6. 可重复执行策略

脚本以 `saas` 为 BIM 权威源，对目标库 `bim_*` 表执行可重复重建和租户复制：

- 仅删除并重建 38 张 `bim_*` 表，不删除任何非 BIM 表。
- 重建时同步导入源表结构和数据。
- 将 `saas.oa_company` 同步为 `db_tenant_company`。
- 基准企业保留原主键，其他企业使用独立主键段并重映射全部关联。
- 同步完成后恢复父实体 `com_id NOT NULL` 约束。

> 注意：同步是“以 saas 为白名单主数据源覆盖目标 BIM 表”，如果目标系统允许企业自行修改 BIM 数据，后续应改为暂存表 + 差异合并，并保留企业级覆盖层。

## 7. 本次同步结果

同步执行日期：2026-09-11。

| 表 | 每租户行数 | 企业数 | 校验 |
| --- | ---: | ---: | --- |
| `bim_construct_modular` | 11 | 3 | ✅ |
| `bim_construct_construction` | 182 | 3 | ✅ |
| `bim_material_category` | 21 | 3 | ✅ |
| `bim_material_brand` | 48 | 3 | ✅ |
| `bim_material_series` | 58 | 3 | ✅ |
| `bim_material_material` | 317 | 3 | ✅ |
| `bim_pack_gift_group` | 8 | 3 | ✅ |
| `bim_pack_gift_item` | 58 | 3 | ✅ |
| `bim_pack_gift_pack` | 2 | 3 | ✅ |
| `bim_pack_gift_pack_item` | 4 | 3 | ✅ |

企业来源为 `saas.oa_company`，当前同步企业：`1 包河天蚁`、`2 昭青网络`、`3 昭青科技`。每个企业的 BIM 主数据拥有独立主键段，企业 1 保留源主键，企业 2/3 分别使用 `企业ID × 1,000,000` 偏移。

外键抽样校验：

- 施工项到工程分类孤儿记录：0。
- 材料到分类、品牌、系列孤儿记录：0。
- 套餐分项到施工项孤儿记录：0。
- 套餐分项到材料孤儿记录：0。
- 套餐到套餐分项孤儿记录：0。

## 8. 首次闭环验证

已增加集成测试：

```bash
mvn -Dtest=DecoboonClosedLoopTest test
```

覆盖链路：

```text
客户
→ Quote 套餐报价
→ 提交/客户接受
→ 生成项目
→ 生成阶段/施工任务/材料计划
→ 施工任务完成
→ 项目进度 100%
→ 提交阶段验收
→ 客户确认
```

测试使用 `@Transactional`，执行结束后回滚测试企业、客户、报价、项目和验收数据。
