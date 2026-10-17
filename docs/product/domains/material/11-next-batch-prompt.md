# 材料域下一批提示词（B3）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`；
3. 读取 `state.json`、`00-charter.md`、`10-reconciliation.md`、`03-blueprint.json`、`04-requirements.md`、`05-plan.md`；
4. 读取 `06-implementation.md`、`07-ui-review.md` 和 `evidence/B2/`，核验 B2 是否仍为 `RECONCILED`；
5. 涉及材料对象、权限、金额或成本口径时，读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 及其规范事实源。

执行命令：

```bash
python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py show --domain material
python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py next-prompt --domain material
python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py validate --domain material
```

当前预期：B2 已完成 `DEV_VERIFIED + UI_REVIEWED + RECONCILED`，独立验收仍留 B5。若实际状态不同，以 `state.json` 为准。

## 本轮只做 B3

目标：App 业务体检 + 页面上下文说明。

范围：

- 作业单详情/建单页解释“测量/配送/安装/要求到场/实际到场”的时间语义；
- 作业单状态、价格状态、选材门禁、模板必填字段、取消边界和下一步动作；
- 项目详情材料体检的 App 投影，与 Web `blueprint/check/report` 同源同口径；
- 复用既有 Provider，不新增业务规则、不新增 DDL、不改变金额/成本/权限语义。

硬约束：

- 不改 App 正式版，不做微信正式提审/发布；
- 不伪造 `done:true` 或本地完成态；
- 失败显示“暂不可确认”并保留恢复路径；
- 页面解释必须引用真实对象、状态和业务编号；
- 不顺手推进 B4 助手问答或 B5 独立验收。

建议步骤：

1. 核对 App 当前实现和 Provider 路由；
2. 用真实 App/开发者工具渲染，至少覆盖作业单详情、测量/时间字段、旁路说明、项目材料体检；
3. 定向测试 + App 构建/类型检查；
4. 截图、交互和异常证据落到 `docs/product/domains/material/evidence/B3/`；
5. 更新 `06-implementation.md`、`07-ui-review.md`、`10-reconciliation.md`、`state.json/history`；
6. 有代码变更时只提交本批相关文件到 `dev` 并推送，严禁碰既有未提交迁移模板和材料蓝图 PNG；
7. 停在 `UI_REVIEWED` 或 `RECONCILED`，不得宣称 `ACCEPTED`。

## 返回包

- 已完成批次与证据路径；
- 未闭环项与原因；
- 自主假设/临时决策；
- 需要用户决策的事项；
- 下一批 B4 提示词；
- 是否可进入独立验收；
- 回滚/恢复方式。
