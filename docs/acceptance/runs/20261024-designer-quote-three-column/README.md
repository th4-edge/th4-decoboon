# 2026-10-24 设计师「待我谈单」三栏结构统一

> 状态：`DEV_VERIFIED / UI_REVIEWED / USER_ACCEPTANCE_PENDING`  

## 对齐目标

统一设计师所有「待我--」页面的逻辑骨架：

```text
左侧：服务端待办队列
中间：当前对象工作面
右侧：风险、下一步、主行动、次要操作、下一项
```

## 调整前

- 待我谈单为两栏；
- 风险、下一步和主行动混在中栏；
- 没有独立右栏、下一项和客户档案快捷入口；
- 与待我跟进、待我量房、待我设计、待我签约结构不一致。

## 调整后

- 使用唯一三栏工作台几何 `workbench-layout`；
- 左栏保留服务端分页、筛选、搜索；
- 中栏显示报价对象、金额、停留天数、调整次数、客户电话和谈单依据；
- 右栏展示：
  - 本次谈单；
  - 风险原因；
  - 下一步；
  - 进入报价工作区；
  - 联系客户；
  - 完整档案；
  - 报价历史；
  - 刷新；
  - 下一项；
- 选择报价后写回 `customerId / activePropertyId / quoteId` 对象锚点；
- 菜单首次进入 `quoteId` 可恢复服务端对应分页位置。

## 验证

- 1440 三栏实测：`290px 488px 290px`；
- 1024 响应式为单列，无横向溢出；
- 当前报价 `Q1790383677036` 同时显示在队列、中栏和右栏上下文；
- 右栏包含「本次谈单」「其他可执行操作」；
- Console 0 error；
- Web type-check/build 与 actions/permissions/workbenches/workspaces PASS。

## 用户复核后的二次修正

首次三栏化后仍存在三处明显差异：

1. 左栏分页 footer 有 DOM 和文本，但缺少完整样式，视觉上不成立；
2. `dq__eyebrow` 被 `.dq__head p` 更高优先级覆盖，标题眉题实际显示为次级灰色；
3. 中栏没有复用 `CustomerHeroCard`/`HeroFocusItem`，右栏操作仍用普通按钮，未复用 `WorkbenchQuickAction`。

二次修正：

- 补齐左栏分页样式：flex、分隔线、按钮、disabled 状态；
- 眉题颜色改为品牌色并显式覆盖页面说明色，实测 `rgb(38,101,74)`；
- 中栏改为 `CustomerHeroCard + HeroFocusItem`；
- 右栏次要操作改为 4 个 `WorkbenchQuickAction`：打电话、完整档案、报价历史、刷新数据；
- 保留主行动、风险原因、下一步、下一项和服务端分页；
- 1440/1024 无横向溢出，console 0 error。
