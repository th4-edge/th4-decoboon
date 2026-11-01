# 候选提交冻结记录

时间：`2026-11-02T01:02:00+08:00`

## 已进入本地 dev 的最小提交

| 仓库 | 提交 | 内容 |
| --- | --- | --- |
| th4-decoboon-web | `a6e98ee` | Web 收款冲正入口与收款状态/原因模型，新增冲正抽屉 |
| th4-decoboon | `64abe92e` | 结算列表统一使用 ProjectCostContext 成本完整性投影，新增定向测试 |
| th4-decoboon-web | `54d4fb2` | 报价模型归一化并展示 acceptedBy/acceptedAt 与员工登记边界 |
| th4-decoboon | `7dd71442` | `quote/designer/info` 白名单加入 acceptedBy，补合同结构测试 |
| th4-decoboon-web | `3b2e220` | 店长独立客户360页面、CRM overview 同源投影、内容归因 Provider 与路由 |
| th4-decoboon | `a810ab82` | Provider 静态响应契约审计脚本、发布构建接线与 137 个 Provider 白名单注解 |

## 干净提交复验

使用独立 worktree 从提交本身执行，不依赖主工作区未提交文件：

- 后端：`mvn -q -Dtest=FinanceSettlementPageCostProjectionTest,QuoteViewContractTest test`，PASS。
- Web：`npm run build`，PASS，包含 type-check、分页门禁和客户端文案门禁。
- 干净提交 Provider 契约检查：`providers=541 properties=536 allow=534 findings=0`，self-test PASS。
- Provider 检查脚本与发布构建接线已随 `a810ab82` 入库，干净 worktree 编译和定向测试 PASS。

## 已拒绝的错误提交

- CHG-20261102-002 最初仅路由提交被拒绝：它依赖当时尚未提交的客户360页面实现。
- 已通过 `CHG-20261102-004` 建立最小依赖闭包：页面、CRM overview、内容归因 Provider、店长路由一并提交为 `3b2e220`，干净 worktree build 通过。

## 当前工作区

- 后端仍有 604 个未提交路径（290 个已跟踪修改、314 个未跟踪）。
- Web 仍有 175 个未提交路径（152 个已跟踪修改、23 个未跟踪）。
- 以上均可能包含其他会话成果，不能整目录提交。
- 本记录不包含 push、正式发布或生产部署。

## 下一步

1. 继续 `PI-20261022-015 MAT-X2`：补齐 Web 作业单新建/编辑/测量/提交/取消。
2. App/Owner 授权身份可用后，新 RUN 复验 CRM 与材料跨端分支。
3. 继续分拆剩余混合工作区，保持不得整目录提交。
