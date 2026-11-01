# CHG-20261102-007 · MAT-X2B Web 主材到场确认

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261102-007` |
| 类型 | 功能补强 / 跨端能力补齐 |
| 风险级别 | L2 |
| 状态 | `COMMITTED / DEV_VERIFIED / INDEPENDENT_ACCEPTANCE_PENDING` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联 | `PI-20261022-015`、`MAT-X2`、`BR-054`、`DEC-088`、`DEC-092` |
| Web 提交 | `ec3da3b` |
| 前置提交 | `489948c` MAT-X2A |

## 2. 目标与范围

- 目标：在 Web 主材作业单工作面增加“确认到场”，支持部分到场和尾量续收。
- 页面：`/workbench/material-sheets` 作业单详情与到场弹窗。
- 数据：复用 `project/purchase/page`、`project/goods-receipt/save|page`；不新增 Provider。
- 非范围：不改后端到场逻辑、应付生成、成本公式、库存、异常退换或 DDL；不做到场证据图片上传。
- App 已有到场确认页，Web 复用同一事实和状态推导。

## 3. 角色规则

| 角色 | 到场确认 |
| --- | --- |
| 项目 Pm / 设计负责人 | 可 |
| Boss / StoreManager / Finance | 可兜底 |
| 其他员工 | 拒绝 |

服务端仍执行项目和数据范围校验。

## 4. 实现要点

- 作业单进入 `Ordered / PartiallyOrdered / PartiallyArrived` 后显示“确认到场”。
- 到场弹窗按项目读取采购单，再按作业单行的 `materialPlanId` 匹配采购明细。
- 显示每条采购明细的已到场量、采购量、待收量和采购来源。
- 数量、日期和说明一次提交；页面预校验，Provider 再次执行数量上限与角色校验。
- 部分到场显示“已确认部分到场，剩余数量可继续确认”，尾量到场显示“材料已全部到场”。
- 默认到场日期使用本地时区，不使用 UTC 日期。
- 页面明确“到场只累计实际收货并回填应付账期，不新增应付”。

## 5. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| Web clean commit build | PASS | `ec3da3b` 独立 worktree `npm run build` |
| 表单/概念/权限/动作/状态/页面系统门禁 | PASS | Web 仓库命令输出 |
| Designer 首批到场8㎡ | PASS | 作业单 `PartiallyArrived`，采购 `PartiallyReceived`，累计到场8 |
| 超量13㎡（待收12） | PASS | 页面提示 1–12；Provider 业务400，无落库 |
| Designer 尾量12㎡ | PASS | 作业单 `Arrived`，采购/采购明细 `Received`，累计到场20 |
| 到场记录 | PASS | 两条记录 8+12，到场日期2026-11-02，验收人为设计负责人 |
| 应付 | PASS | 仍只有原1条应付1760，未新增；receiptDate=2026-11-02、dueDate=2026-12-02 |
| 成本 | PASS | 已知采购价88，实际成本1760；无伪0值 |
| 材料计划 | PASS | 计划需求30中已采购/到场20，状态保持 `PartiallyArrived` |
| 三档渲染 | PASS（结构/溢出） | 1440/1280/1024 无横向溢出，无内部编号/技术文案 |
| 视觉人工评审 | `VISUAL_REVIEW_UNAVAILABLE` | 当前会话无图像输入，不冒充视觉 PASS |
| App 真机/独立验收 | BLOCKED | 无授权微信身份，独立验收未执行 |

## 6. 发布与回滚

- 已提交到本地 Web `dev`；未 push、未部署。
- 无数据库迁移；回退 `ec3da3b` 即移除 Web 到场动作和 API 扩展。
- 数据终态为隔离库中的部分/全部到场记录，采购、计划、应付和成本均已对账。
