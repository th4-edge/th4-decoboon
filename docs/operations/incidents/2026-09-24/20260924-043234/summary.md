# 生产后端监控摘要

- 采集时间：2026-09-24T04:32:35+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-24/20260924-043234/raw.txt`
- 可处理异常：71
- 已修复历史异常：0
- 预期业务噪音：67

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
/dev/vda3         40901312 6879112  32131832      18% /
/dev/vda3         40901312 6879112  32131832      18% /
/dev/vda3         40901312 6879112  32131832      18% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3099         525         362        4484        4357
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 9 | medium | `不能操作自己的员工记录` |
| unknown | open | 11 | medium | `企业唯一负责人不能退出，请先转让负责人` |
| expected | observed | 7 | info | `找不到对应的处理服务:.env` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env.local` |
| unknown | open | 2 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 12 | medium | `找不到对应的处理服务:auth/signin` |
| unknown | open | 3 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 4 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 3 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 3 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 3 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 3 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 8 | medium | `接受邀请后 30 天内不能再次加入企业` |
| unknown | open | 3 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 50 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该手机号已有待接受邀请` |
| unknown | open | 4 | medium | `邀请不存在或不可用` |
| expected | observed | 8 | info | `首次登录请授权手机号` |

## 证据

### INC-20260924-e3deff3d62 · unknown

- 指纹：`不能操作自己的员工记录`
- 次数：9
- 首次：2026-09-22T16:13:08.704+08:00
- 最近：2026-09-22T16:19:11.447+08:00

```text
7259:2026-09-22T16:13:08.704+08:00 ERROR 63746 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : company/member/resign处理失败:不能操作自己的员工记录
```

### INC-20260924-be3b1b0eae · unknown

- 指纹：`企业唯一负责人不能退出，请先转让负责人`
- 次数：11
- 首次：2026-09-22T16:13:26.543+08:00
- 最近：2026-09-22T16:19:06.499+08:00

```text
7497:2026-09-22T16:13:26.543+08:00 ERROR 63746 --- [  XNIO-1 task-2] c.t.e.f.w.m.processor.ApiHttpProcessor   : user/user/resign处理失败:企业唯一负责人不能退出，请先转让负责人
```

### INC-20260924-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：2
- 首次：2026-09-22T06:26:40.863+08:00
- 最近：2026-09-23T12:42:45.454+08:00

```text
5282:2026-09-22T06:26:40.863+08:00 ERROR 63746 --- [  XNIO-1 task-7] c.t.e.f.w.m.processor.ApiHttpProcessor   : .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20260924-0cd74dd730 · unknown

- 指纹：`找不到对应的处理服务:auth/signin`
- 次数：12
- 首次：2026-09-21T14:04:41.584+08:00
- 最近：2026-09-21T14:04:48.401+08:00

```text
1615:2026-09-21T14:04:41.584+08:00 ERROR 63746 --- [  XNIO-1 task-6] c.t.e.f.w.m.processor.ApiHttpProcessor   : auth/signin处理失败:找不到对应的处理服务:auth/signin
```

### INC-20260924-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：3
- 首次：2026-09-21T21:34:33.113+08:00
- 最近：2026-09-22T06:40:38.274+08:00

```text
4095:2026-09-21T21:34:33.113+08:00 ERROR 63746 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20260924-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：4
- 首次：2026-09-22T08:07:43.677+08:00
- 最近：2026-09-23T19:11:40.127+08:00

```text
5970:2026-09-22T08:07:43.677+08:00 ERROR 63746 --- [  XNIO-1 task-7] c.t.e.f.w.m.processor.ApiHttpProcessor   : mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20260924-e4bbca461d · unknown

- 指纹：`找不到对应的处理服务:staging/.env`
- 次数：3
- 首次：2026-09-21T21:34:33.397+08:00
- 最近：2026-09-22T06:40:38.525+08:00

```text
4181:2026-09-21T21:34:33.397+08:00 ERROR 63746 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : staging/.env处理失败:找不到对应的处理服务:staging/.env
```

### INC-20260924-416878e316 · unknown

- 指纹：`找不到对应的处理服务:v1/.env`
- 次数：3
- 首次：2026-09-21T21:34:30.882+08:00
- 最近：2026-09-22T06:40:36.298+08:00

```text
3837:2026-09-21T21:34:30.882+08:00 ERROR 63746 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : v1/.env处理失败:找不到对应的处理服务:v1/.env
```

### INC-20260924-e359a7a32a · unknown

- 指纹：`找不到对应的处理服务:v2/.env`
- 次数：3
- 首次：2026-09-21T21:34:31.167+08:00
- 最近：2026-09-22T06:40:36.549+08:00

```text
3923:2026-09-21T21:34:31.167+08:00 ERROR 63746 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : v2/.env处理失败:找不到对应的处理服务:v2/.env
```

### INC-20260924-7cc2b644b1 · unknown

- 指纹：`找不到对应的处理服务:v3/.env`
- 次数：3
- 首次：2026-09-21T21:34:32.830+08:00
- 最近：2026-09-22T06:40:38.024+08:00

```text
4009:2026-09-21T21:34:32.830+08:00 ERROR 63746 --- [  XNIO-1 task-4] c.t.e.f.w.m.processor.ApiHttpProcessor   : v3/.env处理失败:找不到对应的处理服务:v3/.env
```

