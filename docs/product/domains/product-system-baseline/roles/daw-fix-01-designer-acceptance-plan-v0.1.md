# DAW-FIX-01 · 设计师阶段验收提交对齐计划 v0.1

> 日期：2026-10-23  
> 状态：`DEV_VERIFIED_MINIMAL / MODE_GATE_PENDING`  
> 所属目标：Designer App/Web 全流程对齐  
> 唯一范围：用户已确认“设计师允许提交阶段验收”  
> 不在本批：报价/签约队列拆分、报价历史、设计模板 App、其他角色、DDL、金额/成本、验收确认主体变更。

## 1. 目标与结论

目标：

```text
Designer 在 App/Web 都能对本人参与项目的 AcceptanceDone 阶段提交首验和复验；
两端使用同一验收事实、同一状态、同一证据与幂等规则；
Designer 始终不能确认/驳回业主验收。
```

不改变：

- 业主仍是 `AcceptanceDone` 的唯一确认/驳回主体；
- 合同签约、生效、金额、成本、应收、应付、结算不受影响；
- 阶段验收仍复用同一阶段、同一张主验收单和 append-only 轮次；
- 不新增 Provider，不新增请求/响应实体。

## 2. 当前事实与根因

| 层 | 当前事实 | 影响 |
| --- | --- | --- |
| App 能力键 | Designer 无 `act:acceptRequest` | 验收页路由被守卫阻断，阶段详情不显示提交入口 |
| App 验收页 | 已支持非业主提交、复验、证据、轮次；`canSubmit` 依赖 `act:acceptRequest` | 页面主体可复用，不需要重做 |
| Web | Designer 路由已包含验收工作区，页面可提交 | Web 比 App 放行更早，形成跨端差异 |
| 后端 Provider | `project/acceptance/save` 已允许 Boss/Pm/Designer | 后端已是“设计师可提交”现状 |
| 后端阶段模式 | `ProjectStage.acceptanceMode` 已存在，但提交验收链路尚未统一校验模式 | 不能让跨端对齐掩盖验收模式缺失；需列为发布门禁 |

根因不是页面缺失，而是 App 能力矩阵没有跟随业务口径更新；同时 App/Web 没有统一消费“当前阶段是否允许提交验收”的服务端投影。

## 3. 权限矩阵

| 主体 | 查看项目 | 提交首验 | 提交复验 | 确认验收 | 驳回验收 |
| --- | --- | --- | --- | --- | --- |
| Designer（项目参与人） | 是 | 是（AcceptanceDone） | 是（AcceptanceDone） | 否 | 否 |
| Pm（项目参与人） | 是 | 是（AcceptanceDone） | 是（AcceptanceDone） | 否 | 否 |
| Boss | 是 | 是（AcceptanceDone，兜底） | 是 | 否（业主专属） | 否（业主专属） |
| StoreManager | 按现有 Provider 范围；本批不改变 | 本批不改变 | 本批不改变 | 否 | 否 |
| Finance/Marketer/TeamLeader | 不因本批扩大 | 否 | 否 | 否 | 否 |
| 业主本人 | 本人授权项目 | 否 | 否 | 是 | 是 |

关键原则：

- Designer 只获得 `act:acceptRequest`，不得获得 `act:acceptConfirm`。
- `act:acceptConfirm` 仍为外部主体专属能力，不进入任何内部角色超集。
- 前端能力键只控制入口；最终以服务端角色、项目参与关系、阶段模式、任务和验收单状态为准。

## 4. 阶段模式门禁

| acceptanceMode | 完成路径 | 是否允许员工提交业主验收 | 本批处理 |
| --- | --- | --- | --- |
| `None` | 有效任务完成后系统完成 | 否 | 后端阻断；前端显示“该阶段无需验收” |
| `ConstructionDone` | 内部确认施工完成 | 否（不是业主验收） | 后端阻断 `acceptance/save`；内部确认动作另行专项 |
| `AcceptanceDone` | 员工提交，业主确认/驳回 | 是 | 本批主路径 |

历史兼容：

- `acceptanceMode` 为空的历史阶段显示“验收方式待核验”，不得自动当作 `AcceptanceDone`。
- 若生产存量存在空值，需要先做只读数据分布检查和业务处置方案；不得静默回填后放量。
- 本批不创建 DDL/迁移。若数据无法区分，App 只能展示“待核验”，不能开放提交按钮。

## 5. 动作与状态流

```text
Todo
→ 任务全部完成
→ 员工提交首验（AcceptanceDone）
→ Stage=Accepting, Project=Accepting, Acceptance=Pending
→ 业主确认
   → Stage=Done, Project 状态重新推导
→ 业主驳回
   → Acceptance=Rejected
   → 项目团队整改
   → 同一员工角色可提交复验
   → 同一 Acceptance 追加 Round
```

