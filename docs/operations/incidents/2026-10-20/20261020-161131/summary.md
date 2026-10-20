# 生产后端监控摘要

- 采集时间：2026-10-20T16:11:32+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-20/20261020-161131/raw.txt`
- 可处理异常：55
- 已修复历史异常：0
- 预期业务噪音：45

## 服务状态

```text
enabled
active
MainPID=297929
ExecMainStartTimestamp=Tue 2026-10-20 16:10:37 CST
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
/dev/vda3         40901312 22120920  16890024      57% /
/dev/vda3         40901312 22120920  16890024      57% /
/dev/vda3         40901312 22120920  16890024      57% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3466         871         524        3940        3990
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| expected | observed | 1 | info | `手机号或密码错误` |
| unknown | open | 46 | medium | `找不到对应的处理服务:design/items/reorder` |
| unknown | open | 3 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 6 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 43 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 1 | info | `首次登录请授权手机号` |

## 证据

### INC-20261020-57c3fb7d96 · unknown

- 指纹：`找不到对应的处理服务:design/items/reorder`
- 次数：46
- 首次：2026-10-20 13:03:11.309
- 最近：2026-10-20 16:10:32.901

```text
[2026-10-20 13:03:11.309] [ERROR] [XNIO-1 task-9] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - design/items/reorder处理失败:找不到对应的处理服务:design/items/reorder
```

### INC-20261020-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：3
- 首次：2026-10-20 01:46:12.748
- 最近：2026-10-20 12:00:30.120

```text
[2026-10-20 01:46:12.748] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261020-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：6
- 首次：2026-10-20 09:23:59.668
- 最近：2026-10-20 15:46:33.800

```text
[2026-10-20 09:23:59.668] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:访问受限，授权已过期，请重新登录后再访问。
```

