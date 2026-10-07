# 生产后端监控摘要

- 采集时间：2026-10-07T16:54:38+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-07/20261007-165437/raw.txt`
- 可处理异常：127
- 已修复历史异常：0
- 预期业务噪音：217

## 服务状态

```text
enabled
active
MainPID=191193
ExecMainStartTimestamp=Wed 2026-10-07 15:04:28 CST
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
/dev/vda3         40901312 15559544  23451400      40% /
/dev/vda3         40901312 15559544  23451400      40% /
/dev/vda3         40901312 15559544  23451400      40% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3970         594         764        3953        3486
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 1 | medium | `<id>:<id>-10-07T16:37:41.370+08:00 ERROR <id> --- [ <thread> task-4] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 1 | medium | `<id>:<id>-10-07T16:38:15.783+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 1 | medium | `<id>:<id>-10-07T16:38:19.174+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 1 | medium | `<id>:<id>-10-07T16:38:19.847+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 1 | medium | `<id>:<id>-10-07T16:38:20.146+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 1 | medium | `<id>:<id>-10-07T16:38:20.602+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 6 | medium | `<id>:Caused by: java.sql.BatchUpdateException: Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 6 | medium | `<id>:Caused by: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | open | 6 | medium | `<id>:Caused by: org.hibernate.exception.ConstraintViolationException: could not execute batch [Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'] [insert into db_payroll_com` |
| unknown | open | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset` |
| unknown | open | 6 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [schedu` |
| unknown | open | 7 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]` |
| unknown | open | 6 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_days] in table [db_payroll_compensation_scheme]` |
| unknown | open | 8 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code]` |
| unknown | open | 7 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_da` |
| unknown | open | 12 | medium | `could not execute batch [Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'] [insert into db_payroll_compensation_scheme (base_salary,com_id,create_date,create_date_time,crea` |
| unknown | open | 2 | medium | `itemId不能为空` |
| unknown | open | 6 | medium | `org.hibernate.engine.jdbc.spi.SqlException: Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | reopened | 13 | medium | `org.springframework.beans.factory.BeanCreationException: Application run failed` |
| unknown | open | 2 | medium | `partnerId不能为空` |
| unknown | open | 2 | medium | `projectId不能为空` |
| unknown | open | 2 | medium | `reconciliationId不能为空` |
| unknown | open | 4 | medium | `已形成考勤、提成或工资事实，不能删除薪酬方案` |
| unknown | open | 5 | medium | `当前账号无权执行该操作` |
| expected | observed | 3 | info | `手机号或密码错误` |
| unknown | open | 2 | medium | `找不到对应的处理服务:` |
| expected | observed | 3 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:payroll/payslip/page` |
| unknown | open | 2 | medium | `找不到对应的处理服务:statistic/teamleader/report` |
| unknown | open | 1 | medium | `找不到对应的实体处理服务[<hex>.env]` |
| unknown | open | 3 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 211 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `请先指定项目经理再开工` |
| unknown | open | 2 | medium | `请选择项目或合同` |

## 证据

### INC-20261007-19e9a0b433 · unknown

- 指纹：`<id>:<id>-10-07T16:37:41.370+08:00 ERROR <id> --- [ <thread> task-4] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：1
- 首次：2026-10-07T16:37:41.370+08:00
- 最近：2026-10-07T16:37:41.370+08:00

```text
2662:2026-10-07T16:37:41.370+08:00 ERROR 191193 --- [  XNIO-1 task-4] o.h.engine.jdbc.spi.SqlExceptionHelper   : Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-86e3979ebb · unknown

- 指纹：`<id>:<id>-10-07T16:38:15.783+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：1
- 首次：2026-10-07T16:38:15.783+08:00
- 最近：2026-10-07T16:38:15.783+08:00

```text
2918:2026-10-07T16:38:15.783+08:00 ERROR 191193 --- [  XNIO-1 task-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-db1cce3797 · unknown

- 指纹：`<id>:<id>-10-07T16:38:19.174+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：1
- 首次：2026-10-07T16:38:19.174+08:00
- 最近：2026-10-07T16:38:19.174+08:00

```text
3068:2026-10-07T16:38:19.174+08:00 ERROR 191193 --- [  XNIO-1 task-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-539194da11 · unknown

- 指纹：`<id>:<id>-10-07T16:38:19.847+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：1
- 首次：2026-10-07T16:38:19.847+08:00
- 最近：2026-10-07T16:38:19.847+08:00

```text
3218:2026-10-07T16:38:19.847+08:00 ERROR 191193 --- [  XNIO-1 task-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-54d79ccda9 · unknown

- 指纹：`<id>:<id>-10-07T16:38:20.146+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：1
- 首次：2026-10-07T16:38:20.146+08:00
- 最近：2026-10-07T16:38:20.146+08:00

```text
3368:2026-10-07T16:38:20.146+08:00 ERROR 191193 --- [  XNIO-1 task-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-d4ed70afe8 · unknown

- 指纹：`<id>:<id>-10-07T16:38:20.602+08:00 ERROR <id> --- [ <thread> task-6] o.h.engine.jdbc.spi.SqlExceptionHelper : Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：1
- 首次：2026-10-07T16:38:20.602+08:00
- 最近：2026-10-07T16:38:20.602+08:00

```text
3518:2026-10-07T16:38:20.602+08:00 ERROR 191193 --- [  XNIO-1 task-6] o.h.engine.jdbc.spi.SqlExceptionHelper   : Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-03cbea4954 · unknown

- 指纹：`<id>:Caused by: java.sql.BatchUpdateException: Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：6
- 首次：未知
- 最近：未知

```text
2792:Caused by: java.sql.BatchUpdateException: Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-77e96809cf · unknown

- 指纹：`<id>:Caused by: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'`
- 次数：6
- 首次：未知
- 最近：未知

```text
2801:Caused by: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'
```

### INC-20261007-bedec83ed1 · unknown

- 指纹：`<id>:Caused by: org.hibernate.exception.ConstraintViolationException: could not execute batch [Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'] [insert into db_payroll_com`
- 次数：6
- 首次：未知
- 最近：未知

```text
2768:Caused by: org.hibernate.exception.ConstraintViolationException: could not execute batch [Duplicate entry '1790066191585976744-1790221396230505495-2026-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'] [insert into db_payroll_compensation_scheme (base_salary,com_id,create_date,create_date_time,create_day,create_hour,create_minute,create_month,create_second,create_time,create_week,create_year,creator_id,effective_from,effective_to,emp_id,remark,scheduled_days,status,store_id,team_id,update_date,update_date_time,update_day,update_hour,update_minute,update_month,update_second,update_time,update_week,update_year,scheme_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)]
```

### INC-20261007-896be00185 · unknown

- 指纹：`Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:431)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:400)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1849)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1798)
	... 15 common frames omitted
```

