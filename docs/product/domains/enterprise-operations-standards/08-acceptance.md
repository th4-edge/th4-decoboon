# 企业运营标准资产 · 独立验收

## EOP-V1 · PASS

保留历史独立验收结论：`ACCEPTED`。

## EOP-V2 · PASS

- 验收日期：2026-10-23
- 验收角色：独立验收代理（未修改实现、DDL、权限、金额语义或业务事实）
- 验收基线：当前工作区最新文件，独立副本 `/tmp/eop-v2-final.dRTT9h`。
- 验收边界：套餐版本行、发布/停用/下一版、当前发布唯一性、报价来源版本、迁移 124、回滚和真实并发。
- 不验收：真实 App/Web 页面与真机；仍由 EOP-V6 验收。
- 业务依据：`DEC-083` §3、`BR-051`、`T12-BIZ-20261022-004`。

### 命令与结果

| 验证项 | 结果 | 证据摘要 |
| --- | --- | --- |
| 指定 9 个后端测试类 | PASS | 共 30 个用例，0 failures / 0 errors / 0 skipped。 |
| `PackageLifecycleContextTest` | PASS | 9/9；包含重复发布幂等、锁内版本变化、唯一键冲突映射 409、禁止恢复外层事务注解。 |
| `QuoteCopyContextTest` | PASS | 2/2；停用/历史套餐重报被拒绝。 |
| `HybridQuoteBuilderTest` | PASS | 3/3；五类套餐行快照进入 QuoteLine。 |
| `ProviderSecurityAcceptanceTest` | PASS | 3/3；Provider Security/公开白名单循环校验通过。 |
| 后端编译 | PASS | `mvn -q -DskipTests compile` 退出码 0。 |
| App 类型检查 | PASS | 当前工作区快照 `npm run type-check` 退出码 0。 |
| 微信小程序构建 | PASS | `npm run build:mp-weixin` 退出码 0。 |
| 迁移 124 有行存量 | PASS | `PACK-101` 回填为 `Published/current=1`；snapshot hash 非空且匹配 `SHA2(snapshot_json,256)`；JSON line sectionName=客厅。 |
| 迁移 124 无行存量 | PASS | `PACK-102` 回填为 `Draft/current=0`，`current_published_code=NULL`，不具备新报价来源资格。 |
| quote_line 快照回填 | PASS | `package_section_name_snapshot=客厅`。 |
| currentPublished 唯一约束 | PASS | `uk_pack_template_current_version(com_id,current_published_code)` 存在；重复 current 插入返回 MySQL `1062`。 |
| 迁移 124 回滚 | PASS | pack_template、pack_line、quote、quote_line 新增列与索引全部移除；基础数据行保留。 |
| 真实并发时序 | PASS | MySQL 8.4 `REPEATABLE-READ` 双会话：A 锁内读取 `lock_a=1`，提交 v2 释放锁；B 等锁后才开事务读取 `tx_after_wait=2`。 |

### 事务与并发复核

- `Build/Publish/Disable` Provider 已移除外层 `@Transactional`。
- `buildNextVersion/publish/disable` 先获取 Redisson 锁，再通过 `TransactionTemplate` 开启独立事务；锁在事务完成后释放。
- 锁内重新查询最新版本；真实 MySQL 验证证明新事务可看到前一锁持有者已提交的 v2。
- `DataIntegrityViolationException` 被映射为业务 409：“套餐版本正在被其他操作更新，请刷新后重试”。
- 反射测试确认 `buildNextVersion/publish/disable` 和 `Build.call` 均未恢复外层事务注解。

### 功能与迁移复核

- 套餐草稿、发布、下一版、停用生命周期通过；发布版本不可原地修改。
- 重复发布幂等；第二次发布返回同一对象，不新增版本。
- 当前发布版本由应用逻辑和数据库唯一投影共同保证。
- 遗留 current Published 无快照时会在报价使用或重复发布前补冻结。
- 停用/历史套餐不能通过 `sourceQuoteId` 重报。
- QuoteLine 同时保留套餐分区、项目分类、类型、工艺和成本快照。
- 迁移 124 对有行/无行存量、快照 JSON/hash、quote 来源、quote_line 分区快照和回滚均通过隔离 MySQL 验证。

### 非阻断事项

- 真实 App 页面操作、微信开发者工具和真机验证尚未执行；本批不宣称 UI 已验收，统一留 EOP-V6。
- 整体问题 `PI-20261022-003` 仍未完成后续 V3～V6，不得标记 `CLOSED`。

### 结论

`EOP-V2 = PASS`。允许进入 EOP-V3。

本次 PASS 仅覆盖套餐不可变版本与报价来源版本切片；真实 App/Web 页面与双端一致性仍由 EOP-V6 独立验收。


## EOP-V3 · PASS

- 验收日期：2026-10-23
- 验收角色：独立验收代理（未修改实现、DDL、权限、金额语义或业务事实）
- 验收基线：当前最新工作区；全新隔离副本 `/tmp/eopv3-pass-backend.zx2Fi1`、`/tmp/eopv3-pass-app.cuzuDl`；隔离 MySQL 8.4 实例 `/tmp/eopv3-mysql.h4fDgm`。
- 业务依据：`DEC-083` §4、`BR-051`、`DEC-048`、`T12-BIZ-20261022-004`。
- 不验收：真实 App/Web 页面与真机，仍留 EOP-V6。

### 命令与结果

