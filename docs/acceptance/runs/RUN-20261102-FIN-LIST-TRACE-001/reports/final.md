# RUN-20261102-FIN-LIST-TRACE-001 验收结论

## 结论

`CONDITIONAL_PASS + PRODUCTION_SMOKE_PASS`

- 本次财务列表可追溯性热修的 Web/Provider 定向验收通过。
- 冻结核心用例要求的 App、Boss 回读和收付款写闭环未在本次只读验收中执行，因此三条冻结用例整体标记 `BLOCKED`，不能宣称完整业务验收通过。
- 后端/Web 已发布生产 `20261102-171937`，生产财务账号登录后只读冒烟通过。

## 已通过

1. 应收列表与选择器：服务端分页、`filter=overdue`、`receivableId` 精确回显通过。
2. 应付列表与选择器：服务端分页、`filter=actionable/urgent/dueSoon`、`payableId` 精确回显通过。
3. 项目资金台账：页签明确最近 12 条，完整入口保留 `projectId`，独立台账未默认限制当前月。
4. 提成负向调整：页面请求 `current=1,size=20`。
5. 月度考勤：抽屉请求 `month/current/size=20`，显示服务端总数。
6. 财务工作台：前 8/前 5 条列表均明确“最多”语义，不再把列表长度当成总笔数。
7. 后端全量测试在停止并发本地服务后单独执行：1226/1226 通过，失败 0、错误 0。
8. 首次全量测试与本地后端/Vite 并发共用工作区时出现 17 个测试上下文 Bean 缺失；停止并发进程后同一提交全量通过，不作产品失败计。

## 未覆盖

- App 端回读；
- 收款、付款、冲正、删除等写操作；
- Boss 跨角色回读；
- 完整冻结核心用例要求的部分写操作和生产 Boss 回读仍未执行；已由生产财务只读冒烟覆盖本次热修目标。

## 证据

- `evidence/api-acceptance.json`
- `evidence/browser-acceptance.json`
- `evidence/receivable-picker-overdue.png`
- `evidence/project-ledger-context.png`
- `evidence/workbench-truncation-labels.png`
- `evidence/commission-adjustments-page.png`
- `evidence/attendance-pagination.png`
- `evidence/production-smoke.json`
- `evidence/prod-workbench-truncation-labels.png`
- `evidence/prod-project-ledger-context.png`
- `evidence/prod-receivable-picker-overdue.png`
