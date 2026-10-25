# 生产后端监控摘要

- 采集时间：2026-10-24T15:16:34+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-24/20261024-151633/raw.txt`
- 可处理异常：6
- 已修复历史异常：0
- 预期业务噪音：78

## 服务状态

```text
enabled
active
MainPID=328325
ExecMainStartTimestamp=Sat 2026-10-24 15:10:27 CST
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
/dev/vda3         40901312 24270468  14740476      63% /
/dev/vda3         40901312 24270468  14740476      63% /
/dev/vda3         40901312 24270468  14740476      63% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3531         211         333        4343        3925
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| expected | observed | 3 | info | `微信支付回调签名缺失` |
| unknown | open | 1 | medium | `微信登录失败：code been used, rid: 6adc531c-577cbac8-51ba533c` |
| expected | observed | 2 | info | `手机号或密码错误` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:actuator/env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/auto_login` |
| expected | observed | 71 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261024-f0e31cb0fc · unknown

- 指纹：`微信登录失败：code been used, rid: 6adc531c-577cbac8-51ba533c`
- 次数：1
- 首次：2026-10-24 14:41:32.413
- 最近：2026-10-24 14:41:32.413

```text
[2026-10-24 14:41:32.413] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/login处理失败:微信登录失败：code been used, rid: 6adc531c-577cbac8-51ba533c
```

### INC-20261024-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：2
- 首次：2026-10-24 10:54:25.847
- 最近：2026-10-24 13:10:41.707

```text
[2026-10-24 10:54:25.847] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20261024-92500a7f2c · unknown

- 指纹：`找不到对应的处理服务:actuator/env`
- 次数：1
- 首次：2026-10-24 14:42:52.286
- 最近：2026-10-24 14:42:52.286

```text
[2026-10-24 14:42:52.286] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - actuator/env处理失败:找不到对应的处理服务:actuator/env
```

### INC-20261024-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：1
- 首次：2026-10-24 08:39:26.817
- 最近：2026-10-24 08:39:26.817

```text
[2026-10-24 08:39:26.817] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261024-3c06097867 · unknown

- 指纹：`找不到对应的处理服务:v1/auto_login`
- 次数：1
- 首次：2026-10-24 07:45:42.350
- 最近：2026-10-24 07:45:42.350

```text
[2026-10-24 07:45:42.350] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/auto_login处理失败:找不到对应的处理服务:v1/auto_login
```

