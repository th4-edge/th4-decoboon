# 客户概念页面清单 · Web / 小程序 v0.1

- 日期：2026-10-29
- 状态：`INVENTORY_READY / RULE_UNIFICATION_PENDING`
- 范围：以 `Customer`、`Property`、`CustomerFollow`、`Measure`、`Portrait`、`Tag` 为主对象的页面
- 不纳入核心计数：报价、合同、项目、设计、验收、客户意见投诉等“客户只是上下文”的页面
- 约束：本清单只盘点现状，不裁决页型、不改后端、不进入视觉设计

## 1. 统计口径

需要区分四个维度：

| 维度 | 含义 | 为什么不能混算 |
| --- | --- | --- |
| 路由 | 用户可进入的 URL/页面入口 | 同一组件可挂多个路由 |
| 页面组件 | 真正承载页面逻辑的 `.vue` 文件 | 多个路由可能共用同一组件 |
| 页面母型 | 列表、对象工作面、表单、队列等交互结构 | 页面数量不等于交互模型数量 |
| 客户概念 | 客户、房屋、跟进、量房、画像、标签等业务概念 | 一个页面可能承载多个概念，一个概念也可能分散多页 |

## 2. 结论先行

### 2.1 Web

当前 Web 客户主链共有：

- **9 个有效客户概念路由**；
- **8 个不同页面组件**：
  - `/designer/customers` 与 `/designer/design` 共用 `DesignerCustomersView.vue`；
- **1 个相邻页面**：客户意见投诉；
- **1 个未挂载历史页面**：`TeamCustomerView.vue`；
- Web 没有独立的客户新增、编辑、跟进、房屋、微信、画像、标签、批量录入路由，部分以 Sheet/Drawer 内嵌。
当前未发现 Web 客户标签维护和客户批量录入的完整入口。

### 2.2 小程序

当前小程序客户分包共有：

- **17 个客户包页面**；
- 其中 **15 个业务页面**；
- **2 个选择器辅助页**：客户选择器、房屋选择器；
- 客户新增、编辑、跟进、量房、房屋、微信、画像、标签、批量录入均为独立页面。

### 2.3 核心差异

```text
Web：少量路由 + 重组件 + 大量内嵌 Sheet/Drawer
小程序：多路由 + 单职责页面 + 页面间跳转
```

这导致两端不是同构页面，而是同业务概念的不同拆页方式。

## 3. Web 客户概念路由与组件

| ID | 路由 | 页面组件 | 页面概念 | 主要角色 | 页面职责 | 当前页型 |
| --- | --- | --- | --- | --- | --- | --- |
| WEB-C01 | `/workbench/my-customers` | `MarketerCustomerView.vue` | 我的客户 | Marketer、Boss | 本人客户列表；新增/编辑、跟进、量房、微信、到店、休眠、流失、房屋维护、只读设计/项目 | 列表 + 详情侧栏/抽屉 |
| WEB-C02 | `/workbench/measure` | `MarketerMeasureView.vue` | 我的预约量房 | Marketer | 本人客户量房任务与预约查看 | 队列/列表 |
| WEB-C03 | `/workbench/customers` | `StoreCustomerView.vue` | 本店客户/全部客户 | Boss、StoreManager、Finance | 客户表格；转交负责人、指派设计师、流失/恢复、房屋维护；查看方案 | 表格 + 弹窗 |
| WEB-C04 | `/designer/my-customers` | `DesignerMyCustomersView.vue` | 我的客户 | Designer、Boss、StoreManager | 设计师客户经营总览、阶段筛选、经营时钟、主行动 | 经营列表 |
| WEB-C05 | `/designer/my-customers/:customerId` | `DesignerCustomer360View.vue` | 客户 360 | Designer、Boss、StoreManager | 客户、房屋、诉求、设计、报价、项目、收付款摘要、跟进、画像、流失 | 对象工作面 |
| WEB-C06 | `/designer/followup` | `DesignerFollowUpTasksView.vue` | 待我跟进 | Designer、Boss、StoreManager | 跟进队列；写跟进、微信、到店、量房、画像、流失 | 行动队列 + 工作面 |
| WEB-C07 | `/designer/measure` | `DesignerMeasureTasksView.vue` | 待我量房 | Designer、Boss、StoreManager | 量房任务队列、量房安排与结论记录 | 队列 + 表单 |
| WEB-C08 | `/designer/customers` | `DesignerCustomersView.vue` | 客户与设计 | Designer、Boss、StoreManager | 按客户/房屋推进设计，含客户、房屋、诉求、方案、量房 | 三栏对象工作面 |
| WEB-C09 | `/designer/design` | `DesignerCustomersView.vue` | 待我设计 | Designer、Boss、StoreManager | 设计任务队列，复用 C08 组件 | 三栏对象工作面 |
| WEB-C10（相邻） | `/workbench/customer-feedback` | `DesignerCustomerFeedbackView.vue` | 客户意见投诉 | Designer、Pm、Boss、StoreManager、Marketer、Finance | 项目下业主意见与投诉处置 | 队列 + 对象侧栏 |
| WEB-C11（未挂载） | 无当前有效路由 | `TeamCustomerView.vue` | 团队客户 | TeamLeader 历史角色 | 团队客户查看 | 历史列表/工作面 |

