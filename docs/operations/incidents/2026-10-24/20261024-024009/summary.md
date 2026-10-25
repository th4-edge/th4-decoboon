# 生产后端监控摘要

- 采集时间：2026-10-24T02:40:10+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-24/20261024-024009/raw.txt`
- 可处理异常：54
- 已修复历史异常：0
- 预期业务噪音：78

## 服务状态

```text
enabled
active
MainPID=314434
ExecMainStartTimestamp=Thu 2026-10-22 13:15:53 CST
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
/dev/vda3         40901312 23744816  15266128      61% /
/dev/vda3         40901312 23744816  15266128      61% /
/dev/vda3         40901312 23744816  15266128      61% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3490         623          19        3659        3966
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 9 | medium | `当前账号无权执行该操作` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 4 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:actuator` |
| unknown | open | 2 | medium | `找不到对应的处理服务:actuator/env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:docker/config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:download` |
| unknown | open | 1 | medium | `找不到对应的处理服务:gql` |
| unknown | open | 3 | medium | `找不到对应的处理服务:graphql` |
| unknown | open | 1 | medium | `找不到对应的处理服务:json` |
| unknown | open | 3 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mongo` |
| unknown | open | 1 | medium | `找不到对应的处理服务:openapi.json` |
| unknown | open | 1 | medium | `找不到对应的处理服务:phpinfo.php` |
| unknown | open | 1 | medium | `找不到对应的处理服务:redis` |
| unknown | open | 2 | medium | `找不到对应的处理服务:secrets.yml` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:swagger.json` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/download` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/models` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/namespaces/default/secrets` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/namespaces/kube-system/configmaps` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/openapi.json` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 3 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 1 | medium | `找不到对应的实体处理服务[ui.html]` |
| unknown | open | 7 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 73 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该员工本月考勤已存在` |
| unknown | open | 1 | medium | `该工资月份没有生效薪酬方案` |

## 证据

### INC-20261024-979f5a015e · unknown

- 指纹：`当前账号无权执行该操作`
- 次数：9
- 首次：2026-10-22T14:33:35.960+08:00
- 最近：2026-10-23T14:38:37.899+08:00

```text
4304:2026-10-22T14:33:35.960+08:00 ERROR 314434 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : payroll/employee/page处理失败:当前账号无权执行该操作
```

### INC-20261024-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：1
- 首次：2026-10-22T20:16:49.301+08:00
- 最近：2026-10-22T20:16:49.301+08:00

```text
9060:2026-10-22T20:16:49.301+08:00 ERROR 314434 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20261024-1426096679 · unknown

- 指纹：`找不到对应的处理服务:actuator`
- 次数：1
- 首次：2026-10-22T19:09:38.418+08:00
- 最近：2026-10-22T19:09:38.418+08:00

```text
8002:2026-10-22T19:09:38.418+08:00 ERROR 314434 --- [  XNIO-1 task-8] c.t.e.f.w.m.processor.ApiHttpProcessor   : actuator处理失败:找不到对应的处理服务:actuator
```

### INC-20261024-92500a7f2c · unknown

- 指纹：`找不到对应的处理服务:actuator/env`
- 次数：2
- 首次：2026-10-22T19:09:36.158+08:00
- 最近：2026-10-22T19:09:38.408+08:00

```text
7744:2026-10-22T19:09:36.158+08:00 ERROR 314434 --- [  XNIO-1 task-8] c.t.e.f.w.m.processor.ApiHttpProcessor   : actuator/env处理失败:找不到对应的处理服务:actuator/env
```

### INC-20261024-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：1
- 首次：2026-10-22T15:48:23.854+08:00
- 最近：2026-10-22T15:48:23.854+08:00

```text
5299:2026-10-22T15:48:23.854+08:00 ERROR 314434 --- [  XNIO-1 task-5] c.t.e.f.w.m.processor.ApiHttpProcessor   : dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20261024-1dc72d18c6 · unknown

- 指纹：`找不到对应的处理服务:docker/config`
- 次数：1
- 首次：2026-10-22T19:09:37.959+08:00
- 最近：2026-10-22T19:09:37.959+08:00

```text
7830:2026-10-22T19:09:37.959+08:00 ERROR 314434 --- [  XNIO-1 task-8] c.t.e.f.w.m.processor.ApiHttpProcessor   : docker/config处理失败:找不到对应的处理服务:docker/config
```

### INC-20261024-4b58a9edf5 · unknown

- 指纹：`找不到对应的处理服务:download`
- 次数：1
- 首次：2026-10-22T19:09:35.381+08:00
- 最近：2026-10-22T19:09:35.381+08:00

```text
7658:2026-10-22T19:09:35.381+08:00 ERROR 314434 --- [  XNIO-1 task-8] c.t.e.f.w.m.processor.ApiHttpProcessor   : download处理失败:找不到对应的处理服务:download
```

### INC-20261024-972186b589 · unknown

- 指纹：`找不到对应的处理服务:gql`
- 次数：1
- 首次：2026-10-22T19:09:39.389+08:00
- 最近：2026-10-22T19:09:39.389+08:00

```text
8174:2026-10-22T19:09:39.389+08:00 ERROR 314434 --- [  XNIO-1 task-8] c.t.e.f.w.m.processor.ApiHttpProcessor   : gql处理失败:找不到对应的处理服务:gql
```

### INC-20261024-8478b5fd37 · unknown

- 指纹：`找不到对应的处理服务:graphql`
- 次数：3
- 首次：2026-10-22T19:09:28.533+08:00
- 最近：2026-10-22T19:09:40.117+08:00

```text
7314:2026-10-22T19:09:28.533+08:00 ERROR 314434 --- [  XNIO-1 task-8] c.t.e.f.w.m.processor.ApiHttpProcessor   : graphql处理失败:找不到对应的处理服务:graphql
```

### INC-20261024-5b44ec0c8f · unknown

- 指纹：`找不到对应的处理服务:json`
- 次数：1
- 首次：2026-10-22T19:09:46.369+08:00
- 最近：2026-10-22T19:09:46.369+08:00

```text
8888:2026-10-22T19:09:46.369+08:00 ERROR 314434 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : json处理失败:找不到对应的处理服务:json
```