| 验证项 | 结果 | 证据 |
| --- | --- | --- |
| 指定后端定向测试 | PASS | Java 22 全新副本共 20/20：`CompanyPlanTemplateContextTest` 11/11、`ContractPlanContextTest` 2/2、`ContractBaselineTermTest` 3/3、`PlanTemplateBaselineContextTest` 1/1、`ProviderSecurityAcceptanceTest` 3/3。 |
| 后端全量测试 | PASS | `mvn -o -q test` 退出码 0；隔离副本报告 1032 tests、0 failures、0 errors、0 skipped。 |
| 后端编译 | PASS | `mvn -o -q -DskipTests compile` 退出码 0。 |
| App 类型检查 | PASS | `npm run type-check` 退出码 0。 |
| 微信小程序构建 | PASS | `npm run build:mp-weixin` 退出码 0，`dist/build/mp-weixin` 生成。 |
| 空 `totalDays` 阶段合计兜底 | PASS | `customConstructionWithoutDeclaredTotalDaysUsesStageWeightsAsFallback` 通过；显式 `setTotalDays(null)`、阶段 40/60，保存后为 100。 |
| 显式 120/阶段权重 100 | PASS | `customConstructionKeepsDeclaredTotalDaysIndependentFromStageWeights` 通过，保存后仍为 120。 |
| 并发跨 code 默认唯一 | PASS | 独立双线程验证使用全企业 default 锁，默认数为 1；Provider 无外层事务，锁先于 TransactionTemplate。 |
| 127 合同工期归档 | PASS | MySQL 8.4：合同归档表保留 `source_total_days=120`；支付/施工计划及明细归档、旧唯一键恢复通过。 |
| 迁移 127 完整矩阵 | PASS | 回填、currentEnabled 唯一、同 code 多版本、重复 current `1062`、回滚均通过。 |
| 默认解析 | PASS | 未指定 code 时仅接受 currentEnabled+defaultPlan；无默认返回 400 显式选择错误。 |
| 平台来源/升级幂等 | PASS | 来源 code/version 进入 Provider/App；相同 sourceBaselineCode 重复 build 返回同一草稿。 |
| 合同 `sourceTotalDays` | PASS | 合同快照写入模板声明总工期；合同 Save/Update/Page/Info/Workbench 白名单和 App contract model 已映射。 |
| 需求/蓝图映射 | PASS | `03-blueprint.json`、`04-requirements.md` 已补 `REQ-EOP-020~026`、版本行、合同快照和默认策略。 |

### 原阻断复验

| 编号 | 当前结论 | 证据 |
| --- | --- | --- |
| B1 无默认静默选择 | RESOLVED | 空 code 无 current+default 返回 400。 |
| B2 跨 code 多默认 | RESOLVED | 顺序与并发发布均保持默认数 1。 |
| B3 App 平台升级不可达 | RESOLVED | 来源 code/version 进入后端视图/Provider 和 App model。 |
| B4 多版本回滚失败 | RESOLVED | v1 current 保留，v2 draft 归档，旧唯一键恢复。 |
| B5 施工总工期快照与口径 | RESOLVED | 合同 `sourceTotalDays` 和独立声明总工期输入已闭环；空值/显式 120 两种边界均通过。 |
| B6 V3 需求/蓝图未映射 | RESOLVED | 03/04 已形成 REQ→对象/字段→API/页面→验收映射。 |
| B7 重复升级不幂等 | RESOLVED | 相同 baseline 重复 build 返回同一草稿。 |
| B8 并发默认重复 | RESOLVED | 全企业 default 锁双线程验证通过。 |
| B9 空 totalDays 兜底错误 | RESOLVED | 改为判断 `request.getTotalDays()==null`；空值 40/60 场景保存 100。 |
| B10 127 合同工期回滚丢失 | RESOLVED | 合同归档表保存 `source_total_days=120`。 |

### 非阻断事项

- 真实 App/微信开发者工具/真机/Web 管理页仍未执行，本批不宣称 UI 已验收，统一留 EOP-V6。
- `GAP-062` 继续以 `DEC-048` 为准；`GAP-063` 不在本批范围。
- 本次未修改实现代码、DDL、权限、金额语义或业务事实。

### 结论

`EOP-V3 = PASS`。付款/施工计划版本行、默认策略、显式平台升级、合同来源与总工期快照、迁移回滚和 App/Provider 契约均完成独立验证。

允许进入 `EOP-V4`；`PI-20261022-003` 整体仍未完成，不得标记 `CLOSED`。真实页面和真机仍由 `EOP-V6` 验收。

## EOP-V4 独立验收计划（尚未执行）

- 当前状态：`PENDING_INDEPENDENT_ACCEPTANCE`
- 开发证据：相关 29 个测试类 102/102、App/Web type-check/build、隔离 MySQL 8.4.0 迁移 128/回滚 PASS。
- 必须独立覆盖：
  - 同 revision 双请求只允许一个成功，另一个稳定 409；
  - 无业务变化保存不增加 revision/审计；
  - 更新、启停、未引用删除各自审计类型正确；
  - 已引用项目删除被拒且历史引用不变；
  - 设计/营业/项目经理读取历史时无成本字段，BOSS/店长/财务可见；
  - 跨企业 itemId 历史与写入均不可泄漏/不可修改；
  - 新报价行保存来源 revision，旧报价行不受后续修改影响；
  - App/Web 同一 revision、同一冲突提示和同一历史异常恢复。
- 全量后端测试当前受并行 `GoodsReceiptRepository` 启动错误阻断，修复后必须作为独立验收前置条件。
- 未取得独立验收 PASS 前，`EOP-V4` 保持 `DEV_VERIFIED`，不得改 `ACCEPTED`。
