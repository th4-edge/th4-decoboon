# 复验矩阵：REQ-20260918-SMB-P0-FIX

- 来源：`T06-ANS-BIZ-20260918-004.md`
- 状态：等待 dev 完成后执行
- 原则：角色 × 对象 × 状态 × 数据范围 × 失败路径；UI、API、数据库三向一致才算证据。

## A. 量房角色与设计师

| 用例 | 角色/前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| A1 无设计师预约 | Marketer，House PendingMeasure | 预约量房 | 成功，Measure Scheduled，designer 仍为空或不指向 Marketer |
| A2 无设计师完成 | Marketer，Measure Scheduled，designer 为空 | 完成量房 | 失败，提示先指定设计师 |
| A3 指定后完成 | StoreManager/Boss 指定 Designer，Designer 登录 | 完成量房 | 成功，Measure.designer=Designer，Property.latestMeasureAt 非空 |
| A4 非指定人员完成 | 其他 Designer/Marketer | 完成同一量房 | 失败 |
| A5 设计节点 | A3 后 Customer 360 | 查看主链 | 设计仍为待设计，不因量房自动完成 |

## B. PM 开工

| 用例 | 角色/前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| B1 PM 量房读取 | PM 已进入 Project.manager，量房 Completed | 打开项目详情 | 显示量房已完成 |
| B2 未完成量房开工 | PM，Measure Scheduled | 开工 | 失败，原因仅显示真实未完成项 |
| B3 合规开工 | PM，合同 Effective、量房 Completed、PM/日期/阶段齐备 | 开工 | 成功，阶段 Doing，页面与 DB 一致 |
| B4 无项目参与 | 非 PM/非项目参与员工 | 开工 API | 403/404 |
| B5 Finished 状态 | Project Finished | 查看项目 | 不显示“开工前仍需完成”的逆向 readiness |

## C. 阶段开工幂等与事务

| 用例 | 角色/前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| C1 首次开工 | PM，阶段 Todo | 开工 | 同一响应与 DB 状态一致 |
| C2 重复开工 | 对同一阶段再次提交 | 重复请求 | 幂等返回已开工；不新增动态、不重置 actualStart |
| C3 异常响应 | 注入序列化/网络失败 | 开工 | 失败响应时 DB 不提交；或返回明确已完成状态 |
| C4 并发开工 | 两个请求同时提交 | 并发 | 仅一个有效开工事实 |
| C5 动态核对 | C1-C4 后 | 查询 ProjectReport | 仅一条 StageOpen 事实 |

## D. 业主确认

| 用例 | 角色/前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| D1 PM 提交验收 | PM，任务全 Completed | 提交阶段验收和证据 | 成功，Pending |
| D2 PM 代确认 | PM | 调用员工确认接口 | 403，阶段仍 Accepting |
| D3 Boss 代确认 | Boss | 调用员工确认接口 | 403，不能冒充业主 |
| D4 业主确认 | CustomerAccess 授权会话 | 调用 customer/acceptance/update | 成功，阶段 Done，confirmedBy 为客户身份 |
| D5 业主驳回 | CustomerAccess | 驳回 | 阶段 Doing，进入整改 |
| D6 重复确认 | 同一验收单再次确认 | 幂等或明确已处理 | 不重复推进、不覆盖历史 |
| D7 跨项目业主 | 另一项目 CustomerAccess | 访问验收单 | 404/403 |

## E. 收款幂等与财务入口

| 用例 | 角色/前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| E1 UI 新建 | Finance，有 finance:write | 打开收款记录 | 显示“登记收款” |
| E2 正常收款 | Finance，应收余额 3,178.35 | 日期选择并保存 | 成功，Receipt/Ledger/Receivable 一致 |
| E3 同键重试 | 相同 idempotencyKey 重复提交 | 重复请求 | 返回原 receipt，不新增金额 |
| E4 不同键重复业务 | 新 idempotencyKey，相同金额/项目/节点/日期 | 第二笔 | 如未超余额则允许按实际两笔处理 |
| E5 超额 | 金额 > 应收余额 | 提交 | 400，拒绝 |
| E6 跨企业/跨项目 | 其他企业或项目 | 伪造 ID | 404/403 |
| E7 日期契约 | 前端日期控件 | 提交 | 后端 LocalDate 解析一致 |
| E8 台账核对 | E2-E4 | 查询 ledger | 每笔收款一一对应，无重复 |

## F. 合同写权限

| 用例 | 角色/前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| F1 Designer 页面 | Accepted Quote | 打开报价详情 | 无“生成合同/签约/生效立项” |
| F2 Designer API | 直接调用 | 生成/签约/生效 | 403 |
| F3 Boss 正常链路 | Accepted Quote | 生成/签约/生效 | 成功，唯一 Contract/Project |
| F4 重复签约/生效 | Boss 重复请求 | 重复 | 幂等，不新增 Contract/Project |
| F5 未接受报价 | Submitted Quote | 生成合同 | 400/403，不创建合同 |

## G. 成本完整性与利润

| 用例 | 前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| G1 完整成本 | 所有应计成本存在 | 财务总览/项目报告/结算 | 三处 profit/margin 相同 |
| G2 缺少一类成本 | 删除/置空一个应计项 | 三处读取 | costComplete=false，profit/margin=null，均显示成本未完整 |
| G3 恢复成本 | 恢复 G2 成本 | 三处读取 | 同一请求重新读取，恢复为一致数值 |
| G4 0 成本 | 成本为 0 但有真实事实 | 读取 | 不能伪装成高利润；按业务口径判断完整或空 |
| G5 跨页 | 同一 Project | 总览、项目明细、结算 | 不允许一个完整一个不完整 |
| G6 项目结算状态 | Draft/Confirmed/Closed | 项目结算页 | 状态显示正确，不再统一为 `-` |

## H. Marketer 工作台

| 用例 | 角色/前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| H1 无跟进记录 | Marketer 无 follows | 登录工作台 | 不再显示未定义错误 |
| H2 有跟进记录 | Marketer 有 follows | 登录工作台 | 数字与 DB 一致 |
| H3 接口失败 | 模拟 500 | 打开工作台 | 显示明确加载失败，不回退旧数据或空值伪装 |

## I. 预约态与完成态

| 用例 | 前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| I1 仅预约 | Measure Scheduled | Customer 360 | 显示“已预约量房”，不显示“已量房” |
| I2 尚未完成 | Measure Scheduled | 分级 | 不获得已完成量房分值 |
| I3 完成 | Measure Completed | Customer 360 | 显示“已量房”，分值正确 |
| I4 多房屋 | A 房 Completed，B 房 Scheduled | 切换房屋 | 状态严格按当前房屋显示，不串号 |

## 完成判定

- P0 任一失败：结论 `BLOCK`。
- P0 全部通过，blocked 业务项仍未裁决：结论 `CONDITIONAL`，不得宣称合同签署/业主代录/人工材料补录已可用。
- 所有条目有 UI、API、DB 证据和失败路径记录：只可进入 acceptance Lane 后续独立验收，本文件不直接宣布最终验收通过。
