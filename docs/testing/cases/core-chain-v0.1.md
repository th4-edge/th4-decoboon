# 核心全链路测试用例库 V0.1

> Suite：`SUITE-CORE-CHAIN-V0.1`  
> Oracle：`ZDQ-BIZ-20261101-V0.1`  
> 用例数：44

本文件由用例 JSON 自动生成，执行以 JSON、oracle lock 和追溯矩阵为准。

## 客户与房屋（CRM）

### TC-CRM-BOSS-CUSTOMER-001 · Boss建立客户与房屋并保留经营关系

- 优先级：`P1`
- 角色：Boss
- 端面：Web, App
- 类型：happy, parity
- 业务规则：BR-001, OBJ-001, DEC-094, BR-059, OBJ-010
- 对象：Customer, Property
- 夹具：`FC-20261031-01-new`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- Boss登录昭青科技
- 客户手机号在测试前缀下唯一
- 房屋绑定明确客户

**步骤**

1. Boss在Web新建客户
2. 为同一客户新增房屋
3. 刷新列表和详情
4. 在小程序回读客户与房屋

**预期页面**

- 客户卡显示负责人、门店、当前阶段和下一步
- 房屋明确显示所属客户和业务状态

**预期状态**

- Customer和Property均写入服务端真实主键
- Company来自登录员工
- App/Web读取同一客户事实

**不变量**

- 客户、房屋、负责人、设计师和门店归属必须来自同一服务端事实
- 跟进、量房、转交失败时不得改变原客户和房屋状态
- Customer在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 前端伪造companyId改变归属
- 房屋未绑定客户
- 两端负责人或房屋数量不同

**Provider**：`customer/customer/save`, `customer/customer/page`, `customer/customer/info`, `customer/project/page`

