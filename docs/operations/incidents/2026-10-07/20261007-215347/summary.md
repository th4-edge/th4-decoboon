# 生产后端监控摘要

- 采集时间：2026-10-07T21:53:48+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-07/20261007-215347/raw.txt`
- 可处理异常：91
- 已修复历史异常：12
- 预期业务噪音：245

## 服务状态

```text
enabled
active
MainPID=193374
ExecMainStartTimestamp=Wed 2026-10-07 17:14:05 CST
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
/dev/vda3         40901312 15856852  23154092      41% /
/dev/vda3         40901312 15856852  23154092      41% /
/dev/vda3         40901312 15856852  23154092      41% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3728         555         764        4234        3728
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset` |
| unknown | open | 6 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [schedu` |
| unknown | open | 7 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]` |
| unknown | open | 6 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_days] in table [db_payroll_compensation_scheme]` |
| unknown | open | 8 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code]` |
| unknown | open | 7 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_da` |
| unknown | fixed | 6 | medium | `could not execute batch [Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'] [insert into db_payroll_compensation_scheme (base_salary,com_id,create_date,create_date_time,crea` |
| unknown | open | 2 | medium | `itemId不能为空` |
| unknown | fixed | 6 | medium | `org.hibernate.engine.jdbc.spi.SqlException: Duplicate entry '<hex>-<hex>-<id>-09-15' for key 'db_payroll_compensation_scheme.uk_payroll_comp_employee_effective'` |
| unknown | reopened | 13 | medium | `org.springframework.beans.factory.BeanCreationException: Application run failed` |
| unknown | open | 2 | medium | `partnerId不能为空` |
| unknown | open | 2 | medium | `projectId不能为空` |
| unknown | open | 2 | medium | `reconciliationId不能为空` |
| unknown | open | 4 | medium | `已形成考勤、提成或工资事实，不能删除薪酬方案` |
| unknown | open | 5 | medium | `当前账号无权执行该操作` |
| expected | observed | 4 | info | `手机号或密码错误` |
| unknown | open | 2 | medium | `找不到对应的处理服务:` |
| expected | observed | 3 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:payroll/payslip/page` |
| unknown | open | 2 | medium | `找不到对应的处理服务:statistic/teamleader/report` |
| unknown | open | 1 | medium | `找不到对应的实体处理服务[<hex>.env]` |
| unknown | open | 5 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 238 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 2 | medium | `该员工本月考勤已存在` |
| unknown | open | 2 | medium | `请先为该员工配置薪酬方案` |
| unknown | open | 1 | medium | `请先指定项目经理再开工` |
| unknown | open | 2 | medium | `请选择项目或合同` |

## 证据

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

### INC-20261007-1bcef03a08 · unknown

- 指纹：`Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [schedu`
- 次数：6
- 首次：未知
- 最近：未知

```text
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_days] in table [db_payroll_compensation_scheme]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:431)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:400)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1849)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1798)
	... 15 common frames omitted
```

### INC-20261007-e48ced58b0 · unknown

- 指纹：`Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]
	at org.hibernate.tool.schema.internal.AbstractSchemaValidator.validateTable(AbstractSchemaValidator.java:149)
	at org.hibernate.tool.schema.internal.GroupedSchemaValidatorImpl.validateTables(GroupedSchemaValidatorImpl.java:46)
	at org.hibernate.tool.schema.internal.AbstractSchemaValidator.performValidation(AbstractSchemaValidator.java:99)
	at org.hibernate.tool.schema.internal.AbstractSchemaValidator.doValidation(AbstractSchemaValidator.java:77)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.performDatabaseAction(SchemaManagementToolCoordinator.java:289)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.lambda$process$5(SchemaManagementToolCoordinator.java:144)
	at java.base/java.util.HashMap.forEach(HashMap.java:1429)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.process(SchemaManagementToolCoordinator.java:141)
	at org.hibernate.boot.internal.SessionFactoryObserverForSchemaExport.sessionFactoryCreated(SessionFactoryObserverForSchemaExport.java:37)
	at org.hibernate.internal.SessionFactoryObserverChain.sessionFactoryCreated(SessionFactoryObserverChain.java:35)
	at org.hibernate.internal.SessionFactoryImpl.<init>(SessionFactoryImpl.java:324)
	at org.hibernate.boot.internal.SessionFactoryBuilderImpl.build(SessionFactoryBuilderImpl.java:463)
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1506)
	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:66)
	at org.springframework.orm.jpa.LocalContainerEntityManager
