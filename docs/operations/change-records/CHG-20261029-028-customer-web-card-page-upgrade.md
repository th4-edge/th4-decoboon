# CHG-20261029-028 · Web 客户角色卡片页面升级

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261029-028 |
| 类型 | 重构 / 需求 |
| 风险级别 | L2 |
| 状态 | 开发自测通过，待用户体验 |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | 客户页面规则v2、客户页面业务能力清单、`USER-DEC-20261029-007`、`FM-02` |

## 2. 目标与范围

- 目标：将 `/workbench/customers` 升级为角色化客户卡片页面，支持企业负责人、店长、财务三种场景投影。
- 范围内：
  - 角色统计；
  - 门店筛选；
  - 负责人分组；
  - 客户卡片；
  - 独立客户 360 页面 `/workbench/customers/:customerId`；
  - 客户卡按小程序客户 Tab 展示客户身份、房屋、协同人、状态、经营时钟和下一步；
  - 财务合同、项目、待收、逾期作为卡片底部低权重业务上下文；
  - 所有内部角色常驻左侧负责人分组，空客户时保留分组空态；
  - 客户卡“顾问”统一为“客户负责人”；
  - 客户 360 覆盖责任关系、业务阶段、多房屋、房屋关联信息、客户资料、画像和跟进时间轴；
  - 客户 360 支持从任意业务页面 SPA 进入，并通过 Router history 返回来源页；
  - Boss / 店长 / 财务与小程序统一使用 `customer/customer/overview` 返回统计、分组与客户列表；
  - 客户 360 使用 `customer/customer/info` 并消除重复请求；
  - 修复 `customer/customer/overview.data.customers` 未被 Web 适配层读取，导致统计有数据但客户卡为空的问题；
  - 本页及客户/房屋弹窗功能按钮统一“图标 + 文字”；
  - 开发环境登录后 `?preview=1` 示例预览；
  - 企业负责人客户菜单指向统一客户页面。
- 非范围：后端 Provider、DDL、权限、金额/成本/状态机、生产发布。
- 受影响端：Web；App/后端未修改。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：页面展示业务事实，未新增语义
- 业务事实来源与裁决编号：客户页面规则v2、`DEC-080`、`DEC-065`、`DEC-093`
- 尚存 GAP 或假设置：
  - 企业负责人客户概览可复用现有 `customer/customer/overview`；
  - 店长/财务全量客户汇总 Provider 尚未确认；
  - 财务客户卡的真实合同、应收、设计聚合仍需后续 Provider 对账。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 财务登录预览 | `?preview=1` | 客户卡先展示客户状态、房屋、负责人和下一步，财务上下文降为底部摘要 | `screenshots-v2/finance-customer-v2-1440x900.png` |
