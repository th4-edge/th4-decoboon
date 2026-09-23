# 生产后端监控摘要

- 采集时间：2026-09-18T13:01:46+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-18/20260918-130145/raw.txt`
- 可处理异常：96
- 已修复历史异常：0
- 预期业务噪音：56

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
/dev/vda3         40901312 5886452  33124492      16% /
/dev/vda3         40901312 5886452  33124492      16% /
/dev/vda3         40901312 5886452  33124492      16% /
               total        used        free      shared  buff/cache   available
Mem:            7456        2220        2397         124        3237        5235
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 3 | medium | `不能操作自己的员工记录` |
| unknown | open | 75 | medium | `当前员工已离职或停用` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 4 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:checkCleanIp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:graphql` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 1 | medium | `找不到对应的实体处理服务[<hex>.env]` |
| unknown | open | 5 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 49 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 2 | medium | `退出企业后 30 天内不能加入新企业` |
| expected | observed | 2 | info | `首次登录请授权手机号` |

## 证据

### INC-20260918-e3deff3d62 · unknown

- 指纹：`不能操作自己的员工记录`
- 次数：3
- 首次：2026-09-17T01:50:01.179+08:00
- 最近：2026-09-17T20:22:20.714+08:00

```text
2411:2026-09-17T01:50:01.179+08:00 ERROR 39976 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : company/member/info处理失败:不能操作自己的员工记录
```

### INC-20260918-627186ca11 · unknown

- 指纹：`当前员工已离职或停用`
- 次数：75
- 首次：2026-09-17T14:23:37.214+08:00
- 最近：2026-09-17T14:30:41.963+08:00

```text
5593:2026-09-17T14:23:37.214+08:00 ERROR 39976 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : user/user/resign处理失败:当前员工已离职或停用
```

### INC-20260918-c835f67858 · unknown

- 指纹：`找不到对应的处理服务:checkCleanIp`
- 次数：1
- 首次：2026-09-17T05:25:47.118+08:00
- 最近：2026-09-17T05:25:47.118+08:00

```text
2669:2026-09-17T05:25:47.118+08:00 ERROR 39976 --- [  XNIO-1 task-3] c.t.e.f.w.m.processor.ApiHttpProcessor   : checkCleanIp处理失败:找不到对应的处理服务:checkCleanIp
```

### INC-20260918-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：1
- 首次：2026-09-17T09:34:39.759+08:00
- 最近：2026-09-17T09:34:39.759+08:00

```text
3457:2026-09-17T09:34:39.759+08:00 ERROR 39976 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : config处理失败:找不到对应的处理服务:config
```

### INC-20260918-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：1
- 首次：2026-09-17T11:25:08.128+08:00
- 最近：2026-09-17T11:25:08.128+08:00

```text
4391:2026-09-17T11:25:08.128+08:00 ERROR 39976 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20260918-8478b5fd37 · unknown

- 指纹：`找不到对应的处理服务:graphql`
- 次数：1
- 首次：2026-09-17T01:46:05.396+08:00
- 最近：2026-09-17T01:46:05.396+08:00

```text
1743:2026-09-17T01:46:05.396+08:00 ERROR 39701 --- [  XNIO-1 task-3] c.t.e.f.w.m.processor.ApiHttpProcessor   : graphql处理失败:找不到对应的处理服务:graphql
```

### INC-20260918-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：1
- 首次：2026-09-17T23:54:36.942+08:00
- 最近：2026-09-17T23:54:36.942+08:00

```text
16438:2026-09-17T23:54:36.942+08:00 ERROR 39976 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20260918-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：1
- 首次：2026-09-17T11:25:08.425+08:00
- 最近：2026-09-17T11:25:08.425+08:00

```text
4477:2026-09-17T11:25:08.425+08:00 ERROR 39976 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20260918-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：1
- 首次：2026-09-17T11:25:05.462+08:00
- 最近：2026-09-17T11:25:05.462+08:00

```text
4133:2026-09-17T11:25:05.462+08:00 ERROR 39976 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : v1/.env处理失败:找不到对应的处理服务:v1/.env
```

### INC-20260918-e359a7a32a · unknown

- 指纹：`找不到对应的处理服务:v2/.env`
- 次数：1
- 首次：2026-09-17T11:25:05.739+08:00
- 最近：2026-09-17T11:25:05.739+08:00

```text
4219:2026-09-17T11:25:05.739+08:00 ERROR 39976 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : v2/.env处理失败:找不到对应的处理服务:v2/.env
```

