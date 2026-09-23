# 生产后端监控摘要

- 采集时间：2026-09-22T13:01:47+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-22/20260922-130146/raw.txt`
- 可处理异常：45
- 已修复历史异常：0
- 预期业务噪音：44

## 服务状态

```text
enabled
active
MainPID=63746
ExecMainStartTimestamp=Mon 2026-09-21 05:06:10 CST
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
/dev/vda3         40901312 6827320  32183624      18% /
/dev/vda3         40901312 6827320  32183624      18% /
/dev/vda3         40901312 6827320  32183624      18% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3082         591         362        4434        4373
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| expected | observed | 9 | info | `找不到对应的处理服务:.env` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env.local` |
| unknown | open | 2 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 12 | medium | `找不到对应的处理服务:auth/signin` |
| unknown | open | 5 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 5 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 5 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 5 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 5 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 3 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 31 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 2 | info | `首次登录请授权手机号` |

## 证据

### INC-20260922-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：2
- 首次：2026-09-22 06:26:40.863
- 最近：2026-09-22T06:26:40.863+08:00

```text
[2026-09-22 06:26:40.863] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20260922-0cd74dd730 · unknown

- 指纹：`找不到对应的处理服务:auth/signin`
- 次数：12
- 首次：2026-09-21T14:04:41.584+08:00
- 最近：2026-09-21T14:04:48.401+08:00

```text
1615:2026-09-21T14:04:41.584+08:00 ERROR 63746 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : auth/signin处理失败:找不到对应的处理服务:auth/signin
```

### INC-20260922-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：5
- 首次：2026-09-21T21:34:33.113+08:00
- 最近：2026-09-22T06:40:38.274+08:00

```text
[2026-09-22 04:16:25.349] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20260922-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：2
- 首次：2026-09-22 08:07:43.677
- 最近：2026-09-22T08:07:43.677+08:00

```text
[2026-09-22 08:07:43.677] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20260922-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：5
- 首次：2026-09-21T21:34:33.397+08:00
- 最近：2026-09-22T06:40:38.525+08:00

```text
[2026-09-22 04:16:25.749] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20260922-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：5
- 首次：2026-09-21T21:34:30.882+08:00
- 最近：2026-09-22T06:40:36.298+08:00

```text
[2026-09-22 04:16:22.868] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/.env处理失败:找不到对应的处理服务:v1/.env
```

### INC-20260922-e359a7a32a · unknown

- 指纹：`找不到对应的处理服务:v2/.env`
- 次数：5
- 首次：2026-09-21T21:34:31.167+08:00
- 最近：2026-09-22T06:40:36.549+08:00

```text
[2026-09-22 04:16:23.132] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v2/.env处理失败:找不到对应的处理服务:v2/.env
```

### INC-20260922-7cc2b644b1 · unknown

- 指纹：`找不到对应的处理服务:v3/.env`
- 次数：5
- 首次：2026-09-21T21:34:32.830+08:00
- 最近：2026-09-22T06:40:38.024+08:00

```text
[2026-09-22 04:16:24.968] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v3/.env处理失败:找不到对应的处理服务:v3/.env
```

### INC-20260922-dfb2d5ba8f · unknown

- 指纹：`找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php`
- 次数：1
- 首次：2026-09-21T08:19:35.388+08:00
- 最近：2026-09-21T08:19:35.388+08:00

```text
1529:2026-09-21T08:19:35.388+08:00 ERROR 63746 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php处理失败:找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php
```

### INC-20260922-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：3
- 首次：2026-09-21T23:20:57.955+08:00
- 最近：2026-09-22T10:27:23.371+08:00

```text
[2026-09-22 10:27:23.371] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:访问受限，授权已过期，请重新登录后再访问。
```

