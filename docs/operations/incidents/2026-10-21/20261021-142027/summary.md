# 生产后端监控摘要

- 采集时间：2026-10-21T14:20:28+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-21/20261021-142027/raw.txt`
- 可处理异常：63
- 已修复历史异常：9
- 预期业务噪音：92

## 服务状态

```text
enabled
active
MainPID=305676
ExecMainStartTimestamp=Wed 2026-10-21 14:16:21 CST
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
/dev/vda3         40901312 22876092  16134852      59% /
/dev/vda3         40901312 22876092  16134852      59% /
/dev/vda3         40901312 22876092  16134852      59% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3658         418         400        4077        3797
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 2 | medium | `不能操作自己的员工记录` |
| unknown | open | 16 | medium | `当前账号无权执行该操作` |
| unknown | fixed | 9 | medium | `当前账号无权维护报价项目库` |
| expected | observed | 1 | info | `手机号或密码错误` |
| unknown | open | 1 | medium | `找不到对应的处理服务:` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:actuator` |
| unknown | open | 4 | medium | `找不到对应的处理服务:actuator/env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:docker/config` |
| unknown | open | 2 | medium | `找不到对应的处理服务:download` |
| unknown | open | 2 | medium | `找不到对应的处理服务:gql` |
| unknown | open | 6 | medium | `找不到对应的处理服务:graphql` |
| unknown | open | 2 | medium | `找不到对应的处理服务:json` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mongo` |
| unknown | open | 2 | medium | `找不到对应的处理服务:openapi.json` |
| unknown | open | 2 | medium | `找不到对应的处理服务:phpinfo.php` |
| unknown | open | 2 | medium | `找不到对应的处理服务:redis` |
| unknown | open | 2 | medium | `找不到对应的处理服务:swagger.json` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/download` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/models` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/namespaces/default/secrets` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/namespaces/kube-system/configmaps` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/openapi.json` |
| unknown | open | 2 | medium | `找不到对应的实体处理服务[ui.html]` |
| expected | observed | 85 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| expected | observed | 4 | info | `首次登录请授权手机号` |

## 证据

### INC-20261021-e3deff3d62 · unknown

- 指纹：`不能操作自己的员工记录`
- 次数：2
- 首次：2026-10-21 11:36:37.602
- 最近：2026-10-21 11:36:37.604

```text
[2026-10-21 11:36:37.602] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - company/member/info处理失败:不能操作自己的员工记录
```

### INC-20261021-979f5a015e · unknown

- 指纹：`当前账号无权执行该操作`
- 次数：16
- 首次：2026-10-21 10:29:27.765
- 最近：2026-10-21 14:13:20.403

```text
[2026-10-21 10:29:27.765] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - user/employee/page处理失败:当前账号无权执行该操作
```

### INC-20261021-f7155d2eba · unknown

- 指纹：`找不到对应的处理服务:`
- 次数：1
- 首次：2026-10-21 02:25:33.150
- 最近：2026-10-21 02:25:33.150

```text
[2026-10-21 02:25:33.150] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - 处理失败:找不到对应的处理服务:
```

### INC-20261021-1426096679 · unknown

- 指纹：`找不到对应的处理服务:actuator`
- 次数：2
- 首次：2026-10-21 04:18:47.114
- 最近：2026-10-21 12:47:31.286

```text
[2026-10-21 04:18:47.114] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - actuator处理失败:找不到对应的处理服务:actuator
```

### INC-20261021-92500a7f2c · unknown

- 指纹：`找不到对应的处理服务:actuator/env`
- 次数：4
- 首次：2026-10-21 04:18:44.909
- 最近：2026-10-21 12:47:31.291

```text
[2026-10-21 04:18:44.909] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - actuator/env处理失败:找不到对应的处理服务:actuator/env
```

### INC-20261021-1dc72d18c6 · unknown

- 指纹：`找不到对应的处理服务:docker/config`
- 次数：2
- 首次：2026-10-21 04:18:46.706
- 最近：2026-10-21 12:47:30.905

```text
[2026-10-21 04:18:46.706] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - docker/config处理失败:找不到对应的处理服务:docker/config
```

### INC-20261021-4b58a9edf5 · unknown

- 指纹：`找不到对应的处理服务:download`
- 次数：2
- 首次：2026-10-21 04:18:44.461
- 最近：2026-10-21 12:47:28.141

```text
[2026-10-21 04:18:44.461] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - download处理失败:找不到对应的处理服务:download
```

### INC-20261021-972186b589 · unknown

- 指纹：`找不到对应的处理服务:gql`
- 次数：2
- 首次：2026-10-21 04:18:47.828
- 最近：2026-10-21 12:47:32.245

```text
[2026-10-21 04:18:47.828] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - gql处理失败:找不到对应的处理服务:gql
```

### INC-20261021-8478b5fd37 · unknown

- 指纹：`找不到对应的处理服务:graphql`
- 次数：6
- 首次：2026-10-21 04:18:38.421
- 最近：2026-10-21 12:47:32.916

```text
[2026-10-21 04:18:38.421] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - graphql处理失败:找不到对应的处理服务:graphql
```

### INC-20261021-5b44ec0c8f · unknown

- 指纹：`找不到对应的处理服务:json`
- 次数：2
- 首次：2026-10-21 04:18:53.423
- 最近：2026-10-21 12:47:40.313

```text
[2026-10-21 04:18:53.423] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - json处理失败:找不到对应的处理服务:json
```

