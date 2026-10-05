# 生产后端监控摘要

- 采集时间：2026-10-05T19:22:27+08:00
- 原始快照：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/operations/incidents/2026-10-05/20261005-192227/raw.txt`
- 可处理异常：71
- 已修复历史异常：0
- 预期业务噪音：150

## 服务状态

```text
enabled
active
MainPID=146604
ExecMainStartTimestamp=Mon 2026-10-05 19:21:39 CST
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
/dev/vda3         40901312 9824780  29186164      26% /
/dev/vda3         40901312 9824780  29186164      26% /
/dev/vda3         40901312 9824780  29186164      26% /
               total        used        free      shared  buff/cache   available
Mem:            7456        2995         603         265        4420        4461
Swap:              0           0           0
```

## 异常分组

| 分类 | 状态 | 次数 | 严重度 | 指纹 |
| --- | --- | ---: | --- | --- |
| unknown | open | 7 | medium | `Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quanti` |
| unknown | open | 7 | medium | `Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_formula] in table [quote_line]` |
| unknown | open | 8 | medium | `Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_for` |
| unknown | reopened | 7 | medium | `org.springframework.beans.factory.BeanCreationException: Application run failed` |
| unknown | open | 5 | medium | `同级分类名称已存在` |
| unknown | open | 3 | medium | `客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除` |
| unknown | open | 1 | medium | `工程量必须大于0` |
| expected | observed | 3 | info | `找不到对应的处理服务:.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:.git/config` |
| unknown | open | 1 | medium | `找不到对应的处理服务:backup` |
| unknown | open | 1 | medium | `找不到对应的处理服务:chat/get-chat-session/1` |
| unknown | open | 1 | medium | `找不到对应的处理服务:config.js` |
| unknown | open | 1 | medium | `找不到对应的处理服务:design/items/save` |
| unknown | open | 1 | medium | `找不到对应的处理服务:dev/.env` |
| unknown | open | 2 | medium | `找不到对应的处理服务:index.php/v1/config/application` |
| unknown | open | 1 | medium | `找不到对应的处理服务:mcp` |
| unknown | open | 1 | medium | `找不到对应的处理服务:modular-connector/login/slrdg` |
| unknown | open | 1 | medium | `找不到对应的处理服务:phpinfo.php` |
| unknown | open | 1 | medium | `找不到对应的处理服务:raster/search/` |
| unknown | open | 1 | medium | `找不到对应的处理服务:staging/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/config/application` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/fabric_device_auth/fortigate/init` |
| unknown | open | 2 | medium | `找不到对应的处理服务:v1/init_consts` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/monitor/messages` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v1/version` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v2/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:v3/.env` |
| unknown | open | 1 | medium | `找不到对应的处理服务:vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php` |
| unknown | open | 1 | medium | `找不到对应的实体处理服务[complete]` |
| unknown | open | 2 | medium | `设计方案尚未完成，不能生成客户报价` |
| unknown | open | 5 | medium | `访问受限，授权已过期，请重新登录后再访问。` |
| expected | observed | 144 | info | `访问受限，缺少授权标识，请先登录后再访问。` |
| unknown | open | 1 | medium | `该房屋已指定设计师，量房完成必须由指定设计师执行` |
| expected | observed | 3 | info | `首次登录请授权手机号` |

## 证据

### INC-20261005-7a15e72695 · unknown

- 指纹：`Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quanti`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: jakarta.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_formula] in table [quote_line]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:431)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:400)
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1849)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1798)
	... 15 common frames omitted
```

### INC-20261005-2ed6f423ba · unknown

- 指纹：`Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_formula] in table [quote_line]`
- 次数：7
- 首次：未知
- 最近：未知

```text
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_formula] in table [quote_line]
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
	at org.springframework.orm.jpa.LocalContainerEntityManagerFacto
```

### INC-20261005-23fca06cdb · unknown

- 指纹：`Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_for`
- 次数：8
- 首次：2026-10-05 18:42:18.138
- 最近：2026-10-05 18:45:35.692

```text
[2026-10-05 18:42:18.138] [ERROR] [main] [o.s.orm.jpa.LocalContainerEntityManagerFactoryBean] [?] [?] - Failed to initialize JPA EntityManagerFactory: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_formula] in table [quote_line]
```

### INC-20261005-6e45ec8624 · unknown

- 指纹：`org.springframework.beans.factory.BeanCreationException: Application run failed`
- 次数：7
- 首次：2026-10-05 18:42:18.234
- 最近：2026-10-05 18:45:07.180

```text
[2026-10-05 18:42:18.234] [ERROR] [main] [org.springframework.boot.SpringApplication] [?] [?] - Application run failed
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [quantity_formula] in table [quote_line]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1802)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:601)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:523)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:336)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:288)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:334)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:204)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:970)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:627)
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplication
```

### INC-20261005-c9e817936a · unknown

- 指纹：`同级分类名称已存在`
- 次数：5
- 首次：2026-10-05 18:28:45.958
- 最近：2026-10-05 18:31:40.261

```text
[2026-10-05 18:28:45.958] [ERROR] [XNIO-1 task-9] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - finance/ledger-category/save处理失败:同级分类名称已存在
```

### INC-20261005-83305d84a1 · unknown

- 指纹：`客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除`
- 次数：3
- 首次：2026-10-05 17:44:26.379
- 最近：2026-10-05 17:44:59.569

```text
[2026-10-05 17:44:26.379] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - customer/customer/delete处理失败:客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除
```

### INC-20261005-cc3580e077 · unknown

- 指纹：`工程量必须大于0`
- 次数：1
- 首次：2026-10-05 19:03:00.959
- 最近：2026-10-05 19:03:00.959

```text
[2026-10-05 19:03:00.959] [ERROR] [XNIO-1 task-2] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - design/items/save处理失败:工程量必须大于0
```

### INC-20261005-8f4d3f4876 · unknown

- 指纹：`找不到对应的处理服务:.git/config`
- 次数：1
- 首次：2026-10-05 15:52:03.199
- 最近：2026-10-05 15:52:03.199

```text
[2026-10-05 15:52:03.199] [ERROR] [XNIO-1 task-3] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - .git/config处理失败:找不到对应的处理服务:.git/config
```

### INC-20261005-1638ca7b18 · unknown

- 指纹：`找不到对应的处理服务:backup`
- 次数：1
- 首次：2026-10-05 16:31:12.712
- 最近：2026-10-05 16:31:12.712

```text
[2026-10-05 16:31:12.712] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - backup处理失败:找不到对应的处理服务:backup
```

### INC-20261005-5f3c586f32 · unknown

- 指纹：`找不到对应的处理服务:chat/get-chat-session/1`
- 次数：1
- 首次：2026-10-05 16:31:14.686
- 最近：2026-10-05 16:31:14.686

```text
[2026-10-05 16:31:14.686] [ERROR] [XNIO-1 task-4] [c.t.e.framework.web.mvc.processor.ApiHttpProcessor] [?] [?] - chat/get-chat-session/1处理失败:找不到对应的处理服务:chat/get-chat-session/1
```

