# 材料域 B6.2 独立黑盒复验

日期：2026-10-17
环境：本地 dev，后端 8090、App H5 8080、Web 4175、MySQL `127.0.0.1:3306/decoboon`。
性质：独立黑盒验收与证据采集；未修改实现代码、`state.json`、`08-acceptance.md`、`10-reconciliation.md` 或 B6/B6.1 证据。

## 结论

- B6.2 独立验收结论：**FAIL**。
- 矩阵：**150 项，149 PASS / 1 FAIL**。
- B6 原有 6 个缺陷：**6/6 独立复验 PASS，均验证关闭**。
- 新发现：**B62-DEF-001（P2，OPEN）**，多供应商拆单后作业单价格投影仍停留在 `Unpriced`。
- 因此可确认“6 个修复有效”，但**不满足进入最终 `ACCEPTED` 决策的完整独立证据条件**。

## 六项缺陷逐项结果

| 原缺陷 | 严重度 | B6.2 结果 | 独立证据 |
| --- | --- | --- | --- |
| B6-DEF-001 Designer/PM 读作业单成本 | P1 | PASS / CLOSED | `F01`；Designer/PM 字段白名单无成本字段，Finance/Boss 保留 |
| B6-DEF-002 Designer/PM 读采购金额/供应商 | P1 | PASS / CLOSED | `F02`；履约字段保留，金额/单价/供应商/合同隐藏 |
| B6-DEF-003 Designer/PM 读结算金额/供应商 | P1 | PASS / CLOSED | `F03`；`page|info` 均 403，财务/店长/Boss 可读 |
| B6-DEF-004 重复下单重复采购/应付 | P1 | PASS / CLOSED | `F04`；相同幂等键同载荷只生成 1 份采购/明细/应付；异载荷返回逻辑 409 |
| B6-DEF-005 未到场可结算/数量超到场 | P1 | PASS / CLOSED | `F05`；未到场 preview 为空且 confirm 拒绝；6/10 到场后 7 被拒、6 成功 |
| B6-DEF-006 暂估应付显示 0/未标记 | P2 | PASS / CLOSED | `F06`；`estimated=true`，Web/App 显示待定价/待结算且禁止提前付款；撤销回退正确 |

## 新缺陷

**B62-DEF-001（P2，OPEN）多供应商拆单后作业单价格投影仍停留为 `Unpriced`**

- 实测：同一作业单行拆为 3@100（Priced）与 5（Unpriced），全部到场后将 5@120 月结，采购明细事实分别为 `Priced` / `Settled`；
- 但 `material/sheet/info` 的 sheet/item `priceState` 仍为 `Unpriced`，`purchasePrice/settlementPrice` 为空，DB 同值；
- 影响：SCN-021 场景 B 的多供应商价格分批事实无法在作业单读模型正确表达，界面会继续提示“成本待结算”；采购与应付金额事实本身正确，因此暂定 P2；
- 证据：`blackbox-results.json#B10`、`api-samples.json#multi_supplier_price_state`。

## 回归覆盖

- B6 主链：建单 → PM 测量 → 提交 → 有价/无价下单 → 部分/全部到场 → 逐单/月结预览与确认 → 撤销 → 成本回退；
- 多供应商：同一作业单拆给两个供应商，验证两张采购单、两类付款价格和逐单/月结互斥；
- 权限：Designer/PM/Boss/StoreManager/Finance 的读、下单、结算负向与正向；
- 幂等：相同 payload 重试、相同 key 异 payload、重复结算、助手 `requestId+idempotencyKey`；
- 边界：未到场、部分到场、结算数量超到场、暂估应付、结算与撤销回退；
- 助手：50/50 黄金问题、本人项目、跨企业 404、无证据、审计幂等与不存问答正文；
- 旧路线：`project/purchase/save|batch` 不可达，3 个旧 App 路由空白，材料计划页无直采入口；
- 跨端：Web/App 对同一暂估应付均显示非真实金额语义；App 工作台状态与 Provider 一致。

## 真实 UI

- Web `/finance/payables`，1440×900：目标行显示“待结算”，不显示 `¥0.00`，“安排付款”按钮禁用；`innerWidth=scrollWidth=1440`；Console Errors=0（1 条初始懒加载路由 warning）。
- App H5 应付页，390×844：显示“待定价 / 暂估/待定价 / 待付 待结算 / 结算后安排付款”；目标是付款按钮不存在；`innerWidth=scrollWidth=390`；Console Errors=0。
- App H5 空态：显示“暂无应付账款记录”及恢复说明。
- App H5 权限态：Designer 直达应付页被重定向首页，显示“当前账号无权执行该操作 / 当前账号无权访问该功能”。
- App H5 主材下单工作台：真实显示“已进入采购 / 成本待结算 / 已到场 / 已定价 / 部分到场”，无横向溢出。
- 截图为真实 Playwright 390×844 / 1440×900 渲染；另独立读取 DOM 文本、按钮 disabled、viewport 和 console 指标。

## 清理与边界

- 临时作业单、明细、采购、应付、到场、结算、选材和临时供应商均已删除；
- 恢复后计数：采购 6、采购明细 6、应付 11、到场 3、结算 0、结算明细 0、作业单 0、选材 0、临时供应商 0；
- 两个材料计划恢复 `ReadyToOrder`，`arrived_at/selection_id` 为空，价格/成本恢复原值；
- 项目 44 临时 PM 负责人已恢复为 `...0108`；
- 未执行 DDL/迁移、生产部署、正式小程序提审/发布；
- 55 条 B6.2 助手审计记录保留在追加式 AI 用量账本（50 条黄金问题 + 5 条辅助探针），未删除审计事实；未保留任何临时材料业务行。

## 环境限制

- 8090 JVM 实际 classpath 指向主仓库 `target/classes`，而非 `/tmp` 检出；独立 SHA-256 比对确认 material 133/133、finance payables page 8/8、project purchase page 6/6 的 class 与 `8cc1d11` 完全一致，且 material 源码工作区干净。App/Web 分别使用 `46a5be4` / `04ba3b4`。
- 提供的 StoreManager 属于单门店租户；同租户跨门店负向无法在缺少多门店 StoreManager 账号时独立执行。已用“跨企业且跨门店”的项目目标验证 404，并明确保留该覆盖缺口。

## 独立证据条件判断

- **6 个 B6 缺陷关闭证据：满足。** 已独立复跑权限、幂等、到场门禁、暂估应付与撤销回退，并有真实 API/DB/UI 证据。
- **B6.2 整体 ACCEPTED 条件：不满足。** 新发现 `B62-DEF-001` 仍 OPEN；应由实现者修复后再次独立复验多供应商价格投影。

## 实际命令摘要

- `git -C <checkout> rev-parse --short HEAD`：确认 `8cc1d11` / `46a5be4` / `04ba3b4`。
- `POST /api/auth/session/password-login`（6 个指定账号，密码不落证据）→ `POST /api/{provider}`：执行权限、建单、测量、提交、下单、到场、结算、撤销、助手与跨租户请求。
- `mysql -h127.0.0.1 -uroot ... decoboon -e "select ..."`：核对采购/明细/应付/结算/到场/计划计数与状态，并执行精确 ID 清理及恢复校验。
- `playwright-cli --session b62web|b62app ... goto/snapshot/eval/screenshot/console`：1440×900 与 390×844 真实渲染、DOM/文本/宽度/按钮/Console/截图。
