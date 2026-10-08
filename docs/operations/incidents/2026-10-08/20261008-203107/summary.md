# 生产后端监控摘要

- 采集时间：2026-10-08T20:31:08+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon-payroll-confirm-boss/docs/operations/incidents/2026-10-08/20261008-203107/raw.txt`
- 可处理异常：22
- 已修复历史异常：6
- 预期业务噪音：151

## 服务状态

```text
enabled
active
MainPID=205998
ExecMainStartTimestamp=Thu 2026-10-08 20:30:20 CST
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
/dev/vda3         40901312 17181052  21829892      45% /
/dev/vda3         40901312 17181052  21829892      45% /
/dev/vda3         40901312 17181052  21829892      45% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3702         616         638        4073        3753
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 2 | medium | `下次跟进时间不能早于今天` |
| unknown | fixed | 6 | medium | `客户存在未完成量房预约，请先处理预约后休眠` |
| unknown | open | 3 | medium | `客户尚未跟进或已流失，不能指定设计师` |
| unknown | open | 1 | medium | `当前账号无权执行该操作` |
| expected | observed | 2 | info | `手机号或密码错误` |
| unknown | open | 1 | medium | `找不到对应的处理服务:` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:auth.json` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:postman.json` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 5 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 147 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该房屋已有客户确认报价或唯一项目，设计方案已锁定；调整请发起合同变更单` |

## 证据

### INC-20261008-05ad2b36fb · unknown

- 指纹：`下次跟进时间不能早于今天`
- 次数：2
- 首次：2026-10-08 11:13:24.671
- 最近：2026-10-08 11:13:28.402

```text
[2026-10-08 11:13:24.671] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - customer/customer-follow/save处理失败:下次跟进时间不能早于今天
```

### INC-20261008-b241888433 · unknown

- 指纹：`客户尚未跟进或已流失，不能指定设计师`
- 次数：3
- 首次：2026-10-08 16:40:14.156
- 最近：2026-10-08 16:40:32.586

```text
[2026-10-08 16:40:14.156] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - customer/customer/handoff处理失败:客户尚未跟进或已流失，不能指定设计师
```

### INC-20261008-979f5a015e · unknown

- 指纹：`当前账号无权执行该操作`
- 次数：1
- 首次：2026-10-08 20:18:07.650
- 最近：2026-10-08 20:18:07.650

```text
[2026-10-08 20:18:07.650] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - customer/customer/overview处理失败:当前账号无权执行该操作
```

### INC-20261008-f7155d2eba · unknown

- 指纹：`找不到对应的处理服务:`
- 次数：1
- 首次：2026-10-08 20:16:32.341
- 最近：2026-10-08 20:16:32.341

```text
[2026-10-08 20:16:32.341] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - 处理失败:找不到对应的处理服务:
```

### INC-20261008-a490454967 · unknown

- 指纹：`找不到对应的处理服务:auth.json`
- 次数：1
- 首次：2026-10-08 01:48:43.986
- 最近：2026-10-08 01:48:43.986

```text
[2026-10-08 01:48:43.986] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth.json处理失败:找不到对应的处理服务:auth.json
```

### INC-20261008-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：1
- 首次：2026-10-08 15:08:27.913
- 最近：2026-10-08 15:08:27.913

```text
[2026-10-08 15:08:27.913] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20261008-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：2
- 首次：2026-10-08 00:17:24.389
- 最近：2026-10-08 17:58:19.305

```text
[2026-10-08 00:17:24.389] [ERROR] [XNIO-1 task-9] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261008-1bc8c48ec8 · unknown

- 指纹：`找不到对应的处理服务:postman.json`
- 次数：1
- 首次：2026-10-08 01:48:43.986
- 最近：2026-10-08 01:48:43.986

```text
[2026-10-08 01:48:43.986] [ERROR] [XNIO-1 task-9] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - postman.json处理失败:找不到对应的处理服务:postman.json
```

### INC-20261008-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：1
- 首次：2026-10-08 15:08:28.008
- 最近：2026-10-08 15:08:28.008

```text
[2026-10-08 15:08:28.008] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20261008-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：1
- 首次：2026-10-08 15:08:27.222
- 最近：2026-10-08 15:08:27.222

```text
[2026-10-08 15:08:27.222] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/.env处理失败:找不到对应的处理服务:v1/.env
```