幂等：

- 已有 Pending 验收时重复提交返回同一验收事实，不新增轮次；
- 已 Confirmed 时不得再提交；
- 双击、并发和网络重试不得形成第二张主验收单。

## 6. 实施级改动清单

### 6.1 App 权限与入口

文件：`th4-decoboon-app/src/roles/permission.ts`

- Designer 能力矩阵增加 `act:acceptRequest`；
- 不增加 `act:acceptConfirm`；
- 保留 `EXTERNAL_ONLY_CAPABILITIES = ['act:acceptConfirm']`。

文件：`th4-decoboon-app/src/roles/guard.ts`

- `/subpackages/project/pages/accept/edit` 现有 `act:acceptRequest | act:acceptConfirm` 规则不变；
- 增加权限矩阵回归用例，确认 Designer 放行、Marketer/Finance 仍拒绝。

文件：`th4-decoboon-app/src/subpackages/project/pages/detail.vue`

- 将“阶段验收提交”从施工阶段开始权限中拆开；
- 新增 `canSubmitAcceptance = can(role, 'act:acceptRequest') && stage.acceptanceMode === 'AcceptanceDone'`；
- 顶部待验收横幅仅在可进入验收详情时展示可执行动作；无权限时只显示状态，不显示可点击伪装；
- 施工开始继续使用 `act:startStage`，不得混用。

文件：`th4-decoboon-app/src/subpackages/project/pages/stage/detail.vue`

- 保持 `canAcceptRequest` 使用 `act:acceptRequest`；
- 增加 `acceptanceMode === 'AcceptanceDone'` 条件；
- `None` 显示“无需验收”；`ConstructionDone` 显示“施工完成确认由内部处理”；空值显示“验收方式待核验”。

文件：`th4-decoboon-app/src/subpackages/project/pages/task/detail.vue`

- 只有 `act:acceptRequest` 且当前阶段 `AcceptanceDone` 时，允许进入“提交/查看验收单”；否则只显示状态文案。

### 6.2 Web 页面门禁

文件：`th4-decoboon-web/src/views/designer/DesignerAcceptanceView.vue`

- `canSubmit` 增加 `selectedStage.acceptanceMode === 'AcceptanceDone'`；
- 增加 Designer/Pm/Boss 的角色能力判断，页面不依赖静态路由作为唯一授权；
- `None`、`ConstructionDone`、空模式给出明确只读原因；
- 保留业主确认不可代录文案。

文件：`th4-decoboon-web/src/views/workbench/ProjectExecutionView.vue`

- 若展示阶段验收入口，必须携带 `stageId` 并遵循同一个 `acceptanceMode` 和权限判断；
- 不在通用项目执行页复制第二套验收状态机。

### 6.3 后端总门禁

文件：`th4-decoboon/src/main/java/com/th4/edge/decoboon/project/service/acceptances/save/support/Save.java`

- 保留服务端角色校验：Boss/Pm/Designer；
- 将 Designer 放入显式允许集合，禁止依赖前端角色；
- 调用 `ProjectContext.submitAcceptance` 前校验：
  - 项目参与关系；
  - `stage.acceptanceMode = AcceptanceDone`；
  - 当前阶段不允许 `NotApplicable`；
  - 若已有 Pending/Confirmed，按既有幂等/阻断规则处理。

文件：`th4-decoboon/src/main/java/com/th4/edge/decoboon/project/context/ProjectContext.java`

- 在 `submitAcceptance` 内统一校验 `acceptanceMode`，作为所有入口的总门禁；
- `None`：返回业务错误“该阶段无需业主验收”；
- `ConstructionDone`：返回业务错误“该阶段为内部施工完成确认，不提交业主验收”；
- `AcceptanceDone`：继续现有任务、证据、轮次和幂等逻辑；
- 不改变确认/驳回方法。

文件：`th4-decoboon/src/main/java/com/th4/edge/decoboon/project/context/ProjectAccessContext.java`

- 如需集中表达提交角色，可新增 `requireAcceptanceSubmitter`，允许 Boss/Pm/Designer；不得把 StoreManager/Finance/Marketer 纳入本批；
- 项目可见/参与关系仍使用现有 `visibleChild` 或项目参与查询，禁止先 `findById` 再前端判断。

### 6.4 契约与序列化

- 不新增 Provider 路由；
- 不新增请求实体；
- 阶段响应继续返回 `acceptanceMode`；
- 验收响应继续返回 `stage/project/submittedBy/status/evidenceUrls/round`；
- Designer 响应不得出现成本、利润、内部审批或供应商价格。

