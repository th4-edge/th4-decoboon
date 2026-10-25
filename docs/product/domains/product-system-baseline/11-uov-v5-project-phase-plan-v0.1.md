# UOV-V5 项目 PHASE 事实追踪准备单 v0.1

> 日期：2026-10-23
>
> 当前状态：`U5-A / U5-B1 / U5-B2 DEV_VERIFIED`；`U5-C / U5-D ACCEPTED`；U5-E 仅允许重新对账后准备；U5-F 未启动
>
> 前置：U1/U2/U3/U4 独立验收 PASS；合同事实节点契约已验收
>
> 依据：`DEC-086 §5`、`BR-052 §4`、`ANS-BIZ-20261022-007`、`T12-BIZ-20261022-007`、`GAP-065`

## 一、目标

让项目成为事实索引根，而不是所有事实的所有者：

```text
Project
→ ProjectFactTrace
→ PHASE / 时间线 / 异常视图
→ 每个节点引用来源对象和版本
→ 点击进入统一对象视图
→ 返回保留项目、阶段、筛选和滚动锚点
```

首期只做项目事实索引与展示，不重构项目状态机，不新增 DDL。

## 二、适用范围

- 角色：BOSS、店长、设计师、项目经理、财务、业主只读。
- 对象：Customer、Property、Measure、Design、Quote、ProjectContract、Project、ProjectStage、ProjectTask、SiteReport、MaterialSelection、MaterialPlan、PurchaseOrder、GoodsReceipt、ContractChange、Acceptance、Receivable、Receipt、Payable、Payment、ProjectCostContext、CustomerFeedback。
- 入口：App 项目详情、Web 项目执行工作面、业主项目旅程。
- 不适用：合同完整上游链（U4 已完成并单独验收）、PDF 归档、全域对象统一视图扩展。

## 三、现有基础

| 能力 | 当前入口 | 可复用 | U5 缺口 |
| --- | --- | --- | --- |
| 统一事实节点 | `BusinessFactNode` / `BusinessFactContract` | 阶段、类型、排序、projectId、导航、成本完整性 | 缺少项目聚合上下文 |
| 合同事实链 | `ContractWorkbenchContext` | 上游/下游节点构造方式 | U5 不能复制合同完整明细 |
| App 项目详情 | `subpackages/project/pages/detail.vue` | 项目、阶段、任务、日报、验收、财务、材料和变更数据 | 多接口拼装、多 Tab；无 PHASE 索引 |
| App 业主旅程 | `subpackages/project/pages/journey.vue` + `CustomerJourneyContext` | 量房、设计、报价、合同、材料、施工、验收、付款、反馈 | 独立 section 模型，不是统一项目事实索引 |
| Web 项目执行 | `ProjectExecutionView.vue` | 项目、阶段、任务、现场汇报、节点留证 | 无 PHASE 事实链与返回锚点 |
| 项目详情 Provider | `project/project/info` | 项目身份、客户/房屋、报价、责任人、财务摘要 | 未返回事实节点集合 |
| 项目概览 Provider | `project/project/overview` | 风险、状态、进度、金额摘要 | 属于经营聚合，不应承担事实链 |

## 四、U5 垂直切片

| 切片 | 用户闭环 | 依赖 | 退出条件 | 回滚 |
| --- | --- | --- | --- | --- |
| U5-A | 冻结 `ProjectFactTrace` 契约与节点来源映射 | U4 accepted；`BusinessFactNode` | 已完成：模型、排序、分组、业主过滤、跨项目/重复节点/缺口校验和 9/9 测试 | 文档/代码独立回退 |
| U5-B1 | 后端当前项目事实聚合：来源/售前/签约/履约/变更/验收/财务/售后 | U5-A | 已完成：project/project/info 返回当前事实、来源缺口和角色成本裁剪；12/12 测试通过 | 关闭新聚合，保留旧项目详情 |
| U5-B2 | 历史/替代/作废节点折叠、利润/结算节点 | U5-B1 | 已完成：历史折叠、结算快照利润/毛利率继承、角色裁剪；18/18 测试通过 | 回退历史聚合，不影响当前事实 |
| U5-C | App 项目详情 PHASE 视图 | U5-B | 第二轮独立验收 PASS / ACCEPTED：成本根字段移除、服务端顺序直连、缺失契约不兜底；type-check/build/subpackages/navigation PASS | App 回退旧 Tab/时间线 |
| U5-D | Web 项目执行工作面 PHASE 视图 | U5-B | 第二轮独立验收 PASS：宽屏事实索引、历史锚点、采购来源入口或显式摘要-only；原施工执行保留 | Web 回退旧工作面 |
| U5-E | 业主项目 journey 消费 owner 事实白名单 | U5-B；Owner 授权 | 只显示授权事实，隐藏成本/利润/公式/供应商价 | 回退旧 journey section 投影 |
| U5-F | U5 契约测试、权限负向、真实页面证据 | U5-B~E | 独立验收 PASS | 前后端独立回滚 |

