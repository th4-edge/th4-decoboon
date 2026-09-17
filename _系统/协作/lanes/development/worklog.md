# Development Worklog

## Current

已完成 `REQ-20260915-DEV-OWNER-CLOSURE`、`REQ-20260915-DEV-T05`、`REQ-20260915-DEV-FIX-FILE-AUTH`、`REQ-20260915-DEV-P05-P07`、`REQ-20260915-DEV-P06`、`REQ-20260915-DEV-FIX-P05`、`REQ-20260915-DEV-FIX-P06`、`REQ-20260915-DEV-FIX-P05-FINAL` 和 `REQ-20260915-WEB-BUILD-1`；发布迁移基线与 Web 第一阶段已落地。

`REQ-20260915-DEV-MINIAPP-PASSWORD` 已实现小程序密码设置闭环：现有受保护的 `auth/session/password-change` 在未设置密码时执行首次设密并记录 `PasswordSet`，已设置时必须校验原密码，同时兼容小程序微信身份令牌与 Web 员工令牌；`auth/session/info` 增加 `passwordSet`；小程序新增“我的 -> 账号密码”设置/修改页。后端全量 `79 tests / 0 failures / 0 errors`，`npm run verify:mp-weixin` 通过；临时隔离探针已跑通“微信员工登录 -> 首次设密 -> Web 手机号密码登录”1/1，但真实微信交互和正式 acceptance 用例仍待执行。

`REQ-20260916-DEV-WEB-QUOTE-WORKBENCH` 已完成 Web 设计/报价/增减项高频录入闭环：设计方案新增 Excel 式工作台，支持多行粘贴、企业报价项目库匹配、连续键盘录入、区域维护、逐行保存和从方案一键生成报价；报价详情支持回到设计方案调整并导出 CSV；项目详情新增合同增减项批量工作台和明细页签，新增受保护 Provider `project/contract-change/batch-save`，同一事务保存一张变更单的全部增减项明细，并复用项目数据范围和计划编辑权限。Web `type-check`、`verify:system`、`build` 通过；后端 `compile` 通过，除固定路由总数断言外其余全量测试通过。Provider 扫描更新为 `total=198 protected=187 public=11 violations=0`。

真实浏览器闭环验证补充：已通过 `18660219918` 登录 `127.0.0.1`，验证施工/主材/软装目录新增、编辑、停用；设计 Excel 两行粘贴、保存和报价生成；报价提交、接受、签约、合同生效立项；增减项 700 元双明细批量录入、审批和客户确认；项目完工、6 阶段验收、收款和资金流水。过程中修复客户确认响应膨胀、合同变更履约 property 空值、锁组件条件注册、报价状态推进后详情丢失、方案锁定提示、完整目录分页、通用编辑 ID 注入、目录更新 itemId 映射、财务金额/百分比/枚举显示等问题。

`REQ-20260916-WEB-REBUILD-2` 已完成 Web v2 全业务体系重建：旧 Web 45 个源码文件已归档为可浏览目录和 SHA-256 校验压缩包；新工程对齐 App 领域模型，建立 8 类员工角色/DataScopeType/能力矩阵、31 个资源页、40 个导航入口、角色工作台、客户/报价/项目详情、财务总览、经营报表、消息、订阅和个人中心。`npm run type-check`、`npm run verify:system`、`npm run build` 均通过；真实后端和 8 类角色浏览器联调仍待 acceptance。

`REQ-20260916-DEV-OSS-STORAGE` 已完成阿里云 OSS 企业/项目存储改造：`decoboon` Bucket 位于华东1（杭州），使用标准存储、同城冗余和私有访问；创建 `decoboon-oss` RAM 用户及仅授权该 Bucket 的 `decoboon-oss-rw` 策略。后端新增统一 `ObjectStorage` 抽象，上传并入受保护 `file/private/grant`，对象键按 `companies/{comId}/projects/{projectId}/...` 和 `companies/{comId}/...` 隔离，谈单 PPT 直接生成到 OSS。真实 SDK、Spring Bean、业务上传 Provider 和 PPT 生成探针均通过，预签名 URL 200、无签名直链 403；小程序与 Web 类型/构建通过。

`REQ-20260916-DEV-WECHAT-PAY` 已完成微信支付 API v3 参数部署：核对商户号 `1750623009`、AppID `wxe5cc5fec9e50a9a9`、证书序列号 `24A00BDC638A9B5BE07C8E6BF396096BA379AE07` 和微信支付公钥 ID。商户证书、私钥、PKCS#12 和公钥已放入 Git 忽略目录 `config/wechat-pay/`，API v3 密钥和商户参数放入权限 600 的 `.env.wechat-pay.local`；代码仓库不再保留敏感默认值。正式交易查询接口验签通过，Spring `WechatPayClient` 调用通过，新增启动期配置和密钥文件校验。

`REQ-20260916-DEV-PROD-RELEASE` 已完成生产部署并复核：`application-prod.yml`、`cmd.sh`、systemd、Nginx HTTPS/SPA 和发布脚本生效；后端与 Web `20260916-0415` 已上线。空库完成 88 张表和 `01/10/11/12/17/21` BASELINE 账本，`decoboon.service` active/enabled，`cmd.sh check` 通过，Nginx `nginx -t` 通过并监听 80/443，HTTP 301、HTTPS 首页/静态 JS/API 鉴权/支付回调路由冒烟通过；TLS 证书有效期至 2026-12-13 UTC。

验证记录：小程序 `verify:mp-weixin` 通过；Web 修复后 Provider 扫描 196 条路由、185 条受保护、11 条公开白名单、0 violations；后端全量 77 tests 仅剩旧 `ProviderSecurityAcceptanceTest` 的公开白名单预期失败，`WebPasswordSessionAcceptanceTest` 5/5 通过；迁移与恢复脚本 `bash -n` 通过。

`REQ-20260915-DEV-FIX-P05-FINAL` 已把迁移策略收敛为：新空库正式支持 `baseline`，只执行 `00-迁移账本.sql` + `01-空库基线.sql`；存量库不得自动跑不完整 `upgrade`，必须 DBA 审计后 `adopt` 登记，差异迁移另行完整设计。`10-设计转化域迁移.sql` 已标记为不完整历史草稿并禁止自动执行，密码登录迁移为 `21-用户密码登录.sql`。`baseline` 失败会尝试写入 `FAILED`，脚本对 `RUNNING`/`FAILED` 拒绝自动重跑；迁移文件和恢复模板增加 SHA-256 兼容校验、漂移检测及 `realpath` 规范化危险根路径判断。真实 MySQL 8 基线、差异迁移和恢复演练仍为 BLOCKED，静态/故障注入通过不等于生产演练通过。

最终本地验证：三个模板 `bash -n` 通过；fake mysql 故障注入 `baseline` 返回退出码 `4`、账本为 `FAILED|...` 且输出 `FAILED 01`；checksum 漂移返回退出码 `8` 和 `CHECKSUM_DRIFT`；`upgrade` 返回退出码 `9` 和 `UPGRADE_DISABLED`；`RESTORE_ROOT=/var/..` 经规范化后返回退出码 `2`；`mvn -o -q test` 退出码 `0`，Surefire `72 tests / 0 failures / 0 errors / 0 skipped`。

Web 最终本地验证：依赖通过 npm 镜像安装并生成锁文件；`npm run type-check` 通过；`npm run build` 通过，Vite 报告主入口 chunk 约 1.10 MB（gzip 约 362 KB）的体积警告。

Web 修复验证：`WebPasswordSessionAcceptanceTest` 5/5 通过；Provider 扫描 `total=196 protected=185 public=11 violations=0`；全量测试 77 项仅剩旧 `ProviderSecurityAcceptanceTest` 白名单计数/集合预期失败，等待 acceptance lane 更新测试预期。

`REQ-20260915-WEB-BUILD-1` 已完成 Web 工程、员工密码会话、角色菜单/路由、客户/项目/报价/财务页面及页面壳；后端新增 `Password` 凭证、BCrypt、失败锁定、首次设置、改密和登出 Provider。Web `npm run type-check`、`npm run build` 通过。

`REQ-20260915-WEB-FIX-1` 已按验收报告修复：密码登录/首设拆为公开 Provider，改密/登出拆为独立 `Security` Provider；Web 驾驶舱、财务汇总、报价、任务和资金流水字段改为真实实体契约，项目状态筛选补齐后端 `Preparing/Ongoing` 等枚举支持，报价 keyword 筛选下推后端。Provider 扫描更新为 `total=196 protected=185 public=11 violations=0`。

`REQ-20260916-DEV-PROD-SENTINEL` 已建立生产后端哨兵闭环：新增只读采集与分析脚本、项目级 `backend-prod-sentinel` Skill、受控发布/回滚脚本和事故 registry；监控首次运行捕获 `auth/session/info` 的 `LazyInitializationException`，已通过 `CredentialsRepository` 的 `@EntityGraph` 加载员工、门店、团队和企业关系，新增非事务回归测试 `AuthSessionInfoLazyAcceptanceTest`。排除两个既有陈旧静态断言后全量测试通过；发布包部署到 `/opt/decoboon/releases/20260916-053143`，首次部署因就绪等待不足触发自动回滚，修正后重新发版成功，HTTPS 健康检查返回 200。

## Outputs

