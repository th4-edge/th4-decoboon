# 生产后端监控摘要

- 采集时间：2026-10-26T10:34:12+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-26/20261026-103411/raw.txt`
- 可处理异常：56
- 已修复历史异常：0
- 预期业务噪音：138

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
/dev/vda3         40901312 24267148  14743796      63% /
/dev/vda3         40901312 24267148  14743796      63% /
/dev/vda3         40901312 24267148  14743796      63% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3807         395         331        3882        3649
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 2 | medium | `任务状态或动作不能为空` |
| unknown | open | 1 | medium | `客户已完成首联跟进，手机号不允许修改` |
| expected | observed | 10 | info | `手机号或密码错误` |
| expected | observed | 3 | info | `找不到对应的处理服务:.env` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env.local` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 6 | medium | `找不到对应的处理服务:account` |
| unknown | open | 1 | medium | `找不到对应的处理服务:credentials.json` |
| unknown | open | 2 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:market/fastBuy` |
| unknown | open | 3 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 2 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 2 | medium | `提交阶段验收前必须上传现场照片或视频` |
| unknown | open | 5 | medium | `设计分项不存在或无权访问` |
| unknown | open | 19 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 124 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `量房完成时间不能早于预约时间` |
| unknown | open | 2 | medium | `阶段内仍有未完成施工任务` |

## 证据

### INC-20261026-1e5e8eab34 · unknown

- 指纹：`任务状态或动作不能为空`
- 次数：2
- 首次：2026-10-25T18:55:04.310+08:00
- 最近：2026-10-25T18:55:04.374+08:00

```text
13306:2026-10-25T18:55:04.310+08:00 ERROR 328325 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : project/task/update处理失败:任务状态或动作不能为空
```

### INC-20261026-d9b924eefd · unknown

- 指纹：`客户已完成首联跟进，手机号不允许修改`
- 次数：1
- 首次：2026-10-25T18:29:46.500+08:00
- 最近：2026-10-25T18:29:46.500+08:00

```text
11462:2026-10-25T18:29:46.500+08:00 ERROR 328325 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : customer/customer/update处理失败:客户已完成首联跟进，手机号不允许修改
```

### INC-20261026-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：1
- 首次：2026-10-25T09:37:21.552+08:00
- 最近：2026-10-25T09:37:21.552+08:00

```text
6970:2026-10-25T09:37:21.552+08:00 ERROR 328325 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20261026-74fa9d02c1 · unknown

- 指纹：`找不到对应的处理服务:account`
- 次数：6
- 首次：2026-10-26 00:03:55.118
- 最近：2026-10-26T00:51:11.101+08:00

```text
[2026-10-26 00:03:55.118] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - account处理失败:找不到对应的处理服务:account
```

### INC-20261026-a69432181b · unknown

- 指纹：`找不到对应的处理服务:credentials.json`
- 次数：1
- 首次：2026-10-25T09:03:11.605+08:00
- 最近：2026-10-25T09:03:11.605+08:00

```text
6884:2026-10-25T09:03:11.605+08:00 ERROR 328325 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : credentials.json处理失败:找不到对应的处理服务:credentials.json
```

### INC-20261026-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：2
- 首次：2026-10-26 07:04:35.350
- 最近：2026-10-26T07:04:35.350+08:00

```text
[2026-10-26 07:04:35.350] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20261026-57044ecb62 · unknown

- 指纹：`找不到对应的处理服务:market/fastBuy`
- 次数：2
- 首次：2026-10-26 00:57:42.753
- 最近：2026-10-26T00:57:42.753+08:00

```text
[2026-10-26 00:57:42.753] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - market/fastBuy处理失败:找不到对应的处理服务:market/fastBuy
```

### INC-20261026-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：3
- 首次：2026-10-25T08:50:08.721+08:00
- 最近：2026-10-26T08:46:47.508+08:00

```text
[2026-10-26 08:46:47.508] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261026-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：2
- 首次：2026-10-26 07:04:35.803
- 最近：2026-10-26T07:04:35.803+08:00

```text
[2026-10-26 07:04:35.803] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20261026-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：2
- 首次：2026-10-26 07:04:31.748
- 最近：2026-10-26T07:04:31.748+08:00

```text
[2026-10-26 07:04:31.748] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/.env处理失败:找不到对应的处理服务:v1/.env
```

