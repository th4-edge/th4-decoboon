# 装企同频业主视角只读预览 P0 · 独立验收

> **更正（2026-10-21）**：下文“员工预览复用业主 `journey.vue`，UI 一致性 PASS”已被用户指出为错误结论。
> `journey.vue` 只是业主首页的二级页面，不是真实业主四 Tab 主界面。
> 四 Tab 复验结论见 `docs/acceptance/runs/20261021-owner-preview-four-tabs-independent/README.md`。

- 日期：2026-10-21
- 验收模式：初轮独立黑盒验收 + 后续研发修复 + 测试环境独立复验
- 后端：`dev` / `dbde1e3`
- App：`dev` / `ad3fa58`
- 首次独立验收：FAIL（证据已保留）
- 后续复验结论：**PASS（decoboon-test 测试环境）**
- 发布判断：允许进入发布准备；生产迁移与正式发布仍需单独授权

## 0. 后续复验结论

2026-10-21 修复与复验已完成：

- D-002 迁移脚本失败路径：PASS
- D-003 固定水印：PASS
- D-004 OSIV=false 项目详情主链：PASS
- D-001 `decoboon-test` 111→123 正式迁移链：PASS
- API 角色矩阵：59/59 PASS
- App 测试环境：旧 15/15 仅证明 `journey.vue` 只读预览 PASS；不能证明业主四 Tab UI 一致，已由四 Tab 专项复验替代
- 后端全量测试：894/894 PASS
- App verify：PASS
- 残余 P2：`finance/project/report`、`project/report/page` 可选投影仍有 OSIV=false 懒加载错误，不阻断预览主链

证据：`fix-verification/README.md`、`EXACT-UI-DECISION.md` 与 `migration/25-test-apply-112-123.log` 至 `migration/58-exact-ui-app-verify.log`。

## 1. 初轮环境与边界

- MySQL `8.0.24`，本地 `127.0.0.1:3306`
- 未连接生产库
- dev 全量备份后克隆到 `decoboon_owner_preview_acceptance`
- 迁移 SQL 另在 `decoboon_owner_preview_migration_smoke` 做重复执行与回滚冒烟
- 原始 `decoboon`、`decoboon-test` 未执行 121/122/123
- App 使用微信开发者工具 `2.02.2609231 RC` + `miniprogram-automator`
- App 构建通过 shell 环境变量覆盖 API 地址，未改 `.env`
- 详细 Git 边界：`git-boundary.md`

## 2. 初轮逐项结论（历史）

| 验收项 | 结论 | 说明 / 证据 |
| --- | --- | --- |
| 迁移目标确认与备份 | PASS | 全量 gzip + SHA256；`migration/README.md` |
| 正式迁移 121→123 | BLOCKED | 121 `Duplicate column 'store_id'`，账本 FAILED；`migration/02-apply-121-123.log` |
| 123 表、主键、索引、外键 | PASS | 隔离 SQL/实际克隆均验证；`migration/08-...txt` |
| 123 重复执行 | PASS | 第二次 `SKIP`；`migration/06-...log` |
| 123 回滚后重放 | PASS | 回滚表数 0，重放表数 1；`migration/09-...txt` |
| 五类角色可预览 | PASS | API 与 App 均验证 |
| 业务员、财务拒绝 | PASS | API 403，App 无入口 |
| 跨企业/店/团队/非成员 | PASS | 均匀拒绝 |
| 离职/停用员工 | PASS | 403 |
| `actions=[]` / 无 feedback | PASS | 五类角色一致 |
| 无 CustomerAccess / 业主 token | PASS | 业务表前后校验一致 |
| 无项目/阶段/任务/验收/付款副作用 | PASS | CHECKSUM、计数、关键行快照一致 |
| 成功/拒绝审计 | PASS | 最小字段完整，无浏览轨迹 |
| 预览内容与业主端同源 | PASS | 同一 `CustomerJourneyContext` + 同一个 `journey.vue`；真实业主 token 并排截图仍受安全边界限制 |
| App 入口/路由/三 Tab | PASS | 五类允许角色入口可见；三 Tab 可打开 |
| 刷新、返回员工项目 | PASS | 下拉刷新、返回栈、真实底部按钮点击均通过 |
| 空态、错误态、重试 | PASS | 空记录提示、网络错误、重试入口可见 |
| 固定水印完整文案 | FAIL | 缺“业主视角预览 · 仅内部使用”前缀；D-003 |
| OSIV=false 下入口可达 | FAIL | 项目详情 3 个依赖接口懒加载失败；D-004 |
| 后端全量测试 | PASS | 893/893 |
| App verify | PASS | 类型检查、权限/导航、构建、分包 |
| 发布前门禁 | FAIL | 存在 P0/P1 阻断 |

## 3. 缺陷

1. **D-001 / P0**：迁移链漂移，121 失败，123 未通过正式账本链执行。
2. **D-002 / P1**：迁移脚本失败分支自身出现未绑定变量异常。
3. **D-003 / P1**：预览水印缺固定前缀。
4. **D-004 / P1**：OSIV=false 时项目详情接口懒加载失败，业主视角入口不可达。

详见 `defects/D-000-summary.md` 及各项证据。

### 后续修复状态

- D-002：FIX_VERIFIED
- D-003：FIX_VERIFIED
- D-004：FIX_VERIFIED
- D-001：隔离修复链 PASS；`decoboon-test` 目标环境已修复并复验 PASS；dev 历史漂移仍保留

修复证据与测试见 `fix-verification/README.md`。`decoboon-test` 已完成重新独立复验。

## 4. 未完成 / 不再声称

- 未连接生产，未执行生产迁移。
- 未创建或复用业主 token；真实业主 UI 并排视觉比对标 `PARTIAL`。
- loading 骨架没有独立截图，仅代码/页面状态路径确认，标 `PARTIAL`。
- 未上传体验版，未发布。
- P0 功能可在测试环境标记“独立验收 PASS”；正式发布仍需生产迁移、体验版验证和用户本人完成正式版审核发布。
- D-005 P2 可选 report/finance 投影的 OSIV=false 问题待后续优化。

## 5. 发布前剩余事项

1. 生产库按 L3 流程单独备份、审计并执行 121→123（本次未连接生产）。
2. 使用测试环境构建并上传小程序体验版，完成体验版验证。
3. 正式提审与发布由用户本人在微信公众平台完成。
4. D-005 作为 P2 技术债，不阻断本 P0 主链，发布后排期修复。

结构化结果：`run.json`
