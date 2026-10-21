# 设计方案与设计报价审视 · UI 与交互审查

## 审查对象

- 页面/路由：
  - Web：`/workbench/design-reviews`、`/workbench/design-reviews/:designId`
  - Web：`/finance/design-reviews`、`/finance/design-reviews/:designId`
  - Web：设计师方案轨道与设计工作区
  - App：`subpackages/design/pages/review`、`review-detail`
  - App：设计工作台与方案列表
- 角色：BOSS、店长、财务、设计师
- 设备/分辨率：Web 1440×900、1280×900、1024×900；微信开发者工具原生 960×1708；App 同源 H5 390×844
- 业务蓝图节点：`priceProjection`、`currentQuote`、`uiBindings`、`costVisible`
- 环境：本地 dev 后端；麦鸿装饰 `com_id=1790066191585976744`；5 个方案、246 个设计分项、5 份报价

## 证据

| 场景 | 截图/路径 | 结果 | 问题 |
| --- | --- | --- | --- |
| Web BOSS 列表（1440/1280/1024） | `screenshots/boss-list-*.png` | PASS：5 个方案均显示价格，当前正式报价 `¥65,406.10`，无报价回退方案计算价；三档无横向溢出 | 无 |
| Web BOSS 详情（1440/1280/1024） | `screenshots/boss-detail-*.png` | PASS：正式报价、方案计算价、成本、毛利、47 个分项；门店显示“总店” | 已修复“门店待确认” |
| Web 店长列表/详情 | `screenshots/store-manager-list-1440.png`、`store-manager-detail-1440.png` | PASS：本店范围、价格与成本可见，只读 | 无 |
| Web 财务列表/详情 | `screenshots/finance-list-1440.png`、`finance-detail-1440.png` | PASS：成本/毛利可见，报价项目库与设计方案入口均可发现 | 无 |
| Web 设计师执行页 | `screenshots/designer-design-list-1440.png`、`designer-design-workspace-1440.png` | PASS：设计工作台/方案列表显示价格；页面无成本、毛利字段 | 无 |
| Web 设计师直链 | `screenshots/designer-review-forbidden-1440.png` | PASS：`/workbench/design-reviews` 进入无权访问页 | 无 |
| App 原生 BOSS/店长/财务列表 | `screenshots/app-mp-*-review-list.png` | PASS：微信开发者工具真实页面，5 个方案；正式报价、方案计算价、只读标识完整 | 无 |
| App 原生 BOSS/店长/财务详情 | `screenshots/app-mp-*-review-detail.png` | PASS：微信开发者工具真实页面；价格、成本、毛利、分项，无操作按钮 | 无 |
| App 原生设计师工作台 | `screenshots/app-mp-designer-workbench.png` | PASS：显示方案计算价 `11.8万`；无成本/毛利 | 无 |
| App 原生设计师运营页直链 | `screenshots/app-mp-designer-review-denied.png` | PASS：页面未渲染运营内容，`.dr`/方案卡数量均为 0 | 已修复 |
| App H5 交叉复核 | `screenshots/app-h5-*` | PASS：390×844 同源渲染与原生结果一致 | 无 |
| 空态 | 页面内置空态文案 | PASS：区分“当前范围没有设计方案”和接口失败 | 无 |
| 失败 | 接口错误文案 | PASS：显示“设计方案暂不可确认”，不伪装 0 元或空数据 | 无 |
| 无权限 | Web forbidden、App 首页回退 | PASS：服务端仍按公司/门店/本人范围二次校验 | 无 |
| 长内容 | 实测“荷塘家园24-2503 方案一”和长分项名称 | PASS：卡片换行，详情表不破坏布局 | 无 |
| 极端数值 | 实测 `¥82` 与 `¥118,025.45` 两档 | PASS：JzMoney 分别显示元/万元，无重复货币符号 | 已修复 |

## 缺陷

| ID | 页面 | 现象 | 期望 | 级别 | 状态 |
| --- | --- | --- | --- | --- | --- |
| UI-DQ-001 | Web/App 详情头部 | 门店被显示为“门店待确认” | 显示方案所属门店 | P1 | 已修复：`design/scheme/info` 白名单补 `store` |
| UI-DQ-002 | App 列表/详情/工作台 | 小于 1 万元金额出现 `¥¥82` | 统一使用金额组件，元/万元只出现一次币种 | P1 | 已修复：改用 `JzMoney` |
| UI-DQ-003 | App 运营查看深链 | 设计师可绕过入口直接打开运营查看页 | 与 Web 一致，仅 BOSS/店长/财务可达 | P1 | 已修复：新增 `view:design-review`，路由和页面双层拦截 |
| UI-DQ-004 | App 原生分包 | 新页面 WXSS 使用 `> :nth-child(3)` 导致微信编译失败，`navigateTo` 无回调 | 使用页面自有 class 控制布局，原生编译可通过 | P1 | 已修复：移除伪类子选择器，原生页面已实际打开 |

## 交互审查

- 页面只读，没有审批、核价、退回、通过、驳回或任务动作。
- 当前正式报价优先；没有正式报价时显示“方案计算价 · 待报价”；无分项显示“暂不可计算”。
- 成本不完整时显示“成本未完整”，不计算可靠毛利。
- 经营角色可在同一详情看到方案、正式报价、成本完整性和分项价格，不需要跳转多个模块拼信息。
- 设计师继续使用原设计工作区，不承担经营查看入口。

## 跨端一致性

- App 与 Web 均消费服务端 `calculatedAmount/currentQuote*`，不在前端自行选择当前报价。
- 金额文案分别遵循 H5/小程序 `JzMoney` 与 Web 金额格式，但金额口径一致。
- 设计师成本裁剪：Web DOM 无成本/毛利；App H5 无成本/毛利；服务端详情响应成本字段为 0。
- 微信开发者工具最终使用隔离项目真实打开 BOSS、店长、财务、设计师页面；原生小程序与 Web 价格口径一致。
- 初次原生编译发现 `> :nth-child(3)` 选择器不兼容，修复后重新编译并完成冒烟。

## 结论

- Web：通过。
- App H5 同源渲染：通过。
- 微信小程序原生页面：BOSS、店长、财务、设计师工作台与权限拦截均通过。
- UI 门禁：通过，可推进 `UI_REVIEWED`；独立验收仍不可由开发自签。
