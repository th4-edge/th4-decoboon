# Web 客户概念页面 · 真实角色渲染与交互验收

- 日期：2026-10-29
- 状态：`DEV_VERIFIED / USER_EXPERIENCE_PENDING`
- 实际页面：`/workbench/customers`
- 设计预览：`/workbench/customers?preview=1`
- 涉及角色：企业负责人、店长、财务
- 性质：开发自测与真实浏览器渲染证据，不是独立验收，不是发布结论

## 1. 页面意图

| 项 | 内容 |
| --- | --- |
| 页面类型 | 角色化客户卡片场景页 |
| 企业负责人场景 | 从全企业客户变化、负责人分布和门店范围观察经营 |
| 店长场景 | 从本店客户变化、本店负责人分布找到下一步 |
| 财务场景 | 围绕客户查看量房、设计、报价、合同、项目、应收和财务下一步 |
| 第一问题 | 当前客户发生了什么、谁负责、我现在能做什么 |
| 三秒结论 | 角色统计 → 客户结果 → 客户卡当前下一步 |
| 主行动 | 每张客户卡一个当前下一步；详情内一个主行动 |
| 渐进披露 | 客户卡 → 客户详情 → 房屋业务链 → 专业工作面 |

## 2. 页面结构

```text
角色统计
→ 门店范围 / 搜索 / 筛选 / 排序
→ 负责人分组（Boss / StoreManager）
→ 客户卡片
→ 客户详情抽屉
```

- 企业负责人：全企业统计、门店筛选、负责人分组；
- 店长：本店同口径统计、锁定本店、本店负责人分组；
- 财务：不显示销售负责人分组，卡片重点显示合同、项目、待收、逾期、量房、设计、报价和财务下一步；
- 不使用三栏对象工作面。

## 3. 真实页面与预览模式

普通地址使用真实 Provider：

```text
/workbench/customers
```

开发环境设计预览使用同一页面与示例数据：

```text
/workbench/customers?preview=1
```

预览模式显示明确提示，不代表真实业务事实。

## 4. 交互验收

| 交互 | 结果 | 证据 |
| --- | --- | --- |
| 财务角色真实登录 | PASS | Finance 真实登录态 |
| 企业负责人真实登录 | PASS | Boss 真实登录态 |
| 店长角色投影 | PASS | 本地临时角色验证，验证后恢复 Finance |
| 企业负责人门店筛选 | PASS | 滨湖店客户卡 3、负责人组 4、统计切换为本店口径 |
| 财务“有逾期”筛选 | PASS | 仅陈先生一张卡 |
| 客户卡点击 | PASS | 打开客户详情抽屉 |
| 客户详情业务链 | PASS | 量房、设计、报价、合同、项目、应收状态出现 |
| 财务客户下一步 | PASS | 陈先生显示“登记收款” |
| 1024 客户详情 | PASS | 抽屉无横向溢出，文本 ≥12px |

## 5. 渲染证据

| 页面 | 视口 | 证据 |
| --- | --- | --- |
| 财务客户页面 | 1440×900 | `screenshots/finance-preview-1440x900.png` |
| 财务客户页面 | 1280×800 | `screenshots/finance-preview-1280x800.png` |
| 财务客户页面 | 1024×768 | `screenshots/finance-preview-1024x768.png` |
| 财务客户详情 | 1440×900 | `screenshots/finance-preview-drawer-1440x900.png` |
| 财务客户详情 | 1024×768 | `screenshots/finance-preview-drawer-1024x768.png` |
| 企业负责人客户页面 | 1440×900 | `screenshots/boss-preview-1440x900.png` |
| 店长客户页面 | 1440×900 | `screenshots/storemanager-preview-1440x900.png` |
| 财务真实空态 | 1440×900 | `screenshots/finance-real-empty-1440x900.png` |

### DOM / 渲染检查

- 1440 / 1280 / 1024 均无横向溢出；
- 客户页面内可见文本没有小于 12px；
- 1024 客户详情无横向溢出；
- 财务预览页面 Console 0 error；
- 真实空态、加载态、有权限状态由生产页面覆盖；
- 原型覆盖正常、加载、空、失败、无权限、长内容、极端值七态。

## 6. 视觉预检

- `finance-web-vision-review.md`
- `boss-web-vision-review.md`
- `storemanager-web-vision-review.md`

使用 `qwen2.5vl:7b` 做本地视觉预检。机器评审只作预检，最终视觉与业务判断由用户完成。

## 7. 已知边界

- `?preview=1` 使用示例数据，不代表真实客户事实；
- 真实 Finance/StoreManager 汇总统计仍需 Provider 对账；当前真实页面用“按当前客户列表统计”表达；
- 财务卡片的完整合同、项目、应收、量测/设计/报价聚合需要在真实数据环境中继续核对 Provider 字段；
- 当前分页使用 20 条首屏，后续是否补分页/加载更多需按真实数据规模确认；
- 本轮未修改后端、数据库、权限、金额或状态机。

