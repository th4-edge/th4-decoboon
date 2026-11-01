# 业主闭环 P0 开发交接

## 输入依据

- `docs/plan/2026-09-15-上线前业务功能规划.md` 的 `PP-01`、`PP-02`、`T02-T04`
- 现有员工邀请、JWT `Credentials/Employee` principal、项目四级数据范围和验收状态机

## 后端接口

| 路由 | 调用身份 | 作用 |
| --- | --- | --- |
| `identity/customer-invite/save` | 员工 | 为当前可见项目的显式客户生成一次性业主邀请 |
| `identity/customer-invite/info` | 公开 | 通过 token 或邀请 ID + 手机号查询，回传脱敏手机号 |
| `identity/customer-invite/accept` | 微信 Credentials | 校验手机号、有效期、状态后消费邀请并建立项目授权 |
| `identity/customer-invite/page` | 员工 | 按项目分页查询邀请 |
| `identity/customer-invite/cancel` | 员工 | 撤销待接受邀请；已接受时同时撤销业主项目授权 |
| `customer/project/page` | 业主 Credentials | 当前微信身份显式授权的项目分页 |
| `customer/project/info` | 业主 Credentials | 授权项目详情 |
| `customer/property/info` | 业主 Credentials | 授权项目住房详情 |
| `customer/stage/list` | 业主 Credentials | 授权项目阶段与进度 |
| `customer/stage/info` | 业主 Credentials | 授权阶段详情 |
| `customer/report/page` | 业主 Credentials | 仅 `visibleToCustomer=true` 的公开动态 |
| `customer/acceptance/page` | 业主 Credentials | 授权项目验收单 |
| `customer/acceptance/info` | 业主 Credentials | 授权验收详情 |
| `customer/acceptance/update` | 业主 Credentials | 确认或驳回待处理验收；同结果重复提交幂等 |
| `customer/contract/info` | 业主 Credentials | 最小合同摘要，不返回成本、报价或内部财务字段 |
| `file/private/grant` | Employee / Customer | 校验业务对象和文件引用后签发短期文件 URL |
| `file/private/download` | 签名 URL 持有者 | 校验 HMAC 和有效期后读取 upload-dir 文件 |

`auth/session/info` 现在在 `Credentials` 上返回 `sessionType` 和 `customerAccesses`，用于前端识别
`User`、`Employee`、`Customer`，并取得全部显式授权项目。业主接口只接受 `Credentials` token，
不接受员工 token 冒充业主。

## Changed Files