## 7. 回归矩阵

### 7.1 正向

1. Designer 是项目参与人，阶段 `AcceptanceDone`，任务全部完成，上传证据后提交成功；
2. Designer 对同一事件重复提交，返回同一验收事实；
3. 业主驳回后，Designer 可提交复验，追加新 Round；
4. Web 和 App 对同一验收记录显示同一状态、轮次、证据和下一步；
5. 提交后重新读取，阶段仍为 `Accepting`，项目状态为 `Accepting`。

### 7.2 反向

1. Designer 不是项目参与人：403/404，不泄露项目存在性；
2. Designer 对 `None` 阶段提交：400；
3. Designer 对 `ConstructionDone` 阶段提交：400；
4. `acceptanceMode` 为空：400 或“待核验”，不得默认通过；
5. 任务未全部完成：400；
6. 无现场证据：400；
7. 已有 Pending：返回幂等结果或明确阻断；
8. 已 Confirmed：拒绝再次提交；
9. 复验没有整改说明或整改后照片：400；
10. Designer 调用 `customer/acceptance/update`：403；
11. 跨企业、跨门店、跨项目：拒绝；
12. 非参与员工伪造项目/阶段 ID：拒绝。

### 7.3 App/Web 一致性

- 同一 Designer、同一项目、同一阶段，App 和 Web 的：
  - 可否提交；
  - 阻断原因；
  - 验收轮次；
  - 验收状态；
  - 证据数量；
  - 提交后的阶段状态；
  - 下一步责任人；
  必须一致。

## 8. 数据与发布门禁

本项涉及权限，按高风险变更处理：

1. 先做只读数据分布检查：`acceptanceMode` 的 `None/ConstructionDone/AcceptanceDone/空值` 数量及受影响项目；
2. 不修改历史验收记录，不删除既有 Round；
3. 若存在空值，先由产品/业务确认历史阶段如何处理；
4. 发布前备份数据库和目标阶段/验收事实；
5. 发布后先做 Designer 单角色冒烟，再做 Pm/Boss/业主反向冒烟；
6. 失败回滚代码和权限配置；不得反向删除已产生的验收事实。

## 9. 完成定义

- [ ] 用户确认“首验 + 复验均可由 Designer 提交”；
- [ ] App/Web 入口与服务端角色校验一致；
- [ ] `AcceptanceDone` 门禁由后端统一执行；
- [ ] `None/ConstructionDone/空值` 有明确不可提交原因；
- [ ] 业主确认/驳回仍只能本人执行；
- [ ] 正向、反向、幂等、并发、跨端和数据范围证据齐全；
- [ ] 真实页面/真机证据补充；
- [ ] 不包含设计模板、报价队列或其他角色范围。

## 10. 建议执行顺序

```text
1. 只读数据分布与现有测试盘点
2. 后端总门禁 + 定向测试
3. App 能力键/入口 + 页面回归
4. Web 门禁/文案对齐
5. 真实 App/Web/业主冒烟
6. 独立验收
```

当前状态只到本计划，不进入代码修改。

## 11. 本次编码进展（2026-10-23）

已完成的最小同源切片：

- App Designer 能力矩阵增加 `act:acceptRequest`，未增加 `act:acceptConfirm`；
- App 项目详情的验收入口改为由 `act:acceptRequest` 控制，不再借用 `act:startStage`；
- 后端 Provider 原有 Boss/Pm/Designer 角色门禁保持不变，未扩大其他角色；
- 新增后端回归：
  - 项目参与 Designer 可提交阶段验收；
  - 非参与 Designer 被 404 拒绝且不产生验收事实。

验证：

- `mvn -Dtest=ProjectMemberSwitchAndStageOpenTest test`：17/17 PASS；
- App `npm run type-check`：PASS；
- App `npm run build:mp-weixin`：PASS；
- App `check:action`、`check:contract-sign`、`check:navigation`：PASS。

尚未完成、不得宣称完整的发布门禁：

- `None / ConstructionDone / 空 acceptanceMode` 的阶段完成路径仍未统一实现；
- 当前 `submitAcceptance` 未统一按 `acceptanceMode` 阻断非 `AcceptanceDone`；
- 这属于既有阶段验收模式缺口，不能在本次权限对齐中静默补成一套新的状态机；
- 在实现两种内部完成路径并完成数据分布核验前，只允许把本次改动视为“Designer 权限与入口对齐”，不能视为阶段验收全链路完成。

本次未修改：

- Web 业务代码；
- 后端业务 Provider/状态机；
- DDL、迁移、生产数据；
- 报价/签约队列和设计模板。
