# 主材选材业务流程 开发交接

- ISSUE-ID: REQ-20260921-MATERIAL-SELECTION
- 日期: 2026-09-21
- lane: development
- 业务依据: `SCN-006`、`BR-004`、`DEC-003`（装得清业务智能体，`verified`）；未冻结项 `GAP-005`

## 输入依据

- 业务场景 `01-知识库/03-业务场景/SCN-006-签约后主材选材确认.md`
- 业务规则 `01-知识库/02-业务规则/BR-004-签约后主材选材文本化规则.md`
- 已确认决策 `01-知识库/09-已确认决策/DEC-003-签约后主材选材文本化.md`
- 待冻结项 `01-知识库/08-冲突与缺口/GAP-005-主材选材单归属与版本待冻结.md`
- 相关规划 `docs/plan/2026-09-20-历史导入到PDF归档实施路线.md`（`material_selection` 交接项）

## changed_files / output_paths

后端（新增 `material` 域）：

- `src/main/java/com/th4/edge/decoboon/material/configure/SelectionStatus.java`
- `src/main/java/com/th4/edge/decoboon/material/model/MaterialSelection.java`
- `src/main/java/com/th4/edge/decoboon/material/repository/MaterialSelectionRepository.java`
- `src/main/java/com/th4/edge/decoboon/material/context/MaterialSelectionContext.java`
- `src/main/java/com/th4/edge/decoboon/material/service/selections/{page,info,save,confirm}/**`
- `docs/design/migrations/46-主材选材记录.sql`

测试：

- `src/test/java/com/th4/edge/decoboon/material/MaterialSelectionFlowTest.java`
- `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java`（路由总数同步 + 4 条鉴权断言）

App（`../../前端代码/th4-decoboon-app`）：

- `src/domains/material/model.ts`、`src/domains/material/api.ts`（新增 `MaterialSelection` 模型与 `materialSelectionApi`）
- `src/subpackages/project/pages/selection.vue`（选材文本编辑 / 提交 / 客户确认 / 历史版本）
- `src/subpackages/project/pages/detail.vue`（项目详情新增「主材选材」入口）
- `src/config/index.ts`（新增 `projectSelection` 路由常量）
- `src/pages.json`（注册 `subpackages/project/pages/selection`）

文档：

- `docs/design/20-主材选材文本化设计.md`

## verification

- 后端 `mvn -o compile`：PASS（1024 source files）
- 后端 `MaterialSelectionFlowTest`：PASS（版本原地保存、已确认后新版本、确认幂等、无上下文拒绝、空文本拒绝、合同金额不变）
- 后端 `ProviderSecurityAcceptanceTest`：PASS，路由总数 272（public 12 + protected 260，含同期企业负责人项目经营聚合 1 条）
- App `npm run type-check`：PASS
- App `npm run build:mp-weixin`：PASS；`npm run check:subpackages`：PASS（主包 564900 B，分包 14 个 / 79 页）
- 未执行：后端全量测试、目标环境迁移执行、真实租户端到端

## 交付口径

- 四个受保护 Provider：`material/selection/page|info|save|confirm`
- 文本化保存，**不**拆逐材料颜色/规格/大小结构化字段
- 同一「企业+房屋+合同」版本递增；未确认版本原地保存，已确认版本保留并形成新版本
- 客户确认记录确认人姓名 + 操作员工 + 确认时间；重复确认幂等
- 保存与确认**不**写回报价、合同金额、成本、材料计划、采购或库存

## risks

1. `GAP-005` 三项未冻结：主归属对象、版本保留数量、客户确认方式（线上/线下/代录）。本次按临时建议实现（`Property + ProjectContract` 主归属、保留全部历史版本、确认人姓名+操作员工兼容线下代录），**未**实现采购前强制门禁。
2. 客户侧自助线上确认为独立能力，本次以员工代录兼容，不新增业主账号确认链路。
3. `ProviderSecurityAcceptanceTest` 的路由总数断言为共享文件，与并行进行的工人档案/凭证、企业负责人项目经营聚合线路同期修改；当前按实测 272 对齐（`grep -c '@Service'` 与断言一致），后续线路新增路由需再次同步。
5. 同一工作区存在并行 lane，App `src/domains/material/api.ts` 一度出现重复 `normalizeSelection`/`SELECTION_STATUS` 声明并导致小程序构建失败；已确认为单一实现后构建恢复。合并他方分支时需再次确认该文件不重复。

## next_action

1. acceptance lane 按 `docs/design/20-主材选材文本化设计.md` §9 设计独立验收（含越权、多房屋、版本变更、金额不变）。
2. 目标环境执行迁移 46 并回读。
4. 业务智能体裁决 `GAP-005` 后，再决定是否把「采购前必须有已确认选材」升级为强制门禁。

## acceptance_needed

- 后端全量测试与 `MaterialSelectionFlowTest` 独立复核
- App `verify:mp-weixin` / 分包检查 / 真机现场路径
- 迁移 46 执行与回滚
- 跨企业、跨房屋、无维护资格三类越权用例
