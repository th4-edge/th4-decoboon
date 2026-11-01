# 财务客户负责人候选目录权限修复

## 用户报告

财务登录后进入客户详情，点击“客户负责人”，页面提示“当前账号无权执行该操作”。

## 复现

- 角色：财务 `13910002003`
- 页面：`/workbench/customers/242541216321765376`
- 点击控件：客户负责人
- 复现结果：提示两次“当前账号无权执行该操作”，负责人候选框为空。
- 根因：财务点击后调用 `user/employee/page`，该 Provider 未允许 Finance，直接403。

## 业务预期

`USER-DEC-20261029-009` 明确财务可以转交客户负责人，但不能指派/改派设计师。总部财务企业范围，门店财务本门店范围。

## 修复结果

- 财务 API：`user/employee/page` 返回200，9名企业候选；
- 店长 API：200，8名本店候选；
- 业务员 API：仍403；
- 财务 Web 客户详情点击负责人：无权限错误，下拉出现9名候选；
- 选择现任负责人“审计业务”提交：提示“已转交客户负责人”，DB负责人保持“审计业务”，未改变业务归属；
- 定向测试、后端构建、App type-check/build、客户管理契约、Provider响应契约检查通过；
- App已移除本地门店二次过滤，dev构建已更新。

## 限制

App 与 Web 共用 `user/employee/page`，Provider已修复；miniprogram-automator未成功触发App负责人控件 `@tap`，App UI保留用户手工复验。

## 证据

- 修复前：`evidence/01-finance-customer-detail-before.*`、`02-finance-click-owner-permission-error.*`
- Web修复后：`evidence/03-finance-owner-dialog-after-fix.*`、`04-finance-owner-options-after-fix.*`
- 同负责人提交：`evidence/07-finance-owner-transfer-same-owner-after-fix.*`
- API与权限范围：`evidence/05-finance-employee-candidates-after-fix.json`
- DB：`evidence/08-customer-owner-db-after-fix.tsv`
- 代码变更：`src/main/java/com/th4/edge/tenant/user/service/user/employee/page/support/Page.java`
- 定向测试：`src/test/java/com/th4/edge/tenant/user/service/user/employee/page/support/EmployeePageAccessTest.java`
