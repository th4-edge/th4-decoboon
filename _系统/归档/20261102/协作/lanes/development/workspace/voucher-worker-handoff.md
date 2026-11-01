# 凭证套打 + 工人档案 · 后端实施交接

- 日期：2026-09-21
- Lane：development
- 依据：`DEC-005`、`DEC-006`、`SCN-008`、`SCN-009`、`BR-006`、`BR-007`、`OBJ-002`、`OBJ-003`、
  `REQ-BIZ-20260921-002`、`docs/product/2026-09-21-凭证套打与单据模板字段冻结.md`（v2.0）

## 1. 输入依据

- 凭证定位：收付款单据套打，**不做**会计记账凭证/科目/总账。
- 凭证落库（第五轮修订，撤销早期"不留归档"）；编号首次生成时产生并落库，重打读同一记录。
- 合并判定：`收款方/付款方 + 项目 + 款项类型` 三者相同。
- 工人档案：独立实体 + 企业级共享；项目经理维护；财务、企业负责人、店长、设计师可查看。
- 敏感字段：完整身份证号与银行账号仅企业负责人、财务与维护人（项目经理）可见；店长、设计师脱敏。
- 版式：L1 平台行业基线 + L2 受控参数（字段显隐 / 抬头 / 字号 / 主色 / 签字栏）；不做 L3 自由套打。

## 2. changed_files

### 工人档案（新域 `labor`）

- `src/main/java/com/th4/edge/decoboon/labor/configure/WorkerStatus.java`
- `src/main/java/com/th4/edge/decoboon/labor/model/Worker.java`
- `src/main/java/com/th4/edge/decoboon/labor/repository/WorkerRepository.java`
- `src/main/java/com/th4/edge/decoboon/labor/context/WorkerPrivacyContext.java`
- `src/main/java/com/th4/edge/decoboon/labor/service/worker/page/{Page.java,support/Page.java}`
- `src/main/java/com/th4/edge/decoboon/labor/service/worker/save/{Save.java,support/Save.java}`
- `src/main/java/com/th4/edge/decoboon/labor/service/worker/update/{Update.java,support/Update.java}`

### 凭证（`document` 域扩展）

- `src/main/java/com/th4/edge/decoboon/document/model/VoucherRecord.java`
- `src/main/java/com/th4/edge/decoboon/document/model/VoucherItem.java`
- `src/main/java/com/th4/edge/decoboon/document/repository/VoucherRecordRepository.java`
- `src/main/java/com/th4/edge/decoboon/document/repository/VoucherItemRepository.java`
- `src/main/java/com/th4/edge/decoboon/document/context/VoucherContext.java`
- `src/main/java/com/th4/edge/decoboon/document/context/VoucherPdfRenderer.java`
- `src/main/java/com/th4/edge/decoboon/document/context/VoucherAmountWords.java`
- `src/main/java/com/th4/edge/decoboon/document/service/voucher/build/{Build.java,support/Build.java}`
- `src/main/java/com/th4/edge/decoboon/document/service/voucher/page/{Page.java,support/Page.java}`

### 关联改动

- `src/main/java/com/th4/edge/decoboon/finance/model/Payment.java`：新增可选 `worker` 关联。
- `src/main/java/com/th4/edge/decoboon/finance/repository/LedgerRepository.java`：新增按 ID 集合查询。
- `docs/design/migrations/44-工人档案与凭证表.sql`：`db_worker`、`db_voucher_record`、`db_voucher_item`，并为 `db_finance_payment` 增加 `worker_id`。
- `src/test/java/.../document/VoucherAmountWordsTest.java`、`VoucherBuildAcceptanceTest.java`
- `src/test/java/.../labor/WorkerPrivacyContextTest.java`
- `src/test/java/.../security/ProviderSecurityAcceptanceTest.java`：路由总数 256 → 261（public 12 + protected 249），并补 5 条未授权断言。

## 3. 新增路由

| 路由 | 能力 | 权限 |
| --- | --- | --- |
| `labor/worker/page` | 工人档案分页 | 财务、企业负责人、店长、设计师、项目经理 |
| `labor/worker/save` | 新增工人档案 | 项目经理 |
| `labor/worker/update` | 编辑 / 停用工人档案 | 项目经理 |
| `document/voucher/build` | 生成凭证 PDF 并落库 | 财务、企业负责人 |
| `document/voucher/page` | 凭证记录分页 | 财务、企业负责人、店长、项目经理、设计师 |

## 4. verification

