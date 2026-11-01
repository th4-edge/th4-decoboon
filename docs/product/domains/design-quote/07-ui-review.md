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

## DQ-X1 待我谈单页面复核（2026-10-24）

| 角色 | 视口 | 结果 | 证据 |
| --- | --- | --- | --- |
| Designer | 1440×1000 | 14 个当前任务；1/1 页；console 0 error | `docs/acceptance/runs/20261024-dq-x1/screenshots/designer-quotes-1440.png` |
| Boss | 1440×1000 / 1280×900 | 22 个当前任务；1/2 页；翻页后 2/2 且总数不变；console 0 error | `boss-quotes-1440.png`、`boss-quotes-page2-1440.png`、`boss-quotes-page2-1280.png` |
| StoreManager | 1440×1000 | 22 个本店当前任务；1/2 页；console 0 error | `store-manager-quotes-1440.png` |
| Finance | 1440×1000 | 直链跳转 `/forbidden`；console 0 error | `finance-forbidden-1440.png` |

复核中发现并修复两个阻断页面渲染的既有未提交问题：

- `CustomerHeroCard.vue` 引用未定义 Sass token `$danger-600`，改为 `$danger-500`。
- `DesignerMeasureTasksView.vue` 将 `number | null` 直接绑定到字符串房屋 ID，改为显式转换。

本记录为开发侧 UI 证据，不替代独立验收。

## DQ-X3 / DQ-X4 / DQ-X5 · App 方案模版真实页面复核（2026-10-30）

- 当前状态：`UI_REVIEWED`（微信开发者工具原生小程序）；物理真机未执行。
- 环境：iPhone 16 模拟器，393×754，DPR 3，SDK 3.17.5；`dist/dev/mp-weixin`；本地 8090 后端。
- 证据：`th4-decoboon-app/docs/acceptance/runs/20261030-dq-x3-x5-app-template-acceptance/`。
- 覆盖：
  - 模板库空态、筛选、新建入口；
  - 新建草稿、区域、分项/公式、发布确认与发布后只读；
  - 模板详情、无价格展示、实例化并回读方案；
  - 新建 V2/V3 草稿版本、幂等复用、复制为个人草稿；
  - 停用确认、全部状态可见、已停用筛选与停用后阻断；
  - 缺房屋上下文、空公式、错误单位、引用区域删除、非维护者动作。
- 最终列表/详情/方案页 console error：0。
- 视觉：`qwen2.5vl:7b` 完成两轮机器预检；7B 多图评审存在状态误判，只作为预检，最终视觉签字待人工或更强视觉模型。

### 真实页面修复

| ID | 页面 | 现象 | 级别 | 状态 |
| --- | --- | --- | --- | --- |
| UI-DQ-TPL-001 | App 模板编辑器 | 全局确认弹窗宿主缺失，发布/删除无反馈 | P1 | 已修复 |
| UI-DQ-TPL-002 | App 模板详情 | 区域类型显示内部枚举 `Bedroom` | P1 | 已修复 |
| UI-DQ-TPL-003 | 模板新版本/复制 | 区域参考面积、周长丢失 | P1 | 已修复并补定向测试 |
| UI-DQ-TPL-004 | App 模板编辑/详情 | 模板主档状态压过当前版本状态 | P1 | 已修复 |
| UI-DQ-TPL-005 | 模板列表 | “全部状态”排除已停用，已停用筛选失效 | P1 | 已修复 |
| UI-DQ-TPL-006 | 模板 Provider | 响应泄漏 `materialPrice/manualPrice/fixedTotalPrice` | P1 | 已修复并补白名单契约测试 |
| UI-DQ-TPL-007 | App 模板详情 | 英文 eyebrow `DESIGN TEMPLATE` | P2 | 已修复 |

### 未覆盖与边界

- 未连接物理 iPhone/Android，不声明物理真机通过。
- 独立验收尚未签署；开发侧真实页面证据不能替代 `ACCEPTED`。
- 房屋删除 `GAP-WEB-025` 保持独立待裁决，不在本批扩展。

## DQ-X6 财务报价页功能复核（2026-10-31）

- 角色：财务 `审计财务`，本地数据范围。
- 页面：`/finance/quotes`。
- 结果：KPI 显示报价总数 2、报价总额 ¥3,522.44、成本未完整 0、预计毛利 ¥895.94；summary 请求实际发生 1 次；console error 0，失败请求 0。
- 1440/1280/1024 三档截图已保存，1280/1024 无横向溢出。
- 当前主模型不支持图片输入，因此不声明视觉评审通过；本记录仅证明功能、数据和响应式结构。
- 证据：`docs/acceptance/runs/20261031-dq-x6-quote-finance-summary/`。

