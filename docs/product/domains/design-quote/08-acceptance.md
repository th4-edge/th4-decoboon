# 设计方案与设计报价审视 · 验收记录

## 范围

- Batch：B1
- 角色：BOSS、店长、财务、设计师
- 环境：本地 dev 后端 `127.0.0.1:8090`；Web `127.0.0.1:8080`；App 同源 H5 `127.0.0.1:5173`；微信小程序 release 构建
- 数据/租户：麦鸿装饰 `com_id=1790066191585976744`；5 个方案、246 个设计分项、5 份报价
- 说明：开发侧证据完成后，已由独立验收 hat 使用 `raw/run_acceptance.py` 做只读黑盒复核，未修改实现代码。

## 用例

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 | 有当前正式报价的方案 | 列表/详情显示正式报价、状态和方案计算价 | `Q1791283627581`、`¥65,406.10`、方案计算价 `¥62,291.52` | Web/App 列表与详情截图 | PASS |
| 正向 | 无正式报价的方案 | 回退显示方案计算价，不伪造报价编号 | `11.03万`、`11.8万` 等正常显示 | `boss-list-*.png`、`app-h5-*`、`app-h5-designer-workbench.png` | PASS |
| 正向 | 详情成本与毛利 | BOSS/店长/财务可见；成本完整时计算毛利 | 可见预计成本和毛利；成本不完整时显示不可计算 | Web/App 财务详情截图 | PASS |
| 正向 | 设计师执行视图 | 价格可见，成本/毛利不可见 | App 设计工作台显示 `11.8万`，无成本/毛利；详情 API 成本字段 0 | `app-h5-designer-workbench.png`、HTTP JSON 检查 | PASS |
| 正向 | 只读经营查看 | 不出现审批、核价、退回、通过、驳回动作 | Web/App DOM 与按钮扫描无写动作 | `07-ui-review.md` | PASS |
| 反向 | 非法设计 ID | 返回不存在或无权访问，不返回其他方案 | `404 设计方案不存在或无权访问` | HTTP 负向证据 | PASS |
| 越权 | 临时二店店长/门店财务访问总店方案 | 列表 0 条；详情 404；显式跨店筛选 403 | 列表 0、详情 404、筛选 403 | `raw/http-negative-checks.json` | PASS |
| 越权 | 其他企业 BOSS 访问麦鸿方案 | 列表 0 条；详情 404 | 列表 0、详情 404 | `raw/http-negative-checks.json` | PASS |
| 越权 | 设计师直达 App 运营查看页 | 不渲染经营查看内容 | 原生页面 `.dr` 与方案卡均为 0；H5 直链回退首页 | `app-mp-designer-review-denied.png`、`app-h5-designer-review-denied.png` | PASS |
| 幂等 | 只读详情重复请求 | 不改变方案、报价、成本或状态 | 仅读查询，无写接口调用 | 代码/接口审查 | PASS |
| 边界 | 1 万元以下金额 | 只显示一次货币符号 | `¥82`，不再出现 `¥¥82` | App H5 列表 | PASS |
| 边界 | 1 万元以上金额 | 万元显示且无截断 | `6.54万`、`11.8万`、`118,025.45` | Web/App 截图 | PASS |
| 跨端 | App/Web 同一方案价格 | 正式报价优先且金额一致 | 正式报价 `65,406.10`；方案计算价 `62,291.52` | Web/App 截图 | PASS |
| 性能 | 列表/详情各 10 次 | P95≤500ms | 列表 P95 145.4ms、详情 P95 132.6ms | `raw/http-performance.csv` | PASS（当前数据量） |
| 性能 | 10 倍数据量（5→50 方案） | SQL 条数稳定、P95≤500ms | 基线 P95 54.2ms；50 方案 P95 72.1ms；批量上下文固定 4 类查询 | `raw/performance-10x.json`、`raw/http-performance-10x.csv` | PASS |
| 原生端 | 微信开发者工具页面级点击 | BOSS/店长/财务/设计师主路径可点验 | 四角色原生页面实际打开；列表/详情/成本/权限表现符合 | `screenshots/app-mp-*` | PASS |
| 独立验收 | 2026-10-22 只读黑盒复核 | 关键业务、权限、金额、负向和只读副作用全部通过 | 24/24 检查通过，无 P0/P1 缺陷 | `../20261022-design-quote-independent/raw/acceptance-summary.json` | PASS |

