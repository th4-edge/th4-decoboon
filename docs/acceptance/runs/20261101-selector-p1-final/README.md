# P1 业务对象选择器统一（开发自测）

> 状态：`DEV_SELF_CHECK / WEB_RUNTIME_VERIFIED / PHYSICAL_DEVICE_NOT_RUN`
> 日期：2026-11-01
> 关联变更：`CHG-20261101-016`、`CHG-20261101-018`、`CHG-20261101-021`
> 业务依据：`DEC-021` / `BR-019`、`DEC-023` / `BR-021`、`DEC-024` / `BR-022`

## 1. 本批范围

- Web 员工选择器服务端分页、按 ID 回读、普通目录与薪酬目录 Provider 显式分离。
- 清除负责人、设计师、受理人、任务责任人、薪酬员工等入口的 `el-select` 绕过。
- 移除身份域遗留的员工/设计师全量读取 API。
- App 离职交接、任务负责人、财务工人、合同报价来源、供应商价格选择复核。
- 财务工资付款与付款补录的工人选择锁定为仅在职工人。

## 2. 结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| Web 员工分页门禁与负向自测 | PASS | `npm run check:forms:self-test && npm run check:forms` |
| Web 类型检查与生产构建 | PASS | 命令输出 |
| Web 业务概念/权限 | PASS | `check:concepts`、`check:permissions` |
| Web 动作注册门禁 | BLOCKED（既有退款/员工付款动作未登记，与本批选择器无关） | `check:actions` 输出 |
| Web 普通员工目录 | PASS；请求 `user/employee/page` | `evidence/web-runtime.json` |
| Web 薪酬员工目录 | PASS；请求 `payroll/employee/page` | `evidence/web-runtime.json` |
| TeamLeader 岗位可见 | PASS | 薪酬员工选择器实测 |
| Web 1440/1280/1024 | PASS；无横向溢出 | `evidence/web-runtime.json`、截图 |
| App 选择器/财务选择器/工人补录契约 | PASS | 命令输出 |
| App 类型检查与 mp-weixin 构建 | PASS | 命令输出 |
| 后端定向测试 | PASS；8 类 / 11 测试 | `evidence/web-runtime.json` |
| 视觉机器预检 | PASS（非人工视觉终审） | `evidence/vision-review-1440.md`、`vision-review-1024.md` |

## 3. App P1 路径

- 离职交接：`employee-picker?source=directory&activeOnly=1&excludeEmpId=...`。
- 任务负责人：`employee-picker?source=directory&activeOnly=1&roles=boss,storeManager,pm,designer,marketer`。
- 财务工人：工资付款和付款补录均进入 `worker-picker`，并传 `activeOnly=1`；停用工人不可用于新付款。
- 合同报价来源：`quote-picker` 固定 `Accepted` 状态，服务端搜索分页；合同页再次校验状态并处理报价更换后的关联失效。
- 供应商价格：供应商使用 `supplier-picker`，报价库项目使用 `CatalogPicker`；编辑按 `partnerPriceId`、`partnerId`、`itemId` 正读取。

## 4. 截图

- `evidence/screenshots/web-design-employee-directory-1440.png`
- `evidence/screenshots/web-design-employee-directory-1024.png`
- `evidence/screenshots/web-payroll-employee-directory-1280.png`
- `evidence/screenshots/web-customer-transfer-employee-directory-1280.png`

## 5. 边界

- 本批为开发自测，不是独立验收。
- App 本批未重新执行微信开发者工具原生小程序走查；使用静态契约、类型检查、构建和既有 DevTools 证据。
- 未连接物理 iPhone/Android。
- 未提交、未上传体验版、未发布。