## 五、事实节点映射

| FactStage | FactType | 来源对象 | 节点摘要最小字段 | 可见性 |
| --- | --- | --- | --- | --- |
| Source | Customer | Customer | 姓名、负责人、客户状态 | Internal + OwnerVisible |
| Source | Property | Property | 小区、地址摘要、房屋状态 | Internal + OwnerVisible |
| Source | ProjectOrigin | Project/Contract | 正常/承接来源、接管基线 | Internal + OwnerVisible（仅来源文案） |
| PreSale | Measure | Measure/MeasureRegion | 最近已完成量房、区域数/实测数/待补数 | Internal + OwnerVisible |
| PreSale | Design | Design | 设计名称、版本、状态、区域/分项数 | Internal + OwnerVisible |
| PreSale | Quote | Quote | 报价编号、版本、状态、金额、成本完整性（按角色） | Internal；Owner 仅销售字段 |
| Signing | QuoteAcceptance | Quote | 接受状态、登记人/时间（Owner 不显示登记人） | Internal + OwnerVisible |
| Signing | Contract | ProjectContract | 合同编号、状态、签约额/当前额 | Internal + OwnerVisible（销售口径） |
| Fulfillment | Project | Project | 项目编号、状态、进度、当前阶段 | Internal + OwnerVisible |
| Fulfillment | ConstructionPlan | ProjectPlan/ContractPlan | 计划版本、起止日期、阶段数 | Internal + OwnerVisible（摘要） |
| Fulfillment | Stage | ProjectStage | 阶段、状态、进度、计划/实际日期 | Internal + OwnerVisible |
| Fulfillment | Task | ProjectTask | 任务、状态、工程量、责任人 | Internal + OwnerVisible（摘要） |
| Fulfillment | SiteReport | ProjectReport | 日报、业务时间、客户可见性 | Internal + OwnerVisible（仅公开） |
| Fulfillment | MaterialSelection | MaterialSelection | 版本、状态、选材摘要 | Internal + OwnerVisible |
| Fulfillment | MaterialPlan | MaterialPlan | 计划、品种数、状态 | Internal；Owner 摘要 |
| Fulfillment | Purchase | PurchaseOrder | 采购/应付摘要 | Internal；Owner 不显示采购价 |
| Fulfillment | GoodsReceipt | GoodsReceipt | 到货时间、数量/状态 | Internal；Owner 摘要 |
| Change | ContractChange | ContractChange | 类型、状态、差额、生效时间 | Internal；Owner 按 DEC-068 |
| Acceptance | Acceptance | Acceptance | 阶段、结论、轮次、待整改数 | Internal + OwnerVisible |
| Acceptance | Rectification | AcceptanceIssues/Round | 整改责任人、期限、状态 | Internal + OwnerVisible |
| Acceptance | Reinspection | AcceptanceRound | 复验轮次、结论 | Internal + OwnerVisible |
| Finance | Receivable | Receivable | 应收/已收/待收 | Internal；Owner 付款口径 |
| Finance | Receipt | Receipt | 收款时间、金额、方式 | Internal；Owner 摘要 |
| Finance | Payable | Payable | 应付/已付/待付 | Internal |
| Finance | Payment | Payment | 付款时间、金额 | Internal |
| Finance | Cost | ProjectCostContext | 预计/实际成本、完整性 | BOSS/店长/财务 |
| Finance | ProfitSettlement | ProjectCostContext/ProjectContract | 利润、毛利率、结算状态 | BOSS/店长/财务 |
| AfterSales | Delivery | Project/交付对象 | 交付日期、状态 | Internal + OwnerVisible |
| AfterSales | Warranty | Warranty/售后对象 | 质保期、维保状态 | Internal + OwnerVisible |
| AfterSales | Feedback | CustomerFeedback | 意见类型、状态、处理摘要 | Internal + OwnerVisible |
| AfterSales | Repurchase | Customer 经营关系 | 复购意向/项目摘要（仅有事实时） | Internal |