```

### INC-20261007-59fd222772 · unknown

- 指纹：`Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_days] in table [db_payroll_compensation_scheme]`
- 次数：6
- 首次：未知
- 最近：未知

```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_days] in table [db_payroll_compensation_scheme]
	at org.hibernate.tool.schema.internal.AbstractSchemaValidator.validateTable(AbstractSchemaValidator.java:149)
	at org.hibernate.tool.schema.internal.GroupedSchemaValidatorImpl.validateTables(GroupedSchemaValidatorImpl.java:46)
	at org.hibernate.tool.schema.internal.AbstractSchemaValidator.performValidation(AbstractSchemaValidator.java:99)
	at org.hibernate.tool.schema.internal.AbstractSchemaValidator.doValidation(AbstractSchemaValidator.java:77)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.performDatabaseAction(SchemaManagementToolCoordinator.java:289)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.lambda$process$5(SchemaManagementToolCoordinator.java:144)
	at java.base/java.util.HashMap.forEach(HashMap.java:1429)
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.process(SchemaManagementToolCoordinator.java:141)
	at org.hibernate.boot.internal.SessionFactoryObserverForSchemaExport.sessionFactoryCreated(SessionFactoryObserverForSchemaExport.java:37)
	at org.hibernate.internal.SessionFactoryObserverChain.sessionFactoryCreated(SessionFactoryObserverChain.java:35)
	at org.hibernate.internal.SessionFactoryImpl.<init>(SessionFactoryImpl.java:324)
	at org.hibernate.boot.internal.SessionFactoryBuilderImpl.build(SessionFactoryBuilderImpl.java:463)
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1506)
	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:66)
	at org.springframework.orm.jpa.LocalContainer
```

### INC-20261007-ee1db0bdeb · unknown

- 指纹：`Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code]`
- 次数：8
- 首次：2026-10-07 01:00:27.177
- 最近：2026-10-07 01:03:44.581

```text
[2026-10-07 01:00:27.177] [ERROR] [main] [o.s.orm.jpa.LocalContainerEntityManagerFactoryBean] [?] [?] - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]
```

### INC-20261007-b51f08da31 · unknown

- 指纹：`Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_da`
- 次数：7
- 首次：2026-10-07 05:47:29.093
- 最近：2026-10-07 05:50:27.406

```text
[2026-10-07 05:47:29.093] [ERROR] [main] [o.s.orm.jpa.LocalContainerEntityManagerFactoryBean] [?] [?] - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [scheduled_days] in table [db_payroll_compensation_scheme]
```

### INC-20261007-e649d9bcbb · unknown

- 指纹：`itemId不能为空`
- 次数：2
- 首次：2026-10-07 06:47:03.797
- 最近：2026-10-07 06:47:03.833

```text
[2026-10-07 06:47:03.797] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - carryover/payment-plan/report处理失败: itemId不能为空
```

### INC-20261007-6e45ec8624 · unknown

- 指纹：`org.springframework.beans.factory.BeanCreationException: Application run failed`
- 次数：13
- 首次：2026-10-07 01:00:27.275
- 最近：2026-10-07 05:49:58.370

```text
[2026-10-07 01:00:27.275] [ERROR] [main] [org.springframework.boot.SpringApplication] [?] [?] - Application run failed
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1802)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:601)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:523)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:336)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:288)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:334)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:204)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:970)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:627)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplic
```

### INC-20261007-776a4033e6 · unknown

- 指纹：`partnerId不能为空`
- 次数：2
- 首次：2026-10-07 06:47:06.531
- 最近：2026-10-07 06:47:06.665

```text
[2026-10-07 06:47:06.531] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - partner/statement/report处理失败: partnerId不能为空
```

### INC-20261007-ab3c0c0c20 · unknown

- 指纹：`projectId不能为空`
- 次数：2
- 首次：2026-10-07 06:47:03.660
- 最近：2026-10-07 06:47:03.694

```text
[2026-10-07 06:47:03.660] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - finance/project/report处理失败: projectId不能为空
```

