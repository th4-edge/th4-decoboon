# 生产后端监控摘要

- 采集时间：2026-09-16T05:29:44+08:00
- 原始快照：`docs/operations/incidents/2026-09-16/20260916-052935/raw.txt`
- 可处理异常：4
- 预期业务噪音：34

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
{"success":false,"errorCode":{"code":"tokenIsUndefined","message":"访问受限，缺少授权标识，请先登录后再访问。","system":false}}
```

## 资源状态

```text
Filesystem     1024-blocks    Used Available Capacity Mounted on
/dev/vda3         40901312 5200144  33810800      14% /
/dev/vda3         40901312 5200144  33810800      14% /
/dev/vda3         40901312 5200144  33810800      14% /
               total        used        free      shared  buff/cache   available
Mem:            7456        1821        3480          10        2427        5634
Swap:              0           0           0
```

## 异常分组

| 分类 | 次数 | 严重度 | 指纹 |
| --- | ---: | --- | --- |
| code_bug | 4 | critical | `LazyInitializationException: Could not initialize proxy - no session` |
| expected | 2 | info | `当前用户已有在职企业` |
| expected | 2 | info | `微信支付回调原始报文不存在` |
| expected | 2 | info | `微信支付回调签名缺失` |
| expected | 2 | info | `手机号或密码错误` |
| expected | 2 | info | `找不到对应的处理服务:.env` |
| expected | 22 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | 2 | info | `首次登录请授权手机号` |

## 证据

### INC-20260916-a1233597b0 · code_bug

- 指纹：`LazyInitializationException: Could not initialize proxy - no session`
- 次数：4
- 首次：2026-09-16 05:14:57.806
- 最近：2026-09-16T05:15:18.762+08:00

```text
[2026-09-16 05:14:57.806] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:Could not initialize proxy [com.th4.edge.tenant.user.model.Employee#1789506897394275742] - no session
```

