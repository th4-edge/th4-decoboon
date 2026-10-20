# 生产后端监控摘要

- 采集时间：2026-10-20T20:20:57+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-20/20261020-202056/raw.txt`
- 可处理异常：17
- 已修复历史异常：46
- 预期业务噪音：92

## 服务状态

```text
enabled
active
MainPID=300287
ExecMainStartTimestamp=Tue 2026-10-20 20:19:18 CST
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
/dev/vda3         40901312 22279348  16731596      58% /
/dev/vda3         40901312 22279348  16731596      58% /
/dev/vda3         40901312 22279348  16731596      58% /
               total        used        free      shared  buff/cache   available
Mem:            7456        4167         194         524        3916        3288
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 6 | medium | `当前账号无权执行该操作` |
| expected | observed | 5 | info | `手机号或密码错误` |
| unknown | fixed | 46 | medium | `找不到对应的处理服务:design/items/reorder` |
| unknown | open | 3 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 7 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 86 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该房屋已有客户确认报价或唯一项目，设计方案已锁定；调整请发起合同变更单` |
| expected | observed | 1 | info | `首次登录请授权手机号` |

## 证据

### INC-20261020-979f5a015e · unknown

- 指纹：`当前账号无权执行该操作`
- 次数：6
- 首次：2026-10-20 17:27:19.415
- 最近：2026-10-20 17:44:33.067

```text
[2026-10-20 17:27:19.415] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - user/employee/page处理失败:当前账号无权执行该操作
```

### INC-20261020-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：3
- 首次：2026-10-20 01:46:12.748
- 最近：2026-10-20 12:00:30.120

```text
[2026-10-20 01:46:12.748] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261020-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：7
- 首次：2026-10-20 09:23:59.668
- 最近：2026-10-20 18:57:52.665

```text
[2026-10-20 09:23:59.668] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:访问受限，授权已过期，请重新登录后再访问。
```

### INC-20261020-0c8f095267 · unknown

- 指纹：`该房屋已有客户确认报价或唯一项目，设计方案已锁定；调整请发起合同变更单`
- 次数：1
- 首次：2026-10-20 18:08:45.094
- 最近：2026-10-20 18:08:45.094

```text
[2026-10-20 18:08:45.094] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - design/talk/build处理失败:该房屋已有客户确认报价或唯一项目，设计方案已锁定；调整请发起合同变更单
```

