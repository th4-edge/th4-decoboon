# 材料计划状态机与"已下单后能否再选材"规则印证

- 日期：2026-09-23
- 类型：业务规则梳理 + 代码印证（verify-only，未改动任何业务代码）
- 依据事实源：装得清业务智能体 `BR-004`、`DEC-003`、`SCN-006`、`GAP-005`；设计稿 `docs/product/2026-09-23-材料计划业务梳理与设计.md` v1.2（DEC-MP-01～10）
- 印证方式：源码逐条核对（后端 Provider + 实体注释 + App 页面）

---

## 一、直接回答：已下单的材料计划，客户还能再选材吗？

**当前实现：能。** 前后端都没有禁止。

| 层 | 现状 | 证据 |
| --- | --- | --- |
| 后端保存 | **不校验**材料计划状态与采购单，已下单/已到货仍可保存新版本 | `material/service/selections/save/support/Save.java`：只校验 property/contract/project/contractLine/content，无 plan/purchase 校验 |
| 后端确认 | **不校验**，可直接把新版本置为 Confirmed | `material/service/selections/confirm/support/Confirm.java` |
| App | **允许编辑**，仅弹一句提示 | `subpackages/project/pages/selection-item.vue`:223「该分项已经进入下单或到场状态。修改选材会形成新版本，不会覆盖已确认记录。」保存按钮仍可点（只受 `canEdit` 控制） |
| 是否影响采购 | **不影响**（符合 BR-004"不改变采购事实"） | 采购单、应付、到场均按原下单事实保留 |

**一句话结论**：系统允许"已下单后再做一次选材确认"，但这次确认**只是留痕、不会改变已发生的采购事实**——这与已冻结的 BR-004/DEC-003 一致；**但"是否应当在已下单后禁止/拦截"这条规则，业务知识库尚未冻结**（GAP-005 只冻结了"变更形成新版本"，未冻结"下单后是否可变更"）。

---

## 二、状态机（代码印证后的实际运行口径）

### 2.1 选材记录 `MaterialSelection.status`

```
Draft(草稿) ──提交──► PendingConfirm(待客户确认) ──客户确认──► Confirmed(已确认, 版本锁定)
   ▲                        │                                      │
   └────────── 原地保存 ◄────┘                            再次保存 ──► 新版本 v+1 (Draft/PendingConfirm)
```

- 已确认版本 `locked()`，**不允许原地修改**；变更必须形成新版本（BR-004 / DEC-003）✅ 已实现
- 无"作废/撤销"状态；版本只增不减
- 选材记录**不参与**报价、合同、成本、材料计划数量、采购、库存的计算 ✅ 已实现

### 2.2 材料计划行 `MaterialPlan.status`

```
Planned(待选材) ──选材客户确认──► ReadyToOrder(可下单)
     │                                   │
     │                            财务确认下单（累计下单量）
     │                                   ▼
     │                    PartiallyOrdered(部分下单) ──► Ordered(已下单)
     │                                                        │
     │                                             到场确认（累计到场量）
     │                                                        ▼
     │                              PartiallyArrived(部分到场) ──► Arrived(已到场)
     │
     └── 变更新增 ──► ReadyToOrder
     └── 减项数量归零 ──► Cancelled；减项/退货 ──► 采购调整 或 退货单
（历史兼容值 Requested / Approved 仅读）
```

状态由 `MaterialPlanStatusContext` 按采购明细/采购单**实时派生**，禁止人工写入（可完成状态投影）✅ 已实现

### 2.3 关键门禁（唯一方向性约束）

| 门禁 | 方向 | 实现位置 | 状态 |
| --- | --- | --- | --- |
| 选材未确认不得下单 | 选材 → 采购 | `purchase/save#requireConfirmedSelection`、`purchase/batch#requireOrderable` | ✅ 已实现（DEC-MP-05，仅对 `sourceType=Selection`） |
| 已下单后不得再改选材 | 采购 → 选材 | 无 | ❌ **未实现（缺口）** |
| 到场确认人限本项目 PM/设计负责人 | 采购 → 到场 | `goods-receipt/save#requireArrivalConfirmer` | ✅ 已实现（DEC-MP-07） |
| 已到货扣减必须走退货单 | 变更 → 应付/成本 | `MaterialDeductionContext#createReturn` | ✅ 已实现（DEC-MP-02） |

