# 候选提交冻结记录

时间：`2026-11-02T01:02:00+08:00`

## 已进入本地 dev 的最小提交

| 仓库 | 提交 | 内容 |
| --- | --- | --- |
| th4-decoboon-web | `a6e98ee` | Web 收款冲正入口与收款状态/原因模型，新增冲正抽屉 |
| th4-decoboon | `64abe92e` | 结算列表统一使用 ProjectCostContext 成本完整性投影，新增定向测试 |
| th4-decoboon-web | `54d4fb2` | 报价模型归一化并展示 acceptedBy/acceptedAt 与员工登记边界 |
| th4-decoboon | `7dd71442` | `quote/designer/info` 白名单加入 acceptedBy，补合同结构测试 |

## 干净提交复验

使用独立 worktree 从提交本身执行，不依赖主工作区未提交文件：

- 后端：`mvn -q -Dtest=FinanceSettlementPageCostProjectionTest,QuoteViewContractTest test`，PASS。
- Web：`npm run build`，PASS，包含 type-check、分页门禁和客户端文案门禁。
- 主工作区 Provider 契约检查：`providers=547 properties=542 allow=540 findings=0`，self-test PASS。
- 注意：Provider 契约检查脚本当前仍未被版本库跟踪，因此干净提交 worktree 无法执行该命令；这是发布门禁的可复现性问题，需后续单独入库，不能混入本缺陷提交。

## 已拒绝的错误提交

- CHG-20261102-002 店长客户360路由：当前 retest 依赖尚未提交的 `StoreCustomerView.vue` 客户360实现。
- 曾生成仅含两条路由改动的临时提交，随后从本地 dev 回退，未保留。
- 原因：只提交路由会让干净版本“能进入页面”但页面不具备已验证的客户360能力，属于假修复。

## 当前工作区

- 后端仍有 604 个未提交路径（290 个已跟踪修改、314 个未跟踪）。
- Web 仍有 175 个未提交路径（152 个已跟踪修改、23 个未跟踪）。
- 以上均可能包含其他会话成果，不能整目录提交。
- 本记录不包含 push、正式发布或生产部署。

## 下一步

1. 将 `StoreCustomerView.vue` 客户360实现按独立 CHG 拆分、审查、验证并提交，再重做 CHG-002。
2. 处理未被版本库跟踪的 Provider 契约检查脚本，确保发布门禁可从干净提交复现。
3. 继续 `PI-20261022-015 MAT-X2`：补齐 Web 作业单新建/编辑/测量/提交/取消。