- `src/main/java/com/th4/edge/tenant/user/configure/SessionType.java`
- `src/main/java/com/th4/edge/tenant/user/model/Credentials.java`
- `src/main/java/com/th4/edge/tenant/user/model/Company.java`
- `src/main/java/com/th4/edge/tenant/user/context/UserIdentityContext.java`
- `src/main/java/com/th4/edge/tenant/user/service/auth/session/info/support/Info.java`
- `src/main/java/com/th4/edge/tenant/user/service/auth/session/info/Info.java`
- `src/main/java/com/th4/edge/tenant/user/service/user/user/{info/Info.java,update/Update.java}`
- `src/main/java/com/th4/edge/tenant/billing/filter/SubscriptionAccessRequestFilter.java`
- `src/main/java/com/th4/edge/decoboon/customer/configure/CustomerAccessStatus.java`
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerInvite.java`
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerAccess.java`
- `src/main/java/com/th4/edge/decoboon/customer/repository/CustomerInviteRepository.java`
- `src/main/java/com/th4/edge/decoboon/customer/repository/CustomerAccessRepository.java`
- `src/main/java/com/th4/edge/decoboon/customer/context/CustomerInviteContext.java`
- `src/main/java/com/th4/edge/decoboon/customer/context/CustomerAccessContext.java`
- `src/main/java/com/th4/edge/decoboon/customer/service/invite/**`
- `src/main/java/com/th4/edge/decoboon/customer/service/project/**`
- `src/main/java/com/th4/edge/decoboon/customer/service/property/**`
- `src/main/java/com/th4/edge/decoboon/customer/service/stage/**`
- `src/main/java/com/th4/edge/decoboon/customer/service/report/**`
- `src/main/java/com/th4/edge/decoboon/customer/service/acceptance/**`
- `src/main/java/com/th4/edge/decoboon/customer/service/contract/**`
- `src/main/java/com/th4/edge/decoboon/project/model/Acceptance.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/AcceptanceRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectReportRepository.java`
- `src/main/java/com/th4/edge/decoboon/project/context/ProjectContext.java`
- `src/main/java/com/th4/edge/decoboon/project/service/acceptances/update/support/Update.java`
- `src/main/java/com/th4/edge/decoboon/project/service/acceptances/page/Page.java`
- `src/main/java/com/th4/edge/decoboon/project/service/tasks/update/Update.java`
- `src/main/java/com/th4/edge/decoboon/collab/context/NoticeContext.java`
- `src/main/java/com/th4/edge/decoboon/project/repository/ProjectStageRepository.java`
- `src/main/java/com/th4/edge/decoboon/common/configure/PublicFileWebConfigure.java`
- `src/main/java/com/th4/edge/decoboon/common/file/**`
- `src/main/java/com/th4/edge/decoboon/common/configure/ProviderSecurityValidator.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `docs/design/migrations/17-业主闭环.sql`
- `docs/design/migrations/18-私有文件访问发布说明.md`
- `docs/design/migrations/19-生产发布基线.md`
- `docs/design/migrations/20-小程序发布合规基线.md`
- `_系统/协作/lanes/development/workspace/provider-security-scan.py`
- `_系统/协作/lanes/development/workspace/provider-security-scan.md`
- `_系统/协作/lanes/development/workspace/provider-security-scan.csv`
- `../../前端代码/th4-decoboon-app/src/App.vue`
- `../../前端代码/th4-decoboon-app/.env`
- `../../前端代码/th4-decoboon-app/.env.production`
- `../../前端代码/th4-decoboon-app/vite.config.ts`
- `../../前端代码/th4-decoboon-app/package.json`
- `../../前端代码/th4-decoboon-app/scripts/verify-release-env.mjs`
- `../../前端代码/th4-decoboon-app/src/manifest.json`
- `../../前端代码/th4-decoboon-app/src/pages.json`
- `../../前端代码/th4-decoboon-app/src/pages/legal/{agreement,privacy}.vue`
- `../../前端代码/th4-decoboon-app/src/pages/auth/login.vue`
- `../../前端代码/th4-decoboon-app/src/pages/index/index.vue`
- `../../前端代码/th4-decoboon-app/src/pages/mine/index.vue`
- `../../前端代码/th4-decoboon-app/src/pages/dynamic/index.vue`
- `../../前端代码/th4-decoboon-app/src/pages/invite/accept.vue`
- `../../前端代码/th4-decoboon-app/src/domains/identity/{api,model,session}.ts`
- `../../前端代码/th4-decoboon-app/src/domains/project/{api,scope}.ts`
- `../../前端代码/th4-decoboon-app/src/domains/{acceptance,catalog,design,property,schedule,site,trade}/api.ts`
- `../../前端代码/th4-decoboon-app/src/domains/trade/model.ts`
- `../../前端代码/th4-decoboon-app/src/domains/file/api.ts`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/{invite,detail}.vue`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/stage/detail.vue`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/accept/edit.vue`

## T05 小程序接入

- 邀请页支持员工 `inviteId` 路径和业主 `type=customer + token` 路径。
- 业主邀请通过 `identity/customer-invite/info|accept` 查询、校验和一次性接受。
- `auth/session/info` 依据 `sessionType=Customer` 和 `customerAccesses` 切换业主会话，多项目授权写入 `CustomerIdentity.projectIds`。
- `projectApi`、`siteApi`、`acceptanceApi`、`tradeApi` 在 `role=customer` 时自动切换 owner-only 路由；员工分支保持原路径。
- 项目详情业主模式只加载项目、阶段、公开动态、验收和合同摘要，不请求任务、成员、财务或内部报价接口。
- 验收确认只提交 `acceptanceId + confirmed`，不再提交 `customerId`。
- 401 继续由 HTTP 层清会话并回登录页；邀请失效由接口错误信息提示。

