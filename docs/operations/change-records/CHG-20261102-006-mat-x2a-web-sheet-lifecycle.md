# CHG-20261102-006 · MAT-X2A Web 主材作业单维护全生命周期

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261102-006` |
| 类型 | 功能补强 / 跨端能力补齐 |
| 风险级别 | L2 |
| 状态 | `COMMITTED / DEV_VERIFIED / INDEPENDENT_ACCEPTANCE_PENDING` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联 | `PI-20261022-015`、`MAT-X2`、`OBJ-008`、`BR-044`、`DEC-072`、`DEC-073`、`DEC-088`、`BR-054` |
| Web 提交 | `489948c` |

## 2. 目标与范围

- 目标：补齐 Web 端主材作业单的新建、编辑、测量/时间确认、提交请购、取消和导出。
- 页面：`/workbench/material-sheets`，在 Designer、Boss、StoreManager、Pm、Finance 的统一工作台壳层中按角色显示动作。
- 数据：作业单行只从 `project/material-plan/page` 返回的项目材料计划加入；保存、更新、测量、提交、取消和导出全部复用 App 已在用的 `material/sheet/*` Provider。
- 非范围：不改后端 Provider、状态机、权限、价格/成本公式，不新增 DDL，不做到场确认、退换货、独立验收或发布。
- 财务下单继续复用既有 `/finance/material-ordering`，不在本切片复制下单逻辑。

## 3. 角色动作矩阵

| 角色 | 新建/编辑 | 测量 | 提交请购 | 取消 | 导出 | 财务下单入口 |
| --- | --- | --- | --- | --- | --- | --- |
| Designer | 可 | 可 | 可 | 可 | 可 | 无 |
| Boss | 可 | 可 | 可 | 可 | 可 | 可 |
| StoreManager | 可 | 可 | 可 | 可 | 可 | 可 |
| Pm | 不可 | 可 | 不可 | 不可 | 可 | 不可 |
| Finance | 不可 | 不可 | 不可 | 可 | 可 | 可 |

服务端仍是最终门禁；页面按钮可见性不替代 Provider 权限。

## 4. 实现要点

- 新增 `MaterialOrderSheetsView.vue`：筛选、状态摘要、作业单列表、详情、建单/编辑、测量、提交、取消和导出。
- 扩展 `materialSheetApi`：补 `save/update/measure/cancel/remove/exportFile` 和项目材料计划读取。
- 作业单 unit 统一保留 Provider 枚举值，页面单独格式化单位，避免把中文单位回传 Provider。
- 建单表单显示模板要求的测量/配送/安装字段；保存并提交时将现场时间和提交合并为一次用户动作。
- ProjectPicker 移除内部项目编号展示，并移除固定 200 条项目反推项目经理的兜底。
- 页面显示采购状态和下一步，但不展示成本、利润或内部作业单编号。

## 5. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| Web clean commit build | PASS | `489948c` 独立 worktree `npm run build` |
| 表单/概念/权限/动作/工作台/状态/页面系统门禁 | PASS | Web 仓库命令输出 |
| Designer 新建草稿 | PASS | `RUN-20261101-CORE-P0-002` 证据 `20`–`24` |
| Designer 编辑 | PASS | 备注更新后回读为“已编辑” |
| Designer 确认测量 | PASS | 测量 2026-11-05、配送 2026-11-08、安装 2026-11-10 |
| Designer 提交请购 | PASS | 状态 `Submitted / 待下单`，等待财务下单 |
| 导出 | PASS | 页面提示“作业单已导出” |
| 取消 | PASS | 原单保留为 `Cancelled`，取消原因回读 |
| API/DB 回读 | PASS | `sourceKind=Plan`、数量10、项目/计划/房屋一致；取消单无采购/应付新增 |
| Pm/Finance 越权建单 | PASS | Provider 业务 403；页面不显示新建按钮 |
| Pm 页面动作 | PASS | 页面不显示编辑/提交/取消，显示确认测量 |
| 三档渲染 | PASS（结构/溢出） | 1440×900、1280×800、1024×768 无横向溢出 |
| 视觉人工评审 | `VISUAL_REVIEW_UNAVAILABLE` | 当前会话无图像输入，不冒充视觉 PASS |
| App 真机/独立验收 | BLOCKED | 无授权微信身份，独立验收未执行 |

## 6. 发布与回滚

- 已提交到本地 Web `dev`；未 push、未部署。
- 无数据库迁移；回退 `489948c` 即可移除页面及 Web API 扩展。
- 原始 `TC-MAT-DESIGNER-SHEET-002` 因 App 未验证仍保持 `BLOCKED`，不得宣称三端全部通过。
