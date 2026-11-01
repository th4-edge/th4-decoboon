# 员工薪酬与薪资经营 · B1 独立验收

## 1. 验收结论

- Domain：`payroll-labor`
- Batch：B1 财务 Web 薪酬方案维护闭环
- 验收性质：独立黑盒验收；未修改实现代码、未部署、未提交、未访问生产写入
- 验收日期：2026-10-21
- 环境：本地后端 `127.0.0.1:8090`、本地 Web `127.0.0.1:8080`、隔离 MySQL、微信开发者工具本地模拟器
- **Verdict：PASS**
- B1 高风险项全部关闭；可进入 `RELEASED` 发布门禁，但本次未执行实际部署或发布。

## 2. 需求逐项结果

| REQ | 结果 | 独立证据与说明 |
| --- | --- | --- |
| REQ-B1-01 列表与筛选 | PASS | Finance 列表真实加载 5 条；姓名、状态、生效月份和本店筛选符合预期；空态、断网错误态和恢复均验证。 |
| REQ-B1-02 新增方案 | PASS | Web 与 App 新增均成功；App 创建徐勇 `5432.12 / 23 天 / 2026-10-21`，列表精确显示 `¥5,432.12`，API 回读 1 条且 `locked=false`；缺 `scheduledDays` 返回 400 且未落库。 |
| REQ-B1-03 编辑未锁方案 | PASS | Web 与 App 均可编辑未锁方案；App 将应出勤改为 24 天，金额保持 `5432.12`，列表继续精确显示 `¥5,432.12`，API 与 `locked=false` 一致。 |
| REQ-B1-04 新增后续方案 | PASS | 锁定行只显示“新增后续方案”；Web 与 App 均进入后续方案表单，员工只读并预填原工资/应出勤。 |
| REQ-B1-05 终止/删除 | PASS | Web 终止和删除通过；App 真实点击删除、出现二次确认，确认后列表由 6 条恢复为 5 条，API 徐勇方案数为 0。 |
| REQ-B1-06 数据范围与权限 | PASS | StoreManager 页面无写按钮且直接写请求返回 403；Finance 非法 storeId、跨企业门店/员工请求被拒绝；BOSS 可写。 |
| REQ-B1-07 字段完整与中文展示 | PASS | 字段、岗位、门店、团队、锁定状态正确；App 薪酬金额固定两位小数，API `5432.12` 与列表 `¥5,432.12` 完全一致。 |
| REQ-B1-08 输入校验 | PASS | 工资 0、应出勤 32、倒置日期均返回 400；缺 `scheduledDays` 返回约定 400；错误输入均未落库。 |
| REQ-B1-09 唯一版本 | PASS | 同员工同生效日期返回 409；定向与全量测试覆盖唯一版本校验。 |
| REQ-B1-10 审计与恢复 | PASS | CREATE/UPDATE/DELETE 日志包含操作人、企业、门店、方案、员工与前后快照。 |
| REQ-B1-11 App 回归 | PASS | 微信开发者工具真实完成 App 新增、编辑、删除、锁定态、后续方案、两位小数金额和 API 回读；console error 为 0。 |

## 3. 自动化验证

- 后端定向复验：`12 / 0 / 0`，BUILD SUCCESS。
- 后端全量（本批次前序独立验收）：`900 / 0 / 0`。
- Web：type-check、build、权限、门店范围、工作台检查 PASS。
- App：type-check、薪酬闭环契约、mp-weixin build PASS。
- App 运行时使用本地 API `http://127.0.0.1:8090/api/`，未访问生产 API。

## 4. App 金额精度与 CRUD 证据

| 操作 | 实际结果 |
| --- | --- |
| 新增前 | 徐勇方案数 `0`，列表 5 条。 |
| 新增 | 真实选择徐勇，录入 `5432.12 / 23 天`；列表 6 条，精确显示 `¥5,432.12`，API `baseSalary=5432.12`、`locked=false`。 |
| 编辑 | 初值回填 `5432.12 / 23`；应出勤改为 24 天并保存；列表继续显示 `¥5,432.12 / 24 天`，API 数值一致且未锁定。 |
| 删除 | 真实点击“删除方案”，二次确认后列表恢复 5 条，徐勇卡片消失，API 方案数 `0`。 |
| 运行错误 | App console error `0`。 |

