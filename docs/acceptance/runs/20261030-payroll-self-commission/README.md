# 2026-10-30 员工本人提成跨端开发验证

## 结论

- Domain：`payroll-labor`
- Batch：`PAY-SELF-COMMISSION`
- Phase：`ACCEPTED`
- 状态：`USER_ACCEPTED_NON_INDEPENDENT / PENDING_RELEASE`
- 未发布、未独立验收

## Provider 对账

| 能力 | Provider | App | Web | 服务端权限 |
| --- | --- | --- | --- | --- |
| 本人提成列表 | `payroll/commission/page` | `myCommissionPage(current,size)`，内部固定 `selfOnly:true` | `myEmployeeCommissions({ current,size })`，内部固定 `selfOnly:true` | 非管理员工始终 Self；管理角色 `selfOnly=true` 时也只返回登录人 |
| 本人提成详情 | `payroll/commission/info` | `myCommissionInfo(commissionId)` | `myEmployeeCommissionInfo(commissionId)` | 统一 DataScope Specification；他人记录返回不存在或无权访问 |
| 管理端员工提成 | 同一 Provider | 原 `commissionPage` 保留 | 原 `employeeCommissions` 保留 | BOSS/Finance/StoreManager 原企业/门店范围不退化 |

## 完整闭环范围

本批只补“员工本人查看本人提成”的读侧闭环，不开放计提、释放、负向调整、发放或管理动作。

## 自动化证据

- 后端定向：8/8 PASS。
- Java 22 全量：1144 tests / 0 failures / 0 errors / 0 skipped。
- App：薪酬契约、type-check、UI、导航、小程序构建、分包检查 PASS。
- Web：生产构建及其门禁、权限、门店范围、工作台检查 PASS。
- App `check:client-copy` 被并行改动中的设计模板页阻断，与本批文件无关；本批新增/修改页面和 API 已做定向文案扫描，未发现生产界面研发型文案。

## 真实页面证据

| 场景 | App（微信开发者工具） | Web | 结果 |
| --- | --- | --- | --- |
| 普通员工本人提成 | 仅 1 条：提成验收甲 / ¥2,500 / 已释放 ¥1,500 / 已发放 ¥500 / 待发放 | 同一记录和金额 | PASS |
| 本人详情 | 主合同签约额 10万、比例 2.5%、已计提/释放/发放和备注一致 | 同一字段一致 | PASS |
| 本人只读 | 卡片无计提、释放、调整、发放按钮 | 页面无管理动作按钮 | PASS |
| 他人详情拒绝 | 已知他人记录请求返回 404“提成不存在或无权访问” | API 层同样 404 | PASS |
| Finance 本人视角 | 仅本人 ¥600 / 已发放记录 | 仅本人 ¥600 / 已发放记录 | PASS |
| Finance 管理范围 | 管理页显示 3 条（张伟/乙/甲） | 管理页显示同样的 3 条 | PASS |
| 响应式 | 微信开发者工具 390 宽渲染 | 1440/1280/1024 均无横向溢出 | PASS |

## 证据目录

- `mini/results.json`：微信开发者工具自动操作与 8 项检查 PASS。
- `web/results.json`：Web 菜单、本人页、详情、管理层和响应式证据。
- `api/results.json`：接口范围、越权和篡改参数证据。
- `h5/results.json`：App H5 辅助回归；不作为小程序正式体验证据。
- `test-fixtures.md`：本地隔离夹具与清理记录。
- `cleanup-verification.txt`：测试员工和提成记录删除后均为 0。

## 当前门禁

- 真实 App/Web 页面：PASS。
- 独立验收：未执行；不得自称独立验收。
- 用户确认：2026-10-30 已确认真实页面验收结果，记 `MAPPED_USER_ACCEPTED_NON_INDEPENDENT`。
- 生产发布：未执行。