| title | path | audience | status |
|---|---|---|---|
| 业主闭环 P0 后端交接 | `_系统/协作/lanes/development/workspace/owner-closure-handoff.md` | product-planning / acceptance / 前端 | ready |
| 业主闭环生产迁移建议 | `_系统/协作/lanes/development/workspace/owner-closure-migration.sql` | acceptance / 运维 | migration_required |
| 业主闭环正式迁移 | `docs/design/migrations/17-业主闭环.sql` | acceptance / 运维 | ready |
| 私有文件访问发布说明 | `docs/design/migrations/18-私有文件访问发布说明.md` | acceptance / 运维 / 前端 | ready |
| 生产发布基线 Runbook | `docs/design/migrations/19-生产发布基线.md` | acceptance / 运维 | ready_with_blockers |
| Provider 安全扫描报告 | `_系统/协作/lanes/development/workspace/provider-security-scan.md` | acceptance / 运维 | ready |
| Provider 安全逐路由清单 | `_系统/协作/lanes/development/workspace/provider-security-scan.csv` | acceptance / 运维 | ready |
| 小程序发布合规基线 | `docs/design/migrations/20-小程序发布合规基线.md` | acceptance / 运维 / 产品 | ready_with_blockers |
| 小程序发布门禁 | `../../前端代码/th4-decoboon-app/scripts/verify-release-env.mjs` | acceptance / 运维 / 前端 | ready |
| Web 企业工作台 | `../../前端代码/th4-decoboon-web/` | product-planning / acceptance / 前端 | ready_with_blockers |
| Web 员工密码会话 | `auth/session/password-login`、`auth/session/password-setup` 公开；`password-change`、`logout` Security | acceptance / 运维 | ready |
| 小程序密码设置闭环开发交接 | `_系统/协作/lanes/development/workspace/miniapp-password-closure-handoff.md` | acceptance / user | ready_pending_acceptance |
| Web v2 全业务体系 | `../../前端代码/th4-decoboon-web/` | product-planning / acceptance / user | built_pending_acceptance |
| Web v2 开发交接 | `_系统/协作/lanes/development/workspace/web-v2-full-business-handoff.md` | acceptance / user | ready_pending_acceptance |
| 阿里云 OSS 对象存储 | `docs/design/migrations/22-OSS对象存储部署说明.md` | acceptance / 运维 / development | configured_pending_acceptance |
| OSS 存储开发交接 | `_系统/协作/lanes/development/workspace/aliyun-oss-storage-handoff.md` | acceptance / 运维 / user | ready_pending_acceptance |
| 微信支付 API v3 配置 | `docs/design/migrations/23-微信支付配置.md` | acceptance / 运维 / development | configured_pending_acceptance |
| 微信支付配置交接 | `_系统/协作/lanes/development/workspace/wechat-pay-config-handoff.md` | acceptance / 运维 / user | ready_pending_acceptance |
| 生产发版包与配置 | `_系统/协作/lanes/development/workspace/production-release-config-handoff.md` | acceptance / 运维 / product-planning | deployed_pending_acceptance |
| 生产后端哨兵 | `_系统/协作/lanes/development/workspace/backend-prod-sentinel-handoff.md` | acceptance / 运维 / development | deployed_fixed_first_incident |
| Web 设计/报价/增减项高频录入闭环 | `_系统/协作/lanes/development/workspace/web-design-quote-change-workbench-handoff.md` | acceptance / product-planning / user | ready_pending_acceptance |
| 用户密码迁移 | `docs/design/migrations/21-用户密码登录.sql` | DBA / acceptance | ready |
| 空库基线 DDL | `docs/design/migrations/01-空库基线.sql` | DBA / acceptance | generated_unverified |
| 迁移账本 | `docs/design/migrations/00-迁移账本.sql` | DBA / 运维 | ready |
| 迁移执行模板 | `docs/design/migrations/run-migrations.sh.template` | DBA / 运维 | ready_unverified |
| 安全恢复模板 | `docs/design/migrations/rollback-safe.sh.template` | DBA / 运维 | ready_unverified |

## Requests

| to | request | status | link |
|---|---|---|---|
| acceptance | 独立验证小程序首次设密、改密、Web 手机号密码登录闭环、审计和敏感字段边界。 | pending_acceptance | `_系统/协作/lanes/development/workspace/miniapp-password-closure-handoff.md` |
| acceptance | 独立验证 Web v2 的 8 类角色菜单、数据范围、页面/Provider 契约、401/403 和关键业务动作。 | pending_acceptance | `_系统/协作/lanes/development/workspace/web-v2-full-business-handoff.md` |
| acceptance | 独立验证 OSS 企业/项目隔离、跨租户拒绝、预签名过期、大文件/视频和历史文件迁移方案。 | pending_acceptance | `_系统/协作/lanes/development/workspace/aliyun-oss-storage-handoff.md` |
| acceptance | 独立验证微信支付 JSAPI 预下单、支付查询、真实回调验签解密、金额/商户/AppID 边界和重复通知。 | pending_acceptance | `_系统/协作/lanes/development/workspace/wechat-pay-config-handoff.md` |
| acceptance | 使用真实企业账号验证生产 Web/API 全业务闭环、微信支付首笔回调和密钥/备份回滚。 | pending_acceptance | `_系统/协作/lanes/development/workspace/production-release-config-handoff.md` |
| acceptance | 验证 Web 方案 Excel 粘贴录入、服务端汇算、报价生成与导出，以及增减项多行原子提交、角色权限和跨企业边界；同步把 Provider 路由总数预期从 196 更新为 198。 | pending_acceptance | `_系统/协作/lanes/development/workspace/web-design-quote-change-workbench-handoff.md` |

## Notes

- 职责：负责功能实现、生产代码修改、自测证据和按授权提交代码，不负责最终验收结论。
- 主动写入范围：`src/main/**`、`pom.xml`、`docs/design/migrations/**`、`../../前端代码/th4-decoboon-app/src/**`、`../../前端代码/th4-decoboon-app/scripts/**`、`../../前端代码/th4-decoboon-app/package.json`、`../../前端代码/th4-decoboon-web/**`、`_系统/协作/lanes/development/**`
- 跨出写入范围前，必须先登记共享请求或取得用户明确授权。
- 交接必须包含可核验的文件、验证结果、风险和下一步。

## Next

- 完成装得清 Web 端规划后的工程搭建、User 登录和核心业务界面。
- acceptance lane 独立验证邀请消费、显式项目授权、会话身份隔离、owner-only 读取和验收状态机。
- acceptance lane 复验匿名 URL、签名过期、跨企业/跨项目文件授权和业务对象路径绑定。
- acceptance lane 复验 Provider Security 扫描、公开白名单、订阅写闸门和 prod profile fail-fast。
- acceptance lane 和产品/法务确认小程序正式域名、AppID、协议正文、隐私政策和真机提审材料。
- DBA 在隔离 MySQL 8 环境验证 `baseline`、DBA 审计后的 `adopt` 和 `rollback-safe` 模板并留存账本证据；任何存量库差异迁移必须先形成独立完整迁移并通过评审。
- acceptance lane 独立验证 Web `type-check`/`build`、四种角色菜单、登录/锁定/改密/登出和 401/403 路由行为；后端密码会话需要补独立验收用例。
- acceptance lane 独立验证小程序首次设密 -> Web 密码登录闭环，并执行微信开发者工具/真机交互回归。
- acceptance lane 使用真实后端分别以 Boss/StoreManager/TeamLeader/Marketer/Designer/Pm/ProjectSupport/Finance 登录，完成 Web v2 全业务体系联调和跨范围伪造验证。
- acceptance lane 独立验证 OSS 上传、私有读取、预签名过期、跨企业和跨项目拒绝；运维确认历史 `upload/**` 文件迁移与生产 AccessKey 注入。
- acceptance lane 在真实支付环境验证 JSAPI 预下单、查询、回调验签解密、订单绑定和异常金额/重复通知；运维建立证书与微信支付公钥到期监控。
- acceptance lane 使用生产真实 Designer/Boss/Finance 账号验证设计明细粘贴保存、报价生成导出、增减项批量提交审批，以及批量接口的跨企业越权负向用例。
- 运维执行生产数据库备份恢复与上一版本回滚演练，并建立 TLS、微信支付证书和 API v3 密钥到期监控。
- 法务、产品和小程序管理员提供正式协议文本、AppID、域名与备案材料后执行 `build:mp-weixin:release` 提审门禁。
- 微信真机授权、弱网、401/邀请失效和真机页面回归。

## 2026-09-16 生产懒加载缺陷修复（INC-20260916-a1233597b0）

- 事故：`PlanVersion` `LazyInitializationException`，命中 `project/project/page`、`project/report/page`、`collab/notice/count`，共 42 次（05:40 - 06:04）。
- 根因：生产 `application-prod.yml` 为 `spring.jpa.open-in-view: false`，而 `SubscriptionAccessRequestFilter`（事务外）→ `SubscriptionContext.requireModule` → `effectiveVersion` → `subscription.getPlanVersion().getModules()` 访问未初始化代理；dev/test 默认 OSIV=true 掩盖了该缺陷。
- 源码快照：`dist/source-backups/th4-decoboon-source-20260916-064737.tar.gz`（sha256 `1de1025d1088def66d306bc7ffa8e97f2672a5fc7be2896604dc12dc828026b8`）。
- 修复：`src/main/java/com/th4/edge/tenant/billing/repository/SubscriptionRepository.java` 为 `findByCompany_ComId` 增加 `@EntityGraph(attributePaths = {"planVersion", "plan", "company"})`（单文件、单根因）。
- 回归测试：新增 `src/test/java/com/th4/edge/tenant/billing/SubscriptionLazyLoadingRegressionTest.java`，显式 `spring.jpa.open-in-view=false` 且不加 `@Transactional`；修复前 3/3 失败，修复后 3/3 通过。
- 定向测试：`SubscriptionLazyLoadingRegressionTest`(3) + `SubscriptionBillingClosedLoopTest`(1) + `SubscriptionAccessFilterAcceptanceTest`(4) 全部通过。
- 全量测试：83 项，3 项历史无关失败（`ProductionReleaseBaselineAcceptanceTest` 2 项部署文档断言、`ProviderSecurityAcceptanceTest` 1 项路由数 196→197），已单独记录并证明与本次改动无关。
- 发版：release `dist/releases/20260916-065142` → 生产 `/opt/decoboon/releases/20260916-065204`（上一版 `20260916-053143`），配置校验通过。
- 发版后验证：`decoboon.service` active、HTTPS health 200；10 分钟窗口采集无该指纹新增（最近一次仍为发版前 06:04:02）。
- 规范沉淀：新增 `_系统/协作/lanes/development/workspace/code-error-catalog.md`（ECD-001）；`rules.md` 新增 §4.5 事务边界与懒加载禁则并写入 §8 自检清单；`AGENTS.md` 新增「事务边界与懒加载禁则（强制）」。

## 2026-09-16 生产 OSIV 配置调整发版（open-in-view=true）

- 决策：项目负责人要求将生产 `spring.jpa.open-in-view` 由 `false` 调整为 `true`（配置文件 06:55 修改，未改其他文件）。
- 测试：全量 `mvn -o -q test` 83 项，失败仍为同一组 3 项历史问题（`ProductionReleaseBaselineAcceptanceTest` ×2、`ProviderSecurityAcceptanceTest` ×1），无新增回归；`SubscriptionLazyLoadingRegressionTest` 继续在 `open-in-view=false` 下通过。
- 构建：`dist/releases/20260916-065823`，已校验包内 `config/application-prod.yml: open-in-view: true`。
- 发版：生产 `/opt/decoboon/releases/20260916-065845`（上一版 `20260916-065204`），配置校验通过。
- 复核：服务器生效配置 `open-in-view: true`、release 指向新版本、service active、HTTPS health 200、进程 06:59:33 启动；10 分钟窗口无 LazyInitializationException 新增。
- 风险：OSIV=true 会占用请求级数据库连接（Hikari 默认上限 10）并掩盖懒加载设计问题；`@EntityGraph` 修复保留，规范禁则不放宽。

## 2026-09-16 本地开发密钥加载机制（IDEA 可直接启动）