### 3.1 Web 内嵌客户能力

| 客户概念/动作 | 当前实现位置 | 是否有独立路由 |
| --- | --- | --- |
| 客户新增 | `CustomerCreateSheet.vue`，Marketer/Designer My Customers 等页面内嵌 | 否 |
| 客户编辑 | `CustomerEditSheet.vue`，Marketer/Designer 360 等页面内嵌 | 否 |
| 写跟进 | `CustomerFollowFormDrawer.vue`，多个页面内嵌 | 否 |
| 房屋新增/编辑 | `PropertyFormDrawer.vue`，多个页面内嵌 | 否 |
| 记录微信 | Marketer 客户页、Designer 跟进页内嵌 Drawer | 否 |
| 记录到店 | Marketer 客户页、Designer 跟进页内嵌动作 | 否 |
| 客户画像 | Designer 跟进页内嵌 Drawer；客户 360 只展示 | 否 |
| 客户标签 | 客户 360/详情展示；未见完整维护页 | 否 |
| 客户流失/恢复 | `CustomerLossDrawer.vue`，多个页面内嵌 | 否 |
| 客户批量录入 | 未发现 Web 完整入口 | 否 |
| 客户选择器 | `CustomerPicker.vue` 组件 | 否 |
| 房屋选择器 | `PropertyPicker.vue` 组件 | 否 |

## 4. 小程序客户概念页面

| ID | 页面路径 | 页面标题 | 页面概念 | 角色/入口 | 页面职责 | 页型 |
| --- | --- | --- | --- | --- | --- | --- |
| APP-C01 | `/pages/customer/list` | 客户 | 客户列表/客户 Tab | Boss、StoreManager、Marketer、Pm、Finance | 角色范围内的客户池、阶段、跟进、经营摘要 | 列表/经营首页 |
| APP-C02 | `/subpackages/customer/pages/detail` | 客户详情 | 客户 360/对象工作面 | 有 `view:customers` 的角色 | 客户、房屋、跟进、设计、报价、项目、主行动 | 对象工作面 |
| APP-C03 | `/subpackages/customer/pages/edit` | 新建客户/编辑客户资料 | 客户新增/编辑 | 有 `act:followCustomer` 的角色 | 建客户档案、编辑基础资料 | 表单 |
| APP-C04 | `/subpackages/customer/pages/follow` | 跟进客户 | 写跟进 | 有 `act:followCustomer` 的角色 | 记录沟通、下次跟进、图片 | 表单 |
| APP-C05 | `/subpackages/customer/pages/flow` | 签约后业务流转 | 客户关联全链 | 有客户详情入口的角色 | 客户下房屋、设计、报价、合同、项目流转 | 关系总览 |
| APP-C06 | `/subpackages/customer/pages/portrait-edit` | 客户画像 | 客户画像 | 客户主页进入 | 维护客户画像 | 表单 |
| APP-C07 | `/subpackages/customer/pages/tag-manage` | 客户标签 | 客户标签 | 客户主页进入；字典维护限 Boss/StoreManager | 绑定/解绑标签、维护字典 | 对象子页 |
| APP-C08 | `/subpackages/customer/pages/batch` | 批量录入客户 | 客户批量导入 | 当前页面内限 Boss/StoreManager | 粘贴手机号、预览、批量建客户 | 批处理表单 |
| APP-C09 | `/subpackages/customer/pages/designer-followup-workbench` | 待我跟进 | 跟进队列 | 有 `act:followCustomer` 的角色 | 今日待联系客户、写跟进 | 行动队列 |
| APP-C10 | `/subpackages/customer/pages/measure-workbench` | 量房经营工作台 | 量房队列 | 有 `view:measure-workbench` 的角色 | 客户/房屋量房经营队列 | 工作台 |
| APP-C11 | `/subpackages/customer/pages/designer-measure-workbench` | 待我量房 | 设计师量房队列 | 有 `view:measure-workbench` 的角色 | 设计师待量房任务 | 队列 |
| APP-C12 | `/subpackages/customer/pages/measure` | 约定量房时间 | 量房预约 | 有 `act:scheduleMeasure` 的角色 | 选择房屋、约定时间 | 表单 |
| APP-C13 | `/subpackages/customer/pages/measure-record` | 录入量房数据 | 量房记录 | 有 `edit:design` 的角色 | 原始结构、尺寸、诉求、证据、区域 | 专业表单 |
| APP-C14 | `/subpackages/customer/pages/property-edit` | 新增房屋/编辑房屋 | 房屋新增/编辑 | 客户主页进入 | 房屋地址、面积、户型、备注 | 表单 |
| APP-C15 | `/subpackages/customer/pages/wechat-record` | 记录客户微信/更新客户微信 | 微信事实 | 有客户操作入口的角色 | 记录是否加微信、微信号 | 表单 |
| APP-C16 | `/subpackages/customer/pages/customer-picker` | 客户选择器 | 选择客户 | 有 `view:customers` 的角色 | 跨业务选择客户对象 | 选择器 |
| APP-C17 | `/subpackages/customer/pages/property-picker` | 房屋选择器 | 选择房屋 | 有 `view:customers` 的角色 | 跨业务选择房屋对象 | 选择器 |

