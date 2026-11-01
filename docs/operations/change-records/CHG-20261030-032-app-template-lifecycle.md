# CHG-20261030-032 · App 方案模版复制、停用与新版本

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-032 |
| 类型 | 功能补齐 / 跨端能力 |
| 风险级别 | L2 |
| 状态 | 微信开发者工具真实页面复核通过；物理真机与独立验收待执行（CHG-20261030-033） |
| 负责人 | codex |
| 日期 | 2026-10-30 |
| 关联需求/裁决 | `DEC-084 §5`、`DEC-050` 跨端修订、`BR-040`、`OBJ-005`、领域批次 `DQ-X5` |

## 2. 目标与范围

- 目标：完成 App 已发布模板后续生命周期：复制为个人草稿、停用模板、新建草稿版本。
- 范围内：
  - 模板卡“更多”动作面板；
  - `design/template/copy|disable|version/build` App 消费；
  - 动作权限与归属过滤；
  - 停用二次确认；
  - 新建版本后进入草稿编辑器。
- 非范围：
  - 真实开发者工具/真机视觉验收；
  - 房屋删除语义裁决；
  - 任何价格、成本、利润或权限语义修改。

## 3. 业务与决策

- 业务依据：
  - `DEC-084`：App/Web 设计模板双端完整；
  - `DEC-050`：发布后不可变；修改生成新版本；停用不影响历史报价；
  - `BR-040`：模板不承载价格/成本，复制只产生个人草稿。
- 不改变：模板发布状态机、归属权限、历史报价快照和公式校验。
- 尚存项：真实页面验收；房屋删除 `GAP-WEB-025` 独立等待用户裁决。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 复制任意可见模板 | 生成当前员工个人 Draft，源模板不变 | App 动作面板/API |
| 2 | 已发布模板新建版本 | 生成/复用唯一 Draft，原发布版本继续可用 | App 动作面板/API |
| 3 | 停用已发布模板 | 二次确认后停用；停止新增实例化和编辑 | App 动作面板/API |
| 4 | 非维护者 | 不显示维护动作；服务端仍拒绝 | App 权限/后端 |
| 5 | Provider 协同 | copy/disable/version/build 均为 App/Web 共享 | Provider audit |
| 6 | 设计模板完整度 | 设计模板 Provider `PARITY_REQUIRED=0` | Provider audit |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | 设计域 DQ-X5 账本、Provider 基线/评审、本变更记录 |
| th4-decoboon-app | `dev` | 未提交 | `src/domains/designtemplate/api.ts`、`src/subpackages/design/pages/templates.vue`、`verify-design-items-contract.mjs` |
| th4-decoboon-web | `dev` | 不适用 | 复用既有模板 Provider，无本轮源码改动 |

## 6. 验证

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| App 类型检查 | `npm run type-check` | PASS |
| App 模板契约 | `npm run check:design-items` | PASS |
| App 其他门禁 | `check:navigation`、`check:ui`、`check:client-copy`、`check:pagination` | PASS |
| App 发布构建 | `npm run build:mp-weixin:release` | PASS；主包 1,077,228 B、最大分包 734,997 B、总包 4,222,981 B、180 页 |
| Provider 巡检 | `--self-test`、`--check` | PASS；设计模板 `PARITY_REQUIRED=0` |
| 领域状态 | `domain_state.py validate` | PASS；DQ-X5 `DEV_VERIFIED` |

- 未执行项及原因：未进行真实模板卡动作面板、复制、停用和新版本的开发者工具/真机复核。
- 已知失败及是否与本次相关：无；仅既有 Sass legacy API 警告。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 当前 App/后端工作区 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：复制错误修改源模板、停用未确认、版本重复生成或历史报价受影响。
- 回滚步骤：移除模板卡动作和 App copy/disable/version API；恢复 Provider 评审与 DQ-X5 账本。
- 回滚验证：App type-check/build 通过，模板库与已发布模板实例化链路不受影响。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；首次动作面板混用了局部 JzActionSheet 与全局动作宿主，已改为 JzConfirmDialog 统一驱动。
- 返工原因：模板生命周期动作需要同时驱动确认弹窗与动作面板，必须使用页面级全局宿主。
- 最终结论：设计模板 App/Web Provider 路由级已完整共享；真实 UI 验收仍未完成，房屋删除仍是唯一 Web-first 待裁决项。

## 10. 复盘与沉淀

- 是否出现重复问题：模板完整能力被拆成读、写、生命周期三批，避免单批过大。
- 是否需要新增门禁：模板契约门禁已扩展；房屋删除仍保持严格模式阻断。
- 候选 `EXP/ANT/PATH`：
  - `PATH`：Provider 共享先归零，再做真实 UI 验收；
  - `ANT`：把“路由共享”写成“双端已验收”；
  - `EXP`：动作面板与确认弹窗共用 JzConfirmDialog 宿主，避免重复弹层状态。
- 后续动作与负责人：用户真机复核模板动作面板与状态回读；确认房屋删除物理/软删规则。负责人：用户 + codex。
