# AI 用量与经营参谋联合独立验收报告

> 请求编号：`REQ-20260926-AI-USAGE-BRIEF-ACCEPTANCE`  
> 验收日期：2026-09-26  
> 验收 lane：acceptance  
> 后端基线：`5252afa`  
> Web 基线：`68e6bff`  
> 最终状态：**CONDITIONAL**

## 0. 结论

A0.1 AI 调用与影子用量底账、A1 AI 经营参谋 P0 基线版已通过独立复验：

- 企业总量 = 门店分摊 + 企业公共；
- 业务门店优先，企业公共不归默认总店；
- 失败、取消和重复调用不形成第二次有效影子点数；
- 三个 AI Provider 仅 BOSS 可读，非 BOSS 被拒绝；
- A1 的四项前序缺陷已全部关闭；
- 迁移 64 校验和匹配，在隔离 MySQL 8.0.24 连续执行两次成功；
- 后端全量 115 suites / 385 tests / 0 failures / 0 errors / 0 skipped；
- Web `/ai/center` 已改为手动生成，增加预计消耗说明，并在来源抽屉展示对象级来源；
- Web type-check、verify:system、build 均通过。

本轮没有发现新的可复现生产功能缺陷。仍有以下外部/环境边界：

- 同幂等键的并发写入冲突回查未执行；
- 尚无当前可用的同版本后端、真实 BOSS token 和企业数据，未完成真实浏览器加载/筛选/分页；
- 生产迁移账本已由运维证据登记 SUCCESS，但当前沙箱不能直连生产库再次查询；
- A1 当前为 `BaselineAiBriefGenerator`，未接真实大模型，真实模型效果不在本轮范围。

因此最终状态为 **CONDITIONAL**，不是失败；上述 BLOCKED 项完成后方可升级为 PASS。

## 1. 验收输入

| 项目 | 内容 |
| --- | --- |
| 后端 | `feature/ai-integration`，`5252afa` |
| Web | `feature/ai-web-remote`，`68e6bff` |
| 迁移 | `docs/design/migrations/64-AI调用与影子用量底账.sql` |
| 迁移证据 | `docs/operations/data-changes/2026-09-26-AI调用与影子用量底账迁移64.md` |
| A0.1 Provider | `ai/usage/report`、`ai/usage/page` |
| A1 Provider | `ai/brief/report` |
| A1 生成器 | `BaselineAiBriefGenerator`，不使用真实大模型 |

## 2. 执行命令与结果

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home/bin:$PATH \
mvn -o -q -DforkCount=0 test

npm run type-check
npm run verify:system
npm run build

