# PAY-MANAGER-SENSITIVE-FIELDS · 店长本店薪酬字段可见范围

## 状态

- 批次：`PAY-MANAGER-SENSITIVE-FIELDS`
- 业务裁决：`DEC-096` / `ANS-BIZ-20261030-001` / `EV-428`
- 当前状态：`DEV_VERIFIED / PENDING_REAL_STORE_MANAGER_UI`
- DDL/迁移：无

## 用户裁决

店长在本店范围内可查看员工薪酬详情全部现有字段，字段范围与 BOSS 在门店范围内一致；跨店、跨企业仍拒绝。B7 原按 `DEC-093` 保持动作权限不变；动作权限随后由 `DEC-097` 在本店范围放开，单独转入 B8A。

- 决策：`DEC-096`
- 答复：`ANS-BIZ-20261030-001`
- 缺口：`GAP-074` 已关闭

## 实现边界

- 不新增银行卡、身份证、税率、应纳税所得额等当前不存在字段。
- 保留 `payroll/store-payroll/info` 和 `payroll/item/page` 对店长的现有完整字段返回。
- 增加回归断言，防止后续把 `socialInsuranceAmount`、`taxAmount` 等详情字段误裁剪。
- 本批字段读取不覆盖动作实现；动作角色、混合门店和跨店验收见 B8A。
- 不改变工资公式、成本、应付、工资条和发放。

## 验证

- `PayrollCompensationGuardTest` 定向断言店长本店详情字段完整。
- Java 22 全量测试。
- 后端定向门店范围/权限回归。
- 真实店长页面验收在受控企业补录；无生产发布。
