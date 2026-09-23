# 生产后端监控摘要

- 采集时间：2026-09-21T05:11:28+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-21/20260921-051127/raw.txt`
- 可处理异常：9
- 已修复历史异常：0
- 预期业务噪音：9

## 服务状态

```text
enabled
active
MainPID=63746
ExecMainStartTimestamp=Mon 2026-09-21 05:06:10 CST
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
/dev/vda3         40901312 6821056  32189888      18% /
/dev/vda3         40901312 6821056  32189888      18% /
/dev/vda3         40901312 6821056  32189888      18% /
               total        used        free      shared  buff/cache   available
Mem:            7456        2730         951         362        4426        4726
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 2 | medium | `找不到对应的处理服务:document/contract/build` |
| unknown | open | 1 | medium | `找不到对应的处理服务:document/quote/build` |
| unknown | open | 1 | medium | `找不到对应的处理服务:material/selection/page` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| external_dependency | open | 1 | high | `找不到对应的处理服务:plan/payment-template/page` |
| unknown | open | 1 | medium | `找不到对应的处理服务:project/contract-line/page` |
| unknown | open | 1 | medium | `找不到对应的处理服务:project/contract/records` |
| unknown | open | 1 | medium | `找不到对应的处理服务:project/project/overview` |
| expected | observed | 9 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

## 证据

### INC-20260921-f6d5bd8e64 · unknown

- 指纹：`找不到对应的处理服务:document/contract/build`
- 次数：2
- 首次：2026-09-21 04:29:24.550
- 最近：2026-09-21 04:29:43.153

```text
[2026-09-21 04:29:24.550] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - document/contract/build处理失败:找不到对应的处理服务:document/contract/build
```

### INC-20260921-4a567b43df · unknown

- 指纹：`找不到对应的处理服务:document/quote/build`
- 次数：1
- 首次：2026-09-21 04:29:43.241
- 最近：2026-09-21 04:29:43.241

```text
[2026-09-21 04:29:43.241] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - document/quote/build处理失败:找不到对应的处理服务:document/quote/build
```

### INC-20260921-a97df7ac52 · unknown

- 指纹：`找不到对应的处理服务:material/selection/page`
- 次数：1
- 首次：2026-09-21 04:29:42.974
- 最近：2026-09-21 04:29:42.974

```text
[2026-09-21 04:29:42.974] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - material/selection/page处理失败:找不到对应的处理服务:material/selection/page
```

### INC-20260921-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：1
- 首次：2026-09-21 00:50:41.784
- 最近：2026-09-21 00:50:41.784

```text
[2026-09-21 00:50:41.784] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20260921-4d26bd91f8 · external_dependency

- 指纹：`找不到对应的处理服务:plan/payment-template/page`
- 次数：1
- 首次：2026-09-21 04:29:43.057
- 最近：2026-09-21 04:29:43.057

```text
[2026-09-21 04:29:43.057] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - plan/payment-template/page处理失败:找不到对应的处理服务:plan/payment-template/page
```

### INC-20260921-cc330a02ba · unknown

- 指纹：`找不到对应的处理服务:project/contract-line/page`
- 次数：1
- 首次：2026-09-21 04:29:42.887
- 最近：2026-09-21 04:29:42.887

```text
[2026-09-21 04:29:42.887] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/contract-line/page处理失败:找不到对应的处理服务:project/contract-line/page
```

### INC-20260921-37aad1816b · unknown

- 指纹：`找不到对应的处理服务:project/contract/records`
- 次数：1
- 首次：2026-09-21 04:29:42.733
- 最近：2026-09-21 04:29:42.733

```text
[2026-09-21 04:29:42.733] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/contract/records处理失败:找不到对应的处理服务:project/contract/records
```

### INC-20260921-92056cdfef · unknown

- 指纹：`找不到对应的处理服务:project/project/overview`
- 次数：1
- 首次：2026-09-21 04:29:42.663
- 最近：2026-09-21 04:29:42.663

```text
[2026-09-21 04:29:42.663] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/project/overview处理失败:找不到对应的处理服务:project/project/overview
```

