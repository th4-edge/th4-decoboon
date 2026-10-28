# 推广前最低闭环 · PROMO-D1 基线收口

> 日期：2026-10-28  
> 关联问题：`PI-20261028-001`  
> 性质：开发自测与 UI 渲染证据，不是独立业务验收，不代表可以发布或推广  
> 结论：`DEV_VERIFIED / PROMOTION_BLOCKED`

## 一、本批范围

本批只处理推广前基线的第一层问题：

1. 三仓静态质量基线；
2. 与最新业务决策冲突的 App 首页治理脚本；
3. Web 财务报价页的研发术语、裸枚举、大页固定值和前端逐行利润计算；
4. 财务报价页正常、空、失败和极端值状态的真实渲染证据。

本批不做：

- 不新增业务对象、状态、权限、金额公式或 DDL；
- 不进行生产发布、生产迁移、小程序提审或正式版发布；
- 不把开发自测当成独立验收；
- 不把单个页面改善当成 Web 整体 UI 完成。

## 二、三仓基线

| 仓库 | 分支 | 基线时状态 | 本批结论 |
| --- | --- | --- | --- |
| 后端 `th4-decoboon` | `dev` | 存在大量治理/领域/事故文档未提交改动，分支与远端分叉 | Java 22 `mvn -o -q -DskipTests package` PASS；仍未形成唯一发布提交 |
| App `th4-decoboon-app` | `dev` | 约 90 个源码/配置改动，分支与远端分叉 | `npm run verify:mp-weixin` PASS；真机和独立验收未执行 |
| Web `th4-decoboon-web` | `dev` | 财务报价等源码未提交，分支与远端分叉 | 静态治理、类型检查和生产构建 PASS；视觉全量收口未完成 |

## 三、本批修复

### 3.1 App 首页治理脚本

- 原脚本仍冻结旧设计师入口顺序，和当前五阶段工作队列实现冲突。
- 已按当前实现将门禁冻结为：待我跟进 / 待我量房 / 待我设计 / 待我谈单 / 待我签约。
- `DPR-D01` 的「工作台 / 客户 / 签单 / 项目 / 我的」一级导航改造仍由 `PI-20261024-001 / DESIGNER-D2-1` 单独跟踪，不在本脚本中冒充完成。
- `npm run check:home` PASS。

### 3.2 Web 财务报价列表

- 移除研发术语：`服务端`、`REGION / TYPE 双投影`、`快照求和`。
- 移除前端逐行利润 `金额 - 成本` 计算；页面只展示服务端报价、标准成本和成本完整性。
- 移除 `size=200` 大页固定值，改为服务端分页，每页 20 条。
- 报价汇总和报价记录分别请求；报价记录失败时仍给出明确错误态，汇总失败时列表仍可查看并显示“报价汇总暂不可确认”。
- 总报价数改用服务端分页总数，不再用当前页状态推导待确认数。
- 页面改为统一表格语法，保留客户/房屋、报价、设计师、状态、金额、成本、成本状态和明细入口。

### 3.3 Web 裸枚举

- `DesignerProjectContractView.vue` 的合同变更状态改为使用 `CONTRACT_CHANGE_STATUS_LABELS`，不再直接渲染 `customerConfirmed` 等内部值。

## 四、自动化验证

| 检查 | 结果 |
| --- | --- |
| 后端 Java 22 package（skip tests） | PASS |
| App `check:home` | PASS |
| App `npm run verify:mp-weixin` | PASS |
| App type-check / 小程序构建 / 分包 | PASS |
| Web `check:enums` | PASS，裸枚举问题 0 |
| Web `check:pagination` | PASS，无新增大页固定值 |
| Web `check:client-copy` | PASS，生产界面未发现研发型提示 |
| Web `check:actions` / `concepts` / `forms` / `metrics` / `page-system` / `permissions` / `states` / `workbenches` / `workspaces` / `store-scope` | PASS |
| Web type-check / 生产构建 | PASS |
| 三仓 `git diff --check` | PASS |

## 五、真实渲染证据

渲染方式：本地 Vite + Playwright 路由 Mock，仅用于验证页面结构、状态和响应式表现，不代表真实 Provider 数据验收。

截图目录：Web 仓库 `output/playwright/promo-d1/`

| 场景 | 视口 | 证据 |
| --- | --- | --- |
| 正常数据 | 1440×900 | `finance-quotes-1440x900.png` |
| 正常数据 | 1024×768 | `finance-quotes-1024x768.png` |
| 真实空态 | 1440×900 | `finance-quotes-empty-1440x900.png` |
| 加载失败 | 1440×900 | `finance-quotes-error-1440x900.png` |
| 长名称、超大金额、缺失成本 | 1440×900 | `finance-quotes-extreme-1440x900.png` |
| 长名称、超大金额、缺失成本 | 1024×768 | `finance-quotes-extreme-1024x768.png` |

### 5.1 渲染观察

- 1024 宽度下页面级 `scrollWidth = clientWidth = 1024`，无页面横向溢出。
- 报价表在窄屏内容区内提供自身横向滚动，不把整页撑破。
- 状态同时使用文字和颜色；成本完整性使用“完整 / 未完整”文字。
- 正常态 6 行数据全部渲染；空态、失败态、极端值态均独立呈现。
- 直接深链加载存在 1 条 Vue Router 首屏匹配 warning，无未处理异常；作为 P2 保留观察。

## 六、尚未闭合

以下事项仍使推广门禁保持 BLOCKED：

1. 三仓工作区不干净，首发提交、构建产物和回滚点未唯一化；
2. 真实项目尚未按“合同生效 → 项目履约 → 变更 → 材料 → 验收整改 → 收付款/成本 → 业主回读”完成三端走查；
3. Web 全站 Token、页面母型、七态和视觉一致性尚未完成；
4. App 真机、弱网、相机/相册、长内容与多机型证据未完成；
5. 现有开放 P0/P1 尚未按首发范围逐项关闭或取得条件性首发批准；
6. 企业指标仍有正式口径差异提示，相关数字不得对外作为正式经营结果。

## 七、门禁结论

```text
PROMO-D1 静态基线：PASS
PROMO-D1 UI 切片：CONDITIONAL（仅财务报价列表/详情相关）
推广前最低业务闭环：NOT PASS
当前状态：产品验证中，不得进入正式首发推广
```

## 八、下一步

1. 冻结三仓首发提交范围，建立唯一发布候选与回滚点；
2. 选择一个真实项目启动 `PROMO-D2` 入口场景闭环；
3. 先打穿“合同生效 → 项目/阶段/任务 → 现场留证 → 验收/整改/复验 → 业主回读”；
4. 完成真实 App/Web/业主端对账后，再进入变更、材料、收款/成本支撑场景。
