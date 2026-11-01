# RUN-20261101-CRM-FIN-OWNER-001 验收报告

## 运行信息

- 用例：`TC-CRM-FINANCE-OWNER-006`
- Plan：`TP-20261101-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1`
- 后端：`7b7dc5f0ed3795fbd6d13640f5dfb7f51b8c3dd8`
- Web：`e40ca8baf6203d71f5d0375cc043d424a0aa5873`
- App：`f2fb91e0785bf953940257ce97e3ea84fc2ae808`
- 环境：本地昭青科技，客户 `242541216321765376`

## 结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| --- | --- | --- | --- | --- |
| 1 | 0 | 0 | 1 | 0 |

结论：`BLOCKED`。Web/API/数据范围通过，App点击证据受自动化环境阻断。

## 已验证

### Web真实页面

- 财务打开客户详情，点击“客户负责人”；
- 页面无403，负责人弹层正常打开；
- 下拉显示9名候选人；
- 选择现任负责人“审计业务”并提交，页面提示“已转交客户负责人”；
- DB客户负责人仍为 `199500000000000106 / 审计业务`，没有业务归属副作用。

### 角色与数据范围

- 总部财务：`user/employee/page` 200，9名企业候选；
- 店长：200，8名本店候选；
- 业务员：仍403，不因本次修复扩大权限；
- 财务主更新Provider与候选Provider权限已一致。

### 测试资产门禁

- 新增 `TC-CRM-FINANCE-OWNER-006`；
- `USER-DEC-20261029-009` 已进入 Oracle；
- 新增 `role-action-provider-matrix.json`，登记角色、动作、主Provider和依赖Provider；
- `test_assets.py validate` 通过：43条用例、1个动作矩阵、72条规则；
- `self-test` 负向验证：动作依赖Provider写错、已批准决策未映射均会被识别。

## 未完成

- App客户详情点击负责人弹层：微信开发者工具返回内部错误  
  `Cannot destructure property 'rawPath' of 't.getPageMetaByWebviewId(...)' as it is null`；
- 因此未取得App点击截图和候选列表，不能声明App UI PASS；
- App类型检查、小程序构建、客户管理契约已通过，dev构建已更新，后端候选Provider也已实测修复。

## 残余风险

- App实际点击仍需手工或真机复验；
- 本轮选择现任负责人提交，未更换到其他负责人，避免影响现有客户/项目主链；
- 真实换人应使用独立测试客户验证负责人、门店和团队归属同步变化。

## 下一步

使用微信开发者工具或真机，以财务账号进入客户详情，点击“客户负责人”，确认弹层出现候选人且无权限错误。通过后本用例可从BLOCKED转复验PASS。
