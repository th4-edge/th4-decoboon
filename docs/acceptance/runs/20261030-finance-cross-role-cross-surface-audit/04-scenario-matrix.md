# 场景矩阵

| 场景 | 角色 | 端面 | 页面动作 | 页面结果 | API/DB 对账 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| FIN-S01 全局应收与门店下钻 | Boss / Finance | Web | 打开应收计划，切换全部/A/B/C | 全部 ¥135,672.23；A ¥37,478.75；B ¥44,974.50；C ¥52,470.25 | 与 DB 一致；三店合计比全公司少 1 笔 ¥748.73 空门店应收 | PASS_WITH_DATA_GAP |
| FIN-S02 财务应收汇总 | Finance | App | 打开应收计划 | 顶部应收/已收/待收/逾期均为 ¥0，列表有 73 条实记录 | Web/`finance/receivable/report` 返回 73 笔和正确金额 | FAIL |
| FIN-S03 财务收款汇总 | Finance | Web/App | 打开收款记录 | Web ¥13,477.14/18 笔；App ¥0/0 笔，列表同源存在 | DB 18 笔、¥13,477.14 | FAIL_CROSS_SURFACE |
| FIN-S04 财务付款汇总 | Finance | Web/App | 打开付款记录 | Web ¥9,500/19 笔；App ¥0/0 笔，列表同源存在 | DB 19 笔、¥9,500 | FAIL_CROSS_SURFACE |
| FIN-S05 应付汇总 | Boss / Finance | Web/App | 打开应付账款 | Web/App 均为总额 ¥47,000、已付 ¥9,000、待付 ¥38,000 | DB 一致 | PASS |
| FIN-S06 台账 | Boss / Finance | Web/App | 打开资金台账 | Web/App 73 笔、收入 ¥14,225.87、支出 ¥28,800、净现金流 ¥-14,574.13 | DB 与页面一致，但来源层不守恒 | FAIL_DATA_INTEGRITY |
| FIN-S07 店长本店范围 | StoreManager | Web/App | 打开台账；篡改 `storeId=B` | 仍为 A 总店 20 笔、净现金流 ¥-4,256.35 | 显式 API 跨店返回 403 | PASS_SCOPE |
| FIN-S08 店长应收直接入口 | StoreManager | Web/App | 直接打开应收计划 | 可读 A 总店 20 笔，页面提供服务端范围内的操作 | 菜单未显式展示该入口，但业务允许本店应收 | GAP_NAVIGATION |
| FIN-S09 手工应收类型 | Finance | Web | 打开“新增应收节点” | 可选首期款/进度款/尾款/质保金/其他应收 | `BR-055` 仅允许手工“其他应收” | FAIL_BUSINESS_RULE |
| FIN-S10 手工应付类型 | Finance | Web | 打开“新增应付成本”并展开类型 | 可选材料款/工人工资/项目款/供应商款/费用支出/其他应付 | `BR-055` 仅允许其他应付、费用支出 | FAIL_BUSINESS_RULE |
| FIN-S11 已收清节点再收款 | Finance | Web | 点击已收清节点“登记收款”并输入 ¥1.00 | 表单打开，确认按钮可点；前端提交校验限制不超过待收 | App 对 `outstanding=0` 不显示“登记收款” | FAIL_CROSS_SURFACE / P2 |
| FIN-S12 项目账本 | Boss / Finance | Web/App | 打开项目账本 | 均加载失败；Web 显示“暂不可确认”，App 同时显示 0 金额 | 隔离库为旧 schema，接口 `undefinedErrorCode` | BLOCKED_ENVIRONMENT |
| FIN-S13 费用/凭证 | Boss / Finance / StoreManager | Web/App | 打开费用、凭证页面 | 均显示泛化错误；同时出现 0 单/0 张 | 隔离库缺当前表/列 | BLOCKED_ENVIRONMENT + FAIL_ERROR_STATE |
| FIN-S14 成本完整性 | Boss / Finance | DB | 检查 18 个项目结算 | 18 个结算利润/毛利率均为 `null`，未发现成本未完整却展示利润 | DB 与 `MET-002` 缺失值规则一致 | PASS_DATA |