- 问题：IDEA 直接运行 `com.th4.edge.Application` 不加载 `.env.*.local`，导致小程序微信登录返回 `503 微信小程序 appId/appSecret 尚未配置`；同时 OSS 降级为本地存储、微信支付保持 disabled。代码未变，仅启动方式不同。
- 方案：利用 Spring Boot 自动加载 `file:./config/application-dev.yml`（优先级高于 classpath）的特性，把 `.env.*.local` 转成 Spring 格式的外置 dev 配置，**无需修改 `src/main/resources`**。
- 新增 `scripts/gen-local-dev-config.sh`：白名单提取微信小程序 / OSS / 微信支付 / 文件签名 / Web 首设 token，**排除 DB_*、REDIS_***，生成 `config/application-dev.yml`（权限 600，值不回显）。
- `.gitignore` 新增 `config/application-dev.yml`、`.env.dev.local`。
- 新增文档 `docs/deploy/LOCAL-DEV.md`（IDEA/命令行启动、依赖、自检命令、安全约束）。
- 验证（`env -i` 完全干净环境启动，等同 IDEA 不注入变量）：
  - 启动成功 `Started Application in 10.159s`；
  - `auth/session/login` → `400 微信登录失败：invalid code`（不再是 503 未配置）✅
  - `order/callback` 空请求 → `403 微信支付回调签名缺失`（不再是"商户号...尚未配置"）✅
  - OSS：生成配置 `provider=oss` + access-key 已配置，与支付同文件同源加载；端到端上传验证见 `docs/deploy/LOCAL-DEV.md` §5。
- 安全提醒：`.env.dev.local` 目前仍被 git 索引跟踪（`git ls-files` 可见），仓库尚无 commit；建议 `git rm --cached .env.dev.local` 并轮换其中密钥。

## 2026-09-16 生产后端监控（exit 20，仅归档）

- 运行时间：2026-09-16 13:02:32 +08:00；命令：`bash scripts/prod-monitor.sh collect "35 minutes ago"`。
- 结果：退出码 20；服务 enabled/active/running，HTTPS health 200，根盘 15%，未发现基础设施异常。
- 历史复核：`INC-20260916-a1233597b0` 保持 fixed，最近发生 06:04:02，本次 35 分钟窗口无新增。
- 新发现：`invite/invitation/save` 因请求字段 `telNo` 与 Provider 注入名 `invitePhone` 不一致，导致 `Invite.telNo` 非空约束失败；另行记录授权过期和 `config` 扫描探测两个 unknown。
- 门禁：collect 返回 20，自动修改源码、快照、测试、构建和发版均禁止；未改源码、未重启、未发版。
- 证据与建议：`docs/operations/incidents/2026-09-16/20260916-130232/triage.md`，邀请字段绑定问题保持 open 等待人工确认或符合自动修复门禁的运行。

## 2026-09-16 本地 dev 库数据清空（用户要求）

- 目标：本地 `127.0.0.1:3306/decoboon`（主机 `MacBook-Pro-3.local`，**非生产**）。
- 备份：`/private/tmp/decoboon-backup/decoboon-20260916-131526.sql`（977,939 bytes，mysqldump `--single-transaction --routines --triggers`）。
- 执行：生成 `/private/tmp/truncate-decoboon-all.sql`（`SET FOREIGN_KEY_CHECKS=0` + 87 张表 `TRUNCATE`），执行 0 错误。
- 校验：87 张表结构完整；关键表 company/employee/credential/store/customer/project/quote/catalog_item/plan/notice 全部为 0；对估算非空的 8 张表逐表精确 count，结果全部为空。
- 影响与恢复：
  - 企业/员工/凭证已清空 → 小程序登录将进入"首次登录授权手机号 → 注册企业"流程；
  - 套餐（plan/plan_version/plan_addon）清空 → 重启后端后 `SubscriptionPlanInitializer` 会自动重新 seed；
  - 报价项目库 `catalog_item`（原 554 条）清空 → 需重新执行 `scripts/import-maihong-catalog.py` 导入；
  - 需要恢复时：`mysql -uroot -pmysqladmin decoboon < /private/tmp/decoboon-backup/decoboon-20260916-131526.sql`。

## 2026-09-16 小程序「未加入企业」页优化

- 文件：`../../前端代码/th4-decoboon-app/src/pages/company/join.vue`（重写）。
- 邀请入口：恢复并扩展为三种输入——邀请编号（纯数字）、邀请链接（自动解析 `inviteId`/`token`）、纯 token 串；员工邀请走 `invite/invitation/info`，业主邀请走 `identity/customer-invite/info`，自动分流后跳转 `pages/invite/accept`。
- 按钮改版：原「创建新企业」（primary）+「切换微信账号」（plain）两个裸露按钮，改为底部**并排**两个次要按钮（`jz-btn--ghost` 创建新企业 / `jz-btn--neutral` 切换微信账号），主次分明。
- 规范修正：原页面有 2 个主色实心按钮，违反 `AGENTS.md §5.4`「单页面主色实心按钮 ≤ 1 个」，现仅保留「查看邀请」一个主 CTA。
- 视觉与交互：身份卡（头像首字 + 昵称 + 手机号 + 未归属企业徽标）、邀请卡（标题/说明/输入/主按钮/提示）、回车提交、提交 loading + disabled 防重复、切换账号二次确认；间距均为 8 的倍数，圆角 2 种（卡片 20rpx / 控件 12rpx），卡片具备 background+radius+shadow。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS（DONE Build complete）；`npm run check:subpackages` PASS（主包 395KB / 最大分包 155KB / 总包 1.13MB）；dev watch 已将产物刷新到 `dist/dev/mp-weixin`。

## 2026-09-16 小程序经营驾驶舱「设置目标」交互优化

- 文件：`../../前端代码/th4-decoboon-app/src/subpackages/statistic/pages/analysis.vue`。
- 根因：全局 `.jz-btn` 使用 `display: inline-flex`（宽度自适应内容），而 `.ana-target__save` 只有 `margin-top`、没有任何对齐设置，导致「保存目标」按钮渲染在面板**左下角**且为孤立小按钮，视觉突兀。
- 改法：
  - 目标面板底部改为**右对齐操作区**：`[取消 · jz-btn--neutral] [保存目标 · jz-btn--primary]`，保存按钮位于右侧；
  - 面板头部补充月份标识（标题 + 右侧 `2026-09`）；
  - 四个目标字段增加单位后缀（元 / 个），输入控件统一为「容器背景 + 圆角 + 内嵌 input」结构；
  - `saveTarget` 增加 `savingTarget` 状态，按钮 `:loading` + `:disabled` 防重复提交，并新增 `closeTarget()` 取消路径；
  - 间距修正为 8 的倍数：grid gap `18→16rpx`、`margin-top 22→24rpx`、输入高度 `78→80rpx`、label 间距 `10→8rpx`，面板 padding `24→32rpx`。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev watch 已刷新 `dist/dev/mp-weixin/subpackages/statistic/pages/analysis.wxml`。

## 2026-09-16 小程序 TabBar 遮挡内容修复（全 TabBar 页面）

- 根因：`JzTabBar.vue` 为 `position: fixed; bottom: 0`，实际高度 = 8rpx(padding) + 92rpx(min-height) + 8rpx + `safe-area-inset-bottom` ≈ **108rpx + 安全区**；而各 TabBar 页面底部占位普遍只有 24–64rpx，导致内容滚到底仍被 TabBar 遮挡（首页、我的、动态、项目、财务、人员绩效均受影响）。
- 统一方案：在 `src/styles/base.scss` 新增全局占位类
  `.jz-tabbar-gap { height: $jzb-tabbar-height; padding-bottom: $jzb-safe-bottom; }`
- 逐页修复：
  - `src/pages/index/index.vue`：新增 `<view class="jz-tabbar-gap" />`（原无占位）；
  - `src/pages/finance/index.vue`：新增占位（原无占位）；
  - `src/pages/mine/index.vue`：`.mine__pad`（height 30rpx）→ 全局占位类，并删除原样式；
  - `src/subpackages/statistic/pages/performance.vue`：`.perf__pad`（height 24rpx）→ 全局占位类，并删除原样式；
  - `src/pages/dynamic/index.vue`：`.dy__body` 底部 `40rpx` → `calc($jzb-tabbar-height + $jzb-safe-bottom + 24rpx)`；
  - `src/pages/project/list.vue`：`.pl__body` 底部 `64rpx` → 同上 calc 表达式。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev watch 已刷新产物，`app.wxss` 含全局类、4 个页面 wxml 含占位节点。

## 2026-09-16 小程序「成员管理」页交互优化

- 文件：`../../前端代码/th4-decoboon-app/src/subpackages/company/pages/members.vue`（重写，209 行）。
- 筛选栏：三个独立主色实心方块（选中态深绿底白字、`gap: 14rpx`）→ **分段控件**（白底容器 + 8rpx 内边距 + 选中态 `$jzb-primary-lighter` 浅底 + 主色文字），点击区域由 `<text>` 改为 `<view>`（min-height 72rpx），新增 `switchFilter()` 避免同项重复请求。
- 成员卡片：原右侧竖排三个 22rpx 裸文字（点击区域极小、与信息抢位）→ 拆分为「信息区 + 底部等宽操作区」：
  - 信息区：头像 + 姓名 + 状态标签 + 角色/门店/团队 + `chevron-right`，整块可点进详情；
  - 操作区：`border-top` 分隔，每项 `flex:1`、`min-height: 88rpx`、带图标，项间 1rpx 分隔线，`:active` 背景反馈；
  - 状态分支：在职=`交接/停用/注销`，停用=`恢复在职/注销`，已离职=`已离职，仅可查看`（只读提示），危险操作（注销）使用 `$jzb-danger`。
- 其他：卡片由「一张大卡片包多行」改为「每成员一张卡片 + 16rpx 间距」，卡片具备 background+radius+shadow；间距统一 8 的倍数（原 14rpx/26rpx/12rpx/10rpx/6rpx 全部修正）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS；dev watch 已刷新 `members.wxml`。

## 2026-09-16 小程序「新建/编辑客户」表单对齐与地址展示修复

- 文件：`../../前端代码/th4-decoboon-app/src/subpackages/customer/pages/edit.vue`。
- 根因 1（性别/客户来源值不靠右）：`<picker>` 作为 flex 子项没有 `flex: 1`，宽度收缩到内容宽度，导致内部 `.ce__picker { justify-content: flex-end }` 没有可分配空间，值只能贴着 label 显示。
- 根因 2（房屋地址显示不全且不靠右）：地址行使用 `.ce__row--col`（`flex-direction: column; align-items: flex-start`），input 在交叉轴宽度收缩，且与其他字段布局不一致。
- 修复：
  - 新增 `.ce__row picker { flex: 1; min-width: 0; }`，picker 撑满剩余宽度，选中值真正右对齐；
  - `.ce__picker` 增加 `gap: 8rpx` 与文本溢出保护（`overflow/text-overflow/white-space`）；
  - 房屋地址改回横向行（与姓名/手机号/意向小区一致），input 沿用 `flex: 1; min-width: 0; text-align: right`，宽度从"内容宽"变为"占满剩余宽度"；`maxlength=200`，placeholder 精简为"详细地址，转待量房前必填"；
  - 纵向行样式仅保留给「备注」textarea；间距修正：`padding: 18→16rpx`、提示条 `padding: 20→24rpx`。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev watch 已刷新 `edit.wxml/edit.wxss`（含 `.ce__row picker` 规则与新 placeholder）。

