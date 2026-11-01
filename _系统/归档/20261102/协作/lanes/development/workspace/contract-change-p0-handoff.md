# 合同增减项 P0 开发交接

- request_id：`REQ-20260921-DEV-CONTRACT-CHANGE-P0`
- lane：`development`
- 状态：`implementation_done_self_tested_pending_independent_acceptance`
- 设计依据：`../../../../../前端代码/th4-decoboon-app/docs/product/2026-09-21-合同增减项业务流程与交互设计.md`（v1.2 已冻结）

## 输入依据

- 用户 2026-09-21 确认的 7 条决策（见设计文档 §12）。
- 既有事实：后端 `ContractChange` 全套能力、`project/contract-line/page`（含成本角色裁剪）、App `change.vue` 工作台均已存在。
- 本次补齐：**撤回**、**App 逐条/批量发起**、**变更单详情与审批/客户确认**、**工作台审批权限纠偏与入口**。

## 变更文件

### 后端（新增）

- `src/main/java/com/th4/edge/decoboon/project/service/contractchange/cancel/Cancel.java`
- `src/main/java/com/th4/edge/decoboon/project/service/contractchange/cancel/support/Cancel.java`
  - 路由 `project/contract-change/cancel`；仅 `Pending` 可撤回；允许**发起人本人或同团队负责人**（TeamLeader 需同 team，StoreManager/Boss 放行）；置 `ContractChangeStatus.Cancelled`；无金额/应收/任务/材料副作用。

### App（新增）

- `src/subpackages/project/pages/change-create.vue` — 发起增减项：逐条 / 批量双模式；5 类变更；原合同行选择（替换/数量/价格必选）；金额三段结论卡；成本按角色隐藏；签证照片。
- `src/subpackages/project/pages/change-detail.vue` — 变更单详情：变更前/本次/变更后、明细、签证与签认、流转记录、审批（仅 boss/finance）、客户确认代登记（boss/pm/designer，可上传签认单）、撤回。

### App（改造）

- `src/subpackages/project/pages/change.vue`
  - 审批权限由 `boss/pm/designer` **纠偏为 `boss/finance`**（与后端 `update` 一致）。
  - 客户确认代登记角色补 `designer`（与后端 `customer-confirm` 一致）。
  - 新增「发起增减项」入口、列表卡片点击进入详情。
- `src/domains/trade/api.ts` — 新增 `cancelContractChange`、`batchSaveContractChange`。
- `src/config/index.ts` — 新增 `projectChangeCreate`、`projectChangeDetail`。
- `src/pages.json` — 注册 `pages/change-create`、`pages/change-detail`。

## Verification

- 后端：`mvn -q -DskipTests compile` 通过。
- App：`npm run type-check` 通过。
- App：`npm run build:mp-weixin` → `Build complete`。
- App：`npm run check:subpackages` 通过（84 页，+2）。

## 全量验证结果（2026-09-21 04:45）

- 后端 `mvn clean test`：**228 tests / 0 failures / 1 error**。
  - 唯一 error 为 `AuthSessionInfoLazyAcceptanceTest` 的 Spring 测试上下文加载偶发（`class path resource ... cannot be opened`），**单跑该测试通过**（1/1 BUILD SUCCESS），与本次改动无关。
  - 注：非 clean 的 `mvn test` 会因 `target/classes` 陈旧产生 56 个 `NoClassDefFound ...$1` 假失败；**验证后端必须用 `mvn clean test`**。
- 后端 `ProviderSecurityAcceptanceTest`：**3 tests / 0 failures / BUILD SUCCESS**。当前基线断言 `hasSize(277)`（public 12 + protected 265）**已包含本次新增的 `project/contract-change/cancel`**，无需 acceptance 再改。
- App：`type-check` / `build:mp-weixin` / `check:subpackages` 全通过。

## GAP-3 补做（2026-09-21 04:50）

- 后端新增 `project/contract-change/info`（`contractchange/info/{Info.java, support/Info.java}`）：一次返回变更单 + 明细 + 原合同行 + 合同金额上下文；无成本权限时服务端置空 `costDelta`（`BR-003`）。
- App：`tradeApi.contractChangeInfo`；`change-detail.vue` 改为单条读取，不再按项目列表回读过滤。
- 路由基线 277 → **278**（protected 265 → 266），已同步 `ProviderSecurityAcceptanceTest`（该文件当时也被并发会话改动，合并时注意保留双方注释）。验证：`mvn test -Dtest=ProviderSecurityAcceptanceTest` → 3/3 BUILD SUCCESS。

## 全量测试的环境坑（重要）

`mvn clean test` 在本机会**间歇性**失败，两种表现，均非代码缺陷：

1. `NoClassDefFound ...$1` / `程序包 ... support 不存在` —— `target/classes` 陈旧，必须 `mvn clean test`。
2. `MockitoInitializationException: Could not self-attach to current VM ... Failed to load interface org.mockito.plugins.MockMaker` —— Mockito inline-mock-maker 自附加失败，导致 Spring 上下文批量级联报错（一次 191 errors）。隔离单跑即通过。
   - 缓解：避免与其它 Maven/JVM 重负载并发；或给 surefire 配 `-javaagent:.../mockito-core.jar`。

## Risks

1. **并发写入（04:49 已恢复）**：开发期间另一会话曾同时改写 `src/styles/tokens.ts`、`src/domains/*/meta.ts`、`src/subpackages/project/pages/*`，并同步更新了 `ProviderSecurityAcceptanceTest` 路由基线。用户已于 04:45 确认停止；此后工作区 10 分钟 0 次写入。合并前建议再确认一次。
2. **变更单详情用列表回读**：App 详情页经 `project/contract-change/page` 按 `projectId` 取回再按 `changeId` 过滤，尚无单条聚合 Provider（设计文档 `GAP-3`）。列表 >500 条时需补 `info`。
3. **成本口径**：项目经理/设计师/业务员不可见成本（`BR-003`）。其发起的增项 `costDelta` 为 0，会呈现「成本未完整」，符合 `MET-002`，但需产品确认这是期望行为。
4. **批量明细由前端给金额**：`batch-save` 不重算 `amountDelta`（仅做符号校验），与设计文档「金额服务端计算」在批量路径存在差异，需后续统一。

## Next Action

1. acceptance 补撤回 / 审批 / 客户确认 / 发起的数据权限与状态机用例（路由基线已同步，无需再改）。
2. 真机与微信开发者工具验证逐条与批量发起、审批、客户确认、撤回四条链路。
3. 后端补 `GAP-3` 变更单聚合 Provider（`P1`）。

## Acceptance Needed

- 撤回：Pending 可撤（本人/同团队）、非 Pending 拒绝、非同团队拒绝。
- 审批：仅 boss/finance；非授权角色拒绝；状态不合法拒绝。
- 客户确认：仅 Approved 可确认；重复确认幂等；越权拒绝。
- 发起：替换/数量/价格未选合同行被拒；变更后合同额 < 0 被拒；批量一次提交为一张变更单。
- 数据权限：跨企业/跨门店/跨团队访问变更单与合同行返回无权限。