## 文件鉴权

- 删除 `/upload/**` 全局静态映射，旧匿名 URL 统一返回 404。
- 删除公开的 `/upload/talk/{file}` 下载入口。
- 阻断框架通用 `{modelName}/download/{property}` 任意路径下载入口。
- 私有文件统一通过 `file/private/grant` 授权；授权时按员工四级数据范围或业主显式项目授权校验业务对象，并确认目标路径确实属于该对象。
- 授权响应只包含短期 HMAC URL；`file/private/download` 校验签名、有效期、真实路径和目录边界后返回文件。
- `/public/**` 仅映射显式 `public-dir`，与私有 `upload-dir` 物理隔离。
- 前端 `fileApi` 已接入动态、验收、合同变更、量房、任务、设计、谈单和材料库文件 URL；签名 URL 回写业务前会恢复为原始 `upload/...` 路径。
- 历史 `upload/...` 数据无需改写；旧 URL 失效后由业务 API 重新签发。

## Provider 安全与订阅封口

- 本轮修复后静态扫描 196 条具体 `@Service` 路由：185 条受保护、11 条公开白名单、0 violations；旧 `ProviderSecurityAcceptanceTest` 的 192/9 预期已过期。
- 修复 `auth/session/info`、`project/acceptance/page`、`project/task/update`、`user/user/info`、`user/user/update` 的 `Security` 遗漏。
- 新增 `ProviderSecurityValidator`，启动时对白名单外的缺失 `Security` 路由 fail-fast。
- `SubscriptionAccessRequestFilter` 继续作用于所有 `Security` Provider：查询放行，写操作按订阅状态拒绝，订阅/套餐/订单路由豁免。
- 完整逐路由扫描见 `provider-security-scan.md` 和 `provider-security-scan.csv`。

## 生产发布基线

- `application-prod.yml` 的数据库、Redis、微信、文件目录和签名密钥全部改为环境变量占位。
- 生产 `ddl-auto=validate`，关闭 SQL 输出。
- Runbook 见 `docs/design/migrations/19-生产发布基线.md`。
- 新增 `00-迁移账本.sql`、`01-空库基线.sql`、可复现基线生成脚本、迁移执行模板和安全恢复模板。
- 迁移策略已收敛：`new_empty_database` 正式支持 `baseline`，只执行 `00` + `01` 并把 10/11/12/17/21 登记为 `BASELINE`；`existing_database` 禁用自动 `upgrade`，必须先做 DBA 完整结构审计并使用 `adopt` 登记，实际差异迁移另行设计、评审和演练。
- `baseline` 失败会尝试把账本从 `RUNNING` 更新为 `FAILED`；`RUNNING`/`FAILED` 状态拒绝自动重跑。迁移和恢复模板均使用 `sha256sum`/`shasum`（必要时 `openssl`）兼容校验，并对目录做 `realpath` 规范化和危险根路径检查。
- 回滚采用“新数据库 + 新文件目录恢复后切换”，不删除或覆盖原生产数据；模板对空值、绝对路径、危险根目录、目录边界、恢复库非空和备份校验值进行强校验。
- 当前仍需 DBA 在隔离 MySQL 8 环境实跑 `baseline`、DBA 审计后的 `adopt` 和 `rollback-safe`，并补齐生产域名、凭据和备份资源；真实 MySQL 8 演练仍为 BLOCKED。

## 小程序发布合规

- `.env` 仅用于本地 HTTP 开发；生产构建从 CI 或 `.env.production` 注入 HTTPS API 地址。
- `vite.config.ts` 对缺少配置、非 HTTPS 生产地址以及缺失协议版本/主体信息执行构建失败。
- `build:mp-weixin:release + scripts/verify-release-env.mjs` 提供独立发布门禁，拦截占位域名、AppID、草稿协议和待补充法务信息；普通 `verify:mp-weixin` 不视为可提审构建。
- 登录页新增可点击《用户协议》《隐私政策》，同意记录包含协议版本、隐私版本、时间、主体、联系方式和 App 版本，并独立于登录会话保留。
- 协议与隐私页面明确标记“待法务审核”，没有编造正式法律条款。
- 未发现定位调用，已移除 `getLocation` 和 `scope.userLocation` 声明。
- 未确认的微信 AppID 已清空，构建产物使用 `touristappid`；发布前必须替换为管理员确认的正式 AppID。
- 发布检查和提审材料见 `docs/design/migrations/20-小程序发布合规基线.md`。