**页面目标**：`web:/workbench/customers`, `web:/workbench/customers/:customerId`, `app:/pages/customer/list`, `app:/subpackages/customer/pages/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CRM-MARKETER-MEASURE-002 · 业务员完成首联并预约量房

- 优先级：`P1`
- 角色：Marketer
- 端面：Web, App
- 类型：happy, recovery
- 业务规则：BR-001, SCN-001, DEC-052
- 对象：Customer, Follow, Measure
- 夹具：`FC-20261031-03-pending_measure`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 客户已分配给业务员
- 下一步为待首联或待量房

**步骤**

1. 登记跟进记录
2. 预约未来量房时间
3. 刷新客户详情和量房工作台
4. 重复进入确认下一步不丢失

**预期页面**

- 客户显示已首联、量房时间、负责人和下一动作
- 量房预约在工作台可见

**预期状态**

- Follow有真实内容和下次跟进时间
- Measure.scheduledAt存在且为待量房
- 预约失败时原客户状态不变

**不变量**

- 客户、房屋、负责人、设计师和门店归属必须来自同一服务端事实
- 跟进、量房、转交失败时不得改变原客户和房屋状态
- Customer在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 空跟进内容被保存
- 预约过去时间成功
- 员工通过改请求绕过门店范围

**Provider**：`customer/customer-follow/save`, `customer/customer/measure`, `customer/customer/page`, `project/manager/customer-follow/page`

**页面目标**：`web:/workbench/customers`, `app:/subpackages/customer/pages/follow`, `app:/subpackages/customer/pages/measure-workbench`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CRM-MANAGER-HANDOFF-003 · 店长把客户与房屋转交设计师

- 优先级：`P0`
- 角色：Boss, StoreManager, Designer
- 端面：Web, App
- 类型：happy, handoff, permission
- 业务规则：BR-001, DEC-066, OBJ-001
- 对象：Customer, Property, Employee
- 夹具：`FC-20261031-03-pending_measure`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 店长在总店范围
- 目标设计师为同企业在职员工
- 房屋未绑定其他设计师

**步骤**

1. 店长选择目标设计师并转交
2. 设计师查看待我跟进/待我量房
3. Boss回读负责人

**预期页面**

- 客户与房屋显示同一承接设计师
- 设计师工作台出现该客户
- Boss与店长看到相同事实

**预期状态**

- 客户设计师、房屋设计师和待办负责人同源
- 交接记录保留原负责人和新负责人
- 非本店设计师不能承接

**不变量**

- 客户、房屋、负责人、设计师和门店归属必须来自同一服务端事实
- 跟进、量房、转交失败时不得改变原客户和房屋状态
- Customer在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 跨企业设计师ID被接受
- 客户转交但房屋仍指向旧设计师
- 设计师可见未转交客户

**Provider**：`customer/customer/handoff`, `customer/customer/info`, `customer/customer/page`, `user/designer/list`

**页面目标**：`web:/workbench/customers/:customerId`, `app:/subpackages/customer/pages/detail`, `app:/subpackages/customer/pages/designer-followup-workbench`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CRM-DESIGNER-PROPERTY-004 · 设计师完成量房且多房屋绑定不串房

- 优先级：`P0`
- 角色：Designer
- 端面：Web, App
- 类型：happy, boundary, parity
- 业务规则：BR-001, OBJ-007, DEC-052, DEC-062, DEC-069
- 对象：Property, Measure, MeasureRegion
- 夹具：`FC-20261031-04-measured`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 同一客户至少两套房屋
- 设计师已承接其中一套

**步骤**

1. 打开指定房屋量房
2. 录入区域实测面积/周长并完成量房
3. 切换到另一套房屋回读
4. 刷新并回读量房记录

**预期页面**

- 当前房屋名称、地址和量房状态明确
- 测量区域显示实测来源与完成时间

**预期状态**

- 完成时间不得早于预约时间
- 量房完成后原始区域冻结
- 方案继承量房快照但不反向修改量房事实
- activePropertyId始终明确

**不变量**

- 客户、房屋、负责人、设计师和门店归属必须来自同一服务端事实
- 跟进、量房、转交失败时不得改变原客户和房屋状态
- Property在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 页面隐式使用第一套房屋
- 完成量房自动写设计方案
- 修改区域反向改写历史方案快照

**Provider**：`property/measure/save`, `property/measure/page`, `property/measure/region/save`, `property/measure/region/list`

**页面目标**：`web:/designer/measure`, `app:/subpackages/customer/pages/measure-record`, `app:/subpackages/design/pages/scheme`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CRM-PERMISSION-STORE-005 · 店长不得查看或操作其他门店客户

- 优先级：`P0`
- 角色：StoreManager
- 端面：Web, App
- 类型：permission, negative
- 业务规则：BR-001, DEC-065, DEC-070
- 对象：Customer, Store
- 夹具：`NEGATIVE-CROSS-STORE`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 总店店长与滨湖店客户均存在
- 账号角色为StoreManager

**步骤**

1. 访问本店客户列表
2. 用已知ID读取滨湖店客户
3. 尝试修改滨湖店客户

**预期页面**

- 列表不出现滨湖店客户
- 直接访问显示无权限或不存在
- 不泄露其他门店详情

**预期状态**

- 数据范围由登录员工门店推导
- 前端伪造storeId不扩大范围
- 越权请求无写副作用

**不变量**

- 客户、房屋、负责人、设计师和门店归属必须来自同一服务端事实
- 跟进、量房、转交失败时不得改变原客户和房屋状态
- Customer在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 跨店客户出现在列表
- 直接ID读取成功
- 越权修改返回成功

**Provider**：`customer/customer/page`, `customer/customer/info`, `customer/customer/update`

**页面目标**：`web:/workbench/customers`, `app:/pages/customer/list`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CRM-FINANCE-OWNER-006 · 财务客户详情转交负责人候选与权限范围同源

- 优先级：`P1`
- 角色：Finance, StoreManager, Marketer, Boss
- 端面：Web, App
- 类型：permission, happy, negative, parity
- 业务规则：BR-001, DEC-065, DEC-070, USER-DEC-20261029-009
- 对象：Customer, Employee, Store
- 夹具：`FC-20261031-03-pending_measure`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 存在总部财务、门店财务、店长和业务员测试账号
- 客户处于财务可见数据范围并已绑定客户负责人
- 存在可用于真实转交且不影响核心链路的独立测试客户

**步骤**

1. 总部财务打开客户详情并点击客户负责人
2. 验证负责人候选非空且覆盖企业范围后完成一次受控转交
3. 门店财务点击客户负责人并验证候选人仅为本门店人员
4. 店长回读候选人范围和转交结果
5. 业务员尝试加载候选人或直接转交并确认被拒绝
6. 在Web和App刷新后回读同一负责人事实

**预期页面**

- 财务点击客户负责人不出现无权限错误
- 财务看到非空负责人候选及岗位/门店摘要
- 无权限角色不显示可执行的负责人转交入口或明确说明无权限
- Web与App显示同一客户负责人

**预期状态**

- 总部财务候选范围为企业，门店财务和店长为本人门店
- 财务主更新Provider和候选目录Provider同时允许
- 业务员等其他角色被主写Provider或候选Provider明确拒绝
- 转交成功后客户负责人、门店和团队归属来自同一负责人事实
- 失败或拒绝请求不改变原负责人

**不变量**

- 客户负责人转交必须同时验证主写Provider和候选目录依赖Provider
- 财务不得获得设计师指派或改派能力
- 同一角色的候选数据范围必须与其中台数据范围一致
- Customer在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 财务点击客户负责人后候选目录返回403
- 总部财务被错误限制为当前默认门店
- 门店财务或店长可跨门店转交
- 业务员可加载负责人候选或成功转交
- 财务通过客户负责人入口获得设计师指派能力
- 转交失败但客户负责人发生变化

**Provider**：`user/employee/page`, `customer/customer/update`, `customer/customer/info`

**页面目标**：`web:/workbench/customers/:customerId`, `app:/subpackages/customer/pages/detail`

**证据**：page_path, before_after_screenshot, network_response, role_api_responses, state_readback

## 设计方案与报价（DESIGN_QUOTE）

### TC-DQ-DESIGNER-DESIGN-001 · 设计师从已量房房屋建立需求与方案

- 优先级：`P1`
- 角色：Designer
- 端面：Web, App
- 类型：happy
- 业务规则：BR-001, OBJ-007, DEC-069
- 对象：Design, Requirement, Measure
- 夹具：`FC-20261031-05-designing`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 房屋已完成量房
- 设计师为该客户承接人

**步骤**

1. 从量房记录进入设计
2. 录入需求和空间尺寸
3. 保存方案并刷新
4. 回读方案绑定客户和房屋

**预期页面**

- 方案显示房号、风格、空间和设计状态
- 需求与方案上下文连续

**预期状态**

- Design绑定明确customerId/propertyId
- 量房快照与服务端数据一致
- 方案保存不修改历史量房单

**不变量**

- 方案、报价版本和金额快照必须由服务端事实推导
- 历史设计/报价版本不得被新版本覆盖
- Design在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 未量房直接创建正式方案
- 设计绑定错误房屋
- 保存后客户/房屋上下文丢失

**Provider**：`design/requirement/save`, `design/scheme/save`, `design/scheme/page`, `property/measure/page`

**页面目标**：`web:/designer/customers`, `web:/designer/design/:designId`, `app:/subpackages/design/pages/list`, `app:/subpackages/design/pages/scheme`

**证据**：page_path, before_after_screenshot, state_readback

### TC-DQ-DESIGNER-SCHEME-002 · 设计方案完成前必须绑定有效分项

- 优先级：`P1`
- 角色：Designer
- 端面：Web, App
- 类型：happy, negative, boundary
- 业务规则：BR-001, DEC-050
- 对象：Design, DesignItem, CatalogItem
- 夹具：`FC-20261031-06-scheme_ready`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 方案处于草稿
- 企业报价项目库含施工与材料项

**步骤**

1. 添加空间和施工/材料分项
2. 将方案提交为Ready
3. 刷新方案和报价入口

**预期页面**

- 分项来自企业报价项目库并显示数量、单位、价格来源
- 方案Ready后才出现生成报价入口

**预期状态**

- DesignItem引用真实CatalogItem
- 金额由服务端按目录和数量计算
- 空分项或非法数量不能Ready

**不变量**

- 方案、报价版本和金额快照必须由服务端事实推导
- 历史设计/报价版本不得被新版本覆盖
- Design在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 手改前端金额写库
- 无材料项却生成正式报价
- 停用项目进入新方案

**Provider**：`design/item/save`, `design/scheme/build`, `design/scheme/info`, `catalog/item/page`

**页面目标**：`web:/designer/design/:designId`, `app:/subpackages/design/pages/scheme`

**证据**：page_path, before_after_screenshot, state_readback

### TC-DQ-DESIGNER-QUOTE-003 · 报价草稿由服务端金额生成并可提交

- 优先级：`P1`
- 角色：Designer
- 端面：Web, App
- 类型：happy, boundary, parity
- 业务规则：BR-001, BR-002, DEC-071
- 对象：Quote, QuoteLine, Design
- 夹具：`FC-20261031-07-quote_draft`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 方案已Ready
- 设计分项和折扣条件已确定

**步骤**

1. 生成报价
2. 核对折扣、分项金额和总额
3. 提交报价
4. 刷新并跨端回读

**预期页面**

- 报价列表显示Draft/Submitted状态和版本
- 页面金额与后端汇总一致

**预期状态**

- Quote绑定设计、客户和房屋
- 总额由服务端计算
- 提交后状态不可回到草稿
- 历史版本不覆盖

**不变量**

- 方案、报价版本和金额快照必须由服务端事实推导
- 历史设计/报价版本不得被新版本覆盖
- Quote在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 前端reduce重算正式金额
- 空报价提交成功
- 刷新后金额变化

**Provider**：`quote/save`, `quote/update`, `quote/info`, `quote/line/page`

**页面目标**：`web:/designer/quotes/:quoteId`, `app:/subpackages/project/pages/owner-quote`, `app:/pages/project/list`

**证据**：page_path, before_after_screenshot, state_readback

### TC-DQ-BOSS-QUOTE-004 · 报价驳回、重提和接受保留版本历史

- 优先级：`P1`
- 角色：Boss, Designer
- 端面：Web, App
- 类型：happy, order, history
- 业务规则：BR-002, DEC-071
- 对象：Quote
- 夹具：`FC-20261031-08-quote_rejected`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 存在已提交报价
- Boss具备接受权限
- 设计师可发起新版本

**步骤**

1. Designer提交报价V1
2. Boss核对后驳回或接受
3. 如驳回，设计师修改后生成V2
4. 回读V1历史与当前V2

**预期页面**

- 页面显示当前有效报价和版本历史
- Boss看到客户、房屋、设计、金额和决策入口

**预期状态**

- 状态按Draft→Submitted→Rejected/Accepted流转
- 被替代历史保留只读
- 报价接受与客户归属口径不丢失

**不变量**

- 方案、报价版本和金额快照必须由服务端事实推导
- 历史设计/报价版本不得被新版本覆盖
- Quote在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- V1被覆盖删除
- Marketer直接接受报价
- 接受后金额快照变化

**Provider**：`quote/update`, `quote/info`, `quote/page`

**页面目标**：`web:/designer/quotes`, `web:/designer/quotes/:quoteId`, `app:/subpackages/project/pages/owner-quote`

**证据**：page_path, before_after_screenshot, state_readback

### TC-DQ-PERMISSION-QUOTE-005 · 报价接受登记权限与操作人留痕

- 优先级：`P0`
- 角色：Marketer, Designer, Boss
- 端面：Web, App
- 类型：permission, negative, happy, idempotency
- 业务规则：BR-002, DEC-071
- 对象：Quote, EmployeeRole
- 夹具：`FC-20261031-09-quote_submitted`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 报价已Submitted
- Marketer和Designer登录正常

**步骤**

1. Marketer查看报价只读视图
2. 尝试接受报价
3. Designer尝试Boss确认
4. Boss执行合法接受后回读

**预期页面**

- Marketer可查看报价事实但无报价接受动作
- Designer在Submitted报价上看到登记客户接受的合法动作
- Accepted后面显示登记人/时间来源，不写成客户系统自助确认

**预期状态**

- Marketer置Accepted返回403且报价保持Submitted
- Designer置Accepted成功，报价进入Accepted并写入acceptedBy=当前设计师、acceptedAt=当前时间
- 重复置Accepted被状态机拒绝（400），状态仍Accepted且acceptedBy/acceptedAt不被覆盖

**不变量**

- 报价接受登记人来自登录员工，不信任前端employeeId
- acceptedBy与acceptedAt同时存在，重复请求不得覆盖首次登记
- Marketer不能置Accepted
- 报价金额和成本快照不因接受动作改变

**禁止结果**

- Marketer接受报价成功
- acceptedBy为空或由前端伪造
- 重复接受覆盖首次acceptedBy或产生第二事实
- 把员工登记说成客户本人系统确认

**Provider**：`quote/info`, `quote/update`

**页面目标**：`web:/workbench/customers`, `web:/designer/quotes/:quoteId`, `app:/subpackages/quote/pages/detail`

**证据**：role_api_responses, before_after_screenshots, accepted_by_db, repeat_accept_state_readback

## 合同与项目立项（CONTRACT）

### TC-CT-BOSS-CONTRACT-001 · 合同从草稿到签约再到生效的顺序不可跳步

- 优先级：`P0`
- 角色：Boss, Designer
- 端面：Web, App
- 类型：happy, order
- 业务规则：BR-002, DEC-040, DEC-048
- 对象：Contract, Quote, Project
- 夹具：`FC-20261031-11-contract_signed`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 报价Accepted
- 存在启用的施工计划和付款计划
- 用户授权本地合同测试

**步骤**

1. Boss从报价发起合同草稿
2. 填写签约金额、开工日和计划模板
3. 执行签约
4. 再执行生效

**预期页面**

- 合同页显示草稿、待签约、已签约和已生效状态
- 签约前不生成立项项目

**预期状态**

- 合同绑定报价、客户和房屋
- 签约金额大于0
- 计划开工日与施工模板锁定
- 未签约不能生效

**不变量**

- 合同签约、生效和项目派生必须按状态机顺序发生
- 合同金额与项目、应收派生事实保持一致
- Contract在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 创建合同请求偷偷完成签约
- 合同未签先生成项目
- 生效后回写报价快照

**Provider**：`project/contract/save`, `project/contract/update`, `project/contract/info`

**页面目标**：`web:/designer/contract`, `web:/designer/projects/:projectId/contract`, `app:/subpackages/project/pages/contract`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CT-BOSS-PROJECT-002 · 合同生效后项目、阶段、材料计划和应收同源生成

- 优先级：`P0`
- 角色：Boss, Finance, Pm
- 端面：Web, App
- 类型：happy, parity
- 业务规则：BR-002, BR-001, DEC-048, OBJ-001, SCN-002
- 对象：Contract, Project, ProjectStage, MaterialPlan, Receivable
- 夹具：`FC-20261031-11-contract_signed`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 合同已Signed
- 施工和付款计划有效

**步骤**

1. Boss执行合同生效
2. 打开项目和详情
3. 财务查看应收
4. 项目经理查看阶段和待办

**预期页面**

- 项目状态、合同金额、开工日和负责人可见
- 阶段含适用/不适用判断
- 应收来自同一合同节点

**预期状态**

- 合同与项目一对一生成
- 项目、阶段、材料计划和应收共享租户/门店/项目归属
- 重复生效幂等

**不变量**

- 合同签约、生效和项目派生必须按状态机顺序发生
- 合同金额与项目、应收派生事实保持一致
- Contract在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 生效后无项目
- 重复生效生成两套项目或应收
- 财务金额与合同不一致

**Provider**：`project/contract/update`, `project/project/page`, `project/project/info`, `project/stage/page`, `finance/receivable/page`

**页面目标**：`web:/workbench/customers/:customerId`, `web:/pm/workbench`, `web:/finance/projects/:projectId`, `app:/subpackages/project/pages/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CT-ORDER-CONTRACT-003 · 合同未签约直接生效必须被拒绝

