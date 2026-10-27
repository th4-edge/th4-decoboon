# 生产后端监控摘要

- 采集时间：2026-10-27T16:20:25+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon-ready-edit-hotfix/docs/operations/incidents/2026-10-27/20261027-162024/raw.txt`
- 可处理异常：33
- 已修复历史异常：0
- 预期业务噪音：48

## 服务状态

```text
enabled
active
MainPID=349270
ExecMainStartTimestamp=Tue 2026-10-27 16:19:07 CST
ActiveState=active
SubState=running
```

## 健康检查

```text
health_http=200
{"success":false,"errorCode":{"code":"tokenIsUndefined","message":"访问受限，缺少授权标识，请先登录后再访问。","system":false}}
```

## 资源状态

```text
Filesystem     1024-blocks     Used Available Capacity Mounted on
/dev/vda3         40901312 25204824  13806120      65% /
/dev/vda3         40901312 25204824  13806120      65% /
/dev/vda3         40901312 25204824  13806120      65% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3588         423         330        4072        3868
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| code_bug | open | 18 | critical | `Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null` |
| unknown | open | 8 | medium | `Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials` |
| expected | observed | 1 | info | `手机号或密码错误` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 47 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该阶段无合同施工任务，不能提交验收，请先核对报价与合同施工行` |
| unknown | open | 2 | medium | `邀请已过期` |
| unknown | open | 2 | medium | `量房完成时间不能早于预约时间` |

## 证据

### INC-20261027-b63f1ec203 · code_bug

- 指纹：`Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null`
- 次数：18
- 首次：2026-10-27 15:38:31.126
- 最近：2026-10-27 15:44:25.586

```text
[2026-10-27 15:38:31.126] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/cancel处理失败:Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null
```

### INC-20261027-9c36075e08 · unknown

- 指纹：`Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials`
- 次数：8
- 首次：2026-10-27 00:07:05.612
- 最近：2026-10-27 15:39:40.759

```text
[2026-10-27 00:07:05.612] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/page处理失败:Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials
```

### INC-20261027-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：1
- 首次：2026-10-27 12:10:02.020
- 最近：2026-10-27 12:10:02.020

```text
[2026-10-27 12:10:02.020] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261027-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：1
- 首次：2026-10-27 15:38:12.775
- 最近：2026-10-27 15:38:12.775

```text
[2026-10-27 15:38:12.775] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - carryover/batch/info处理失败:访问受限，授权已过期，请重新登录后再访问。
```

### INC-20261027-4b40ffd35e · unknown

- 指纹：`该阶段无合同施工任务，不能提交验收，请先核对报价与合同施工行`
- 次数：1
- 首次：2026-10-27 09:39:32.257
- 最近：2026-10-27 09:39:32.257

```text
[2026-10-27 09:39:32.257] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/acceptance/save处理失败:该阶段无合同施工任务，不能提交验收，请先核对报价与合同施工行
```

### INC-20261027-c0fb198c0a · unknown

- 指纹：`邀请已过期`
- 次数：2
- 首次：2026-10-27 00:09:51.230
- 最近：2026-10-27 00:09:56.841

```text
[2026-10-27 00:09:51.230] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/cancel处理失败:邀请已过期
```

### INC-20261027-c5a21e677b · unknown

- 指纹：`量房完成时间不能早于预约时间`
- 次数：2
- 首次：2026-10-27 10:16:09.294
- 最近：2026-10-27 10:16:26.502

```text
[2026-10-27 10:16:09.294] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - property/measure/save处理失败:量房完成时间不能早于预约时间
```

