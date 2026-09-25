# 生产后端监控摘要

- 采集时间：2026-09-24T12:24:47+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-09-24/20260924-122445/raw.txt`
- 可处理异常：31
- 已修复历史异常：8
- 预期业务噪音：131

## 服务状态

```text
enabled
active
MainPID=80532
ExecMainStartTimestamp=Thu 2026-09-24 12:07:51 CST
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
/dev/vda3         40901312 7310012  31700932      19% /
/dev/vda3         40901312 7310012  31700932      19% /
/dev/vda3         40901312 7310012  31700932      19% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3229         574         486        4428        4226
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | reopened | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [comple` |
| unknown | reopened | 7 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [completed_at] in table [db_measure]` |
| unknown | fixed | 8 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [completed_at` |
| unknown | reopened | 7 | medium | `org.springframework.beans.factory.BeanCreationException: Application run failed` |
| unknown | open | 4 | medium | `当前套餐最多支持 5 个席位，已使用 5 个。请购买席位包或升级订阅后继续邀请成员。` |
| unknown | open | 2 | medium | `微信登录失败：code been used, rid: 6ab4a4f1-5d18a70e-1c836727` |
| unknown | open | 2 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 114 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 2 | medium | `邀请不存在或不可用` |
| expected | observed | 17 | info | `首次登录请授权手机号` |

## 证据

### INC-20260924-6014ce48c3 · unknown

- 指纹：`Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [comple`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [completed_at] in table [db_measure]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:431)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:400)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1849)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1798)
	... 15 common frames omitted
```

### INC-20260924-04802ca455 · unknown

- 指纹：`Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [completed_at] in table [db_measure]`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [completed_at] in table [db_measure]
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
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBe
```

### INC-20260924-6e45ec8624 · unknown

- 指纹：`org.springframework.beans.factory.BeanCreationException: Application run failed`
- 次数：7
- 首次：2026-09-24 04:36:20.714
- 最近：2026-09-24 04:38:58.254

```text
[2026-09-24 04:36:20.714] [ERROR] [main] [org.springframework.boot.SpringApplication] [?] [?] - Application run failed
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [completed_at] in table [db_measure]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1802)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:601)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:523)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:336)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:288)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:334)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:204)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:970)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:627)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationCont
```

### INC-20260924-67c6d53d64 · unknown

- 指纹：`当前套餐最多支持 5 个席位，已使用 5 个。请购买席位包或升级订阅后继续邀请成员。`
- 次数：4
- 首次：2026-09-24 11:43:15.867
- 最近：2026-09-24 11:44:23.626

```text
[2026-09-24 11:43:15.867] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - invite/invitation/save处理失败:当前套餐最多支持 5 个席位，已使用 5 个。请购买席位包或升级订阅后继续邀请成员。
```

### INC-20260924-72da57a9d1 · unknown

- 指纹：`微信登录失败：code been used, rid: 6ab4a4f1-5d18a70e-1c836727`
- 次数：2
- 首次：2026-09-24 12:20:01.330
- 最近：2026-09-24T12:20:01.330+08:00

```text
[2026-09-24 12:20:01.330] [ERROR] [XNIO-1 task-5] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/login处理失败:微信登录失败：code been used, rid: 6ab4a4f1-5d18a70e-1c836727
```

### INC-20260924-060771017b · unknown

- 指纹：`访问受限，授权已过期，请重新登录后再访问。`
- 次数：2
- 首次：2026-09-24 06:16:27.797
- 最近：2026-09-24 10:06:20.698

```text
[2026-09-24 06:16:27.797] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - auth/session/info处理失败:访问受限，授权已过期，请重新登录后再访问。
```

### INC-20260924-f2807bf3f6 · unknown

- 指纹：`邀请不存在或不可用`
- 次数：2
- 首次：2026-09-24 03:32:53.629
- 最近：2026-09-24 03:32:55.905

```text
[2026-09-24 03:32:53.629] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - identity/customer-invite/info处理失败:邀请不存在或不可用
```

