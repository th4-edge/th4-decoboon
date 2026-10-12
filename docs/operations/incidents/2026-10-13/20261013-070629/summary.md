# 生产后端监控摘要

- 采集时间：2026-10-13T07:06:30+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-13/20261013-070629/raw.txt`
- 可处理异常：9
- 已修复历史异常：0
- 预期业务噪音：25

## 服务状态

```text
enabled
active
MainPID=246609
ExecMainStartTimestamp=Tue 2026-10-13 07:00:30 CST
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
/dev/vda3         40901312 18825216  20185728      49% /
/dev/vda3         40901312 18825216  20185728      49% /
/dev/vda3         40901312 18825216  20185728      49% /
               total        used        free      shared  buff/cache   available
Mem:            7456        4014        1267        1145        3616        3441
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `当前账号无权执行该操作` |
| expected | observed | 4 | info | `手机号或密码错误` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:chat` |
| unknown | open | 1 | medium | `找不到对应的处理服务:cluster_status` |
| unknown | open | 1 | medium | `找不到对应的处理服务:deployments` |
| unknown | open | 1 | medium | `找不到对应的处理服务:generate` |
| unknown | open | 1 | medium | `找不到对应的处理服务:models` |
| unknown | open | 1 | medium | `找不到对应的处理服务:tags` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/nodes` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/pods` |
| expected | observed | 20 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261013-979f5a015e · unknown

- 指纹：`当前账号无权执行该操作`
- 次数：1
- 首次：2026-10-13 02:45:40.027
- 最近：2026-10-13 02:45:40.027

```text
[2026-10-13 02:45:40.027] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - statistic/team-leader/report处理失败:当前账号无权执行该操作
```

### INC-20261013-d34acc1ab2 · unknown

- 指纹：`找不到对应的处理服务:chat`
- 次数：1
- 首次：2026-10-13 04:06:21.473
- 最近：2026-10-13 04:06:21.473

```text
[2026-10-13 04:06:21.473] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - chat处理失败:找不到对应的处理服务:chat
```

### INC-20261013-5cc442b712 · unknown

- 指纹：`找不到对应的处理服务:cluster_status`
- 次数：1
- 首次：2026-10-13 04:06:17.624
- 最近：2026-10-13 04:06:17.624

```text
[2026-10-13 04:06:17.624] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - cluster_status处理失败:找不到对应的处理服务:cluster_status
```

### INC-20261013-796c9739f8 · unknown

- 指纹：`找不到对应的处理服务:deployments`
- 次数：1
- 首次：2026-10-13 04:06:21.452
- 最近：2026-10-13 04:06:21.452

```text
[2026-10-13 04:06:21.452] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - deployments处理失败:找不到对应的处理服务:deployments
```

### INC-20261013-7b8c639979 · unknown

- 指纹：`找不到对应的处理服务:generate`
- 次数：1
- 首次：2026-10-13 04:06:18.317
- 最近：2026-10-13 04:06:18.317

```text
[2026-10-13 04:06:18.317] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - generate处理失败:找不到对应的处理服务:generate
```

### INC-20261013-88cb58bc86 · unknown

- 指纹：`找不到对应的处理服务:models`
- 次数：1
- 首次：2026-10-13 04:06:21.332
- 最近：2026-10-13 04:06:21.332

```text
[2026-10-13 04:06:21.332] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - models处理失败:找不到对应的处理服务:models
```

### INC-20261013-a1c8e8cad5 · unknown

- 指纹：`找不到对应的处理服务:tags`
- 次数：1
- 首次：2026-10-13 04:06:17.655
- 最近：2026-10-13 04:06:17.655

```text
[2026-10-13 04:06:17.655] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - tags处理失败:找不到对应的处理服务:tags
```

### INC-20261013-0a17dd40a9 · unknown

- 指纹：`找不到对应的处理服务:v1/nodes`
- 次数：1
- 首次：2026-10-13 04:06:17.468
- 最近：2026-10-13 04:06:17.468

```text
[2026-10-13 04:06:17.468] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/nodes处理失败:找不到对应的处理服务:v1/nodes
```

### INC-20261013-e65acbe89a · unknown

- 指纹：`找不到对应的处理服务:v1/pods`
- 次数：1
- 首次：2026-10-13 04:06:17.497
- 最近：2026-10-13 04:06:17.497

```text
[2026-10-13 04:06:17.497] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/pods处理失败:找不到对应的处理服务:v1/pods
```

