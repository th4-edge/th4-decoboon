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

## 5. dev 端到端冒烟（2026-10-10，真实登录）

账号：设计师 董夏鹏（19900000005）；脚本：`smoke-area-measure.sh`。

| 步骤 | 结果 |
| --- | --- |
| 登录 | OK |
| 新建区域写入实测 12.04㎡ / 15.90m（快照 3.60×4.20×2.80） | 面积/周长/来源 = 12.04 / 15.9 / `Measured` |
| 只改量房快照长宽（9.99×9.99） | 面积/周长仍为 12.04 / 15.90 |
| 实测面积 0 | 拒绝：「实测面积必须大于 0（实测值由设计师按 CAD 录入）」 |
| 方案详情待补投影 | 分项 193 项、待补 **84 项**（区域未录入实测值的分项保持待补，未按 0=赠送 处理） |

Web（`http://127.0.0.1:8080/designer/design/234387416973049856`）：新增空间弹窗渲染「量房快照 长/宽/层高」+「实测面积（㎡）/实测周长（米）」+ 说明文案，未填实测值的「保存空间」保持 disabled；填入 12.05/15.91 后保存成功，空间数 9→10、方案面积同步为 12.05㎡。

### 5.1 冒烟中发现并修复的问题

1. **`design/scheme/info` 的 `pendingMeasure` 白名单加错对象**：首次脚本运行返回「待补 0 项」，定位为白名单补到了 `CatalogItem` 而非 `DesignItem`（`"formula"` 首次出现位置在前）。已修正并复测为 84 项。
2. **删除区域不重算方案面积**（既有缺陷，本次一并修复）：`design/space/delete` 原只删区域，`design.area` 残留旧值；而该字段是混合报价的面积比例基数。现改为删除后统一走 `DesignFormulaRecalcContext.recalculate`（方案面积 + 全屋聚合分项），并加 `@Transactional`。冒烟复测：删除区域后 `design.area` 由 12.05 → 0.00。
3. **枚举序列化形态**：`areaSource` 在响应中为 `{name,value,key}` 对象，App/Web 的取值改为走 `enumValue()`（原先按字符串处理会解析失败）。

### 5.2 冒烟数据清理

- 临时区域（`冒烟-区域实测`、`冒烟-UI实测`）均已删除，`db_design_space` 残留 0 行。
- 被冒烟触及的 2 个演示方案（`231734618544406528`、`234387416973049856`）`db_design.area` 已按新口径置为「实测面积合计」（当前为 0，界面显示「面积待补充」）；其余历史方案的 `design.area` 仍为迁移前的推算汇总，需迁移 107 批量对齐（待用户确认，见下）。

## 6. 已知遗留（需专项处理）

1. **历史方案的 `db_design.area` 未对齐**：迁移 106 只标记了区域口径，未重算方案面积。现存大量演示方案 `design.area` 仍是推算汇总（如 50.54），而区域均为 `Estimated` → 与「方案面积 = 实测合计」不一致，且会作为混合报价比例基数。建议新增迁移 107 统一置为实测合计（未录入即 0），影响：演示数据面积显示「待补充」、混合报价需先录入实测面积。**待用户确认后再执行。**
2. **历史中文公式阻断区域保存**：部分老分项 `formula` 为 `(人工单价) × 工程量` 等中文字面公式，`design/space/save` 触发重算时报 `TEMPLATE_VARIABLE_UNKNOWN: 未识别的专业变量：人工单价`，整个保存失败（事务回滚）。属既有缺陷，需业务口径决定「跳过重算」还是「要求修复公式」，未在本次改动范围内。
3. 实测值与快照估算的偏差提示阈值未实现；房屋级套内/建筑面积两口径未落地。

## 7. 迁移 107：方案面积对齐实测口径

脚本：`docs/design/migrations/107-方案面积对齐实测口径.sql`
回滚：`docs/design/migrations/rollback/107-方案面积对齐实测口径-rollback.sql`

- 目的：`db_design.area` 语义改为「区域实测面积合计」，避免历史推算汇总继续充当混合报价的面积比例基数。
- dev 执行前备份：`db_design-before-107.sql`，SHA256 `8e04b616…`；执行后 `db_design` 52 行面积合计 `2166.02 → 0.00`（当前无区域为实测来源），账本 `107 / SUCCESS`。
- 影响：演示方案面积显示「面积待补充」；混合报价需先录入区域实测面积。回滚只能按「全部区域合计」近似恢复旧口径，精确回滚需用备份。

## 8. GAP-042 口径落地（历史中文公式降级 + 公式待修订）

口径（用户 2026-10-10 采纳建议）：**非专业变量公式降级为「不参与重算」，保留既有工程量与金额，并标记「公式待修订」**；不阻断区域保存、不静默改金额。

- `DesignTemplateFormulaContext.isSupportedFormula`：只读判定公式是否为专业变量公式（先剥离 `__PENDING_MEASURE__|` 包装层）。
- `DesignFormulaRecalcContext.recalculate`：不可用公式跳过重算、保留原工程量与公式，并置 `formulaRevisionNeeded`；专业变量公式照常按 `TEMPLATE_SIZE_MISSING` 落待补。
- `design/scheme/info` 服务端投影 `formulaRevisionNeeded`；Web 分项表增加「公式待修订」标记与汇总计数，App 分项卡片显示「公式待修订（沿用原工程量）」。

冒烟复测（均为真实登录，脚本自建自删临时区域）：

| 方案 | 之前 | 现在 |
| --- | --- | --- |
| `228668922952417280`（6 条中文公式分项） | 区域保存直接失败（`TEMPLATE_VARIABLE_UNKNOWN`），事务回滚 | 保存成功；`待补 0 / 公式待修订 6`，工程量与金额保留原值 |
| `234387416973049856`（84 条待补分项） | 待补 84 项 | 保存成功；`待补 84 / 公式待修订 0`（待补包装层不再被误判为待修订） |

测试：后端定向 166/0/0（新增降级跳过、待补包装层不误判 2 项）；App/Web `type-check` PASS。
