# RUN-20261101-PKG-CLOSE-001 验收报告

## 运行信息

- Plan：`TP-20261101-V0.1`
- Suite：`SUITE-CORE-CHAIN-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- App：`1.0.74 / 174`，`e3c95d4`；本地构建树 SHA-256 `59a02a8a…`
- Backend：本地 dev `7b7dc5f`，工作区有未提交变更
- 环境、租户：本地受控库 / 昭青科技 `199500000000000001`
- 数据前缀：`AUDIT-20261101-PKGCLOSE-001-`
- 数据备份：全库备份 SHA-256 `3f4d380b…`
- 写后清理：刘洋（演示04）客户状态恢复 PendingMeasure，房屋状态恢复 Measured；AUDIT 前缀套餐与报价测试数据保留，生产未写入

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| --- | --- | --- | --- | --- |
| 1 | 0 | 0 | 1 | 0 |

> 正式结论 `BLOCKED` 的原因是套餐专项冻结用例缺失。另执行 16 项套餐专项探索检查，结果为 6 PASS / 8 FAIL / 2 BLOCKED。

## 缺陷

| DEF | 优先级 | 状态 | 场景 | 证据 | 复验条件 |
| --- | --- | --- | --- | --- | --- |
| DEF-20261101-004 | P1 | OPEN | 选品多选未完整落库，重复确认产生同一项目重复行 | `evidence/EXP-PKG-004/`、`raw/package-db-state.ndjson` | 一次确认两个不同项目，仅两条不同行且重复提交幂等 |
| DEF-20261101-005 | P1 | OPEN | 已发布套餐列表显示0分区/0项/配置中 | `evidence/EXP-PKG-007/`、`raw/manager-list-readback.json` | 列表与API/DB的status、分区数、项目数一致 |
| DEF-20261101-006 | P1 | OPEN | 缺少发布/下一版/停用，已发布套餐可进编辑但保存409 | `evidence/EXP-PKG-007/`、`raw/quote-and-update-readback.json` | App完成生命周期动作，已发布只读 |
| DEF-20261101-007 | P1 | OPEN | 套餐报价详情区域分组显示“综合项目” | `evidence/EXP-PKG-006/clean-quote-detail.png`、`raw/quote-and-update-readback.json` | 页面与API按区域A快照分组 |
| DEF-20261101-008 | P2 | OPEN | 套餐入口选客户后清空已选套餐 | `evidence/EXP-PKG-006/16-quote-created.txt` | 选客户和房屋后套餐保持 |
| DEF-20261101-009 | P2 | OPEN | 房屋选择标题显示URL编码姓名 | `evidence/EXP-PKG-006/14-property-picker.png` | 标题显示明文客户姓名 |
| DEF-20261101-010 | P2 | OPEN | 非维护角色可通过深链进入套餐编辑页并看到编辑控件 | `evidence/EXP-PKG-008/`、`raw/role-negative-provider.json` | 深链被页面守卫阻断，服务端403保持 |
| DEF-20261101-011 | P1 | OPEN | 套餐报价详情展示内部报价编号 | `evidence/EXP-PKG-006/clean-quote-detail.png` | 页面与列表不展示Q编号 |

## 已确认通过

- 线上只读数据：空套餐和空分区能够被当前 1.0.74 页面正确显示。
- 套餐基本信息和说明可创建，服务端生成 code/version/status。
- 新增区域A/B后立即显示，重新进入仍存在。
- 空区域可删除；有项目行的区域不会进入删除流程。
- 套餐报价金额、折扣、税金、成本和 costComplete 页面/API/DB 一致。
- 成本可见性：Boss/Finance 可见，Designer/Pm/Marketer 被服务端裁剪。
- 服务端对 Designer/Finance 的套餐写请求返回403。

## 覆盖与缺口

- 已执行套餐探索项：16项；测试写操作后已恢复既有客户/房屋状态，生产数据未写入。
- 正式冻结套餐用例：0项。
- 缺少：套餐模板状态机、发布/下一版/停用、项目行批量幂等、历史版本快照回读、套餐报价区域分组、套餐选择器分页、跨角色/跨端套餐一致性。
- 线上 1.0.72 + 生产旧后端存在“空套餐可生成零项目报价”的静态风险，但缺少生产 Boss 授权和可回滚写入环境，本轮未执行，不能判为已验证缺陷。
- Web 当前没有对应套餐模板配置页面，本轮只能验证 App；因此不满足 App/Web 同源套餐维护声明。

## 结论

- 结论：`BLOCKED`
- 能证明什么：1.0.74 对“空分区不显示”的直接修复在线上数据和本地页面回读中有效；套餐报价金额与成本计算链路在本地受控数据上基本一致。
- 不能证明什么：不能证明套餐模板全生命周期、完整配置链、历史版本保护、App/Web 同源和正式发布就绪。
- 残余风险：存在 5 个 P1 OPEN、3 个 P2 OPEN；套餐专项无冻结 Oracle，不能签正式 PASS。
- 下一步：先冻结套餐专项测试用例与 Oracle 资产；研发修复 DEF-004 至 DEF-011；修复后创建新的 `RUN-*` 复验并将体验版真实写入纳入。