- 优先级：`P0`
- 角色：Boss
- 端面：Web, App
- 类型：negative, order, permission
- 业务规则：BR-002, DEC-040, DEC-048
- 对象：Contract
- 夹具：`FC-20261031-11-contract_signed`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 存在Draft合同
- Boss具备生效权限

**步骤**

1. 打开合同草稿
2. 尝试直接执行生效
3. 刷新合同和项目列表
4. 按正确顺序签约后再生效

**预期页面**

- 页面提示必须完成签约
- 刷新后仍为草稿且无项目

**预期状态**

- 数据库合同状态保持Draft/Signed正确值
- 无项目、无应收副作用

**不变量**

- 合同签约、生效和项目派生必须按状态机顺序发生
- 合同金额与项目、应收派生事实保持一致
- Contract在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 直接生效成功
- 出现半成品项目
- 仅前端隐藏按钮但API可绕过

**Provider**：`project/contract/update`, `project/contract/info`

**页面目标**：`web:/designer/contract`, `app:/subpackages/project/pages/contract`

**证据**：page_path, before_after_screenshot, state_readback

## 合同变更（CHANGE）

### TC-CHG-DESIGNER-CHANGE-001 · 增减项三类动作生成当前有效剩余

- 优先级：`P1`
- 角色：Designer, Boss, Finance
- 端面：Web, App
- 类型：happy, boundary, parity
- 业务规则：BR-035, BR-041, DEC-055, DEC-068, SCN-004
- 对象：ContractChange, ContractChangeItem, Project
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目合同Effective
- 存在可变更合同分项

**步骤**

1. Designer发起Add/Remove/Replace场景
2. 核对源分项和目标分项
3. 完成审批或生效流程
4. 财务与项目回读当前有效剩余

**预期页面**

- 变更页显示类型、来源、数量、金额影响和当前状态
- 历史变更与当前有效剩余分开显示

**预期状态**

- 只按当前有效剩余计算下游成本与材料
- Pending不提前扣减
- 历史快照不被重写

**不变量**

- 当前有效剩余按已确认变更计算，Pending不得提前扣减
- 历史合同、报价和成本快照不得被变更回写
- ContractChange在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 删除历史变更
- Pending直接扣减成本
- Replace只关闭来源而不产生目标

**Provider**：`project/contract-change/save`, `project/contract-change/update`, `project/contract-change/page`, `project/contract-change/range`

**页面目标**：`web:/workbench/contract-changes`, `web:/designer/projects/:projectId/changes`, `app:/subpackages/project/pages/change`

**证据**：page_path, before_after_screenshot, state_readback

### TC-CHG-FINANCE-COST-002 · 有效减项不得制造伪成本完整性或伪利润

- 优先级：`P0`
- 角色：Finance, Boss
- 端面：Web
- 类型：negative, boundary, parity
- 业务规则：BR-003, BR-041, BR-045, MET-002, DEC-092
- 对象：ProjectCost, ContractChange, Settlement
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目存在已确认Remove/Quantity变更
- 至少一个真实应计成本仍缺失

**步骤**

1. 财务打开项目账本
2. 核对预计成本、实际成本和缺失项
3. 打开结算草稿
4. 尝试确认结算

**预期页面**

- 成本状态明确显示缺失项
- 利润显示成本未完整而非0
- 确认结算按钮禁用并说明原因

**预期状态**

- 有效Remove从材料完整性分母剔除
- 未发生成本不得按0计
- costComplete=false时profit和grossMargin为空

**不变量**

- 当前有效剩余按已确认变更计算，Pending不得提前扣减
- 历史合同、报价和成本快照不得被变更回写
- ProjectCost在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 显示0利润
- 以预计值冒充实际成本
- 成本不完整仍确认结算

**Provider**：`finance/project/report`, `finance/settlement/save`, `finance/settlement/update`

**页面目标**：`web:/finance/projects/:projectId`, `app:/subpackages/finance/pages/project`

**证据**：page_path, before_after_screenshot, state_readback