---

## 三、印证的业务规则清单

| 编号 | 规则 | 来源 | 实现状态 |
| --- | --- | --- | --- |
| R1 | 选材结果以文本保存，不拆逐材料颜色/规格/尺寸字段 | BR-004 / DEC-003 | ✅ 已实现（`content` + `selectionJson`） |
| R2 | 已确认版本不覆盖，变更形成新版本 | BR-004 / DEC-003 | ✅ 已实现（`SelectionStatus.locked()` + 版本 +1） |
| R3 | 选材不改变报价/合同/成本/材料计划数量/采购/库存事实 | BR-004 / DEC-003 / SCN-006 | ✅ 采购与应付不受影响 |
| R4 | 多房屋必须显式 `propertyId`，不串房屋 | BR-004 / SCN-006 | ✅ 已实现 |
| R5 | 采购前必须存在已确认选材 | **DEC-MP-05（2026-09-23 用户裁决）** | ✅ 已实现 |
| R6 | 材料采购前是否强制选材（GAP-005 待确认项 4） | GAP-005 | ⚠️ **已被 DEC-MP-05 裁决，但业务知识库尚未回写** |
| R7 | 已下单/已到货后是否允许再做选材变更 | 无来源 | ❌ **规则未冻结** |
| R8 | 已下单/已到货的材料数量或金额变化必须走增减项 | 设计稿 v1.2 §4.2 | ✅ 已实现（采购调整/退货单） |
| R9 | 选材文本不参与供应商匹配 | DEC-MP-08 | ✅ 已实现 |

---

## 四、印证中发现的两个问题

### 缺陷 D1（P1，数据一致性）新版本选材会覆盖"已下单"计划行的选材快照

`Confirm.promoteLinkedPlan` 中：

```java
plan.setSelection(selection);           // 无条件覆盖
plan.setSelectedBrand(contractLine.getBrand());   // 无条件覆盖
plan.setSelectedSpec(contractLine.getSpec());
if (plan.getStatus() == Planned) { plan.setStatus(ReadyToOrder); }   // 只有状态是条件更新
```

**后果**：已下单/已到货的计划行，其"选材结果/品牌/规格"会被新版本覆盖，而采购单仍是旧材料 → **计划行与采购单口径不一致**，工作台与采购页可能各说一套。
（状态不会被回退，这一点是对的。）

### 缺口 G1（P2，规则未冻结）"下单后能否再选材"没有业务裁决

- 若判"允许" → 现状即符合，但需明确"改材料必须另走增减项"，并避免 D1 的覆盖；
- 若判"禁止" → 需要在 `selection/save` 与 `selection/confirm` 增加计划/采购状态门禁，并给客户明确提示与"去发起增减项"入口。

---

## 五、建议（待裁决后实施，本次未改代码）

1. **业务裁决（建议）**：已下单及之后状态，**禁止**新增/确认"更换材料"性质的选材版本；确需更换的走增减项（变更单 → 采购调整/退货单 → 新选材确认）。
2. **无论裁决如何，建议先修 D1**：`promoteLinkedPlan` 仅在计划行尚未进入采购（`Planned`/`ReadyToOrder`）时才更新选材快照；已下单后新版本仅作为"待变更记录"保留，不覆盖计划行快照。
3. 若采纳"禁止"：在 `material/selection/save` 与 `confirm` 增加门禁，返回 400 + 明确文案，并在 App 选材页把编辑区替换为"该材料已下单，如需更换请发起增减项"＋跳转按钮。
4. 业务知识库需回写：`GAP-005` 待确认项 4 已由 DEC-MP-05 裁决；新增待确认项"下单后选材变更规则"（建议编号 GAP-006）。

---

## 六、T05 研发咨询单（草稿，待提交业务智能体）

- 咨询对象：装得清业务智能体
- 问题：已下单/已到货的主材分项，客户是否还可以再做选材确认？
  - 选项 A（建议）：禁止，改材料必须走增减项；选材仅保留历史版本查看。
  - 选项 B：允许补充记录，但不得覆盖计划行已用于采购的选材快照；系统提示"仅留痕、不改变采购"。
- 影响范围：选材保存/确认门禁、材料计划工作台待办口径、变更单与选材的关联、对外话术。
- 现状证据：`material/selection/save`、`confirm` 无状态门禁；App 仅提示；采购事实不回写。

---

