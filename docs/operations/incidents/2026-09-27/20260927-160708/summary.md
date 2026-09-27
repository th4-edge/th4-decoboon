# 生产后端监控摘要

- 采集时间：2026-09-27T16:07:09+08:00
- 原始快照：`/Users/zhangwei/.codex/worktrees/operation-onboarding-console/th4-decoboon/docs/operations/incidents/2026-09-27/20260927-160708/raw.txt`
- 可处理异常：53
- 已修复历史异常：0
- 预期业务噪音：83

## 服务状态

```text
enabled
active
MainPID=101384
ExecMainStartTimestamp=Sun 2026-09-27 15:51:25 CST
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
/dev/vda3         40901312 8427732  30583212      22% /
/dev/vda3         40901312 8427732  30583212      22% /
/dev/vda3         40901312 8427732  30583212      22% /
               total        used        free      shared  buff/cache   available
Mem:            7456        3269         390         498        4592        4187
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_desi` |
| unknown | open | 7 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_context_snapshot]` |
| unknown | open | 8 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_con` |
| unknown | open | 1 | medium | `com.th4.edge.framework.exception.DefinedException: Application run failed` |
| unknown | open | 5 | medium | `name不能为空` |
| unknown | reopened | 7 | medium | `org.springframework.beans.factory.BeanCreationException: Application run failed` |
| unknown | open | 2 | medium | `平台账号或密码错误` |
| expected | observed | 2 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:gql` |
| unknown | open | 1 | medium | `找不到对应的处理服务:graphql` |
| unknown | open | 1 | medium | `找不到对应的处理服务:session/properties` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/version` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 4 | medium | `订阅不存在` |
| unknown | open | 2 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 72 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该手机号已绑定其他微信，请联系客服` |
| expected | observed | 9 | info | `首次登录请授权手机号` |

## 证据

### INC-20260927-20406376dc · unknown

- 指纹：`Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_desi`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_context_snapshot]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:431)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:400)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1849)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1798)
	... 15 common frames omitted
```

### INC-20260927-a8a380ae4e · unknown

- 指纹：`Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_context_snapshot]`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_context_snapshot]
	at org.hibernate.tool.schema.internal.AbstractSchemaValidator.validateTable(AbstractSchemaValidator.java:136)
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
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.create
```

### INC-20260927-bc0021868c · unknown

- 指纹：`Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_con`
- 次数：8
- 首次：2026-09-27 05:20:46.351
- 最近：2026-09-27 05:23:56.465

```text
[2026-09-27 05:20:46.351] [ERROR] [main] [o.s.orm.jpa.LocalContainerEntityManagerFactoryBean] [?] [?] - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_context_snapshot]
```

### INC-20260927-f8c5f0971d · unknown

- 指纹：`com.th4.edge.framework.exception.DefinedException: Application run failed`
- 次数：1
- 首次：2026-09-27 14:30:33.794
- 最近：2026-09-27 14:30:33.794

```text
[2026-09-27 14:30:33.794] [ERROR] [main] [org.springframework.boot.SpringApplication] [?] [?] - Application run failed
com.th4.edge.framework.exception.DefinedException: 密码必须同时包含字母和数字
	at com.th4.edge.platform.context.PlatformPasswordContext.validate(PlatformPasswordContext.java:35)
	at com.th4.edge.platform.configure.PlatformBootstrap.run(PlatformBootstrap.java:70)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:380)
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:727)
	at com.th4.edge.platform.configure.PlatformBootstrap$$SpringCGLIB$$0.run(<generated>)
	at org.springframework.boot.SpringApplication.lambda$callRunner$4(SpringApplication.java:784)
	at org.springframework.util.function.ThrowingConsumer$1.acceptWithException(ThrowingConsumer.java:82)
	at org.springframework.util.function.ThrowingConsumer.accept(ThrowingConsumer.java:60)
	at org.springframework.util.function.ThrowingConsumer$1.accept(ThrowingConsumer.java:86)
	at org.spr
```

### INC-20260927-ca771ba23d · unknown

- 指纹：`name不能为空`
- 次数：5
- 首次：2026-09-27 05:27:25.457
- 最近：2026-09-27 05:44:16.268

```text
[2026-09-27 05:27:25.457] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - site/lead/submit处理失败: name不能为空
```

### INC-20260927-6e45ec8624 · unknown

- 指纹：`org.springframework.beans.factory.BeanCreationException: Application run failed`
- 次数：7
- 首次：2026-09-27 05:20:46.441
- 最近：2026-09-27 05:23:29.798

```text
[2026-09-27 05:20:46.441] [ERROR] [main] [org.springframework.boot.SpringApplication] [?] [?] - Application run failed
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [db_design_context_snapshot]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1802)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:601)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:523)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:336)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:288)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:334)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:204)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:970)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:627)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:
```

### INC-20260927-aa9694d67a · unknown

- 指纹：`平台账号或密码错误`
- 次数：2
- 首次：2026-09-27 14:23:30.757
- 最近：2026-09-27 14:24:50.962

```text
[2026-09-27 14:23:30.757] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - platform/auth/login处理失败:平台账号或密码错误
```

### INC-20260927-c50a9523db · unknown

- 指纹：`找不到对应的处理服务:dev/.env`
- 次数：1
- 首次：2026-09-27 12:53:54.552
- 最近：2026-09-27 12:53:54.552

```text
[2026-09-27 12:53:54.552] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - dev/.env处理失败:找不到对应的处理服务:dev/.env
```

### INC-20260927-972186b589 · unknown

- 指纹：`找不到对应的处理服务:gql`
- 次数：1
- 首次：2026-09-27 05:44:47.512
- 最近：2026-09-27 05:44:47.512

```text
[2026-09-27 05:44:47.512] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - gql处理失败:找不到对应的处理服务:gql
```

### INC-20260927-8478b5fd37 · unknown

- 指纹：`找不到对应的处理服务:graphql`
- 次数：1
- 首次：2026-09-27 05:44:45.902
- 最近：2026-09-27 05:44:45.902

```text
[2026-09-27 05:44:45.902] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - graphql处理失败:找不到对应的处理服务:graphql
```