## 2026-09-16 小程序「客户列表」层次感与客户卡片优化

- 文件：`../../前端代码/th4-decoboon-app/src/pages/customer/list.vue`（仅模板展示结构与样式，业务逻辑未改动）。
- 客户卡片重构为**三层结构**：
  1. 主行 `cl__item-head`：头像 + 姓名 + 阶段标签 + 电话（左），**跟进状态右对齐独立显示**（逾期/今日/下次，按 tone 着色）；
  2. 信息行 `cl__item-facts`：`border-top` 分隔，图标 + 小区 · 面积 · 预算（无值时降级占位，预算为空不显示）；
  3. 标签行 `cl__item-tags`：微信/到店/量房/设计（或待指派设计）/已转项目，最多 5 枚，`gap: 8rpx` 换行；
  4. 营销角色额外一行 `cl__item-foot`：黄金时段标签 + 录入天数。
  - 删除原右侧竖排的 `chevron-right + 已转项目 + 跟进文案` 混排结构（信息竞争、阅读跳跃），以及不再使用的 `.cl__who/.cl__concept-tags/.cl__right/.cl__meta-line/.cl__project/.cl__gender` 样式（残留检查为 0）。
- 页面层次与间距：
  - 卡片由「flex 横排 + 内嵌多行」改为「上下分层 + 分隔线」，单卡片信息分三档字号/颜色，主次清晰；
  - 卡片间距 `18→16rpx`、内边距 `26→24rpx`；搜索框高 `84→88rpx`；筛选 chip `62→64rpx`、右间距 `14→16rpx`；跟进指标条 padding/margin 全部归一；
  - 全页非 8 倍数间距（12/18/22/14/26/10/7/6/4/28/150rpx）统一修正为 8 的倍数（16/24/32/152rpx 等）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 395KB / 总包 1.13MB）；dev watch 已刷新 `list.wxml`（含 `cl__item-head`、`cl__item-facts`、`cl__item-tags`）。

## 2026-09-16 小程序客户详情链路体验优化（详情/跟进 + 2 个新页面）

- 全局修复：`src/styles/base.scss` 的 `.jz-section__more` 原为纯文本样式（无 `display:flex`），区块标题右侧的「新增房屋」「编辑资料」出现**图标与文字换行**；改为 `inline-flex + align-items:center + gap:8rpx + flex-shrink:0`，全站同类入口一并修复。
- 客户详情 `src/subpackages/customer/pages/detail.vue`：
  - 顶部客户卡片重构为**两层**：上层「头像 + 姓名 + 阶段标签 + 电话（带图标）」；下层 `border-top` 分隔的「负责顾问 / 接手设计师」双列归属区，替代原先平铺 4 行文本；
  - **约定量房前置校验**：无房屋档案时弹窗引导「请先建立房屋档案 → 新增房屋」，不再允许在无地址情况下直接进入量房约定；
  - 「新增房屋」「记录加微信」由 `uni.showModal` 内联弹窗改为**跳转独立页面**；
  - 「记录加微信 / 记录到店」由 `jz-btn--text` 小文字按钮改为**等宽卡片按钮**（`jz-btn--ghost` + `jz-btn--neutral`，带图标，flex:1）；
- 新增页面（已在 `pages.json` 分包注册、`config` 增加路径常量）：
  - `src/subpackages/customer/pages/property-edit.vue`「新增房屋」：顶部客户信息卡（姓名/电话）+ 详细地址（必填）/小区/楼栋/房号/面积/户型/备注，提交 loading+disabled 防重复；
  - `src/subpackages/customer/pages/wechat-record.vue`「记录客户微信」：顶部客户信息卡（含当前微信/到店状态）+ 微信号输入 + 说明，支持首次记录与更新。
- 跟进页 `src/subpackages/customer/pages/follow.vue`：
  - 顶部展示**客户基础信息**（头像、姓名、电话、小区、阶段标签）；
  - 新增**上次跟进卡片**（跟进方式、时间、内容、上次约定的下次跟进日期），并默认带出上次约定的跟进日期作为本次选择值；
  - 跟方式 chip 与表单间距统一为 8 的倍数（原 12/14/30/20rpx 等）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 396KB / 分包页 51→53 / 总包 1.15MB）；dev watch 已生成两个新页面产物。

## 2026-09-16 小程序 release 构建并上传微信平台

- 命令：`npm run build:mp-weixin:release`（type-check → check:release-env → build → check:subpackages），四道门禁全部 PASS。
- 产物：`../../前端代码/th4-decoboon-app/dist/build/mp-weixin`，主包 396,161 B、最大分包 155,523 B、总包 1,146,678 B，主包 12 页 + 分包 12 个 / 53 页。
- 生产配置：`VITE_APP_ENV=production`、`VITE_API_BASE=https://decoboon.joytsing.com/api/`、法务主体「安徽昭青网络科技有限公司」/`decoboon@joytsing.com`、`VITE_LEGAL_REVIEWED=true`、AppID `wxe5cc5fec9e50a9a9`。
- 上传：`cli upload --version 1.0.0 --desc "客户详情/跟进链路优化、成员管理交互重构、客户列表卡片分层、TabBar遮挡修复、新增房屋与记录微信独立页面"` → 返回 `✔ upload`（微信平台开发版本）。
- 本次版本包含：客户详情卡片分层与量房前置校验、新增「新增房屋」「记录客户微信」独立页面、跟进页客户信息+上次跟进、成员管理分段筛选与卡片操作区、客户列表三层卡片、TabBar 底部遮挡修复（全局 `jz-tabbar-gap`）、客户表单右对齐修复、经营驾驶舱目标按钮右对齐、`.jz-section__more` 图标换行全局修复。
- 待人工：微信公众平台 → 版本管理 → 设为体验版 / 提交审核 / 发布（需管理员账号操作，CLI 不具备该权限）。

## 2026-09-16 邀请页「已授权自动进工作台」

- 文件：`../../前端代码/th4-decoboon-app/src/pages/invite/accept.vue`。
- 背景：员工通过邀请链接授权手机号并加入企业后，前端会 `reLaunch` 到工作台；但**再次点开同一邀请链接**时，后端返回 `status=Accepted`，页面只显示「邀请已使用」且无任何操作入口，用户被困住。
- 改动（`onLoad` 内新增两段判定）：
  1. **业主侧**：`isCustomerInvite && session.token && session.role === 'customer'` → 直接 `reLaunch(P.home)`（已完成绑定不再重复授权）；
  2. **员工侧**：`loadInvite()` 成功后，若 `!isCustomerInvite && session.token && session.role && invite.status === 'accepted'` → 直接 `reLaunch(P.home)`；
  3. 邀请仍为 `pending` 时保持原流程（含"已在他企 → 确认切换企业"分支），不破坏切换企业能力。
- 依据核验：后端 `MembershipContext.requireInvite()` 不校验邀请状态（仅处理过期），状态校验只在 `accept()` 中，因此已接受邀请仍可查询到 `Accepted`，前端 `normalizeInvite` 映射为 `accepted`，判定可靠。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS；dev 产物 `pages/invite/accept.js` 已含 `session.role` 判定与 `reLaunch`。
- 待办：本次改动尚未上传微信平台（上一版 1.0.0 已上传），如需生效需重新执行 release 构建 + 上传。

## 2026-09-16 工作台首页：顶部锚定 + 企业展示强化 + 微信头像 + 老板视角

- 组件增强：
  - `src/components/JzAvatar.vue`：新增 `src` 属性（微信头像），`<image mode="aspectFill">` 渲染，加载失败自动回退姓名首字，`watch(src)` 重置失败态；
  - `src/components/JzPageHeader.vue`：新增 `fixed` 属性，固定模式下 `.jz-header__bar` 为 `position: fixed` 并自带渐变底色与投影，滚动时不随内容滚走。
- 首页 `src/pages/index/index.vue` + `src/styles/home.scss`：
  - Header 启用 `fixed`（顶部锚定），副标题改为 `orgLine`（企业全称）强化企业展示；
  - 原「角色横幅」升级为**个人信息卡**：微信头像（112rpx）+ 姓名 + 角色标签 + 企业行（building 图标）+ 门店/团队行（users 图标），角色文案区分「全公司经营视图 / 门店经营视图 / 团队经营视图」；
  - 项目区块标题按角色动态：BOSS=`企业项目 · 全公司`、店长=`门店项目`、团队长=`团队项目`、其他=`我的项目`、业主=`我的家 · 施工进度`；
  - 新增 computed：`avatarUrl`（emp.user → session.user 回退）、`orgLine`、`orgSubLine`、`projectSectionTitle`。
- 微信头像接入（员工侧 6 处，客户头像保持字母，因客户无微信头像）：
  `pages/index/index.vue`、`pages/mine/index.vue`、`subpackages/team/pages/list.vue`、`subpackages/team/pages/designers.vue`（设计师/绩效 2 处）、`subpackages/project/pages/members.vue`、`subpackages/project/pages/detail.vue`（2 处）、`components/JzReportCard.vue`（动态作者）。
  - 注：`designers.vue` 的「待指派客户」列表属客户对象，已回退为字母头像（type-check 捕获后修正）。
- BOSS 视角核验：
  - 数据层：`domains/project/scope.ts` 的 `isProjectVisible` 对 `boss/storeManager/teamLeader/finance` 直接返回 true，故项目与动态本就为全公司范围，本次修正的是「我的项目」文案误导；
  - 权限层：`roles/catalog.ts` 中 BOSS 快捷入口为经营驾驶舱/成员管理/人员绩效/客户管理/合同签约/邀请成员/我的团队/财务总览/消息通知/全部项目，**不含任何设计类业务**（设计调度属店长/团队长，设计业绩属设计师），符合"BOSS 不做设计师业务"。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 398KB / 53 页）；组件产物 `JzPageHeader.wxss` 含 `jz-header--fixed`，`JzAvatar.wxml` 含 `jz-avatar__img`。
- 待办：本次改动尚未上传微信平台，如需生效需重新 release 构建 + 上传。

## 2026-09-16 门店维度（经营驾驶舱 / 成员管理 / 人员绩效）

- 后端能力：
  - 新增 `user/store/list` Provider（`tenant/user/service/user/store/list`）：企业负责人返回企业全部门店，店长/团队长/普通员工仅返回本门店，支持 `currentOnly` 强制单店；
  - `BossDashboardQuery` 增加 `storeId`，`BossDashboardContext.dashboard()` 按门店过滤项目/合同/回款/应付（四类实体均有 `store_id` 关联）；
  - `PerformanceQuery` 增加 `storeId`，`PerformanceReportContext.report()` 按门店过滤人员与报价/客户/项目口径（用 final 变量避免 lambda 捕获问题）；
  - `company/member/page` 增加 `storeId` 过滤，在既有角色数据范围（scoped）内再限定门店，不突破权限边界。
