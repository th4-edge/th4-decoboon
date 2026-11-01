# CHG-20261030-002 · 前端 Provider 契约巡检与漂移门禁

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-002 |
| 类型 | 工程治理 / 跨端契约 |
| 风险级别 | L2 |
| 状态 | 开发自测通过，待用户确认 |
| 负责人 | codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | 前端架构审查 P0：跨端 Provider 契约仍依赖人工映射 |

## 2. 目标与范围

- 目标：把 App/Web 的 Provider 路由消费关系从人工文档升级为可生成、可检查的机器基线。
- 范围内：
  - 后端 `@Service` 路由扫描；
  - App/Web 领域 API 路由调用点扫描；
  - 共享、App-only、Web-only、未匹配路由分类；
  - 路由与动态调用点漂移检查；
  - 26 个 Web-only 写/命令能力的证据化评审清单；
  - 严格模式 `--strict-web-only`；
  - 负向自测和架构说明。
- 非范围：
  - 修改任何业务 Provider 或前端运行时代码；
  - 裁决 Web-only 能力是否合法；
  - 请求/响应字段、枚举、错误码、金额、权限和状态机完整对账；
  - 接入生产发版脚本。
- 受影响端：后端工程脚本；App/Web 仅作为只读扫描输入。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否，本次只读取现有路由。
- 业务事实来源与裁决编号：不适用。
- 尚存 GAP：
  - 24 个 Web-only 写/命令能力已全部建立评审状态，其中 19 项仍未关闭；
  - 27 个动态 URI 调用点待逐步收敛；
  - 字段级、权限级、错误码级和金额级契约待下一阶段补齐。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | App/Web 使用同一后端路由 | 分类为 `shared` | `--write` 基线 |
| 2 | 前端使用后端未声明路由 | `--check` 失败 | `--self-test` 负向夹具 |
| 3 | 路由新增、移除或端侧归属变化 | `--check` 失败 | `--self-test` 与漂移比较 |
| 4 | 动态 URI 调用变化 | 记录并触发基线漂移 | `--self-test` 动态夹具 |
| 5 | 当前仓库执行检查 | 当前基线与事实一致，退出码 0 | `--check` 输出 |
| 6 | 本地同名 `put()` 不误判 | 仅扫描从 HTTP 适配器导入的方法 | 公式辅助文件未进入动态调用清单 |
| 7 | Web-only 写能力评审覆盖 | 24/24 有状态、理由、证据和下一步 | `app-web-provider-reviews.json` |
| 8 | 严格模式能识别未关闭债务 | `--check --strict-web-only` 返回 1 | 19 项 open debt |
| 9 | 动态 URI 不造成共享误判 | `project/contract/workbench` 识别为 App/Web 共享只读路由 | 基线分类 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | `scripts/audit-frontend-provider-contract.py`、`docs/design/app-web-provider-contract.json`、`docs/design/app-web-provider-reviews.json`、架构说明、本变更记录 |
| th4-decoboon-app | `dev` | 不适用 | 只读扫描 |
| th4-decoboon-web | `dev` | 不适用 | 只读扫描 |

## 6. 验证

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| Python 语法 | `python3 -m py_compile scripts/audit-frontend-provider-contract.py` | PASS |
| 负向自测 | `python3 scripts/audit-frontend-provider-contract.py --self-test` | PASS：共享、未匹配后端、动态路由三类夹具通过 |
| 基线生成 | `python3 scripts/audit-frontend-provider-contract.py --write` | PASS：542 后端业务路由，466 前端唯一路由，0 未匹配 |
| 漂移检查 | `python3 scripts/audit-frontend-provider-contract.py --check` | PASS；评审覆盖 26/26 |
| 当前统计 | 后端 542；共享 217；App-only 207；Web-only 42；Web-only 写 24；动态调用 27；领域外直接传输 0 | PASS |
| 评审分类 | 已关闭 5；`PARITY_REQUIRED` 12；`WEB_FIRST_PENDING_REGISTRATION` 7 | 19 项 open debt |
| 严格模式负向验证 | `python3 scripts/audit-frontend-provider-contract.py --check --strict-web-only` | PASS：按预期返回 1，并列出 19 项未关闭债务 |

- 未执行项及原因：未运行 App/Web 类型检查与构建；本次不修改两端源码或依赖，扫描器只读。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 当前后端工作区 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：脚本误报导致正常 Provider 变更无法通过，或基线被错误更新。
- 回滚步骤：删除新增脚本、基线、架构说明和本变更记录。
- 回滚验证：不影响任何 App/Web/后端运行时。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；首次扫描后修正本地 `put()` 误报、核心 HTTP 自调用误报、动态 URI 共享路由误判和读取能力分类。
- 返工原因：扫描器需限定真实 HTTP 导入；动态路由变量需要结合后端已知路由推断；`workbench` 属于读取能力。
- 最终结论：Provider 路由级基线、24 项 Web-only 写能力评审清单和严格模式已建立；5 项已关闭，19 项债务显式保留，待逐项业务/研发闭环。

## 10. 复盘与沉淀

- 是否出现重复问题：跨端契约此前只靠人工文档，路由事实与文档长期可能漂移。
- 是否需要新增门禁：是；本次新增路由级漂移门禁及负向自测。
- 候选 `EXP/ANT/PATH`：
  - `PATH`：跨端契约先自动盘点事实，再人工裁决语义；
  - `ANT`：只维护人工 Provider 表格，不校验实际调用点；
  - `EXP`：静态扫描必须限定真实 HTTP 适配器导入，避免同名本地函数污染。
- 后续动作与负责人：先关闭 12 项设计模版 `PARITY_REQUIRED`，再处理 7 项 Web-first 待备案和 1 项退出登录对齐；之后扩展字段级契约。负责人：codex + 用户。
