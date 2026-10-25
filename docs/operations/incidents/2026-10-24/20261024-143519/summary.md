# 生产后端监控摘要

- 采集时间：2026-10-24T14:35:20+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-24/20261024-143519/raw.txt`
- 可处理异常：8
- 已修复历史异常：0
- 预期业务噪音：58

## 服务状态

```text
enabled
active
MainPID=323633
ExecMainStartTimestamp=Sat 2026-10-24 02:55:24 CST
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
/dev/vda3         40901312 23992188  15018756      62% /
/dev/vda3         40901312 23992188  15018756      62% /
/dev/vda3         40901312 23992188  15018756      62% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3004         850          19        3917        4452
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| expected | observed | 4 | info | `手机号或密码错误` |
| expected | observed | 4 | info | `找不到对应的处理服务:.env` |
| unknown | open | 4 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/auto_login` |
| expected | observed | 50 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261024-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：4
- 首次：2026-10-24 10:54:25.847
- 最近：2026-10-24T13:10:41.707+08:00

```text
[2026-10-24 10:54:25.847] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20261024-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：2
- 首次：2026-10-24 08:39:26.817
- 最近：2026-10-24T08:39:26.817+08:00

```text
[2026-10-24 08:39:26.817] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261024-3c06097867 · unknown

- 指纹：`找不到对应的处理服务:v1/auto_login`
- 次数：2
- 首次：2026-10-24 07:45:42.350
- 最近：2026-10-24T07:45:42.350+08:00

```text
[2026-10-24 07:45:42.350] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/auto_login处理失败:找不到对应的处理服务:v1/auto_login
```

