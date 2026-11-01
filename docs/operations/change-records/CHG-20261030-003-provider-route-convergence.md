# CHG-20261030-003 · App/Web Provider 路由收敛

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-003 |
| 类型 | 跨端契约修复 |
| 风险级别 | L2 |
| 状态 | 开发自测通过，待用户验收 |
| 负责人 | codex |
| 日期 | 2026-10-30 |
| 关联需求/裁决 | `CHG-20261030-002`、`DEC-084`、`DEC-075`、`BR-033`、`GAP-WEB-002` |

## 2. 目标与范围

- 目标：关闭路由级检查发现的 `ROUTE_MISMATCH`，让同一业务能力在 App/Web 使用同一 Provider。
- 范围内：
  - App 项目成员更新改为 `project/project/update`；
  - App 设计分项新增、编辑、删除改为 `design/items/save`；
  - Web 移除未被页面调用的单行设计分项 Provider 包装；
  - 将 `design/items/reorder` 明确为 Web 排序交互并转入 Web-first 待备案；
  - 更新 Provider 基线、评审清单和说明。
- 非范围：
  - App 设计模板完整能力；
  - 风险处置、房屋删除的 Web-first 业务备案；
  - App 退出登录 Provider 对齐；
  - 字段级、权限级、金额级完整契约扩展。
- 受影响端：App、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及项目成员和设计分项保存路径，不改变业务字段、权限或金额公式。
- 业务事实来源：
  - `DEC-075` / `BR-033`：项目成员编辑权限和候选人规则；
  - `DEC-084`：设计模板双端完整，本次不处理；
  - `WEB-建设核心思路 A2`：禁止同义 Provider；
  - `GAP-WEB-002`：报价工作台 Web 优先边界。
- 尚存 GAP：
  - 设计分项排序是否属于 Web-first 交互，待备案；
  - 设计模板 App 双端补齐；
  - 风险处置和房屋删除 Web-first 备案；
  - App 退出登录 Provider 对齐。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | App 更换项目经理/设计师 | 调用 `project/project/update`，不再调用 `project/member/save` | App 代码检索 |
| 2 | Web 更换项目经理/设计师 | 继续调用 `project/project/update` | Web 代码检索 |
| 3 | App 新增/编辑设计分项 | 调用 `design/items/save`，不再调用 `design/item/save` | App type-check/build |
| 4 | App 删除设计分项 | 通过 `design/items/save.deletedItemIds` 原子删除 | App 代码检索 |
| 5 | Web 删除设计分项 | 通过 `design/items/save.deletedItemIds` 原子删除 | Web type-check/build |
| 6 | Web 分项排序 | 仍使用 `design/items/reorder`，并在评审清单标记待备案 | Provider audit |
| 7 | 契约巡检 | 0 未匹配路由；`ROUTE_MISMATCH=0` | `--check` |
| 8 | 严格模式 | 仍有 19 项显式未关闭债务，按预期返回 1 | `--strict-web-only` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | Provider 基线、评审清单、架构说明、本变更记录 |
| th4-decoboon-app | `dev` | 未提交 | `src/domains/project/api.ts`、`src/domains/design/api.ts`、项目成员页、设计方案页 |
| th4-decoboon-web | `dev` | 未提交 | `src/domains/design/api.ts` |

## 6. 验证

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| App 类型检查 | `npm run type-check` | PASS |
| App 定向门禁 | `check:design-items`、`check:ui`、`check:navigation`、`check:pagination`、`check:client-copy` | PASS |
| App 发布构建 | `npm run build:mp-weixin:release` | PASS；主包 1,072,282 B、最大分包 734,997 B、总包 4,150,539 B |
| Web 类型检查 | `npm run type-check` | PASS |
| Web 完整构建 | `npm run build` | PASS |
| 后端定向测试 | `mvn -q -Dtest=DesignItemBatchSaveTest,ProjectMemberSwitchAndStageOpenTest,DesignItemReorderTest test` | PASS |
| Provider 巡检 | `python3 scripts/audit-frontend-provider-contract.py --self-test`、`--check` | PASS；24 项评审覆盖，19 项 open debt |
| 严格模式负向 | `python3 scripts/audit-frontend-provider-contract.py --check --strict-web-only` | PASS：按预期返回 1 |

- 未执行项及原因：未进行浏览器/真机写入验收；本次为 Provider 路由与前端调用适配，需用户在当前本地环境完成真实角色写入抽查。
- 已知失败及是否与本次相关：App 构建仅保留既有 Sass legacy API 警告，不影响构建结果。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 当前后端/App/Web 工作区 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：项目成员指派、设计分项新增/编辑/删除在真实链路出现失败或权限/审计回归。
- 回滚步骤：恢复 App 项目、设计 API 与对应页面；恢复 Web 设计 API；恢复本次 Provider 基线和评审清单。
- 回滚验证：App/Web 类型检查与构建通过，原有 Provider 路由重新可用。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；Web 删除单行包装后暴露 `normalizeItem` 仍被方案详情使用，已恢复。
- 返工原因：只删除 Provider 包装时误删了共用的响应归一化函数，已按原契约恢复。
- 最终结论：3 项路由不一致已收敛为 0；真实写入待用户本地验收。

## 10. 复盘与沉淀

- 是否出现重复问题：App/Web 各自拥有同义 Provider 包装是跨端契约漂移的典型入口。
- 是否需要新增门禁：已有 Provider 巡检和评审覆盖，无需再加新脚本；后续将此检查纳入跨端 release preflight。
- 候选 `EXP/ANT/PATH`：
  - `EXP`：移除 Provider 包装前先检索共用归一化函数，不能只看导出名；
  - `ANT`：以“页面能用”替代 Provider 同源核对；
  - `PATH`：先统一 Provider，再处理 Web-only 备案和字段级契约。
- 后续动作与负责人：真实角色验证 App 项目成员切换、设计分项新增/编辑/删除；负责人：用户 + codex。
