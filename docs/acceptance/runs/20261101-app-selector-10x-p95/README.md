# App 全量选择器 10×、P95 与微信开发者工具复核

> 状态：`DEV_SELF_CHECK / PERFORMANCE_DEVTOOLS_VERIFIED / PHYSICAL_DEVICE_NOT_RUN`，不是独立验收结论。
> 变更：`CHG-20261101-023`
> 日期：2026-11-01
> 业务依据：`DEC-021` / `BR-019`、`DEC-036` / `BR-031`

## 1. 范围

- 23 个 App 选择器后端加载场景。
- 固定测试租户 `BENCH-SELECTOR-10X-*` 数据段，行数达到原数据 10 倍以上或至少 200 条。
- 每场景 20 次采样、并发 5，记录 P95、首屏行数、总行数和 SQL statement 增量。
- 微信开发者工具原生小程序逐个打开 22 个选择器页面。
- 结束后按固定 Snowflake ID 区间清理。

## 2. 数据规模

| 对象 | 测试后行数 |
| --- | ---: |
| 客户 | 242 |
| 房屋 | 215 |
| 设计方案 | 212 |
| 报价 | 231 |
| 项目 | 204 |
| 员工 | 209 |
| 工人 | 200 |
| 供应商 | 201 |
| 报价库项目 | 233 |
| 内容模板 | 200 |
| 费用分类 | 200 |
| 账务分类 | 200 |
| 应收 | 213 |
| 应付 | 203 |
| 套餐 | 203 |
| 付款计划 | 201 |
| 施工计划 | 201 |
| 基础报价行 | 311 |

## 3. 结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 后端 10× / P95 / SQL | PASS，23/23 | `evidence/benchmark.json` |
| P95 最高值 | 114.67ms | 客户选择器 |
| 首屏分页 | 分页对象均 ≤20/30；房屋与账务树按有界例外 | `evidence/benchmark.json` |
| SQL 随行数增长 | 分页 Provider 10→20 行增量为 0 | `evidence/benchmark.json` |
| 项目选择器 | 约 417 SQL → 16 SQL | `evidence/before-after.json` |
| 套餐选择器 | 213 SQL → 14 SQL | `evidence/before-after.json` |
| 计划模板选择器 | 414 SQL → 13 SQL | `evidence/before-after.json` |
| 设计选择器 | 122 SQL → 19 SQL | `evidence/before-after.json` |
| 报价行选择器 | 200 行全量 → 20 行分页 | `evidence/before-after.json` |
| 微信开发者工具 | PASS，22/22 | `evidence/devtools/walkthrough.json` |
| 视觉机器预检 | PASS | `evidence/devtools/visual-precheck.md` |
| 数据清理 | PASS，固定区间全部为 0 | `evidence/cleanup-verification.json` |
| 物理手机 | `NOT_RUN_NO_CONNECTED_DEVICE` | `evidence/devtools/environment.json` |

## 4. 微信开发者工具环境

- 微信开发者工具 2.02.2609231。
- iPhone 16 原生小程序模拟器。
- 393×852，DPR 3，SDK 3.17.5。
- App 构建：`dist/dev/mp-weixin`。
- 后端：`http://192.168.3.7:8090/api`（本地 dev 后端）。
- 22 个选择器逐页截图，清单见 `evidence/devtools/walkthrough.json`。
- `CatalogPicker` 是自定义组件，automator 不穿透内部 DOM；通过截图 + `qwen2.5vl:7b` 机器视觉预检确认全屏选择层、搜索/分类和业务项目。

## 5. 未覆盖与边界

- 未连接物理 iPhone/Android，因此不得声明“物理真机通过”。
- 微信开发者工具原生小程序运行通过，不等于独立验收通过。
- 共享 dev 库存在非本批次 ID 区间的目录/账务分类并发漂移；本批次固定夹具 ID 区间均已清理为 0。
- 未提交、未上传体验版、未发布正式版。
