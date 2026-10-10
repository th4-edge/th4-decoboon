# 区域实测面积/周长迁移与验证（2026-10-10）

依据：`BR-040 §4` / `DEC-062` / `EV-213` / `T12-BIZ-20261010-006`。

## 1. 迁移

脚本：`docs/design/migrations/106-区域实测面积周长.sql`
回滚：`docs/design/migrations/rollback/106-区域实测面积周长-rollback.sql`

- `db_design_space.area` / `perimeter` 改为可空（`NULL = 未录入`，不得用 0 冒充）。
- 新增 `db_design_space.area_source`（`Measured` 实测 / `Estimated` 历史估算）。
- 存量行统一标记 `Estimated`：只标记，不重算、不覆盖；估算值不再作为工程量基数。

## 2. dev 执行记录

- 目标库：`127.0.0.1:3306/decoboon`（dev）
- 执行前备份：`db_design_space-before.sql`，行数 99，SHA256 `fe10d35eb40579716eb327ac3a5ff72b426478ca66112ffca3538b4fd40254ff`（见 `backup-sha256.txt`）
- 执行后校验：
  - `information_schema`：`area/perimeter` `IS_NULLABLE=YES`；`area_source` 存在
  - `select count(*), sum(area is null), sum(perimeter is null), sum(area_source='Estimated') from db_design_space;` → `144, 0, 0, 144`
  - 账本：`db_schema_migration` → `106 / 区域实测面积周长 / SUCCESS`

## 3. 代码验证

- `mvn -o -DskipTests compile`：通过
- 定向测试（design / designtemplate / trade.quote / common.migration / customer / owner）：158 项全通过
  - `DesignSpaceSaveMeasuredTest`（区域保存实测录入、未录入不落 0、估算值不沿用、方案面积只汇总实测）5 项
  - `DesignFormulaRecalcContextTest`（实测值取数、改长宽不重算、估算值待补、全屋聚合整项待补）6 项
  - `DesignTemplateFormulaContextTest`（A/P 实测输入、L/W 禁止、未就绪不按 0）10 项
- 前端：App `npm run type-check` 通过；Web `npm run type-check` 通过

## 4. 未完成

- 真实登录的接口端到端冒烟、App 真机与 Web 浏览器页面走查、独立验收。
- test/prod 未迁移、未发布。
