# Web 业务界面内部编号清理 · 开发验证证据

> 状态：`DEV_VERIFIED_PENDING_USER`  
> 风险级别：L1  
> 变更记录：`CHG-20261029-022-web-identifier-exposure-cleanup.md`  
> 业务决策：`DEC-095` / `BR-060` / `USER-DEC-20261029-008`

## 1. 结论

Web 个人中心和个人信息、企业信息不再出现员工编号、企业编号。项目、报价、合同、方案、费用、凭证等业务界面不再把内部系统编号作为默认识别信息；搜索提示改为名称、客户、房屋和地址。

业务对象、权限、Provider 主键、审计、二维码和法定打印模板中的编号字段均未删除。

## 2. 业务边界

- 使用名称、地址、角色、状态、金额、日期、责任人识别对象。
- 内部 ID 和业务流水号只用于数据、接口、权限、审计、日志和必要的法定打印载体。
- 统一社会信用代码、法定证照号码、发票法定号码不按内部编号处理。
- 凭证模板设计器和打印模板允许保留凭证号字段，因为其不是普通业务流程识别入口。

## 3. 页面变化

### 个人中心

- 删除最左身份卡中的“员工编号”。
- 删除“基本资料”中的“员工编号”。
- 保留姓名、手机号、状态、角色、数据范围、门店和团队。

![个人中心 1440](screenshots/account-profile-1440.png)

### 企业信息

- 删除“企业编号”。
- 保留企业展示名称、简称、办公地址与品牌资料。

![企业信息 1440](screenshots/account-company-1440.png)

### 其他 Web 页面

- 报价、合同、项目、方案卡片移除系统编号；
- 费用、凭证列表改用摘要、类型、对方、项目和时间识别；
- 项目/客户/员工/房屋/应付/应收选择器移除编号；
- 搜索提示移除“编号”。

## 4. 响应式与 DOM 证据

| 页面 | 1440×900 | 1024×768 | 390×844 |
| --- | --- | --- | --- |
| 个人信息 | overflow 0；编号词 0 | overflow 0；编号词 0 | overflow 0；编号词 0 |
| 企业信息 | overflow 0；编号词 0 | overflow 0；编号词 0 | overflow 0；编号词 0 |

原始 DOM 证据：`idless-personal-center-evidence.json`。

## 5. 门禁

```text
npm run check:identifier-exposure:self-test  PASS
npm run check:identifier-exposure            PASS
npm run check:ai-flavor                      PASS
npm run check:client-copy                    PASS
npm run type-check                           PASS
npm run build                                PASS
```

编号门禁规则：

- 扫描 `src/views/**`、`src/layouts/**`、`src/components/**`；
- 拦截用户可见的“编号/工号/单号”；
- 拦截模板直接输出 `empId`、`comId`、`customerId`、`quoteNo`、`contractNo`、`project.no` 等；
- 拦截“搜索编号”类可见提示；
- 排除技术日期格式、统一社会信用代码、法定证照号码和凭证套打模板。

## 6. 业务知识落位

- `EV-427`：用户确认业务界面不展示系统内部编号
- `DEC-095`：业务界面不展示系统内部编号
- `BR-060`：业务界面内部编号隐藏规则
- `T12-BIZ-20261029-003`：研发交接
- 项目产品决策：`USER-DEC-20261029-008`
- 业务知识校验：`288 张知识卡 / 405 条注册证据，PASS`

## 7. 未闭环

- Web 本批已完成门禁与重点页面真实渲染。
- App/Admin 存量页面的同类清理需按 `BR-060` 分批执行并补门禁。
- 未发布生产，最终感受由用户验收。