## 六、默认展示与排序

1. 默认 PHASE；固定阶段顺序 `Source → PreSale → Signing → Fulfillment → Change → Acceptance → Finance → AfterSales`。
2. 默认展示全部已发生正式事实；当前阶段和异常阶段展开，其余阶段折叠摘要。
3. 未来未发生节点不生成空白事实卡。
4. 当前必需缺失显示“待补齐”；历史缺失显示“暂不可确认”；可选未发生显示“无记录”。
5. 历史/替代/作废节点默认折叠在同一 `nodeKey` 下，`current=false`，可展开追溯。
6. 排序沿用 `BusinessFactContract.stableOrder()`：
   `stage.order → businessTime asc nulls last → recordedAt asc nulls last → factType.order → objectId`。
7. 节点只显示摘要，不复制报价/设计/材料完整明细。

## 七、返回与权限

每个节点必须包含：

- `nodeId/nodeKey/stage/factType/objectType/objectId/projectId/version/current`
- `status/statusName/businessTime/recordedAt/actorName/title/summary/sourceLabel`
- `amount/quantity/progress/costComplete`
- `uncertainty/relations/visibility/navigationPath/returnPath/returnAnchor`

规则：

1. `projectId` 由当前项目上下文绑定，不能信任前端传入的业务归属。
2. 内部节点按既有项目数据权限裁剪；Owner 只接收 `OwnerVisible` 且销售/公开字段。
3. `cost/costComplete/利润/毛利率/供应商执行价`仅 BOSS/店长/财务。
4. 未生效增减项不显示金额；历史/失效节点不参与当前汇总。
5. 读取失败显示“部分事实暂不可确认”，不得用空数组伪装无事实。

## 八、技术边界与查询

- 不新增 DDL。
- 建议不新增 Provider：在 `project/project/info` 上增加瞬态 `ProjectFactTrace` 投影，避免新增 `project/fact/*` 路由。
- 后端聚合按事实族批量读取，禁止逐节点查询或循环体内查询。
- 第一阶段优先做只读索引；编辑、审批、收付款仍走原 Provider。
- 所有跳转携带 `projectId + objectId + version + returnPath + returnAnchor`。
- 事实节点来源缺失只进入 `sourceGaps`，不生成占位业务对象。

## 九、验收最低要求

1. 项目详情返回固定 8 阶段事实索引。
2. 节点来源、版本、状态、业务时间和记录时间可追溯。
3. 阶段顺序和节点排序稳定；输入顺序变化不影响结果。
4. 当前/历史/失效节点状态明确。
5. App/Web/业主三端事实集合可对账。
6. 业主不看到成本、利润、内部公式、供应商价或内部审批。
7. 一个项目一次读取的事实链 SQL 不随节点数量线性增加；全链页面查询预算按 U6 端到端测量。
8. 真实页面、越权和历史回读由独立验收确认。

## 十、门禁

```text
U1/U2/U3/U4：PASS / ACCEPTED
U5-A：DEV_VERIFIED
U5-B1：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-B2：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-C：ACCEPTED
U5-D：PASS / ACCEPTED
U5-E：RECONCILIATION_AND_PREPARATION_ONLY
U5-F：NOT_STARTED
U6：未启动
GAP-065：OPEN
GAP-071：OPEN
PI-20261022-010：DEV_IN_PROGRESS
```

本准备单只能作为 U5 研发任务输入，不得据此宣称项目事实追踪已实现或已验收。
