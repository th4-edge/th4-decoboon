# H1 员工薪酬 P0 事故收口验收执行包

## 状态

- 开发状态：`DEV_VERIFIED`
- 黑盒验证：`PASS`（14 API + 三角色 Web）
- 独立签署：`PENDING`
- 生产发布：未执行
- DDL：无

## 范围

1. 工资单确认与提成解耦；
2. Web 薪酬按钮能力键；
3. 考勤详情门店数据范围。

## 已取得证据

- 生产 release `20261022-131500` 的 `PayrollSettlementContext.class` 经 `javap` 核对，无 `commissionComplete` 阻断；
- 定向测试：7/0/0/0；
- H1 隔离 JDK 22 全量：931/0/0/0；
- 共享工作区 JDK 26 全量：1072/0/0/0；
- 共享工作区 JDK 22 全量：1072 tests，1 个并行费用专项失败（非 H1）；
- Web type-check、build、权限、门店范围、工作台门禁 PASS；
- 代码证据与差异见 `docs/acceptance/20261023-payroll-cross-surface-audit/README.md`。

## 独立验收最低用例

### H1-01

- 提成 `Accrued/Released/InPayment` 时，BOSS 可确认工资单；
- Finance 确认仍被拒绝；
- 考勤不完整、无工资项、实发为负仍阻断；
- 重复确认不重复生成应付、成本或工资条。

### H1-02

- BOSS：可用薪酬方案、工资单生成、最终确认；
- Finance：可用薪酬方案和生成，不显示最终确认；
- StoreManager：不可生成工资单、不可最终确认、不可计提提成；本店考勤入口可用；
- 员工发放按钮不纳入 H1 结论，按 B3B 权限裁决继续跟踪；
- Web/App 能力键与页面结果一致。

### H1-03

- 门店财务/店长读取本店 `attendance/summary/info` 成功；
- 读取他店已知 `summaryId` 返回 404；
- 读取跨企业已知 `summaryId` 返回 404；
- 错误响应不泄露记录是否存在；
- 列表、详情、编辑、删除使用同一数据范围。

## 证据要求

- 真实登录态账号、角色、门店和对象 ID；
- HTTP 请求/响应、页面截图、DOM/控制台错误；
- 代码提交 SHA 与运行制品 SHA；
- 若仅完成本地测试，不得标记 `ACCEPTED` 或 `CLOSED`。


## 实际执行结果（2026-10-23）

- API：14/14 PASS。
- Web：BOSS/Finance/StoreManager 三角色真实登录态 PASS，console error=0。
- 详细记录：`02-black-box-acceptance.md`
- 接口证据：`api-results.json`、`http/`
- 截图：`screenshots/`
- 环境：`environment.txt`
- 结论：`BLACK_BOX_PASS_PENDING_INDEPENDENT_SIGNOFF`。
