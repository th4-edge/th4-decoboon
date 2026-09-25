# BUG 缺陷台账

| BUG ID | 严重度 | 角色/场景 | 标题 | 最小复现 | 期望 | 实际 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|
| BUG-20260924-001 | P0 | Marketer / 设计 | 业务员可新增设计 | 黄贤杰 token 调 `design/scheme/save` | 403/业务拒绝 | 修复后返回 403，未落库 | `p0-fix-verification.txt` | FIXED_VERIFIED |
| BUG-20260924-002 | P1 | PM / 施工任务 | 默认任务负责人错误 | 打开王昌富项目 44 默认任务 | 负责人为 PM/施工人 | 修复后 24/24 均为对应 PM | `p1-task-owner-fix.txt`、任务截图 | FIXED_VERIFIED |
| BUG-20260924-003 | P2 | 初始化 | 报价导入脚本分页幂等失败 | 空库重复执行导入 | 仍为 550 条唯一项 | 672 条，122 条重复 | `raw/sql/01-fixture-repairs.sql` | OPEN |
| BUG-20260924-004 | P2 | 初始化 | 套餐模板保存异常 | 导入套餐行 `pack/line/save` | 保存模板和分项 | `orphan deletion` 异常，留下空套餐 | 错误日志、导入结果 | OPEN |
| BUG-20260924-005 | P2 | Finance / 项目账本 | 总成本与非零分项不一致 | 打开项目 44 项目账本 | 成本拆口和总成本关系可理解 | 总成本 3 万，四项均显示 0 | API、截图 | OPEN |
| BUG-20260924-006 | P3 | Designer / 增减项 | 负数金额符号位置不统一 | 查看增减项负金额 | `-¥105` | 修复后为 `-¥105` | `p3-money-format-verification.txt` | FIXED_VERIFIED |

## BUG-20260924-001 详情

- 业务前置：业务员已经有客户和房屋。
- 复现：`POST /api/design/scheme/save`，仅带合法 `customerId/propertyId/name`。
- 影响：越权新增设计，污染版本与责任链。
- 已验证：测试记录确实创建，随后已清理；同角色不具备前端 `edit:design` 权限。

## BUG-20260924-002 详情

- 业务前置：项目由王昌富负责，合同生效后自动生成施工任务。
- 复现：打开 `/subpackages/project/pages/task/detail?id=<taskId>`。
- 实际：页面显示“负责人 黄贤杰（业务员）”。
- 代码根因：默认任务通过 `ProjectPermissionContext.apply` 复制项目经营 owner，而非 `project.manager`。
- 影响：施工任务责任与绩效归因错误。
