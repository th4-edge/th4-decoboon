# 生产后端监控摘要

- 采集时间：2026-10-05T18:17:22+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-05/20261005-181721/raw.txt`
- 可处理异常：62
- 已修复历史异常：0
- 预期业务噪音：131

## 服务状态

```text
enabled
active
MainPID=138035
ExecMainStartTimestamp=Mon 2026-10-05 05:24:48 CST
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
/dev/vda3         40901312 9094272  29916672      24% /
/dev/vda3         40901312 9094272  29916672      24% /
/dev/vda3         40901312 9094272  29916672      24% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3327         917         137        3643        4128
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 6 | medium | `客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除` |
| expected | observed | 4 | info | `找不到对应的处理服务:.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 2 | medium | `找不到对应的处理服务:backup` |
| unknown | open | 2 | medium | `找不到对应的处理服务:chat/get-chat-session/1` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config.js` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 3 | medium | `找不到对应的处理服务:index.php/v1/config/application` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 2 | medium | `找不到对应的处理服务:modular-connector/login/slrdg` |
| unknown | open | 2 | medium | `找不到对应的处理服务:phpinfo.php` |
| unknown | open | 2 | medium | `找不到对应的处理服务:raster/search/` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/config/application` |
| unknown | open | 4 | medium | `找不到对应的处理服务:v1/fabric_device_auth/fortigate/init` |
| unknown | open | 4 | medium | `找不到对应的处理服务:v1/init_consts` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/monitor/messages` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/version` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 2 | medium | `找不到对应的实体处理服务[complete]` |
| unknown | open | 4 | medium | `设计方案尚未完成，不能生成客户报价` |
| unknown | open | 9 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 124 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 2 | medium | `该房屋已指定设计师，量房完成必须由指定设计师执行` |
| expected | observed | 3 | info | `首次登录请授权手机号` |

## 证据

### INC-20261005-83305d84a1 · unknown

- 指纹：`客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除`
- 次数：6
- 首次：2026-10-05 17:44:26.379
- 最近：2026-10-05T17:44:59.569+08:00

```text
[2026-10-05 17:44:26.379] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - customer/customer/delete处理失败:客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除
```

### INC-20261005-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：2
- 首次：2026-10-05 15:52:03.199
- 最近：2026-10-05T15:52:03.199+08:00

```text
[2026-10-05 15:52:03.199] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20261005-1638ca7b18 · unknown

- 指纹：`找不到对应的处理服务:backup`
- 次数：2
- 首次：2026-10-05 16:31:12.712
- 最近：2026-10-05T16:31:12.712+08:00

```text
[2026-10-05 16:31:12.712] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - backup处理失败:找不到对应的处理服务:backup
```

### INC-20261005-5f3c586f32 · unknown

- 指纹：`找不到对应的处理服务:chat/get-chat-session/1`
- 次数：2
- 首次：2026-10-05 16:31:14.686
- 最近：2026-10-05T16:31:14.686+08:00

```text
[2026-10-05 16:31:14.686] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - chat/get-chat-session/1处理失败:找不到对应的处理服务:chat/get-chat-session/1
```

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
- 次数：3
- 首次：2026-10-05 01:41:03.560
- 最近：2026-10-05T16:29:27.618+08:00

```text
[2026-10-05 01:41:03.560] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - index.php/v1/config/application处理失败:找不到对应的处理服务:index.php/v1/config/application
```

### INC-20261005-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：2
- 首次：2026-10-05 18:08:30.801
- 最近：2026-10-05T18:08:30.801+08:00

```text
[2026-10-05 18:08:30.801] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261005-95fa567f7f · unknown

- 指纹：`找不到对应的处理服务:modular-connector/login/slrdg`
- 次数：2
- 首次：2026-10-05 16:32:26.509
- 最近：2026-10-05T16:32:26.509+08:00

```text
[2026-10-05 16:32:26.509] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - modular-connector/login/slrdg处理失败:找不到对应的处理服务:modular-connector/login/slrdg
```

### INC-20261005-8f59be778a · unknown

- 指纹：`找不到对应的处理服务:phpinfo.php`
- 次数：2
- 首次：2026-10-05 06:17:26.827
- 最近：2026-10-05T06:17:26.827+08:00

```text
[2026-10-05 06:17:26.827] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - phpinfo.php处理失败:找不到对应的处理服务:phpinfo.php
```

