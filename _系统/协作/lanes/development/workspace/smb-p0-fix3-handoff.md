# 交接：REQ-20260918-SMB-P0-FIX-3（量房幂等与执行人角色）

- request_id: `REQ-20260918-SMB-P0-FIX-3`
- lane: development
- 输入：`docs/acceptance/中小装企P0实机复验报告.md`、`_系统/协作/lanes/acceptance/workspace/smb-p0/recheck-a4-i.json`、`docs/acceptance/evidence/smb-p0/recheck2/**`
- 范围：仅修 DEF-SMB-P0-009 / DEF-SMB-P0-010；未动合同、验收、材料等 blocked 规则
- 探针证据：`_系统/协作/lanes/development/workspace/smb-p0-fix3-probe.txt`

## 1. changed_files

- `src/main/java/com/th4/edge/decoboon/property/service/measures/save/support/Save.java`
- `src/main/java/com/th4/edge/decoboon/property/context/PropertyContext.java`
- `_系统/协作/lanes/development/worklog.md`、本交接、`workspace/smb-p0-fix3-probe.txt`、`_系统/协作/shared.md`


## 2. fix_results

### DEF-SMB-P0-009 重复 Completed 覆盖历史时间
- 修复：`property/measure/save` 先解析"待完成预约（Scheduled）"与"已完成事实（Completed）"，在通过执行人权限校验后，若为 Completed 请求且**没有 Scheduled 记录但已有 Completed 记录**，直接 `return completed`，不进入任何写路径。
- 因此 `measureTime`、`property.latestMeasureAt`、`executor`、`designer`、`status`、附件字段全部保持原值；只有"预约 → 完成"的正常路径才写完成事实。
- 顺序保证：执行人权限校验在幂等返回之前，业务员重复提交 Completed 仍然 403（A4 不回归）。

### DEF-SMB-P0-010 executor.role 响应错误
- 根因（已定位到框架实现）：`JwtSecurityManager.authenticate` 解析 JWT 后执行 `BeanUtils.instantiateClass(Employee.class)`，只通过 `setUserId` 写入 `empId`；`Employee.role` 字段默认值为 `EmployeeRole.Marketer`、`name` 为 null。原实现 `target.setExecutor(employee)` 把这个占位对象写入实体关系，响应即出现 `executor.role=Marketer.name 缺失`。
- 修复：`PropertyContext` 新增 `requireEmployee(principal)`（委托 `DataScopeContext/TenantContext` 重新加载完整员工并校验在职），`Save` 使用该完整实体作为当前操作人：既用于执行人权限比对，也写入 `Measure.executor`。
- 同一响应其他嵌套：`designer` 来自房屋/客户实体（DB）本就正确；`owner` 未在 `property/measure/save` 的 `@Properties` 白名单内（不返回），无串值风险；响应仍只输出 `empId/name/role`，不暴露员工敏感字段。

## 3. idempotency_evidence

`property/measure/save`（property `226701441081802752`，measure `226701602042413056`，Designer 会话）：

- 重复调用（仅 `propertyId + status=Completed`，不带 measureTime）：
  - 返回 `measureId=226701602042413056`、`measureTime=2026-09-19 00:00:00`、`status=Completed`（原事实）。
  - DB 前后完全一致：`measure_time=2026-09-19 00:00:00`、`latest_measure_at=2026-09-19 00:00:00`、`designer_id=205`、`executor_id=205`、`designer_conclusion=NULL`、该房屋量房记录数=1。
- 并发 3 连发重复 completion：3 个响应均 `success=true` 且为同一 measureId/measureTime，DB 不变、记录数仍为 1。
- A4 回归：业务员（Marketer 204）重复 completion → `403 该房屋已指定设计师，量房完成必须由指定设计师执行`，未落库。

## 4. serialization_evidence

重复 completion 响应原文（节选）：

```json
"designer": {"empId":"1890000000000000205","name":"李设计","role":{"name":"设计师","value":"Designer","key":"Designer"}},
"executor": {"empId":"1890000000000000205","name":"李设计","role":{"name":"设计师","value":"Designer","key":"Designer"}},
"status":   {"name":"已完成","key":"Completed"}
```

对照 DB：`db_tenant_employee` 中 emp 205 = 李设计 / Designer。修复前同一响应为 `"executor":{"empId":"1890000000000000205","role":{"name":"业务员","value":"Marketer"}}`（name 缺失、角色错误），修复后与 DB 一致。

## 5. verification

- 后端：`mvn -o -q test` → **43 suites / 133 tests / 0 failures / 0 errors / 0 skipped**。
- 运行期探针（真实 HTTP + MySQL 回查）见 `smb-p0-fix3-probe.txt`；未修改夹具数据。

## 6. restart_evidence

- 后端：`./run-dev.sh`（loader `target/lib,target/classes`，profile dev）→ 监听 `*:8090`；探针全部返回真实业务响应。
- MySQL 仅沙箱外可达，故重启与探针在沙箱外执行。

## 7. remaining_defects / risks

- 框架层 `@User` 主体是"只带 empId 的占位 Employee"：任何 Provider 若把该主体直接写入响应可见关系，都会出现 `name` 缺失、`role=Marketer` 的错误投影。本轮仅修复量房执行人；建议后续统一约定"业务归属与响应关联必须使用 TenantContext/DataScopeContext 重新加载的完整员工实体"，并对其它 Provider 做一次同类扫描（本轮按要求未扩大范围）。
- DEF-SMB-P0-009 的幂等依赖"该房屋无 Scheduled 记录"判定：若同一房屋存在新预约，则按"完成新预约"处理（符合业务预期）。

## 8. acceptance_needed

- 复跑 A4/I1/I2/I4/B2/C4/F/G 回归（本轮仅动量房 save/provider 与 PropertyContext）。
- 针对 DEF-009 复验：重复/并发 completion 后 `measureTime`、`latestMeasureAt`、`executor`、`designer`、`status`、附件与记录数全部不变。
- 针对 DEF-010 复验：`property/measure/save`、`property/measure/page`、`property/measure/info` 的 `designer/executor.role` 与 DB 员工角色一致，且不暴露员工敏感字段。
