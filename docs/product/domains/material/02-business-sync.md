# 材料履约与材料业务助手 · 业务智能体同步

## 1. 本轮同步结论

- 同步状态：**核心材料事实已 aligned，结构化蓝图可进入实现**。
- 业务事实源：`zhuangdeqing-business` 规范目录。
- 本轮不新增业务规则；只把已裁决材料事实产品化、结构化、可解释化。
- 2026-10-17 用户追加裁决（历史）：App 单端完成材料下单与结算闭环，旧“材料计划直接采购”路线下线；其中“Web 不作为必经节点”已由 2026-10-22 `DEC-088/BR-054` 覆盖。
- 新规则或口径变化仍必须走 T05 → T06/T12。
- 最近核对实现：`97d5705`（2026-10-17）。

## 2. 已引用的业务事实

| 类型 | 编号 | 用于 |
| --- | --- | --- |
| 业务对象 | OBJ-008 | 主材下单作业单、品类模板、作业单与采购单边界 |
| 业务规则 | BR-009 | 选材门禁、进入采购后锁定、换材走增减项 |
| 业务规则 | BR-044 | 作业单三层分工、品类基线、字段个性化边界 |
| 业务规则 | BR-045 | P1/P2/P3、无价下单、暂估应付、成本完整性 |
| 业务场景 | SCN-005 | 项目采购到供应商应付闭环 |
| 业务场景 | SCN-006 | 签约后主材选材确认 |
| 业务场景 | SCN-021 | 作业单成单、到货与结算 |
| 决策 | DEC-008 | 材料计划选材门禁与换材路径 |
| 决策 | DEC-056 | App 主材选材现场代录与门禁 |
| 决策 | DEC-072 | 作业单、无价下单、暂估应付、结算价 |
| 决策 | DEC-073 | 作业单与采购单两张单、结算互斥 |
| 决策 | DEC-074 | 材料员=财务、月结期间、结算撤销 |
| 约束 | DEC-029 / BR-027 | 业务并发使用 RedissonLock，禁止数据库锁 |

## 3. 已确认的产品化边界

- 蓝图不复制业务事实，只引用业务编号；
- 业务助手只读，不做审批/付款/删除/改权限；
- 业务助手沿用登录员工的数据范围，不通过大模型扩大权限；
- 金额/成本/数量/状态由领域服务计算，不由模型计算；
- `gap`/`stale`/`draft` 不作为正式回答；
- 小程序正式版发布由用户本人完成。

## 4. 已实现但仍需页面与独立验收的事项

| 事项 | 代码事实 | 已覆盖测试 | 当前状态 | 不能宣称什么 |
| --- | --- | --- | --- | --- |
| 测量字段按品类模板显隐/必填 | `material/sheet/enums` 投影企业生效 `headerFields/lineFields`；提交校验 `visible && required` 的单据头字段 | `MaterialSheetTemplateFieldConfigTest`、`MaterialSheetOrderFlowTest.submitWithoutRequiredMeasurementMustBeRejected` | `CODE_VERIFIED`，待 App/Web 页面证据和独立验收 | 不能说所有品类都已覆盖；V1 基线只有 Tile |
| 作业单独立取消 Provider | `material/sheet/cancel`；仅未进入采购前可取消，原因写入备注 | `MaterialSheetOrderFlowTest.cancelBeforeOrderShouldMoveToCancelled`、`cancelAfterOrderShouldBeRejected` | `CODE_VERIFIED`，待页面动作证据和独立验收 | 不能说可撤销已下单采购；必须走采购调整/退货 |

定向测试已于 2026-10-17 执行通过：
`MaterialSheetOrderFlowTest`、`MaterialSheetTemplateFieldConfigTest`、`MaterialSheetEnumsProviderTest`、`MaterialSheetTemplateWhitelistTest`、`MaterialSheetResponseProjectionContractTest`。

## 5. 未决项与非阻塞处理

| 问题 | 影响 | 处理 |
| --- | --- | --- |
| 请购时间/采购下单时间是否新增独立字段 | 单据和助手时间解释 | 保持 `createTime`=建单、`orderedAt`=采购下单；新增字段需业务裁决，已在蓝图标记 `GAP-MAT-TIME-SEMANTICS` |
| 助手回答范围/表达边界 | 运行时权限与措辞 | 已确定为只读、同用户范围、无高风险写入；B4 用黄金问题和越权用例验收 |

## 6. 同步状态

- 业务知识来源版本：以规范目录当前索引为准。
- 蓝图状态：`ready-for-implementation`，版本 `1.0.0`。
- 需要重新同步的节点：仅时间字段新增口径；测量门禁和取消路径已按 `97d5705` 校正。
- 对外表达边界：只能说“助手帮助解释/校核/引导”，不能说“助手自动审批、自动付款或替代老板决策”。

## 7. 2026-10-17 App 单端闭环裁决（历史，已被 2026-10-22 覆盖）

| 项 | 内容 |
| --- | --- |
| 决策来源 | 用户书面确认（最高权威） |
| 唯一路线 | `MaterialPlan → MaterialOrderSheet → 提交请购 → App 下单 → PurchaseOrder/PurchaseOrderItem → Payable → GoodsReceipt → MaterialSettlement` |
| 旧路线 | “材料计划直接采购”（`project/purchase/save`、`project/purchase/batch`）从 App 产品面下线，后端新下单入口收口；历史采购单保留只读 |
| App 角色 | Designer 建单/提交；PM 测量/到场；Finance/Boss/StoreManager 下单/结算；客户只读 |
| 闭环边界 | L1 下单履约 + L2 材料结算/成本完整；Web 不是接管端 |
| 不变量 | 作业单不带价格；1 张作业单可对应 N 张采购单；无价形成暂估应付；成本缺失时利润为 `null`；同一采购明细只能结算一次 |
| 验收 | App 真机/开发者工具全链走查；旧入口不存在；无价、部分下单、多供应商、到场、逐单/月结、撤销结算、越权均有证据 |
| 状态 | `BIZ_ALIGNED`，允许进入 B5 蓝图/实现 |

> 本节只保留历史实施证据。`Web 不是接管端` 已被 2026-10-22 用户“App/Web 强制能力一致完整”裁决和 `DEC-088 / BR-054` 覆盖；不得再据此豁免 Web 写能力。

## 8. 2026-10-22 App/Web 强制能力与主材异常闭环

| 项 | 内容 |
| --- | --- |
| 业务答复 | `ANS-BIZ-20261022-009` |
| 正式决策 | `DEC-088`、`BR-054` |
| 研发交接 | `T12-BIZ-20261022-009` |
| 跨端 | 施工排期、节点留证、日报、作业单全生命周期、到场确认均为双端强制；设备输入可不同，Web 必须有等价闭环 |
| 状态 | `Pricing` 退出新流转；`Settled` 由有效到场与未撤销结算覆盖推导；`Closed` 结算后显式关闭 |
| 价格 | P1/P2/P3 只允许服务端只读投影，禁止前端写入或参与计价 |
| 异常 | `MaterialAdjustmentOrder` 统一承载退货/补发/换货，复用下层财务/库存/往来事实 |
| 事实节点 | 作业单、采购、到场、异常处置、结算五个独立节点族，接入 `PI-20261022-010` |
| 实现缺口 | `GAP-067`；未授权编码，DDL/权限/金额成本切片需用户确认 |
