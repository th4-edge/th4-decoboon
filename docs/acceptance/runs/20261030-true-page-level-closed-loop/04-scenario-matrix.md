# 场景与状态矩阵

| 轮次 | 场景 ID | 客户/对象 | 当前状态 | 当前阶段 | 覆盖区域 | 发起→接收 | 端面 | 页面结果 | 对账结果 | 证据 | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | S01 | PAGE-20261030 页面闭环客户 | New→Serving | 客户建档 | 客户状态 | Boss→Marketer | Web | 新增客户、建 1栋101、转交业务员、指派设计师 | DB 客户/两房屋存在 | S01-S04 | PASS |
| 2 | S02 | 2栋201 | Measuring→Serving | 首联/量房 | 客户状态 | Marketer→Designer | Web | 首联、补充 2栋201、安排量房、设计师完成量房 | 客户 Serving，两房屋显式 | S05-S10 | PASS |
| 3 | S03 | Design/Quote `241938042028294144` / `241938621509140480` | Selected/Accepted | 设计方案/报价 | 设计方案、方案报价 | Designer→Boss | Web | 方案、6 分项、报价、客户接受登记 | 原合同 ¥1,026.69 | S12-S19 | PASS |
| 4 | S04 | Contract `241939522005237760` | Signed/Effective | 合同/项目生成 | 方案报价 | Designer→Boss | Web + App | Web 发起签约，App 生效，项目生成 | 项目 `241940507872198656` | S20-S22 | PASS |
| 5 | S05 | 材料选材/作业单/采购 | Arrived/Settled | 选材→采购→到场→结算 | 材料选材、材料计划 | Designer→Finance→Pm | App + Web | 2 主材选材；瓷砖作业单、财务下单、PM 到场、财务结算 ¥88 | 采购 `PO278...`，供应商 ID 正确 | S25-S34 | PASS（修复后） |
| 6 | S06 | 变更 `1793373550448924493` | CustomerConfirmed | 减项生效 | 增减项 | Designer→Boss | Web | 发起地板减项并发起审批，Boss 生效 | -¥168，当前合同 ¥858.69 | S30-S31 | PASS |
| 7 | S07 | Pm 项目工作面 | Ongoing | 拆除改造 | 施工进度 | Pm | Web | 开启阶段、任务开始/提交/完成、上传测试留证 | 任务 Done，阶段 Accepting | S35-S41 | PASS |
| 8 | S08 | 验收 `241948201022652416` | Pending | 拆除改造 | 施工进度 | Pm→业主 | Web + App | 提交第 1 轮阶段验收，显示待业主确认 | DB Pending | S42-S46a, S65 | PARTIAL/BLOCKED |
| 9 | S09 | 应收 3 节点 | Received | 项目收款 | 财务闭环 | Finance | Web | 收款 ¥410.68 + ¥140.00，待收 ¥0 | 应收/收款/台账一致 | S59-S62 | PASS |
| 10 | S10 | 项目结算 `241949278535155712` | Draft | 成本未完整 | 财务闭环 | Finance | Web + App | 最终金额 ¥858.69、质保金 ¥25.76、利润空；确认按钮禁用 | 后端拒绝确认；成本缺失 1 项（真实人工成本） | S54-S58, S64 | PARTIAL/BLOCKED |
| 11 | S11 | 跨角色回读 | Accepting | 项目验收中 | 财务/经营 | Boss/StoreManager/Finance | Web + App | 合同、回款、待收、应付、成本、结算跨角色一致 | `coreBalanced=true` | S66-S69, SQL | PASS（资金）/PARTIAL（成本） |
