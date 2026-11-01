# Provider 响应契约修复与 Web/小程序主链回归报告

> 执行日期：2026-10-31
> 环境：本地 `decoboon`，后端 `127.0.0.1:8090`，Web `127.0.0.1:8080`，微信开发者工具 `dist/dev/mp-weixin`
> 测试企业：昭青科技 · 业务闭环审计（本地合成数据）
> 状态：`DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE`

## 1. 目标

1. 按 `scripts/audit-provider-response-contract.py` 门禁检查全部 Provider 响应；
2. 结合 App/Web 实际消费字段补齐 `@Properties` 和嵌套白名单；
3. 修复非法 JSON、懒加载关联泄漏和响应体异常；
4. 在真实 Web 和微信小程序页面完成主链操作、接口调用和事实回读。

## 2. 后端结论

| 检查 | 结果 |
| --- | --- |
| Provider 总数 | 542 |
| 声明 `@Properties` | 537 |
| 声明非空 `allow` | 535 |
| 完全未声明且为标量/文件 | 5 |
| 仅声明空 `@Properties` 的标量 | 2 |
| 静态审计 finding | 0 |
| 后端全量测试 | 1170 tests / 0 failures / 0 errors / 0 skipped |
| 代表接口 HTTP JSON 冒烟 | 17 / 17 success，0 敏感字段 |

修复内容：

1. `statistic/boss/workbench` 嵌套 `Project.customer.company` 缺少白名单，曾返回非法 JSON，小程序首页经营指标显示“—”。补齐 Customer、Company 等摘要白名单后，首页恢复真实签约额、回款额和在建项目。
2. 16 个返回读模型或实体的 Provider 补齐 `@Properties`；5 个布尔/文件响应按规范作为标量/二进制豁免。
3. 补齐 69 个前端的读模型、业务状态视图、合同履约档案、角色报告、领料/退料、企业成员等响应路径的递归白名单。
4. `statistic/success/report` 在 `Company.createTime` 存在但 `toLocalDate()` 为空时抛异常；改为回退当天，并新增回归测试。
5. 更新 App/Web Provider 路由基线：`finance/payment/report`、`finance/receipt/report`、`finance/receivable/report` 由 Web-only 读路由校正为 App/Web 共享。

## 3. Web 真实页面

| 页面/动作 | 结果 |
| --- | --- |
| 工作台、客户列表、客户 360 | PASS |
| 写客户跟进并回读 | PASS，无 console error / 网络失败 |
| 待我谈单、报价工作区 | PASS |
| 主材选材看板、项目选材工作区 | PASS |
| 整改跟进、项目工地、项目合同档案 | PASS |
| 财务工作台、应收、收款、应付、付款、资金台账 | PASS |
| 项目账本、项目核算 | PASS |
| 1440×900 截图与页面/接口证据 | 已保存 |

Web 共 18 个页面/动作，控制台 error=0，失败请求=0，页面未出现 `NaN/undefined/null` 伪事实。

## 4. 微信小程序真实页面

| 场景 | 结果 |
| --- | --- |
| Boss 首页经营指标 | PASS，签约额/回款额/在建项目恢复真实值 |
| 客户列表、客户详情 | PASS |
| 客户跟进写入并详情回读 | PASS |
| 项目列表、项目详情 | PASS |
| 选材工作台、整改看板、项目旅程 | PASS |
| 报价列表、报价详情 | PASS |
| 财务首页、应收、应付、台账、项目核算 | PASS |
| 我的页面、首页二次回读 | PASS |
| 使用进度/用户成功报告 | PASS |

小程序使用 `miniprogram-automator` 在微信开发者工具真实运行。首次脚本的 toast 文本等待项超时，但页面已显示新跟进并写入成功；随后用独立写后回读场景验证 `W01-W04` 全部 PASS，因此该超时判定为测试脚本等待方式问题，不是产品失败。

## 5. 关键接口响应定义

- `statistic/boss/workbench`：`asOf/month/indicators/attentionProjects/businessEvents/alertReport`；嵌套 `Project/Customer/Company/Employee/Property` 均为摘要白名单。
- `statistic/success/report`：企业激活分、使用天数、风险等级、里程碑、岗位激活和下一步；公司创建日期缺失时回退当天，不阻断。
- `material/selection/item/page`：合同主材行、选材结果、附件、下单状态和下一步。
- `material/selection/board`：项目/房屋/客户/设计师摘要、选材进度、下单进度和下一步。
- `material/plan/workbench`：项目级材料计划汇总与逐项计划行。
- `project/acceptance/rectify/page`：整改阶段、轮次、问题、责任人、截止日期、逾期和下一步。
- `order/prepay`：微信 `requestPayment` 所需的 `appId/timeStamp/nonceStr/package/signType/paySign`。
- `platform/auth/logout`：仅 `userId/name/role/status/permissions`。
- `order/callback`：仅 `orderNo/status/amount/paidTime`，不返回支付订单完整关联图。

全部字段定义以 Provider 实现类上的 `@Properties(allow=...)` 和静态审计结果为准。

## 6. 剩余边界

- 本轮是开发自测与真实页面主链回归，不是独立验收。
- 未执行生产发布、正式小程序提审或发布。
- Web-only 房屋删除仍处于 `WEB_FIRST_PENDING_REGISTRATION`，属于既有业务语义债务，不是本轮响应序列化问题。
- 全产品 258 个 Web 路由和 437 个 App 路由未逐页穷举；本轮覆盖签约后工地和财务主链，静态门禁覆盖全部 Provider。

## 7. 证据索引

- `provider-response-audit.json`
- `provider-response-self-test.txt`
- `backend-full-test.json`
- `api-contract-smoke-final.json`
- `api-boss-workbench.json`、`api-success-report.json`
- `web/batch1.json`、`web/batch2.json`、`web/customer-follow-write.json`
- `web/screenshots/`
- `app/provider-response-app-e2e/`
- `app/provider-response-app-continuation/`
- `app/provider-response-app-write-readback/`
- `app/provider-response-app-success/`
