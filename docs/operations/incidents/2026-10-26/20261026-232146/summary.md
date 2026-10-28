# 生产后端监控摘要

- 采集时间：2026-10-26T23:21:48+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-26/20261026-232146/raw.txt`
- 可处理异常：62
- 已修复历史异常：0
- 预期业务噪音：132

## 服务状态

```text
enabled
active
MainPID=338867
ExecMainStartTimestamp=Mon 2026-10-26 11:02:41 CST
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
/dev/vda3         40901312 24516148  14494796      63% /
/dev/vda3         40901312 24516148  14494796      63% /
/dev/vda3         40901312 24516148  14494796      63% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3827         666         331        3591        3629
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 6 | medium | `Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.user.store.list.support.List.employee to com.th4.edge.tenant.user.model.Credentials` |
| unknown | open | 2 | medium | `不能操作自己的员工记录` |
| unknown | open | 1 | medium | `当前微信凭证不存在` |
| expected | observed | 5 | info | `手机号或密码错误` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env` |
| unknown | open | 3 | medium | `找不到对应的处理服务:account` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:market/fastBuy` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 28 | medium | `收款金额不能超过应收余额` |
| unknown | open | 1 | medium | `设计方案不存在或无权访问` |
| unknown | open | 10 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 124 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 4 | medium | `该手机号已有待接受邀请` |
| expected | observed | 2 | info | `首次登录请授权手机号` |

## 证据

### INC-20261026-8796714619 · unknown

- 指纹：`Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.user.store.list.support.List.employee to com.th4.edge.tenant.user.model.Credentials`
- 次数：6
- 首次：2026-10-26 17:51:26.179
- 最近：2026-10-26T17:56:40.882+08:00

```text
[2026-10-26 17:51:26.179] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - user/store/list处理失败:Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.user.store.list.support.List.employee to com.th4.edge.tenant.user.model.Credentials
```

### INC-20261026-e3deff3d62 · unknown

- 指纹：`不能操作自己的员工记录`
- 次数：2
- 首次：2026-10-26 17:37:52.524
- 最近：2026-10-26T17:37:52.524+08:00

```text
[2026-10-26 17:37:52.524] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - company/member/info处理失败:不能操作自己的员工记录
```

### INC-20261026-c0d1327688 · unknown

- 指纹：`当前微信凭证不存在`
- 次数：1
- 首次：2026-10-26 10:55:15.605
- 最近：2026-10-26 10:55:15.605

```text
[2026-10-26 10:55:15.605] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:当前微信凭证不存在
```

### INC-20261026-74fa9d02c1 · unknown

- 指纹：`找不到对应的处理服务:account`
- 次数：3
- 首次：2026-10-26 00:03:55.118
- 最近：2026-10-26 00:51:11.101

```text
[2026-10-26 00:03:55.118] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - account处理失败:找不到对应的处理服务:account
```

### INC-20261026-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：1
- 首次：2026-10-26 07:04:35.350
- 最近：2026-10-26 07:04:35.350

```text
[2026-10-26 07:04:35.350] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20261026-57044ecb62 · unknown

- 指纹：`找不到对应的处理服务:market/fastBuy`
- 次数：1
- 首次：2026-10-26 00:57:42.753
- 最近：2026-10-26 00:57:42.753

```text
[2026-10-26 00:57:42.753] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - market/fastBuy处理失败:找不到对应的处理服务:market/fastBuy
```

### INC-20261026-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：1
- 首次：2026-10-26 08:46:47.508
- 最近：2026-10-26 08:46:47.508

```text
[2026-10-26 08:46:47.508] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261026-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：1
- 首次：2026-10-26 07:04:35.803
- 最近：2026-10-26 07:04:35.803

```text
[2026-10-26 07:04:35.803] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20261026-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：1
- 首次：2026-10-26 07:04:31.748
- 最近：2026-10-26 07:04:31.748

```text
[2026-10-26 07:04:31.748] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v1/.env处理失败:找不到对应的处理服务:v1/.env
```

### INC-20261026-e359a7a32a · unknown

- 指纹：`找不到对应的处理服务:v2/.env`
- 次数：1
- 首次：2026-10-26 07:04:32.043
- 最近：2026-10-26 07:04:32.043

```text
[2026-10-26 07:04:32.043] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - v2/.env处理失败:找不到对应的处理服务:v2/.env
```

