# 2026-10-23 Web 项目内合同档案

> 状态：`DEV_VERIFIED / UI_REVIEWED / USER_ACCEPTANCE_PENDING`

## 决策

- Web 不提供全局合同档案菜单；
- 合同档案通过项目工地 → 项目 → 合同档案进入；
- App 同样使用项目内合同路径；
- 合同页只读，不在合同档案内提供登记收款、合同修改、签约或生效动作。

## 实现

- 路由：`/designer/projects/:projectId/contract`
- 页面：项目合同档案
- Provider：
  - `project/contract/info`
  - `project/contract-line/page`
  - `project/contract-change/page`
- Web 项目卡片的“查看合同”已改为当前项目合同路由，不再跳待我签约队列。

## 验证

- 王亮项目 `229353590450290688` 可进入合同 `CC1790371757137`；
- 显示客户、房屋、项目、报价、合同状态、签约/生效日期、合同范围和变更历史；
- 页面无全局“合同档案”菜单；
- 页面无“登记收款”等写动作；
- 1440/1024 无横向溢出；
- Console 0 error。