## 构建与测试

- 后端定向：`DesignPriceContextTest`、`DesignCostContextTest`、`DesignSchemeInfoFormulaTest`、`DesignerWorkbenchAggregationTest` 13/13 PASS。
- 后端全量：`mvn -o test`，901/901 PASS。
- App：`type-check`、UI、导航、设计分项、动作门禁 PASS。
- App release：`build:mp-weixin:release` PASS；主包 937071 B，最大分包 664803 B，总包 3701188 B，17 分包/167 页。
- Web：构建与角色可达、权限、表单门禁 PASS（见前序开发验证）。

## 数据与凭据恢复

- BOSS、店长临时登录哈希已按 `raw/credential-backup.tsv` 恢复，失败次数清零、锁定时间清空。
- 临时二店已删除；店长/门店财务归属已恢复总店。
- 其他企业测试凭据已恢复为原手机号、类型、状态和空哈希。
- 10 倍性能测试新增 45 个临时方案，已在测试后删除；企业方案数恢复为 5。
- 未修改生产数据库，未发布生产。

## 结论

- 状态：PASS（独立只读验收）
- 已通过：业务口径、价格投影、成本裁剪、Web 渲染、App 原生页面、跨店/跨企业负向、10 倍数据量性能、App release 构建。
- 独立验收：24/24 检查通过，证据见 `../20261022-design-quote-independent/`。
- 未覆盖：真实手机系统能力（相机/相册/订阅消息等本功能不涉及）。
- 遗留风险：`GAP-061` 报价过期口径仍未关闭，本批未展示“报价过期”。
- 门禁结论：可推进 `ACCEPTED`；生产发布仍需用户明确确认。

## DQ-X2：设计师五阶段 App/Web 统一（试运营验收）

- Batch：`DQ-X2`
- 验收模式：试运营单人模式只读验收，不等同第三方独立验收。
- 环境：dev 后端、Web 真实 Chromium 1440×900、微信开发者工具原生模拟器。
- 身份：`18660219918 / 张伟 / Designer / 安徽昭青网络科技`。

| 类型 | 用例 | 期望 | 实际 | 结果 |
| --- | --- | --- | --- | --- |
| 正向 | 五阶段入口与顺序 | 两端口径一致 | App/Web 均为待我跟进→待我量房→待我设计→待我谈单→待我签约 | PASS |
| 正向 | 五页标题 | 与菜单一致 | 两端 H1/页面标题全部一致 | PASS |
| 正向 | 跟进筛选 | 五个筛选项一致 | 全部待办/已逾期/今日跟进/3天内/未安排 | PASS |
| 正向 | 量房筛选 | 六个筛选项一致 | 全部任务/逾期/今天/已预约/待预约/待交设计 | PASS |
| 正向 | 设计筛选 | 四个筛选项一致 | 全部/待设计/设计中/方案完成 | PASS |
| 正向 | 谈单筛选与关键词 | 两端同一业务集合 | 草稿命中 `Q1790383677036`；周先生搜索为空 | PASS |
| 正向 | 签约筛选与关键词 | 两端同一业务集合 | 待发起合同=周先生、待企业确认=空、已生效=王亮 | PASS |
| 跨端 | 同一业务样本 | 五阶段数据一致 | 跟1/量1/设2/谈1/签2，样本标识两端一致 | PASS |
| 边界 | App 分页表达 | 仅下拉刷新+触底加载 | 无上一页/下一页/页码/总页数 | PASS |
| 边界 | Web 分页表达 | 保留分页组件 | 谈单/签约 `el-pagination` 可用 | PASS |
| 日志 | Web console | 0 error | 0 error | PASS |
| 副作用 | 只读验收 | 无业务写和 DB 变更 | 仅页面进入、搜索、筛选、截图 | PASS |

