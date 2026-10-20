# 生产后端监控摘要

- 采集时间：2026-10-20T16:07:42+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-20/20261020-160741/raw.txt`
- 可处理异常：138
- 已修复历史异常：0
- 预期业务噪音：114

## 服务状态

```text
enabled
active
MainPID=292714
ExecMainStartTimestamp=Mon 2026-10-19 13:25:35 CST
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
/dev/vda3         40901312 21984756  17026188      57% /
/dev/vda3         40901312 21984756  17026188      57% /
/dev/vda3         40901312 21984756  17026188      57% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3959         421         524        3897        3496
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 2 | medium | `No bean named 'system' available` |
| expected | observed | 2 | info | `手机号或密码错误` |
| unknown | open | 1 | medium | `找不到对应的处理服务:` |
| unknown | open | 2 | medium | `找不到对应的处理服务:config` |
| unknown | open | 2 | medium | `找不到对应的处理服务:config/` |
| unknown | open | 2 | medium | `找不到对应的处理服务:contents/` |
| unknown | open | 2 | medium | `找不到对应的处理服务:contents/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:contents/root` |
| unknown | open | 2 | medium | `找不到对应的处理服务:contents/root/` |
| unknown | open | 2 | medium | `找不到对应的处理服务:contents/workspace` |
| unknown | open | 2 | medium | `找不到对应的处理服务:contents/workspace/` |
| unknown | open | 74 | medium | `找不到对应的处理服务:design/items/reorder` |
| unknown | open | 2 | medium | `找不到对应的处理服务:env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:kernels` |
| unknown | open | 1 | medium | `找不到对应的处理服务:material/health/report` |
| unknown | open | 6 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 2 | medium | `找不到对应的处理服务:object_info` |
| unknown | open | 2 | medium | `找不到对应的处理服务:queue` |
| unknown | open | 2 | medium | `找不到对应的处理服务:sessions` |
| unknown | open | 2 | medium | `找不到对应的处理服务:settings` |
| unknown | open | 2 | medium | `找不到对应的处理服务:spec` |
| unknown | open | 2 | medium | `找不到对应的处理服务:status` |
| unknown | open | 2 | medium | `找不到对应的处理服务:status/` |
| unknown | open | 2 | medium | `找不到对应的处理服务:system_stats` |
| unknown | open | 2 | medium | `找不到对应的处理服务:tags` |
| unknown | open | 2 | medium | `找不到对应的处理服务:terminals` |
| unknown | open | 2 | medium | `找不到对应的处理服务:version` |
| unknown | open | 12 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 110 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 2 | info | `首次登录请授权手机号` |

## 证据

### INC-20261020-c08609e4cc · unknown

- 指纹：`No bean named 'system' available`
- 次数：2
- 首次：2026-10-19T20:50:54.223+08:00
- 最近：2026-10-19T20:51:54.435+08:00

```text
4851:2026-10-19T20:50:54.223+08:00 ERROR 292714 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : system-info处理失败:No bean named 'system' available
```

### INC-20261020-f7155d2eba · unknown

- 指纹：`找不到对应的处理服务:`
- 次数：1
- 首次：2026-10-19T20:50:55.051+08:00
- 最近：2026-10-19T20:50:55.051+08:00

```text
6485:2026-10-19T20:50:55.051+08:00 ERROR 292714 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : 处理失败:找不到对应的处理服务:
```

### INC-20261020-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：2
- 首次：2026-10-19T20:50:54.392+08:00
- 最近：2026-10-19T20:51:54.423+08:00

```text
5710:2026-10-19T20:50:54.392+08:00 ERROR 292714 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : config处理失败:找不到对应的处理服务:config
```

### INC-20261020-5f55b30272 · unknown

- 指纹：`找不到对应的处理服务:config/`
- 次数：2
- 首次：2026-10-19T20:50:54.492+08:00
- 最近：2026-10-19T20:51:54.547+08:00

```text
6399:2026-10-19T20:50:54.492+08:00 ERROR 292714 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : config/处理失败:找不到对应的处理服务:config/
```

### INC-20261020-e0e4df6c81 · unknown

- 指纹：`找不到对应的处理服务:contents/`
- 次数：2
- 首次：2026-10-19T20:50:54.400+08:00
- 最近：2026-10-19T20:51:54.233+08:00

```text
5797:2026-10-19T20:50:54.400+08:00 ERROR 292714 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : contents/处理失败:找不到对应的处理服务:contents/
```

### INC-20261020-b695769ce3 · unknown

- 指纹：`找不到对应的处理服务:contents/.env`
- 次数：2
- 首次：2026-10-19T20:50:54.273+08:00
- 最近：2026-10-19T20:51:54.214+08:00

```text
5367:2026-10-19T20:50:54.273+08:00 ERROR 292714 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : contents/.env处理失败:找不到对应的处理服务:contents/.env
```

### INC-20261020-cb81988995 · unknown

- 指纹：`找不到对应的处理服务:contents/root`
- 次数：2
- 首次：2026-10-19T20:50:54.441+08:00
- 最近：2026-10-19T20:51:54.183+08:00

```text
6313:2026-10-19T20:50:54.441+08:00 ERROR 292714 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : contents/root处理失败:找不到对应的处理服务:contents/root
```

### INC-20261020-04b2780e01 · unknown

- 指纹：`找不到对应的处理服务:contents/root/`
- 次数：2
- 首次：2026-10-19T20:50:54.254+08:00
- 最近：2026-10-19T20:51:54.406+08:00

```text
5195:2026-10-19T20:50:54.254+08:00 ERROR 292714 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : contents/root/处理失败:找不到对应的处理服务:contents/root/
```

### INC-20261020-9b624416f3 · unknown

- 指纹：`找不到对应的处理服务:contents/workspace`
- 次数：2
- 首次：2026-10-19T20:50:54.216+08:00
- 最近：2026-10-19T20:51:54.297+08:00

```text
4735:2026-10-19T20:50:54.216+08:00 ERROR 292714 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : contents/workspace处理失败:找不到对应的处理服务:contents/workspace
```

### INC-20261020-8cfe62be13 · unknown

- 指纹：`找不到对应的处理服务:contents/workspace/`
- 次数：2
- 首次：2026-10-19T20:50:54.247+08:00
- 最近：2026-10-19T20:51:54.368+08:00

```text
5108:2026-10-19T20:50:54.247+08:00 ERROR 292714 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : contents/workspace/处理失败:找不到对应的处理服务:contents/workspace/
```

