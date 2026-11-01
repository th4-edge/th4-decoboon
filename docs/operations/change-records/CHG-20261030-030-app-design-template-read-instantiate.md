# CHG-20261030-030 · App 方案模版库浏览与实例化

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-030 |
| 类型 | 功能补齐 / 跨端能力 |
| 风险级别 | L2 |
| 状态 | 微信开发者工具真实页面复核通过；物理真机与独立验收待执行（CHG-20261030-033） |
| 负责人 | codex |
| 日期 | 2026-10-30 |
| 关联需求/裁决 | `DEC-084 §5`、`DEC-050` 跨端修订、`BR-040`、`OBJ-005`、领域批次 `DQ-X3` |

## 2. 目标与范围

- 目标：补齐 App 设计模版双端能力的第一垂直切片：浏览已发布模板、查看区域/分项/公式、从已发布模板实例化到当前房屋。
- 范围内：
  - App `design/template/page|info|instantiate` 领域 API；
  - App 模板库与模板详情页面；
  - 设计转化页入口；
  - 模板查看/编辑权限与路由守卫；
  - 领域 DQ-X3 需求、蓝图、对账和状态账本。
- 非范围：
  - App 模板新建、编辑、发布、停用、复制；
  - App 区域/分项/版本维护；
  - 价格、成本、毛利展示；
  - 后端 DDL、权限语义修改和生产发布。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及设计模版对象读取、实例化和模板权限；不改变价格、成本、利润或模板状态机。
- 业务依据：
  - `DEC-084`：设计模版 App/Web 双端完整；
  - `DEC-050`：归属、版本、发布不可变、实例化快照和权限规则；
  - `BR-040`：模版不承载价格、成本或供应商历史价；
  - `OBJ-005`：方案库对象定义。
- 尚存 GAP：
  - App 模板编辑/发布/停用/复制/区域分项维护仍未实现；
  - 真实开发者工具/真机页面复核未执行；
  - 房屋删除 `GAP-WEB-025` 与设计域无关，但 Provider 严格门禁仍保留。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 设计师进入客户房屋设计方案页 | 可看到「从方案模版新建」入口 | App 页面代码 |
| 2 | 查看模板库 | 只展示 `Published`，支持搜索、范围筛选和分页 | 页面契约/构建 |
| 3 | 查看模板详情 | 展示版本、区域、分项、公式；不展示价格/成本 | 页面代码 |
| 4 | 缺 `propertyId` 或模板为草稿/停用 | 阻断实例化并显示原因 | 页面代码 |
| 5 | 已发布模板实例化 | 调用唯一 `design/template/instantiate`，成功回读设计方案 | App API + 页面代码 |
| 6 | App/Web Provider | `page|info|instantiate` 均归 shared | Provider audit |
| 7 | 领域账本 | DQ-X3 进入 `DEV_VERIFIED`，UI 仍 PENDING_RENDER | `state.json` / `validate` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | `docs/product/domains/design-quote/*`、Provider 基线/评审、本变更记录 |
| th4-decoboon-app | `dev` | 未提交 | `src/domains/designtemplate/*`、`src/subpackages/design/pages/templates.vue`、`template-detail.vue`、`list.vue`、配置/权限/路由 |
| th4-decoboon-web | `dev` | 不适用 | 复用既有模板 Provider，无本轮源码改动 |

## 6. 验证

| 检查 | 命令/页面 | 结果 |
| --- | --- | --- |
| App 类型检查 | `npm run type-check` | PASS |
| App 分包/导航/UI/文案/分页门禁 | `check:subpackages`、`check:navigation`、`check:ui`、`check:client-copy`、`check:pagination` | PASS |
| App 发布构建 | `npm run build:mp-weixin:release` | PASS；主包 1,075,678 B、最大分包 734,997 B、总包 4,186,107 B |
| 后端模板测试 | `DesignTemplateWhitelistContractTest`、`DesignTemplateInstantiateInheritanceTest`、`DesignTemplateFormulaContextTest` | PASS |
| Provider 巡检 | `--self-test`、`--check` | PASS；`design/template/page|info|instantiate` 为 shared |
| 领域状态 | `domain_state.py validate/show` | PASS；DQ-X3 `DEV_VERIFIED` |

- 未执行项及原因：未使用真实开发者工具/真机打开模板库、详情和实例化结果；需在用户本地完成真实页面复核后进入 `UI_REVIEWED`。
- 已知失败及是否与本次相关：无；构建仅有既有 Sass legacy API 警告。

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

- 回滚条件：模板库打开失败、模板详情错误展示草稿/停用模板、实例化错误执行或生成结果无法回读。
- 回滚步骤：删除 App 模板领域与两个页面路由，恢复设计转化页入口和权限/路由；恢复领域 DQ-X3 记录与 Provider 评审。
- 回滚验证：App type-check/build 通过，原设计转化链路不受影响。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；首次补权限/路由后仍需明确模板草稿/停用阻断和 App 实例化回读边界。
- 返工原因：需要把模板完整能力拆成先读后写两批，避免一次性实现 11 项写能力。
- 最终结论：App 模板库读取和实例化 3 个 Provider 已完成共享；真实 UI 和剩余模板写能力未完成。

## 10. 复盘与沉淀

- 是否出现重复问题：设计模板长期只有 Web 消费，App 缺失导致 Provider 评审一直为 PARITY_REQUIRED。
- 是否需要新增门禁：已有 Provider 巡检；本批新增的模板页面应补入后续真实渲染门禁。
- 候选 `EXP/ANT/PATH`：
  - `PATH`：先完成读取/实例化闭环，再补编辑/发布；
  - `ANT`：把“模板详情能看”写成双端完整；
  - `EXP`：实例化必须携带当前 `propertyId`，且成功后回读设计方案事实。
- 后续动作与负责人：真实 App 模板页复核；随后实现模板编辑、发布、停用、复制与区域/分项维护。负责人：用户 + codex。
