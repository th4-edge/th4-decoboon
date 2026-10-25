# 材料履约与材料业务助手 · 需求与验收条件

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-MAT-001 | 用户能按角色看懂材料全链路 | 登录员工有模块访问权 | 项目/企业数据范围 | 标准/企业/项目三层 | Web 蓝图中心、App 入口 | blueprint definition/runtime | 角色投影正确、无跨租户 | OBJ-008/BR-044 |
| REQ-MAT-002 | 老板/财务能打开项目材料业务体检 | 项目有材料数据 | Boss/StoreManager/Finance | 缺选材/待下单/待到货/成本不完整 | 业务体检卡、可跳转 | blueprint/check/report | 计数与详情同源、异常可恢复 | DEC-008/BR-045 |
| REQ-MAT-003 | 用户能理解当前作业单为什么不能继续 | 打开作业单详情 | 当前用户数据范围 | Sheet/Plan/Purchase/Receipt | 右侧“为什么/下一步” | assistant + material/sheet/info | 回答引用真实对象和规则 | SCN-021 |
| REQ-MAT-004 | 用户能查询字段/状态/时间语义 | 打开蓝图或字段 | 当前角色可见范围 | 字段字典、状态机 | 字段搜索、对象详情 | blueprint field dictionary | 不含价格/成本给无权角色 | BR-044/BR-045 |
| REQ-MAT-005 | 财务能解释应付与成本为什么不完整 | 采购/结算存在 | Finance/Boss/StoreManager | Payable、costComplete、P2/P3 | 采购/结算页助手 | Payable + ProjectCostContext + settlement | 不用 0 或预计成本冒充实际成本 | BR-045/DEC-072 |
| REQ-MAT-006 | PM/设计师能确认测量和到场并得到下一步 | 作业单可维护/采购已下单 | PM/Designer + fallback | measureAt、GoodsReceipt | App 作业单详情/到货页 | material/sheet/measure + goods-receipt/save | 部分到场、超量、无价、账期正确 | SCN-021 |
| REQ-MAT-007 | 减项时能知道走计划/采购/退货哪条路径 | 合同变更生效 | 财务/项目角色 | Plan/Purchase/Return/Payable | 变更/助手解释 | ContractChangeFulfillmentContext | 已到货必须退货、应付/成本/库存一致 | DEC-MP-02/DEC-073 |
| REQ-MAT-008 | 业务助手只回答当前用户有权看的数据 | 任意问答 | 登录员工/项目参与关系 | tenant/project scope | Web/App 助手 | assistant tools + Provider | 跨租户/越权请求被拒绝 | BR-027/权限规范 |
| REQ-MAT-009 | 业务助手对业务规则/金额不做幻觉 | 任意问答 | 只读 | RAG + rule engine + live data | 证据/下一步展示 | 业务知识引用 + 确定性校核 | 黄金问题、无证据拒答 | BR-045/OBJ-008 |
| REQ-MAT-010 | 本批交付可与设计/蓝图/页面/测试对账 | 批次完成 | 不适用 | 对账矩阵 | 证据面板 | domain_state/reconciliation | 无 UNMAPPED/STALE | domain protocol |
| REQ-MAT-011 | 用户看到的作业单字段与品类模板一致 | 企业模板已发布或回退平台基线 | 登录员工 | 模板版本、header/line 字段 | App/Web 建单/详情 | material/sheet/enums + submit | 显隐/必填/顺序一致，必填缺失可恢复 | OBJ-008/BR-044 |
| REQ-MAT-012 | 用户能取消尚未进入采购的作业单 | 作业单未进入采购 | 现有操作权限 | Sheet[Cancelled]、取消原因 | App/Web 作业单详情 | material/sheet/cancel | 已下单/到场/结算拒绝直接取消 | OBJ-008/DEC-MP-02 |
| REQ-MAT-013 | 财务能在 App/Web 从待下单作业单完成采购下单 | 作业单已提交请购 | Finance/Boss/StoreManager | Sheet/Plan/Purchase/Payable | App/Web 主材下单工作台/下单页 | material/sheet/order | 双端动作同源；多供应商、部分下单、可空单价、暂估应付正确 | SCN-021/DEC-072/DEC-073/DEC-088 |
| REQ-MAT-014 | 财务能在 App/Web 完成材料结算并收敛成本 | 采购明细已到场 | Finance/Boss/StoreManager | PurchaseOrderItem/MaterialSettlement/Payable | App/Web 主材结算 | material/settlement/preview/confirm/cancel | 双端动作同源；逐单/月结互斥、撤销回退、成本完整/利润 null 正确 | BR-045/DEC-073/DEC-074/DEC-088 |
| REQ-MAT-015 | App 不再提供材料计划直接采购入口 | 登录员工在 App 材料/供应商页面 | 按现有权限 | MaterialPlan/PurchaseOrder | App 材料计划/供应商 | 旧 `project/purchase/save`/`batch` 下线 | 旧入口不可达，历史采购单仍可查/到场 | 2026-10-17 用户裁决 |
| REQ-MAT-016 | 用户能在真实材料业务上下文中打开「问材料」并得到可追溯回答 | 用户在项目/作业单/采购/结算页面 | 当前登录员工数据范围 | 项目/作业单/采购/结算上下文 | App/Web 统一上下文抽屉 | `material/assistant/ask` + 发布蓝图建议问题 | 结论/证据/只读工具/下一步/拒答/权限正确；单轮；不改金额或权限语义 | 2026-10-17 用户研发要求 / BR-045 / OBJ-008 |
| REQ-MAT-017 | 同一角色能在 App/Web 完成作业单新建/编辑/测量/提交/取消/导出和到场确认 | 作业单可操作、采购已下单 | 按 BR-054 角色矩阵 | MaterialOrderSheet/PurchaseOrder/GoodsReceipt | App/Web 作业单/到场页 | material/sheet/* + project/goods-receipt/save | 同角色动作集合相同；设备输入可不同但结果/错误恢复/审计同源 | DEC-088/BR-054 |
| REQ-MAT-018 | 用户看到的作业单状态与采购、到场、异常、结算事实一致 | 作业单已有采购或结算事实 | 项目/财务数据范围 | Sheet/Purchase/Receipt/Adjustment/Settlement | App/Web 作业单详情 | 服务端状态推导 | 部分到场/多供应商/无价/撤结算/关闭正确；Pricing 不新写 | DEC-088/BR-054 |
| REQ-MAT-019 | 有权角色能对破损、错发、少发、色差、补发、换货和无损退货发起独立处置 | 已下单或已到场 | PM/Designer 上报；Finance/Boss/店长确认 | MaterialAdjustmentOrder 及下层事实 | App/Web 异常处置页 | adjustment/* | 部分数量、证据、供应商确认、财务/库存/往来对账、幂等、越权 | DEC-088/BR-054 |
| REQ-MAT-020 | 作业单价格投影只读且不参与任何计价 | 采购/结算事实存在 | BOSS/店长/财务可见；其他不可见 | P1/P2/P3 投影 | App/Web 作业单详情 | 服务端聚合投影 | 前端不可写；多价返回 Mixed；不形成第二套成本/应付 | DEC-088/BR-045 |
| REQ-MAT-021 | 项目事实链能看到五个独立材料节点 | 项目已有材料事实 | 项目/财务/业主字段白名单 | Sheet/Purchase/Receipt/Adjustment/Settlement | 项目事实追踪/统一对象视图 | PI-010 契约 | 五节点身份/状态/版本/撤销链可追溯；不新增第二套事实模型 | DEC-088/PI-20261022-010 |

## 非功能要求

- 列表/体检接口响应体最小字段白名单；
- 统计/计数使用聚合投影，不在循环内查库；
- 助手回答不得直接查生产库，使用 Provider 工具；
- 助手 UI 必须使用稳定 requestId/idempotencyKey，不保存问题/回答正文；上下文 ID 只作为参数，权限仍由后端校验；
- 所有高风险动作只读或需人工确认；
- App/Web 对材料下单、到场、异常处置、结算和关闭均为强制能力；同角色动作集合、权限、状态、完成条件和错误恢复同源。

## 明确不做

- 不新增自动审批/自动付款/自动删除；
- 不在首版铺满全部主材品类；
- 不把业务规则复制进产品代码形成第二事实源。
