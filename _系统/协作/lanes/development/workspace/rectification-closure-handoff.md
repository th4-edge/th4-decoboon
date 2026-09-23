# 阶段验收整改闭环 开发交接

- ISSUE-ID: REQ-20260921-RECTIFICATION-CLOSURE / 2026-09-22 返工批次
- lane: development
- 状态: `development_rework_done_pending_reacceptance`
- 业务依据: `SCN-010`、`BR-008`、`DEC-007`、用户 2026-09-21 四条裁决、验收报告 `DEF-RECTIFY-001..004`

## 本批修复范围

| 缺陷 | 修复结果 |
| --- | --- |
| `DEF-RECTIFY-001` 存量 Pending 无第 1 轮 | 迁移 47 增加幂等回填：每张历史 Acceptance 补建第 1 轮，复制状态、结论、问题、现场证据、整改责任人/截止日期 |
| `DEF-RECTIFY-002` 重复/并发提交不幂等 | 提交/复验按阶段行级排他锁串行化；已有 Pending 时直接返回已有验收事实，不新增轮次、不返回未定义错误 |
| `DEF-RECTIFY-003` 驳回缺问题/责任人/截止日期 | `customer/acceptance/update` 新增责任人和截止日期入参；服务端强制校验问题清单、项目团队责任人和截止日期 |
| `DEF-RECTIFY-004` App 业主驳回交互不完整 | App 业主驳回改为完整表单：问题清单、项目团队责任人、整改截止日期、取消/提交动作 |

## changed_files / output_paths

后端：

- `src/main/java/com/th4/edge/decoboon/project/context/ProjectContext.java`
  - `submitAcceptance` 使用 `ProjectStageRepository.findLockedByStageId`；
  - Pending 重复提交返回已有 Acceptance；
  - 轮次号改为基于最新轮次递增；
  - `confirmAcceptance` 驳回时强制校验并同时写入 Acceptance + 当前 AcceptanceRound。
- `src/main/java/com/th4/edge/decoboon/customer/service/acceptance/update/support/Update.java`
  - 新增 `rectificationOwnerId`、`rectificationDueDate`；
  - 责任人限当前企业且必须是当前项目团队人员；
  - 确认/驳回统一按“阶段 -> 验收单”锁顺序处理。
- `src/main/java/com/th4/edge/decoboon/customer/service/acceptance/info/support/Info.java`
  - 业主验收详情返回 `submittedBy`、项目经理、设计师、履约专员的最小人员白名单，供驳回选择责任人。
- `src/main/java/com/th4/edge/decoboon/project/repository/AcceptanceRepository.java`
  - 业主读取查询增加 EntityGraph 预取，避免 OSIV 关闭时人员候选懒加载；
  - 新增 `findFirstForCustomerByStage`。
- `src/main/java/com/th4/edge/decoboon/project/service/acceptances/{info,save,update}/support/*.java`
  - 验收响应补充项目团队人员最小字段。
- `docs/design/migrations/47-阶段验收整改轮次.sql`
  - 新增第 1 轮存量回填、问题集合回填、现场证据集合回填；重复执行幂等。

App：

- `src/domains/acceptance/api.ts`
  - `confirm` 支持问题清单、整改责任人、截止日期；
  - 业主验收详情解析项目团队候选人。
- `src/domains/acceptance/model.ts`
  - `Acceptance` 增加 `rectificationCandidates`。
- `src/subpackages/project/pages/accept/edit.vue`
  - 业主驳回表单：问题清单、责任人选择、截止日期选择；
  - 前端阻止缺项提交；
  - 提交验收表单移除员工侧“需整改项”，问题由业主驳回时产生。

## verification

- 后端 `mvn -o -q -DskipTests compile`：PASS。
- 后端回归（排除暂未升级的 `OwnerClosureAcceptanceTest`）：75 suites / 215 tests / 0 failures / 0 errors / 0 skipped。
- `ProviderSecurityAcceptanceTest`：PASS，路由仍为 277（public 12 + protected 265）。
- `OwnerClosureAcceptanceTest` 只读与业主确认幂等定向用例：PASS。
- `OwnerClosureAcceptanceTest` 全套旧契约：15 tests 中 3 个报错，均为旧 helper 未传驳回责任人/截止日期或未传问题清单，符合本批服务端收紧预期；该测试归 acceptance lane 更新，development 未修改测试。
- App `npm run type-check`：PASS。
- App `npm run build:mp-weixin`：PASS。
- App `npm run check:subpackages`：PASS；主包 596717 B，最大分包 302511 B，总包 2014017 B，14 分包 / 86 页。

## risks / blockers

1. 迁移 47 尚未在目标等价 MySQL 执行；本轮本机 MySQL 未运行，未做真实回填预演。
2. 并发修复已通过阶段行级锁实现，但尚未在 MySQL 8 做生产等价并发/故障注入。
3. 业主 App 真机表单与微信授权会话仍未执行，不能由 type-check/build 代替。
4. 驳回问题、责任人和截止日期现在由业主同屏填写；这是本次为保证驳回后立即形成可执行整改事实的实现口径。
5. 旧 `OwnerClosureAcceptanceTest` 是本批唯一已知红灯，原因是测试仍使用旧契约，不应通过放宽生产校验解决。

## next_action

1. acceptance 更新 `OwnerClosureAcceptanceTest` 驳回 helper，并复验：
   - 缺问题清单 → 400；
   - 缺责任人/截止日期 → 400；
   - 顺序重复 Pending → 返回已有轮次且不新增；
   - 并发首次/复验 → 同一 Acceptance/轮次，无 `undefinedErrorCode`；
   - 存量 Acceptance 回填后第 1 轮完整。
2. 在等价 MySQL 执行迁移 47，先盘点存量、预演回填、重复执行和失败回滚。
3. 真机完成“提交 → 驳回填写完整整改要求 → 复验 → 确认”路径。

## acceptance_needed

- 第 1 轮提交 → 驳回 → 第 2 轮复验 → 通过；历史轮次不变。
- 连续驳回 3 次后第 4 轮通过，`roundNo` 不跳号。
- 已通过后再次提交被拒绝。
- 员工代业主确认/驳回 → 403。
- 跨企业 / 非参与人读取轮次 → 404。
- 重复和并发提交稳定幂等，只保留一个 Pending 轮次。
- 缺问题、责任人、截止日期时驳回被拒绝。
- 整改前后合同金额、成本、应收应付、结算和利润不变。