## 5. 缺陷状态

| ID | 级别 | 状态 | 说明 |
| --- | --- | --- | --- |
| ACC-B1-001 | P3 | RESOLVED | 缺 `scheduledDays` 返回 code=400、指定文案且未落库。 |
| ACC-B1-002 | P3 | RESOLVED | 无效保存提示在修改工资后消失。 |
| ACC-B1-U01 | P2 | CLOSED | App 新增/编辑/删除已真实运行时走查完成。 |
| ACC-B1-003 | P2 | RESOLVED | App 列表金额固定两位小数，`5432.12` 显示为 `¥5,432.12`，与 API 一致。 |

当前无开放失败项或未覆盖高风险项。既有 Vue Router 首路由 warning 为非阻塞壳层观察。

## 6. 清理与数据恢复

- 三个临时口令账号的密码哈希已按备份恢复，最终核对 `3/3` 一致。
- 徐勇 `emp_id=1790224826490830054`：薪酬方案 `0` 条。
- 吴濛濛 `emp_id=1790221992067828768`：恢复原始 `1` 条。
- 当前薪酬方案总数 `5`，本次临时方案残留 `0`。
- 微信开发者工具项目与临时构建目录已关闭/删除；未部署、未提交、未修改实现代码。

## 7. 证据

- 验收目录：`docs/acceptance/runs/20261021-payroll-b1-independent/`
- 截图目录：`output/playwright/payroll-b1-independent/`
- ACC-B1-003 端到端结果：`acc003-app-precision-e2e.json`
- App 截图：`acc003-app-01-create-filled.png` ～ `acc003-app-06-deleted-list.png`
- 构建/契约：`acc003-app-validation.log`
- 清理结果：`acc003-cleanup.txt`
- 哈希清单：`evidence-sha256.txt`、`screenshot-sha256.txt`

## 8. 发布判断

- 当前阶段：`PASS`
- 是否可进入 `RELEASED` 门禁：**是**
- 实际部署/发布：本次未执行


## H1 黑盒验证（2026-10-23）

- API：14 PASS / 0 FAIL；覆盖提成解耦、财务拒绝、异常前置、重复确认幂等和跨店/跨企业考勤详情。
- Web：BOSS/Finance/StoreManager 三角色按钮矩阵 PASS。
- 证据：`docs/acceptance/runs/20261023-payroll-h1/02-black-box-acceptance.md`。
- 结论：`BLACK_BOX_PASS_PENDING_INDEPENDENT_SIGNOFF`；不冒充独立验收。


## B2 黑盒验证（2026-10-23）

- API：10 PASS / 0 FAIL。
- Web：27 天联动、编辑回填、完整字段、无方案提示、历史月份上下文 PASS。
- 店长本店新增/编辑/删除 PASS；跨店新增/更新/删除 404；已生成工资项考勤更新 409。
- 隔离 JDK 22 全量：931/0/0/0。
- 证据：`docs/acceptance/runs/20261023-payroll-b2/`。
- 结论：`BLACK_BOX_PASS_PENDING_USER_ACCEPTANCE`；不冒充独立验收。
- 用户验收：2026-10-23 用户指令“下一步”承接 B3A，记为 `USER_ACCEPTED_NON_INDEPENDENT`。
- 用户验收：2026-10-23 用户指令“下一步”承接 B2 结果，记为 `USER_ACCEPTED_NON_INDEPENDENT`。


## B3A 黑盒验证（2026-10-23）

- API：20 PASS / 0 FAIL。
- BOSS：详情、完整工资项、确认二次确认、确认后回读、Draft 删除 PASS。
- Finance：详情可读；最终确认与删除 403；生成草稿成功。
- StoreManager：本店详情可读；确认、删除、生成 403；他店已知 `sheetId` 详情 404。
- 总部 Finance：企业范围读取他店工资单成功。
- 重复确认后应付与工资条保持 `5/5`，无重复财务事实。
- 表头应发/扣款/实发与工资项汇总三组一致。
- 隔离 JDK22 全量：932/0/0/0。
- 证据：`docs/acceptance/runs/20261023-payroll-b3a/`。
- 结论：`BLACK_BOX_PASS_PENDING_USER_ACCEPTANCE`；不冒充独立验收。


## B3B1 黑盒验证（2026-10-23）