## DQ-X7 项目合同档案功能复核（2026-10-31）

- 页面：`/designer/projects/241940507872198656/contract`，Boss 真实账号。
- 合同范围显示 6 项 / ¥977.80；合同变更显示 1 项 / ¥-168.00，均与服务端聚合一致。
- 修复施工计划验收方式 `[object Object]`，重新渲染显示中文枚举文案。
- 1440/1280/1024 三档截图已保存；1440 console/network error 0，1280/1024 无横向溢出，无 `NaN/undefined/null/[object Object]`。
- 当前主模型不支持图片输入，不声明视觉评审通过。
- 证据：`docs/acceptance/runs/20261031-dq-x7-contract-archive-summary/`。

## DQ-X8 合同行分页功能复核（2026-10-31）

- 页面：`/designer/projects/241940507872198656/contract`，Boss 真实账号。
- 网络请求为 `current:1,size:20`，不再出现固定 `size:500`；页面显示 6 项 / ¥977.80，6 条合同行全部可读。
- 合同变更页面发起减项并打开来源选择：显示 7 个服务端 range 来源，网络只请求 `contract-change/page` 与 `contract-change/range`，没有合同行大页请求。
- 1440/1280/1024 均无横向溢出；console 0 error，网络无 4xx/5xx。
- 当前真实合同只有 6 行，翻页按钮未出现；真实 API 以 `size:2` 验证相邻页 ID 无交集，Provider 单测验证默认/上限/窗口。
- 当前主模型不支持图片输入，不声明视觉评审通过。
- 证据：`docs/acceptance/runs/20261031-dq-x8-contract-line-pagination/`。

## DQ-X9 小程序合同范围功能复核（2026-10-31）

- 页面：微信开发者工具原生 `subpackages/project/pages/contract`，Boss 真实账号，真实项目合同。
- 合同范围显示“共 6 项 · 合计 ¥978”，并明确“合同明细已全部加载”；4/4 页面检查 PASS。
- H5 实际请求 `current:1,size:20`，不再使用无界合同行调用；H5 DOM 393px 无横向溢出。
- 原生小程序截图 `app-contract-native.png` 已保存。
- H5 开发运行器存在 uni-h5 `clientWidth` 既有运行时错误，不作为小程序原生通过依据。
- 当前主模型不支持图片输入，不声明视觉评审通过。
- 证据：`docs/acceptance/runs/20261031-dq-x9-owner-contract-line-pagination/`。

## DQ-X10 后端成本聚合（2026-10-31）

- 本批不改变 Provider 响应字段、页面交互或视觉样式，未重复执行 Web/App 页面回归。
- 合同 workbench 的真实 API 响应已对 Boss、Designer、StoreManager 三种角色验证，数据与 DQ-X9 一致。
- 页面证据继续引用 `20261031-dq-x8-contract-line-pagination` 与 `20261031-dq-x9-owner-contract-line-pagination`；不新增视觉结论。

## DQ-X11 增减项来源选择器功能复核（2026-10-31）

- Web：发起减项 → 打开来源选择 → 主材筛选，显示 3 项；请求体包含 `current:1,size:20,itemType:"Material"`，console 0 error。
- 小程序原生：同一链路初始显示 7 项，主材筛选后 3 项，2/2 检查 PASS。
- 真实合同只有 7 个来源，Web 分页器未在真实数据下出现；分页窗口由 API size=2 三页验证。
- H5 开发运行器仍有既有 uni-h5 控制台错误，不作为原生页面通过依据。
- 当前主模型不支持图片输入，不声明视觉评审通过。
- 证据：`docs/acceptance/runs/20261031-dq-x11-contract-change-range-picker/`。

## UI-QF-01 设计队列同族降噪（2026-11-01）

- 页面：`/designer/followup`、`/designer/measure`、`/designer/contract`。
- 删除重复眉题、动作二次说明、泛化完成提示、量房重复下一动作及签约重复刷新。
- 保留待跟进下一步、量房真实阻断、签约下一责任人和三页全部既有合法动作。
- 精确 DOM 结果：三页重复眉题节点均为 0；量房重复下一步节点为 0；泛化完成说明为 0；签约“下一责任人”仍存在。
- 三页 1440/1280/1024 均无横向溢出；Web type-check 与完整 build PASS。
- 本地视觉模型出现明显结构误判，不据此扩改；人工视觉签字和独立验收未执行。
- 证据：`th4-decoboon-web/output/playwright/designer-queue-family-ia-20261101/`，变更 `CHG-20261101-020`。
