# 生产后端监控摘要

- 采集时间：2026-09-17T18:41:11+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-17/20260917-184110/raw.txt`
- 可处理异常：187
- 已修复历史异常：0
- 预期业务噪音：94

## 服务状态

```text
enabled
active
MainPID=39976
ExecMainStartTimestamp=Thu 2026-09-17 01:46:11 CST
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
/dev/vda3         40901312 5884792  33126152      16% /
/dev/vda3         40901312 5884792  33126152      16% /
/dev/vda3         40901312 5884792  33126152      16% /
               total        used        free      shared  buff/cache   available
Mem:            7456        2198        2423         124        3234        5258
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 4 | medium | `不能操作自己的员工记录` |
| unknown | open | 150 | medium | `当前员工已离职或停用` |
| expected | observed | 2 | info | `手机号或密码错误` |
| expected | observed | 8 | info | `找不到对应的处理服务:.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:checkCleanIp` |
| unknown | open | 2 | medium | `找不到对应的处理服务:config` |
| unknown | open | 2 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:graphql` |
| unknown | open | 2 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 2 | medium | `找不到对应的实体处理服务[<hex>.env]` |
| unknown | open | 9 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 80 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 4 | medium | `退出企业后 30 天内不能加入新企业` |
| expected | observed | 4 | info | `首次登录请授权手机号` |

## 证据

### INC-20260917-e3deff3d62 · unknown

- 指纹：`不能操作自己的员工记录`
- 次数：4
- 首次：2026-09-17 01:50:01.179
- 最近：2026-09-17T14:01:10.550+08:00

```text
[2026-09-17 01:50:01.179] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - company/member/info处理失败:不能操作自己的员工记录
```

### INC-20260917-627186ca11 · unknown

- 指纹：`当前员工已离职或停用`
- 次数：150
- 首次：2026-09-17 14:23:37.214
- 最近：2026-09-17T14:30:41.963+08:00

```text
[2026-09-17 14:23:37.214] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - user/user/resign处理失败:当前员工已离职或停用
```

### INC-20260917-c835f67858 · unknown

- 指纹：`找不到对应的处理服务:checkCleanIp`
- 次数：2
- 首次：2026-09-17 05:25:47.118
- 最近：2026-09-17T05:25:47.118+08:00

```text
[2026-09-17 05:25:47.118] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - checkCleanIp处理失败:找不到对应的处理服务:checkCleanIp
```

### INC-20260917-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：2
- 首次：2026-09-17 09:34:39.759
- 最近：2026-09-17T09:34:39.759+08:00

```text
[2026-09-17 09:34:39.759] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - config处理失败:找不到对应的处理服务:config
```

### INC-20260917-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：2
- 首次：2026-09-17 11:25:08.128
- 最近：2026-09-17T11:25:08.128+08:00

```text
[2026-09-17 11:25:08.128] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20260917-8478b5fd37 · unknown

- 指纹：`找不到对应的处理服务:graphql`
- 次数：2
- 首次：2026-09-17 01:46:05.396
- 最近：2026-09-17T01:46:05.396+08:00

```text
[2026-09-17 01:46:05.396] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - graphql处理失败:找不到对应的处理服务:graphql
```

### INC-20260917-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：2
- 首次：2026-09-17 11:25:08.425
- 最近：2026-09-17T11:25:08.425+08:00

```text
[2026-09-17 11:25:08.425] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20260917-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：2
- 首次：2026-09-17 11:25:05.462
- 最近：2026-09-17T11:25:05.462+08:00

```text
[2026-09-17 11:25:05.462] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/.env处理失败:找不到对应的处理服务:v1/.env
```

### INC-20260917-e359a7a32a · unknown

- 指纹：`找不到对应的处理服务:v2/.env`
- 次数：2
- 首次：2026-09-17 11:25:05.739
- 最近：2026-09-17T11:25:05.739+08:00

```text
[2026-09-17 11:25:05.739] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v2/.env处理失败:找不到对应的处理服务:v2/.env
```

### INC-20260917-7cc2b644b1 · unknown

- 指纹：`找不到对应的处理服务:v3/.env`
- 次数：2
- 首次：2026-09-17 11:25:07.690
- 最近：2026-09-17T11:25:07.690+08:00

```text
[2026-09-17 11:25:07.690] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v3/.env处理失败:找不到对应的处理服务:v3/.env
```