- API：14 PASS / 0 FAIL；覆盖复核提交、Pending 展示、无原因退回、原因回读、BOSS 直接确认、重复确认幂等、店长发放 403 与草稿删除回归。
- Web：Finance/BOSS/StoreManager 三角色运行态 PASS，console error 0。
- App H5：Finance 提交复核、BOSS 退回、店长无员工发放入口 PASS，console error 0。
- 结构化审计：`SUBMIT_REVIEW`、`RETURN_TO_DRAFT`、`CONFIRM` 均记录操作人、范围、状态和原因。
- 隔离 JDK22 全量：938/0/0/0。
- 证据：`docs/acceptance/runs/20261023-payroll-b3b1/`。
- 结论：`BLACK_BOX_PASS_PENDING_USER_ACCEPTANCE`；不冒充独立验收。


## 2026-10-30 App 月度考勤应出勤调整补充验证

- 业务裁决：`USER-DEC-20261029-006`，Web/App 均采用“薪酬方案派生默认值 + 月度调整 + 考勤快照”规则。
- App 变更：月度考勤新增/编辑页增加应出勤数字输入，新增/更新均提交 `scheduledDays`；无方案时阻断，不使用 22 兜底。
- 验证：App payroll contract PASS；App type-check PASS；`build:mp-weixin` PASS；定向薪酬测试 17/17；Java 22 全量 1129/0/0/0。
- 未覆盖：真实财务账号在 App/微信开发者工具中提交并回读调整值；真实页面证据未取得，因此不能标记为独立验收或已发布。


## 2026-10-30 员工发放跨端补充验证

- Web 从工资单详情发放时，只加载当前工资单来源的 `PayrollItem` 应付；不自动选中其他员工应付。
- Web 发放抽屉继承当前门店范围；App 发放提交日期、备注和复用幂等键。
- 验证：后端 `PayrollEmployeePaymentPageScopeTest`、`PayrollEmployeePaymentAccessTest` 2/2 PASS；Java 22 全量 1130/0/0/0；App/Web type-check 和构建 PASS。
- 未覆盖：真实 Finance/BOSS 在 Web/App 完成发放、重复提交和资金台账/工资条回读。


## 2026-10-30 薪酬入口权限与门店范围补充验证

- App 店长在员工提成页不再看到“计提”；财务/BOSS 保留入口，直接深链仍由路由守卫拦截。
- Web 单店范围下，月度考勤分页和员工选择器继承 `storeId`；提成计提员工选择器继承 `storeId`。
- 验证：后端 `PayrollAttendanceInfoScopeTest`、`PayrollCompensationScopeTest`、`PayrollEmployeePaymentAccessTest` 6/6 PASS；App 契约/type-check/文案/UI/小程序构建 PASS；Web type-check/生产构建 PASS。
- 未覆盖：真实店长/财务账号的门店页面回读、独立验收和发布。


## 2026-10-30 员工本人月度考勤开发验证

- 业务依据：`BR-038`、`DEC-017`。
- 后端定向：`PayrollAttendanceAccessContextTest` 3/3、`PayrollAttendanceSelfViewTest` 3/3、`PayrollAttendanceInfoScopeTest` 2/2，合计 8/8 PASS。
- Java 22 全量：1136 tests / 0 failures / 0 errors / 0 skipped。
- App：payroll contract、type-check、client-copy、UI、navigation、`build:mp-weixin`、subpackages PASS。
- Web：type-check、production build、permissions、store-scope、workbenches PASS。
- Provider：App/Web 均复用 `attendance/summary/page|info`；本人视角固定 `selfOnly=true`，服务端对非管理员工强制 Self。
- 状态：`DEV_VERIFIED / PENDING_REAL_UI_ACCEPTANCE`；未独立验收、未发布。
- 待补：普通员工两端同月数据一致；他人详情拒绝；管理角色本人视角仅本人；管理端原范围不退化。
- 证据：`docs/acceptance/runs/20261030-payroll-self-attendance/`。


## 2026-10-30 员工本人提成开发验证

