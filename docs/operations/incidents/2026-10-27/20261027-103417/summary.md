# 生产后端监控摘要

- 采集时间：2026-10-27T10:34:18+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-27/20261027-103417/raw.txt`
- 可处理异常：9
- 已修复历史异常：0
- 预期业务噪音：35

## 服务状态

```text
enabled
active
MainPID=346913
ExecMainStartTimestamp=Tue 2026-10-27 10:33:07 CST
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
/dev/vda3         40901312 24953568  14057376      64% /
/dev/vda3         40901312 24953568  14057376      64% /
/dev/vda3         40901312 24953568  14057376      64% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3702         274         330        4107        3753
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 4 | medium | `Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 34 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该阶段无合同施工任务，不能提交验收，请先核对报价与合同施工行` |
| unknown | open | 2 | medium | `邀请已过期` |
| unknown | open | 2 | medium | `量房完成时间不能早于预约时间` |

## 证据

### INC-20261027-9c36075e08 · unknown

- 指纹：`Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials`
- 次数：4
- 首次：2026-10-27 00:07:05.612
- 最近：2026-10-27 00:13:34.008

```text
[2026-10-27 00:07:05.612] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/page处理失败:Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials
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

