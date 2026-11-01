# 体验日志

| Step | 端面 | 角色 | 页面目标 | 页面回读 | 异常/恢复 | 证据 |
| --- | --- | --- | --- | --- | --- | --- |
| S01-S04 | Web | Boss | 客户、房屋、转交、指派 | 客户和两套房屋建立，业务员/设计师可见 | 员工选择器白名单修复后恢复 | S01-S04 |
| S05-S10 | Web | Marketer/Designer | 首联、2栋201、量房 | 客户 Serving，2栋201 Serving | 多房屋显式选择 | S05-S10 |
| S12-S19 | Web | Designer | 方案、6分项、报价、客户接受 | 报价 Accepted，金额 ¥1,026.69 | 变更预览白名单修复 | S12-S19 |
| S20-S22 | Web/App | Boss | 合同发起/生效/项目生成 | Contract Effective，Project Ongoing | App 合同页回读 | S20-S22 |
| S25-S34 | App/Web | Designer/Finance/Pm | 选材、作业单、采购、到场、材料结算 | 瓷砖 1㎡，供应商 ID 正确，结算 ¥88 | 结算 supplierId 与下单选择器修复 | S25-S34 |
| S35-S41 | Web | Pm | 开工、任务、留证 | 任务 Done，阶段 Accepting | 响应白名单修复后刷新回读 | S35-S41 |
| S42-S46a | Web | Pm | 提交阶段验收 | 第 1 轮 Pending，员工无代确认按钮 | 响应白名单修复 | S42-S46a |
| S48-S63 | App | 业主 | 项目码/手机号进入 | 当前微信号非业主预留号，403 | `BLOCKED_NEEDS_USER` | S48-S51/S63 |
| S50-S58 | Web | Finance | 项目账本、结算 Draft | 合同/回款/待收/成本/结算回读；成本未完整禁确认 | 白名单、瞬态完整度、前后端门禁修复 | S50-S58 |
| S59-S62 | Web | Finance | 登记收款 | 三应收节点收清，待收 ¥0 | 页面直接核销 | S59-S62 |
| S64-S69 | Web/App | Finance/Boss/Store/Pm | 跨角色/跨端回读 | 项目金额一致；财务首页现金流一致；验收 Pending | App 首页取整差异 | S64-S69 |
