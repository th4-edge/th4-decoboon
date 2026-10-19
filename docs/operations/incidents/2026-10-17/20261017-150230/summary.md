# 生产后端监控摘要

- 采集时间：2026-10-17T15:02:31+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-17/20261017-150230/raw.txt`
- 可处理异常：35
- 已修复历史异常：1
- 预期业务噪音：100

## 服务状态

```text
enabled
active
MainPID=281990
ExecMainStartTimestamp=Sat 2026-10-17 15:00:54 CST
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
/dev/vda3         40901312 21738236  17272708      56% /
/dev/vda3         40901312 21738236  17272708      56% /
/dev/vda3         40901312 21738236  17272708      56% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3825         598         650        3984        3630
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| code_bug | open | 1 | critical | `Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null` |
| unknown | open | 1 | medium | `Failed to convert from type [java.lang.String] to type [java.lang.Long] for value [x]` |
| unknown | fixed | 1 | medium | `customerId不能为空` |
| unknown | open | 1 | medium | `null` |
| unknown | open | 3 | medium | `作业单不存在或无权访问` |
| unknown | open | 1 | medium | `作业单行必须来源材料计划；合同外主材请先发起增减项` |
| unknown | open | 16 | medium | `分项已生成报价快照，归档报价后才能删除` |
| unknown | open | 2 | medium | `当前账号无权执行该操作` |
| expected | observed | 10 | info | `手机号或密码错误` |
| unknown | open | 2 | medium | `找不到对应的处理服务:` |
| unknown | open | 2 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 3 | medium | `提交请购前请先完成：测量时间` |
| expected | observed | 90 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该房屋已指定设计师，量房完成必须由指定设计师执行` |
| unknown | open | 1 | medium | `采购单当前状态不能审批` |

## 证据

### INC-20261017-b63f1ec203 · code_bug

- 指纹：`Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null`
- 次数：1
- 首次：2026-10-17 11:22:24.423
- 最近：2026-10-17 11:22:24.423

```text
[2026-10-17 11:22:24.423] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - material/sheet/enums处理失败:Cannot invoke "com.th4.edge.tenant.user.model.Company.getComId()" because the return value of "com.th4.edge.tenant.user.model.Employee.getCompany()" is null
```

### INC-20261017-1638a78ea5 · unknown

- 指纹：`Failed to convert from type [java.lang.String] to type [java.lang.Long] for value [x]`
- 次数：1
- 首次：2026-10-17 11:22:17.227
- 最近：2026-10-17 11:22:17.227

```text
[2026-10-17 11:22:17.227] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - material/sheet/cancel处理失败:Failed to convert from type [java.lang.String] to type [java.lang.Long] for value [x]
```

### INC-20261017-2be88ca424 · unknown

- 指纹：`null`
- 次数：1
- 首次：2026-10-17 03:27:23.535
- 最近：2026-10-17 03:27:23.535

```text
[2026-10-17 03:27:23.535] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:null
```

### INC-20261017-0e0e3b6a7b · unknown

- 指纹：`作业单不存在或无权访问`
- 次数：3
- 首次：2026-10-17 10:43:13.141
- 最近：2026-10-17 11:25:05.314

```text
[2026-10-17 10:43:13.141] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - material/sheet/info处理失败:作业单不存在或无权访问
```

### INC-20261017-0133839b83 · unknown

- 指纹：`作业单行必须来源材料计划；合同外主材请先发起增减项`
- 次数：1
- 首次：2026-10-17 10:09:32.222
- 最近：2026-10-17 10:09:32.222

```text
[2026-10-17 10:09:32.222] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - material/sheet/save处理失败:作业单行必须来源材料计划；合同外主材请先发起增减项
```

### INC-20261017-a0cdb72877 · unknown

- 指纹：`分项已生成报价快照，归档报价后才能删除`
- 次数：16
- 首次：2026-10-17 13:20:15.610
- 最近：2026-10-17 13:23:42.853

```text
[2026-10-17 13:20:15.610] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - design/items/save处理失败:分项已生成报价快照，归档报价后才能删除
```

### INC-20261017-979f5a015e · unknown

- 指纹：`当前账号无权执行该操作`
- 次数：2
- 首次：2026-10-17 10:18:51.169
- 最近：2026-10-17 10:19:47.649

```text
[2026-10-17 10:18:51.169] [ERROR] [XNIO-1 task-8] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - statistic/team-leader/report处理失败:当前账号无权执行该操作
```

### INC-20261017-f7155d2eba · unknown

- 指纹：`找不到对应的处理服务:`
- 次数：2
- 首次：2026-10-17 12:43:10.087
- 最近：2026-10-17 13:28:06.571

```text
[2026-10-17 12:43:10.087] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - 处理失败:找不到对应的处理服务:
```

### INC-20261017-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：2
- 首次：2026-10-17 00:00:36.923
- 最近：2026-10-17 00:49:16.570

```text
[2026-10-17 00:00:36.923] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

### INC-20261017-dfb2d5ba8f · unknown

- 指纹：`找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php`
- 次数：1
- 首次：2026-10-17 09:12:50.621
- 最近：2026-10-17 09:12:50.621

```text
[2026-10-17 09:12:50.621] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php处理失败:找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php
```

