# 生产后端监控摘要

- 采集时间：2026-10-03T03:29:40+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-03/20261003-032939/raw.txt`
- 可处理异常：97
- 已修复历史异常：0
- 预期业务噪音：129

## 服务状态

```text
enabled
active
MainPID=103193
ExecMainStartTimestamp=Sun 2026-09-27 16:48:56 CST
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
/dev/vda3         40901312 8745524  30265420      23% /
/dev/vda3         40901312 8745524  30265420      23% /
/dev/vda3         40901312 8745524  30265420      23% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3408         966         260        3637        4048
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `name不能为空` |
| unknown | open | 7 | medium | `下次跟进时间不能早于今天` |
| unknown | open | 2 | medium | `客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除` |
| expected | observed | 9 | info | `找不到对应的处理服务:.env` |
| unknown | open | 6 | medium | `找不到对应的处理服务:2.0/mlflow/experiments/list` |
| unknown | open | 6 | medium | `找不到对应的处理服务:2.0/mlflow/experiments/search` |
| unknown | open | 6 | medium | `找不到对应的处理服务:actpt.data` |
| unknown | open | 1 | medium | `找不到对应的处理服务:checkCleanIp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:index.php/v1/config/application` |
| unknown | open | 7 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 12 | medium | `找不到对应的处理服务:merge` |
| unknown | open | 1 | medium | `找不到对应的处理服务:modular-connector/login/qrvuf` |
| unknown | open | 1 | medium | `找不到对应的处理服务:raster/search/` |
| unknown | open | 2 | medium | `找不到对应的处理服务:secrets.yml` |
| unknown | open | 8 | medium | `找不到对应的处理服务:session/properties` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 6 | medium | `找不到对应的处理服务:system/system/string_render` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/config/application` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/fabric_device_auth/fortigate/init` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/init_consts` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/monitor/messages` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/version` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 3 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 17 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 120 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261003-ca771ba23d · unknown

- 指纹：`name不能为空`
- 次数：1
- 首次：2026-09-27T16:49:47.734+08:00
- 最近：2026-09-27T16:49:47.734+08:00

```text
757:2026-09-27T16:49:47.734+08:00 ERROR 103193 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : site/lead/submit处理失败: name不能为空
```

### INC-20261003-05ad2b36fb · unknown

- 指纹：`下次跟进时间不能早于今天`
- 次数：7
- 首次：2026-09-30T16:03:29.918+08:00
- 最近：2026-09-30T16:03:35.642+08:00

```text
13211:2026-09-30T16:03:29.918+08:00 ERROR 103193 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : customer/customer-follow/save处理失败:下次跟进时间不能早于今天
```

### INC-20261003-83305d84a1 · unknown

- 指纹：`客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除`
- 次数：2
- 首次：2026-10-01T14:31:16.985+08:00
- 最近：2026-10-01T14:31:19.202+08:00

```text
18119:2026-10-01T14:31:16.985+08:00 ERROR 103193 --- [  XNIO-1 task-3] c.t.e.f.w.m.processor.ApiHttpProcessor   : customer/customer/delete处理失败:客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除
```

### INC-20261003-ad7561a398 · unknown

- 指纹：`找不到对应的处理服务:2.0/mlflow/experiments/list`
- 次数：6
- 首次：2026-09-28T14:58:45.015+08:00
- 最近：2026-09-30T03:56:14.961+08:00

```text
4092:2026-09-28T14:58:45.015+08:00 ERROR 103193 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : 2.0/mlflow/experiments/list处理失败:找不到对应的处理服务:2.0/mlflow/experiments/list
```

### INC-20261003-afef172054 · unknown

- 指纹：`找不到对应的处理服务:2.0/mlflow/experiments/search`
- 次数：6
- 首次：2026-09-28T14:58:44.978+08:00
- 最近：2026-09-30T03:56:14.928+08:00

```text
4006:2026-09-28T14:58:44.978+08:00 ERROR 103193 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : 2.0/mlflow/experiments/search处理失败:找不到对应的处理服务:2.0/mlflow/experiments/search
```

### INC-20261003-4444eb57d2 · unknown

- 指纹：`找不到对应的处理服务:actpt.data`
- 次数：6
- 首次：2026-09-28T14:58:45.868+08:00
- 最近：2026-09-30T03:56:15.802+08:00

```text
4436:2026-09-28T14:58:45.868+08:00 ERROR 103193 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : actpt.data处理失败:找不到对应的处理服务:actpt.data
```

### INC-20261003-c835f67858 · unknown

- 指纹：`找不到对应的处理服务:checkCleanIp`
- 次数：1
- 首次：2026-09-29T02:02:32.604+08:00
- 最近：2026-09-29T02:02:32.604+08:00

```text
5480:2026-09-29T02:02:32.604+08:00 ERROR 103193 --- [  XNIO-1 task-3] c.t.e.f.w.m.processor.ApiHttpProcessor   : checkCleanIp处理失败:找不到对应的处理服务:checkCleanIp
```

### INC-20261003-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：1
- 首次：2026-09-29T13:13:44.643+08:00
- 最近：2026-09-29T13:13:44.643+08:00

```text
7286:2026-09-29T13:13:44.643+08:00 ERROR 103193 --- [  XNIO-1 task-3] c.t.e.f.w.m.processor.ApiHttpProcessor   : dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20261003-80b62c529e · unknown

- 指纹：`找不到对应的处理服务:index.php/v1/config/application`
- 次数：1
- 首次：2026-10-02T11:48:24.203+08:00
- 最近：2026-10-02T11:48:24.203+08:00

```text
20141:2026-10-02T11:48:24.203+08:00 ERROR 103193 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : index.php/v1/config/application处理失败:找不到对应的处理服务:index.php/v1/config/application
```

### INC-20261003-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：7
- 首次：2026-09-27T17:34:20.851+08:00
- 最近：2026-10-02T18:09:41.373+08:00

```text
1254:2026-09-27T17:34:20.851+08:00 ERROR 103193 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : mcp处理失败:找不到对应的处理服务:mcp
```

