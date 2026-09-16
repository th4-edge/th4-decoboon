# 生产后端监控摘要

- 采集时间：2026-09-17T01:44:34+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-17/20260917-014433/raw.txt`
- 可处理异常：1
- 已修复历史异常：0
- 预期业务噪音：10

## 服务状态

```text
enabled
active
MainPID=36047
ExecMainStartTimestamp=Thu 2026-09-17 01:40:59 CST
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
/dev/vda3         40901312 5880848  33130096      16% /
/dev/vda3         40901312 5880848  33130096      16% /
/dev/vda3         40901312 5880848  33130096      16% /
               total        used        free      shared  buff/cache   available
Mem:            7456        2197        2429         124        3228        5258
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 10 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20260917-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：1
- 首次：2026-09-17 01:39:15.650
- 最近：2026-09-17 01:39:15.650

```text
[2026-09-17 01:39:15.650] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/project/page处理失败:访问受限，授权已过期，请重新登录后再访问。
```

