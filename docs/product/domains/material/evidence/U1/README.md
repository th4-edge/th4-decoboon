# U1 · App 主材作业单详情工作台化

- 日期：2026-10-18
- 性质：UX 回归自测与真实 H5 渲染证据；不是独立验收
- 范围：App `subpackages/project/pages/material-sheet-detail.vue`
- 环境：本地 dev，MySQL `127.0.0.1:3306/decoboon`，后端 `127.0.0.1:8090`，App H5 `127.0.0.1:8081`
- 账号：Boss `19900000001`、Designer `19900000005`、PM `19900000007`、Finance `19900000002`（本地测试密码）
- 测试单据：`TILE-20261018-001/002/003`（走查后已 cancel → delete；选材记录保留为本地测试数据）

## 1. 页面契约

| 项 | 结果 |
| --- | --- |
| 角色 | Designer / PM / Boss / StoreManager / Finance |
| 首屏第一问 | 当前单号 + 状态 + 项目 |
| 首屏第二问 | 还差哪些现场字段 / 有几行待下单 |
| 首屏第三问 | 一个主按钮：提交请购 / 保存现场信息 / 去下单 |
| 解释位置 | “为什么这样填？”折叠，不占首屏 |
| 成本语言 | Designer/PM 不显示价格状态、成本、利润；Boss/店长/财务才显示商业价格状态 |

## 2. 改动

- 移除首屏 `MaterialSheetContextGuide` 大段说明卡；
- 新增紧凑状态卡（单号、状态、项目、测量/配送/安装）；
- 新增“下一步/现场信息”卡，直接内嵌测量/配送/安装字段；
- Designer 点“提交请购”时自动保存现场字段再提交，避免“先保存再提交”两步；
- PM 可保存现场信息；已提交后显示“现场信息已提交，如需修改可更新后保存”；
- Designer/PM 隐藏价格状态；Draft 阶段 Boss/财务也不提前显示“成本待结算”；
- 底部只保留一个主按钮，导出/编辑降为次要动作；
- “时间口径/作业单边界”收进“为什么这样填？”。

## 3. 真实渲染结果

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| Designer Draft 390×844 | PASS：首屏“还差 1 项：测量时间”，主按钮“提交请购”，无成本语言 | `screenshots/u1-designer-draft-390x844.png` |
| Boss Draft 390×844 | PASS：同上，且显示商业价格状态 | `screenshots/u1-boss-draft-390x844.png` |
| PM Draft 390×844 | PASS：显示现场字段，主按钮“保存现场信息” | `screenshots/u1-pm-draft-390x844.png` |
| Finance Draft 390×844 | PASS：不显示现场字段，提示“等待现场信息”，无主按钮 | `screenshots/u1-finance-draft-390x844.png` |
| Designer 选择测量日期后 | PASS：任务变为“可以提交了”，提交后进入“已提交请购 / 等待财务” | `screenshots/u1-designer-submit-flow-390x844.png` |
| Designer Submitted 390×844 | PASS：顶部“已提交请购”，无主按钮，仅导出 | `screenshots/u1-designer-submitted-390x844.png` |
| Finance Submitted 390×844 | PASS：显示“待下单 / 有 1 行待下单”，主按钮“去下单” | `screenshots/u1-finance-submitted-390x844.png` |
| PM Submitted 390×844 | PASS：显示“现场信息已提交，如需修改可更新后保存” | `screenshots/u1-pm-submitted-390x844.png` |
| Designer 帮助展开 375×667 | PASS：说明可见、无横向溢出、Console Errors 0 | `screenshots/u1-designer-help-375x667.png` |

## 4. 指标

- 390×844：`scrollWidth = innerWidth = 390`，无横向溢出；
- 页面高度约 888px，内容可滚动，主按钮 sticky 可见；
- 375×667 帮助展开：`scrollWidth = innerWidth = 375`，无横向溢出；
- 业务 Console Errors：0；
- 提交链路：选择测量日期 → 任务提示“可以提交了” → 点“提交请购” → 状态“已提交请购”，无额外保存步骤。

## 5. 命令

```bash
npm run type-check
npm run check:ui
npm run check:subpackages
npm run build:mp-weixin
```

全部 PASS。

## 6. 未闭环

- U2：财务下单/结算页面的工作台化尚未执行；
- U3：助手技术语言、蓝图入口、老板材料风险页尚未执行；
- 本轮为真实 H5 自测，不等同于微信开发者工具真机/独立用户验收；
- 测试作业单已清理；选材记录 `U1工作台走查-瓷砖选材` 保留在本地 dev，作为本轮测试数据。
