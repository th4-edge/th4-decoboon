# B2 原始线上问题最小 Web 热修发布证据

- 状态：`RELEASED_PENDING_FINANCE_UI_CONFIRMATION`
- 问题：Web 月度考勤选择员工后应出勤仍显示硬编码 22
- 生产 Web release：`/opt/decoboon-web/releases/20261023-193138`
- 回滚目标：`/opt/decoboon-web/releases/20261022-131900`
- 源码分支：`hotfix/payroll-attendance-scheme-20261023`
- 源码提交：`59072ab`
- 标签：`v2026.10.23.1`
- 后端：未变更，继续使用 `/opt/decoboon/releases/20261022-131500`
- DDL：无

## 最小范围

只修改 2 个 Web 文件：

- `src/components/finance/FinanceAttendanceDrawer.vue`
- `src/domains/finance/api.ts`

未带入考勤编辑/删除、店长权限、工资单、复核、作废、发放或其他未发布改动。

## 构建与门禁

- type-check PASS
- production build PASS
- client-copy / permissions / store-scope / workbenches PASS
- 本地制品与生产 `index.html` SHA256 一致：`4b00e0a96b4d944367bcf9085d4aa2babb117111f5f06115cfbb388cf95a93e7`
- 当前生产入口：`assets/index-C-EkkOlM.js`
- 当前考勤 chunk：`assets/FinanceEmployeeSalariesView-BUZjUrPn.js`
- 考勤 chunk SHA256：`0846ebbb962e8d742e6b5d074ce66ea90dda3ecb96f35e1c1ec65398ebda7a1d`
- 当前考勤 chunk 不含 `scheduledDays:"22"`，包含 `payroll/compensation/info` 与“选择员工后联动”

## 生产冒烟

| 项 | 结果 |
| --- | --- |
| `/` | HTTP 200 |
| `/login` | HTTP 200 |
| `/finance/employee-salaries` | HTTP 200 |
| `/v3/` | HTTP 301 → `/` |
| `payroll/compensation/info` | HTTP 200，未登录 token 校验正常 |
| 当前 index 引用考勤 chunk | `FinanceEmployeeSalariesView-BUZjUrPn.js` |
| 生产 UI 无方案分支 | 选择无方案员工显示“该工资月份没有生效薪酬方案”，未回退 22；未保存 |
| 生产薪酬方案数据 | 麦鸿 5 份有效方案均为 27 天 |

## 待确认

未持有麦鸿财务账号，需财务刷新生产页面核验：

1. 麦鸿员工应出勤显示 27；
2. 切换员工/月份联动刷新；
3. 无方案不回退 22。

## 回滚

```bash
ssh root@120.55.15.159 "ln -sfn /opt/decoboon-web/releases/20261022-131900 /opt/decoboon-web/current && nginx -t && systemctl reload nginx"
```
