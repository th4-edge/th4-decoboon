# RUN-20261101-CORE-B1-WRITE-001 受控写入验收报告

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| ---: | ---: | ---: | ---: | ---: |
| 3 | 2 | 0 | 1 | 0 |

## 用例结论

- `TC-CT-BOSS-CONTRACT-001` PASS：Boss 通过 Web 完成报价→发起合同→确认签约→确认生效；合同 `Effective`，项目、8阶段、1材料计划、3应收同源生成。
- `TC-CT-ORDER-CONTRACT-003` PASS：PendingSign 合同直接 Effective 返回 400“合同签约后才能生效”，状态和项目事实不变。
- `TC-DQ-PERMISSION-QUOTE-005` BLOCKED：测试 Oracle 与 `DEC-071` 冲突。实际验证为 Marketer 置 Accepted 返回 403，Designer 置 Accepted 返回 200 并记录 acceptedBy；`DEC-071` 明确允许设计师登记报价接受，禁止 Marketer。旧用例“Designer不可接受”必须修正后才能复验。

## 数据

- 新客户：王芳（造数5100）
- 报价：`242541219522019328`
- 合同：`242541468600762368`
- 项目：`242541620656865280`
- 合同金额：`¥2,495.75`
- 合同状态：`Effective`
- 报价状态：`Converted`
- 项目阶段：8
- 材料计划：1
- 应收：3

## 结论

`BLOCKED`

两项合同状态机用例通过，但报价权限用例因测试资产过期不能形成最终通过结论。实际产品行为符合 `DEC-071`，本轮不将其登记为产品缺陷。
