# CHG-20261029-023 · 财务角色业务域与能力体系梳理

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261029-023 |
| 类型 | 需求 |
| 风险级别 | L0 |
| 状态 | 已关闭 |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `FIN-Q01~08`、`REQ-BIZ-20261029-002`、`PI-20261029-001`、`DEC-016/065/075/079/080/081/084/089/091/092/093` |

## 2. 目标与范围

- 目标：以现有线上产品为基线，重新梳理财务角色的企业业务域、业务能力和页面投影，并建立现有 Web 24 个 Finance 菜单与小程序 6 个财务角色 Tab、财务首页、19 个快捷入口的任务链验收台账。
- 范围内：财务域 domain packet、六个真实场景、能力地图、对象/事件模型、页面母型、共享组件、当前 App/Web/后端对账、UI 基线证据、业务咨询单、双端现有入口验收台账和用户渐进升级决策。
- 非范围：业务状态/金额/权限变更、后端 Provider/Java 业务逻辑调整、DDL、财务页面整体重做、导航重构、独立验收和发布。
- 受影响端：后端仓库文档；App/Web 仅做只读盘点。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是（仅梳理与引用，不修改业务事实）
- 业务事实来源与裁决编号：`DEC-016`、`DEC-065`、`DEC-075`、`DEC-079`、`DEC-080`、`DEC-081`、`DEC-084`、`DEC-089`、`DEC-091`、`DEC-092`、`DEC-093`、`BR-050/055/057/058`
- 尚存 GAP 或假设置：`FIN-Q01~08`、`GAP-070`、`GAP-071`、`INFO-FINANCE-004` 冲突

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 财务业务域框架可恢复 | 读取 `state.json` | 能识别 phase、批次、阻塞、下一动作 | `docs/product/domains/finance/state.json` |
| 2 | 财务能力不按菜单堆砌 | 对照 `DEC-016/065/079/080/084/093` | 区分财务主责、共管、控制、观察和排除 | `01a-business-capability-blueprint-v0.1.md` |
| 3 | 页面与功能可复用 | 对照能力地图 | 财务域页面与客户/报价/合同/项目/材料/薪酬主域页面分离 | `01b-capability-map-v0.1.json`、`10-reconciliation.md` |
| 4 | 业务问题可交接 | 读取 T05 | 有 MECE 的业务问题和影响说明 | `REQ-BIZ-20261029-002` |
| 5 | 不误报 UI 验收 | 读取 UI 文档 | 明确历史截图、机器预检和当前真实渲染的差异 | `07-ui-review.md` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 未提交 | `docs/product/domains/finance/**`（含 `11-menu-acceptance.md`、`12-app-finance-entry-acceptance.md`）、`USER-DEC-20261029-007`、决策 registry/README、`DOMAIN-REGISTRY.md`、`PROGRAM-BOARD.json`、`docs/operations/change-records/CHG-20261029-023-*`、当前项目/工作记录 |
| 业务智能体 | 本地目录 | 未提交 | `02-采集与咨询/REQ-BIZ-20261029-002-*` |
| th4-decoboon-web | dev | 只读 | 财务页面与历史截图盘点 |
| th4-decoboon-app | dev | 只读 | 财务页面盘点 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| Domain packet | `python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py validate --domain finance` | PASS | `docs/product/domains/finance/state.json` |
| 能力地图 JSON | `python3 -m json.tool docs/product/domains/finance/01b-capability-map-v0.1.json` | PASS | 同文件 |
| State JSON | `python3 -m json.tool docs/product/domains/finance/state.json` | PASS | 同文件 |
| 业务知识库 | `python3 05-自动化/knowledge.py validate && python3 05-自动化/knowledge.py index` | PASS；286 张知识卡/404 条证据 | 业务智能体 `06-状态/` |
| 真实页面/接口冒烟 | 本轮未执行；只读取历史截图和现有审计 | 不适用/未执行 | `07-ui-review.md` |
| 负向/越权/边界 | 本轮为文档梳理，未执行运行验证 | 不适用 | `10-reconciliation.md` |

- 未执行项及原因：本批为 L0 产品文档梳理，不涉及代码运行；UI 当前真实渲染留待 B2。
- 已知失败及是否与本次相关：历史财务汇总/列表金额差异、成本/利润与发票口径仍存在，已登记，不在本次修复。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 不适用 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：文档结论与正式业务事实冲突且无法通过 T06 修订。
- 回滚命令/步骤：回退本变更记录、`docs/product/domains/finance/` 新增文件和业务咨询单；不影响运行系统。
- 回滚验证：`domain_state.py validate` 重新确认 domain packet 状态。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是（文档 JSON/state 校验）
- 返工原因：无
- 最终结论：已关闭（业务框架梳理完成，等待业务对齐）

## 10. 复盘与沉淀

- 是否出现重复问题：是；再次确认“角色能力直接映射为菜单”是重复出现的信息架构问题。
- 是否需要新增门禁：否；沿用 domain packet、业务咨询、Provider 对账和 UI Craft 渲染门禁。
- 候选 `EXP/ANT/PATH`：候选反模式：`角色能力菜单化`、`财务域复制跨域对象`、`指标卡代替经营结论`。
- 后续动作与负责人：按 `11-menu-acceptance.md` 与 `12-app-finance-entry-acceptance.md` 对 Web 和小程序双端现有入口逐项验收；业务冲突再按需请求 T06/T12，后端缺口另行裁决。
