# tenant 租户与用户域

装得清企业租户、组织、员工与登录凭证域。

## 当前包结构

```text
tenant/user
├── configure    # CredType / EmployeeRole / EmployeeStatus
├── model        # Company / Store / Team / Employee / Credentials / Setting
└── service      # auth/session、invite/invitation、company/member、user、security
```

## 详细设计

见：[docs/租户设计方案.md](../../../../../../../../../../docs/租户设计方案.md)

## 核心约定

- 企业：`Company`，主键 `comId`，表 `db_tenant_company`
- 门店：`Store`，主键 `storeId`，表 `db_tenant_store`
- 团队：`Team`，主键 `teamId`，表 `db_tenant_team`
- 员工：`Employee`，主键 `empId`，承载企业归属、状态与头像
- 用户身份：`Credentials(type=Wx)`，`idKey=openId`、`idSecret=unionId`
