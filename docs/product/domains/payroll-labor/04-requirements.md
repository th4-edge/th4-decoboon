# B1 财务薪酬方案维护闭环 · 需求与验收条件

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-B1-01 | 财务能查看本范围内的薪酬方案并按员工、状态、生效月份、门店查找 | 已登录且为 BOSS/财务/店长 | BOSS 企业；总部财务企业并可收窄门店；门店财务/店长本店 | 只读方案版本、员工归属、状态和锁定状态 | `/finance/compensation-schemes` 列表、筛选、分页、空态/错误态 | `payroll/compensation/page` | 页面字段与响应一致；跨店不出现；刷新后保持筛选 | BR-038、DEC-065、UI-B1-01/02 |
| REQ-B1-02 | 财务能为员工新增薪酬方案，包含固定工资和月应出勤天数 | 目标员工在当前数据范围；当前人可写 | BOSS/财务；店长拒绝 | 新增 Active；员工归属决定 store/team | 文档式抽屉；必填校验；服务端结果回读 | `payroll/compensation/save` | 成功保存 `baseSalary/scheduledDays/effectiveFrom`；缺任一字段 400；删除 Web 端缺字段缺陷 | BR-038、DEC-045、UI-B1-03 |
| REQ-B1-03 | 财务能编辑尚未形成薪酬事实的方案 | 方案存在且无考勤、提成、工资项事实 | BOSS/财务；本店财务限本店 | 员工不可更换；可改工资、应出勤、有效期、备注 | 编辑抽屉；锁定方案不显示编辑按钮 | `payroll/compensation/info/update` | 正常修改并回读；事实存在时 409；跨店 404/403 | BR-038、UI-B1-03/04 |
| REQ-B1-04 | 员工已有薪酬事实时，财务能通过新增后续方案承接调薪 | 存在锁定方案；新方案生效日期晚于原版本 | BOSS/财务 | 旧方案和事实不变；新方案 Active；同周期最新 effectiveFrom 生效 | 行操作「新增后续方案」，预填员工与当前值，强制新生效日期 | `payroll/compensation/save` | 新方案成功；旧方案仍可追溯；工资月份匹配新方案 | BR-038、DEC-045、RULE-B1-08/09 |
| REQ-B1-05 | 财务能终止或删除未形成事实的方案 | 方案无考勤、提成、工资项事实 | BOSS/财务 | 终止写 effectiveTo；删除物理移除未使用方案 | 终止需确认；删除需二次确认并说明不可恢复 | `payroll/compensation/update/delete` | 终止后有效期正确；删除后列表消失；有事实时 409 | BR-038、RULE-B1-11、UI-B1-05 |
| REQ-B1-06 | BOSS/财务/店长只能读取和操作各自数据范围 | 登录员工和门店事实有效 | 写：BOSS/财务；读：BOSS/财务/店长 | 总部财务企业；门店财务/店长本店 | 店长只读；越权提示统一 | `payroll/employee/page`、`payroll/compensation/page/info/save/update/delete` | 门店财务无法选/存/改他店员工；店长写 403；跨店 404/403 | BR-038、DEC-065、CHK-B1-01/02 |
| REQ-B1-07 | 页面、App 和接口使用完整、同名的薪酬方案字段 | 后端响应含字段白名单 | 同查看权限 | `scheduledDays`、`createdBy`、`createTime`、`updateTime`、`locked` 必须返回 | Web/App 模型不得丢字段或补默认值 | `payroll/compensation/page/info` | Web 显示应出勤天数和更新时间；App 回归不丢失 scheduledDays | BR-038、CHK-B1-06 |
| REQ-B1-08 | 金额、出勤天数和日期输入均受服务端校验 | 请求已进入 Provider | 同写权限 | 工资 > 0；应出勤 1～31；失效日期不早于生效日期 | 前端即时错误、服务端错误可恢复 | `payroll/compensation/save/update` | 0、负数、31 天以上、倒置日期均 400 且不落库 | BR-015、BR-038、CHK-B1-03 |
| REQ-B1-09 | 同一员工同一生效日期不会创建重复方案 | 员工有效 | 同写权限 | 业务唯一键 `company + employee + effectiveFrom` | 错误保留输入并提供打开已有方案/改日期路径 | `payroll/compensation/save/update` | 重复 409；并发唯一约束错误转换为 409 | BR-038、CHK-B1-04 |
| REQ-B1-10 | 事实形成前的薪酬方案变更可追溯 | 写请求成功 | 同写权限 | 不新增 DDL；记录 createdBy/createTime/updateTime 和结构化审计快照 | 列表显示创建人与更新时间 | `payroll/compensation/save/update/delete` | 每条写请求有操作人、企业、门店、动作和前后快照；删除保留日志 | BR-038、RULE-B1-12 |
| REQ-B1-11 | App 现有薪酬方案维护能力保持闭环 | App 登录且具备能力键 | 同 Web | 共用同一 Provider 和锁定规则 | `compensation-list`、`compensation-edit` | 同 Provider | App 新增/编辑/删除回归通过；锁定方案提示新增后续方案 | BR-038、CHK-B1-08、UI-B1-06 |

## 非功能要求

- 性能：薪酬方案分页只返回白名单字段；不得按行查询员工、门店、团队或锁定状态；分页默认 20、最大 200。
- 可访问性：Web 表单所有字段有 label，抽屉支持键盘操作和错误聚焦；表格操作按钮有明确可读名称。
- 异常恢复：保存失败保留输入；权限、重复、锁定和并发冲突使用服务端原因；错误后重新读取事实。
- 审计/幂等：创建/更新/删除由服务端校验唯一键和事实锁定；重复提交不产生重复方案；结构化审计记录前后快照。
- 跨端一致性：Web 与 App 共用 `payroll/compensation/*` 和 `payroll/employee/page`；能力键、角色和数据范围必须一致。
- 响应式：Web 在 1024、1280、1440 宽度无页面级横向溢出；窄屏操作收进行菜单。
- 安全：任何请求都不得以客户端 companyId/storeId/empId 决定归属；成本、薪酬和员工信息按角色裁剪。

## 明确不做

- 不新增薪酬方案状态或金额公式。
- 不做月度考勤编辑。
- 不做工资单生成、确认、作废和发放。
- 不做已形成事实方案的历史回写。
- 不做 DDL 或生产数据迁移。
- 不改店长工资单确认/作废边界。
