# 设计方案与设计报价审视 · Run Envelope（自主运行授权包）

## 1. 目标

- Domain：`design-quote`
- North Star：设计方案成为客户房屋下可持续编辑、可计算当前报价、可被负责人/店长/财务按数据范围审视的唯一设计事实；设计师负责推进，经营角色不靠列表猜测价格与风险。
- 目标角色：Designer、Boss、StoreManager、Finance；TeamLeader/Marketer/Pm 按既有只读范围。
- 本期范围：B1 服务端价格读模型、成本角色裁剪、App/Web 价格展示、BOSS/店长/财务只读运营查看。
- 本期不做：审批、核价、退回、通过/拒绝、报价过期推断、合同/项目/财务主链改造。

## 2. 批次队列

| 批次 | 垂直切片 | 允许执行 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- |
| B1 | 三端看到同一方案当前价格并完成只读运营查看 | 后端读模型和测试、Web/App 页面、构建和静态门禁 | 三端构建通过、成本裁剪有测试、范围门禁通过 | DEV_VERIFIED |
| B2 | UI/验收与性能 | 真实角色渲染、跨店跨企业 HTTP、10倍数据验证 | UI_REVIEWED / ACCEPTED | PENDING |
| DQ-X3 | App 方案模版库浏览与实例化 | 复用 `design/template/page|info|instantiate`；App 列表/详情/实例化页面；类型检查与构建 | 3 个路由 App/Web 共享；模板不含价格；实例化后回读；真实页面复核 | UI_REVIEWED |
| DQ-X4 | App 方案模版创建、区域/分项维护与发布 | 复用 `design/template/save|publish|room/save|room/delete|items/batchsave`；App 编辑页与构建 | 5 个路由 App/Web 共享；发布后只读；模板不含价格；真实页面复核 | UI_REVIEWED |
| DQ-X5 | App 已发布模版后续生命周期 | 复用 `design/template/copy|disable|version/build`；App 动作面板与构建 | 3 个路由 App/Web 共享；历史报价不受影响；真实页面复核 | UI_REVIEWED |
| DQ-X6 | 财务报价服务端汇总 | 复用报价数据范围；新增单行聚合 Provider；Web 删除前端求和 | summary 与分组/列表对账；页面真实渲染 | DEV_VERIFIED |
| DQ-X7 | 项目合同档案服务端汇总 | workbench 增加合同行汇总；Web 消费服务端摘要 | 行数与金额对账；三角色成本边界；真实页面 | DEV_VERIFIED |
| DQ-X8 | 合同基线行数据库分页 | `project/contract-line/page` 使用 Pageable；Web 按页消费；变更来源只用 range | 默认/上限/稳定排序/角色成本测试；真实 API 和 Web 页面 | DEV_VERIFIED |
| DQ-X9 | 业主与小程序合同行分页同源 | `customer/contract-line/page` Pageable；App 显式 current/size；workbench 行摘要 | 业主 API 双页、App 门禁负向自测、小程序原生页面 | DEV_VERIFIED |
| DQ-X10 | 合同读模型成本聚合 | `ProjectContractContext.enrich` 单条成本聚合；补齐 DQ-X6 Provider 门禁登记 | L2 全量 1184/1184；三角色 API 与 DB checksum | DEV_VERIFIED |
| DQ-X11 | 增减项来源选择器分页 | Range PICKER_PAGE、服务端筛选/facets、批量快照 | API size=2、Web/小程序真实页面、独立复验 55/55 | ACCEPTED |

## 3. 自主决策权限

监督/执行任务可以自行决定：

- 技术实现方式：聚合查询、瞬态读模型、现有 Context 扩展；
- 页面布局与交互细节：在冻结的信息层级和只读边界内；
- 测试补充：价格选择、成本裁剪、导航、页面契约；
- 已裁决规则内的字段/API 调整：`DEC-078` 范围内；
- 本地/开发/测试环境验证。

## 4. 人工门禁

遇到以下情况必须停止该分支并写入决策包：

- 新增审批、核价、退回或审视状态；
- 改变价格、成本、利润、权限或数据范围语义；
- DDL、迁移、生产数据删除；
- 生产发布；
- 正式小程序提审与发布；
- 报价过期规则（`GAP-061`）需要业务裁决。

## 5. 质量门禁

- 业务裁决已引用：`DEC-078`、`ANS-BIZ-20261021-003`；
- 定向测试与构建通过；
- 微信开发者工具原生页面证据：已完成；物理真机：待执行；
- 无 UNMAPPED/STALE；
- 独立验证 PASS：待执行；
- 回滚路径明确：三端回退到各自上一稳定提交/版本。

## 6. 预算与止损

- 每个缺陷最多修复轮次：2
- UI 审查最多轮次：2
- 同指标连续无改善停止次数：2
- 最大自主批次：1
- 无进展时处理：停止并回报缺失账号、数据或环境。

## 7. Return Packet

回来后必须提供：

- 已完成批次与证据；
- 未完成项与原因；
- 自主假设；
- 需要用户决策的事项；
- 下一批提示词；
- 是否可进入 ACCEPTED / RELEASED。

## 8. 授权确认

- 授权人：用户
- 授权时间：2026-10-21
- 授权范围：完善设计方案后同步业务智能体，并在 Web/App 进行功能完善
- 备注：不含生产发布和正式小程序提审发布。
