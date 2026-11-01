# Web 客户概念工作台 · 交互设计验收记录

- 日期：2026-10-29
- 类型：高保真 Web 交互原型
- 状态：`PROTOTYPE_RENDERED / USER_REVIEW_PENDING`
- 原型：`docs/product/prototypes/customer-workspace/index.html`
- 设计依据：`14-customer-page-business-capability-list-v0.1.md`、`15-customer-page-rule-freeze-candidate-v2.md`
- 边界：示例数据，不接真实 Provider；不是生产页面、不是独立验收、不是发布候选

## 1. 页面意图

- 页面类型：角色化客户场景页 + 客户卡片 + 客户详情下钻
- 目标角色：企业负责人、店长、财务
- 第一问题：
  - 企业负责人：当前客户变化如何影响经营？
  - 店长：本店客户由谁负责、哪些需要推进？
  - 财务：哪些客户有合同、项目、应收和财务下一步？
- 三秒结论：通过角色统计、门店范围、负责人分组和客户卡片的下一步达成
- 唯一主行动：每张客户卡一个当前下一步；详情内一个当前主行动
- 渐进披露：客户卡 → 客户详情 → 房屋业务链 → 专业工作面

## 2. 冻结方向验证

| 规则 | 原型实现 |
| --- | --- |
| Web 不采用三栏对象工作面 | 页面为“统计 + 查询/筛选 + 负责人分组 + 客户卡片” |
| 企业负责人有全企业统计和负责人分组 | `role=boss` 显示 4 项全公司统计、门店筛选、负责人分组 |
| 店长补齐本店统计和本店负责人分组 | `role=store` 显示本店统计、锁定本店范围、本店负责人分组 |
| 财务使用同一客户卡片体系 | `role=finance` 显示合同、项目、待收、逾期统计和客户卡片 |
| 财务卡保留量房、设计、报价、合同、项目、应收 | 财务卡业务链显示量房、设计、报价状态，并展示合同/项目/待收/逾期 |
| 客户卡是协作桥接点 | 卡上同时呈现身份、协同人、经营事实和下一步；点击打开详情 |
| 不复制专业工作台 | 详情只展示房屋和业务链摘要，不包含方案/报价/合同/施工编辑器 |

## 3. 交互验收

| 交互 | 结果 | 证据 |
| --- | --- | --- |
| 企业负责人 / 店长 / 财务切换 | PASS | 原型评审控制条；角色标题、统计、分组和卡片同步变化 |
| 负责人分组切换 | PASS | 分组按钮更新客户卡过滤结果 |
| 财务“有逾期”筛选 | PASS | 仅显示陈先生一张卡 |
| 搜索客户 | PASS | 关键词过滤客户卡 |
| 门店筛选 | PASS | 企业负责人/总部财务可切换全部门店与单店；店长锁定本店 |
| 点击客户卡打开详情 | PASS | 详情展示多房屋、业务链、跟进和当前主行动 |
| 键盘 Enter 打开客户卡 | PASS | 卡片可聚焦；Enter 打开详情 |
| Escape 关闭详情并归还焦点 | PASS | 焦点返回打开卡片的客户 |
| 刷新反馈 | PASS | 原型显示加载态后恢复 |

## 4. 渲染证据

| 视口 | 结果 | 证据 |
| --- | --- | --- |
| 1440×900 财务 | PASS | `screenshots/finance-1440x900.png` |
| 1280×800 财务 | PASS | `screenshots/finance-1280x800.png` |
| 1024×768 财务 | PASS | `screenshots/finance-1024x768.png` |
| 1440×900 企业负责人 | PASS | `screenshots/boss-1440x900.png` |
| 1440×900 店长 | PASS | `screenshots/store-1440x900.png` |
| 客户详情抽屉 | PASS | `screenshots/finance-drawer-1440x900.png` |

### 渲染检查

- 1440/1280/1024 均无横向溢出；
- 1440/1280/1024 页面可见文字均 ≥12px；
- 浏览器 Console 0 error / 0 warning；
- 1024 下客户详情抽屉宽度 720px，页面无横向溢出；
- 页面使用 Web Token 的颜色、字阶、圆角和阴影语义；
- 正常、加载、空、失败、无权限、长内容、极端值均有状态预演。

### 七态证据

- `screenshots/states/normal-1440x900.png`
- `screenshots/states/loading-1440x900.png`
- `screenshots/states/empty-1440x900.png`
- `screenshots/states/error-1440x900.png`
- `screenshots/states/forbidden-1440x900.png`
- `screenshots/states/long-1440x900.png`
- `screenshots/states/extreme-1440x900.png`

## 5. 视觉预检

使用本地 `qwen2.5vl:7b` 进行视觉预检：

- `finance-vision-review.md`
- `boss-vision-review.md`
- `finance-1024-vision-review.md`

预检结论：

- 页面具有真实工作台感；
- 企业负责人视角的统计层级清晰；
- 财务卡片信息密度较高，需要在用户评审时重点检查卡片是否过密；
- 机器视觉提出“统计分类可加强说明、状态标签对比度、卡片密度”等意见，后续由用户按真实业务判断是否调整。

注意：本地 7B 视觉模型只作为预检，不替代人工最终审美和业务验证。

## 6. 当前限制

- 使用示例数据，不接真实 Provider；
- 企业负责人统计与分组使用 App 已在用的 `customer/customer/overview` 形态模拟；
- 店长统计和本店负责人分组当前后端只有 Boss 聚合能力，真实生产落地需要另行确认 Provider 缺口；
- 财务卡片中的合同、项目、应收聚合需要后续做现有 Provider 字段对账；
- 本轮不修改生产 Web 页面、后端、数据库和权限规则；
- 正式发布前必须接入真实 Provider 并做真实角色验收。

## 7. 评审入口

直接打开：

```text
docs/product/prototypes/customer-workspace/index.html
```

角色和状态参数：

```text
?role=boss
?role=store
?role=finance
?state=normal|loading|empty|error|forbidden|long|extreme
?review=0
```

## 8. 结论

```text
PROTOTYPE_RENDERED = PASS
INTERACTION_SMOKE = PASS
RESPONSIVE_OVERFLOW = PASS
VISUAL_PREFLIGHT = PASS_WITH_USER_REVIEW_REQUIRED
PRODUCTION_INTEGRATION = NOT_STARTED
BACKEND_CHANGE = NONE
USER_ACCEPTANCE = PENDING
```