## 迁移

正式迁移已落位：

- `docs/design/migrations/17-业主闭环.sql`
- `docs/design/migrations/18-私有文件访问发布说明.md`
- `docs/design/migrations/19-生产发布基线.md`
- `docs/design/migrations/20-小程序发布合规基线.md`
- `docs/design/migrations/00-迁移账本.sql`
- `docs/design/migrations/01-空库基线.sql`
- `docs/design/migrations/12-租户简称扩展.sql`
- `docs/design/migrations/21-用户密码登录.sql`
- `docs/design/migrations/generate-baseline.sh.template`
- `docs/design/migrations/run-migrations.sh.template`
- `docs/design/migrations/rollback-safe.sh.template`
- lane 建议稿：`_系统/协作/lanes/development/workspace/owner-closure-migration.sql`

迁移模板本轮已增加 baseline 故障账本保护、校验和漂移阻断、危险路径规范化，并移除了默认 `upgrade` 执行承诺。`10-设计转化域迁移.sql` 已标记为不完整历史草稿，禁止自动执行；10/11/12/17/21 仅作历史版本标识，在存量库上发现差异时不得直接依赖它们补跑，必须另行提供完整差异迁移。

## 验证

- `npm run verify:mp-weixin`：通过；主包 378198 B，最大分包 154634 B，总包 1103656 B。
- `mvn -o -q -Dtest=OwnerClosureAcceptanceTest test`：通过，11 项业主闭环验收测试全部通过。
- `mvn -o -q -Dtest=OwnerFileExposureAcceptanceTest test`：通过，匿名 `/upload/private-contract.txt` 返回 4xx。
- `mvn -o -q test`：72 tests / 0 failures / 0 errors / 0 skipped。
- Spring 启动注册日志包含全部新增 `@Service` 路由
- 三个迁移/恢复模板 `bash -n` 通过；baseline 故障注入退出码 4 且账本写入 `FAILED`；checksum 漂移退出码 8；`upgrade` 退出码 9；危险路径变体经 `realpath` 后退出码 2。

## 风险

- 本次收紧 `UserIdentityContext` 不再按手机号自动把 Credentials 绑定到 Employee，避免业主因同手机号继承员工身份；历史未显式关联的数据需迁移核验。
- 邀请表新增非空 `customer_id`，生产迁移必须先按 `project_id` 回填再收紧非空。
- 验收状态机修正为“Pending/Rejected 优先于 100% 任务进度”，需 acceptance lane 覆盖确认后再驳回、驳回后重提场景。
- 小程序仍需微信开发者工具/真机验证合法域名、手机号授权、分享路径、签名文件展示和弱网/401。
- 生产必须配置稳定且多实例一致的 `TH4_FILE_SIGNATURE_SECRET`，以及隔离的 `TH4_FILE_UPLOAD_DIR`、`TH4_FILE_PUBLIC_DIR`；未配置签名密钥时会使用进程随机密钥，重启后旧签名立即失效。
- 小程序正式域名、AppID、备案、主体、服务类目、协议和隐私政策仍未确认，当前协议页面与 `https://api.invalid` 只能用于构建验证，不能提审。

## 验收重点

- 邀请 token 重放、过期、撤销、手机号不匹配、跨企业项目。
- 业主跨项目访问项目、住房、阶段、动态、验收、合同，统一返回 404/无对象存在性泄露。
- 一个微信身份多项目授权、同一项目多业主授权。
- 验收确认/驳回幂等、状态越级、并发提交、阶段和项目汇总一致。
- 员工四级数据权限回归。