## 8. 构建检查

- `npm run type-check` PASS；
- `npm run build` PASS；
- `npm run check:actions` PASS；
- `npm run check:permissions` PASS；
- `npm run check:workbenches` PASS；
- `npm run check:workspaces` PASS；
- `npm run check:forms` PASS；
- `npm run check:concepts` PASS；
- `npm run check:states` PASS；
- `npm run check:page-system` PASS；
- `npm run check:pagination` PASS。

## 9. 结论

```text
REAL_ROLE_RENDER = PASS
ROLE_PROJECTION = PASS
CUSTOMER_CARD_SCENE = PASS
RESPONSIVE = PASS
BUILD_AND_GATES = PASS
USER_EXPERIENCE = PENDING
INDEPENDENT_ACCEPTANCE = PENDING
BACKEND_CHANGE = NONE
RELEASE = NOT_STARTED
```

---

## 10. v2 客户中心卡片修正（2026-10-29）

用户体验反馈后完成第二轮重构：

- 客户卡改为小程序客户 Tab 的信息顺序：身份、阶段、房屋、协同人、行为标签、经营时钟、当前业务、下一步；
- 财务合同/项目/待收/逾期降为卡片底部低权重上下文；
- 移除“财务关注优先 / 风险优先 / 金额优先”等排序控件和前端重排；
- 左侧负责人分组对所有内部角色常驻，空客户时仍显示“全部客户”和分组空态；
- 页面及客户/房屋弹窗的功能按钮补齐图标，浏览器审计缺失图标数为 0。

### v2 渲染证据

| 页面 | 视口 | 证据 |
| --- | --- | --- |
| 财务客户页面 | 1440×900 | `screenshots-v2/finance-customer-v2-1440x900.png` |
| 财务客户页面 | 1280×800 | `screenshots-v2/finance-customer-v2-1280x800.png` |
| 财务客户页面 | 1024×768 | `screenshots-v2/finance-customer-v2-1024x768.png` |
| 财务客户详情 | 1440×900 | `screenshots-v2/finance-customer-v2-detail-1440x900.png` |
| 客户详情 | 1024×768 | `screenshots-v2/customer-v2-detail-1024x768.png` |
| 企业负责人客户页面 | 1440×900 | `screenshots-v2/boss-customer-v2-1440x900.png` |
| 店长客户页面 | 1440×900 | `screenshots-v2/storemanager-customer-v2-1440x900.png` |
| 空客户左侧分组 | 1440×900 | `screenshots-v2/finance-customer-v2-empty-1440x900.png` |

### v2 DOM 审计

- 1440 / 1280 / 1024 无横向溢出；
- 页面可见文字均 ≥12px；
- 当前页及客户详情可见按钮缺失图标数为 0；
- 财务空客户仍保留负责人分组区；
- 店长本店范围、负责人分组和客户卡同步；
- 客户详情 1440 无横向溢出；
- Console 无 error。

### v2 视觉预检

- 模型：`qwen2.5vl:7b`
- 结果：`vision-v2/finance-customer-v2-1440-vision.md`
- 结论：视觉层级、排版、配色、组件均为 4/5；产品真实性 3/5 的原因是预览数据明确为示例，不代表真实业务事实。

---

## 11. v3 客户 360 宽幅工作面（2026-10-29）

- 客户卡“顾问”统一改为“客户负责人”；
- 客户详情升级为宽幅客户 360，对齐小程序客户详情的信息顺序；
- 客户身份、负责人/设计师、业务阶段、多房屋、房屋关联信息、客户资料、画像和跟进时间轴进入同一工作面；
- 房屋关联信息包含需求、方案、报价、合同、项目、量房、收款七个页签；
- 1440 使用 1240px 宽幅双栏，1024 使用 992px 双栏，均无横向溢出。

### v3 渲染证据

| 页面 | 视口 | 证据 |
| --- | --- | --- |
| 客户 360 首屏 | 1440×900 | `screenshots-v3/customer360-v3-1440x900.png` |
| 客户 360 首屏 | 1280×800 | `screenshots-v3/customer360-v3-1280x800.png` |
| 客户 360 首屏 | 1024×768 | `screenshots-v3/customer360-v3-1024x768.png` |
| 房屋业务与关联信息 | 1440×900 | `screenshots-v3/customer360-v3-business-1440x900.png` |
| 内容来源与低频动作 | 1440×900 | `screenshots-v3/customer360-v3-content-source-1440x900.png` |

### v3 DOM 与交互审计

