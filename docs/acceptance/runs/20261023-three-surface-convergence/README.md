# 装得清三端收敛验证（2026-10-23）

## 目的

停止继续扩张新业务切片，冻结当前三仓工作区为一个可复现快照，验证当前“最新代码树”是否具备进入提交整理和独立验收的基础。

本批次只做收敛验证和阻断项修复，不新增业务对象、不改变权限/金额/状态机、不执行 DDL、不发布。

## 快照

```text
/tmp/zhuangdeqing-converge.JgGxh1
```

快照包含：

- 后端：`pom.xml + src + config + data + docs/design + docs/operations + docs/product`
- App：当前工作区源码、脚本、配置和依赖软链
- Web：当前工作区源码、脚本、配置和依赖软链

源码摘要：

```text
backend HEAD 7cf83fa
app HEAD     aa387a8
web HEAD     0cbdc06

backend src digest 4e104dc38fdc93af8fd065f6025bbf99130669eaf8d076584eb01539614488cc
app src digest     69f10122e4cebf16c6fa32b8a9d1eb64961005cce917de863e72e2aa6e446e55
web src digest     ca10da9eeed8cd40e1b8b310c265323447e2234eb393c79d4ea6cf416ea40f93
```

详细关键文件哈希见快照内 `MANIFEST.txt`。

## 验证结果

### 后端

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home/bin:$PATH \
mvn -o -q clean test
```

结果：

```text
test classes: 261
tests: 1056
failures: 0
errors: 0
skipped: 0
exit: 0
```

日志：`/tmp/zhuangdeqing-converge.JgGxh1/backend-full-final.log`

### App

命令：

```bash
npm run verify:mp-weixin
```

结果：`exit 0`

覆盖：

- 版本/客户来源/员工展示/客户端文案；
- onboarding、document、plan、home、action、selectors、contract-sign、media；
- 设计分项、财务 IA、财务选择器、薪酬闭环、注册、上传、客户管理；
- 导航与深链权限、Owner Preview、Owner UI V2、开发 API；
- 数字输入、收款冲正、退款金额、金额格式、合同变更明细；
- UI 统一化规范、TypeScript、小程序构建、分包体积和分包规范。

日志：`/tmp/zhuangdeqing-converge.JgGxh1/app-verify-final.log`

构建结果：

```text
主包 1026313 B
最大分包 724747 B
总包 3980883 B
主包 14 页，分包 17 个 / 173 页
```

### Web

命令：

```bash
npm run type-check
npm run build
npm run check:actions
npm run check:permissions
npm run check:states
npm run check:workspaces
npm run check:workbenches
npm run check:page-system
npm run check:forms
npm run check:concepts
npm run check:enums
npm run check:metrics
npm run check:store-scope
```

结果：`exit 0`

覆盖：

- TypeScript 与生产构建；
- 业务动作、权限/角色/数据范围、状态机；
- 工作台、对象工作空间、页面系统；
- 表单与选择器、业务概念与枚举；
- 指标注册与门店范围请求护栏。

日志：`/tmp/zhuangdeqing-converge.JgGxh1/web-verify.log`

## 本批次修复的阻断项

1. **后端 Context 全局启动失败**
   - `GoodsReceiptRepository` 派生查询使用不存在的 `purchaseOrder` 属性；
   - 改为实体真实属性 `purchase`，并同步 `ProjectFactTraceContext` 与测试；
   - 修复后 247 个连锁 Context 错误消失。

2. **支付并发断言依赖调度顺序**
   - 分布式锁允许后到请求返回同一笔付款，唯一索引也可能把输家转为 409；
   - 测试改为验证真实不变量：至少一个成功、所有成功返回同一 `paymentId`、数据库仅一笔 Payment/Ledger、应付只累计一次。

3. **App 冻结角色预览契约与 DEC-082 冲突**
   - `teamLeader` 不再要求 `view:owner-preview`，明确为不得获得任何应用能力。

4. **App 用户文案暴露研发术语**
   - 报价分组读取失败提示改为“当前报价明细暂不可确认，请稍后重新读取”。

5. **App 数值输入规范**
   - 施工计划标准总工期输入补齐 `jz-numeric-input`、占位样式和左对齐规则。

6. **App 生命周期直接调用原生模态**
   - 冻结角色退出改为先回登录页，再以统一 toast 告知原因。

## 当前边界

- 这是**开发侧三端收敛验证**，仍不是独立验收。
- 未执行真实登录态浏览器/小程序真机、真实 OSS、正式 MySQL、生产冒烟或发布。
- 当前工作区仍有大量未提交改动，不能把所有文件作为一个提交直接打包：

```text
backend: 437 entries
app:      85 entries
web:      52 entries
```

- `RQ-04` 高风险动作主体矩阵、`RQ-05` 外部主体身份边界仍未裁决。
- `FIN-WRITE-D2` 全量证据已绿，但仍待独立复验人复判；`COST-D1` 待独立验收。

## 下一门禁

1. 按业务域拆分提交批次，禁止一次性提交整个 437/85/52 工作区；
2. 提交前再次对同一提交快照执行三端验证；
3. 进入独立验收：D2 全量证据复判、COST-D1、U5-C；
4. 独立验收通过后再决定是否进入发布候选，不提前宣称可发布。

## 建议提交批次

当前只是计划，尚未执行 Git 提交：

1. 后端公共/身份基础：`common`、`tenant/user`、TeamLeader 冻结、门店范围与审计。
2. 企业运营标准资产：`catalog`、`pack`、`plan`、`standardasset`、单据/凭证模板。
3. 业务运营主链：`customer`、`property`、`design`、`quote`、`project`、`material`。
4. 财务运营：`finance`、`payroll`、`document`，以及对应迁移和金额/权限回归。
5. 统一对象视图：项目事实追踪、合同/报价统一视图及相关契约测试。
6. App：U5-C、财务、Owner Preview、报价/计划/施工页面及契约脚本。
7. Web：统一对象、财务、项目、权限/动作/状态和对应标准配置。
8. 产品档案与验收：`docs/product`、`docs/acceptance`、`docs/operations`、工作记录。

不同批次应保留独立回滚点，不能把 DDL、金额口径、权限和普通 UI 改动混在同一个提交里。
