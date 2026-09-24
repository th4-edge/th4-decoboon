# 阿里云 OSS 对象存储部署说明

## 1. Bucket

- Bucket：`decoboon`
- 地域：华东1（杭州）
- 公网 Endpoint：`https://oss-cn-hangzhou.aliyuncs.com`
- 存储类型：标准存储
- 冗余类型：同城冗余
- Bucket ACL：必须保持私有

## 2. 对象路径策略

除明确公开的员工头像外，业务对象只保存 OSS 对象键，不保存公网永久 URL。

### 项目文件

```text
companies/{comId}/projects/{projectId}/files/{businessType}/{yyyy}/{MM}/{uuid}.{ext}
```

### 企业文件

```text
companies/{comId}/files/{businessType}/{yyyy}/{MM}/{uuid}.{ext}
```

### 系统生成文件

```text
companies/{comId}/files/{businessType}/{yyyy}/{MM}/generated-{uuid}.{ext}
```

### 员工头像（私有对象 + 后端公开代理）

```text
avatar/{comId}/{empId}-{uuid}.{jpg|jpeg|png|webp}
```

- Bucket 仍保持私有；
- OSS 对象保持私有，不调用 `PutObjectAcl`；员工头像通过既有公开路由 `/file/private/download?path=avatar%2F...` 仅放行严格头像对象键并长期展示；
- 代理只允许读取 `avatar/` 命名空间，其他 OSS 业务对象仍必须走私有文件授权；
- 员工资料保存业务域名下的稳定代理地址，不保存预签名临时地址；
- `user/user/update` 仅在 multipart 文件字段 `avatar` 存在时写入 OSS；客户端直填非空 `avatarUrl` 会被拒绝。

企业 ID 来自当前登录员工；项目 ID 必须通过 `ProjectAccessContext` 校验。前端不能指定企业 ID。

## 3. RAM 最小权限

建议创建专用 RAM 用户 `decoboon-oss`，只授权 `decoboon` Bucket：

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "oss:PutObject",
        "oss:GetObject",
        "oss:DeleteObject",
        "oss:AbortMultipartUpload",
        "oss:ListObjects",
        "oss:GetBucketInfo"
      ],
      "Resource": [
        "acs:oss:*:*:decoboon",
        "acs:oss:*:*:decoboon/*"
      ]
    }
  ]
}
```

AccessKey 只允许通过部署环境变量或外部 `config/` 文件注入，禁止提交到 Git。

## 4. 环境变量

```bash
TH4_STORAGE_PROVIDER=oss
ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_BUCKET=decoboon
ALIYUN_OSS_ACCESS_KEY_ID=...
ALIYUN_OSS_ACCESS_KEY_SECRET=...
ALIYUN_OSS_PRESIGN_TTL_SECONDS=300
ALIYUN_OSS_KEY_PREFIX=
ALIYUN_OSS_MAX_FILE_SIZE_BYTES=524288000
```

`application-prod.yml` 默认使用 `oss`，任一必要参数缺失时应用拒绝启动。

## 5. 上传与访问

- 业务文件上传和授权统一使用受保护的 `file/private/grant`。
- multipart 存在文件时：写入私有 OSS，返回稳定对象键和短期下载地址。
- 普通授权请求：校验业务对象数据范围后，为 OSS 对象生成 300 秒预签名 GET URL。
- 员工头像走 `user/user/update` 的 `avatar` 文件字段：写入私有 OSS 后返回稳定业务域名地址 `/file/private/download?path=avatar%2F...`；Bucket ACL 和对象 ACL 都保持私有。
- `TalkPptContext` 生成 PPTX 后直接写入 OSS，不再写入本地磁盘。
- 本地 `upload-dir` 仅保留为开发/测试 `local` provider 和历史数据兼容，不是生产业务文件真源。

## 6. 验证

1. 登录系统后上传一张图片和一份 PPTX/PDF。
2. 确认 Bucket 对象路径以 `companies/` 开头，并包含正确的企业/项目 ID。
3. 使用当前账号可生成预签名 URL；无权限账号不能调用 `file/private/grant`。
4. 过期预签名 URL 返回 403。
5. 将 Bucket 设置为私有，确认普通业务对象无签名直链不可访问。
6. 上传员工头像后确认对象路径以 `avatar/{comId}/` 开头、对象保持私有，且 `/file/private/download?path=avatar%2F...` 头像 URL 在小程序长期可访问。