## 项目阶段与施工（PROJECT）

### TC-PJ-PM-STAGE-001 · 项目经理只推进适用阶段且不适用阶段不进入分母

- 优先级：`P1`
- 角色：Pm, Boss
- 端面：Web, App
- 类型：happy, boundary, parity
- 业务规则：BR-008, DEC-049, DEC-057, SCN-003
- 对象：Project, ProjectStage, Task
- 夹具：`FC-20261031-12-project_started`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目已由合同生效生成
- 存在无合同施工行的空阶段

**步骤**

1. Pm打开项目工作面
2. 查看阶段列表和本项不涉及原因
3. 开启首个适用阶段
4. Boss回读项目状态和当前阶段

**预期页面**

- 不适用阶段明确显示原因且不可开工
- 当前适用阶段可执行开始动作
- 项目状态更新为施工中

**预期状态**

- NotApplicable阶段排除进度分母
- 项目状态与阶段状态一致
- 重复开工幂等

**不变量**

- 阶段、任务和项目状态由服务端事实单向推进
- 失败、重复或越权动作不得产生半成品或重复进度
- Project在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 空阶段可提交开工或验收
- 项目进度分母包含不适用阶段
- App/Web当前阶段不一致

**Provider**：`project/stage/page`, `project/stage/update`, `project/project/info`

**页面目标**：`web:/pm/workbench`, `web:/workbench/customers/:customerId`, `app:/subpackages/project/pages/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-PJ-PM-TASK-002 · 施工任务完成必须有真实进度与现场证据

- 优先级：`P1`
- 角色：Pm
- 端面：Web, App
- 类型：happy, negative, recovery
- 业务规则：BR-008, SCN-003, DEC-088
- 对象：Task, Evidence
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 适用阶段已开工
- 存在Todo任务

**步骤**

1. Pm开始任务
2. 未上传证据直接尝试完成
3. 上传合成测试证据后完成
4. 刷新并回读任务、阶段和项目

**预期页面**

- 任务显示负责人、状态、进度和证据
- 无证据时完成按钮禁用或后端拒绝

**预期状态**

- Task状态按Todo→Doing→Done推进
- 完成写真实evidenceUrls
- 阶段和项目进度由任务事实推导

**不变量**

- 阶段、任务和项目状态由服务端事实单向推进
- 失败、重复或越权动作不得产生半成品或重复进度
- Task在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 无证据完成任务
- 只在前端伪造100%
- 刷新后任务回旧状态但页面显示完成

**Provider**：`project/task/update`, `project/task/page`, `project/task/info`

**页面目标**：`web:/pm/workbench`, `web:/pm/projects/:projectId/acceptance`, `app:/subpackages/project/pages/task/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-PJ-PERMISSION-003 · 项目经理不得修改价格、合同或财务归属

- 优先级：`P0`
- 角色：Pm, Boss, Finance, StoreManager
- 端面：Web, App
- 类型：permission, negative
- 业务规则：BR-043, DEC-016, DEC-089
- 对象：Project, Contract, EmployeeRole
- 夹具：`FC-20261031-12-project_started`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- Pm为项目负责人
- 项目合同和财务事实已存在

**步骤**

1. Pm打开项目详情
2. 尝试修改合同金额或财务归属
3. Finance查看金额
4. Boss查看成员编辑入口
5. Boss和店长打开项目成员调整并验证候选人可加载

**预期页面**

- Pm只看到施工相关动作
- 合同和财务字段只读
- Boss/Finance按权限看到对应动作
- Boss和店长可执行项目成员调整；Pm和财务无成员调整入口

**预期状态**

- 价格、成本和财务归属不因Pm请求变化
- 角色能力从登录员工与项目参与关系推导
- 拒绝有可读原因

**不变量**

- 阶段、任务和项目状态由服务端事实单向推进
- 失败、重复或越权动作不得产生半成品或重复进度
- Project在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- Pm修改合同金额成功
- 伪造managerId扩大权限
- 财务字段出现在无权限页面

**Provider**：`project/project/update`, `project/task/update`, `finance/project/report`, `user/employee/page`

