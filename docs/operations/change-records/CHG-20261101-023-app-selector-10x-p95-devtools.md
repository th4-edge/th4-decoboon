# CHG-20261101-023 · App 选择器 10× 数据、P95 与微信开发者工具复核

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261101-023 |
| 类型 | 性能专项/缺陷修复/开发验证 |
| 风险级别 | L2 |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/事故/业务裁决 | `DEC-021` / `BR-019`、`CHG-20261101-015`、`CHG-20261101-022`、`DEC-036` / `BR-031` |

## 2. 目标与范围

- 目标：对 App 全部业务对象选择器执行 ≥10× 数据量、HTTP P95、SQL statement 增长和微信开发者工具原生小程序复验。
- 范围内：
  - 建立 `scripts/benchmark-app-selectors-10x.py`，固定 `BENCH-SELECTOR-10X-*` 数据段，覆盖 23 个选择器 Provider 场景。
  - 修复项目、设计、套餐、报价行和计划模板选择器的全量读取与 N+1。
  - 新增项目轻量选择器 Provider `project/project/picker/page`。
  - 设计、套餐、报价行、计划模板选择器改为数据库分页并批量装配摘要。
  - 微信开发者工具逐个打开 22 个选择器页面，记录首屏业务卡片、总数和截图。
  - 使用 `miniprogram-automator` + 本地视觉预检复核 `CatalogPicker` 自定义组件边界。
- 非范围：不修改业务对象、状态机、权限角色、金额公式、成本口径和正式数据；不提交、不上传体验版、不发布。
- 受影响端：后端、App。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及读取规模、SQL 和 P95；不改变业务规则和权限结果。
- 业务事实来源与裁决编号：`DEC-021` / `BR-019`、`DEC-036` / `BR-031`。
- 尚存 GAP 或假设置：物理手机未连接，微信开发者工具原生小程序通过不等于物理真机通过；目录树和房屋选择保留有界子集例外。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 23 个选择器加载场景 | 100% 无错误，P95 ≤500ms | `benchmark.json` |
| 2 | 首屏行数 | 分页对象 ≤20/30，树/客户房屋按有界例外 | `benchmark.json` |
| 3 | SQL 增长 | 10→20 行 SQL 增量为 0 或可解释的多 Provider 组合 | `benchmark.json` |
| 4 | 项目选择器 | SQL 从约 417 降至 16，P95 ≤70ms | `benchmark.json` |
| 5 | 套餐/计划/设计/报价行 | 移除全量与逐行查询 | `before-after.json` |
| 6 | 微信开发者工具 | 22/22 选择器页面可见、路径正确、无加载失败 | `devtools/walkthrough.json` |
| 7 | 自定义组件 CatalogPicker | 原生组件边界无法深查 DOM，视觉预检确认全屏选择层和业务项目 | `catalog-vision-review.md` |
| 8 | 数据清理 | `BENCH-SELECTOR-10X` 区间全部为 0 | `cleanup-verification.json` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 未提交 | 项目 Picker Page、Plan Picker Page、Design/Package/QuoteLine Page、Repository、benchmark、证据 |
| th4-decoboon-web | dev | 不适用 | 未修改 |
| th4-decoboon-app | dev | 未提交 | 项目/设计/套餐/报价行/计划选择器分页接入、DevTools walkthrough |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 10× 后端基准 | `python benchmark-app-selectors-10x.py benchmark` | PASS，23/23 | `benchmark.json` |
| P95 | 每项 20 样本、并发 5 | PASS，最高 114.67ms | `benchmark.json` |
| SQL 增长 | size=10 对比 size=20 | 分页 Provider 增量为 0；报价库双 Provider 已单独说明 | `benchmark.json` |
| App 全量校验 | `type-check/check:selectors/audit:selectors/check:ui/check:pagination/check:client-copy/check:action/check:navigation` | PASS | 最终命令输出 |
| App 分包构建 | `npm run build:mp-weixin && npm run check:subpackages` | PASS；主包 1,075,482 B，最大分包 754,758 B，总包 4,285,979 B | `dist/build/mp-weixin` |
| 后端定向测试 | 7 个 Quote/Partner/Content/Expense/Project/Plan Picker 测试 | PASS | Maven 输出 |
| 后端编译 | `mvn -q -DskipTests compile` | PASS | 命令输出 |
| Provider 响应契约 | `audit-provider-response-contract.py --check` | PASS，0 findings | 命令输出 |
| 微信开发者工具 | iPhone 16 模拟器 393×852，SDK 3.17.5 | PASS，22/22 | `devtools/walkthrough.json` |
| 图像预检 | `qwen2.5vl:7b` | PASS（机器预检） | `visual-precheck.md` |
| 物理手机 | 无连接设备 | `NOT_RUN_NO_CONNECTED_DEVICE` | `environment.json` |
| 数据清理 | 固定 ID 区间检查 | PASS，全部 0 | `cleanup-verification.json` |
| 补丁完整性 | 后端与 App 分别执行 `git diff --check` | PASS，无输出 | 最终命令输出 |

- 未执行项及原因：未连接物理 iPhone/Android；未上传体验版或发布正式版。
- 已知失败及是否与本次相关：共享 dev 库存在非本批次 ID 区间的目录/账务分类并发漂移，不影响固定夹具全部清理。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 不适用 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：选择器数据错误、分页漏数、SQL/延迟回归、项目/计划选择或返回路径阻断。
- 回滚命令/步骤：回退本批 App 与后端代码，保留原 Provider 路由；使用上一稳定制品。
- 回滚验证：恢复选择器入口并确认企业权限、分页总数和业务保存仍正常。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否
- 返工原因：首轮基准发现项目/计划/套餐/设计/报价行存在全量或 N+1，已二次优化后全量重跑。
- 最终结论：开发自测已通过 10×/P95、SQL 增长、App 全量门禁、构建和微信开发者工具原生小程序复核；物理 iPhone/Android 与独立验收待执行，不宣称验收完成。

## 10. 复盘与沉淀

- 是否出现重复问题：是，接口名称带 page 不等于真实分页；必须用总行数、响应行数和 SQL 增长共同验证。
- 是否需要新增门禁：是，保留 benchmark 脚本作为批次门禁；后续只对新增对象扩展，不增加无关重复检查。
- 候选 `EXP/ANT/PATH`：选择器性能验收必须同时看 P95、首屏行数、SQL 随行数增长和原生小程序渲染。
- 后续动作与负责人：接入物理手机/体验版复验；独立验收；CatalogPicker 双 Provider SQL 可继续下压。