- 前端接入：
  - 新增 `components/JzStoreFilter.vue`：门店选择器（`全部门店 + 各门店`），**门店数 ≤ 1 时自动不渲染**；
  - `identityApi.storeList()` 与 `memberPage({ storeId })`、`statisticApi.bossDashboard(month, storeId)`、`statisticApi.performance(month, role, storeId)`；
  - 接入页面：经营驾驶舱 `subpackages/statistic/pages/analysis.vue`、成员管理 `subpackages/company/pages/members.vue`、人员绩效 `subpackages/statistic/pages/assessment.vue`。
- 测试与回归：
  - `ProviderSecurityAcceptanceTest` 路由总数同步 197→198、protected 186→187（新增 store/list）；
  - **发现并修复本地密钥机制副作用**：`config/application-dev.yml`（含 OSS 配置）会被未指定 profile 的测试加载，导致 `OwnerFileExposureAcceptanceTest` 期望的本地签名链接变成 OSS 预签名 URL；已在该测试显式固定 `th4.edge.components.storage.provider=local`；
  - 结果：`mvn -o -q test` → 83 tests / 0 failures / 0 errors；前端 `type-check`、`build:mp-weixin` 均 PASS。
- 待办：客户列表（`customer/customer/page`）门店维度未接入——该 Provider 走框架 `AbstractPage<Customer>`，需单独评估分页扩展点后再加 `storeId` 条件。

## 2026-09-16 B 方案：客户分页显式 Provider + 门店维度

- 背景：客户分页原继承框架 `AbstractPage<Customer>`，无法安全接收门店筛选（直接暴露 `storeIdEq` 缺少服务端强校验）。按既有已验证模式（同成员分页）改为显式 Provider。
- 后端：重写 `decoboon/customer/service/cust/page/support/Page.java`
  - 通过 `DataScopeContext.requireScope()` 复用与 `DataScopeRequestFilter` 完全一致的数据范围：企业负责人=全公司、店长=本门店、团队长=本团队、普通员工=本人负责客户（分别调用 `CustomerRepository` 对应的四种查询）；
  - 新增 `@AutoInject Long storeId`，在企业负责人数据范围内按门店收窄；非企业负责人忽略前端传值、固定本门店（避免越权）；
  - 内存分页（与成员分页同构），页大小上限 200。
- 前端：`crmApi.page` 的 `CustomerPageQuery` 增加 `storeId` 并透传查询参数；`pages/customer/list.vue` 接入 `JzStoreFilter`（门店 ≤1 自动隐藏）+ 首屏/翻页均带 `storeId`。
- 已回退的方案：注解 + 过滤器的通用门店拦截（`@StoreScope` + `StoreScopeCriteria` + `StoreScopeRequestFilter`）在启用后触发 `ApplicationContext` 加载异常（`dataScopeContext` Lookup method resolution failed / `FileAuthorizationContext$Authorization` NoClassDefFoundError，clean 重编译后仍复现），为保持代码库可发布状态已回退，未留半成品。
- 验证：`mvn -o -q clean test` → 84 tests / 0 failures / 0 errors；前端 `type-check`、`build:mp-weixin` 均 PASS。

## 2026-09-16 装得清运营管理端（历史方案，已被独立平台组织重构取代）

- 输入：用户要求构建 `th4-decoboon-admin`，覆盖全平台企业运营、订阅人工管控、营销推广支持和系统运维监控。
- 前端：完成 Vite + Vue 3 + TypeScript + Element Plus 工程，含运营总览、客户企业、订阅续费、增长运营、套餐策略、系统运维、操作审计 7 个模块。
- 后端：新增 `com.th4.edge.platform`，实现企业分页/详情、运营总览、套餐列表、人工订阅调整、操作审计、系统状态 7 条 Provider。
- 安全：`Company.platform=true` + `EmployeeRole.Boss` 白名单；订阅写操作强制原因与审计；普通租户请求返回 403。
- 数据：新增 `Company.platform` 和 `db_platform_operation_log`，同步增量迁移与空库基线。
- 验证：前端 `type-check`/`build` PASS，浏览器 7 路由及订阅调整交互 PASS；后端 `mvn -o -q clean test` 为 29 suites / 84 tests / 0 failures / 0 errors；Provider Security 路由总数为 205。
- 交接：`_系统/协作/lanes/development/workspace/admin-console-handoff.md`。
- 风险：尚未在生产执行迁移、创建平台运营账号和做真实 HTTPS 验收。

## 2026-09-16 金额单位自适应 + 财务模块视觉统一

- 金额展示（全局）：
  - `core/format.ts` 的 `money()` 改为**万元/元自适应**：`|n| ≥ 10000` 显示 `12.34万`，否则显示 `¥9,800`；新增 `moneyParts()` 返回 `{ value, unit, text, unitKey }`；
  - 新增 `components/JzMoney.vue`：数值与单位分离渲染，**万元单位用品牌色、元单位用次要灰**（单位字号 0.72em），满足"单位颜色差距"要求；
  - 由于 `money()` 是全局函数，25 个引用文件一次性获得单位自适应能力。
- 财务工作台（`pages/finance/index.vue`）：风险待办的「催收 / 待付」由两个裸文字 + 内联 `#3568C7` 改为**分段控件**（`.fin-urgent-tabs`：灰底容器 + 选中态品牌浅底 + 品牌色文字，间距 8rpx），样式落在 `styles/finance.scss`；撤收/待付卡片金额改用 `<JzMoney>`。
- 品牌色统一：批量替换 `#3568C7 → #0E8A7D`（品牌深色）、`#EEF3FF / #EEF3FC → #E8F7F5`（品牌浅底），共 **10 个文件**，消除财务卡片与主题色不一致的蓝色。
- 表单对齐（`styles/finance.scss`）：根因是 `.fin-field input` 缺少对齐声明（picker 的 `.fin-field__value` 已右对齐），导致"标题靠左、录入靠左"的错位；现改为 **input 右对齐、textarea 保持左对齐**，与 `新建客户` 表单口径一致。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 / 53 页）。

## 2026-09-16 财务配色与表单对齐（二次修复 + 开发者工具验证）

- 用户反馈上次修复未生效，复查后定位到两个真实根因：
  1. **财务卡片蓝色并非 `#3568C7`**：实际来源是蓝紫渐变 `#2F54B8` / `#243E8A`（`styles/finance.scss` 财务头部渐变与筛选选中态、`subpackages/finance/pages/project.vue` 的表头/结算卡渐变）；本次统一替换为品牌色 `#0E8A7D` + `#096C64`，阴影 rgba 一并调整；
  2. **表单右对齐漏在全局 `jz-field`**：上次只改了财务专用的 `.fin-field input`，而其它模块的新增/录入表单用的是全局 `.jz-field`（`styles/base.scss`），其 `input` 同样缺少对齐声明；本次给 `.jz-field input` 补 `text-align: right`（textarea 保持左对齐）。
- 验证（微信开发者工具实机访问，`cli auto` + miniprogram-automator）：
  - `pages/finance/index` 正常渲染：当前账面现金余额 / 项目合同额 / 逾期 / 待收工程款 / 本月回款 / 待付账款 等指标卡可读；
  - `subpackages/finance/pages/entry`（新增应收节点）正常渲染，字段为「所属项目 / 节点名称 / 施工节点 / 应收类型 / 应收金额（元）/ 约定收款日 / 需要开票 / 备注」，标题与录入分离；
  - 截图：`/private/tmp/mp-automator/out/finance-index.png`、`finance-entry.png`。
- 产物核对：`dist/dev/mp-weixin/subpackages/finance/pages/project.wxss` 中品牌色 11+3 处、`#2F54B8` 0 处；`app.wxss` 含 2 处 `text-align: right`；全仓 `grep` 已无 `#2F54B8 / #243E8A / #3568C7` 残留。
- 说明：本地数据库已清空，页面金额均为 0（<1 万 → 元单位），万元分支逻辑在 `core/format.ts` 中实现但未在本次实机覆盖。

## 2026-09-16 财务表单 picker/switch 右对齐（第三次修复，含开发者工具实测）

- 用户指出「所属项目 / 应收类型 / 约定收款日 / 需要开票」仍未靠右。根因与客户表单同源：
  1. `<picker>` 作为 `.fin-field` 的 flex 子项**没有 `flex: 1`**，宽度收缩到内容宽度，其内部 `.fin-field__value { flex:1; text-align:right }` 无处伸展 → 值贴左；
  2. 「需要开票」用的是 `<switch>`，**没有 `margin-left: auto`**，紧贴标签靠左。
- 修复：`styles/finance.scss` 增加 `.fin-field picker { flex:1; min-width:0 }`、`.fin-field switch { margin-left:auto; flex-shrink:0 }`；`styles/base.scss` 同步为 `.jz-field picker { flex:1; min-width:0 }` 并补 `.jz-field__value` 右对齐 + 省略号（全站表单统一）。
- 验证：
  - 编译产物 `dist/dev/mp-weixin/subpackages/finance/pages/entry.wxss` 已含 `.fin-field picker.data-v-*`、`.fin-field switch.data-v-*` 规则（21:10 刷新）；
  - 微信开发者工具实测（miniprogram-automator）：`subpackages/finance/pages/entry` 正常渲染，窗口宽 390px，「所属项目」label.left=26（标题靠左）、value 元素 left=119 且 `flex:1 + text-align:right`（录入靠右），值文本「企业通用账务」；
  - 截图：`/private/tmp/mp-automator/out/entry-align-2.png`。

## 2026-09-16 运营管理端真实后端化与组织设计

- 移除 `VITE_DEMO_MODE`、mock Provider、演示账号和断网回退。
- 新增 `npm run verify:real-api` 构建门禁，校验 7 个平台 Provider 和真实 API 配置。
- 新增默认关闭的 `PlatformBootstrap`：显式提供 `PLATFORM_BOOTSTRAP_*` 时创建独立平台组织、部门和 SuperAdmin，不使用租户 Employee/Company。
- 新增 `docs/product/装得清运营管理端用户组织与权限设计.md`，覆盖独立组织、岗位、权限矩阵、数据范围、职责分离和账号生命周期。
- 新增账号初始化和实时后端对接运维文档。
- 验证：前端 `verify:real-api` + `type-check` + `build` PASS；后端编译和平台/Provider 安全测试 PASS。

## 2026-09-16 本地版本提交 1.0.1（后端 + 前端双仓库）

- 后端仓库 `后端代码/th4-decoboon`（根目录正确）：
  - `00ba624 chore(release): 本地版本提交 1.0.1`（首提交，44 文件）
  - `f5a0c57 chore(release): 补全 1.0.1 本地版本（源码/文档/示例配置）`（补全 995 文件，含 src/main、src/test、docs、_系统、.agents 与 `.env.*.example` 模板）
  - 提交前补充 `.gitignore`：`logs/`、`data/`、`__pycache__/`、`*.pyc`；清理已暂存的 `scripts/__pycache__`
  - 安全核验：`.env.dev.local`、`.env.oss.local`、`.env.wechat-pay.local`、`config/application-dev.yml`、`config/wechat-pay/*.pem` 全部 IGNORED；提交的 `.env.*.example` 为模板且不含真实密钥
  - 最终工作区：干净（0 未提交）
