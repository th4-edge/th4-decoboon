# 生产后端监控摘要

- 采集时间：2026-11-02T03:06:23+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon-hotfix-20261102/docs/operations/incidents/2026-11-02/20261102-030622/raw.txt`
- 可处理异常：0
- 已修复历史异常：0
- 预期业务噪音：4

## 服务状态

```text
enabled
active
MainPID=384891
ExecMainStartTimestamp=Mon 2026-11-02 03:05:28 CST
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
/dev/vda3         40901312 26234628  12776316      68% /
/dev/vda3         40901312 26234628  12776316      68% /
/dev/vda3         40901312 26234628  12776316      68% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3407        1317         451        3479        4048
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| expected | observed | 4 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
