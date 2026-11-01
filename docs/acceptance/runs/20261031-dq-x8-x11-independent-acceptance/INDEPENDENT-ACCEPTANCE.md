# DQ-X8/X9/X10/X11 同一合同链路 · 独立黑盒验收

- 运行 ID：`20261031-dq-x8-x11-independent-acceptance`
- 日期：2026-10-31（Asia/Shanghai）
- 模式：独立验收 hat，只读黑盒；未修改实现代码、迁移、配置或业务数据
- 初次验收结论：**FAIL**（历史，证据保留）
- 独立复验结论：**PASS**
- 当前验收结论：**PASS**
- 初次验收检查：**140**；通过 **139**，失败 **1**
- 独立复验检查：**55**；通过 **55**，失败 **0**
- 累计执行：**195**；通过 **194**，历史失败 **1**，当前失败 **0**
- 当前开放缺陷：**P0=0，P1=0，P2=0**
- 历史缺陷：**P1=1，CLOSED**（见第 5、8 节）
- 发布边界：未发布生产；未上传、提审或发布微信小程序正式版

## 1. 范围

本轮覆盖：

- `project/contract-change/range`：默认分页、连续翻页、上限、关键词/类型/区域/分组筛选、facets、非法筛选、指定来源回带、Boss/StoreManager/Designer 权限。
- 合同变更明细批量快照：单次加载代码契约、剩余量/待审批占用/可变更量定向测试。
- `project/contract-line/page`：默认 20、上限 50、稳定排序、相邻页、Designer 成本裁剪。
- `project/contract/workbench`：6 项 / ¥977.80、1 变更 / ¥-168.00、成本摘要与角色边界。
- Web 真实页面：发起减项 → 来源选择 → 主材筛选；三档视口；合同档案回归。
- 微信开发者工具原生页面：同一合同来源链路，初始 7 项、主材 3 项。
- 定向测试、全量后端测试、Provider 审计、Web/App 构建与分页门禁。

## 2. 环境与数据

| 项 | 值 |
| --- | --- |
| 后端 | `127.0.0.1:8092`，工作区 HEAD `7b7dc5f0ed3795fbd6d13640f5dfb7f51b8c3dd8` |
| Web | `127.0.0.1:8081`，HEAD `e40ca8baf6203d71f5d0375cc043d424a0aa5873` |
| App | HEAD `f2fb91e0785bf953940257ce97e3ea84fc2ae808` |
| 微信开发者工具 | `dist/build/mp-weixin`，automation `127.0.0.1:9430`，构建注入 `http://127.0.0.1:8092/api/` |
| 数据库 | `decoboon`，只做 SELECT / CHECKSUM |
| 项目/合同 | `241940507872198656` / `241939522005237760` |
| 角色 | Boss、StoreManager、Designer |
| 既有 8090 | PID 34129，验收前已存在，全程保留且未终止 |

验收后相关数据未变化：

| 表 | 行数 | 业务和 | CHECKSUM |
| --- | ---: | ---: | ---: |
| `db_contract_line` | 6 | 成本 ¥866.50 | 2999847383 |
| `db_contract_change` | 1 | ¥-168.00 | 778537963 |
| `db_contract_change_item` | 1 | ¥-168.00 | 2916314061 |
| `db_project_contract` | 1 | 当前额 ¥858.69 | 369397714 |
| `db_project` | 1 | 合同额 ¥858.69 | 528973700 |

相关行业务 fingerprint 前后一致，详见 `raw/db-baseline.txt`、`raw/db-after.txt`、`raw/integrity-comparison.json`。

## 3. 用例矩阵与结果

### 3.1 DQ-X11 `project/contract-change/range`

