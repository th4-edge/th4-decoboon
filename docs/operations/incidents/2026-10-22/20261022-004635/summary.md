# 生产后端监控摘要

- 采集时间：2026-10-22T00:46:36+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-22/20261022-004635/raw.txt`
- 可处理异常：5
- 已修复历史异常：0
- 预期业务噪音：28

## 服务状态

```text
enabled
active
MainPID=310624
ExecMainStartTimestamp=Thu 2026-10-22 00:44:32 CST
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
/dev/vda3         40901312 23407712  15603232      61% /
/dev/vda3         40901312 23407712  15603232      61% /
/dev/vda3         40901312 23407712  15603232      61% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3662         348         400        4144        3794
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `null` |
| unknown | open | 3 | medium | `找不到对应的处理服务:project/owner-preview/info` |
| unknown | open | 1 | medium | `找不到对应的处理服务:session/properties` |
| expected | observed | 28 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261022-2be88ca424 · unknown

- 指纹：`null`
- 次数：1
- 首次：2026-10-22 00:16:30.946
- 最近：2026-10-22 00:16:30.946

```text
[2026-10-22 00:16:30.946] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:null
```

### INC-20261022-c27c97486f · unknown

- 指纹：`找不到对应的处理服务:project/owner-preview/info`
- 次数：3
- 首次：2026-10-22 00:07:29.791
- 最近：2026-10-22 00:17:11.433

```text
[2026-10-22 00:07:29.791] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/owner-preview/info处理失败:找不到对应的处理服务:project/owner-preview/info
```

### INC-20261022-301f44d7f3 · unknown

- 指纹：`找不到对应的处理服务:session/properties`
- 次数：1
- 首次：2026-10-22 00:07:43.117
- 最近：2026-10-22 00:07:43.117

```text
[2026-10-22 00:07:43.117] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - session/properties处理失败:找不到对应的处理服务:session/properties
```

