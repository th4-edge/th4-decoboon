# 生产后端监控摘要

- 采集时间：2026-09-26T02:22:27+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-26/20260926-022226/raw.txt`
- 可处理异常：20
- 已修复历史异常：0
- 预期业务噪音：110

## 服务状态

```text
enabled
active
MainPID=80532
ExecMainStartTimestamp=Thu 2026-09-24 12:07:51 CST
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
/dev/vda3         40901312 7321008  31689936      19% /
/dev/vda3         40901312 7321008  31689936      19% /
/dev/vda3         40901312 7321008  31689936      19% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3416         465         486        4350        4039
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `微信登录失败：code been used, rid: 6ab4a4f1-5d18a70e-1c836727` |
| unknown | open | 2 | medium | `找不到对应的处理服务:` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 4 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 2 | medium | `找不到对应的处理服务:openapi.json` |
| unknown | open | 2 | medium | `接受邀请后 30 天内不能再次加入企业` |
| unknown | open | 6 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 93 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该手机号已绑定其他微信，请联系客服` |
| expected | observed | 15 | info | `首次登录请授权手机号` |

## 证据

### INC-20260926-72da57a9d1 · unknown

- 指纹：`微信登录失败：code been used, rid: 6ab4a4f1-5d18a70e-1c836727`
- 次数：1
- 首次：2026-09-24T12:20:01.330+08:00
- 最近：2026-09-24T12:20:01.330+08:00

```text
3098:2026-09-24T12:20:01.330+08:00 ERROR 80532 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : auth/session/login处理失败:微信登录失败：code been used, rid: 6ab4a4f1-5d18a70e-1c836727
```

### INC-20260926-f7155d2eba · unknown

- 指纹：`找不到对应的处理服务:`
- 次数：2
- 首次：2026-09-26 01:20:47.724
- 最近：2026-09-26T01:20:47.724+08:00

```text
[2026-09-26 01:20:47.724] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - 处理失败:找不到对应的处理服务:
```

### INC-20260926-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：2
- 首次：2026-09-24T14:45:18.598+08:00
- 最近：2026-09-25T02:12:37.564+08:00

```text
5031:2026-09-24T14:45:18.598+08:00 ERROR 80532 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20260926-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：4
- 首次：2026-09-24T13:23:32.616+08:00
- 最近：2026-09-25T23:35:36.156+08:00

```text
4128:2026-09-24T13:23:32.616+08:00 ERROR 80532 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20260926-090b7ceb5e · unknown

- 指纹：`找不到对应的处理服务:openapi.json`
- 次数：2
- 首次：2026-09-26 01:20:48.505
- 最近：2026-09-26T01:20:48.505+08:00

```text
[2026-09-26 01:20:48.505] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - openapi.json处理失败:找不到对应的处理服务:openapi.json
```

### INC-20260926-b0d2dae5eb · unknown

- 指纹：`接受邀请后 30 天内不能再次加入企业`
- 次数：2
- 首次：2026-09-24T12:28:33.876+08:00
- 最近：2026-09-24T12:29:20.064+08:00

```text
3419:2026-09-24T12:28:33.876+08:00 ERROR 80532 --- [  XNIO-1 task-7] c.t.e.f.w.m.processor.ApiHttpProcessor   : invite/invitation/accept处理失败:接受邀请后 30 天内不能再次加入企业
```

### INC-20260926-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：6
- 首次：2026-09-24T14:59:55.785+08:00
- 最近：2026-09-25T18:18:39.849+08:00

```text
5326:2026-09-24T14:59:55.785+08:00 ERROR 80532 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : material/plan/workbench处理失败:访问受限，授权已过期，请重新登录后再访问。
```

### INC-20260926-309d1d47f2 · unknown

- 指纹：`该手机号已绑定其他微信，请联系客服`
- 次数：1
- 首次：2026-09-24T13:47:56.725+08:00
- 最近：2026-09-24T13:47:56.725+08:00

```text
4858:2026-09-24T13:47:56.725+08:00 ERROR 80532 --- [  XNIO-1 task-9] c.t.e.f.w.m.processor.ApiHttpProcessor   : auth/session/login处理失败:该手机号已绑定其他微信，请联系客服
```

