# 员工薪酬与薪资经营 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

## B1 对账矩阵

| REQ | 业务规则/蓝图 | 后端/API | Web | App | 自动化测试 | UI/证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-B1-01 方案列表与筛选 | BR-038、DEC-065；enter/filter、UI-B1-01/02 | `payroll/compensation/page`；DataScope + EntityGraph + filters | `/finance/compensation-schemes` 列表、筛选、分页 | `compensation-list` 同 Provider | 全量 900/0/0；Web build/check:permissions/check:store-scope/check:workbenches PASS | 01–03 三档截图；筛选王宏文=1、无结果=0 | PASS |
| REQ-B1-02 新增方案 | BR-038、DEC-045；create、UI-B1-03 | `payroll/compensation/save`；`scheduledDays` 必填、金额/日期/唯一校验 | 文档式新增抽屉，字段完整 | `compensation-edit` 已有同能力 | 定向 11/0/0；全量 900/0/0 | 04 抽屉、05 校验截图；真实创建徐勇 `¥8,888/22天` | PASS |
| REQ-B1-03 编辑未锁方案 | BR-038；edit、UI-B1-03/04 | `payroll/compensation/info/update`；DataScope + 事实锁 + 字段校验 | 编辑抽屉；员工只读；锁定提示按服务端 `locked` 显示 | `compensation-edit` | 定向事实锁/审计测试 PASS；全量 PASS | 真实编辑徐勇 `¥8,888→¥9,999`；UI-B1-002 已修复 | PASS |
| REQ-B1-04 新增后续方案 | BR-038、DEC-045；lock/successor、UI-B1-04 | `save` 允许新增更晚版本；事实锁不回写旧方案 | Web 行操作“新增后续方案”，预填原值 | App `successorId` 路径同 Provider | 锁定状态 3 聚合测试 PASS；App type-check/contract/build PASS | 06 后续方案截图；真实锁定行入口可见 | PASS |
| REQ-B1-05 终止/删除 | BR-038；terminate-delete、UI-B1-05 | `update` 写 effectiveTo；`delete` 事务内事实锁 + 审计 | 终止 prompt、删除二次确认、锁定隐藏 | App locked 隐藏删除 | 删除锁/审计定向测试 PASS | 真实终止 `2026-10-21→2026-10-21`；真实删除后列表 6→5 | PASS |
| REQ-B1-06 数据范围与权限 | BR-038、DEC-065；CHK-B1-01/02/07 | `payroll/employee/page` 与 compensation 全部 Provider 同 DataScope | 财务可写、BOSS 全企业可写、店长本店只读 | App 能力键 `view/edit:payroll` | `PayrollCompensationScopeTest`：同店通过、跨店拒绝、总部跨店通过；全部 PASS | 08 店长只读、09 BOSS 可写；Finance 写入口 | PASS |
| REQ-B1-07 字段完整与跨端一致 | BR-038；CHK-B1-06/08 | page/info 返回 scheduledDays、createdBy、createTime、updateTime、locked | 财务员工映射修复岗位/门店/团队；角色中文化 | App 模型补 locked/lockedReason/createdBy/updatedAt | 白名单断言 PASS；Web/App type-check/build PASS | 列表最终截图显示中文岗位、门店、团队 | PASS |
| REQ-B1-08 输入校验 | BR-015、BR-038；CHK-B1-03 | salary > 0；scheduledDays 1～31；effectiveTo ≥ effectiveFrom | 前端即时错误并保留输入 | 原表单同步约束 | `PayrollCompensationContextTest` + Guard 测试 PASS | 05 客户端校验截图；服务端错误由 Provider 再次兜底 | PASS |
| REQ-B1-09 唯一版本 | BR-038；CHK-B1-04 | 同员工同 effectiveFrom 409；UI 与 DB 唯一约束共同兜底 | 错误保留输入并提示改日期/编辑原方案 | 同 Provider 错误提示 | 重复生效日期测试 PASS | 04/05 抽屉恢复路径；真实创建唯一日期成功 | PASS |
| REQ-B1-10 审计与恢复 | BR-038；RULE-B1-12、recover | 结构化 CREATE/UPDATE/DELETE 审计；createdBy/createTime/updateTime | 列表显示创建人与更新时间；失败保留输入 | 同 Provider | 审计快照测试 PASS；删除前快照记录 | 编辑后列表更新时间变化；真实 CRUD 成功 | PASS |
| REQ-B1-11 App 回归 | BR-038；UI-B1-06 | 共用 `payroll/compensation/*` | Web 同能力 | 列表锁定状态、新增/编辑/删除、后续方案 | `type-check`、`verify-payroll-closure-contract`、`build:mp-weixin` PASS | Web 与 App 字段/状态一致；App 真机与微信运行时独立验收待做 | PASS |

## 后续批次（未开始）

| REQ | 内容 | 状态 |
| --- | --- | --- |
| REQ-002 | 财务修正月度考勤 | PENDING |
| REQ-003 | 工资单复核、确认与发放 | PENDING |
| REQ-004 | 工资与提成解耦落地 | PENDING |

## 未映射

- 无。REQ-B1-01～11 均有业务规则、蓝图节点、Provider/API、页面、测试和 UI 证据。

## 已过期

- 无。

## 未决业务问题

- `GAP-060` 店长工资单确认/作废边界：B1 不涉及，非本批阻塞。
- 店长员工发放权限最终边界：B3 前关闭。
- 双门店真实 UI 负向：自动化已覆盖，独立验收阶段补真实双门店账号/对象。
- App 微信运行时/真机：独立验收阶段补。

## 本批结论

- 设计—蓝图—代码—Web—App—测试—UI：`PASS`
- 缺陷：UI-B1-001、UI-B1-002 均已修复并复验。
- 无 DDL/迁移；无未映射或过期项。
- 独立验收：`PASS`；ACC-B1-001/002/003 已关闭，ACC-B1-U01 已关闭。
- 当前状态：`ACCEPTED`
- 下一阶段：`RELEASED` 发布门禁；必须由用户确认 L3 生产发布，未确认前不得部署。
