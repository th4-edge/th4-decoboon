# 生产后端监控摘要

- 采集时间：2026-09-16T13:02:32+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-16/20260916-130232/raw.txt`
- 可处理异常：6
- 已修复历史异常：22
- 预期业务噪音：60

## 服务状态

```text
enabled
active
MainPID=33085
ExecMainStartTimestamp=Wed 2026-09-16 06:59:33 CST
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
Filesystem     1024-blocks    Used Available Capacity Mounted on
/dev/vda3         40901312 5735320  33275624      15% /
/dev/vda3         40901312 5735320  33275624      15% /
/dev/vda3         40901312 5735320  33275624      15% /
               total        used        free      shared  buff/cache   available
Mem:            7456        1994        2781         124        3076        5461
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| code_bug | fixed | 22 | critical | `LazyInitializationException: Could not initialize proxy - no session` |
| unknown | open | 1 | medium | `<id>:Caused by: org.hibernate.PropertyValueException: not-null property references a null or transient value: com.th4.edge.tenant.user.model.Invite.telNo` |
| unknown | open | 2 | medium | `not-null property references a null or transient value: com.th4.edge.tenant.user.model.Invite.telNo` |
| expected | observed | 1 | info | `当前用户已有在职企业` |
| expected | observed | 1 | info | `微信支付回调原始报文不存在` |
| expected | observed | 1 | info | `微信支付回调签名缺失` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 7 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config` |
| unknown | open | 2 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 48 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 1 | info | `首次登录请授权手机号` |

## 证据

### INC-20260916-a8b87027fc · unknown

- 指纹：`<id>:Caused by: org.hibernate.PropertyValueException: not-null property references a null or transient value: com.th4.edge.tenant.user.model.Invite.telNo`
- 次数：1
- 首次：未知
- 最近：未知

```text
2023:Caused by: org.hibernate.PropertyValueException: not-null property references a null or transient value: com.th4.edge.tenant.user.model.Invite.telNo
```

### INC-20260916-cd863ee0f9 · unknown

- 指纹：`not-null property references a null or transient value: com.th4.edge.tenant.user.model.Invite.telNo`
- 次数：2
- 首次：2026-09-16 12:30:13.247
- 最近：2026-09-16T12:30:13.247+08:00

```text
[2026-09-16 12:30:13.247] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/save处理失败:not-null property references a null or transient value: com.th4.edge.tenant.user.model.Invite.telNo
```

### INC-20260916-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：1
- 首次：2026-09-16 06:33:46.274
- 最近：2026-09-16 06:33:46.274

```text
[2026-09-16 06:33:46.274] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - config处理失败:找不到对应的处理服务:config
```

### INC-20260916-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：2
- 首次：2026-09-16 12:29:27.868
- 最近：2026-09-16T12:29:27.868+08:00

```text
[2026-09-16 12:29:27.868] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - statistic/boss/dashboard处理失败:访问受限，授权已过期，请重新登录后再访问。
```

