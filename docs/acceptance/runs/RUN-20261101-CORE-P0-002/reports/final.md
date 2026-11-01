# RUN-20261101-CORE-P0-002 验收报告

## 运行信息

- Plan：`TP-20261101-V0.1`
- Suite：`SUITE-CORE-CHAIN-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- 代码提交：backend `7b7dc5f0ed3795fbd6d13640f5dfb7f51b8c3dd8`；web `e40ca8baf6203d71f5d0375cc043d424a0aa5873`；app `f2fb91e0785bf953940257ce97e3ea84fc2ae808`
- 环境：后端 `http://127.0.0.1:8092/api`，Web `http://127.0.0.1:5175`，App `mp-weixin-accept-8092`
- 租户：昭青科技 `199500000000000001`
- 数据库：隔离库 `decoboon_accept_20261101`
- 数据前缀：`AUDIT-20261101-COREP0002-`

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| --- | --- | --- | --- | --- |
| 23 | 2 | 2 | 19 | 0 |

## 缺陷

| DEF | 优先级 | 状态 | 场景 | 证据 | 复验条件 |
| --- | --- | --- | --- | --- | --- |
| `DEF-20261102-001` | P0 | VERIFIED | Web 收款记录缺少 Boss/店长冲正入口 | `evidence/RETEST-DEF-20261102-001/` | 已由店长页面复验；冻结用例角色口径另见 `TCR-20261102-001` |
| `DEF-20261102-002` | P0 | VERIFIED | 结算列表成本完整性投影错误 | `evidence/RETEST-DEF-20261102-002/` | 7 个成本字段与项目报告同源；App 回读待身份 |
| `DEF-20261102-003` | P0 | VERIFIED | 店长客户列表进入 forbidden，阻断客户360 | `evidence/TC-CRM-MANAGER-HANDOFF-003/` | 本店客户可进入并指派；跨店仍拒绝 |
| `DEF-20261102-004` | P1 | VERIFIED | 报价详情缺少客户接受登记人 | `evidence/TC-DQ-PERMISSION-QUOTE-005/` | 页面显示登记人/时间/员工登记边界 |

## 材料主链新增证据

- 未选材阻断：`TC-MAT-DESIGNER-SELECTION-001/07-*`、`08-*`
- 选材交付财务：`TC-MAT-DESIGNER-SELECTION-001/02-*`、`03-*`、`05-*`、`06-*`
- 作业单来源与提交：`TC-MAT-DESIGNER-SHEET-002/03-*`、`04-*`
- 财务 Web 回读：`TC-MAT-DESIGNER-SHEET-002/05-*`
- 数量边界：`TC-MAT-BOUNDARY-QUANTITY-003/01-*`、`02-*`
- Finance Web 下单、采购/应付与幂等：`TC-MAT-FINANCE-ORDER-004/07-*` 至 `11-*`

## 覆盖与缺口

- 已覆盖用例：23/23 已处理，无 `NOT_RUN`。
- Web/Provider/DB：合同、CRM、设计报价、财务成本/应付、材料选材/作业单/下单等主要正向与负向证据已形成。
- App 阻塞：无授权微信身份，全部 App 页面分支均为 `BLOCKED_IDENTITY`。
- Owner 阻塞：无真实业主身份和真人见证，业主身份与确认用例均为 `BLOCKED_IDENTITY`。
- 已知跨端 GAP：`PI-20261022-015 MAT-X2` 明确 Web 仍缺作业单新建/编辑/测量/提交/取消；当前不得宣称 App/Web 作业单全生命周期一致。
- 测试资产冲突：`TCR-20261102-001` 记录冻结用例要求 Finance 冲正，与 `DEC-075/DEC-076` 冲突。
- 跨端对账：只能证明 Web/API/DB 局部同源，不能证明 App/Web/Owner 全范围同源。

## 结论

- 结论：`BLOCKED`
- 能证明什么：隔离库上，已执行的 Web/API/DB 主链、权限、金额、成本和材料下单部分真实可用；4 个已登记缺陷在缺陷级复验通过。
- 不能证明什么：App/业主端页面闭环、业主本人确认、Web/App 全域同屏同源、正式发布就绪。
- 残余风险：当前工作树约 600 个未提交变更，包含大量跨会话存量；不能直接作为清洁发布候选，也不能把缺陷级 `VERIFIED` 当成整条 P0 用例 `PASS`。
- 下一步：
  1. 按 CHG 分拆当前 4 个已复验缺陷修复，保留无关存量，先形成可审计候选提交。
  2. 处理 `TCR-20261102-001`，由测试资产授权主体拆分 Finance 收款与 Boss/店长冲正用例。
  3. 选择并执行 `MAT-X2`：补齐 Web 作业单新建/编辑/测量/提交/取消，或先提供匹配 App/Owner 授权身份复跑对应页面。
  4. 新开 RUN 完成 App/Web/Owner 跨端复验后，再判断 `PASS / CONDITIONAL_PASS / FAIL / BLOCKED`。