- 前端仓库 `前端代码/th4-decoboon-app`：
  - 首次确认时该目录**没有 `.git`**，`git rev-parse --show-toplevel` 指向 `/Users/zhangwei` 主目录仓库（存在误提交主目录的高风险）；随后确认已具备独立 `.git`（根目录校验通过）后执行首次提交
  - `6b30de8 chore(release): 初始化前端工程并提交 1.0.1`（232 文件），工作区干净
  - `.gitignore` 已忽略 `node_modules`、`dist`、`unpackage`、`*.local`、`.env.local`；敏感内容扫描（appsecret / access-key / private-key）无命中
  - 版本号：`src/manifest.json` → `versionName 1.0.1` / `versionCode 101`
- 状态：**仅本地提交，未发版**（用户要求先完成企业负责人功能梳理）

## 2026-09-16 运营管理端独立平台组织重构（架构修正）

- 用户要求：运营业务领域必须与多租户装企业务体系彻底隔绝，数据不得交织和关联。
- 平台认证：改为独立 `platform/auth/login|info|logout`，token audience 为 `PlatformUser`；租户 Employee token 不再能承载平台身份。
- 平台组织：新增独立 `PlatformOrg`、`PlatformDepartment`、`PlatformUser`、`PlatformRole`、`PlatformPermission` 和平台独立 BCrypt 密码。
- 数据隔离：删除 `Company.platform`；平台表不建立指向 `db_tenant_company`、`db_tenant_employee`、`db_tenant_credentials` 的外键。
- 审计隔离：`PlatformOperationLog` 只引用平台组织和平台用户；租户企业ID、企业名称和订阅ID仅保存标量快照。
- 跨域边界：租户业务只允许由 `TenantDataAdapter` 转换为 `PlatformTenant/PlatformSubscription/PlatformPlan` 快照，平台实体不持有租户实体。
- 初始化：`PlatformBootstrap` 只创建平台独立组织、部门、SuperAdmin 和 BCrypt 密码。
- 验证：`mvn -o -q clean test` = 29 suites / 85 tests / 0 failures / 0 errors；`PlatformAdminAcceptanceTest` 验证平台表无租户外键；Provider Security = 208 routes（public 12 + protected 196）。

## 2026-09-16 订阅中心：订单记录操作按钮降级

- 反馈：订单与续费记录下的「查询结果 / 继续支付」按钮突兀。
- 根因：每笔待支付订单都渲染一个 `jz-btn--primary`（主色实心）按钮，导致页面出现多个主 CTA（违反 `AGENTS.md §5.4` 单页主色实心按钮 ≤ 1），且与订单金额挤在同一行、`gap:10rpx` 非 8 倍数。
- 修复（`subpackages/subscription/pages/index.vue`）：
  - 「继续支付」→ `jz-btn--ghost`（描边，次要）；
  - 「查询结果」→ `jz-btn--text`（纯文字，最轻）；
  - 操作区间距 `10rpx → 16rpx`，新增 `flex-shrink: 0` 防止被金额挤压；
  - 订单卡片的层级恢复为「套餐/订单号/状态 → 明细 → 金额+时间 → 操作」。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev 产物 `index.wxml` 已含 `subscription__order-query` 与 `jz-btn--ghost`。
- 备注：支付相关后端修复见提交 `a004f15`（微信支付 amount.total 被 JacksonLongConfigure 序列化为字符串的根因）。

## 2026-09-16 订阅中心：hero 卡片与头部间距/配色层次

- 反馈：当前订阅卡片与头部没有间距，且颜色与头部显得突兀。
- 根因（`subpackages/subscription/pages/index.vue`）：
  1. `.subscription__content { padding: 0 $jzb-page-pad; }` 上下无内边距 → hero 卡片紧贴 `JzPageHeader` 的渐变头部；
  2. hero 背景 `linear-gradient(135deg, #0E766E, #16A394, #52BFAF)` 与头部 `linear-gradient(135deg, #169C90, #16A394, #0C776E)` 同为深绿渐变 → 两块深绿直接相接，视觉上像被切开。
- 修复：
  - `.subscription__content` 顶部内边距 `0 → 24rpx`（8 的倍数），卡片与头部之间露出页面底色；
  - hero 渐变改为**更亮的中绿** `#12A08F → #2BC4B4 → #6FD3C6`，与深绿头部形成「深→浅」层次；阴影由 `0 20rpx 50rpx rgba(14,118,110,.20)` 调整为 `0 16rpx 36rpx rgba(14,138,125,.16)`，避免与头部阴影叠加显得厚重。
- 保留：卡片圆角、`hero-glow` 装饰、白字与内部结构（用户反馈卡片设计本身没问题）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev 产物 `index.wxss` 已含新 padding 与新渐变、旧深绿 `#0E766E` 计数为 0。

## 2026-09-16 IDEA 平台运营启动与本地数据同步

- IDEA：新增并选择 `平台运营开发环境` Spring Boot 配置；首次账号创建使用 `平台运营初始化` 配置。
- 数据：清理本地旧 `db_tenant_company.platform` 和融合审计表，执行独立 `db_platform_*` 迁移；清理前备份到 `/private/tmp/decoboon-before-platform-sync.sql`。
- 账号：创建独立 `PlatformOrg / PlatformDepartment / PlatformUser`，手机号 `18660219918`，岗位 `SuperAdmin`，密码只保存在本地 IDEA 配置且已 Git 忽略。
- 验证：IDEA 中后端运行、`platform/auth/login` 成功、管理端真实登录成功、租户列表显示本地“麦鸿设计”、系统运维加载 MySQL/Redis/应用进程；平台到租户外键数 0。
- 文档：`docs/operations/2026-09-16-IDEA平台运营启动与本地数据同步.md`。

## 2026-09-16 报价项目库 P0：编号后端自动生成

- 规则：`{类型前缀}-{企业内 4 位序号}`（施工 SG / 主材 ZC / 软装 RZ / 电器 DQ / 综合 ZH / 定制 DZ），序号按「企业 + 类型」独立递增。
- 后端：
  - 新增 `catalog/context/CatalogCodeContext`：`nextCode(comId, type)` 取同企业同类型最大编号 +1；非本前缀的自定义编号回退序号 0 后 +1；
  - `CatalogItemRepository` 增加 `findFirstByCompany_ComIdAndTypeOrderByCodeDesc`；
  - `catalog/item/save` 的 Save Provider：**code 为空/空白时后端生成**，前端传入则保留（兼容企业自定义命名）；
  - 迁移脚本 `docs/design/migrations/24-报价项目编号自动生成.sql`：空串规范为 NULL + `(com_id, type, code)` 唯一约束，并附历史重复数据自查 SQL。
- 前端：app `catalog/manage.vue` 编号占位文案改为「编码（留空自动生成）」；web `resources/index.ts` 表单字段 label 同步为「编码（留空自动生成）」。
- 验证：后端 `mvn -o -q compile` PASS；`DecoboonClosedLoopTest` 1 test 0 failures；app / web `type-check` 均 PASS。
- 提交：后端 `e58c82b`；app 单独提交（web 因无独立仓库未提交，见仓库结构说明）。
- 下一步：报价库 P1（拼音缩写 + 拼音检索）。

## 2026-09-16 报价项目库 P1：拼音首字母与拼音检索

- 依赖：引入 `pinyin4j 2.5.1`（pom.xml）。
- 数据模型：`catalog_item.name_initials VARCHAR(32)`（@Keyword）+ 索引 `idx_catalog_item_initials (com_id, name_initials)`。
- 后端：
  - 新增 `catalog/context/PinyinContext`：中文取拼音首字母（「轻钢龙骨石膏板吊平顶」→ `QGLGSGBDDP`），字母数字保留大写；
  - `catalog/item/save` 与 `catalog/item/update` 均在写入 name 后自动刷新 `nameInitials`；
  - `catalog/item/page` 的 keyword 检索由 5 字段扩展为 7 字段 OR：新增 `nameInitials`（拼音检索）与 `series`（原先遗漏），大小写不敏感。
- 迁移：`docs/design/migrations/25-报价项目拼音检索.sql`（加列 + 索引 + 存量回填说明，拼音需应用层回填）。
- 验证：`mvn -o -q compile` PASS；`DecoboonClosedLoopTest` 1 test / 0 failures。
- 提交：后端 `feat(catalog): 报价项目拼音首字母与拼音检索（P1）`。
- 仓库治理：Web 工程已完成独立化（`fb70802` 首次提交，170 文件，node_modules/dist 已忽略）。
- 下一步：报价库 P2（分类明细动态渲染）。

## 2026-09-16 Web v2 业务交互升级

- 目标：将 Web 从通用资源表格升级为面向业务人员的工作台，参考 App 客户详情和经营链路，减少技术 ID、错误枚举和无效筛选。
- 新增：`src/resources/options.ts`（业务实体选择器数据源）、`src/resources/dictionaries.ts`（统一业务字典）。
- 重构：资源页支持 entity 选择器、依赖字段联动、URL 上下文继承、业务衔接提示、条件空态和业务动作标签；客户详情改为销售阶段 + 房屋工作台 + 下一步动作。
- 修正：客户/项目/现场动态分页 Provider 的业务筛选；任务取消状态、材料计划状态、量房字段、客户预算单位、消息类型、订阅订单、业绩角色、项目/报价状态映射。
- 验证：Web type-check/verify:system/build PASS；后端 Maven 29 suites / 85 tests / 0 failures / 0 errors。
- 下一步：部署前由 acceptance 在真实后端账号下验证客户详情、实体选择器交互、查询条件和跨角色数据范围。

## 2026-09-17 报价项目库 P3/P4 完成

- P3 成本价角色保护（后端 `23399c1` / app `90dfa5f`）：
  - 新增 `CatalogCostContext`：仅 Boss / StoreManager / Finance 可见成本价；
  - `catalog/item/page|save|update` 返回前统一裁剪，无权角色 `costPrice` 置空 →
    配合 `non_null` 序列化策略，**响应中不出现该字段**（服务端裁剪，非前端隐藏）；
  - app 端成本价从三连输入行拆出为独立敏感区块（锁图标 + 仅管理层可见 + 橙色警示底 + 说明文案），无权限角色不渲染。
- P4 价格模式展示（app，前端）：
  - 新增 `pricePreview`：按 `pricingMode` 即时展示对外报价口径
    （`材料 ¥120 + 人工 ¥45 = ¥165/㎡` / `材料 ¥120/㎡` / `人工 ¥45/㎡` / `固定总价 ¥1,200（不按工程量折算）`）；
  - 在材料/人工价输入下方展示「报价展示」预览行。
