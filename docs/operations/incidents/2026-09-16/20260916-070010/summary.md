# 生产后端监控摘要

- 采集时间：2026-09-16T07:00:11+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-16/20260916-070010/raw.txt`
- 可处理异常：1
- 已修复历史异常：22
- 预期业务噪音：34

## 服务状态

```text
enabled
active
MainPID=33085
ExecMainStartTimestamp=Wed 2026-09-16 06:59:33 CST
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
/dev/vda3         40901312 5729276  33281668      15% /
/dev/vda3         40901312 5729276  33281668      15% /
/dev/vda3         40901312 5729276  33281668      15% /
               total        used        free      shared  buff/cache   available
Mem:            7456        1897        2885         124        3068        5558
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| code_bug | fixed | 22 | critical | `LazyInitializationException: Could not initialize proxy - no session` |
| expected | observed | 1 | info | `当前用户已有在职企业` |
| expected | observed | 1 | info | `微信支付回调原始报文不存在` |
| expected | observed | 1 | info | `微信支付回调签名缺失` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config` |
| expected | observed | 28 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 1 | info | `首次登录请授权手机号` |

## 证据

### INC-20260916-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：1
- 首次：2026-09-16 06:33:46.274
- 最近：2026-09-16 06:33:46.274

```text
[2026-09-16 06:33:46.274] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - config处理失败:找不到对应的处理服务:config
```

