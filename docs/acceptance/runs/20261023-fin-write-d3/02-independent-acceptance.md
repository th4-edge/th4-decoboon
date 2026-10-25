# FIN-WRITE-D3 独立复验报告

- 结论：**PASS**
- 验收对象：`FIN-WRITE-D3` 店长本店凭证生成、查看、重打与跨端动作对齐
- 业务依据：`ANS-BIZ-20261022-010`、`T12-BIZ-20261022-010`、`DEC-089`、`BR-055`
- 验收日期：2026-10-23
- 验收角色：独立验收；只读实现、业务规则、权限、既有测试与 DDL
- 隔离快照：`/tmp/fin-write-d3-reaccept.IMTrgp`
- 后端运行时：Java `22.0.1`，Maven `3.9.16`
- 结论范围：仅代表 `FIN-WRITE-D3`，不代表 `PI-20261022-016` 整体闭环或已发布

## 一、复验结论

上一轮 F1 已关闭：

1. `document/voucher/page`、`document/voucher/report` 已收窄为 `Boss / StoreManager / Finance`，`Pm / Designer` 不再可读。
2. App 已增加凭证列表、模板列表和模板编辑的页面级权限规则：
   - 凭证列表、模板列表：`view:finance`
   - 模板编辑：`edit:doc-template`
3. HTTP 用例已增加设计师访问凭证分页返回 403 的独立断言。
4. Web 的 Designer/Pm 原本不装载凭证路由，现在与后端角色矩阵一致；店长 Web/App 入口仍保持可达。

未发现新的 D3 阻断项。后端主链、门店范围、模板独立授权、App/Web 权限与构建均通过独立验证。

## 二、独立核验边界

本次核验：

- 核对 latest diff，而不复用上一轮结论；
- 使用全新隔离副本 `/tmp/fin-write-d3-reaccept.IMTrgp`；
- 后端使用 Java 22 重新编译并执行定向与相关回归；
- Web、App 使用隔离副本执行契约检查和构建；
- 未修改 `src/main`、`src/test`、前端代码、权限、业务规则或 DDL；
- 未把开发自测日志直接作为验收证据。

未执行：

- 真实账号登录后的 Web/App 页面截图与逐按钮复核；
- 真机 PDF 打开、系统打印和转发；
- 全量后端测试；
- `rebuildForPayment` 的跨店 HTTP 动态负向用例。

上述未覆盖项不改变本切片代码与权限边界的 PASS 结论，但仍是正式发布前门禁。

## 三、F1 关闭证据

### 3.1 后端凭证查看角色收窄

静态证据：

- `src/main/java/com/th4/edge/decoboon/document/service/voucher/page/support/Page.java:95-96`：只允许 `Boss / StoreManager / Finance`。
- `src/main/java/com/th4/edge/decoboon/document/service/voucher/report/support/Report.java:79-80`：同样只允许 `Boss / StoreManager / Finance`。
- 店长仍使用 `DataScopeSpecification.visible(VoucherRecord.class, ...)`，不存在因角色收窄绕过门店范围的情况。
- 报表新增 `StoreFilterContext` 门店筛选，与分页保持同一门店口径。

HTTP 证据：

- `FinanceStoreScopeHttpAcceptanceTest.java:999-1001`：Designer 调用 `document/voucher/page` 返回失败，错误码为 `403`。
- Java 22 独立执行该用例通过。

### 3.2 App 页面级守卫补齐

静态证据：

- `th4-decoboon-app/src/roles/guard.ts:79`：`financeVouchers` 要求 `view:finance`。
- `th4-decoboon-app/src/roles/guard.ts:80`：`financeVoucherTemplates` 要求 `view:finance`。
- `th4-decoboon-app/src/roles/guard.ts:81`：`financeVoucherTemplate` 要求 `edit:doc-template`。
- `canVisit` 现在会命中这些规则，不再对凭证页面走无规则默认放行。
- StoreManager / Finance 具备 `view:finance`；BOSS 通过内部能力并集具备；Designer / Pm 不具备，与 Web 和后端一致。
- 模板编辑不因 `view:finance` 自动放行，仍要求独立的 `edit:doc-template`。

