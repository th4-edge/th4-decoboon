# Web 客户页面开发验收 · 2026-10-30

- 状态：`PASS / REAL_FINANCE_ACCEPTED`
- 页面：`/workbench/customers`
- 客户 360：`/workbench/customers/:customerId`
- 角色测试：Finance
- 数据模式：真实 Finance 数据 + 设计预览回归
- 边界：未修改后端业务逻辑；已使用真实 Finance 账号完成页面验收

## 1. 验收结论

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| 客户菜单与列表结构 | PASS | 客户卡片、负责人分组、筛选和搜索入口完整 |
| Finance 角色投影 | PASS | 使用 Finance 身份投影；客户负责人称谓正确 |
| 顶部统计 | PASS | Finance 与 Boss/店长统一消费 `customer/customer/overview`，不从分页列表计算 |
| 预览模式统计 | PASS | 使用聚合夹具，不从预览客户卡推导统计 |
| 负责人分组 | PASS | 使用 Provider `ownerGroups` / 聚合夹具，空态保留 |
| 客户卡内容 | PASS | 客户负责人、负责设计师、房屋、跟进、业务和下一步完整 |
| 客户 360 | PASS | 独立页面，不是 Drawer；含责任关系、业务阶段、多房屋、七类关联页签、资料、画像、内容来源和跟进 |
| 返回链路 | PASS | 客户列表 → 客户 360 → 返回列表 → 再次进入均正常 |
| 其他业务页进入 | PASS | SPA 历史返回来源页；旧设计师客户 360 路径统一重定向 |
| 响应式 | PASS | 1440/1280/1024 无横向溢出；1024 负责人分组自适应到列表上方 |
| 按钮图标 | PASS | 页面与客户 360 可见按钮缺失图标数为 0 |
| 字号 | PASS | 可见文字无小于 12px |
| Console | PASS | 无 error |
| 真实 Finance 数据验收 | PASS | 18660219918 Finance 登录成功；客户“郑和”、统计 1、负责人分组 1、两张房屋均正常 |

## 2. DOM 与交互证据

### 客户列表

- Finance 指标卡 4 个；
- 负责人分组包含“全部客户”与负责人；
- 客户卡 7 个（预览）；
- 卡片字段为“客户负责人 / 负责设计师 / 项目”；
- 无横向溢出；
- 缺失图标按钮数 0；
- 最小可见文字 ≥12px。

### 客户 360

- URL 进入 `/workbench/customers/preview-c1?preview=1&from=/workbench/customers?preview=1`；
- 可见独立页面 `.customer-detail--page`；
- 无可见客户详情 Drawer；
- 页面包含：
  - 装修房屋；
  - 当前房屋业务链；
  - 房屋关联信息；
  - 跟进记录；
  - 客户资料；
  - 客户画像；
  - 内容来源；
  - 客户动作；
- 七类页签：需求、方案、报价、合同、项目、量房、收款；
- 返回按钮回到原客户列表并保留 `?preview=1`；
- 再次点击客户可再次进入；
- 缺失图标按钮数 0；
- 最小可见文字 ≥12px。

## 3. 数据源验收

- Finance 客户列表真实模式只调用一次 `customer/customer/overview`；
- 不使用 `customer/customer/page` 后再由前端统计；
- 指标、阶段计数、负责人分组和列表均消费同一 overview 响应；
- 客户 360 只调用一次 `customer/customer/info`；
- 路由监听已去重，不重复请求同一客户；
- `CustomerOverviewFinanceScopeTest` PASS；
- `CustomerStoreScopeAnnotationTest` PASS。

## 4. 自动化门禁

```text
npm run type-check             PASS
npm run check:actions         PASS
npm run check:permissions     PASS
npm run check:states          PASS
npm run check:page-system     PASS
npm run check:pagination      PASS
```

## 5. 渲染证据

| 页面 | 视口 | 证据 |
| --- | --- | --- |
| 客户列表 | 1440×900 | `screenshots/customer-list-1440x900.png` |
| 客户列表 | 1280×800 | `screenshots/customer-list-1280x800.png` |
| 客户列表 | 1024×768 | `screenshots/customer-list-1024x768.png` |
| 客户 360 | 1440×900 | `screenshots/customer-360-1440x900.png` |

## 6. 视觉预检

- `vision/customer-list-1440-vision.md`
- `vision/customer-360-1440-vision.md`

机器视觉未发现阻断级布局问题；最终视觉判断仍需真实账号人工验收。

## 7. 真实账号验收

- 账号：18660219918 Finance；
- 企业：昭青装饰；
- 页面客户总数：1；
- 客户：郑和；
- 负责人分组：张伟 1；
- 客户 360 房屋：2；
- 客户 360 为独立页面，无 Drawer；
- 返回客户列表后客户仍正常显示；
- 验收过程中发现并修复 `overview.customers` 字段未被 Web 适配层读取的问题。

## 8. 最终判定

```text
DEV_ACCEPTANCE = PASS
REAL_FINANCE_ACCEPTANCE = PASS
CODE_REGRESSION_FOUND = YES / FIXED
  - overview.customers mapping
BLOCKING_UI_DEFECT = NO
```