## 附：本次印证的源码位置

- 选材保存：`src/main/java/com/th4/edge/decoboon/material/service/selections/save/support/Save.java`
- 选材确认：`src/main/java/com/th4/edge/decoboon/material/service/selections/confirm/support/Confirm.java`
- 选材状态：`material/configure/SelectionStatus.java`（`locked()`）
- 计划状态：`project/configure/MaterialPlanStatus.java`、`project/context/MaterialPlanStatusContext.java`
- 下单门禁：`project/service/purchases/save/support/Save.java`、`.../purchases/batch/support/Batch.java`
- 扣减/退货：`project/context/MaterialDeductionContext.java`
- App 选材页：`th4-decoboon-app/src/subpackages/project/pages/selection-item.vue`

---

## 七、用户裁决与实施结果（2026-09-23 更新）

**裁决：判"禁止"。** 已进入采购的材料计划，不允许再保存或确认新版本选材；更换材料必须走增减项。

### 7.1 已实现（后端）

| 变更 | 位置 | 说明 |
| --- | --- | --- |
| 门禁判定 | `material/context/MaterialSelectionContext#selectionLocked(MaterialPlan)` | 仅 `Planned`（待选材）与 `ReadyToOrder`（可下单）允许改选材；`Requested/Approved/PartiallyOrdered/Ordered/PartiallyArrived/Arrived/Closed/Cancelled` 一律锁定 |
| 同一合同行多条计划 | `#selectionLocked(List)` + `#plansOf(project, contractLine)` + `MaterialPlanRepository#findByProjectAndContractLine` | 原计划与变更计划**任一条**已进入采购即锁定，避免"变更计划 Ready 就放行"的漏洞 |
| 保存门禁 | `material/selection/save` | 保存前调用 `requireSelectionEditable(plansOf(...))`，拒绝并返回可执行文案 |
| 确认门禁 | `material/selection/confirm` | 确认前同样校验 |
| 缺陷 D1 修复 | `Confirm#promoteLinkedPlan` | 已进入采购的计划行**不再回写** `selection/selectedBrand/selectedSpec`，杜绝"计划行选材 ≠ 采购单材料" |
| orderStatus 扩展 | `MaterialSelectionContext#applyOrderStatus` | 新增 `ordering`（已请购）、`partiallyOrdered`（部分下单），使 App 与门禁同口径 |

**错误文案**：`该材料已进入采购（{状态名}），不能再修改选材；如需更换材料请发起增减项`

### 7.2 已实现（App）

| 变更 | 位置 | 说明 |
| --- | --- | --- |
| 锁定判定 | `subpackages/project/pages/selection-item.vue` | 新增 `locked = orderStatus !== 'none'`；`canEditNow = canEdit && !locked` |
| 编辑区 | 同上 | 锁定后选材输入框禁用、保存草稿/提交客户确认按钮与"客户已确认"按钮隐藏 |
| 换材料入口 | 同上 | 锁定提示改为"该分项已进入采购（…），不能再修改选材" + 说明 + **「去发起增减项」按钮**（带 projectId/contractId 跳 `change-create`） |
| 口径对齐 | `subpackages/project/pages/selection.vue` | 统计与筛选统一为"已进入采购"（`orderStatus !== 'none'`），并补齐新状态文案 |
| 状态类型 | `domains/material/model.ts` | `MaterialOrderStatus` 增加 `ordering` / `partiallyOrdered` |

### 7.3 验证

- 新增 `MaterialSelectionGateTest`（4 例）：Planned/ReadyToOrder 放行；8 个采购/关闭状态全部锁定；锁定抛错含"发起增减项"；同一合同行多计划任一锁定即锁定。
- 后端全量：`mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` → **271 tests / 0 failures / 0 errors**。
- App：`type-check` / `build:mp-weixin` / `check:subpackages`（92 页）/ `build:h5` 全部通过。

### 7.4 待业务回写（business 智能体）

1. `GAP-005` 待确认项 4（采购前是否强制选材）→ 已由 **DEC-MP-05** 裁决为"必须"，请回写并置为 resolved。
2. 新增决策记录：**"材料进入采购后禁止变更选材，换材料走增减项"**（建议编号 DEC-MP-11 / 或 GAP-006 → DEC）。
3. 对外话术需同步：选材页对已下单材料的文案为"不能修改，请发起增减项"。
