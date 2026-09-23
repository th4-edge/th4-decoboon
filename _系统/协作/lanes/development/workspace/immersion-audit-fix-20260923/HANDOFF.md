# REQ-BIZ-20260923-002 沉浸式审计修复 · 研发独立复核与验证交接

- request_id: REQ-BIZ-20260923-002
- 状态: **DEV_IN_PROGRESS**（发现 1 项范围不一致 + 1 项需业务澄清）
- 验证人: development lane（本线程）
- 验证时间: 2026-09-23

## changed_files（本次复核的候选改动集，未新增修改）

后端 11 个：
```text
customer/repository/CustomerRepository.java
customer/service/cust/info/support/Info.java
design/context/DesignContext.java
design/service/schemes/info/support/Info.java
design/service/schemes/page/support/Page.java
project/context/ProjectAccessContext.java
project/repository/ProjectRepository.java
project/service/projects/info/support/Info.java
project/service/projects/page/support/Page.java
project/service/stages/page/support/Page.java
project/service/tasks/page/support/Page.java
```
App 3 个：
```text
src/domains/project/meta.ts
src/subpackages/design/pages/list.vue
src/pages/invite/accept.vue
```
> 说明：以上为交接单列出的改动，本次复核**未新增任何代码改动**（只读审查 + 跑测试）。

## 权限审查结论（问题 1：无权限扩大）

| 候选实现 | 审查结论 |
|---|---|
| `ProjectAccessContext.visibleReadableProject` / `requireReadableProject` | 业务员专用**只读** Specification：`company.comId = 当前企业` AND (`project.owner` OR `customer.owner` OR `quote.createdBy`)；非业务员回退原 `requireProject`。仅被 `projects/info`、`stages/page`、`tasks/page` 读取路径调用，**无写路径引用** ✅ |
| `DesignContext.visibleReadableDesign` / `requireReadableDesign` | 同上模式：`company` AND (`customer.owner` OR `property.salesman`)；仅被 `design/schemes/info`、`design/schemes/page` 调用 ✅ |
| `customer/cust/info` 设计师分支 | `findByCustIdAndCompany_ComIdAndDesigner_EmpId(custId, comId, empId)` 三重约束，跨企业/非指派均不可命中 ✅ |
| `ProjectAccessContext.requireMemberEditor` | **收权**：仅 Boss 或本门店 StoreManager 可切换项目成员（PM 不再可自行改派）；已接入 `projects/update`、`members/save` ✅ |
| `CustomerRepository.findTop200ByCompany_ComIdOrderByCustIdDesc` | 仅 `BusinessEventContext`（统计事件流）使用，`com_id` 约束 ✅ |
| 跨企业/跨门店/跨团队 | 所有新增 Specification 首选 `company.comId` 等值约束，未发现放宽 ✅ |

## 发现（需处理）

### F-1｜项目列表与详情对业务员的范围不一致（DEV_IN_PROGRESS 主因）
- 列表 `project/service/projects/page/support/Page.java`：`case Marketer -> findByCompany_ComIdAndOwner_EmpIdOrderByProjectIdDesc` → **仅** `project.owner`
- 详情/阶段/任务：`visibleReadableProject` = `project.owner` **OR** `customer.owner` **OR** `quote.createdBy`
- `project.owner` 由 `ProjectPermissionContext.resolveOwner` 决定 = `property.owner ?: customer.owner`
- **后果**：当 `property.owner` 与 `customer.owner` 不是同一人时，业务员作为**客户负责人**在「项目列表」看不到该项目，但用 URL 能打开「项目详情/阶段/任务」→ 列表⊂详情，验收项「业务员本人客户：项目列表可见」在该情形不成立。
- 业务裁决原文：「数据以客户负责人关系、房屋业务员或项目经营负责人快照为准」→ 列表应取同一并集。
- **建议修法**：列表改用与详情一致的 `visibleReadableProject` 规则（新增按 owner/customer.owner/quote.createdBy 并集的仓储查询或 Specification 分页），或在 `ProjectAccessContext` 暴露统一的业务员只读列表查询。

### F-2｜`requireChangeInitiator` 新增了增减项发起权限面（需业务澄清）
- 新增方法允许 `Boss / Pm / StoreManager / 项目设计师` 发起增减项，已接入 `contract-change/save`。
- 交接单未包含此项，属**权限判定变更**（非扩大越权，但改变了谁能发起增减项）。
- **需要**：业务/DEC 明确「增减项发起人」范围后再进入验收；本次不判定通过。

## tests（本机实测）

| 项 | 命令 | 结果 |
|---|---|---|
| 后端编译 | `mvn -o -q -DskipTests compile` | **PASS** |
| 定向权限测试 | `mvn -o test -Dtest='DataScopeRequestFilterIntegrationTest,ProviderSecurityAcceptanceTest,DetailResponseWhitelistContractTest,EmployeeRoleContractTest,DesignContextMultiPropertyTest'` | **15/15 PASS**（0 failures / 0 errors） |
| 后端全量 | `mvn -o test` | **304 tests / 2 failures**（见下方归因） |
| App 类型检查 | `npm run type-check` | **PASS** |
| App 构建 | `npm run build:mp-weixin` | **PASS** |

