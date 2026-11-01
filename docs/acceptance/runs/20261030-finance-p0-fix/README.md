# 财务 P0 跨端汇总与记账门禁修复 · 验证记录

## 状态

```text
代码修复：DEV_VERIFIED
真实页面：WEB + 微信开发者工具 PASS
历史账务数据：DATA_REPAIR_PENDING
独立验收：NOT_STARTED
发布：未发布
```

## 修复范围

- App 应收、收款、付款汇总改为复用 Web 同源部门报告 Provider。
- App 财务首页在汇总失败时显示“暂不可确认”，不再把未知显示为 0。
- Web 财务工作台失败态不再显示 0 金额或 0 笔数。
- 手工应收仅允许“其他应收”。
- 手工应付仅允许“其他应付/费用支出”。
- 后端 Save 对非法手工类型返回 400。
- Web 已收清应收不再显示“登记收款”。
- Web `/finance/salaries` 正确进入工人工资视图，不再显示手工新增入口。
- 自动台账按 `com_id + source + source_id` 复用同事实，来源冲突返回 409。
- 历史双记/缺记数据只形成修复计划，未执行。

## 自动化验证

| 检查 | 结果 |
| --- | --- |
| 后端全量 `mvn test` | PASS：1153 tests / 0 failures / 0 errors |
| 财务定向测试 | PASS：FinanceClosedLoop 14、StoreScope HTTP 22、Receipt 6、Payment 8、并发 2 |
| Web `npm run build` | PASS |
| App `type-check` | PASS |
| App 财务 IA / picker / 付款工人契约 | PASS |
| App `build:mp-weixin:release` | PASS；主包 1,078,914 B，总包 4,226,618 B |
| App `verify:mp-weixin` | BLOCKED：既有邀请流程契约“无员工身份不调用门店接口”，与本修复无关 |
| 非法应收类型 API | PASS：400「手工应收仅支持其他应收」 |
| 非法应付类型 API | PASS：400「手工应付仅支持其他应付或费用支出」 |
| 非法请求写入计数 | PASS：应收仍 73、应付仍 37 |

## 真实页面验证

### Web

- 财务工作台汇总失败：金额为 `—`，文案“金额与笔数暂不可确认”，无伪 0。
- 应收计划：73 笔、¥135,672.23、已收 ¥14,225.87、待收 ¥121,446.36、逾期 ¥17,969.40。
- 已收清应收行不显示“登记收款”。
- 新增其他应收：类型仅“其他应收”。
- 新增手工应付：类型仅“费用支出”“其他应付”。
- 工人工资路由：正确显示工人工资视图，无手工新增按钮。

### App

- 财务首页汇总失败：金额为 `-`，显示“财务汇总暂不可确认”。
  - 备注：该页截图拍摄于“禁伪 0”修复后、最后一次清理多余空态文案前；最终版本已通过 TypeCheck 与 Release build。
- 应收：13.57万 / 1.42万 / 12.14万 / 1.8万。
- 收款：1.35万 / 18 笔。
- 付款：¥9,500 / 19 笔。
- 新增其他应收：类型“其他应收”，提交按钮“创建其他应收”。
- 新增手工应付：类型“其他应付”，提示材料、工资、项目款须由来源闭环生成。

## 证据

- 测试汇总：`raw/test-summary.txt`
- Web/App 页面文本：`raw/notes/`
- Web/App 截图：`raw/screenshots/`
- 非法类型 API：`raw/api/receivable-illegal-first.json`、`raw/api/payable-illegal-material.json`
- 写入计数未变化：`raw/api/negative-write-counts.tsv`
- 历史数据修复计划：`../20261030-finance-cross-role-cross-surface-audit/data-repair-plan.md`

## 剩余门禁

1. 用户确认 `decoboon-test` 历史账务修复方案并完成备份。
2. 修复后 `coreBalanced=true`。
3. 发布前按 L3 走变更记录、制品门禁和生产冒烟。
4. 独立验收仍不能由本轮开发自测替代。
