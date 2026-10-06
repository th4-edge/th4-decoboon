# 生产后端监控摘要

- 采集时间：2026-10-06T18:25:01+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-06/20261006-182500/raw.txt`
- 可处理异常：95
- 已修复历史异常：11
- 预期业务噪音：165

## 服务状态

```text
enabled
active
MainPID=166070
ExecMainStartTimestamp=Tue 2026-10-06 18:22:57 CST
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
/dev/vda3         40901312 11885632  27125312      31% /
/dev/vda3         40901312 11885632  27125312      31% /
/dev/vda3         40901312 11885632  27125312      31% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3311         570         514        4385        4144
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempo` |
| unknown | reopened | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual` |
| external_dependency | open | 7 | high | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempotency_fingerprint] in table [db_finance_payment]` |
| unknown | reopened | 7 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual_cost_amount] in table [db_finance_settlement]` |
| unknown | open | 7 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempotency_` |
| unknown | fixed | 8 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual_cost_` |
| unknown | reopened | 14 | medium | `org.springframework.beans.factory.BeanCreationException: Application run failed` |
| unknown | open | 19 | medium | `下次跟进时间不能早于今天` |
| unknown | open | 1 | medium | `只有客户已确认的报价可以生成合同` |
| unknown | open | 18 | medium | `工人工资、班组结算请通过工资应付和发放登记` |
| unknown | open | 1 | medium | `工程量必须大于0` |
| expected | observed | 1 | info | `手机号或密码错误` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env` |
| expected | observed | 1 | info | `找不到对应的处理服务:.env.local` |
| unknown | open | 1 | medium | `找不到对应的处理服务:session/properties` |
| expected | observed | 162 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 6 | medium | `该房屋已指定设计师，量房完成必须由指定设计师执行` |
| unknown | fixed | 3 | medium | `请先录入方案区域和尺寸` |

## 证据

### INC-20261006-43e03a18fa · unknown

- 指纹：`Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempo`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempotency_fingerprint] in table [db_finance_payment]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:431)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:400)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1849)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1798)
	... 15 common frames omitted
```

### INC-20261006-de9394cdc0 · unknown

- 指纹：`Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual_cost_amount] in table [db_finance_settlement]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:431)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:400)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1849)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1798)
	... 15 common frames omitted
```

### INC-20261006-89773f5799 · external_dependency

- 指纹：`Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempotency_fingerprint] in table [db_finance_payment]`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempotency_fingerprint] in table [db_finance_payment]
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
	at org.springframework.orm.jpa.LocalContainerEnt
```

### INC-20261006-fd03000078 · unknown

- 指纹：`Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual_cost_amount] in table [db_finance_settlement]`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual_cost_amount] in table [db_finance_settlement]
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
	at org.springframework.orm.jpa.LocalContainerEntit
```

### INC-20261006-85477e80d9 · unknown

- 指纹：`Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempotency_`
- 次数：7
- 首次：2026-10-06 14:30:07.578
- 最近：2026-10-06 14:32:59.361

```text
[2026-10-06 14:30:07.578] [ERROR] [main] [o.s.orm.jpa.LocalContainerEntityManagerFactoryBean] [?] [?] - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [idempotency_fingerprint] in table [db_finance_payment]
```

### INC-20261006-6e45ec8624 · unknown

- 指纹：`org.springframework.beans.factory.BeanCreationException: Application run failed`
- 次数：14
- 首次：2026-10-06 02:23:27.736
- 最近：2026-10-06 14:32:59.453

```text
[2026-10-06 02:23:27.736] [ERROR] [main] [org.springframework.boot.SpringApplication] [?] [?] - Application run failed
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [manual_cost_amount] in table [db_finance_settlement]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1802)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:601)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:523)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:336)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:288)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:334)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:204)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:970)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:627)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServ
```

### INC-20261006-05ad2b36fb · unknown

- 指纹：`下次跟进时间不能早于今天`
- 次数：19
- 首次：2026-10-06 14:25:30.207
- 最近：2026-10-06 14:26:09.272

```text
[2026-10-06 14:25:30.207] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - customer/customer-follow/save处理失败:下次跟进时间不能早于今天
```

### INC-20261006-0e18c4a6b0 · unknown

- 指纹：`只有客户已确认的报价可以生成合同`
- 次数：1
- 首次：2026-10-06 00:53:33.913
- 最近：2026-10-06 00:53:33.913

```text
[2026-10-06 00:53:33.913] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - project/contract/save处理失败:只有客户已确认的报价可以生成合同
```

### INC-20261006-0b18278681 · unknown

- 指纹：`工人工资、班组结算请通过工资应付和发放登记`
- 次数：18
- 首次：2026-10-06 15:09:32.284
- 最近：2026-10-06 16:07:18.945

```text
[2026-10-06 15:09:32.284] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - finance/ledger/update处理失败:工人工资、班组结算请通过工资应付和发放登记
```

### INC-20261006-cc3580e077 · unknown

- 指纹：`工程量必须大于0`
- 次数：1
- 首次：2026-10-06 17:44:26.525
- 最近：2026-10-06 17:44:26.525

```text
[2026-10-06 17:44:26.525] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - design/items/save处理失败:工程量必须大于0
```

