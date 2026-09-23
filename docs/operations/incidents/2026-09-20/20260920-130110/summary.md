# 生产后端监控摘要

- 采集时间：2026-09-20T13:01:11+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-20/20260920-130110/raw.txt`
- 可处理异常：62
- 已修复历史异常：0
- 预期业务噪音：28

## 服务状态

```text
enabled
active
MainPID=47233
ExecMainStartTimestamp=Fri 2026-09-18 13:10:08 CST
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
/dev/vda3         40901312 6358712  32652232      17% /
/dev/vda3         40901312 6358712  32652232      17% /
/dev/vda3         40901312 6358712  32652232      17% /
               total        used        free      shared  buff/cache   available
Mem:            7456        2461        1679         243        3840        4994
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| expected | observed | 6 | info | `找不到对应的处理服务:.env` |
| unknown | open | 11 | medium | `找不到对应的处理服务:auth/signin` |
| unknown | open | 2 | medium | `找不到对应的处理服务:chunkplayer/index.html` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config` |
| unknown | open | 4 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:getServices` |
| unknown | open | 2 | medium | `找不到对应的处理服务:graphql` |
| unknown | open | 2 | medium | `找不到对应的处理服务:host.describe` |
| unknown | open | 1 | medium | `找不到对应的处理服务:jmreport/excelQueryByTemplate` |
| unknown | open | 2 | medium | `找不到对应的处理服务:kconfig` |
| unknown | open | 6 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 4 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:system/base/info` |
| unknown | open | 2 | medium | `找不到对应的处理服务:system/setting` |
| unknown | open | 2 | medium | `找不到对应的处理服务:user/current` |
| unknown | open | 2 | medium | `找不到对应的处理服务:user/status` |
| unknown | open | 4 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/GetLicEndTime` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/version` |
| unknown | open | 4 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 4 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:x` |
| expected | observed | 22 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20260920-0cd74dd730 · unknown

- 指纹：`找不到对应的处理服务:auth/signin`
- 次数：11
- 首次：2026-09-18T14:05:25.255+08:00
- 最近：2026-09-18T14:05:32.582+08:00

```text
845:2026-09-18T14:05:25.255+08:00 ERROR 47233 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : auth/signin处理失败:找不到对应的处理服务:auth/signin
```

### INC-20260920-5589e060f2 · unknown

- 指纹：`找不到对应的处理服务:chunkplayer/index.html`
- 次数：2
- 首次：2026-09-20 04:20:00.649
- 最近：2026-09-20T04:20:00.649+08:00

```text
[2026-09-20 04:20:00.649] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - chunkplayer/index.html处理失败:找不到对应的处理服务:chunkplayer/index.html
```

### INC-20260920-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：1
- 首次：2026-09-19T11:50:39.008+08:00
- 最近：2026-09-19T11:50:39.008+08:00

```text
5624:2026-09-19T11:50:39.008+08:00 ERROR 47233 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : config处理失败:找不到对应的处理服务:config
```

### INC-20260920-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：4
- 首次：2026-09-18T14:16:58.578+08:00
- 最近：2026-09-19T11:09:42.617+08:00

```text
2135:2026-09-18T14:16:58.578+08:00 ERROR 47233 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20260920-6f7f4c367b · unknown

- 指纹：`找不到对应的处理服务:getServices`
- 次数：2
- 首次：2026-09-20 04:20:14.851
- 最近：2026-09-20T04:20:14.851+08:00

```text
[2026-09-20 04:20:14.851] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - getServices处理失败:找不到对应的处理服务:getServices
```

### INC-20260920-8478b5fd37 · unknown

- 指纹：`找不到对应的处理服务:graphql`
- 次数：2
- 首次：2026-09-20 04:20:12.168
- 最近：2026-09-20T04:20:12.168+08:00

```text
[2026-09-20 04:20:12.168] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - graphql处理失败:找不到对应的处理服务:graphql
```

### INC-20260920-62ccf61446 · unknown

- 指纹：`找不到对应的处理服务:host.describe`
- 次数：2
- 首次：2026-09-20 03:51:23.343
- 最近：2026-09-20T03:51:23.343+08:00

```text
[2026-09-20 03:51:23.343] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - host.describe处理失败:找不到对应的处理服务:host.describe
```

### INC-20260920-1651161ec6 · unknown

- 指纹：`找不到对应的处理服务:jmreport/excelQueryByTemplate`
- 次数：1
- 首次：2026-09-19T14:05:41.204+08:00
- 最近：2026-09-19T14:05:41.204+08:00

```text
5710:2026-09-19T14:05:41.204+08:00 ERROR 47233 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : jmreport/excelQueryByTemplate处理失败:找不到对应的处理服务:jmreport/excelQueryByTemplate
```

### INC-20260920-c2523033ac · unknown

- 指纹：`找不到对应的处理服务:kconfig`
- 次数：2
- 首次：2026-09-20 04:19:53.110
- 最近：2026-09-20T04:19:53.110+08:00

```text
[2026-09-20 04:19:53.110] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - kconfig处理失败:找不到对应的处理服务:kconfig
```

### INC-20260920-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：6
- 首次：2026-09-18T18:38:40.318+08:00
- 最近：2026-09-20T02:46:23.741+08:00

```text
[2026-09-20 02:26:19.563] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