### 4.1 小程序内嵌客户能力

| 客户概念/动作 | 当前实现位置 | 是否有独立页面 |
| --- | --- | --- |
| 记录到店 | 客户列表/详情内动作 | 否 |
| 客户休眠/恢复 | 客户列表/详情内动作 | 否 |
| 客户流失/恢复 | 客户列表/详情内动作 | 否 |
| 转交负责人 | 客户列表/详情的动作 | 否 |
| 指派设计师 | 客户列表/详情的动作 | 否 |
| 客户状态/阶段 | 列表、详情、经营队列 | 否 |

## 5. 客户概念覆盖矩阵

| 客户概念 | Web | 小程序 | 对齐结论 |
| --- | --- | --- | --- |
| 客户列表/客户池 | 4 套角色页面 | 1 个角色感知列表 | WEB 重复，APP 统一 |
| 客户详情/360 | Customer360 + 多处侧栏 | 独立详情页 | 需统一对象工作面 |
| 客户新增 | 内嵌 Sheet | 独立 edit 页 | 页面结构不同 |
| 客户编辑 | 内嵌 Sheet/360 | 独立 edit 页 | 页面结构不同 |
| 写跟进 | 内嵌 Drawer + 队列 | 独立 follow 页 + 队列 | 交互粒度不同 |
| 跟进队列 | DesignerFollowUpTasks | designer-followup-workbench | 概念相近 |
| 量房队列 | MarketerMeasure + DesignerMeasureTasks | measure-workbench + designer-measure-workbench | 两套角色队列 |
| 量房预约 | 多个内嵌 Dialog | 独立 measure 页 | Web/APP 拆法不同 |
| 量房记录 | DesignerMeasureTasks/DesignerCustomersView 内嵌 | 独立 measure-record 页 | Web 复杂内嵌，APP 单职责 |
| 房屋新增/编辑 | PropertyFormDrawer | 独立 property-edit 页 | 页面结构不同 |
| 客户画像 | FollowUp 内嵌 Drawer；360 展示 | 独立 portrait-edit 页 | Web 入口分散 |
| 客户标签 | 只读展示为主，未发现完整维护 | 独立 tag-manage 页 | Web 能力缺口 |
| 微信记录 | 内嵌 Drawer | 独立 wechat-record 页 | 页面结构不同 |
| 到店记录 | 内嵌动作 | 内嵌动作 | 概念一致 |
| 客户流失/恢复 | 内嵌 Drawer | 内嵌动作 | 概念一致但入口不同 |
| 客户批量录入 | 未发现完整入口 | 独立 batch 页 | Web 能力缺口 |
| 签约后业务流转 | Customer360 关联页签 | 独立 flow 页 | 组织方式不同 |
| 客户选择器 | 组件 | 独立选择器页 | 跨端载体不同 |
| 房屋选择器 | 组件 | 独立选择器页 | 跨端载体不同 |

## 6. 当前重复与冲突

### 6.1 Web 重复

1. 客户列表有四个主要角色页面：
   - Marketer `MarketerCustomerView`
   - Store/Boss/Finance `StoreCustomerView`
   - Designer `DesignerMyCustomersView`
   - Designer/Boss/StoreManager `DesignerCustomersView`
