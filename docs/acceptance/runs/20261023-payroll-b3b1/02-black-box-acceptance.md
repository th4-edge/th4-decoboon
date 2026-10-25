# B3B1 薪酬复核与员工发放权限闭环 · 黑盒验收记录

## 1. 环境

- 后端：Java 22，本地 `127.0.0.1:8090`
- Web：Vite `127.0.0.1:8085`
- App：uni-app H5 `127.0.0.1:8081`，API 经 H5 同源代理
- 数据：独立 MySQL schema `decoboon_b3b1_20261023`
- 角色：BOSS、总部财务、店长
- 生产：未访问
- DDL：无

## 2. 结论

`BLACK_BOX_PASS_PENDING_USER_ACCEPTANCE`

- API：`14 PASS / 0 FAIL`
- Web：Finance 提交复核、BOSS 退回/确认、店长无发放入口 PASS
- App H5：Finance 提交复核、BOSS 退回、店长无发放入口 PASS
- Console error：Web/App 运行态均为 `0`
- 隔离 JDK22 全量：`938 / 0 / 0 / 0`

## 3. API 结果

| 用例 | 结果 |
| --- | --- |
| 确认后工资单保留退回原因 | `remark=退回原因：考勤数据需要修正`，PASS |
| 重复确认 | 应付/工资条保持 `5/5`，PASS |
| Finance 确认/删除 | 403 / 403，PASS |
| 店长确认/删除 | 403 / 403，PASS |
| 店长提交复核 | 403，PASS |
| 店长员工应付分页 | 403，PASS |
| 店长员工发放 | 403，PASS |
| Finance 提交 `Draft → Pending` | 成功，PASS |
| BOSS 无原因退回 | 400「退回原因不能为空」，PASS |
| BOSS 有原因退回 | `Pending → Draft`，原因回读，PASS |
| BOSS 删除退回草稿 | 成功，删除后详情 404，PASS |

证据：`api-results.json`、`http/`、`sql/`、`audit-lines.txt`。

## 4. Web 结果

- Finance：Draft 显示“提交复核”和“员工发放”，不显示确认/删除；提交后状态变为“待确认”。
- BOSS：Pending 显示“退回草稿”和“确认工资单”；退回原因必填，保存后详情显示“退回原因：考勤数据需要修正”；可从退回后的 Draft 直接确认。
- StoreManager：不显示员工发放、提交复核、确认工资单；本店工资详情仍可读。
- Console error：0。

截图：

- `screenshots/web-finance-submitted.png`
- `screenshots/web-boss-returned.png`
- `screenshots/web-boss-confirmed.png`
- `screenshots/web-manager-no-pay.png`

## 5. App H5 结果

- Finance：Draft 工资单显示“提交复核”，二次确认后状态变为“待确认”。
- BOSS：Pending 显示“退回草稿”和“确认工资单”；输入原因后成功退回，详情显示退回原因。
- StoreManager：薪酬工作台不出现“员工发放”；薪酬页面仍按只读范围展示。
- Console error：0。

截图：

- `screenshots/app-finance-draft.png`
- `screenshots/app-finance-submitted.png`
- `screenshots/app-boss-pending.png`
- `screenshots/app-boss-returned.png`
- `screenshots/app-manager-no-pay.png`

## 6. 审计与自动化

- `SUBMIT_REVIEW`、`RETURN_TO_DRAFT`、`CONFIRM` 均输出结构化日志，包含操作人、企业、门店、工资单、周期、前后状态和原因。
- 定向测试 `9/0/0/0`。
- 隔离 JDK22 全量 `938/0/0/0`。
- Web type-check/build/权限/门店范围/工作台/客户端文案 PASS。
- App type-check、薪酬闭环契约、微信小程序构建 PASS。

## 7. 边界

- 本次为同一执行会话开发后的黑盒验证，不冒充独立验收。
- 未发布生产，未执行 DDL。
- 工资单作废、反向应付/成本、版本/唯一约束归 B3B2。
- 批量/部分发放、提成释放 UI、人工调整归 B4，仍未裁决。