| 用例 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- |
| 默认分页 | current=1、size=20、total=7、6 原合同+1 已生效 | 1/20/7/6/1 | PASS |
| `size=2` 页 1/2/4 | 2/2/1 条，无重复 | 2/2/1 条 | PASS（单看三页） |
| 连续翻页 1→4 | 覆盖 7 条，无遗漏、无重复 | 仅 6 条、5 个唯一 ID；缺 2 行，已生效目标重复 | **FAIL（P1）** |
| `size=500` | 服务端截为 50 | size=50，返回 7 | PASS |
| `itemType=Material` | 3 条主材 | 3 条，全部 Material | PASS |
| `keyword=全屋保洁` | 1 条 | 1 条，名称匹配 | PASS |
| `spaceName=__none__` | 7 条 | 7 条 | PASS |
| 不存在区域 | 0 条 | 0 条 | PASS |
| `group=effective/contract` | 1/6 条 | 1/6 条 | PASS |
| 类型+分组联合筛选 | 1 条主材已生效 | 1 条 | PASS |
| facets | 施工3、综合1、主材3，空间综合7 | 一致 | PASS |
| 非法类型/分组 | 业务 400，不返回来源 | code=400；`分项类型不合法` / `来源分组不合法` | PASS |
| 指定来源回带 | 不受当前页筛选影响 | selected 正确回带 | PASS |
| Boss/StoreManager/Designer | 三角色同范围 7 条 | 三角色均成功且 ID 集合一致 | PASS |
| 无令牌 | 拒绝 | `tokenIsUndefined` | PASS |
| 不存在合同 | 404 | 业务 404 | PASS |

API 断言共 91 项：90 PASS、1 FAIL。完整原始响应见 `raw/api/`，汇总见 `raw/api-acceptance-results.json`。

### 3.2 批量快照与数量口径

| 用例 | 结果 |
| --- | --- |
| 变更明细单次加载 | PASS；调用链只出现一次合同级明细查询，所有来源共享同一 `changeItems` |
| 批量投影不再逐来源查库 | PASS；`ContractChangeRangeContextTest.batchProjectionMustReuseProvidedItemsWithoutRepositoryQuery` 验证零仓储交互 |
| Provider 只加载一次 | PASS；`RangeProviderTest` 使用 `verify(itemRepository, times(1))` |
| 剩余量/待审批占用/可变更量 | PASS；10→生效后剩余7→待审批占用1→可变更6 的断言通过 |
| 定向测试 | PASS；`RangeProviderTest` 5/5，`ContractChangeRangeContextTest` 6/6，共 11/11 |

证据：`raw/batch-snapshot-contract.txt`、`raw/backend-targeted-tests.log`。

### 3.3 DQ-X8/X9/X10 回归

| 用例 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- |
| 合同行默认分页 | 20 条页大小、total=6 | size=20、totalElements=6 | PASS |
| 合同行上限 | `size=500` 截为 50 | size=50 | PASS |
| 合同行稳定排序/翻页 | sort ASC、lineId ASC，相邻页无重复 | 6 个 ID 完整且唯一 | PASS |
| 合同行角色范围 | 三角色同一合同范围 | ID 集合一致 | PASS |
| Boss/StoreManager 成本 | cost 返回 | 6 行均返回 cost | PASS |
| Designer 成本 | cost 不返回 | cost 字段 0 个 | PASS |
| workbench 汇总 | 6 项 / ¥977.80 | 一致 | PASS |
| 变更汇总 | 1 变更 / ¥-168.00 | 一致 | PASS |
| 当前合同额 | ¥858.69 | 三角色一致 | PASS |
| 成本摘要 | 成本不完整，预计 ¥746.50 / 实际 ¥88.00 / 缺 1 项 | Boss、StoreManager 一致；Designer 无 cost 对象 | PASS |
| Web 合同档案 | 6 行 / ¥977.80 / 1 变更 / ¥-168.00 | 实际渲染与请求一致 | PASS |

`customer/contract-line/page` 的业主身份直接黑盒未执行，原因见“未覆盖项”。

### 3.4 Web 真实页面

| 用例 | 实际 | 结果 |
| --- | --- | --- |
| 发起减项并打开来源 | 来源选择器初始 7 项 | PASS |
| 主材筛选 | 3 项：实木复合地板、抛釉砖、已生效实木复合地板 | PASS |
| 初始请求体 | `contractId,current:1,size:20` | PASS |
| 主材请求体 | `contractId,current:1,size:20,itemType:"Material"` | PASS |
| 网络 | 相关响应 200；全链路无 HTTP 4xx/5xx | PASS |
| Console | error=0，exception=0 | PASS |
| 1440×900 | 页面 scrollWidth=clientWidth=1440，无横向溢出 | PASS |
| 1280×900 | scrollWidth=clientWidth=1280，无横向溢出 | PASS |
| 1024×900 | scrollWidth=clientWidth=1024，无横向溢出 | PASS |