- 客户 360 宽度：1440 下 1240px，1024 下 992px；
- 1024 双栏：608px 主栏 + 280px 侧栏；
- 页面与客户 360 均无横向溢出；
- 客户 360 可见文字均 ≥12px；
- 客户 360 可见按钮缺失图标数为 0；
- 合同页签显示合同金额与生效时间；
- 项目页签显示当前阶段、进度、项目经理与合同额；
- 客户卡显示“客户负责人 / 负责设计师”，不再出现“顾问”；
- 内容来源展示主归因、辅助触点、归因员工、置信度和触达时间；
- Console 无 error。

### v3 视觉预检

- 模型：`qwen2.5vl:7b`
- 首屏：`vision-v3/customer360-v3-1440-vision.md`
- 业务区：`vision-v3/customer360-v3-business-1440-vision.md`
- 内容来源：`vision-v3/customer360-v3-content-source-1440-vision.md`
- 机器评审确认客户状态、业务进度、信息层级和按钮状态可识别；最终审美与业务判断仍由用户完成。

---

## 12. v4 独立客户 360 页面（2026-10-29）

- 客户 360 已从 Drawer 改为独立路由页面；
- 路由：`/workbench/customers/:customerId`；
- 客户列表和其他业务页面通过 SPA 路由进入同一页面；
- 页面顶部“返回上一页”通过 Vue Router history 返回来源页；
- 无来源历史时回退客户列表；
- 返回列表后原 `?preview=1` 与列表上下文保留；
- 再次点击客户可再次进入同一客户 360。

### v4 路由与返回证据

| 场景 | 结果 |
| --- | --- |
| 客户列表点击客户 | PASS，进入 `/workbench/customers/preview-c1?preview=1&from=/workbench/customers?preview=1` |
| 独立页面渲染 | PASS，存在 `.customer-detail--page` |
| Drawer 检查 | PASS，无可见 `.el-drawer` |
| 返回上一页 | PASS，返回 `/workbench/customers?preview=1` |
| 再次点击客户 | PASS，再次进入同一客户 360 |
| 从财务工作台 SPA 进入 | PASS，`history.state.back=/finance/workbench` |
| 返回财务工作台 | PASS，返回 `/finance/workbench` |

### v4 渲染证据

| 页面 | 视口 | 证据 |
| --- | --- | --- |
| 独立客户 360 | 1440×900 | `screenshots-v4/customer360-page-v4-1440x900.png` |
| 独立客户 360 | 1280×800 | `screenshots-v4/customer360-page-v4-1280x800.png` |
| 独立客户 360 | 1024×768 | `screenshots-v4/customer360-page-v4-1024x768.png` |

### v4 DOM 审计

- 页面无横向溢出；
- 客户 360 可见文字均 ≥12px；
- 客户 360 可见按钮缺失图标数为 0；
- 无可见客户详情 Drawer；
- Console 无 error。

### v4 视觉预检

- 模型：`qwen2.5vl:7b`
- 结果：`vision-v4/customer360-page-v4-1440-vision.md`

---

## 13. 客户经营统计同源修正（2026-10-29）

### 问题判定

- `customer/customer/info`：客户 360 正确详情接口，小程序详情页同样使用；
- `customer/customer/overview`：列表统计、负责人分组和客户分页聚合接口；
- 原 Web 只让 Boss 使用 `overview`，店长/财务使用 `page + overview(scope)`，统计再由前端首屏列表计算；
- 该实现违反 `WEB-列表分页与汇总数据规范`、`DEC-036` 与 `BR-031`。

### 修正结果

- Boss / 店长 / 财务统一只调用一次 `customer/customer/overview`；
- 顶部指标、负责人分组、待量房和全量客户数全部消费 Provider 响应；
- 前端不再从首屏客户列表计算经营统计；
- `?preview=1` 同样改为聚合夹具，不再从预览客户卡计算指标和分组；
- 客户 360 路由加载去重，详情只调用一次 `customer/customer/info`；
- 后端 `overview` 失败日志为 403 `当前账号无权执行该操作`，不是 SQL 或参数异常。

### 网络证据

| 场景 | 观测 |
| --- | --- |
| Finance 客户列表 | 仅 `customer/customer/overview` 1 次 |
| Provider mock 指标 | 991 / 992 / 993 / 994 原样显示 |
| Provider 总客户数 | “全部客户”显示 991 |
| Provider 待量房 | 全部客户分组显示待量房 993 |
| 客户 360 | `customer/customer/info` 1 次 |
| 重复路由触发 | 无重复 `info` 请求 |
| 预览模式 | 指标与分组取聚合夹具；无客户列表统计计算；无客户接口请求 |

### 测试

- `mvn -q -Dtest=CustomerOverviewFinanceScopeTest test`：PASS；
- `mvn -q -Dtest=CustomerStoreScopeAnnotationTest test`：PASS；
- `npm run build` 与页面门禁：PASS。