- 业务依据：`BR-015`、`BR-038`、`DEC-016`、`DEC-017`。
- 后端定向：`PayrollCommissionAccessContextTest` 3/3、`PayrollCommissionSelfViewTest` 3/3、`PayrollCommissionInfoScopeTest` 2/2，合计 8/8 PASS。
- Java 22 全量：1144 tests / 0 failures / 0 errors / 0 skipped。
- App：payroll contract、type-check、UI、navigation、`build:mp-weixin`、subpackages PASS；`check:client-copy` 被与本批无关的 `template-edit.vue:461` 阻断。
- Web：type-check、production build、permissions、store-scope、workbenches PASS。
- Provider：App/Web 均复用 `payroll/commission/page|info`；本人视角固定 `selfOnly=true`，服务端对非管理员工强制 Self。
- 状态：`DEV_VERIFIED / PENDING_REAL_UI_ACCEPTANCE`；未独立验收、未发布。
- 待补：普通员工两端记录/金额一致；他人详情拒绝；管理角色本人视角仅本人；管理端原范围不退化。
- 证据：`docs/acceptance/runs/20261030-payroll-self-commission/`。


## 2026-10-30 员工本人提成真实页面验收

- 环境：本地 dev 后端、Web 真实登录态、微信开发者工具真实小程序。
- 普通员工：App/Web 均只见本人 1 条提成；项目、签约额、比例、提成金额、已计提、已释放、已发放和状态一致。
- 本人只读：App 卡片和 Web 页面均无计提、释放、调整、发放、删除动作。
- 越权：普通员工读取他人已知提成返回 404；伪造 `storeId` 与 `selfOnly=false` 仍只返回本人。
- 管理本人视角：Finance 的 App/Web 均只显示本人 `¥600` 记录。
- 管理范围：Finance 的 App/Web 管理视图均显示 3 条记录，范围不退化。
- 微信开发者工具自动检查：8/8 PASS；Web 1440/1280/1024 无横向溢出；无 Runtime exception/console error。
- 清理：测试员工、凭证和提成全部删除，复查均为 0。
- 当前状态：`USER_ACCEPTED_NON_INDEPENDENT / PENDING_RELEASE`；用户已确认，独立验收未执行。
- 证据：`docs/acceptance/runs/20261030-payroll-self-commission/`。
- 用户验收：2026-10-30 用户明确回复“确认”，将本批记为 `USER_ACCEPTED_NON_INDEPENDENT`；不冒充独立验收，生产发布仍未执行。


## 2026-10-30 店长本店薪酬字段可见范围

- 业务裁决：`DEC-096`、`ANS-BIZ-20261030-001`、`EV-428`；`GAP-074` 关闭。
- 口径：店长本店可读全部现有薪酬详情字段；跨店拒绝。B7 当时动作权限按 `DEC-093` 不变，随后由 `DEC-097` 在本店范围放开，动作实现见 B8A。
- 当前实现：`store-payroll/info` 与 `payroll/item/page` 已返回社保/个税汇总及全部现有金额字段，生产代码无需变更。
- 定向回归：`PayrollCompensationGuardTest` 4/4、`PayrollEmployeePaymentAccessTest` 1/1、`PayrollAttendanceInfoScopeTest` 2/2，合计 7/7 PASS。
- 全量：本轮共享 `target` 并发清理导致 1143/2 环境失败，两项均非薪酬；此前同分支全量 1144/0/0/0。
- 待补：受控店长本店字段完整、跨店 404/403 真实页面验收；动作权限验收转入 B8A。
- 状态：`DEV_VERIFIED / PENDING_REAL_STORE_MANAGER_UI`，未发布、未独立验收。
- 证据：`docs/acceptance/runs/20261030-payroll-manager-field-visibility/`。


## 2026-10-30 店长本店薪酬动作权限

- 业务裁决：`DEC-097`，店长本店确认、退回、Draft 删除、员工工资/提成发放已允许；作废由 `DEC-097` 授权但 B8B DDL/反向事实门禁未完成。
- 后端定向：16/16 PASS，覆盖动作角色、混合门店拒绝、发放 actor、Finance 确认/删除拒绝和 BOSS 回归。
- App：薪酬契约、type-check、UI、导航、文案、mp-weixin 构建、分包检查 PASS。
- Web：type-check、production build、权限、门店范围、工作台门禁 PASS。
- 真实店长本店/跨店/动作验收：待受控企业补录。
- 状态：`DEV_VERIFIED / PENDING_REAL_STORE_MANAGER_UI`；未独立验收、未发布。
