# 合同与增减项履约 · 下一批提示词（B2）

继续 `contract-change`。先读取：

1. `AGENTS.md`
2. `docs/operations/试运营单人轻量协作与发版规范.md`
3. `.agents/skills/domain-delivery-orchestrator/SKILL.md`
4. `docs/product/domains/contract-change/state.json`
5. `run-envelope.md`、`00-charter.md`、`03-blueprint.json`、`10-reconciliation.md`
6. 业务事实：`BR-035`、`BR-041`、`BR-045`、`DEC-055`、`DEC-068`、`DEC-072`、`DEC-073`、`GAP-038`、`GAP-049`

当前阶段：`BLUEPRINT_READY`。B2 只有在用户明确授权后才能开始。

## B2 垂直切片

只处理合同变更的技术收敛，不扩到验收、发布或其它业务域：

1. 冻结唯一写入口：旧 `project/contract-change/save` 与 `project/contract-change/batch-save` 的处置方案；
2. 核对 App 报价详情旧调用入口，迁移到 batch-save 或明确下线；
3. 核对 `project/contract-change/range`、`preview`、`batch-save` 是否完全同源；
4. 核对目标 `CatalogItem` 快照、来源剩余快照、待审批占用；
5. 核对审批时合同/项目/应收/任务/材料/成本同事务联动；
6. 核对历史 `Approved`/`CustomerConfirmed` 与 customer-confirm 仅历史兼容；
7. 在干净基线上运行合同变更定向测试；
8. 更新 `06-implementation.md`、`10-reconciliation.md`、`state.json/history`。

## 人工门禁

- 不得新增业务规则；
- 不得改变金额/成本/权限语义；
- 不得新增 DDL/迁移，除非用户明确确认；
- 不得删除历史数据；
- 不得自动审批/付款/改权限；
- 不得把代码存在或构建通过当作业务验收。

## 退出条件

- 写入口唯一或双入口有明确兼容边界；
- 目标/来源快照和差额同源；
- 定向测试在干净基线 PASS；
- 对账矩阵无 `UNMAPPED`/`STALE`；
- 停在 `DEV_VERIFIED`，真实页面证据和独立验收仍由后续批次完成。

## 返回包

- 已完成内容与改动文件；
- 测试命令与结果；
- 双入口处置结论；
- 未闭环项和风险；
- 下一批 UI 审查提示词；
- 是否需要用户决策。
