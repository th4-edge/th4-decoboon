# 生产后端监控摘要

- 采集时间：2026-10-07T00:32:25+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-07/20261007-003223/raw.txt`
- 可处理异常：1
- 已修复历史异常：0
- 预期业务噪音：6

## 服务状态

```text
enabled
active
MainPID=169750
ExecMainStartTimestamp=Wed 2026-10-07 00:30:25 CST
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
/dev/vda3         40901312 12323300  26687644      32% /
/dev/vda3         40901312 12323300  26687644      32% /
/dev/vda3         40901312 12323300  26687644      32% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3310         883         514        4074        4146
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| expected | observed | 6 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261007-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：1
- 首次：2026-10-07 00:12:43.054
- 最近：2026-10-07 00:12:43.054

```text
[2026-10-07 00:12:43.054] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