截图：`screenshots/web-change-create-initial-1440.png`、`screenshots/web-change-source-material-{1440,1280,1024}.png`、`screenshots/web-contract-lines-{1440,1280,1024}.png`。

视觉签字：**未签**。当前执行环境不能读取图片内容，因此只声明 DOM、请求、网络、溢出自动检查通过，不冒充人工视觉验收。

### 3.5 微信开发者工具原生页面

| 用例 | 实际 | 结果 |
| --- | --- | --- |
| 初始来源 | 共 7 项，7 个来源卡 | PASS |
| 主材筛选 | 共 3 项，3 个来源卡 | PASS |
| 页面路径 | `subpackages/project/pages/change-create` | PASS |

证据：`raw/app-native-range-check.json`、`raw/app-native-console.log`、`screenshots/app-range-native-material.png`（788×1706）。

## 4. 构建与门禁

| 检查 | 结果 |
| --- | --- |
| `RangeProviderTest` + `ContractChangeRangeContextTest` | PASS，11/11 |
| 后端全量 `mvn test` | PASS，1186/1186，3分52秒 |
| Provider 响应审计 | PASS，543 Provider / 0 finding |
| Provider 审计负向自测 | PASS |
| 前端 Provider 契约 | PASS，unmatched=0 |
| Web build（含分页/类型/编号/品牌等门禁） | PASS |
| App type-check | PASS |
| App UI 门禁 | PASS |
| App 分页门禁 | PASS |
| App 合同行分页门禁 + 自测 | PASS，5/5 负向样本拦截 |
| App 合同来源分页门禁 + 自测 | PASS，4/4 负向样本拦截 |
| App `build:mp-weixin` | PASS |
| App 分包门禁 | PASS；主包 1,079,696 B，总包 4,229,223 B |
| App 8092 开发模式原生验收构建 | PASS；构建产物注入 `http://127.0.0.1:8092/api/`。直接以 production 模式注入 HTTP 曾被 HTTPS 门禁拒绝，这是预期保护；原生测试改用 development mode 构建 |

## 5. 缺陷

### P1 · DQ-X11 连续翻页遗漏并重复来源（历史记录，已关闭）

- 复现：`project/contract-change/range`，`contractId=241939522005237760`，`current=1..4`，`size=2`。
- 预期：连续四页覆盖 `total=7`，共 7 个唯一来源。
- 实际：页 1/2/3/4 分别 2/2/1/1 条；连续遍历仅 6 条、5 个唯一 ID。
- 遗漏：原合同行 `241939522063958018`、`241939522063958019`。
- 重复：已生效目标 `1793373550452425989` 同时出现在第 3、4 页。
- 影响：当前 7 个来源小于默认 20，默认页面未触发；来源数超过一页后，选择器会漏项并重复。
- 定位线索：`Range.java` 将全局 offset 当作零基页码传给 `PageRequest`（第 170/174/177 行）；本轮只做定位记录，未修改代码。
- 初次证据：`raw/range-pagination-defect-evidence.json`、`raw/api/008-range-size2-page1.json` 至 `raw/api/011-range-size2-page4.json`。
- 当前状态：**CLOSED**。独立复验四页顺序 2/2/2/1、7 个唯一 ID、`hasMore=true/true/true/false`，见第 8 节。

## 6. 未覆盖项与限制

1. 未直接黑盒业主身份 `customer/contract-line/page`：未提供业主凭证，且本轮禁止创建临时账号。DQ-X9 仅由全量后端测试和 App 契约门禁间接回归。
2. 未做视觉签字：截图已保存，但当前环境不能读取图片内容。
3. 当前真实数据仅 7 个来源，Web/原生页面没有触发翻页器；初验分页缺陷与复验修复均由 `size=2` API 连续翻页独立确认。
4. 未发布生产，未上传/提审/发布微信小程序正式版。
5. 验收期间观察到其他未跟踪文档并发出现；未读取为指令、未修改、未删除。相关 tracked 工作区状态前后一致。
6. 原始网络证据中的 Token、密码和授权头已脱敏；未保存明文凭据。
7. 本轮启动的 8092、8081、9430 均已停止；既有 8090（PID 34129）保持运行。

## 7. 结论

