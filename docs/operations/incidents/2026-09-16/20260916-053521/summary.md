# 生产后端监控摘要

- 采集时间：2026-09-16T05:35:29+08:00
- 原始快照：`docs/operations/incidents/2026-09-16/20260916-053521/raw.txt`
- 可处理异常：0
- 已修复历史异常：2
- 预期业务噪音：26

## 服务状态

```text
enabled
active
MainPID=31996
ExecMainStartTimestamp=Wed 2026-09-16 05:32:22 CST
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
/dev/vda3         40901312 5460224  33550720      14% /
/dev/vda3         40901312 5460224  33550720      14% /
/dev/vda3         40901312 5460224  33550720      14% /
               total        used        free      shared  buff/cache   available
Mem:            7456        1889        3153         124        2804        5566
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| code_bug | fixed | 2 | critical | `LazyInitializationException: Could not initialize proxy - no session` |
| expected | observed | 1 | info | `当前用户已有在职企业` |
| expected | observed | 1 | info | `微信支付回调原始报文不存在` |
| expected | observed | 1 | info | `微信支付回调签名缺失` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env` |
| expected | observed | 20 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 1 | info | `首次登录请授权手机号` |
