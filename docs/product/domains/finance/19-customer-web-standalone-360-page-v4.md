# Web 独立客户 360 页面 v4

- 日期：2026-10-29
- 状态：`DEV_VERIFIED / USER_EXPERIENCE_PENDING`
- 客户列表：`/workbench/customers`
- 客户 360 独立页面：`/workbench/customers/:customerId`
- 设计预览：`/workbench/customers?preview=1`
- 生产源码：`../../前端代码/th4-decoboon-web/src/views/workbench/StoreCustomerView.vue`
- 路由：`src/roles/routes.ts`

## 1. 本轮修正

用户明确要求：

1. 客户详情的展开方式不能继续使用 drawer；
2. 客户详情必须是独立页面；
3. 其他业务页面也能通过客户入口进入同一个客户 360；
4. 客户 360 必须返回进入前页面，返回后再次点击客户仍可正常进入。

## 2. 页面与路由

客户详情不再使用 Element Plus Drawer，而是独立路由页面：

```text
/workbench/customers/:customerId
```

页面保持在 `/workbench` 企业工作台壳层内，不新增第二套客户业务体系。

客户列表点击“查看客户”时使用 SPA 路由进入：

```text
/workbench/customers
→ /workbench/customers/:customerId
```

## 3. 返回与上下文

- 页面顶部提供“返回上一页”操作；
- 使用 Vue Router history 返回来源页；
- 无可用来源历史时回退到 `/workbench/customers`；
- 从客户列表进入时保留原筛选、搜索、门店和分组状态；
- 从其他业务页面进入时返回原业务页面；
- 再次点击客户入口仍进入同一个客户 360 页面。

## 4. 跨页面入口

- 客户列表直接进入 `/workbench/customers/:customerId`；
- 历史设计师客户 360 路径 `/designer/my-customers/:customerId` 重定向到统一客户 360；
- 消息通知中的客户入口指向统一客户 360；
- 客户主行动中原本指向旧客户列表锚点的路径改为统一客户 360；
- 企业负责人、店长、财务、业务员、设计师、项目经理的角色路由包均声明该页面，最终数据访问仍由服务端权限与数据范围裁剪。

## 5. 页面内容

独立客户 360 页面继续包含：

- 客户身份、阶段、等级、风险和标签；
- 客户负责人、负责设计师、下次跟进和当前下一步；
- 客户七段业务阶段；
- 多房屋选择；
- 当前房屋业务链；
- 需求、方案、报价、合同、项目、量房、收款七个关联页签；
- 客户资料与画像；
- 内容来源和归因触点；
- 客户跟进时间轴；
- 写跟进、打电话、复制号码、转交负责人、指派设计师、新增房屋和流失操作。

## 6. 验证

### 路由与返回

- 客户列表点击客户：PASS；
- URL 进入独立页面：PASS；
- 页面不存在客户 360 drawer：PASS；
- 点击返回上一页：PASS；
- 返回客户列表后保留 `?preview=1`：PASS；
- 返回后再次进入同一客户：PASS；
- 从财务工作台通过 SPA 路由进入客户 360：PASS；
- 从客户 360 返回财务工作台：PASS。

### 渲染

- 1440×900：`screenshots-v4/customer360-page-v4-1440x900.png`
- 1280×800：`screenshots-v4/customer360-page-v4-1280x800.png`
- 1024×768：`screenshots-v4/customer360-page-v4-1024x768.png`
- 三档均无横向溢出；
- 可见文字均 ≥12px；
- 可见按钮缺失图标数为 0；
- 页面无可见 customer drawer；
- Console 无 error。

### 视觉预检

- 模型：`qwen2.5vl:7b`
- 结果：`vision-v4/customer360-page-v4-1440-vision.md`
- 机器预检确认客户信息、业务进度和跟进记录层级清晰；最终视觉判断由用户完成。

## 7. 边界

- 未修改后端 Provider、权限、金额、成本、状态机或 DDL；
- 客户 360 页面数据仍使用现有客户、房屋、设计、报价、合同、项目、量房、应收和内容归因 Provider；
- 未部署、未独立验收。