### 全量 2 个失败的归因（非本候选改动）
- 失败全部位于 `src/test/java/com/th4/edge/decoboon/statistic/BossMeasureWorkspace2AcceptanceTest.java`（`??` 未跟踪新文件），用例为「今日预约口径」：
  - `todayCount 应包含今天已预约且已完成的量房` expected 2 but was 1
  - `今天的未来预约也必须属于今日预约` expected 2 but was 1
- 其被测实现在 `src/main/java/com/th4/edge/decoboon/statistic/service/boss/measure/`（同样 `??` 未跟踪新文件）。
- 本候选改动集仅涉及 customer / design / project 三域，**不包含** statistic measure 域。
- 结论：该 2 项属**同工作树内其他在飞工作**的失败，**不由本次候选修复引入**；但工作树整体非绿，需在合并前由该在飞工作的责任方处理。

## 前端候选修复核验（问题 2/3/4）

| 项 | 结论 |
|---|---|
| `Accepting` 标签 | App `PROJECT_STATUS_META.accepting` = **待验收**；`STAGE_STATUS_META.accepting` = **待验收** ✅；全仓已无「竣工验收」用于 Accepting（仅 `tokens.ts` 一行注释残留） |
| `¥¥` 金额重复 | 全仓 `grep "¥¥"` 无残留 ✅ |
| 设计列表局部失败 | 改为 `Promise.allSettled`，逐 Provider 记录 `failures`，仅清空失败项、保留成功数据（诉求/报价/项目/房屋分别处理）✅ |
| 邀请错误态 | 缺参→`loadError='邀请参数缺失…'`；请求异常→写入错误信息；页面渲染「邀请不可用 + 原因 + 返回登录」 ✅ |

## 未完成 / BLOCKED（不得用模拟验收冒充）

| 项 | 状态 | 原因 |
|---|---|---|
| 微信开发者工具真实 Mini Program 页面回归 | **未执行** | 本机无 DevTools 交互会话与真机账号；App 侧仅完成 type-check + 构建 |
| 正向可见性端到端（业务员列表/详情/阶段/任务） | **未执行** | 需登录态与种子数据；且 F-1 未修前该链路本身不成立 |
| 反向越权端到端（改设计/成员/状态/合同/金额被拒） | **未执行** | 同上；代码层已确认写路径未接入只读 Specification |
| 幂等/多房屋/只读态禁改 | **未执行** | 同上 |
| 采购→到货→应付→付款对账复核 | **未执行** | 依赖本地运行实例与测试流水 |
| XA-20260923-005 入驻就绪引导 | 按交接口径**不要求**本轮 | — |
| XA-20260923-006 独立业主微信/真机上传 | **保持 BLOCKED** | 需用户在真机扫码；不得以模拟替代 |

## restart/deploy evidence

- **无**。本轮未启动服务、未部署、未提交。生产验证：**未做**。
- 本机仅执行编译与测试命令，无服务重启记录。

## risks

1. **F-1 列表/详情范围不一致**：业务员在 `property.owner ≠ customer.owner` 时列表看不到自己的项目，属业务可见性缺口（偏严，非越权）。
2. **F-2 增减项发起人范围变更**未经业务确认。
3. `requireMemberEditor` 收权后，**项目经理原有的成员切换能力被移除**，可能影响现有 PM 作业流程，需回归确认（本次未做端到端）。
4. 工作树混有大量未跟踪在飞改动（statistic measure、statistic context 等），全量测试非绿；**提交前必须隔离**，否则无法判定回归归属。
5. `design/schemes/page` 的 `condition.and(visibleReadableDesign)` 与 `projects/page` 的手写 switch 两套实现并存，长期有漂移风险。

## acceptance_needed

- 由 acceptance lane 或用户指定责任方**独立验证**以下（本地通过不等于验收）：
  1. F-1 修复后的业务员项目列表/详情/阶段/任务一致性（含 `property.owner ≠ customer.owner` 场景）。
  2. F-2 增减项发起人范围与业务裁决一致性。
  3. 业务员对设计/项目**写操作**全部被拒（改设计、改成员、改状态、改合同、改金额）。
  4. 跨企业/跨门店/跨团队越权返回 404/403。
  5. `Accepting` 在项目列表、项目详情、PM 工作台、经营驾驶舱四处文案一致。
  6. 邀请页无 token / 无效 / 过期三种错误态。
  7. 采购→到货→应付→付款金额对账（1470 元链路）与写入权限。

## learning_request

- 面向 `zhuangdeqing-smb-reviewer` 的学习点：
  1. **同一权限语义必须收敛到单一 Specification**：本次设计域已收敛（`visibleReadableDesign`），项目域出现"列表手写 switch / 详情 Specification"两套实现，直接导致 F-1 类不一致。建议沉淀为"只读可见性单一事实源"审查项。
  2. **经营负责人快照的取值链**（`property.owner ?: customer.owner`）必须与列表查询口径同源，否则"负责人"在列表与详情含义不同。
  3. **收权类改动需标注影响面**：`requireMemberEditor` 移除 PM 改派能力，属流程变更，应进入回归清单而非静默收权。
  4. **工作树卫生**：同树多批在飞改动会让全量测试失败无法归因，建议提交前按批次隔离。
- 建议新增/更新错误集条目：`ECD-*：只读可见性在列表与详情采用不同实现导致可见集不一致`。

## next_action

1. 修 F-1（项目列表采用与详情一致的业务员只读并集）。
2. 就 F-2 取得业务确认。
3. 由 acceptance lane 执行上述端到端矩阵与真机回归。
