# 生产后端监控摘要

- 采集时间：2026-10-22T13:21:53+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-22/20261022-132152/raw.txt`
- 可处理异常：32
- 已修复历史异常：0
- 预期业务噪音：69

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
/dev/vda3         40901312 23642992  15367952      61% /
/dev/vda3         40901312 23642992  15367952      61% /
/dev/vda3         40901312 23642992  15367952      61% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3308         817         274        3909        4147
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `null` |
| expected | observed | 2 | info | `手机号或密码错误` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/COMMIT_EDITMSG` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/HEAD` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/config.worktree` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/info/refs` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/logs/HEAD` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/packed-refs` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/refs/heads/main` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/refs/heads/master` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/refs/remotes/origin/HEAD` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 3 | medium | `找不到对应的处理服务:project/owner-preview/info` |
| unknown | open | 1 | medium | `找不到对应的处理服务:repos` |
| unknown | open | 2 | medium | `找不到对应的处理服务:session/properties` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/repos/search` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/version` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/repos` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v4/projects` |
| unknown | open | 2 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 65 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 2 | medium | `项目不存在或无权访问` |

## 证据

### INC-20261022-2be88ca424 · unknown

- 指纹：`null`
- 次数：1
- 首次：2026-10-22 00:16:30.946
- 最近：2026-10-22 00:16:30.946

```text
[2026-10-22 00:16:30.946] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:null
```

### INC-20261022-a31c75c8ba · unknown

- 指纹：`找不到对应的处理服务:.git/COMMIT_EDITMSG`
- 次数：1
- 首次：2026-10-22 02:53:00.422
- 最近：2026-10-22 02:53:00.422

```text
[2026-10-22 02:53:00.422] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/COMMIT_EDITMSG处理失败:找不到对应的处理服务:.git/COMMIT_EDITMSG
```

### INC-20261022-c538b15f5f · unknown

- 指纹：`找不到对应的处理服务:.git/HEAD`
- 次数：1
- 首次：2026-10-22 02:53:00.365
- 最近：2026-10-22 02:53:00.365

```text
[2026-10-22 02:53:00.365] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/HEAD处理失败:找不到对应的处理服务:.git/HEAD
```

### INC-20261022-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：1
- 首次：2026-10-22 02:53:00.360
- 最近：2026-10-22 02:53:00.360

```text
[2026-10-22 02:53:00.360] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20261022-deb7b43dcb · unknown

- 指纹：`找不到对应的处理服务:.git/config.worktree`
- 次数：1
- 首次：2026-10-22 02:53:00.448
- 最近：2026-10-22 02:53:00.448

```text
[2026-10-22 02:53:00.448] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/config.worktree处理失败:找不到对应的处理服务:.git/config.worktree
```

### INC-20261022-47ccbc0895 · unknown

- 指纹：`找不到对应的处理服务:.git/info/refs`
- 次数：1
- 首次：2026-10-22 02:53:00.284
- 最近：2026-10-22 02:53:00.284

```text
[2026-10-22 02:53:00.284] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/info/refs处理失败:找不到对应的处理服务:.git/info/refs
```

### INC-20261022-4ee9807ecf · unknown

- 指纹：`找不到对应的处理服务:.git/logs/HEAD`
- 次数：1
- 首次：2026-10-22 02:53:00.297
- 最近：2026-10-22 02:53:00.297

```text
[2026-10-22 02:53:00.297] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/logs/HEAD处理失败:找不到对应的处理服务:.git/logs/HEAD
```

### INC-20261022-5e40d63794 · unknown

- 指纹：`找不到对应的处理服务:.git/packed-refs`
- 次数：1
- 首次：2026-10-22 02:53:00.418
- 最近：2026-10-22 02:53:00.418

```text
[2026-10-22 02:53:00.418] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/packed-refs处理失败:找不到对应的处理服务:.git/packed-refs
```

### INC-20261022-bb788f0da0 · unknown

- 指纹：`找不到对应的处理服务:.git/refs/heads/main`
- 次数：1
- 首次：2026-10-22 02:53:00.440
- 最近：2026-10-22 02:53:00.440

```text
[2026-10-22 02:53:00.440] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/refs/heads/main处理失败:找不到对应的处理服务:.git/refs/heads/main
```

### INC-20261022-e91f753ce7 · unknown

- 指纹：`找不到对应的处理服务:.git/refs/heads/master`
- 次数：1
- 首次：2026-10-22 02:53:00.376
- 最近：2026-10-22 02:53:00.376

```text
[2026-10-22 02:53:00.376] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/refs/heads/master处理失败:找不到对应的处理服务:.git/refs/heads/master
```

