# 生产后端监控摘要

- 采集时间：2026-10-06T02:09:37+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-06/20261006-020937/raw.txt`
- 可处理异常：1
- 已修复历史异常：0
- 预期业务噪音：16

## 服务状态

```text
enabled
active
MainPID=149475
ExecMainStartTimestamp=Tue 2026-10-06 02:08:49 CST
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
/dev/vda3         40901312 10137616  28873328      26% /
/dev/vda3         40901312 10137616  28873328      26% /
/dev/vda3         40901312 10137616  28873328      26% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3258         563         265        4198        4198
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `只有客户已确认的报价可以生成合同` |
| expected | observed | 16 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20261006-0e18c4a6b0 · unknown

- 指纹：`只有客户已确认的报价可以生成合同`
- 次数：1
- 首次：2026-10-06 00:53:33.913
- 最近：2026-10-06 00:53:33.913

```text
[2026-10-06 00:53:33.913] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/contract/save处理失败:只有客户已确认的报价可以生成合同
```

