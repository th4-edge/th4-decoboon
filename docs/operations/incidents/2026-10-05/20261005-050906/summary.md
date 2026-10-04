# 生产后端监控摘要

- 采集时间：2026-10-05T05:09:06+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-05/20261005-050906/raw.txt`
- 可处理异常：8
- 已修复历史异常：0
- 预期业务噪音：26

## 服务状态

```text
enabled
active
MainPID=136956
ExecMainStartTimestamp=Mon 2026-10-05 05:06:49 CST
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
/dev/vda3         40901312 8947584  30063360      23% /
/dev/vda3         40901312 8947584  30063360      23% /
/dev/vda3         40901312 8947584  30063360      23% /
               total        used        free      shared  buff/cache   available
Mem:            7456        2767        1456         137        3664        4689
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| expected | observed | 2 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config.js` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:index.php/v1/config/application` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 1 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 24 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261005-492596d42a · unknown

- 指纹：`找不到对应的处理服务:config.js`
- 次数：1
- 首次：2026-10-05 01:40:38.765
- 最近：2026-10-05 01:40:38.765

```text
[2026-10-05 01:40:38.765] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - config.js处理失败:找不到对应的处理服务:config.js
```

### INC-20261005-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：1
- 首次：2026-10-05 04:48:45.371
- 最近：2026-10-05 04:48:45.371

```text
[2026-10-05 04:48:45.371] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20261005-80b62c529e · unknown

- 指纹：`找不到对应的处理服务:index.php/v1/config/application`
- 次数：1
- 首次：2026-10-05 01:41:03.560
- 最近：2026-10-05 01:41:03.560

```text
[2026-10-05 01:41:03.560] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - index.php/v1/config/application处理失败:找不到对应的处理服务:index.php/v1/config/application
```

### INC-20261005-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：1
- 首次：2026-10-05 04:48:45.420
- 最近：2026-10-05 04:48:45.420

```text
[2026-10-05 04:48:45.420] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20261005-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：1
- 首次：2026-10-05 04:48:45.017
- 最近：2026-10-05 04:48:45.017

```text
[2026-10-05 04:48:45.017] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/.env处理失败:找不到对应的处理服务:v1/.env
```

### INC-20261005-e359a7a32a · unknown

- 指纹：`找不到对应的处理服务:v2/.env`
- 次数：1
- 首次：2026-10-05 04:48:45.060
- 最近：2026-10-05 04:48:45.060

```text
[2026-10-05 04:48:45.060] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v2/.env处理失败:找不到对应的处理服务:v2/.env
```

### INC-20261005-7cc2b644b1 · unknown

- 指纹：`找不到对应的处理服务:v3/.env`
- 次数：1
- 首次：2026-10-05 04:48:45.326
- 最近：2026-10-05 04:48:45.326

```text
[2026-10-05 04:48:45.326] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v3/.env处理失败:找不到对应的处理服务:v3/.env
```

### INC-20261005-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：1
- 首次：2026-10-05 02:43:16.850
- 最近：2026-10-05 02:43:16.850

```text
[2026-10-05 02:43:16.850] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:访问受限，授权已过期，请重新登录后再访问。
```