- 验证：后端 `mvn -o -q compile` PASS；app `type-check` + `build:mp-weixin` PASS。
- 待办：Web 端 P2（条件字段组）+ P4（价格展示列）需先扩展通用资源页 `ResourceListView` 的字段能力；
  之后进入供应链 P0（供应商应付台账）。

## 2026-09-17 Web 端报价项目库 P2/P4 补齐

- `ResourceListView`（通用资源页）扩展：支持 `form.fieldsByValue` —— 按表单中某个键的当前值（如
  `type`）动态追加该类型专属字段组；未声明 `fieldsByValue` 的资源页行为完全不变（不影响现有 30+ 页面）。
- 报价项目库（`src/resources/index.ts`）：
  - 表单按类型追加明细：施工=施工工期；主材=损耗率+起订量；软装=交期；电器=保修月数；定制=柜体板材+台面；
  - 补充材料单价 / 人工单价表单字段；
  - 列表新增「报价展示」列，使用既有 `ResourceColumn.formatter` 能力（无需改模板）：
    材料+人工 / 仅材料 / 仅人工 / 固定总价四种口径自动组合。
- 验证：Web `type-check` PASS；`npm run build` PASS（ResourceListView chunk 56.78 kB）。
- 提交：Web `1bde9b9`。
- 差异说明：Web 端明细字段当前为「按类型追加到表单末尾」，**尚未做折叠 + 记忆展开状态**（小程序端已实现折叠与记忆）；
  如需两端完全一致，需在 ResourceListView 表单区引入 `el-collapse` 并持久化展开状态，作为后续小优化项。
- 报价库 P0–P4 至此**后端 + 小程序 + Web 三端齐备**（P4 Web 通过 formatter 实现）；下一步进入供应链 P0（供应商应付台账）。

## 2026-09-17 Web 表单弹窗改抽屉

- 反馈：弹窗缺少合理的边框和间距，长表单体验差。
- 新增 `BusinessDrawer.vue`，统一右侧抽屉标题、说明、滚动正文和粘性底部操作区。
- 资源表单、客户跟进/房屋/量房/设计师指派、设计区域表单全部由 `el-dialog` 改为 `BusinessDrawer`。
- 统一遮罩、抽屉边框/圆角/阴影、表单间距、输入高度、下拉浮层和 `ElMessageBox` 样式。
- 验证：type-check、verify:system、build PASS。

## 2026-09-17 供应链 P0（供应商应付台账）后端 + 小程序完成

- **P0-1 数据模型**（`0a041c5`）：新增 `PayableSettlement`（付款×应付多对多核销明细，刻意区别于项目结算 `Settlement`）；`Payment` 增加 `partner`；迁移 `26-供应商应付核销.sql`。
- **P0-2 账期规则**（`afde8fa`）：`SettlementMode`（ReceiptDays / MonthlyCutoff）；`Partner` 增加 settlementMode/settlementDays/cutoffDay；`Payable` 增加 receiptDate + 账期快照；`PayableDueDateContext` 统一算到期日；迁移 `27-供应商账期规则.sql`。
- **P0-3 多应付核销**（`715a28c`）：`finance/payment/save` 支持 `payableIds[]`（与单张二选一）；按到期日先后分摊、写核销明细、回写应付状态；`PayableSettlement.amount` 统一为 BigDecimal（元）。
- **P0-4 账期接入 + 台账报表**（`f02b3aa`）：收货生成应付改由 `PayableDueDateContext` 计算（替代原 supplierDays 硬编码）；`finance/payable/page` 支持 `partnerId`；新增 `finance/partner-ledger/report`（供应商 × 应付/已付/待付/逾期/未结清笔数/最近到期日，按待付倒序，支持 overdueOnly）。
- **P0-5 小程序前端**（`cd40fe6`）：财务首页新增「供应商往来」入口；新增 `subpackages/finance/pages/partners`（待付/逾期汇总 + 全部/仅看逾期筛选 + 账期文案 + 卡片进入应付明细）；分包页 53 → 54。
- 与并行开发的交集：`ProviderSecurityAcceptanceTest` 已被平台会话更新为 208 条路由；本次基于最新值同步为 **209**（protected 197），未覆盖对方改动。
- 待办：Web 端供应商往来页面（报表型页面，通用资源页不适用，需专用页面）；应付列表页承接 `partnerId` 参数筛选；两条迁移脚本待生产执行。

## 2026-09-17 Web 真实业务全链路走查

- 使用本地 Boss 账号完成客户接入、跟进、房屋、量房、设计、报价、签约、立项、开工、任务、验收和应收联动的实际业务走查。
- 记录开发环境 API、量房日期、量房模型分裂、Boss 兼任设计师、任务验收缺失、开工前置校验、伪编辑入口、项目指挥台缺失等缺陷。
- 报告：`_系统/协作/lanes/development/workspace/web-real-business-walkthrough-2026-09-17.md`。
- 状态：`built_pending_fix_acceptance`，开发侧记录不代替 acceptance 结论。


## 2026-09-17 Web 全业务缺陷修复与工作台升级

- P0：修复量房模型分裂、日期序列化、任务验收缺失、开工前置校验、伪编辑入口。
- 工作台：新增量房工作台、合同履约台、验收闭环工作台，并将项目详情升级为项目指挥台。
- 一致性：统一 API 日期时间、消息文案和量房时间格式；设计分项继承区域；Boss 支持兼任设计与项目负责人。
- 验证：Web type-check/build/verify:system PASS；Maven 29 suites / 85 tests / 0 failures / 0 errors；本地 8091 实例完成量房模型和项目配置接口冒烟。

## 2026-09-17 Web 真实业务全链路第二轮复验

- 结果：客户接入、跟进、预约量房、完成量房、Boss 兼任设计师、设计、报价、签约、立项、项目配置、阶段开工、任务验收、阶段验收、业主确认完整跑通。
- 修复：项目提前完工、量房状态刷新、合同/项目工期同步、已生效合同编辑、项目面积误用报价面积、客户详情量房完成门禁。
- 报告：`_系统/协作/lanes/development/workspace/web-real-business-recheck-2026-09-17.md`。
- 剩余重点：阶段排期、验收证据、整改闭环、量房附件、完整多岗位模型、统一待办中心。

## 2026-09-17 Web 全链路第二轮复验结果

- 新增断言：任务全部完成后项目保持 Accepting，必须业主确认后才进入 Finished。
- 实际复验通过：客户接入、跟进、预约量房、完成量房、Boss 兼任设计师、设计、报价、签约、立项、项目配置、开工、任务验收、阶段验收、业主确认。
- 追加修复：项目/合同日期双向同步、已生效合同编辑、项目面积、客户详情量房完成门禁、量房状态刷新和收款状态字典。
- 报告：`_系统/协作/lanes/development/workspace/web-real-business-recheck-2026-09-17.md`。

## 2026-09-17 排期、验收证据与角色待办中心

- 阶段排期：项目确认开工/完工日期后按阶段权重自动拆分 plannedStart/plannedEnd；总工期不得短于阶段数，最后阶段锁定项目计划完工日。
- 验收证据：任务 Done 前强制上传照片/视频；阶段验收强制影像，支持问题清单、整改责任人和整改截止日期。
- 量房附件：现场照片、结构图/原始图纸、其他附件均进入 Measure 实体和文件授权体系。
- 待办中心：新增 `/todo`，聚合客户跟进、量房、任务验收、阶段验收和逾期/临期回款。
- 数据迁移：`docs/design/migrations/25-阶段排期与验收证据.sql`。
- 验证：Maven 29 suites / 85 tests / 0 failures / 0 errors；Web type-check/verify:system/build PASS。

## 2026-09-17 业务对象树形选择器

- 新增 `BusinessObjectPickerDrawer`：左侧树形分类、右侧搜索列表、业务摘要和单选确认。
- 客户选择：按客户阶段分组、按负责人二级分组。
- 项目工地选择：按项目状态分组、按项目经理二级分组。
- 报价项目库选择：按项目类型和分类二级分组。
- 资源页所有 entity 筛选和表单不再使用下拉，统一打开业务对象选择抽屉；依赖对象仍保留上下游联动。
- 量房、合同和验收工作台的客户/项目入口同步替换。
- 设计分项报价库改为选择器，双击结果可直接确认并回填项目价格。
- 验证：Web type-check/verify:system/build PASS；Maven 29 suites / 85 tests / 0 failures / 0 errors。

## 2026-09-17 供应商采购价与可靠毛利闭环

- 供应商采购价：主材、全屋定制、软装、电器、施工等报价项目统一按供应商维护采购价、人工/班组价、起订量、交期、账期、有效期和启停状态；有效采购价自动形成报价库标准成本。
- 权限边界：采购价和成本字段仅 BOSS、店长、财务可见/可维护；材料员响应中不返回 `costPrice`，访问采购价接口返回 403。App 供应链页按角色隐藏采购价页签，Web 端补齐采购价查询、新增和编辑。
- 销售价与成本价分离：报价库新增/更新不再接受前端直接提交 `costPrice`；标准成本只由供应商采购价刷新。
- 成本完整性：新增 `ProjectCostContext` 统一估算成本、实际成本和缺失项数；结算、项目财务、财务汇总和经营驾驶舱在成本缺失时返回 `null` 利润并标记 `costComplete=false`，App/Web 统一显示“成本未完整”。
- 成本回算：实际成本优先采购入库等实际发生值；缺少实际值时按当前有效采购价回算。实际材料成本只在到货入库累计，领料/损耗不再重复累计。
- 套餐边界：免费版、基础版、专业版、旗舰版统一开放全部业务模块，版本差异只保留门店数和席位数。
- 数据迁移：`docs/design/migrations/28-成本完整性结算快照.sql`，允许结算利润/毛利率为空。
- 验证：后端 `mvn -q test` 96 tests / 0 failures / 0 errors；Web build + verify:system PASS；App `verify:mp-weixin` PASS。真实 MySQL 麦鸿回归：`ZC-0001` 采购价 68→75→68 时，项目估算/实际成本 8160→9000→8160，成本缺失时利润保持 `null`。
- 错误集：新增 ECD-002 成本缺失伪利润、ECD-003 采购与领料重复累计、ECD-004 敏感成本裁剪上下文错误，并同步 `rules.md`、`AGENTS.md`。
- 提交：后端 `5355378`、Web `b0bfb16`、App `11ce7d4`。

## 2026-09-17 首页金额格式与工地详情报错修复

- Boss/财务首页金额卡改用统一 `JzMoney` 口径：金额 `>= 10000` 显示万元，`< 10000` 显示元，单位颜色区分；90242.83 显示 `9.02万`，9800 显示 `¥9,800元`。
- 阶段详情：定位为 `project/stage/info` 误序列化 `project.property` 的循环归属图，真实响应达 240KB+ 且嵌套数百层；裁剪 Project 白名单后降至 462B。
- 任务详情：同步裁剪 Project 的深层 `property/designer/manager`，真实响应降至 1.4KB。
- 前端容错：阶段详情的项目、任务、日报、验收改为独立容错；任务详情的项目/阶段/日报失败不再覆盖已加载的任务主数据。
- 回归：新增 `ProjectDetailSerializationContractTest`；App `verify:mp-weixin` PASS；后端全量测试通过。
- 提交：后端 `b42dd58`、App `1aa0e65`。