# 迁移 64 隔离 MySQL 8.0.24：空结构连续执行两次
mysql < docs/design/migrations/64-AI调用与影子用量底账.sql
mysql < docs/design/migrations/64-AI调用与影子用量底账.sql
```

| 项目 | 结果 |
| --- | --- |
| `AiUsageBriefJointAcceptanceTest` | 9/9 PASS |
| Provider Security | 3/3 PASS；348 routes / 12 public / 336 protected |
| 后端全量 | 115 suites / 385 tests / 0 failures / 0 errors / 0 skipped |
| Web type-check | PASS |
| Web verify:system | PASS |
| Web build | PASS |
| 迁移 64 SHA-256 | `c99dba4b19632b756eaa3d7e81267ed0626d204fc5678761565eaa7f2c267b75` |
| 迁移 64 重复执行 | PASS；表 1、唯一索引 1、核心列 4、数据 0 |
| 真实浏览器/生产库直连 | BLOCKED |
| 真实大模型 | NOT_APPLICABLE_THIS_ROUND |

## 3. 验收矩阵

### 3.1 A0.1 用量与门店归属

| 编号 | 验收项 | 结果 | 证据 |
| --- | --- | --- | --- |
| U-01 | 企业总量 = 门店分摊 + 企业公共 | PASS | 独立测试资源总量 200；门店汇总合计 200 |
| U-02 | 业务门店优先 | PASS | 独立与既有 Context 测试 |
| U-03 | 无业务门店时使用执行门店 | PASS | `AiUsageContextTest` |
| U-04 | 企业公共不归默认总店 | PASS | 显式 EnterprisePublic，allocationStore 为空 |
| U-05 | 失败不重复计有效影子点 | PASS | Failed 不计有效点 |
| U-06 | 取消不重复计有效影子点 | PASS | Cancelled 不计有效点 |
| U-07 | 同幂等键顺序重复只留一条 | PASS | 独立与既有幂等测试 |
| U-08 | 明细不泄露企业对象、幂等键、请求 ID、平台成本 | PASS | Serialization whitelist |
| U-09 | 迁移 64 可重复执行 | PASS | 隔离 MySQL 8.0.24 双跑 |
| U-10 | 并发同幂等键冲突回查 | BLOCKED | 无并发 MySQL 自动测试 |

### 3.2 A1 经营参谋后端

| 编号 | 验收项 | 结果 | 证据 |
| --- | --- | --- | --- |
| B-01 | BOSS 可生成/查看简报 | PASS | Provider 权限和上下文测试 |
| B-02 | 业务员/店长/团队负责人/PM/财务/业主负向 | PASS（代码路径） | 三 Provider 均 `requireBoss`；非 BOSS Context 测试拒绝 |
| B-03 | 业务/交付/资金事实 | PASS | `AiBossBriefContextTest` |
| B-04 | 部分数据源失败保留已有事实并提示暂不可确认 | PASS | 0f78366 修复后验收测试 |
| B-05 | 成本不完整不显示伪毛利 | PASS | 成本状态和 Web 无毛利渲染 |
| B-06 | 财务汇总缺失时成本不可确认 | PASS | `costStatusMustNotTreatMissingFinanceAsComplete` |
| B-07 | 同一简报动作稳定幂等键 | PASS | `repeatedBossBriefActionMustUseStableIdempotencyKey` |
| B-08 | 行动建议类别去重 | PASS | `actionSuggestionsMustTakeAtMostOnePerRiskCategory` |
| B-09 | 对象级来源记录 | PASS | 5252afa sources + Web 抽屉 |
| B-10 | 真实大模型效果 | NOT_APPLICABLE | 基线版明确为规则生成器 |

### 3.3 Web AI Center

| 编号 | 验收项 | 结果 | 证据 |
| --- | --- | --- | --- |
| W-01 | `/ai/center` 路由仅 BOSS 可见 | PASS | router/navigation/permission |
| W-02 | 日期、门店、能力筛选 | PASS | `AiCenterView.vue` |
| W-03 | 调用明细分页与总数映射 | PASS | API current/size/total 映射和页面分页 |
| W-04 | 不展示平台成本、提示词、客户正文 | PASS | model/view/API 静态检查 |
| W-05 | 成本不完整提示 | PASS | `el-alert` |
| W-06 | 生成失败保留页面并可重试 | PASS | 不覆盖已有 brief，按钮保留 |
| W-07 | 仅 BOSS 手动生成 | PASS | 68e6bff 移除 `onMounted(loadBrief)` |
| W-08 | 生成前预计消耗说明 | PASS | 空态显示最低 1 影子点说明 |
| W-09 | 来源抽屉展示 Provider/对象/版本/更新时间 | PASS | 68e6bff 对象级来源 |
| W-10 | 真实浏览器加载、筛选、翻页 | BLOCKED | 无当前可用同版本后端和 BOSS 会话 |

## 4. 前序缺陷关闭复核

| 缺陷 | 结论 | 证据 |
| --- | --- | --- |
| 行动建议同类重复 | CLOSED | `0f78366` + 验收测试 |
| 数据源失败整份简报失败 | CLOSED | `0f78366` + 验收测试 |
| 随机幂等键导致重复计点 | CLOSED | 稳定简报 fingerprint + 测试 |
| 财务缺失误判成本完整 | CLOSED | `0f78366` + 验收测试 |
| Web 打开即自动生成 | CLOSED | `68e6bff` 无自动 `loadBrief` |
| 无预计消耗说明 | CLOSED | `68e6bff` 空态文案 |
| 来源抽屉缺对象级来源 | CLOSED | `5252afa` + `68e6bff` |

## 5. BLOCKED / 剩余风险

- 同幂等键并发：当前唯一索引可防重复数据，但没有验证冲突后是否稳定返回既有记录。
- 真实浏览器：现有工作区没有运行中的同版本后端、Web 页面和真实 BOSS token。
- 生产迁移账本：证据文档登记 SUCCESS，当前沙箱无法直连生产 MySQL。
- 门店级简报：`storeId` 目前只影响 dashboard 查询，财务/预警仍为公司汇总；当前 Web 未暴露简报门店筛选，因此未判失败，但后续开放前需冻结口径。
- 真实模型效果：当前规则生成器，不能据此评价真实 LLM 摘要质量或成本准确性。

## 6. 输出与证据

- 报告：`docs/acceptance/AI用量与经营参谋联合独立验收报告.md`
- 独立验收测试：`src/test/java/com/th4/edge/decoboon/ai/AiUsageBriefJointAcceptanceTest.java`
- Provider 安全测试：`src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java`
- 后端全量日志：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/backend-full-final-5252afa.log`
- 验收测试 XML：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/joint-acceptance-5252afa.xml`
- Web 最终门禁日志：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/web-final.log`
- Web 静态审计：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/web-static-audit-final.json`
- 迁移 64 校验和：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/migration64-sha256.txt`
- 迁移 64 结构检查：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/migration64-inspect.txt`
- 汇总：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/summary.json`

## 7. Next Action

1. 在真实 BOSS 会话 + 同版本后端上完成 `/ai/center` 的加载、手动生成、筛选、分页、成本不完整和来源抽屉验证。
2. 补并发同幂等键的数据库级测试，确认冲突后返回已有记录。
3. 直接复核生产迁移账本 64 和表结构。
4. 接入真实大模型后另立 A1 模型效果/成本验收，不沿用本报告结论。
