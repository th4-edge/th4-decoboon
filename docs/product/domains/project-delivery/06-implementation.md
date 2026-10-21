# 项目施工履约与进度 · 实现记录

## 本批范围

- Batch：B1
- 垂直切片：业务对齐、真实场景、初始蓝图与实现现状核对
- 不包含：代码修改、迁移、页面改造、发布

## 变更

- 后端：无新增代码；仅核对 project/plan 领域现状。
- App：无新增代码。
- Web：无新增代码。
- 迁移：无。
- 配置：无。

## 测试与证据

- 定向测试：本批未新增测试。
- 构建/类型检查：不适用。
- 真实页面/真机：未做。
- 部署/重启：无。
- 截图/日志：无。

## 风险与未闭环

- GAP-034 仍有阶段算法、acceptanceMode、现场汇报角色、取消任务门禁等未决项。
- 当前代码存在两套阶段计划算法和历史回写快照路径，待 B2 收敛。
- 主工作区代码租约仍归材料域，本域不得进入实现。

---

## 独立切片：业主视角只读预览 P0（2026-10-21）

- 来源：`USER-DECISION-20261021-001/002`、`ANS-BIZ-20261021-001`、`T12-BIZ-20261021-001/002`
- 状态：`DEV_VERIFIED`；未改变本域 B1 `GAP-034` 门禁，不代表独立验收。
- 后端：新增 `project/owner-preview/info`；`CustomerJourneyContext` 增加 `EmployeeOwnerPreview` 模式，`actions=[]` 且 P0 跳过 feedback；5 角色白名单 + `ProjectAccessContext.requireProject`；最小审计独立表。
- App：新增 `view:owner-preview`、`P.ownerPreview`、路由守卫、项目详情入口、`useOwnerPreviewJourney`、只读预览页、固定横幅/水印/返回员工项目。
- 迁移：`123-业主视角预览审计.sql` + rollback；未在 MySQL dev/test/prod 执行。
- 验证：后端全量 893/0/0；App `verify:mp-weixin` PASS；H2 隔离端口冷启动并命中新 Provider。
- 证据：`docs/acceptance/runs/20261021-owner-preview-p0/README.md`。
- 未覆盖：MySQL 真实迁移、真实员工 token 端到端、微信开发者工具/真机视觉走查、独立验收与发布。

## OWNER-UI-V2-A · 开发实现（2026-10-21）

### Backend

- `CustomerJourneyContext` 对报价、合同、选材、材料计划、增减项补充只读事实投影：
  - 报价确认时间、登记人、登记来源；
  - 合同生效时间、登记人、登记方式；
  - 选材设计设计师、登记时间、登记方式；
  - 材料要求到场、实际到场；
  - 增减项发起人、发起时间、生效时间。
- 无 DDL、无状态机、无金额公式和权限变化。

### App

- 新增 Owner Token 与 `styles/owner.scss`。
- `OwnerFactHero` 支持项目态与单据态，业务详情改为暖纸色单据 Hero。
- `OwnerTabBar` 统一五槽导航与中央历程入口。
- 新增 `OwnerSection`、`OwnerFactGrid`、`OwnerSignoffCard`。
- 报价、合同、选材、增减项详情使用统一业务事实与签认语义。
- 新增 `owner-material-plan` 页面、路由和选材页入口。
- 家档案增加材料计划条目。
- 业主真实会话与员工预览仍复用同一页面组件树。

### Evidence

- `docs/acceptance/runs/20261021-owner-ui-v2-a/README.md`
- 后端定向测试 8/8 PASS
- App type-check、Owner UI 契约、预览契约、导航、UI 规范、构建、分包 PASS

### Remaining

- 微信开发者工具真实渲染与截图。
- 业主/预览并排视觉一致性检查。
- 三档分辨率、空态/异常态/长内容走查。
- 独立验收。

## OWNER-UI-V2-C · 同屏验收修复（2026-10-21）

### Backend

- `CustomerJourneyContext.collectActions` 不再把「提出意见或投诉」常驻能力计入 `actions/pendingActionCount`。
- 业主旅程仍保留 `feedback` section；员工预览继续按 T12-BIZ-20261021-001 不展示业主反馈私有内容。
- 定向更新 `CustomerJourneyOwnerPreviewModeTest`：反馈存在但无真实验收待办时，业主 `actions=[]`、`pendingActionCount=0`。

### App

- `OwnerHome` 将意见与投诉改为独立服务入口，不再占用首页“当前最值得先看”焦点卡。
- 业主装修历程的反馈 section 提供“查看记录 / 提出意见或投诉”入口；员工预览不渲染该 section。
- 验收脚本增加身份切换重试、稳定文本采样、预览安全层归一化和短批次运行能力。

### Evidence

- `docs/acceptance/runs/20261021-owner-ui-v2-c/README.md`
- `parity-results.json`：10/10 页面业务区文本 PASS（375×667）
- 后端 `CustomerJourneyOwnerPreviewModeTest` PASS，Maven package PASS
- App type-check、Owner UI / 预览 / UI 规范契约 PASS

### Remaining

- 390×844、430×932、375×812 微信运行时检查。
- 空态、错误态、部分同步、长内容与极端数值。
- 预览写动作逐项交互验证、真机/体验版和独立验收。