- App 自动检查：24/24 PASS。
- Web 自动检查：20/20 PASS。
- 汇总证据：`docs/acceptance/runs/20261024-designer-five-stage-unified/acceptance-summary.json`。
- 结论：`PASS`（试运营单人模式）。

### 独立验收 hat 复核（2026-10-24）

- API 黑盒：**103/103 PASS**，覆盖五个 Provider 的全部筛选值、主次页 total 一致性、每页 ≤20 和单请求 ≤500ms。
- App 原生黑盒：**24/24 PASS**，重新验证五入口顺序、五页标题、筛选、关键词、旧名称/页码排除。
- Web 黑盒：**6/6 PASS**，同一设计师账号 1440×900 五页标题、筛选和控制台均符合。
- 构建门禁：App `build:mp-weixin:release` PASS；App 分包 PASS；Web build、permissions、workbenches、workspaces PASS。
- 缺陷：P0=0，P1=0。
- 证据：`docs/acceptance/runs/20261024-designer-five-stage-unified-independent/`。
- 说明：这是独立验收 hat 的只读复核，不冒充第三方验收；不包含小程序上传、正式提审或生产发布。

## DQ-X1：待我谈单查询整改 · 独立验收（2026-10-24）

- 验收方式：独立验收 hat 只读黑盒，不修改产品实现。
- 结果：**18/18 PASS**，P0=0，P1=0。
- 权限：Boss 22、店长本店 22、设计师本人 14；无令牌拒绝；Finance/Marketer/Pm 403；其他企业 Boss/店长均为 0。
- 边界：店长当前页 20 条全部属于本店；设计师当前页 14 条全部由本人创建；`size=500` 被限制为 100。
- SQL：一次列表请求固定 3 条 SELECT（count、ID 页、当前页实体批量预取），无逐行 Customer/Property 查询。
- 副作用：quote/customer/property 行数前后均为 `85/73/67`。
- 性能：10 次请求 P95 47.4ms。
- 构建：后端干净全量 1098/1098 PASS；Web build 与分页/类型/文案门禁 PASS。
- 证据：`docs/acceptance/runs/20261024-dq-x1-independent/`。

## DQ-X8～X11：合同分页、汇总与来源选择器链路 · 独立验收

- 验收方式：独立验收 hat 只读黑盒，实现冻结后先初验，P1 修复后再独立复验。
- 初验：140 项，139 PASS / 1 FAIL；P1=`DQ-X11-P1-RANGE-PAGINATION`，range 连续翻页漏 2 条并重复 1 条已生效目标。
- 修复：`Range.java` 原合同行查询改为正确使用 0 基 `PageRequest(page-1,pageSize)`；新增连续页覆盖测试。
- 复验：55/55 PASS；连续页严格覆盖 7 个唯一来源，默认/筛选/上限/三角色权限/批量快照/Provider 审计通过。
- 当前开放缺陷：P0=0，P1=0，P2=0；历史 P1 已 CLOSED。
- 后端全量补充证据：1187/1187 PASS（实现方执行，独立代理复核日志）。
- Web：合同档案与增减项来源真实页面、三档视口、network/console 检查通过。
- 小程序原生：来源初始 7 项、主材 3 项，通过。
- 未覆盖：业主身份 `customer/contract-line/page` 直接黑盒；视觉签字；当前库无双租户账号，跨企业黑盒未执行。
- 证据：`docs/acceptance/runs/20261031-dq-x8-x11-independent-acceptance/`。
- 结论：**ACCEPTED**；未发布。
