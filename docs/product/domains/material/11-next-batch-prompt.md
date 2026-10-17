# 材料域下一批提示词（B4）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`；
3. 读取 `state.json`、`00-charter.md`、`10-reconciliation.md`、`03-blueprint.json`、`04-requirements.md`、`05-plan.md`；
4. 读取 `06-implementation.md`、`07-ui-review.md`、`evidence/B2/`、`evidence/B3/`；
5. 涉及材料对象、权限、金额/成本口径时，读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 及其规范事实源。

执行命令：

```bash
python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py show --domain material
python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py next-prompt --domain material
python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py validate --domain material
```

当前预期：B2/B3 已完成 `DEV_VERIFIED + UI_REVIEWED + RECONCILED`，B5 独立验收仍未开始。以 `state.json` 为准。

## 本轮只做 B4

目标：只读材料业务助手。

范围：

- 解释材料计划、选材、作业单、采购、到场、结算、退货、增减项和成本完整性；
- 基于 B1 蓝图字段/规则、B2 `blueprint/check/report`、B3 页面上下文；
- 50 个黄金问题；
- 权限与反向验证：跨租户、越权、无证据拒答、成本不完整不输出伪利润；
- 只读工具，不自动审批/付款/删除/改权限。

硬约束：

- 不新增业务规则；缺证据时拒答或提示“暂不可确认”；
- 金额/成本/利润由领域服务或确定性规则给出，不由模型自由计算；
- 助手不得直接查库，只调用 Provider/领域上下文；
- 不把 `gap/stale/draft` 内容当正式回答；
- 不进入 B5 独立验收，不发布正式小程序。

建议步骤：

1. 先列 50 个黄金问题和期望证据/拒答类型；
2. 设计只读工具清单和权限矩阵；
3. 实现最小助手运行时与审计记录；
4. 做确定性校核、证据引用、权限负向和成本边界测试；
5. 证据落 `evidence/B4/`（问题集、响应样本、权限结果、失败/拒答）；
6. 更新 `06-implementation.md`、`07-ui-review.md`、`10-reconciliation.md`、`state.json/history`；
7. 有代码变更只提交本批相关文件到 `dev` 并推送，严禁碰既有未提交迁移模板与材料蓝图 PNG。

## 返回包

- 已完成批次与证据路径；
- 未闭环项与原因；
- 自主假设/临时决策；
- 需要用户决策的事项；
- 下一批 B5 独立验收提示词；
- 是否可进入独立验收；
- 回滚/恢复方式。
