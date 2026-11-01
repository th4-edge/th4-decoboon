# 2026-10-30 店长本店薪酬字段可见范围

## 结论

- Domain：`payroll-labor`
- Batch：`PAY-MANAGER-SENSITIVE-FIELDS`
- Business decision：`DEC-096`
- Phase：`DEV_VERIFIED`
- 状态：`PENDING_REAL_STORE_MANAGER_UI`
- 未发布、未独立验收

## 冻结口径

1. 店长在本店范围内可读取员工薪酬详情的全部现有字段。
2. 字段范围与 BOSS 在门店范围内一致。
3. 跨店、跨企业仍由服务端 DataScope 拒绝。
4. 本裁决只改变读取字段，不扩大 `DEC-093` 的动作权限。
5. 当前不新增银行卡、身份证、税率、应纳税所得额、专项扣除等字段。

## 当前实现符合性

| 能力 | 当前实现 | 结论 |
| --- | --- | --- |
| `payroll/store-payroll/info` | PayrollItem 白名单包含社保、个税和全部现有金额字段 | MATCH |
| `payroll/item/page` | 包含社保、个税、应发、扣款、实发等详情字段 | MATCH |
| `payroll/store-payroll/page` | 只展示应发、扣款合计、实发摘要 | MATCH（摘要页，不是完整详情） |
| 跨店范围 | DataScope + StoreFilterContext | MATCH |
| 店长确认/作废/退回/删除/发放 | `DEC-093` 角色门禁 | MATCH |

## 验证

- 定向：`7/7 PASS`
- 生产代码：无变更
- 回归断言：店长本店薪酬详情字段白名单完整
- 全量：本轮受共享工作区 `target` 并发清理影响，停在 1143/2；两项失败均为非薪酬类生成类/配置发现问题，无薪酬断言失败。此前同分支 Java 22 全量 `1144/0/0/0` 证据见 `../20261030-payroll-self-commission/backend-verification.txt`。
- 真实店长页面：待受控门店账号补录。

## 下一步

使用受控店长账号验证：

1. 本店工资详情可见社保、个税及全部现有金额构成。
2. 跨店详情 404/403。
3. 确认、作废、退回、删除、发放入口不可见且接口 403。
