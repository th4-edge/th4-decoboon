# 阿里云 OSS 对象存储开发交接

## Input

- 用户已开通阿里云 OSS 并创建 `decoboon` Bucket。
- 目标：系统内文件上传、私有文件读取和系统生成文件全部改为 OSS 存储。
- 存储路径按企业和项目隔离。

## Console Facts

- Bucket：`decoboon`
- 地域：华东1（杭州）
- Endpoint：`https://oss-cn-hangzhou.aliyuncs.com`
- 存储类型：标准存储
- 冗余：同城冗余
- Bucket 私有性：无签名直链返回 HTTP 403
- RAM 用户：`decoboon-oss`
- 自定义策略：`decoboon-oss-rw`
- AccessKey：仅保存在被 Git 忽略、权限 `600` 的 `.env.oss.local` 中，不进入仓库和本交接文档。

## Object Key Strategy

项目文件：

```text
companies/{comId}/projects/{projectId}/files/{businessType}/{yyyy}/{MM}/{uuid}.{ext}
```

企业文件：

```text
companies/{comId}/files/{businessType}/{yyyy}/{MM}/{uuid}.{ext}
```

系统生成文件：

```text
companies/{comId}/files/{businessType}/{yyyy}/{MM}/generated-{uuid}.{ext}
```

## Changed Files

- `pom.xml`
- `src/main/java/com/th4/edge/components/storage/**`
- `src/main/java/com/th4/edge/decoboon/common/file/context/FileStorageContext.java`
- `src/main/java/com/th4/edge/decoboon/common/file/context/FileSignatureContext.java`
- `src/main/java/com/th4/edge/decoboon/common/file/context/FileAuthorizationContext.java`
- `src/main/java/com/th4/edge/decoboon/common/file/context/ObjectKeyContext.java`
- `src/main/java/com/th4/edge/decoboon/common/file/model/FileGrant.java`
- `src/main/java/com/th4/edge/decoboon/common/file/service/grants/build/support/Build.java`
- `src/main/java/com/th4/edge/decoboon/design/context/TalkPptContext.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `run-dev.sh`
- `.gitignore`
- `.env.oss.local.example`
- `../../前端代码/th4-decoboon-app/src/domains/file/api.ts`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/report/edit.vue`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/accept/edit.vue`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/task/detail.vue`
- `../../前端代码/th4-decoboon-web/src/domains/file/api.ts`
- `docs/design/migrations/22-OSS对象存储部署说明.md`

## Verification

- Cloud Shell / Aliyun OSS CLI：Bucket 列示、上传、读取、删除全部成功。
- 实际 SDK：上传、读取、删除成功；预签名 URL 返回 200；无签名直链返回 403。
- Spring 完整上下文：OSS Bean 装配成功，上传/读取/签名/删除 1/1 通过。
- 业务上传 Provider：手机号密码登录后上传文件，对象键为 `companies/{comId}/files/report/...`，预签名 URL 成功，OSS 读回内容一致，1/1 通过。
- PPT 生成：谈单 PPT 写入企业 OSS 路径，读取大小大于 1000 字节，1/1 通过。
- `mvn -o -q -DskipTests compile`：通过。
- `OwnerFileExposureAcceptanceTest` + `TalkPptContextTest`：9/9 通过。
- 小程序 `npm run verify:mp-weixin`：通过。
- Web `npm run type-check`、`npm run build`：通过。

## Risks

- AccessKey 已生成并保存在本机忽略文件；部署环境仍需通过环境变量注入。
- 历史本地文件没有自动迁移脚本；当前 `decoboon` Bucket 为空，若生产服务器已有历史 `upload/**` 文件，需要另做清点和迁移。
- 框架通用 `{model}/upload/{property}` 仍写本地目录；当前业务文件入口已经统一走 `file/private/grant`，后续不得新增直接使用框架通用上传的模型字段。
- `ProviderSecurityAcceptanceTest` 当前因并行开发的 `project/contract-change/batch-save` 使 Provider 数成为 197，断言仍固定 196；这与 OSS 改造无关，需 acceptance lane 更新计数。

## Acceptance Needed

- 跨企业、跨项目上传/授权拒绝。
- 预签名 URL 过期后 403。
- 大文件和视频上传耗时/失败重试。
- 历史本地文件迁移方案确认。
