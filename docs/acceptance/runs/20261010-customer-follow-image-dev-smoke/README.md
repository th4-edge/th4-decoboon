# 客户跟进图片 dev 迁移与 OSS API 冒烟（2026-10-10）

## 环境

- dev 数据库：本机临时 MySQL `127.0.0.1:3307/decoboon`
- 后端：`target/th4-decoboon-1.0.0.jar`，隔离端口 `8093`
- 对象存储：阿里云 OSS 私有 Bucket，测试对象统一前缀 `dev-smoke-20261010/`
- 身份：dev 业务员黄贤杰（`empId=1790222057930837664`）
- 边界：只操作本地 dev DB；未触碰 test/prod；临时凭证和客户字段已在脚本 finally 中恢复

## 迁移 105

- 迁移：`docs/design/migrations/105-客户跟进图片.sql`
- 备份：`docs/design/migrations/backups/20261010-160933-105-dev-decoboon-full.sql.gz`
- 备份 SHA256：`81553a04b98b8f2df58d03753b902e1e850e6c6745da42369a33de21723819aa`
- 迁移脚本 SHA256：`86fa15d2abd33ec731938ea42038b1e1a9287ce07f9928169dbf302d4c03a355`
- 账本：`db_schema_migration.version=105, status=SUCCESS`
- 结构验证：
  - `cust_customer_follow_image` 存在
  - `cust_customer_follow.request_id` 存在
  - `uk_customer_follow_company_request (com_id, request_id)` 存在
  - `uk_customer_follow_image_url (url)` 存在

## 真实 OSS API 冒烟

使用业务员真实密码登录链路（临时本地密码哈希，测试后恢复）完成：

1. `file/private/grant` 上传 1x1 PNG：
   - 返回业务路径以 `companies/` 开头
   - `contentType=image/png`
   - 实际 OSS 对象位于隔离前缀 `dev-smoke-20261010/`
2. `customer/customer-follow/save`：
   - 保存成功
   - 返回 `imageCount=1`、`images=1`、`imageUrls=1`
3. `customer/customer-follow/page`：
   - 回读到新跟进记录
   - `imageCount=1`，`imageUrls=1`
4. `file/private/grant` 重新授权图片：
   - 返回短期签名 URL
   - `curl` 下载 HTTP 200，`Content-Type=image/png`
   - URL 带有 OSS 过期参数，未使用永久公网 URL

## 清理结果

- 测试跟进主表和图片表记录均删除
- 业务员密码哈希、登录时间等凭证字段已恢复原值
- 测试客户 `status/last_follow_at/next_follow_at` 已恢复
- OSS 隔离前缀对象第一次清理删除 1 个；再次扫描 `deleted=0`

## 未覆盖

- 未做 App/Web 真实 UI 点击上传与全屏预览；App/Web 已完成类型检查和生产构建，但页面级真机/浏览器验收仍需后续执行。
- 未执行 test/prod 迁移。
- 未发布任何环境。