**页面目标**：`web:/designer/projects`, `web:/pm/workbench`, `web:/finance/projects/:projectId`, `app:/subpackages/project/pages/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-PJ-IDEMPOTENCY-004 · 重复点击任务完成不产生重复进度或副作用

- 优先级：`P1`
- 角色：Pm
- 端面：Web, App
- 类型：concurrency, idempotency
- 业务规则：BR-008, DEC-029
- 对象：Task, Project
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 任务已有证据
- 网络可模拟重复提交

**步骤**

1. 双击完成任务或在网络重试中重复提交
2. 刷新页面
3. 查询任务和项目进度

**预期页面**

- 页面只出现一次成功反馈
- 刷新后仍为单个Done事实

**预期状态**

- 重复请求返回同一结果
- 不重复写动态、通知或成本
- 项目进度不重复累加

**不变量**

- 阶段、任务和项目状态由服务端事实单向推进
- 失败、重复或越权动作不得产生半成品或重复进度
- Task在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 生成两条完成记录
- 进度超过100%
- 第二次请求返回500且状态不明

**Provider**：`project/task/update`, `project/task/info`

**页面目标**：`web:/pm/workbench`, `app:/subpackages/project/pages/task/detail`

**证据**：page_path, before_after_screenshot, state_readback

## 材料选材与采购（MATERIAL）

### TC-MAT-DESIGNER-SELECTION-001 · 未完成选材的计划行不能进入主材作业单

- 优先级：`P0`
- 角色：Designer, Finance
- 端面：Web, App
- 类型：happy, negative
- 业务规则：BR-004, BR-009, DEC-008, DEC-056, SCN-006
- 对象：MaterialPlan, MaterialSelection
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 合同已生效并生成材料计划
- 计划行未完成现场选材确认

**步骤**

1. Designer在App查看材料计划
2. 尝试直接创建作业单
3. 完成选材确认后重试
4. Finance回读可下单状态

**预期页面**

- 未选材行明确显示阻断原因
- 完成后状态变为可下单
- 选材记录显示品牌、规格、颜色与确认人

**预期状态**

- 计划行与选材记录同源
- 选材完成后采购门禁解除
- 未选材不得被前端降级放行

**不变量**

- 选材、计划、作业单、采购、到场和应付必须共用项目与计划来源
- 重复下单或到场不得重复形成采购、应付或实际成本
- MaterialPlan在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 未选材仍可下单
- 作业单绕过计划行
- 选材内容跨端不同

**Provider**：`material/selection/save`, `project/material-plan/page`, `material/sheet/save`

**页面目标**：`web:/designer/projects/:projectId/materials`, `app:/subpackages/project/pages/selection-board`, `app:/subpackages/project/pages/material-sheet-create`

**证据**：page_path, before_after_screenshot, state_readback

### TC-MAT-DESIGNER-SHEET-002 · 作业单只能来源于合同材料计划

- 优先级：`P0`
- 角色：Designer, Finance
- 端面：Web, App
- 类型：happy, negative
- 业务规则：BR-044, BR-045, OBJ-008, DEC-072
- 对象：MaterialOrderSheet, MaterialPlan, PurchaseOrder
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 材料计划ReadyToOrder
- 供应商有效

**步骤**

1. Designer创建Tile作业单并添加计划行
2. 填写安装位置、数量和单位
3. 提交请购
4. Finance查看作业单与采购入口

**预期页面**

- 作业单显示来源计划、安装位置、数量和状态
- 提交后进入已提交请购
- 合同外材料提示先走增减项

**预期状态**

- 作业单行sourceKind=Plan
- 数量不超过净用量+损耗
- 作业单不直接写成本或应付

**不变量**

- 选材、计划、作业单、采购、到场和应付必须共用项目与计划来源
- 重复下单或到场不得重复形成采购、应付或实际成本
- MaterialOrderSheet在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 作业单添加自由文本合同外材料
- 超量保存成功
- 提交后生成第二张重复采购单

**Provider**：`material/sheet/save`, `material/sheet/submit`, `material/sheet/enums`, `material/sheet/page`

**页面目标**：`web:/finance/material-ordering`, `app:/subpackages/project/pages/material-sheet-create`

**证据**：page_path, before_after_screenshot, state_readback

### TC-MAT-BOUNDARY-QUANTITY-003 · 材料下单数量不得超过净用量与损耗

- 优先级：`P0`
- 角色：Designer, Finance
- 端面：Web, App
- 类型：boundary, negative
- 业务规则：BR-009, BR-044, DEC-072
- 对象：MaterialPlan, MaterialOrderSheetItem
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 计划行净用量为N且损耗率为L
- 作业单草稿可编辑

**步骤**

1. 输入等于上限的数量并保存
2. 输入超过上限的数量并提交
3. 刷新作业单

**预期页面**

- 等于上限可保存
- 超过上限显示明确超量提示并阻断

**预期状态**

- 后端按净用量+损耗计算上限
- 非法数量不落库
- 现有行数量不因失败而改变

**不变量**

- 选材、计划、作业单、采购、到场和应付必须共用项目与计划来源
- 重复下单或到场不得重复形成采购、应付或实际成本
- MaterialPlan在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 前端任填任意数量
- 超量后自动截断且无提示
- 失败请求部分写入

**Provider**：`material/sheet/save`, `material/sheet/update`, `material/sheet/submit`, `project/material-plan/page`

**页面目标**：`web:/finance/material-ordering`, `app:/subpackages/project/pages/material-sheet-create`

**证据**：page_path, before_after_screenshot, state_readback

### TC-MAT-FINANCE-ORDER-004 · 作业单下单形成唯一采购单和应付

- 优先级：`P0`
- 角色：Finance, Boss, StoreManager
- 端面：Web, App
- 类型：happy, idempotency, parity
- 业务规则：BR-003, BR-044, BR-045, SCN-005, SCN-021, DEC-072, DEC-073
- 对象：MaterialOrderSheet, PurchaseOrder, Payable
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 作业单Submitted
- 存在有效供应商
- 采购金额可定价或无价

**步骤**

1. Finance选择供应商和expectedAt下单
2. 重复提交同一请求
3. 回读采购单、应付和作业单
4. App财务回读同一事实

**预期页面**

- 作业单显示Ordered
- 采购单显示金额/数量和供应商
- 应付显示同一来源编号

**预期状态**

- 一次业务请求只生成一张采购单和一条应付
- 重复请求幂等
- 采购金额与应付金额一致

**不变量**

- 选材、计划、作业单、采购、到场和应付必须共用项目与计划来源
- 重复下单或到场不得重复形成采购、应付或实际成本
- MaterialOrderSheet在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 重复生成采购单
- 应付金额与采购金额不一致
- App/Web显示不同单号或金额

**Provider**：`partner/page`, `material/sheet/page`, `material/sheet/order`, `project/purchase/page`, `finance/payable/page`, `material/sheet/info`

**页面目标**：`web:/finance/material-ordering`, `app:/subpackages/project/pages/material-sheet-order`, `app:/subpackages/finance/pages/payables`

**证据**：page_path, before_after_screenshot, state_readback

### TC-MAT-PM-ARRIVAL-005 · 材料到货驱动计划、作业单、采购与成本联动

- 优先级：`P1`
- 角色：Pm, Finance
- 端面：Web, App
- 类型：happy, parity, boundary
- 业务规则：BR-003, BR-045, DEC-072, DEC-092
- 对象：GoodsReceipt, PurchaseOrder, MaterialPlan, ProjectCost
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 采购单Ordered
- 采购价已知或明确为空

**步骤**

1. Pm登记到场数量和测试证据
2. Finance回读应付账期与项目成本
3. 不足数量再次到场
4. 达到采购量后回读终态

**预期页面**

- 到场记录显示数量、时间、证据和采购来源
- 采购单显示部分到场或已到货

**预期状态**

- 到场累计不超过采购数量
- 已知价格才写实际成本
- 无价采购不得写0成本或伪利润

**不变量**

- 选材、计划、作业单、采购、到场和应付必须共用项目与计划来源
- 重复下单或到场不得重复形成采购、应付或实际成本
- GoodsReceipt在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 超采购量到场成功
- 无价采购显示0实际成本
- 作业单/采购/计划状态互相矛盾

**Provider**：`project/goods-receipt/save`, `project/goods-receipt/page`, `finance/payable/page`, `finance/project/report`

**页面目标**：`web:/pm/workbench`, `web:/finance/projects/:projectId`, `app:/subpackages/project/pages/material-sheet-detail`

**证据**：page_path, before_after_screenshot, state_readback

## 阶段验收与整改（ACCEPTANCE）

### TC-ACC-PM-SUBMIT-001 · 任务和证据未完成不得提交阶段验收

- 优先级：`P1`
- 角色：Pm
- 端面：Web, App
- 类型：happy, negative, recovery
- 业务规则：BR-008, SCN-003, DEC-087, DEC-088
- 对象：Acceptance, Task, Evidence
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 阶段已开工
- 至少一个任务未完成

**步骤**

1. Pm尝试提交阶段验收
2. 完成全部任务并上传证据后重试
3. 刷新验收工作区

**预期页面**

- 未完成时提交被阻断
- 完成后可填写说明和证据提交
- 提交后显示待业主确认

**预期状态**

- Acceptance状态Pending且员工无代确认入口
- 证据与阶段绑定
- 刷新后状态与轮次不丢失

**不变量**

- 员工只能提交验收，业主本人才能确认或驳回
- 验收轮次和整改结论只追加不覆盖，历史轮次可回读
- Acceptance在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 未完成任务也能提交
- 员工可替业主确认
- 提交失败但数据库已生成Pending

**Provider**：`project/acceptance/save`, `project/acceptance/page`, `project/task/page`

**页面目标**：`web:/pm/projects/:projectId/acceptance`, `app:/subpackages/project/pages/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-ACC-PERMISSION-EMPLOYEE-002 · 任何员工角色都不得代替业主确认验收

- 优先级：`P0`
- 角色：Boss, StoreManager, Pm, Finance
- 端面：Web, App
- 类型：permission, negative
- 业务规则：BR-043, DEC-015, DEC-009
- 对象：Acceptance, EmployeeRole, Owner
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 验收单Pending
- 各员工账号可登录

**步骤**

1. Boss查看验收单
2. 店长、Pm、财务分别查看
3. 分别尝试调用确认或驳回入口
4. Owner使用匹配身份完成合法确认

**预期页面**

- 员工页面文案明确仅业主本人确认
- 员工无确认或驳回按钮
- Owner页面有确认和驳回动作

**预期状态**

- 员工请求返回403
- 状态保持Pending
- Owner成功后记录真实确认主体

**不变量**

- 员工只能提交验收，业主本人才能确认或驳回
- 验收轮次和整改结论只追加不覆盖，历史轮次可回读
- Acceptance在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 员工代确认成功
- 通过改localStorage或接口参数伪造owner
- 员工拒绝后状态变为Failed

**Provider**：`project/acceptance/update`, `project/acceptance/info`, `customer/acceptance/update`

**页面目标**：`web:/pm/projects/:projectId/acceptance`, `app:/subpackages/project/pages/owner-acceptance`

**证据**：page_path, before_after_screenshot, state_readback

### TC-ACC-OWNER-IDENTITY-003 · 非业主手机号不能进入项目确认验收

- 优先级：`P0`
- 角色：Owner
- 端面：Owner
- 类型：permission, negative
- 业务规则：BR-043, DEC-015, DEC-009
- 对象：OwnerAccess, Property, Acceptance
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目码有效
- 当前微信授权手机号与客户预留手机号不一致

**步骤**

1. Owner输入项目码
2. 授权手机号
3. 尝试读取验收详情
4. 更换为匹配业主身份后重试

**预期页面**

- 非匹配手机号显示拒绝原因和恢复路径
- 不显示合同、成本或其他客户信息

**预期状态**

- 授权访问绑定客户手机号与项目
- 拒绝不创建有效owner session
- 匹配后可读取同一Pending验收