## 2026-09-17 首页项目卡片与最新动态优化

- 阶段操作业务说明：阶段操作是项目负责人推进“待开工 → 施工中 → 待验收 → 已完成”的状态入口；包含阶段开工、现场汇报、提交/查看验收和添加施工任务，所有动作进入阶段动态与验收链路。
- 页面表达优化：阶段操作区仅在当前角色存在可执行动作时展示，并补充“推进施工状态并沉淀现场记录”的说明，避免空操作块和语义不清。
- 首页项目卡片重构：从“封面+简单文字”改为项目身份、状态、当前阶段、进度、地址、计划完工、项目经理和进入工地动作的分层卡片；整卡保持可点击，并强化按压反馈。
- 最新动态修复：Boss 首页此前刷新时直接执行 `reports=[]`，而同频动态页单独调用 `siteApi.allReports()`，因此出现“首页为空、点击全部有数据”。首页现直接加载同一全量动态接口后再按首页项目范围筛选。
- 验证：Boss 动态接口真实返回 5 条、首条“阶段开工 · 油漆”；App `verify:mp-weixin` PASS。
- 提交：App `e823963`。

## 2026-09-17 动态卡片与成本未完整图标统一

- 动态卡片：统一优化头像、作者/角色、时间、类型标签、正文、图片宫格、类型强调条和按压反馈；单图全宽、多图双列，动态类型色贯穿卡片。
- 成本未完整：Web 新增统一 `CostIncompleteValue`，展示为“-? 🙂/问号笑图标”，鼠标悬浮提示“成本未完整”。
- 覆盖范围：经营驾驶舱、老板/财务工作台统计卡、财务总览累计利润、项目利润/毛利率列、项目结算资源表。
- 验证：Web type-check/build PASS；App `verify:mp-weixin` PASS。
- 提交：App `88f5809`、Web `4018b6a`。

## 2026-09-17 人员绩效、我的分组与关于装得清

- 人员绩效：定位 `statistic/performance/report` 员工摘要误带出 store/team 循环归属图，原始响应 230KB+ 且无法解析；裁剪员工为 `empId/name/role` 后真实响应降至 1.6KB，3 条绩效数据正常。
- 我的页面：功能导航按“账号与企业、组织与权限、经营工具、我的业务、服务与支持”分组，增加分组标题和说明，降低长列表识别成本。
- 关于装得清：新增独立营销页面，基于产品白皮书表达“报价即预算、项目可追溯、经营可复制”，展示行业问题、核心模块、业务链路和适用客群。
- 客服咨询：关于页提供微信在线客服 `open-type=contact`、复制官方客服邮箱、复制包含当前用户/角色/企业/版本的问题反馈模板；未编造微信号或电话。
- 验证：后端 98 tests / 0 failures；App `verify:mp-weixin` PASS。
- 提交：后端 `d7e43df`、App `b180274`。

## 2026-09-17 三端品牌 Logo 统一

- 以 `logo-export/logo-mark-white@2x.png` 和同套 `logo-mark-color@2x.png` 为唯一母版，生成 256px 运行时资源并校验 SHA-256。
- App：新增 `JzBrandLogo`，登录、邀请和关于页改用统一 Logo；资源位于 `src/static/brand/`。
- Web：新增 `BrandLogo`，登录、工作台和移动登录标题改用统一 Logo，同时设置 favicon；资源位于 `public/brand/`。
- Admin：新增 `BrandLogo`，登录和工作台侧边栏改用统一 Logo，同时设置 favicon；admin 当前无独立 Git 仓库，文件已在工作区更新。
- 规范：`rules.md` §12 和 `AGENTS.md` 已写入资源路径、明暗背景变体、SHA-256 和同步验收要求。
- 验证：App `verify:mp-weixin`、Web build、Admin build 全部 PASS。
- 提交：App `9e166c5`、Web `a280220`、规范 `b7121b4`。

## 2026-09-17 店长数据范围、详情响应与品牌色修复

- 店长首页：后端最新动态分页原先缺少 `StoreManager` 分支，导致门店项目有数据但动态为 0；补齐店长、团队长、财务和履约角色项目范围后，店长真实动态返回 5 条。
- 首页项目：店长项目接口有 1 条，前端增加门店业绩 `recentProjects` 回退，避免列表异常时“门店项目”空白。
- 报价详情：定位 `createdBy/Property.salesman/designer` 未配置 Employee 摘要白名单，响应 286KB 且不可解析；限制为 `empId/name/role` 后报价详情 41KB 且可正常解析。
- 全量详情审计：递归扫描所有 `info/support` Provider 的关联字段，补齐验收详情的 Property 白名单；新增详情白名单契约测试与店长动态范围回归测试。
- 门店业绩：补齐新增/跟进客户、报价状态、项目状态、待办、已完工、未读消息、最近客户、最近报价及设计师全部经营指标。
- 视觉：首页统计、动态、报价池和门店业绩统一品牌色；全局状态筛选按钮改为品牌浅底/渐变选中态，修复方案报价状态筛选样式。
- 我的页面：退出登录改为明确的品牌描边按钮，避免底部弱文本不可见。
- 验证：后端 101 tests / 0 failures；App `verify:mp-weixin` PASS。
- 提交：后端 `d8f1947`、App `4987eb9`。

## 2026-09-17 报价详情与门店业绩 UI 重构

- 报价详情：头部、合同衔接卡、项目衔接卡、合同空态、谈单入口、项目衔接指标和报价推进提示统一为品牌渐变/品牌浅底。
- 门店经营结构：由纵向提醒列表重构为 2 列品牌指标卡，覆盖成员、客户、报价、项目、风险和经营沉淀，并补充子指标。
- 门店业绩：保持并强化最近客户、最近报价、最近项目及设计师完整经营指标；最近报价状态统一映射为中文。
- 验证：App `verify:mp-weixin` PASS。
- 提交：App `7795a28`。

## 2026-09-17 门店业绩首屏层级优化

- 店长业绩页头部改为更深品牌渐变，增加独立阴影和与概览区之间的 34rpx 留白。
- 新增独立白色“门店经营概览”容器，内部指标卡改为品牌浅底、白底图标和轻边框，解决头部与卡片颜色相近、直接贴合的问题。
- 增加“实时”状态标签和概览说明，强化视觉层级与交互识别。
- 验证：App `verify:mp-weixin` PASS。
- 提交：App `eb9cae9`。

### 微信开发者工具复验修正

- 通过微信开发者工具实际进入店长“门店业绩”页后发现：新增的白色概览容器与头部、经营结构形成连续多层卡片，视觉层级过重。
- 最终移除额外白色概览容器，改为“深色头部 → 34rpx 间距 → 概览标题 → 白底指标卡 → 门店经营结构”的平面结构。
- 指标卡保留品牌浅底图标、白色卡片、品牌细边框和按压反馈；微信开发者工具已重新编译复验。
- 提交：App `453df66`。

## 2026-09-17 客户诉求品牌卡片优化

- 设计师客户诉求列表与单个客户诉求详情统一使用品牌渐变头部、品牌浅底卡片、品牌色左侧强调条和品牌标签。
- 诉求数量改为品牌色徽标，搜索框、操作分隔和展开详情统一品牌浅底。
- 高、中、低优先级继续保留层级差异，但全部收敛到品牌色梯度。
- 验证：App `verify:mp-weixin` PASS。
- 提交：App `3d8ad06`。

## 2026-09-17 项目经理履约驾驶舱品牌主题色优化

- `src/subpackages/team/pages/workbench.vue` 的 Hero 从项目经理橙色渐变统一为装得清品牌渐变，阴影同步切换为品牌青绿色。
- 指标卡、快捷入口图标底、Tab 激活态、项目/任务/验收/材料/客户/报价卡片统一品牌色梯度；普通状态使用品牌青绿，逾期、驳回、延迟继续保留红色语义。
- 履约业绩卡片移除暖黄色，改为白色到品牌浅底渐变；金额、查看详情和客户头像统一品牌色，并补充卡片按压反馈。
- 验证：`npm run verify:mp-weixin` PASS；微信开发者工具使用项目经理账号进入履约驾驶舱，实际核对 Hero、指标卡、快捷入口、Tab 和项目卡片背景及尺寸正常。
- 提交：App `67ca1cf`。

## 2026-09-18 报价项目库成本维护入口回归修复

- `REQ-20260918-CATALOG-COST-ENTRY`：定位供应商采购价改造后报价库成本概念缺失。
- App `catalog/manage.vue`：恢复只读标准采购成本区块，BOSS/店长/财务可见；新增“维护供应商采购价”入口，携带目录项 ID 和类型跳转并预选项目。
- App `CatalogPicker.vue`：管理列表为有权限角色展示“成本 ¥x / 成本未维护”。
- Web `catalog` 资源：恢复只读标准采购成本字段与“维护采购价”行操作；增加 capability 过滤，非财务权限角色不显示成本列、字段和入口。
- 根因是成本来源迁移只删旧输入未补新链路，不是后端成本模型丢失。新增 ECD-006 硬性禁则。
- 验证：App `verify:mp-weixin` PASS；Web `type-check + build` PASS；微信开发者工具 BOSS 账号实测报价库列表显示“成本未维护”，采购价维护页预选 `单口变压器35W` 成功。

## 2026-09-18 供应商协作 P0（直送工地优先）

- 先从中小装企真实场景出发完成《供应商协作业务设计与操作手册》，明确询价、项目下单、到货、月结、退换货和定制/劳务六类场景。
- 后端 `PurchaseOrder` 新增 `DeliveryMode`，默认 `Direct` 直送工地；`GoodsReceipt.Save` 仅对 `Warehouse` 采购写入库存，直送直接形成项目实际成本和应付。
- BOSS/财务创建采购单自动确认，项目经理/材料员提交后走审批；采购分页支持按当前数据范围跨项目查看，并补齐材料、供应商摘要。
- App 将“供应链与库存”改为“供应商协作”，项目采购设为默认入口，新增采购单和到货确认页面。
- Web 新增项目采购、材料到货资源及角色能力过滤，供应商列表补充应付、已付、未付余额；材料计划可直接进入采购。
- 迁移：`docs/design/migrations/29-采购交付方式.sql`；本地测试库已执行。
- 规范：`AGENTS.md`、`rules.md` 新增“实际业务场景先行”；错误集新增 ECD-007，禁止按完整 ERP 平铺中小装企主流程。
- 验证：后端 `mvn -q test` PASS；App `npm run verify:mp-weixin` PASS；Web `npm run build`、`npm run verify:system` PASS。
- 提交：后端 `9bbbefb`、App `caa32f7`、Web `e0eebca`。
