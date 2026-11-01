# BUG 缺陷台账

| ID | 严重度 | 状态 | 端面 | 角色 | 最小复现 | 期望 | 实际 | 证据 | 责任建议 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BUG-PAGE-001 | P0 | FIXED | Web/API | Boss/Finance | 打开发员工选择器或应付分页 | 合法 JSON，可选 | 嵌套白名单不足，JSON 截断/无数据 | S03-S04；`EmployeePageSerializationContractTest` | 后端白名单 |
| BUG-PAGE-002 | P0 | FIXED | Web/API | Designer/Boss | 打开合同变更预览 | 显示减项 -¥168 | Project.property 白名单缺失，响应截断 | S30；`DetailResponseWhitelistContractTest` | 后端白名单 |
| BUG-PAGE-003 | P0 | FIXED | Web/API | Finance | 打开应付分页 | Partner 摘要合法序列化 | Partner 白名单缺失 | `FinancePageAggregateWhitelistContractTest` | 后端白名单 |
| BUG-PAGE-004 | P0 | FIXED | Web/API | Finance | 主材结算候选行点击结算本单 | 使用真实 supplierId 分组并确认 | 响应未返回 supplierId，页面提示“供应商待确认” | S28/S33/S34 | 后端白名单 + 前端 PartnerPicker |
| BUG-PAGE-005 | P0 | FIXED | Web/API | Pm | 点击开始任务 | 任务 Doing 并返回合法响应 | 数据库已 Doing，但页面提示“请求失败” | S37/S37a | Task Update Property 白名单 |
| BUG-PAGE-006 | P0 | FIXED | Web/API | Pm | 提交阶段验收/刷新验收页 | 生成 Pending 且页面可回读 | 数据库已生成 Pending，但页面提示“请求失败”；分页也依赖 Property 白名单 | S46/S46a | 验收 Save/Page/Update 白名单 |
| BUG-PAGE-007 | P0 | FIXED | Web/API | Finance | 生成项目结算 | 返回 Final/成本字段并稳定回读 | DB 已生成 Draft，页面无成功反馈；结算 Provider 房屋白名单缺失 | S53/S54 | 结算 Page/Save/Update 白名单 |
| BUG-PAGE-008 | P1 | FIXED | Web/API | Finance | 打开项目结算 Tab | 显示最终金额、质保金、成本完整性 | 最终金额显示 0，结算成本完整度默认 true/0 | S54/S56/S57 | 项目报告 Settlement 白名单 + 瞬态字段回填 |
| BUG-PAGE-009 | P1 | FIXED | Web/API | Finance | 成本未完整时尝试确认结算 | 前端禁用，后端拒绝 | 原可点击且后端未阻断 | S58；`CHG-20261030-038` | 成本门禁 |
| BUG-PAGE-010 | P1 | FIXED | Web | Pm | 进入阶段验收工作区 | 默认指向当前待验收阶段 | 原默认指向“本项不涉及”的第一阶段 | S65 | 前端默认阶段选择 |
| GAP-PAGE-011 | P1 | FIXED / PENDING_INDEPENDENT_ACCEPTANCE | Backend | Finance | 地板有效减项后计算项目成本完整性 | 已取消材料不再制造 Missing | 已按有效 Remove 从分母剔除；修复后仅剩 1 项真实人工成本缺失 | S70-S72；CHG-20261031-039 | 已修复，待独立验收 |
| BLOCK-PAGE-012 | P0 | BLOCKED_NEEDS_USER | App | 业主 | 用当前开发者工具身份进入业主项目码 | 业主预留手机号本人确认 | 当前微信手机号 186****9918 ≠ 客户预留 137****9999，服务端 403 | S63 | 必须由业主本人操作 |