2. 跟进、客户编辑、房屋维护、画像、微信、到店动作分散在多个页面或 Drawer。
3. 客户 360 与 DesignerCustomersView 都展示客户、房屋、方案、报价、项目，信息职责有重叠。
4. 财务「客户」复用 `StoreCustomerView`，但用户期望与设计师同一客户对象工作面，目前为两套。
5. 总部财务标题出现“本店客户/全部客户”混用；Finance 的“查看方案”跳 `/designer/customers` 真实落入 `/forbidden`。

### 6.2 小程序拆分过细

1. 客户概念被拆成 17 个页面，其中不少是单动作表单页（编辑、跟进、量房、房屋、微信、画像、标签）。
2. 有利于移动端快速操作，但增加了页面间上下文传递和返回路径复杂度。
3. 多个页面没有出现在 App `guard.ts` 的显式路由规则中，页面级权限覆盖需要统一核对。
4. `batch.vue` 页面当前只允许 Boss/StoreManager，但 `DEC-080` 已确认财务具备批量录入能力，需核对是否属于前端门户限制或旧实现。

### 6.3 Web 与小程序不同构

| 概念 | Web 结构 | 小程序结构 | 统一难点 |
| --- | --- | --- | --- |
| 客户主工作面 | 列表/360/客户与设计并存 | 客户列表 → 客户详情 | 谁是唯一主工作面未定 |
| 创建/编辑 | Sheet 内嵌 | 独立路由 | 页面级上下文和回读不同 |
| 跟进 | 队列 + Drawer | 独立页面 | 跟进后回读对象不同 |
| 量房 | 队列 + Dialog/内嵌记录 | 队列 + 预约页 + 记录页 | 拆页粒度不同 |
| 画像/标签 | Drawer/只读 | 独立页面 | Web 能力覆盖不完整 |
| 批量录入 | 未发现 | 独立页面 | Web 功能缺口 |

## 7. 规则统一前必须先回答的问题

1. **客户主工作面唯一性**：Web 的 `DesignerCustomer360View` 还是 `DesignerCustomersView` 作为统一客户主工作面？还是两者分工为“客户总览”和“客户设计推进”？
2. **列表页角色化方式**：保留多个角色列表，还是统一一个客户池组件通过角色投影？
3. **单动作页面规则**：Web 是否也应有独立的客户编辑、跟进、房屋、画像、标签、批量录入页面，还是保持 Sheet/Drawer？
4. **小程序拆页规则**：17 个页面中哪些是核心对象页，哪些应为对象内动作，哪些只是选择器？
5. **客户对象与房屋对象关系**：客户详情和工作面中，房屋是主导航、页签还是子对象列表？
6. **量房边界**：量房属于客户概念、房屋概念，还是独立“量房任务/记录”概念？
7. **客户经营 vs 客户档案**：第一层是“今天该跟进谁”，还是“客户资料全集”？
8. **角色投影矩阵**：设计师、业务员、财务、店长、BOSS在客户主工作面上的默认队列、字段、动作和回读分别是什么？
9. **无独立路由的能力**：Web 的 Sheet/Drawer 能力与 App 独立页面是否共享同一业务动作注册和回读范围？
10. **页面命名**：`客户`、`我的客户`、`客户360`、`客户与设计`、`本店客户`、`客户经营`如何统一为一个概念体系？

## 8. 本清单不作为设计定稿

本文件只回答“现在有什么”。页面统一和设计应另起 `客户页面规则统一矩阵`，至少包含：

```text
客户主工作面
+ 客户列表/队列
+ 客户对象子页
+ 客户动作页
+ 客户选择器
+ 角色投影
+ Web/App 差异
+ Provider 与回读
+ 七态
```

## 9. 后续业务能力清单

客户页面应具备的业务能力已拆入 `14-customer-page-business-capability-list-v0.1.md`。本清单只保留 Web/App 现状，不直接作为页面设计稿。

## 10. 证据

- Web 路由：`src/roles/routes.ts`
- Web 角色菜单：`src/roles/workbenchConfig.ts`
- Web 客户页面与组件：`src/views/**`、`src/components/customer/**`
- 小程序页面注册：`src/pages.json`
- 小程序路由常量：`src/config/index.ts`
- 小程序权限：`src/roles/catalog.ts`、`src/roles/permission.ts`、`src/roles/guard.ts`
- 首轮财务客户菜单验收：`12-FM-02-customer-acceptance.md`
