# 生产后端监控摘要

- 采集时间：2026-09-16T05:24:41+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-16/20260916-052441/raw.txt`
- 可处理异常：4
- 预期业务噪音：32

## 服务状态

```text
enabled
active
MainPID=31014
ExecMainStartTimestamp=Wed 2026-09-16 04:15:11 CST
ActiveState=active
SubState=running
```

## 健康检查

```text
health_http=200
{"success":false,"errorCode":{"code":"tokenIsUndefined","message":"访问受限，缺少授权标识，请先登录后再访问。","system":false}}== RESOURCES ==
Filesystem     1024-blocks    Used Available Capacity Mounted on
/dev/vda3         40901312 5200096  33810848      14% /
/dev/vda3         40901312 5200096  33810848      14% /
/dev/vda3         40901312 5200096  33810848      14% /
               total        used        free      shared  buff/cache   available
Mem:            7456        1841        3460          10        2427        5615
Swap:              0           0           0
```

## 资源状态

```text

```

## 异常分组

| 分类 | 次数 | 严重度 | 指纹 |
| --- | ---: | --- | --- |
| code_bug | 2 | critical | `auth/session/info处理失败:Could not initialize proxy [com.th4.edge.tenant.user.model.Employee#<hex>] - no session` |
| expected | 2 | info | `.env处理失败:找不到对应的处理服务:.env` |
| expected | 12 | info | `auth/session/info处理失败:访问受限，缺少授权标识，请先登录后再访问。` |
| expected | 2 | info | `auth/session/login处理失败:首次登录请授权手机号` |
| expected | 2 | info | `auth/session/password-login处理失败:手机号或密码错误` |
| expected | 2 | info | `auth/session/register处理失败:当前用户已有在职企业` |
| expected | 2 | info | `collab/notice/count处理失败:访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | 2 | medium | `no session` |
| expected | 2 | info | `order/callback处理失败:微信支付回调原始报文不存在` |
| expected | 2 | info | `order/callback处理失败:微信支付回调签名缺失` |
| expected | 2 | info | `project/project/page处理失败:访问受限，缺少授权标识，请先登录后再访问。` |
| expected | 2 | info | `statistic/alert/report处理失败:访问受限，缺少授权标识，请先登录后再访问。` |
| expected | 2 | info | `statistic/boss/dashboard处理失败:访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20260916-22c1108268 · code_bug

- 指纹：`auth/session/info处理失败:Could not initialize proxy [com.th4.edge.tenant.user.model.Employee#<hex>] - no session`
- 次数：2
- 首次：2026-09-16 05:14:57.806
- 最近：2026-09-16 05:15:18.762

```text
[2026-09-16 05:14:57.806] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:Could not initialize proxy [com.th4.edge.tenant.user.model.Employee#1789506897394275742] - no session
```

### INC-20260916-e3f4febf79 · unknown

- 指纹：`no session`
- 次数：2
- 首次：2026-09-16T05:14:57.806+08:00
- 最近：2026-09-16T05:15:18.762+08:00

```text
1752:2026-09-16T05:14:57.806+08:00 ERROR 31014 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : auth/session/info处理失败:Could not initialize proxy [com.th4.edge.tenant.user.model.Employee#1789506897394275742] - no session
```

