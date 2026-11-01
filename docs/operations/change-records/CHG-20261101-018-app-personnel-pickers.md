# CHG-20261101-018 · App 人员选择器统一与工人分页

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261101-018 |
| 类型 | 缺陷修复/重构 |
| 风险级别 | L2 |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/事故/业务裁决 | `DEC-021` / `BR-019`、`BR-021`、`BR-022`、`CHG-20261101-015` |

## 2. 目标与范围

- 目标：清除 App 离职交接、任务负责人和财务工人的原生 picker/大数组选择。
- 范围内：
  - 员工选择页增加普通目录/薪酬目录来源、角色集合、仅在职、排除本人和服务端筛选参数。
  - 新增 App 工人选择页、一次性回传通道和路由权限。
  - 离职交接、任务负责人、财务工资付款、付款补录接入专用选择页。
  - 财务工资付款与付款补录的工人选择锁定为仅在职工人。
  - 工人 Provider 改为数据库分页、状态和关键词 SQL 过滤。
  - 选择器契约与分页基线更新。
- 非范围：不修改离职交接、任务保存、财务付款、工资凭证、金额、成本或状态规则；不处理合同报价来源和供应商价格。
- 受影响端：后端、App。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及员工/工人读取范围和付款工人关联；不改变业务公式或权限角色矩阵。
- 业务事实来源与裁决编号：`DEC-021` / `BR-019`、`DEC-023` / `BR-021`、`DEC-024` / `BR-022`。
- 尚存 GAP 或假设置：当前租户无在职工人，工人有数据 UI 采用路由夹具；合同报价来源与供应商价格仍为后续 P1。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 离职交接 | Boss 选择接收人 | 员工选择页排除离职员工本人，只允许在职 | 截图、网络请求 |
| 2 | 任务负责人 | 编辑施工任务 | 只展示允许岗位，选择后员工卡回填 | 截图、网络请求 |
| 3 | 工资付款工人 | 工资类付款 | 进入专用工人选择页，不在页面内加载数组 | 路由、契约 |
| 4 | 付款补录工人 | 付款缺少工人 | 选择页返回后补录并刷新 | 契约、代码路径 |
| 5 | 工人分页 | 请求 `current/size/status` | 服务端数据库分页，最多 50 条 | API 证据、单测 |
| 6 | 工人真实空态 | 当前企业无在职工人 | 显示真实空态和恢复提示 | 截图、API |
| 7 | 移动端 | 320/390 | 无横向溢出 | 截图、几何证据 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 未提交 | EmployeeRepository、Employee Page、WorkerRepository、Worker Page、定向测试、T15、证据 |
| th4-decoboon-web | dev | 不适用 | 未修改 |
| th4-decoboon-app | dev | 未提交 | employee-picker、worker-picker、handover、task edit、finance entry、payments、routes、contract scripts |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| App 类型检查 | `npm run type-check` | PASS | 命令输出 |
| App 分包构建 | `npm run build:mp-weixin` | PASS | `dist/build/mp-weixin` |
| 选择器契约 | `check:selectors` | PASS | 命令输出 |
| 财务选择器 | `check:finance-pickers` | PASS | 命令输出 |
| 财务工人补录 | `check:finance-payment-worker` | PASS | 命令输出 |
| 在职工人约束 | 工资付款/付款补录传 `activeOnly=1`，停用工人不可选 | PASS | `docs/acceptance/runs/20261101-selector-p1-final/evidence/web-runtime.json` |
| 动作/导航/UI/文案/分包 | 对应 npm 脚本 | PASS | 命令输出 |
| 后端定向测试 | 员工/工人/WorkerInfo 测试 | PASS | Maven 输出 |
| Provider 响应契约 | `audit-provider-response-contract.py --check` | PASS，0 findings | 命令输出 |
| Provider 角色烟测 | `action_provider_smoke.py` | 32/32 PASS | `action-provider-smoke.json` |
| H5 页面自测 | 离职交接、任务编辑、工人选择 | PASS（开发自测） | `docs/acceptance/runs/20261101-app-personnel-pickers/` |
| 图像视觉评审 | 截图 | `VISUAL_REVIEW_UNAVAILABLE` | 当前模型无图像输入能力 |

- 未执行项及原因：未执行真实离职、任务保存或付款工人补录提交，避免改业务数据；未做微信开发者工具真机复验。
- 已知失败及是否与本次相关：无业务失败；H5 开发态存在 1 条 `setNavigationBarColor:fail page not found` 的 uni 运行时 rejection。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 不适用 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：员工/工人选择越权、候选范围错误、任务/交接/付款无法选择或页面布局阻断
- 回滚命令/步骤：回退 App 与后端本批提交，使用上一稳定 release
- 回滚验证：来源页面可恢复原选择，服务端权限与付款逻辑仍生效
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否（首轮任务 owner 类型和三处分页基线需修正）
- 返工原因：`owner` 空值类型不匹配；员工/工人选择替换后分页基线需收缩
- 最终结论：本批 P1 路径开发自测通过；待独立验收与物理真机。

## 10. 复盘与沉淀

- 是否出现重复问题：是，选择器页面存在但来源业务仍自行全量拉取
- 是否需要新增门禁：是，App 选择器契约已覆盖人员接入和全量数组反模式
- 候选 `EXP/ANT/PATH`：选择器复用必须按权限来源切换，不能用同一 Provider 覆盖不同角色场景
- 后续动作与负责人：合同报价来源、供应商价格已由 `CHG-20261101-021` 收口；继续物理真机与独立验收。
