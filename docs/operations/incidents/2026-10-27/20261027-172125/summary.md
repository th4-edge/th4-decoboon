# 生产后端监控摘要

- 采集时间：2026-10-27T17:21:27+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon-ready-edit-hotfix/docs/operations/incidents/2026-10-27/20261027-172125/raw.txt`
- 可处理异常：62
- 已修复历史异常：0
- 预期业务噪音：70

## 服务状态

```text
enabled
active
MainPID=350411
ExecMainStartTimestamp=Tue 2026-10-27 17:20:22 CST
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
/dev/vda3         40901312 25350972  13659972      65% /
/dev/vda3         40901312 25350972  13659972      65% /
/dev/vda3         40901312 25350972  13659972      65% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3325         726         330        4031        4130
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| code_bug | open | 18 | critical | `Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null` |
| unknown | open | 8 | medium | `Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials` |
| unknown | open | 6 | medium | `当前账号尚未加入企业，请先接受企业邀请` |
| expected | observed | 1 | info | `手机号或密码错误` |
| unknown | open | 1 | medium | `找不到对应的处理服务:` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env.production` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env.staging` |
| unknown | open | 1 | medium | `找不到对应的处理服务:_profiler/phpinfo` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dashboard/phpinfo.php` |
| unknown | open | 1 | medium | `找不到对应的处理服务:debug` |
| unknown | open | 1 | medium | `找不到对应的处理服务:docs` |
| unknown | open | 1 | medium | `找不到对应的处理服务:docs/` |
| unknown | open | 1 | medium | `找不到对应的处理服务:info.php` |
| unknown | open | 1 | medium | `找不到对应的处理服务:infophp.php` |
| unknown | open | 1 | medium | `找不到对应的处理服务:infos.php` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:php.ini` |
| unknown | open | 1 | medium | `找不到对应的处理服务:php.php` |
| unknown | open | 1 | medium | `找不到对应的处理服务:phpinfo` |
| unknown | open | 1 | medium | `找不到对应的处理服务:phpinfo.php` |
| unknown | open | 2 | medium | `找不到对应的处理服务:src/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:test.php` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/status/config` |
| unknown | open | 1 | medium | `文件不存在或无权访问` |
| unknown | open | 1 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 67 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 3 | medium | `该手机号 7 天内邀请次数已达上限` |
| unknown | open | 1 | medium | `该阶段无合同施工任务，不能提交验收，请先核对报价与合同施工行` |
| unknown | open | 2 | medium | `邀请已过期` |
| unknown | open | 2 | medium | `量房完成时间不能早于预约时间` |

## 证据

### INC-20261027-b63f1ec203 · code_bug

- 指纹：`Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null`
- 次数：18
- 首次：2026-10-27 15:38:31.126
- 最近：2026-10-27 15:44:25.586

```text
[2026-10-27 15:38:31.126] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/cancel处理失败:Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null
```

### INC-20261027-9c36075e08 · unknown

- 指纹：`Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials`
- 次数：8
- 首次：2026-10-27 00:07:05.612
- 最近：2026-10-27 15:39:40.759

```text
[2026-10-27 00:07:05.612] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/page处理失败:Can not set com.th4.edge.tenant.user.model.Employee field com.th4.edge.tenant.user.service.invite.invitation.page.support.Page.employee to com.th4.edge.tenant.user.model.Credentials
```

### INC-20261027-ed07b1c949 · unknown

- 指纹：`当前账号尚未加入企业，请先接受企业邀请`
- 次数：6
- 首次：2026-10-27 17:02:31.084
- 最近：2026-10-27 17:05:05.288

```text
[2026-10-27 17:02:31.084] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/page处理失败:当前账号尚未加入企业，请先接受企业邀请
```

### INC-20261027-f7155d2eba · unknown

- 指纹：`找不到对应的处理服务:`
- 次数：1
- 首次：2026-10-27 16:56:38.041
- 最近：2026-10-27 16:56:38.041

```text
[2026-10-27 16:56:38.041] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - 处理失败:找不到对应的处理服务:
```

### INC-20261027-2efcb5f30b · unknown

- 指纹：`找不到对应的处理服务:_profiler/phpinfo`
- 次数：1
- 首次：2026-10-27 16:56:35.082
- 最近：2026-10-27 16:56:35.082

```text
[2026-10-27 16:56:35.082] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - _profiler/phpinfo处理失败:找不到对应的处理服务:_profiler/phpinfo
```

### INC-20261027-cddd8368fa · unknown

- 指纹：`找不到对应的处理服务:config`
- 次数：1
- 首次：2026-10-27 16:56:38.038
- 最近：2026-10-27 16:56:38.038

```text
[2026-10-27 16:56:38.038] [ERROR] [XNIO-1 task-7] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - config处理失败:找不到对应的处理服务:config
```

### INC-20261027-8630781d19 · unknown

- 指纹：`找不到对应的处理服务:dashboard/phpinfo.php`
- 次数：1
- 首次：2026-10-27 16:56:35.987
- 最近：2026-10-27 16:56:35.987

```text
[2026-10-27 16:56:35.987] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dashboard/phpinfo.php处理失败:找不到对应的处理服务:dashboard/phpinfo.php
```

### INC-20261027-f7eec97ede · unknown

- 指纹：`找不到对应的处理服务:debug`
- 次数：1
- 首次：2026-10-27 16:56:38.037
- 最近：2026-10-27 16:56:38.037

```text
[2026-10-27 16:56:38.037] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - debug处理失败:找不到对应的处理服务:debug
```

### INC-20261027-ab71fe82bb · unknown

- 指纹：`找不到对应的处理服务:docs`
- 次数：1
- 首次：2026-10-27 16:56:38.051
- 最近：2026-10-27 16:56:38.051

```text
[2026-10-27 16:56:38.051] [ERROR] [XNIO-1 task-9] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - docs处理失败:找不到对应的处理服务:docs
```

### INC-20261027-0a497c812e · unknown

- 指纹：`找不到对应的处理服务:docs/`
- 次数：1
- 首次：2026-10-27 16:56:38.040
- 最近：2026-10-27 16:56:38.040

```text
[2026-10-27 16:56:38.040] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - docs/处理失败:找不到对应的处理服务:docs/
```

