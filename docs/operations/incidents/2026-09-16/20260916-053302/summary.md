# 生产后端监控摘要

- 采集时间：2026-09-16T05:33:41+08:00
- 原始快照：`docs/operations/incidents/2026-09-16/20260916-053302/raw.txt`
- 可处理异常：0
- 已修复历史异常：2
- 预期业务噪音：22

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
/dev/vda3         40901312 5460172  33550772      14% /
/dev/vda3         40901312 5460172  33550772      14% /
/dev/vda3         40901312 5460172  33550772      14% /
               total        used        free      shared  buff/cache   available
Mem:            7456        1850        3192         124        2804        5605
Swap:              0           0           0
```

## 异常分组

| 分类 | 次数 | 严重度 | 指纹 |
| --- | ---: | --- | --- |
| code_bug | 2 | critical | `LazyInitializationException: Could not initialize proxy - no session` |
| expected | 1 | info | `当前用户已有在职企业` |
| expected | 1 | info | `微信支付回调原始报文不存在` |
| expected | 1 | info | `微信支付回调签名缺失` |
| expected | 1 | info | `手机号或密码错误` |
| expected | 1 | info | `找不到对应的处理服务:.env` |
| expected | 16 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | 1 | info | `首次登录请授权手机号` |
