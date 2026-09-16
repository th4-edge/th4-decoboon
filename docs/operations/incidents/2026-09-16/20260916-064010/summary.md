# 生产后端监控摘要

- 采集时间：2026-09-16T06:40:11+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-16/20260916-064010/raw.txt`
- 可处理异常：44
- 已修复历史异常：0
- 预期业务噪音：30

## 服务状态

```text
enabled
active
MainPID=31996
ExecMainStartTimestamp=Wed 2026-09-16 05:32:22 CST
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
/dev/vda3         40901312 5460936  33550008      14% /
/dev/vda3         40901312 5460936  33550008      14% /
/dev/vda3         40901312 5460936  33550008      14% /
               total        used        free      shared  buff/cache   available
Mem:            7456        1931        3109         124        2806        5524
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| code_bug | reopened | 42 | critical | `LazyInitializationException: Could not initialize proxy - no session` |
| expected | observed | 1 | info | `当前用户已有在职企业` |
| expected | observed | 1 | info | `微信支付回调原始报文不存在` |
| expected | observed | 1 | info | `微信支付回调签名缺失` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:config` |
| expected | observed | 24 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 1 | info | `首次登录请授权手机号` |

## 证据

### INC-20260916-a1233597b0 · code_bug

- 指纹：`LazyInitializationException: Could not initialize proxy - no session`
- 次数：42
- 首次：2026-09-16 05:14:57.806
- 最近：2026-09-16T06:04:02.211+08:00

```text
[2026-09-16 05:14:57.806] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:Could not initialize proxy [com.th4.edge.tenant.user.model.Employee#1789506897394275742] - no session
```

### INC-20260916-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：2
- 首次：2026-09-16 06:33:46.274
- 最近：2026-09-16T06:33:46.274+08:00

```text
[2026-09-16 06:33:46.274] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - config处理失败:找不到对应的处理服务:config
```

