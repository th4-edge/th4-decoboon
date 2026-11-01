# 接管后剩余施工计划 P0 开发口径

日期：2026-09-26
状态：ready_for_implementation
依据：DEC-011、SCN-011、BR-010、SOL-005

## 场景一：项目经理接管中途工地

- 使用人：项目经理，BOSS 代确认。
- 触发事件：项目已完成承接，当前阶段还剩水电收尾，后续还有泥瓦、木作、安装三个阶段。
- 当前动作：进入“接管后剩余施工计划”，确认接管点，新增三个后续阶段，设置计划起止日期。
- 系统下一步：保留接管基线阶段，新增未来阶段；确认后回写计划状态和版本；任务排期继续复用现有阶段 × 区域 × 工艺入口。
- 异常处理：负责人未建立时 BOSS 代确认并记录应确认角色、实际确认人、版本和时间。

## 场景二：接管后修改未开工阶段

- 使用人：项目经理。
- 触发事件：木作阶段尚未开工，因为材料到场推迟需要顺延七天。
- 当前动作：调整木作阶段计划完工日期并填写原因。
- 系统下一步：若项目尚未发生首笔接管后业务，计划回到“待重新确认”；若已锁定，保留锁定状态并写入计划调整留痕，不删除已开工阶段。
- 异常处理：阶段已有实际开工、任务进展或验收时，服务端拒绝直接删除或改写，要求走调整或后续业务处理。

## 可完成状态投影

| key | label | required | 完成条件 | 异常态 |
| --- | --- | --- | --- | --- |
| remaining-plan | 接管后施工计划 | 是 | 正式项目存在，且当前 planVersion 有确认记录，planStatus=Confirmed/Locked | 暂不可确认 / 待重新确认 / 已锁定 |

## 权限

- 查看：项目可见人。
- 新增、修改、排序、删除、排期：BOSS、项目经理、项目设计师（沿用 requirePlanEditor）。
- 计划确认：项目经理或 BOSS；PM 未建立时 BOSS 代确认留痕。
- 首笔业务后：禁止整批删除重建，仅允许未来未开工阶段调整并填写原因。

## 进度保护

- 项目接管进度与当前接管阶段进度作为不可覆盖基线。
- 当前阶段有效进度 = 基线进度 + (100 - 基线进度) × 接管后剩余任务完成率。
- 项目有效进度 = 项目基线进度 + (100 - 项目基线进度) × 剩余计划加权完成率。
- 新增剩余任务后，进度不得低于接管时进度。

## P0 边界

- 不重建历史阶段、任务、验收、日报和资金流水。
- 不自动改变合同金额、应收、应付。
- 不提供批次导入、OCR 或历史 PDF 解析。
- 任务排期继续复用现有计划编辑入口。

## Development Handoff

### 输入依据

- `DEC-011`、`SCN-011`、`BR-010`、`SOL-005`
- 用户本轮 6 项要求：未来阶段增改排序删除、进度保护、计划确认与调整留痕、App 引导页、业务承接返回交互、列表卡片优化。

### Changed Files

- 后端：`src/main/java/com/th4/edge/decoboon/carryover/**`
- 后端：`src/main/java/com/th4/edge/decoboon/project/model/{Project,ProjectStage}.java`
- 后端：`src/main/java/com/th4/edge/decoboon/project/context/ProjectContext.java`
- 迁移：`docs/design/migrations/62-接管后剩余施工计划.sql`
- 发布：`scripts/build-release.sh` 迁移范围 `24..62`
- 测试：`CarryoverPlanContextTest`、`ProjectCarryoverProgressTest`、`ProviderSecurityAcceptanceTest`
- App：`src/domains/carryover/{model,api}.ts`、`src/subpackages/carryover/pages/{index,edit,plan}.vue`
- App：`src/subpackages/project/pages/plan.vue`、`src/pages.json`、`src/config/index.ts`
- 契约：`scripts/verify-plan-contract.mjs`

### Verification

- 后端全量 `mvn -o -q test` PASS。
- App `npm run verify:mp-weixin` PASS（110 页，分包检查通过）。
- 迁移 62 已在本地 `decoboon` 执行；已有 Carryover 项目 `baseline_progress` 回填成功。
- 微信开发者工具真实 BOSS 会话：业务承接列表显示返回按钮和优化卡片；“去制定”进入四步引导；新增“泥瓦施工”后项目进度保持 40%；确认计划后生成确认留痕；已锁定项目调整未开工阶段时强制填写原因，版本从 v1 升至 v2，项目进度仍为 40%；“排施工任务”跳转现有 `project/pages/plan-edit?id=...`。
- 数据库复核：`carryover_project.plan_status=Locked`、`plan_version=2`、项目/基线阶段进度均为 40；存在一条计划确认记录和阶段新增/调整留痕。

### Risks

- P0 仍是项目管理制，未来阶段尚未自动复制为企业级计划版本。
- 当前阶段未排任务时，项目进度会停留在接管基线，直到剩余任务被排入并推进，这是有意的防高估保护。
- 已锁定项目的调整仅允许未来未开工阶段；已开工、已验收或已有任务进展仍会拒绝直接改删，需后续业务处理。

### Next Action

- acceptance 独立复验微信页面：返回、卡片、阶段增改排序删除、确认留痕、锁定调整和任务排期跳转。
- 如业务需要，再做“接管后计划模板化复制”和“计划版本快照”增强。

### Acceptance Needed

- 真实 BOSS 账号端到端：完成、驳回或取消各关键交互均可解释，错误态可见且可恢复。
- 目标环境迁移 62 执行前完成结构审计和备份。