**不变量**

- 员工只能提交验收，业主本人才能确认或驳回
- 验收轮次和整改结论只追加不覆盖，历史轮次可回读
- OwnerAccess在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 仅凭项目码进入并确认
- 显示其他业主项目
- 非匹配身份可调用更新接口

**Provider**：`project/owner-qrcode/build`, `project/owner-preview/info`, `customer/shareview/info`

**页面目标**：`owner:/subpackages/project/pages/owner-acceptance`, `app:/pages/owner/enter`

**证据**：page_path, before_after_screenshot, state_readback

### TC-ACC-OWNER-CONFIRM-004 · 业主确认后阶段和项目事实继续推进

- 优先级：`P0`
- 角色：Owner, Pm, Boss
- 端面：Owner, Web, App
- 类型：happy, parity
- 业务规则：BR-008, BR-043, DEC-015
- 对象：Acceptance, ProjectStage, Project
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 业主身份匹配
- 验收单Pending
- 阶段任务已完成

**步骤**

1. Owner打开验收详情查看证据
2. 执行确认并二次确认
3. Pm和Boss刷新回读
4. Owner再次进入查看历史

**预期页面**

- Owner看到验收结论确认成功
- 阶段状态更新且项目下一步明确
- 历史轮次保留确认时间和主体

**预期状态**

- 确认只影响对应阶段
- 员工端看到同一确认事实
- 刷新不丢失
- 重复确认幂等

**不变量**

- 员工只能提交验收，业主本人才能确认或驳回
- 验收轮次和整改结论只追加不覆盖，历史轮次可回读
- Acceptance在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 确认后阶段仍Pending
- 生成重复验收轮次
- App/Web显示不同确认时间

**Provider**：`customer/acceptance/update`, `project/acceptance/info`, `project/stage/page`, `project/project/info`