### 3.3 店长 Web/App 入口

Web：

- `src/roles/routes.ts:216-231`：店长装载全部财务路由，包含收付凭证和凭证模板。
- `src/roles/workbenchHome.ts:501-579`：店长首页包含资金台账、凭证模板、收付凭证卡片。
- `src/views/finance/FinanceLedgerView.vue:155-178`：可从台账生成凭证。
- `src/views/finance/FinanceVouchersView.vue:101-104、150-180`：可查看/打印凭证并进入模板页。

App：

- `src/subpackages/finance/pages/ledger.vue:50-51、202-212`：`edit:finance` 下提供“查看已有凭证”和“选记录生成凭证”。
- `src/subpackages/finance/pages/vouchers.vue:346、433-445、548-550`：可查看/打印凭证并进入模板入口。

结论：Web/App 店长入口与后端角色及门店范围一致。

## 四、后端主链核验

### 4.1 VoucherContext#build

静态证据：

- `VoucherContext.java:115-117`：允许 `Boss / Finance / StoreManager`。
- `VoucherContext.java:121-128`：Ledger 按 ID 查询叠加 `DataScopeSpecification.visible`；无权与不存在统一表现为缺失。
- `VoucherContext.java:130-134`：一次凭证必须同门店。
- `VoucherContext.java:136-138`、`VoucherContext.java:282-296`：同一来源组合重打返回既有凭证。
- `VoucherContext.java:253-258`：从原记录路径重新签名。
- `VoucherContext.java:329-335`：付款来源要求企业与门店同时一致。

结论：**PASS**。店长本店生成、跨店拒绝、同号同文件重打的后端主链成立。

### 4.2 VoucherContext#rebuildForPayment

静态证据：

- `VoucherContext.java:204-212`：允许 `Boss / Finance / StoreManager`，按目标 Payment 数据范围读取。
- `VoucherContext.java:214-238`：仅重渲染目标 Payment 对应的有效凭证，沿用原凭证号和对象路径。
- 无权 Payment 在重渲染和版本留痕前返回 404 语义。

结论：**PASS（静态）**。跨店动态用例仍未覆盖，但静态范围和写法无缺口。

### 4.3 模板配置独立授权

- 凭证生成 Provider：`document/voucher/build`
- 模板维护 Provider：`document/template/save|publish|disable`
- `DocumentTemplateContext.java:211-215`：模板维护独立要求 `Boss / StoreManager / Finance`
- 保存为企业级模板，版本快照记录发布人、发布时间和配置；历史版本不回溯

结论：**PASS**。模板配置、生成、重打为相互独立能力。

## 五、独立动态结果

### 5.1 后端 Java 22

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o \
  '-Dtest=FinanceStoreScopeHttpAcceptanceTest#storeManagerCanIssueAndReprintOwnStoreVoucherButNotOtherStore+headFinanceAndBossSeeAllStoresAndCanDrillDownOverHttp+storeFinanceSeesOnlyOwnStoreOverHttp,VoucherBuildAcceptanceTest,DocumentTemplateContextTest' \
  test
