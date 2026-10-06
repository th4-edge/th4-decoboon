# 生产后端监控摘要

- 采集时间：2026-10-07T01:20:38+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-07/20261007-012037/raw.txt`
- 可处理异常：32
- 已修复历史异常：0
- 预期业务噪音：31

## 服务状态

```text
enabled
active
MainPID=172796
ExecMainStartTimestamp=Wed 2026-10-07 01:06:50 CST
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
/dev/vda3         40901312 12638984  26371960      33% /
/dev/vda3         40901312 12638984  26371960      33% /
/dev/vda3         40901312 12638984  26371960      33% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3464         402         639        4525        3991
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset` |
| unknown | open | 7 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]` |
| unknown | open | 8 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code]` |
| unknown | reopened | 7 | medium | `org.springframework.beans.factory.BeanCreationException: Application run failed` |
| unknown | open | 2 | medium | `找不到对应的处理服务:` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| expected | observed | 31 | info | `访问受限，缺少授权标识，请先登录后再访问。` |

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

### INC-20261007-ee1db0bdeb · unknown

- 指纹：`Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code]`
- 次数：8
- 首次：2026-10-07 01:00:27.177
- 最近：2026-10-07 01:03:44.581

```text
[2026-10-07 01:00:27.177] [ERROR] [main] [o.s.orm.jpa.LocalContainerEntityManagerFactoryBean] [?] [?] - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [preset_code] in table [db_document_template]
```

### INC-20261007-6e45ec8624 · unknown

- 指纹：`org.springframework.beans.factory.BeanCreationException: Application run failed`
- 次数：7
- 首次：2026-10-07 01:00:27.275
- 最近：2026-10-07 01:03:16.161

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

### INC-20261007-f7155d2eba · unknown

- 指纹：`找不到对应的处理服务:`
- 次数：2
- 首次：2026-10-07 01:18:27.730
- 最近：2026-10-07T01:18:27.730+08:00

```text
[2026-10-07 01:18:27.730] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - 处理失败:找不到对应的处理服务:
```

### INC-20261007-0277f97d16 · unknown

- 指纹：`找不到对应的处理服务:mcp`
- 次数：1
- 首次：2026-10-07 00:12:43.054
- 最近：2026-10-07 00:12:43.054

```text
[2026-10-07 00:12:43.054] [ERROR] [XNIO-1 task-6] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - mcp处理失败:找不到对应的处理服务:mcp
```

