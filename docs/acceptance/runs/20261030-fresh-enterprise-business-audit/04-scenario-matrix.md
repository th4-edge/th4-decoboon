# 场景与状态矩阵

> 数据固定使用昭青科技 `com_id=199500000000000001`。业务对象由正式 Provider 造数，页面读取和负向检查为真实执行；由于关键创建动作未全部由页面按钮完成，本表不把 Provider 造数冒充完整 UI 闭环。

| 轮次 | 场景 ID | 客户 | 当前状态 | 当前阶段 | 覆盖区域 | 发起角色 | 接收角色 | 端面 | 页面结果 | 接口结果 | 数据库结果 | 幂等/并发 | 证据 | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | FRESH-S01 | 王芳（造数1100） | Serving | Preparing | 客户、报价、合同、项目、财务 | Boss | Finance/StoreManager | Web + App | 客户、项目、金额链可见 | Provider 响应成功 | 1 客户/1 项目/1 合同 | 定向自动化测试 | EV-006~010 | PASS_READ_ONLY |
| 2 | FRESH-S02 | 王芳（造数1100） | Serving | Preparing | 财务汇总与分页 | Finance | Boss | Web | 应收/收款/应付/付款/台账全部有真实行 | 6 类分页均合法 JSON | 金额与不变量一致 | 收付款幂等/并发单测通过 | EV-002/012/013 | PASS_AFTER_P0_FIX |
| 3 | FRESH-S03 | 王芳（造数1100） | Serving | Preparing | 成本完整性 | Finance | Boss | App + Web | 实际成本 ¥800，显示“成本未完整”，利润不展示 | `costComplete=false`、利润 `null` | 预计成本 ¥1,760 | 不适用 | EV-003/004 | PASS |
| 4 | FRESH-S04 | 王芳（造数1100） | Serving | Preparing | 店长门店范围 | StoreManager | Boss | App + Web | 总店客户 1、待开工项目 1、台账 4 | 店长报告修复后合法 JSON | 客户/项目/财务均归总店 | 跨店请求返回 envelope 403 | EV-007/008/009/016 | PASS_AFTER_P0_FIX |
| 5 | FRESH-S05 | 王芳（造数1100） | Serving | Preparing | 客户与房屋面积 | Boss/StoreManager | Finance | Web/App | 客户卡面积已与房屋主档一致为 100㎡ | Provider 更新成功 | 客户 100㎡、房屋 100㎡ | 不适用 | EV-008/011 | PASS_FIXTURE_ALIGNED |
| 6 | FRESH-S06 | 王芳（造数1100） | Serving | Preparing | 报价→合同→项目业务链 | Designer/Marketer | Boss/Finance | Provider + 页面回读 | 报价、合同、项目均能回读 | 合同生效并生成项目 | 1 报价/1 合同/1 项目/8 阶段/2 任务 | 未做页面并发 | EV-006/011 | PARTIAL_PROVIDER_FIXTURE |
| 7 | FRESH-S07 | 王芳（造数1100） | Serving | Preparing | 材料计划 | Boss/Finance | PM | App/Web | 首页显示材料计划 1 条、待下单 | 旧采购脚本 Provider 不存在 | 仅材料计划，无采购/到场/应付 | 未测试 | EV-006/012 | PARTIAL_BLOCKED_UPSTREAM |
| 8 | FRESH-S08 | 王芳（造数1100） | Serving | Preparing | 财务高危动作 | StoreManager | Finance | Web | 店长台账可见“更正/删除” | 未执行高危写操作 | 未产生新事实 | 未测试 | EV-009/017 | BLOCKED_BUSINESS_DECISION |
| 9 | FRESH-S09 | 王芳（造数1100） | Serving | Preparing | 非法 JSON 回归 | App/Web | — | App + Web | 修复前请求失败/伪 0；修复后页面恢复 | 修复前截断，修复后合法 | 数据未变 | 83 项定向回归 | EV-012~015 | FIXED_PENDING_ACCEPTANCE |

## 结论

- 核心客户→报价→合同→项目→应收/收款→应付/付款→台账读取链通过；
- 完整页面创建、材料履约和验收闭环未完成；
- 所有场景最高只能记 `PARTIAL`，不得宣称八大区域完整覆盖。