初次验收为 **FAIL**：`project/contract-change/range` 连续分页存在 P1 契约缺陷，P0=0，P1=1，P2=0。该记录和原始失败证据完整保留。

实现修复后完成独立复验，当前结论为 **PASS**：连续四页严格覆盖 7 个唯一来源，回归、定向测试与 Provider 审计通过，历史 P1 已关闭。当前开放缺陷 P0=0，P1=0，P2=0。


## 8. 复验记录（2026-10-31）

### 8.1 复验结论

- 结论：**PASS**
- 独立复验检查：**55/55 PASS**
- 当前开放缺陷：**P0=0，P1=0，P2=0**
- 历史缺陷：`DQ-X11-P1-RANGE-PAGINATION`，状态 **CLOSED**
- 初次 FAIL 结果及原始证据保留：`raw/result-initial-fail.json`、`raw/api/008-range-size2-page1.json` 至 `raw/api/011-range-size2-page4.json`、`raw/range-pagination-defect-evidence.json`

### 8.2 实现差异与只读边界

复验前确认，自初次验收结束之后，后端 `src/` 下只有以下两个 tracked 文件发生变化：

- `src/main/java/com/th4/edge/decoboon/project/service/contractchange/range/support/Range.java`
- `src/test/java/com/th4/edge/decoboon/project/context/RangeProviderTest.java`

修复契约存在：

- `PageRequest.of(page - 1, pageSize, LINE_SORT)`
- 当前页按 `lineLimit` 截取
- 新增 `consecutivePagesMustCoverContractRowsAndEffectiveSourceWithoutDuplicates`
- 测试严格断言 `1000L,1001L,1002L,1003L,1004L,1005L,201L`

复验前后源码 SHA-256 与相关数据库表 checksum、业务和、行指纹完全一致。证据：`raw/reaccept-db-baseline.txt`、`raw/reaccept-db-after.txt`、`raw/reaccept-integrity.json`。

### 8.3 独立 API 复验

自行启动最新代码 `127.0.0.1:8092`，Boss 登录后自行请求四页，不复用实现方 API 文件：

| 页面 | 返回条数 | `hasMore` |
| --- | ---: | --- |
| page=1 | 2 | true |
| page=2 | 2 | true |
| page=3 | 2 | true |
| page=4 | 1 | false |

连续遍历顺序严格等于数据库期望：

`241939522063958016, 241939522063958017, 241939522063958018, 241939522063958019, 241939522063958020, 241939522063958021, 1793373550452425989`

- 7 条、7 个唯一 ID，无遗漏、无重复。
- 默认：total=7，6 原合同 + 1 已生效。
- Material：total=3。
- keyword=`全屋保洁`：total=1。
- size=500：服务端 size=50。
- Boss/StoreManager/Designer：均成功，total=7，ID 集合一致。
- 独立 API 断言：34/34 PASS。

证据：`raw/independent-reaccept-api.json`、`raw/independent-reaccept-console.log`。

### 8.4 测试与审计

| 检查 | 结果 |
| --- | --- |
| 新增连续页测试单独执行 | PASS，1/1 |
| `RangeProviderTest` | PASS，6/6 |
| `ContractChangeRangeContextTest` | PASS，6/6 |
| 两类定向测试合计 | PASS，12/12 |
| Provider 响应审计 | PASS，543 Provider / 0 finding |
| Provider 审计负向自测 | PASS |
| 前端 Provider 契约 | PASS，unmatched=0 |

实现方另提供全量 1187/1187 PASS 证据 `raw/backend-full-tests-reaccept.log`，本轮将其作为补充证据复核，不冒充独立执行。

### 8.5 复验结果文件

- 当前聚合结果：`result.json`
- 初次 FAIL 结果快照：`raw/result-initial-fail.json`
- 独立 API：`raw/independent-reaccept-api.json`
- 新增测试：`raw/reaccept-new-consecutive-test.log`
- 定向测试：`raw/reaccept-backend-targeted-tests.log`
- 数据/源码完整性：`raw/reaccept-integrity.json`
- 进程清理：`raw/reaccept-process-cleanup.txt`
- domain state 只读证据：`raw/reaccept-domain-state-readonly.txt`

本轮自行启动的 8092 已停止；既有 8090（PID 34129）保留。未修改实现、迁移、配置、业务数据或 domain state。