- `mvn -o test`：**201 tests / 0 failures / 0 errors / 0 skipped**（基线 194，新增 7）。
- 新增断言覆盖：
  - 金额大写：`40.00→肆拾元整`、`40.50→肆拾元伍角`、`40.56→肆拾元伍角陆分`、`1005.00→壹仟零伍元整`、`10000→壹万元整`、`100000000→壹亿元整`、`-40→负肆拾元整`、`0→零元整`。
  - 工人档案脱敏：企业负责人/财务/项目经理见完整值；店长/设计师见 `341222********5970` 与 `***********1266`；空值不处理。
  - 凭证生成：凭证号 `PZ` 前缀、金额合计 100.00、大写"壹佰元整"、明细 2 行、文件已生成并有短期地址。
  - 重打幂等：同一来源组合再次生成，`voucherId` 与 `voucherNo` 不变，凭证表仍只有 1 条。
  - 合并拒绝：不同对方合并报"仅可合并收款方/付款方、项目、款项类型三者相同的记录"。
  - 零金额拒绝："凭证金额不能为 0"。
- Provider 安全验收：261 条路由全部带 `Security` 或公开白名单；新增 5 条路由未授权返回 401。

## 5. 实现要点

- 凭证来源统一取 `Ledger`（收款、付款、手工记账都归集到资金流水），明细行保留各自来源单号以回溯原始款项。
- 合并凭证与单笔凭证走同一实现：单笔不校验合并条件，多笔校验三者相同。
- 模板：复用 `DocumentTemplate`（平台基线瞬态副本不建立实体关联，只存模板版本）与 `DocumentProfile`。
- PDF：OpenPDF + 内嵌 CJK/Roboto 字体（与报价单同源），A4 版式。
- 敏感字段不落库为可检索列：`VoucherItem` 只存来源、金额、账户、类别与备注；身份证号/银行账号完整值仅存在于 PDF。
- 工人档案无 `@DataPermission`（企业级共享主数据，与 `DocumentTemplate` 同模式），企业隔离在服务端强制。

## 6. risks

- **`Receipt` 分支未做端到端验证**：凭证生成已按 `LedgerDirection.Income → DocumentType.Receipt` 分支实现，但到账账户/对方人物信息当前从付款单解析；收款单的对方（客户）联系方式接入尚未验证。
- **L2 配置项尚未提供写入口**：`fieldConfig` / `styleConfig` 的 JSON schema 键已按冻结清单实现，但模板配置界面未做，当前只能用平台基线与已有 `document/template/save`。
- **合并凭证的到账账户**：明细行的到账账户取自对方档案（工人/合作方），未逐行快照；若同一组合内各笔到账账户不同，PDF 明细到账账户列为空，需业务确认。
- **敏感字段可见范围**：冻结清单原文写"完整值仅财务、企业负责人"，实现额外放开了**维护人（项目经理）**（否则项目经理无法核对/修改自己录入的数据）。如需严格按原文，需要回改。
- 迁移 `44` 尚未对任何环境执行；生产应用前需先执行迁移并重启。

## 7. next_action

   - App 端已完成（2026-09-21）：`subpackages/labor` 工人档案（项目经理维护）+ `subpackages/finance/pages/vouchers` 凭证打印 + 资金台账多选生成凭证。
2. 决策：确认"项目经理可见完整敏感字段"是否保留。
3. 验收：`acceptance` lane 覆盖四角色 × 敏感字段 × 跨企业 × 合并/重打负向用例；真机横向验证模板配置与字段生效。

## 7.1 App + 模板配置（2026-09-21 第二轮）

- 后端：凭证字段键统一 `showXxx` 并加入白名单；基线、渲染器、字号范围（9–14）对齐平台。
- App：新增横向模板配置页 `subpackages/finance/pages/voucher-template.vue`（左配置 / 右预览），入口在"收付凭证"页右上角；`edit:doc-template` 仅 BOSS。
- 门禁：修复 `verify-homepage-contract.mjs` 的过期文案断言，`verify:mp-weixin` 全绿。

## 8. acceptance_needed

- 角色矩阵：企业负责人 / 财务 / 项目经理 / 店长 / 设计师 / 业务员（越权）访问 `labor/worker/*`。
- 真实企业数据：工人建档 → 付款 → 生成凭证 → 打印 → 重打同号。
- 跨企业负向：跨企业读取工人档案、凭证记录与 PDF 地址。
- 逾期与订阅：订阅到期仍可打印凭证。