| 2 | 企业负责人预览 | `?preview=1` | 显示全公司统计、门店范围、负责人分组和客户卡 | `screenshots-v2/boss-customer-v2-1440x900.png` |
| 3 | 店长预览 | 临时真实角色验证 | 显示本店统计、本店负责人分组和本店客户卡 | `screenshots-v2/storemanager-customer-v2-1440x900.png` |
| 4 | 客户 360 | 点击客户卡 | 进入独立 `/workbench/customers/:customerId` 页面，展示身份、责任关系、业务阶段、多房屋、七个关联页签和跟进时间轴 | `screenshots-v4/customer360-page-v4-1440x900.png` |
| 5 | 响应式 | 1440/1280/1024 | 无横向溢出，页面可见文字 ≥12px | 三档 v2 截图 |
| 6 | 空客户分组 | Finance 无客户数据 | 左分组仍显示“全部客户”和“暂无负责人分组” | `screenshots-v2/finance-customer-v2-empty-1440x900.png` |
| 7 | 按钮图标门禁 | 当前页及客户详情 DOM | 可见按钮缺失图标数为 0 | 浏览器 DOM 审计 |
| 8 | 独立详情与返回 | 从列表或其他页面进入客户 360 | 打开独立页面；返回来源页；再次进入正常 | v4 路由与返回证据 |
| 9 | 客户统计同源 | Finance 打开客户列表 | 仅调用一次 `customer/customer/overview`，指标与分组直接消费 Provider 结果 | v5 网络证据 |
| 10 | 客户 360 请求 | 打开客户 360 | 仅调用一次 `customer/customer/info` | v5 网络证据 |
| 11 | 真实 Finance 数据 | 18660219918 登录 | 客户总数 1、负责人张伟 1、客户郑和可见、客户 360 房屋 2 | `docs/acceptance/runs/20261030-customer-web-acceptance/` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-web | dev | 未提交 | `src/views/workbench/StoreCustomerView.vue`、`src/domains/crm/{api,model}.ts`、`src/domains/content/{api,model}.ts`、`src/roles/{routes,workbenchConfig,workbenchHome}.ts`、客户/房屋相关组件、`.pagination-baseline.json` |
| th4-decoboon | dev | 未提交 | `docs/product/domains/finance/16-customer-web-role-page-design-v1.md`、`17-customer-web-customer-centric-card-v2.md`、`18-customer-web-360-workspace-v3.md`、`19-customer-web-standalone-360-page-v4.md`、`20-customer-web-overview-same-source-fix.md`、客户页面验收证据 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| TypeScript | `npm run type-check` | PASS | 终端输出 |
| Build | `npm run build` | PASS | 终端输出 |
| 动作注册 | `npm run check:actions` | PASS | 终端输出 |
| 权限 | `npm run check:permissions` | PASS | 终端输出 |
| 状态 | `npm run check:states` | PASS | 终端输出 |
| 表格/选择器 | `npm run check:forms`、`check:concepts` | PASS | 终端输出 |
| 工作台/工作区 | `npm run check:workbenches`、`check:workspaces` | PASS | 终端输出 |
| 分页 | `npm run check:pagination` | PASS | `.pagination-baseline.json` |
| 真实渲染 | Playwright 1440/1280/1024 | PASS，无横向溢出，页面可见文本 ≥12px | `docs/acceptance/runs/20261029-customer-web-page/screenshots/` |
| 交互 | 财务逾期筛选、Boss 门店筛选、客户详情 | PASS | 验收 README |
| 视觉预检 | `qwen2.5vl:7b` | 已执行 v2、v3 与 v4；v4 独立页面完成机器视觉预检，用户登录评审待完成 | `vision-v4/` |

- 未执行项及原因：独立验收、生产发布、真实多角色数据回归未执行。
- 已知失败及是否与本次相关：店长/财务全量聚合 Provider 尚未确认，属于后续数据接入缺口；当前页面明确按“当前客户列表统计”表达。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用（尚未发布） |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 当前线上 Web 版本 |
| 健康检查 | 不适用 |
| 发布后观察 | 用户真实登录评审后再决定 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：用户否决客户卡片场景或角色投影。
- 回滚命令/步骤：回退 Web 相关文件和 `.pagination-baseline.json`；恢复此前的客户页面与菜单指向。
- 回滚验证：`npm run build` 与本机真实登录通过。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否
- 返工原因：首次大页固定值被分页门禁拦截；客户文案被客户端文案门禁拦截；均已在构建前修复
- 最终结论：待用户登录体验验证，不发布

## 10. 复盘与沉淀

- 是否出现重复问题：是；再次确认客户页面不能沿用三栏对象工作面。
- 是否需要新增门禁：否；本轮人工执行页面按钮图标 DOM 审计
- 候选 `EXP/ANT/PATH`：角色场景卡片页、负责人分组与老板/店长同口径、财务卡保留业务链摘要
- 后续动作与负责人：用户登录 `?preview=1` 验证客户中心卡片、左侧分组和按钮图标；确认后再决定是否接入真实完整聚合与独立验收