**页面目标**：`owner:/subpackages/project/pages/owner-acceptance`, `web:/pm/projects/:projectId/acceptance`, `app:/subpackages/project/pages/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-ACC-OWNER-REJECT-005 · 业主驳回后生成整改并进入下一轮复验

- 优先级：`P1`
- 角色：Owner, Pm
- 端面：Owner, Web, App
- 类型：happy, negative, history
- 业务规则：BR-008, SCN-010, DEC-007
- 对象：Acceptance, Rectification, AcceptanceRound
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 业主身份匹配
- 验收Pending且存在现场证据

**步骤**

1. Owner填写问题和证据并驳回
2. Pm查看整改责任、期限和证据
3. Pm完成整改并重新提交
4. Owner复验并确认或再次驳回

**预期页面**

- 驳回后显示整改对象、责任人、截止时间
- 重新提交形成第2轮验收
- 历史轮次和结论只追加不覆盖

**预期状态**

- 整改只承接质量事实，不改变金额或合同
- 每轮有独立证据
- 复验通过后阶段才Done

**不变量**

- 员工只能提交验收，业主本人才能确认或驳回
- 验收轮次和整改结论只追加不覆盖，历史轮次可回读
- Acceptance在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 驳回直接改项目金额
- 第二轮覆盖第一轮
- 整改完成但无复验证据

**Provider**：`customer/acceptance/update`, `project/acceptance/round/page`, `project/acceptance/rectify/page`, `project/acceptance/save`

**页面目标**：`owner:/subpackages/project/pages/owner-acceptance`, `web:/pm/projects/:projectId/acceptance`, `app:/subpackages/project/pages/rectify-board`

**证据**：page_path, before_after_screenshot, state_readback

## 财务与经营结果（FINANCE）

### TC-FIN-FINANCE-RECEIVABLE-001 · 合同节点应收与手工应收同源形成待收计划

- 优先级：`P1`
- 角色：Finance, Boss
- 端面：Web, App
- 类型：happy, parity
- 业务规则：BR-002, BR-003, MET-006, DEC-089
- 对象：Receivable, Contract, Project
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目合同Effective
- 合同付款节点已生成

**步骤**

1. Finance打开应收计划
2. 核对自动应收与手工补充应收
3. 按项目筛选
4. Boss在项目账本回读

**预期页面**

- 应收列表显示项目、阶段、金额、已收、待收和到期日
- 自动和手工来源可区分

**预期状态**

- 应收归属同一项目和门店
- 待收=金额-已收
- 合同节点金额不改写合同金额

**不变量**

- 应收、收款、应付、付款、台账和结算必须按同一项目与门店核算
- 成本不完整时利润为空，不得使用0或预计值伪装实际结果
- Receivable在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 待收为负数
- 自动应收重复生成
- Boss与Finance汇总不同

**Provider**：`finance/receivable/page`, `finance/receivable/report`, `finance/project/report`

**页面目标**：`web:/finance/receivables`, `web:/finance/projects/:projectId`, `app:/subpackages/finance/pages/receivables`

**证据**：page_path, before_after_screenshot, state_readback

### TC-FIN-FINANCE-RECEIPT-002 · 收款不得超过余额且冲正可恢复应收

- 优先级：`P0`
- 角色：Finance
- 端面：Web, App
- 类型：happy, negative, idempotency
- 业务规则：BR-003, DEC-077, MET-005
- 对象：Receipt, Receivable, Ledger
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 存在待收应收
- 金额和到期日明确

**步骤**

1. 登记部分收款
2. 重复提交同一幂等请求
3. 尝试超余额收款
4. 执行收款冲正并回读

**预期页面**

- 收款记录、应收余额和台账同步更新
- 重复请求只生成一笔收款
- 冲正后应收余额恢复

**预期状态**

- 收款累计不超过应收
- 冲正不产生负余额
- 重复收款幂等
- 台账净额与收付事实一致

**不变量**

- 应收、收款、应付、付款、台账和结算必须按同一项目与门店核算
- 成本不完整时利润为空，不得使用0或预计值伪装实际结果
- Receipt在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 重复收款
- 超余额成功
- 冲正后待收不变或变负

**Provider**：`finance/receipt/save`, `finance/receipt/reverse`, `finance/receivable/page`, `finance/ledger/report`

**页面目标**：`web:/finance/receipts`, `web:/finance/receivables`, `app:/subpackages/finance/pages/receipts`

**证据**：page_path, before_after_screenshot, state_readback

### TC-FIN-FINANCE-PAYABLE-003 · 材料采购应付与付款核销保持同一事实

- 优先级：`P0`
- 角色：Finance, Boss, StoreManager
- 端面：Web, App
- 类型：happy, boundary, idempotency
- 业务规则：BR-003, BR-045, DEC-072, DEC-089
- 对象：Payable, Payment, PurchaseOrder
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 采购单已下单
- 自动应付存在
- 另有手工应付可测试双路径

**步骤**

1. Finance查看应付列表
2. 登记部分付款
3. 重复提交付款
4. 按采购来源和手工来源分别核对

**预期页面**

- 应付显示金额、已付、待付、到期日和来源
- 付款后状态为部分付款或已付

**预期状态**

- 应付余额=金额-已付
- 付款不超过余额
- 自动和手工应付使用同一核算口径

**不变量**

- 应收、收款、应付、付款、台账和结算必须按同一项目与门店核算
- 成本不完整时利润为空，不得使用0或预计值伪装实际结果
- Payable在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 付款后待付不更新
- 重复付款
- 来源编号与金额不一致

**Provider**：`finance/payable/page`, `finance/payment/save`, `finance/payment/page`, `finance/partner-ledger/report`

**页面目标**：`web:/finance/payables`, `web:/finance/payments`, `app:/subpackages/finance/pages/payables`, `app:/subpackages/finance/pages/payments`

**证据**：page_path, before_after_screenshot, state_readback

### TC-FIN-FINANCE-COST-004 · 任一应计成本缺失时利润必须为空

- 优先级：`P0`
- 角色：Finance, Boss
- 端面：Web, App
- 类型：negative, boundary, parity
- 业务规则：BR-003, BR-045, MET-002, DEC-092
- 对象：ProjectCost, Settlement, Payable
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目存在合同金额和部分实际成本
- 至少一个人工或材料成本未发生或未定价

**步骤**

1. 财务打开项目核算
2. 核对预计成本、实际成本和成本完整状态
3. Boss回读项目利润
4. 补齐成本后再回读

**预期页面**

- 页面显示成本未完整及缺失项
- 利润和毛利率显示为空或不可确认

**预期状态**

- costComplete=false时profit和grossMargin为null
- 不使用0或预计值伪装实际利润
- 同一项目跨端口径一致

**不变量**

- 应收、收款、应付、付款、台账和结算必须按同一项目与门店核算
- 成本不完整时利润为空，不得使用0或预计值伪装实际结果
- ProjectCost在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 显示0利润
- 用预计成本计算实际利润
- 成本缺失却显示成本完整

**Provider**：`finance/project/report`, `finance/project/page`, `finance/settlement/page`

**页面目标**：`web:/finance/projects/:projectId`, `app:/subpackages/finance/pages/project`

**证据**：page_path, before_after_screenshot, state_readback

### TC-FIN-FINANCE-SETTLEMENT-005 · 结算确认必须受成本完整与状态门禁约束

- 优先级：`P0`
- 角色：Finance, Boss
- 端面：Web, App
- 类型：happy, negative, order
- 业务规则：BR-045, MET-002, DEC-092
- 对象：Settlement, ProjectCost
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目存在结算草稿
- 至少一个应计成本缺失

**步骤**

1. Finance生成结算草稿
2. 核对最终金额、扣减、奖励、质保金和成本
3. 尝试确认结算
4. 补齐成本后重新确认（如获授权）

**预期页面**

- 草稿显示金额、成本完整度和确认门禁
- 成本不完整时确认按钮禁用并说明原因

**预期状态**

- Draft→Confirmed只能在前置满足后发生
- 结算金额和成本快照稳定
- 利润为空时不得确认

**不变量**

- 应收、收款、应付、付款、台账和结算必须按同一项目与门店核算
- 成本不完整时利润为空，不得使用0或预计值伪装实际结果
- Settlement在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 成本不完整仍确认
- 重复生成结算
- 确认后金额或成本被回写

**Provider**：`finance/settlement/save`, `finance/settlement/page`, `finance/settlement/update`

**页面目标**：`web:/finance/projects/:projectId`, `app:/subpackages/finance/pages/project`

**证据**：page_path, before_after_screenshot, state_readback

### TC-FIN-CROSSSURFACE-PARITY-006 · Boss、店长、财务与App回读同一项目金额

- 优先级：`P0`
- 角色：Boss, StoreManager, Finance
- 端面：Web, App
- 类型：parity, permission
- 业务规则：BR-043, MET-005, MET-006, DEC-070, DEC-089, SCN-015, BR-059
- 对象：Project, Receivable, Payable, Settlement
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 项目处于财务可见状态
- 合同金额、已收、待收、应付、已付和成本完整度已形成

**步骤**

1. Finance查看项目核算
2. Boss查看同一项目
3. 总店店长查看本店项目
4. App财务首页和项目页回读

**预期页面**

- 各端显示同项目合同金额、回款、待收、待付和成本状态
- 无权限字段不展示

**预期状态**

- 同一筛选条件下列表total与report count一致
- 金额口径来自服务端聚合
- 门店范围不改变金额事实

**不变量**

- 应收、收款、应付、付款、台账和结算必须按同一项目与门店核算
- 成本不完整时利润为空，不得使用0或预计值伪装实际结果
- Project在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 不同端金额相差一分钱以上
- 无权限角色看到成本价
- App用默认0伪装缺失金额

**Provider**：`finance/project/report`, `finance/receivable/report`, `finance/payable/report`, `project/project/info`

**页面目标**：`web:/finance/projects/:projectId`, `web:/pm/workbench`, `app:/pages/finance/index`, `app:/subpackages/finance/pages/project`

**证据**：four_role_screenshots, api_summary, sql_reconciliation

### TC-FIN-EXPENSE-PROXY-007 · 企业费用代填员工候选与代理权限同源

- 优先级：`P1`
- 角色：Boss, StoreManager, Finance, Pm
- 端面：App
- 类型：permission, happy, negative, recovery
- 业务规则：BR-057, DEC-091
- 对象：Expense, Employee, Store
- 夹具：`FC-20261031-12-project_started`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- Boss、店长、财务和项目经理账号可用
- 店长归属门店且存在本店在职员工
- 存在费用分类和项目夹具

**步骤**

1. Boss打开费用新增并加载企业员工候选
2. 店长打开费用新增并加载本店员工候选
3. 财务和普通员工确认只能提交本人费用
4. 选择可代理员工后保存费用并回读提交人、归属员工和收款员工
5. 刷新费用详情确认同一事实

**预期页面**

- Boss可选择企业范围员工
- 店长只能选择本店员工且候选加载不报权限错误
- 财务和普通员工的费用归属员工只显示本人，不提供代理选择
- 候选加载失败时显示可恢复错误而非伪空列表

**预期状态**

- Boss可代企业范围员工提交
- 店长可代本店员工提交，不得跨店
- 财务和普通员工不得代他人提交
- 报销收款员工必须等于费用归属员工
- 实际提交人与费用归属员工分别留痕

**不变量**

- 费用代理动作必须同时验证费用保存Provider和员工候选Provider
- 候选数据范围必须与费用归属校验一致
- 失败请求不得生成费用或改变既有费用
- Expense在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 店长打开员工候选时报403
- 财务成功代他人提交
- 店长选择其他门店员工
- 报销收款员工与归属员工不一致
- 失败后生成草稿费用

**Provider**：`expense/expenses/save`, `expense/expenses/page`, `payroll/employee/page`

**页面目标**：`app:/subpackages/finance/pages/expense-edit`, `app:/subpackages/payroll/pages/employee-picker`

**证据**：page_path, before_after_screenshot, network_response, role_api_responses, state_readback

## 工作台与正确引导（WORKBENCH）

### TC-WB-BOSS-GUIDANCE-001 · Boss工作台只呈现真实事实和可执行下一步

- 优先级：`P1`
- 角色：Boss
- 端面：Web, App
- 类型：happy, parity
- 业务规则：BR-029, BR-030, DEC-002, DEC-033, DEC-034, SCN-016
- 对象：Workbench, Risk, Project, Receivable
- 夹具：`FC-20261031-13-acceptance_pending`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- Boss登录企业负责人视角
- 存在待验收、待付款或待收风险

**步骤**

1. 打开Boss工作台
2. 点击待验收入口
3. 返回后点击待付款或待收入口
4. 核对入口对象与来源

**预期页面**

- 首页先显示今日经营判断和待处理事项
- 每个卡片显示事实、下一步和责任人

**预期状态**

- 工作台入口落到真实对象锚点
- 汇总数与底层Provider一致
- 无数据时显示暂不可确认而非0伪装

**不变量**

- 工作台待办与汇总必须来自真实对象和独立汇总Provider
- 空态、失败和加载态不得用本地常量伪造业务事实
- Workbench在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 卡片可点击但进入错误对象
- 空数据被显示为0
- 同一入口落到不同状态源

**Provider**：`content/dashboard/report`, `project/project/overview`, `finance/summary/report`

**页面目标**：`web:/workbench`, `app:/pages/index/index`

**证据**：page_path, before_after_screenshot, state_readback

### TC-WB-PM-GUIDANCE-002 · 项目经理工作台从真实项目与阶段生成待办

- 优先级：`P1`
- 角色：Pm
- 端面：Web, App
- 类型：happy, parity, recovery
- 业务规则：BR-030, BR-005, SCN-018
- 对象：Workbench, ProjectStage, Task
- 夹具：`FC-20261031-12-project_started`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- Pm被分配到多个项目
- 存在施工中和待验收项目

**步骤**

1. Pm打开工作台
2. 按今日开工、验收或材料待办进入项目
3. 返回并刷新
4. 核对待办数量与项目状态

**预期页面**

- 工作台显示项目名、阶段、下一步和逾期或异常
- 入口直接定位到项目和阶段

**预期状态**

- 待办来自真实项目、阶段和任务
- 无权限项目不出现
- 刷新后待办一致

**不变量**

- 工作台待办与汇总必须来自真实对象和独立汇总Provider
- 空态、失败和加载态不得用本地常量伪造业务事实
- Workbench在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 默认选中第一条项目代替用户选择
- 入口只到列表不定位对象
- 待办完成后仍显示

**Provider**：`project/project/page`, `project/stage/page`, `project/task/page`

**页面目标**：`web:/pm/workbench`, `app:/pages/project/list`, `app:/subpackages/project/pages/detail`

**证据**：page_path, before_after_screenshot, state_readback

### TC-WB-RECOVERY-003 · 空态、失败和刷新恢复不得伪装成功

- 优先级：`P1`
- 角色：Boss, Finance, Pm
- 端面：Web, App
- 类型：recovery, negative, boundary
- 业务规则：BR-029, BR-030, DEC-002
- 对象：Workbench, QueryState
- 夹具：`NEGATIVE-EMPTY-ERROR`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 构造无数据、接口失败和网络中断场景

**步骤**

1. 打开空数据页面
2. 模拟接口失败
3. 刷新页面并重试
4. 核对页面和数据库状态

**预期页面**

- 空态说明没有真实数据
- 失败态显示暂不可确认和恢复路径
- 重试成功后显示真实数据

**预期状态**

- 未完成查询不显示0或成功
- 失败请求不产生写副作用
- 不出现前端本地演示记录

**不变量**

- 工作台待办与汇总必须来自真实对象和独立汇总Provider
- 空态、失败和加载态不得用本地常量伪造业务事实
- Workbench在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 用0、22或空数组伪装事实
- 提示成功但后端无事实
- 刷新后页面自行补默认数据

**Provider**：`project/project/page`, `finance/receivable/page`, `customer/customer/page`

**页面目标**：`web:/workbench`, `web:/finance/workbench`, `app:/pages/index/index`

**证据**：screenshots_all_states, network_log, db_before_after

## 权限、分页、编号与跨端（CROSS_CUTTING）

### TC-XC-SECURITY-TENANT-001 · 跨企业租户ID不得读取或修改业务对象

- 优先级：`P0`
- 角色：Boss, StoreManager, Finance
- 端面：Web, App
- 类型：permission, negative
- 业务规则：OBJ-001, DEC-070, BR-043
- 对象：Company, Customer, Project, Finance
- 夹具：`NEGATIVE-CROSS-TENANT`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 创建第二企业或使用不存在企业ID
- 当前账号属于昭青科技

**步骤**

1. 尝试用其他企业ID读取客户
2. 尝试读取项目和财务
3. 尝试按其他企业ID保存写操作
4. 用正常账号回读原企业事实

**预期页面**

- 其他企业对象显示不存在或无权限
- 不泄露名称、金额、联系人

**预期状态**

- 服务端从登录员工推导com_id
- 前端companyId被忽略
- 拒绝请求无副作用

**不变量**

- 租户、门店、角色和敏感字段必须由登录员工及服务端权限裁剪
- 增长型列表必须数据库分页且列表总数与同筛选汇总一致
- Company在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 跨企业列表泄漏
- 详情返回其他企业数据
- 写请求成功

**Provider**：`customer/customer/info`, `project/project/info`, `finance/project/report`

**页面目标**：`web:/workbench/customers`, `app:/pages/customer/list`

**证据**：page_path, before_after_screenshot, state_readback

### TC-XC-CONTRACT-PAGINATION-002 · 增长型列表分页与汇总数量同筛选一致

- 优先级：`P1`
- 角色：Boss, Finance
- 端面：Web, App
- 类型：boundary, parity, nonfunctional
- 业务规则：BR-025, BR-031, DEC-036, DEC-037
- 对象：List, ReportSummary
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 客户、应收或项目列表存在多页数据
- 同一筛选条件明确

**步骤**

1. 以默认size打开列表
2. 检查普通列表最大50限制
3. 切换筛选并核对totalElements
4. 调用report或count核对同筛选数量

**预期页面**

- 列表显示分页、总数与当前筛选
- 不出现固定200或500的隐式全量拉取

**预期状态**

- 列表totalElements等于report.count
- 后续页面不靠前端subList伪造
- 排序稳定

**不变量**

- 租户、门店、角色和敏感字段必须由登录员工及服务端权限裁剪
- 增长型列表必须数据库分页且列表总数与同筛选汇总一致
- List在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 超过50的列表仍返回全量
- 列表与报告总数不一致
- 只在前端分页

**Provider**：`customer/customer/page`, `finance/receivable/page`, `finance/receivable/report`

**页面目标**：`web:/workbench/customers`, `web:/finance/receivables`, `app:/pages/customer/list`

**证据**：page_path, before_after_screenshot, state_readback

### TC-XC-COPY-IDENTIFIER-003 · 业务界面不展示系统内部编号

- 优先级：`P1`
- 角色：Boss, StoreManager, Finance, Marketer, Designer, Pm
- 端面：Web, App
- 类型：negative, parity
- 业务规则：BR-060, DEC-095
- 对象：Customer, Quote, Contract, Project, Receivable, Payable
- 夹具：`ALL-STAGE-FIXTURES`
- 执行方式：`review`；状态：`READY`

**前置条件**

- 各角色可访问对应页面
- 数据中已存在内部主键编号

**步骤**

1. 逐页检查列表、详情、表单和搜索提示
2. 检查内部编号是否出现在副标题或提示
3. 用名称、地址、状态、责任人识别对象

**预期页面**

- 用户无需记忆内部编号
- 法定编号和统一社会信用代码仍按规则显示

**预期状态**

- 内部编号仅保留在API、DB、审计、二维码和文件命名
- 页面不以编号作为主要识别

**不变量**

- 租户、门店、角色和敏感字段必须由登录员工及服务端权限裁剪
- 增长型列表必须数据库分页且列表总数与同筛选汇总一致
- Customer在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- 客户编号、项目编号或合同编号出现在UI
- 通过编号才能完成搜索
- 编号被误删导致接口契约变化

**Provider**：`customer/customer/page`, `project/project/page`, `finance/project/report`

**页面目标**：`web:/workbench/customers`, `web:/finance/projects/:projectId`, `app:/pages/customer/list`, `app:/subpackages/project/pages/detail`

**证据**：multi_page_screenshots, dom_text_scan

### TC-XC-FINANCE-VISIBILITY-004 · 成本价与利润字段只对授权角色可见

- 优先级：`P0`
- 角色：Boss, StoreManager, Finance, Designer, Pm, Marketer
- 端面：Web, App
- 类型：permission, negative
- 业务规则：BR-003, DEC-016, DEC-092, OBJ-010
- 对象：Cost, Profit, Project
- 夹具：`FIN-FULL-CHAIN`
- 执行方式：`hybrid`；状态：`READY`

**前置条件**

- 同一项目存在合同价、成本价、实际成本和利润字段

**步骤**

1. Boss、店长、财务查看项目核算
2. Designer、Pm、Marketer查看项目详情
3. 检查API响应字段
4. 与页面显示对照

**预期页面**

- 授权角色看到约定的成本和利润信息
- 非授权角色只看销售价或业务状态

**预期状态**

- 成本字段在响应层裁剪而非仅前端隐藏
- 利润为空规则一致
- 跨角色数据范围正确

**不变量**

- 租户、门店、角色和敏感字段必须由登录员工及服务端权限裁剪
- 增长型列表必须数据库分页且列表总数与同筛选汇总一致
- Cost在刷新、重进和跨端回读后保持同一事实

**禁止结果**

- Designer看到标准成本
- Marketer通过API拿到利润
- 非授权页面显示伪利润

**Provider**：`project/project/info`, `finance/project/report`, `quote/info`

**页面目标**：`web:/finance/projects/:projectId`, `web:/designer/projects/:projectId`, `app:/subpackages/finance/pages/project`

**证据**：role_api_responses, page_screenshots, schema_field_scan

