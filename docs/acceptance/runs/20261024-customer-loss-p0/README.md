# 客户流失业务 P0 开发验证证据

日期：2026-10-24  
状态：`INDEPENDENT_PASS / APP_REAL_DEVICE_AND_RELEASE_PENDING`  
问题：`PI-20261024-002`  
规格：`docs/product/2026-10-24-客户流失业务-P0产品规格.md`

## 1. 实现范围

- 新增 `cust_customer_loss` 迁移、实体、枚举与仓储；
- 新增 `customer/loss/save|update|page`；
- 客户 page/info/overview 装配当前流失摘要；
- 量房、设计、报价、签约、设计师今日队列统一排除整体流失与单房流失；
- Web 客户 Hero 流失/恢复抽屉、客户 360 流失摘要、业务员客户列表入口；
- App 客户详情流失范围/阶段/原因选择、恢复和流失摘要。

## 2. 验证结果

| 验证项 | 命令/范围 | 结果 |
| --- | --- | --- |
| 后端定向测试 | `CustomerLossContextTest` 等 5 个测试类 | PASS |
| 后端全量测试 | Java 22.0.1，`mvn -q test` | PASS：1100 tests，0 failures，0 errors，0 skipped |
| 迁移演练 | 临时 MySQL 8.0 数据库 apply + rollback | PASS：表与索引创建成功，rollback 后表删除 |
| Web 类型检查 | `npm run type-check` | PASS |
| Web 构建 | `npm run build` | PASS |
| Web 动作/权限/状态门禁 | `check:actions`、`check:permissions`、`check:states` | PASS |
| App 类型检查 | `npm run type-check` | PASS |
| App 微信构建 | `npm run build:mp-weixin` | PASS |
| 真实 API 冒烟 | 13 项覆盖流失/恢复/幂等/越权/负向 | PASS |
| 并发黑盒 | 同一房屋并发标记、同一 loss 并发恢复 | PASS |
| 独立验收 | 三轮复核，最终 P0=0/P1=0 | PASS |
| 真实 Web 页面走查 | 8 步覆盖单房流失/整体流失/已流失查询/恢复 | PASS |
| App 动作/文案/客户契约 | `check:action`、`check:client-copy`、`check:customer-admin` | PASS |

## 3. 关键口径

- 客户选其他公司 → `Lost`，不是 `Dormant`；
- 单一 `Lost` 客户状态 + 流失范围/阶段/原因/操作人历史；
- 单房流失不误伤同客户其他房屋；
- 已签合同不使用客户流失；
- 恢复时保留原流失记录；
- 活跃队列不展示流失对象，历史与已流失筛选保留。

## 4. 未完成/发布门禁

- 未执行生产或共享 dev/test 数据库迁移；
- 已完成真实 Web 页面冒烟并保存截图；App 已生成真机预览二维码，等待用户扫码完成真机证据；
- 未上传体验版，未提审，未发布；App 二维码为临时开发预览，不构成正式版；
- 生产迁移前仍需用户确认、数据库备份和回滚点；
- 独立代码验收已 PASS；当前仍不能宣称 App 真机通过、生产已迁移或已上线。
