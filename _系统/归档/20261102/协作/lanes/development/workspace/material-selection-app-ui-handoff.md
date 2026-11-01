# 主材选材 · App UI 梳理与选材跟进台 开发交接

- ISSUE-ID: REQ-20260921-MATERIAL-SELECTION-UI
- 日期: 2026-09-21
- lane: development
- 批次: 产品文档与 UI 设计梳理（G5 → G4 App 侧）

## 输入依据

- 产品稿 `前端代码/th4-decoboon-app/docs/product/2026-09-21-主材选材业务流程与交互设计.md`
- 索引 `docs/product/00-产品文档与UI设计索引.md`（G1~G5）
- 业务口径 `SCN-006` / `BR-004` / `DEC-003`；未冻结 `GAP-005`

## 本轮范围

### G5 · App 状态色映射（已闭环）

**问题**：状态色散落在 `domains/*/meta.ts` 的 40 处裸 hex，与 `tokens.ts` 的语义色不统一，`草稿/待确认/已确认` 类状态靠口口相传。

**处理**：

- `src/styles/tokens.ts` 新增 `STATUS_COLOR`（14 档），`src/uni.scss` 新增对应 `$jzb-status-*`；
- 取值与线上现状**逐值一致**，属「先集中、后收敛」，零视觉变化；
- `src/domains/{project,schedule,acceptance,crm,finance,site}/meta.ts` 裸色值 → `STATUS_COLOR.*`，**meta.ts 裸 hex 归零**；
- `docs/03-视觉与设计规范.md` §9 补 9.2 状态色板 / 9.3 各域对照 / 9.4 使用规则 / 9.5 待收敛；
- 顺带补齐另一 lane 新页面引用的缺失 SCSS 变量：`$jzb-primary-base`、`$jzb-radius-md`（16rpx，已同步进 §4.3 圆角表）。

## changed_files / output_paths

后端：

- `src/main/java/com/th4/edge/decoboon/material/model/SelectionBoardItem.java`
- `src/main/java/com/th4/edge/decoboon/material/service/selections/board/{Board.java,support/Board.java}`
- `src/main/java/com/th4/edge/decoboon/material/repository/MaterialSelectionRepository.java`（+ 按房屋集合查询）
- `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java`（路由 275 = public 12 + protected 263，新增 board 鉴权断言）

App：

- `src/styles/tokens.ts`、`src/uni.scss`
- `src/domains/{project,schedule,acceptance,crm,finance,site}/meta.ts`
- `src/domains/material/{model.ts,api.ts}`（`SelectionBoardItem` + `materialSelectionBoardApi`）
- `src/subpackages/project/pages/selection-board.vue`
- `src/config/index.ts`、`src/pages.json`、`src/pages/mine/index.vue`
- `docs/03-视觉与设计规范.md`、`docs/product/2026-09-21-主材选材业务流程与交互设计.md`

后端文档：`docs/product/00-产品文档与UI设计索引.md`

## verification

- 后端 `mvn -o compile` PASS
- `ProviderSecurityAcceptanceTest` PASS（3/3，路由 275）
- App `npm run type-check` PASS
- App `npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 590440 B，分包 14 个 / 85 页）
- 未执行：`material/selection/board` 的真实租户回读、独立 acceptance

## risks

1. `STATUS_COLOR` 取值与现状一致，状态色**尚未真正统一**（双绿/三橙/双红/三紫），收敛需视觉验收，见规范 §9.5。
2. 页面内仍有 19 处历史裸色值（角色身份色、报价替代态等），其中角色色应单独建角色色板，未纳入本轮。
3. `ProviderSecurityAcceptanceTest` 路由总数仍是共享文件，与并行 lane 同期修改；本次按实测 275 同步。
4. `material/selection/board` 的「在跑」口径取 `Preparing/Ongoing/Suspended/Accepting`，未含 `Finished/Settled`；若业务希望覆盖完工未结算项目需再确认。

## next_action

1. acceptance 独立复核 App 跟进台（角色可见性、范围隔离、空态/失败态）。
3. **G1 整改闭环**：先向业务智能体提 `T05`（不合格→整改→复验的对象、状态、责任人、是否联动金额），裁决后出产品稿。

## acceptance_needed

- 跟进台三态（未创建/草稿/待确认）与角色可见性
- 跨企业、跨门店范围隔离
- 空态（全部已确认）与失败重试