```

结果：**19/19 PASS，BUILD SUCCESS**

拆分：

- `FinanceStoreScopeHttpAcceptanceTest`：`3/3 PASS`
- `VoucherBuildAcceptanceTest`：`6/6 PASS`
- `DocumentTemplateContextTest`：`10/10 PASS`

覆盖：

- 店长本店生成凭证成功；
- 相同来源重复生成返回同一 `voucherId / voucherNo`；
- 店长本店凭证分页可见；
- 店长使用他店 Ledger 生成凭证返回 403/404；
- Designer 访问凭证分页返回 403；
- BOSS / 总部财务企业范围未回归；
- 门店财务本店范围未回归；
- 凭证落库、重打幂等、合并、零金额、工资凭证重渲染；
- 模板保存、发布、停用和版本快照。

### 5.2 App

命令：

```bash
npm run type-check
npm run check:action
npm run check:finance-ia
npm run check:navigation
```

结果：

- `type-check`：PASS
- 动作与按钮契约：PASS
- 财务工作台信息架构：PASS
- 导航与页面权限契约：PASS

### 5.3 Web

命令：

```bash
npm run check:actions
npm run check:permissions
npm run check:workspaces
npm run build
```

结果：

- 业务动作注册与服务端闭环范围检查：PASS
- 业务权限、角色、数据范围和动作角色检查：PASS
- 对象工作空间可达性检查：PASS
- `vue-tsc --noEmit`：PASS
- 客户端文案门禁：PASS
- Vite production build：PASS，`2817` 个模块

## 六、必验覆盖矩阵

| 必验项 | 结果 | 独立证据 |
| --- | --- | --- |
| 店长本店生成凭证 | PASS | Java 22 HTTP；DataScope 静态核验 |
| 店长本店查看凭证 | PASS | HTTP 分页；Web/App 入口 |
| 店长本店重打同号 | PASS | HTTP 同 `voucherId / voucherNo`；原记录返回静态证据 |
| 重打同文件 | PASS（静态） | `existing()` 返回原路径，`signed()` 仅重新签名 |
| 店长跨店生成/重打拒绝 | PASS | HTTP 他店 Ledger 403/404；Payment 范围静态核验 |
| BOSS 不回归 | PASS | Java 22 企业范围 HTTP；凭证与模板回归 |
| 总部财务不回归 | PASS | Java 22 企业范围 HTTP；默认总店升企业范围 |
| 门店财务不回归 | PASS | Java 22 本店范围 HTTP |
| PM/Designer 不可读凭证 | PASS | 后端角色收窄；Designer HTTP 403 |
| App 凭证页面守卫 | PASS | `view:finance` 页面规则 |
| App 模板编辑守卫 | PASS | `edit:doc-template` 页面规则 |
| Web/App 同角色入口一致 | PASS | Web 路由/首页卡片；App 台账与凭证入口 |
| 模板配置独立授权 | PASS | 独立 Provider、企业级保存、版本留痕 |
| Web 动作/权限/构建 | PASS | `check:actions`、`check:permissions`、`build` |
| App 类型与导航契约 | PASS | `type-check`、`check:action`、`check:finance-ia`、`check:navigation` |
| 真实登录页面与真机 PDF | 未覆盖 | 需在 UI 复核/发布门禁补齐 |
| 全量后端测试 | 未执行 | 正式发布前仍需执行 |

## 七、残留风险与未覆盖项

1. 真实 Web/App 登录态页面截图、点击路径、PDF 打开和系统打印尚未独立执行；本次 PASS 覆盖代码、权限、契约、接口和构建，不代表 UI 已通过。
2. 重打同文件由原路径静态实现证明；定向测试未单独断言两次返回的 `path / sha256 / url` 完全相同。
3. `rebuildForPayment` 未有店长跨店 Payment 的 HTTP 动态用例，当前仅有数据范围静态证据。
4. Web 店长左侧静态菜单仍将资金台账标为“只读”，与店长实际写权限存在文案不一致；首页业务卡片已提供凭证与模板入口，不影响本切片权限结论，但建议后续修正文案。
5. 未执行全量后端测试和生产发布冒烟；不得据此宣布发布就绪或把 `PI-20261022-016` 标记为整体闭环。

## 八、验收处置

- `FIN-WRITE-D3` 独立复验：**PASS**
- 上一轮 F1：**CLOSED**
- `PI-20261022-016`：仍不得因 D3 PASS 标记整体闭环
- 下一门禁：按计划进入 D5 组合验收前，补齐真实页面/真机证据、全量测试和发布前回归；D1/D4 的 DDL 仍需单独确认
