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
| REQ-MAT-013 | 财务能在 App 内从待下单作业单完成采购下单 | 作业单已提交请购 | Finance/Boss/StoreManager | Sheet/Plan/Purchase/Payable | App 主材下单工作台/下单页 | material/sheet/order | 多供应商、部分下单、可空单价、暂估应付正确 | SCN-021/DEC-072/DEC-073 |
| REQ-MAT-014 | 财务能在 App 内完成材料结算并收敛成本 | 采购明细已到场 | Finance/Boss/StoreManager | PurchaseOrderItem/MaterialSettlement/Payable | App 主材结算 | material/settlement/preview/confirm/cancel | 逐单/月结互斥、撤销回退、成本完整/利润 null 正确 | BR-045/DEC-073/DEC-074 |
| REQ-MAT-015 | App 不再提供材料计划直接采购入口 | 登录员工在 App 材料/供应商页面 | 按现有权限 | MaterialPlan/PurchaseOrder | App 材料计划/供应商 | 旧 `project/purchase/save`/`batch` 下线 | 旧入口不可达，历史采购单仍可查/到场 | 2026-10-17 用户裁决 |

## 非功能要求

- 列表/体检接口响应体最小字段白名单；
- 统计/计数使用聚合投影，不在循环内查库；
- 助手回答不得直接查生产库，使用 Provider 工具；
- 所有高风险动作只读或需人工确认；
- App 是材料下单与结算的闭环主入口，Web 不是必经节点；App/Web 对同一对象的状态/下一步口径仍须一致。

## 明确不做

- 不新增自动审批/自动付款/自动删除；
- 不在首版铺满全部主材品类；
- 不把业务规则复制进产品代码形成第二事实源。
